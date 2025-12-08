package se.gredor.backend.rest.v1.config

import java.util.*

enum class PerResourceString(
    val configGetter: (RestConfig.PerResourceStrings) -> Optional<String>
) {
    AUTH(RestConfig.PerResourceStrings::auth),
    BANK_ID(RestConfig.PerResourceStrings::bankId),
    INFORMATION(RestConfig.PerResourceStrings::information),
    MESSAGE(RestConfig.PerResourceStrings::message),
    PING(RestConfig.PerResourceStrings::ping),
    SUBMISSION_FLOW(RestConfig.PerResourceStrings::submissionFlow)
}
