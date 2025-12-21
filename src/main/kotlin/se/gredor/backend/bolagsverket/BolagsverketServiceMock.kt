package se.gredor.backend.bolagsverket

import io.quarkus.arc.profile.IfBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.Handlingsinfo
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraUtfall
import java.time.LocalDate

@IfBuildProfile("dev")
@ApplicationScoped
class BolagsverketServiceMock : BolagsverketService {

    override fun getRecords(orgnr: String): BolagsverketRecordsResponse {
        return BolagsverketRecordsResponse(
            foretagsnamn = "Mockbolaget AB",
            rakenskapsperioder = listOf(
                Rakenskapsperiod().from(LocalDate.of(2021, 1, 1)).tom(LocalDate.of(2021, 12, 31)),
                Rakenskapsperiod().from(LocalDate.of(2022, 1, 1)).tom(LocalDate.of(2022, 12, 31)),
                Rakenskapsperiod().from(LocalDate.of(2023, 1, 1)).tom(LocalDate.of(2023, 12, 31)),
                Rakenskapsperiod().from(LocalDate.of(2024, 1, 1)).tom(LocalDate.of(2024, 12, 31)),
            ),
            harVerkstallandeDirektor = false,
            harLikvidator = true
        )
    }

    override fun prepareSubmission(personalNumber: String, foretagOrgnr: String): BolagsverketPreparationResponse {
        return BolagsverketPreparationResponse(
            avtalstext = "Mock avtalstext",
            avtalstextAndrad = LocalDate.of(2023, 10, 4)
        )
    }

    override fun validateSubmission(personalNumber: String, foretagOrgnr: String, ixbrl: ByteArray): KontrolleraSvar {
        return KontrolleraSvar()
            .orgnr(foretagOrgnr)
            .addUtfallItem(
                KontrolleraUtfall()
                    .typ(KontrolleraUtfall.TypEnum.WARN)
                    .text("Mock-varning")
                    .kod("1337")
            )
    }

    override fun submitSubmission(
        personalNumber: String,
        foretagOrgnr: String,
        ixbrl: ByteArray,
        aviseringEpost: String
    ): InlamningOK {
        return InlamningOK()
            .orgnr(foretagOrgnr)
            .avsandare(personalNumber)
            .undertecknare(personalNumber)
            .url("http://127.0.0.1")
            .handlingsinfo(
                Handlingsinfo()
                    .typ(Handlingsinfo.TypEnum.ARSREDOVISNING_KOMPLETT)
                    .dokumentlangd(1337)
                    .idnummer("M0CK")
                    .sha256checksumma("hej".toByteArray())
            )
    }
}
