import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.ext.Provider
import org.jboss.logmanager.MDC
import java.util.*

@Provider
@PreMatching
class MdcRequestFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext?) {
        MDC.put("request.id", UUID.randomUUID().toString())
    }

    companion object {
        fun getRequestId(): String = MDC.get("request.id")
    }
}
