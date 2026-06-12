package org.gem.core.services

data class GemCredentialRuntimeReady(
    val credentialService: CredentialService,
) : GemCredentialRuntimeState
