package se.gredor.backend.rest.v1

import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.apache.pdfbox.Loader
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.InlamningApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.KontrollApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.config.BolagsverketApiConfig
import se.gredor.backend.config.RestConfig
import se.gredor.backend.model.gredor.AuthenticatableRequest
import se.gredor.backend.model.gredor.PreparationRequest
import se.gredor.backend.model.gredor.SubmissionRequest
import se.gredor.backend.model.gredor.ValidationRequest

@Path("/v1/submission-flow/")
class SubmissionFlowResource {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var documentHelper: DocumentHelper

    @Inject
    private lateinit var bolagsverketApiConfig: BolagsverketApiConfig

    @Inject
    private lateinit var restConfig: RestConfig

    @Inject
    @RestClient
    private lateinit var inlamningApi: InlamningApi

    @Inject
    @RestClient
    private lateinit var kontrollApi: KontrollApi

    @POST
    @Path("prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun prepare(@Valid preparationRequest: PreparationRequest): SkapaTokenOK {
        if (!verifyDocumentAndSigner(preparationRequest)) {
            throw BadRequestException("Invalid signer or document")
        }

        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(preparationRequest.signerPnr)
                .orgnr(preparationRequest.companyOrgnr)
        )
        return skapaTokenResult
    }

    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun validate(@Valid validationRequest: ValidationRequest): KontrolleraSvar {
        if (!verifyDocumentAndSigner(validationRequest)) {
            throw BadRequestException("Invalid signer or document")
        }

        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(validationRequest.signerPnr)
                .orgnr(validationRequest.companyOrgnr)
        )

        val handling = Handling()
            .fil(validationRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        val kontrolleraResult = kontrollApi.kontrollera(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            KontrolleraAnrop()
                .handling(handling)
        )

        return kontrolleraResult
    }

    @POST
    @Path("submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submit(@Valid submissionRequest: SubmissionRequest): InlamningOK {
        if (!verifyDocumentAndSigner(submissionRequest)) {
            throw BadRequestException("Invalid signer or document")
        }

        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(submissionRequest.signerPnr)
                .orgnr(submissionRequest.companyOrgnr)
        )

        val handling = Handling()
            .fil(submissionRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        val inlamningResult = inlamningApi.inlamning(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            InlamningAnrop()
                .undertecknare(submissionRequest.signerPnr)
                .handling(handling)
                .addEpostadresserItem(submissionRequest.notificationEmail)
                .addKvittensepostadresserItem(submissionRequest.notificationEmail)
        )

        return inlamningResult
    }

    private fun getBolagsverketApiUrl(): String =
        "${bolagsverketApiConfig.lamnaInArsredovisningBaseurl()}/${bolagsverketApiConfig.lamnaInArsredovisningVersion()}"

    private fun verifyDocumentAndSigner(authenticatableRequest: AuthenticatableRequest): Boolean {
        val pdfByte = authenticatableRequest.signedPdf
        Loader.loadPDF(pdfByte).use { doc ->
            val verifyDocumentResult = documentHelper.verifyDocument(doc, pdfByte)
            if (!verifyDocumentResult.isSignatureValid) {
                logger.info("Dokumentet har inte en giltig signatur")
                return false
            }
            if (!verifyDocumentResult.isCertificateTrusted) {
                logger.info("Dokumentet är inte signerat med ett betrott certifikat")
                return false
            }

            if (restConfig.verifySigner()) {
                val hasValidSigner =
                    documentHelper.documentHasValidSigner(
                        doc,
                        authenticatableRequest.signerPnr,
                        authenticatableRequest.companyOrgnr
                    )
                if (!hasValidSigner) {
                    return false
                }
            } else {
                logger.warn(
                    "Ingen verifiering kommer att göras för kontrollera att det är en behörig person som har signerat: " +
                            "verifieringen är avaktiverad"
                )
            }

            return true
        }
    }
}
