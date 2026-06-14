package org.gem.protocol.libomv

import android.content.Context
import java.nio.file.Path
import org.gem.protocol.libomv.runtime.AndroidGemProtocolIdentityProviders
import org.gem.protocol.libomv.runtime.DefaultLibomvPlatformAdapterBundle
import org.gem.protocol.libomv.runtime.FileInventorySnapshotCacheStore
import org.gem.protocol.libomv.runtime.LoginSecretResolver

fun ProtocolLibomvModule.liveRuntimeForAndroid(
    context: Context,
    secretResolver: LoginSecretResolver,
    inventorySnapshotCacheDirectory: Path,
): LibomvProtocolRuntime {
    val identityProviders = AndroidGemProtocolIdentityProviders.create(context)
    return liveRuntime(
        bundle = DefaultLibomvPlatformAdapterBundle.create(
            secretResolver = secretResolver,
            viewerIdentityProvider = identityProviders.viewerIdentityProvider,
            machineIdentityProvider = identityProviders.machineIdentityProvider,
        ),
        inventorySnapshotCacheStore = FileInventorySnapshotCacheStore(inventorySnapshotCacheDirectory),
    )
}
