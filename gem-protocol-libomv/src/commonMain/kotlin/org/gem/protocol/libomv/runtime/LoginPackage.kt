package org.gem.protocol.libomv.runtime

import org.gem.core.domain.GemDelay

internal data class LoginPackage(
    val loginUri: String,
    val first: String,
    val last: String,
    val passwd: String,
    val start: String,
    val channel: String,
    val version: String,
    val platform: String,
    val mac: String,
    val id0: String,
    val agreeToTos: String,
    val readCritical: String,
    val lastExecEvent: Int,
    val options: List<String>,
    val timeout: GemDelay,
)
