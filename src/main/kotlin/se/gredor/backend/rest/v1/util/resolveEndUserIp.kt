package se.gredor.backend.rest.v1.util

import io.vertx.ext.web.RoutingContext
import se.gredor.backend.rest.RestConfig

/**
 * Returnerar användarens IP-adress.
 */
fun resolveEndUserIp(context: RoutingContext, restConfig: RestConfig): String {
    val endUserIp = if (restConfig.useXRealIp()) {
        context.request().getHeader("X-Real-IP")
    } else {
        context.request().authority().host()
    }

    if (endUserIp == null) {
        throw RuntimeException("Unable to resolve end user's IP-address")
    }

    return when (endUserIp) {
        "localhost" -> {
            "127.0.0.1" // Vid lokalt test
        }

        else -> {
            endUserIp
        }
    }
}
