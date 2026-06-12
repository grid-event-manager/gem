package org.gem.core.ports

import org.gem.core.domain.AccountProfileId

fun interface AccountProfileIdSource {
    fun nextProfileId(): AccountProfileId
}
