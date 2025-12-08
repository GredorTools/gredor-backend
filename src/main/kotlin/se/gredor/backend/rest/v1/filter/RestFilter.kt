package se.gredor.backend.rest.v1.filter

import jakarta.annotation.Priority
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.rest.v1.util.createErrorResponse
import kotlin.jvm.optionals.getOrNull

@Provider
@Priority(Priorities.USER)
class RestFilter : ContainerRequestFilter {

    @Inject
    private lateinit var restConfig: Instance<RestConfig>

    @Context
    private lateinit var resourceInfo: ResourceInfo

    override fun filter(requestContext: ContainerRequestContext) {
        val gredorRestResourceAnnotation =
            resourceInfo.resourceClass?.getAnnotation(GredorRestResource::class.java) ?: return

        val closedResources = restConfig.get().closedResources().getOrNull() ?: return
        val closedResourceMessage = gredorRestResourceAnnotation.value.configGetter.invoke(closedResources).getOrNull()

        if (closedResourceMessage != null) { // Om meddelandet är null så är resursen inte stängd
            requestContext.abortWith(
                createErrorResponse(
                    Response.Status.SERVICE_UNAVAILABLE,
                    closedResourceMessage
                )
            )
        }
    }

}
