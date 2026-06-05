package org.hostess.core.domain

sealed interface LoginComplianceDecision {
    val receipt: LoginComplianceReceipt

    data class Allowed(override val receipt: LoginComplianceReceipt) : LoginComplianceDecision

    data class Denied(override val receipt: LoginComplianceReceipt) : LoginComplianceDecision
}
