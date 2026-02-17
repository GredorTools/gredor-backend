package se.gredor.backend.bankid

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.web.reactive.function.client.WebClient
import se.swedenconnect.bankid.rpapi.service.BankIDClient
import se.swedenconnect.bankid.rpapi.service.impl.BankIDClientImpl
import se.swedenconnect.bankid.rpapi.service.impl.ZxingQRGenerator
import se.swedenconnect.bankid.rpapi.support.WebClientFactoryBean
import se.swedenconnect.security.credential.KeyStoreCredential
import java.io.FileInputStream
import java.security.KeyStore

class BankIdClientProducer {
    @Inject
    private lateinit var bankIdConfig: BankIdConfig

    @Produces
    @ApplicationScoped
    fun produceBankIdClient(): BankIDClient {
        val webClient = bankIdWebClientFactory()

        return BankIDClientImpl(
            bankIdConfig.clientIdentifier(),
            webClient,
            ZxingQRGenerator()
        )
    }

    private fun bankIdWebClientFactory(): WebClient {
        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(this.bankIdConfig.certPath()).use { keyStoreData ->
            keyStore.load(
                keyStoreData,
                this.bankIdConfig.certPassword().toCharArray()
            )
        }
        val credential = KeyStoreCredential(
            keyStore, "1", this.bankIdConfig.certPassword().toCharArray(), null
        )

        var webServiceUrl: String
        var trustedRoot: Resource
        if (bankIdConfig.testMode()) {
            webServiceUrl = WebClientFactoryBean.TEST_WEB_SERVICE_URL
            trustedRoot = WebClientFactoryBean.TEST_ROOT_CERTIFICATE.get()
        } else {
            webServiceUrl = WebClientFactoryBean.PRODUCTION_WEB_SERVICE_URL
            trustedRoot = WebClientFactoryBean.PRODUCTION_ROOT_CERTIFICATE.get()
        }
        trustedRoot = getFixedTrustedRootResource(trustedRoot)

        val webClientFactory = WebClientFactoryBean(webServiceUrl, trustedRoot, credential)
        webClientFactory.afterPropertiesSet()
        return webClientFactory.createInstance()
    }

    /**
     * Tar bort alla avslutande blanksteg på första raden i PEM-filen. Detta
     * pga att Java 25 inte tillåter avslutande blanksteg på första raden.
     * Möjligtvis en bugg, se https://bugs.openjdk.org/browse/JDK-8377975
     */
    private fun getFixedTrustedRootResource(originalTrustedRoot: Resource): InputStreamResource {
        val fixedContent = originalTrustedRoot.inputStream.bufferedReader().use { reader ->
            reader.readLines().joinToString(separator = "\n", postfix = "\n") { line -> line.trimEnd() }
        }
        return InputStreamResource(fixedContent.byteInputStream())
    }
}
