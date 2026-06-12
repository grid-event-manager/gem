package org.gem.ui.runtime

import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.SavedAccountProfile

fun interface GemLoginComplianceProvider {
    fun requestFor(profile: SavedAccountProfile): LoginComplianceRequest
}
