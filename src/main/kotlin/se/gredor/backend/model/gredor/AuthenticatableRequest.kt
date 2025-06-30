package se.gredor.backend.model.gredor

interface AuthenticatableRequest {
    val companyOrgnr: String
    val signerPnr: String
    val signedPdf: ByteArray
}