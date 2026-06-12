package org.gem.protocol.libomv.mapping

import org.gem.core.domain.CoreFailure

internal data class LoginAppearanceState(
    val agentAppearanceService: Boolean?,
    val cofVersion: Int?,
) {
    companion object {
        fun empty(): LoginAppearanceState = LoginAppearanceState(
            agentAppearanceService = null,
            cofVersion = null,
        )
    }
}

internal sealed interface LoginAppearanceStateResult {
    data class Success(val appearanceState: LoginAppearanceState) : LoginAppearanceStateResult
    data class Failure(val failure: CoreFailure) : LoginAppearanceStateResult
}
