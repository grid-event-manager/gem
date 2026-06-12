package org.hostess.core.ports

import org.hostess.core.domain.AccountProfileId

fun interface AccountProfileIdSource {
    fun nextProfileId(): AccountProfileId
}
