package se.gredor.backend.rest.v1

import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
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
        if (!verifySigner(preparationRequest)) {
            throw Exception("Invalid signer or document")
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
    fun validate(@Valid submissionRequest: SubmissionRequest): KontrolleraSvar {
        if (!verifySigner(submissionRequest)) {
            throw Exception("Invalid signer or document")
        }

        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(submissionRequest.signerPnr)
                .orgnr(submissionRequest.companyOrgnr)
        )
        logger.info(skapaTokenResult.toString())

        val handling = Handling()
            .fil(submissionRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        val kontrolleraResult = kontrollApi.kontrollera(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            KontrolleraAnrop()
                .handling(handling)
        )
        logger.info(kontrolleraResult.toString())

        return kontrolleraResult
    }

    @POST
    @Path("submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submit(@Valid submissionRequest: SubmissionRequest): InlamningOK {
        if (!verifySigner(submissionRequest)) {
            throw Exception("Invalid signer or document")
        }

        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(submissionRequest.signerPnr)
                .orgnr(submissionRequest.companyOrgnr)
        )
        logger.info(skapaTokenResult.toString())

        val handling = Handling()
            .fil(submissionRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        // TODO: E-postadresser
        val inlamningResult = inlamningApi.inlamning(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            InlamningAnrop()
                .undertecknare(submissionRequest.signerPnr)
                .handling(handling)
        )
        logger.info(inlamningResult.toString())

        return inlamningResult
    }

    private fun getBolagsverketApiUrl(): String =
        "${bolagsverketApiConfig.lamnaInArsredovisningBaseurl()}/${bolagsverketApiConfig.lamnaInArsredovisningVersion()}"

    private fun verifySigner(authenticatableRequest: AuthenticatableRequest): Boolean {
        if (!restConfig.verifySigner()) {
            logger.warn("Signer verification is disabled")
            return true
        }

        val pdfByte = authenticatableRequest.signedPdf
        val doc = Loader.loadPDF(pdfByte)

        val (isDocumentSignatureValid, isCertificateTrusted) = documentHelper.verifyDocument(doc, pdfByte)
        if (!isDocumentSignatureValid || !isCertificateTrusted) {
            return false
        }

        val hasValidSigner =
            documentHelper.documentHasValidSigner(
                doc,
                authenticatableRequest.signerPnr,
                authenticatableRequest.companyOrgnr
            )
        return hasValidSigner
    }
}
