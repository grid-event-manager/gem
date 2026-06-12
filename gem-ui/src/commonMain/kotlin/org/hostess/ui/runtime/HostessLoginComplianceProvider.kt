package org.hostess.ui.runtime

import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.SavedAccountProfile

fun interface HostessLoginComplianceProvider {
    fun requestFor(profile: SavedAccountProfile): LoginComplianceRequest
}
