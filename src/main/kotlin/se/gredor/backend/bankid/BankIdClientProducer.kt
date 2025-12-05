package se.gredor.backend.bankid

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
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
        var trusedRoot: Resource
        if (bankIdConfig.testMode()) {
            webServiceUrl = WebClientFactoryBean.TEST_WEB_SERVICE_URL
            trusedRoot = WebClientFactoryBean.TEST_ROOT_CERTIFICATE.get()
        } else {
            webServiceUrl = WebClientFactoryBean.PRODUCTION_WEB_SERVICE_URL
            trusedRoot = WebClientFactoryBean.PRODUCTION_ROOT_CERTIFICATE.get()
        }

        val webClientFactory = WebClientFactoryBean(webServiceUrl, trusedRoot, credential)
        webClientFactory.afterPropertiesSet()
        return webClientFactory.createInstance()
    }
}
