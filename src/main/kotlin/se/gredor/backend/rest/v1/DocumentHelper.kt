package se.gredor.backend.rest.v1

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.pdfbox.pdmodel.PDDocument
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Selector
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.api.InformationApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.config.BolagsverketApiConfig
import se.gredor.backend.model.tellustalk.Signature
import java.io.BufferedInputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.SignatureException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

@ApplicationScoped
class DocumentHelper {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var bolagsverketApiConfig: BolagsverketApiConfig

    @Inject
    @RestClient
    private lateinit var informationApi: InformationApi

    fun verifyDocument(
        doc: PDDocument,
        pdfByte: ByteArray
    ): VerifyDocumentResult {
        var isSignatureValid = false
        var isCertificateTrusted = false

        try {
            if (doc.signatureDictionaries.size == 1) {
                // Hämta signatur
                val signature = doc.signatureDictionaries[0]
                val signatureAsBytes = signature.getContents(pdfByte)
                val signedContentAsBytes = signature.getSignedContent(pdfByte)
                val cms = CMSSignedData(CMSProcessableByteArray(signedContentAsBytes), signatureAsBytes)

                // Validera dokument
                val signerInfo: SignerInformation = cms.signerInfos.signers.iterator().next() as SignerInformation
                val cert = cms.certificates
                    .getMatches(signerInfo.sid as Selector<X509CertificateHolder?>?)
                    .iterator()
                    .next()
                val verifier = JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider()).build(cert)
                isSignatureValid = signerInfo.verify(verifier)

                // Skapa TrustStore med rotcertifikatet
                val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
                trustStore.load(null)
                val fis = object {}.javaClass.classLoader.getResourceAsStream("certs/I_CA_Root.cer")
                if (fis == null) {
                    throw Exception("Could not load trusted certificate")
                }
                val bis = BufferedInputStream(fis)
                val cf = CertificateFactory.getInstance("X.509")
                while (bis.available() > 0) {
                    val rootCert = cf.generateCertificate(bis)
                    trustStore.setCertificateEntry(UUID.randomUUID().toString(), rootCert)
                }

                // Validera certifikat
                // TODO: Ytterligare validering?
                val validationResultPerCert = ArrayList<Boolean>()
                for (certInChainHolder in cms.certificates.getMatches(null).toList()) {
                    val certInChain = JcaX509CertificateConverter().getCertificate(certInChainHolder)
                    val certIsValid = getRootCertificate(certInChain, trustStore) != null
                    if (certIsValid) {
                        trustStore.setCertificateEntry(UUID.randomUUID().toString(), certInChain)
                    }
                    validationResultPerCert.add(certIsValid)
                }
                isCertificateTrusted = validationResultPerCert.size > 1 && validationResultPerCert.all { it }
            }
        } catch (_: GeneralSecurityException) {
            // Dokumentet klarade inte verifiering
        } catch (_: CMSException) {
            // Dokumentet klarade inte verifiering
        }

        return VerifyDocumentResult(
            isSignatureValid = isSignatureValid,
            isCertificateTrusted = isCertificateTrusted
        )
    }

    fun documentHasValidSigner(doc: PDDocument, signerPnr: String, companyOrgnr: String): Boolean {
        val catalog = doc.documentCatalog
        val names = catalog.names
        val embeddedFiles = names.embeddedFiles
        val embeddedFileNames = embeddedFiles.names
        val signaturesJsonFile = embeddedFileNames["signatures.json"]
        val signaturesJsonBytes = signaturesJsonFile?.embeddedFile?.toByteArray()

        val mapper = jacksonObjectMapper()
        val signatures: List<Signature> = mapper.readValue(signaturesJsonBytes, jacksonTypeRef<List<Signature>>())
        val signers = signatures.map { it.personalId }

        val grunduppgifter = informationApi.grunduppgifter(
            "${bolagsverketApiConfig.hamtaArsredovisningsinformationBaseurl()}/${bolagsverketApiConfig.hamtaArsredovisningsinformationVersion()}/",
            companyOrgnr
        )
        return grunduppgifter.foretradare.any { signers.contains("se$it") && it.equals(signerPnr) }
    }

    @Throws(KeyStoreException::class)
    fun findIssuerCertificate(certificate: X509Certificate, trustStore: KeyStore): X509Certificate? {
        val aliases = trustStore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            val cert = trustStore.getCertificate(alias)
            if (cert is X509Certificate) {
                val x509Cert = cert
                if (x509Cert.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
                    return x509Cert
                }
            }
        }
        return null
    }

    @Throws(GeneralSecurityException::class)
    fun getRootCertificate(endEntityCertificate: X509Certificate?, trustStore: KeyStore?): X509Certificate? {
        if (endEntityCertificate == null || trustStore == null) {
            return null
        }

        val issuerCertificate = findIssuerCertificate(endEntityCertificate, trustStore)
        if (issuerCertificate != null) {
            return if (isRoot(issuerCertificate)) {
                issuerCertificate
            } else {
                getRootCertificate(issuerCertificate, trustStore)
            }
        }
        return null
    }

    @Throws(GeneralSecurityException::class)
    private fun isRoot(certificate: X509Certificate): Boolean {
        try {
            certificate.verify(certificate.publicKey)
            return certificate.keyUsage != null && certificate.keyUsage[5]
        } catch (_: SignatureException) {
            return false
        }
    }

    data class VerifyDocumentResult(
        val isSignatureValid: Boolean,
        val isCertificateTrusted: Boolean
    )
}
