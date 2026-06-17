package org.gem.core.appearance

data class AppearanceDraft(
    val mode: AppearanceMode,
    val textFonts: Map<AppearanceTextTarget, AppearanceFontFamily>,
    val textColors: Map<AppearanceTextTarget, AppearanceColor>,
    val elementColors: Map<AppearanceElementTarget, AppearanceColor>,
    val selectedProfileId: AppearanceProfileId?,
    val dirty: Boolean,
) {
    init {
        require(textFonts.keys == AppearanceTextTarget.entries.toSet()) {
            "AppearanceDraft textFonts must cover every text target."
        }
        require(textColors.keys == AppearanceTextTarget.entries.toSet()) {
            "AppearanceDraft textColors must cover every text target."
        }
        require(elementColors.keys == AppearanceElementTarget.entries.toSet()) {
            "AppearanceDraft elementColors must cover every element target."
        }
    }

    companion object {
        fun fromProfile(profile: AppearanceProfile): AppearanceDraft =
            AppearanceDraft(
                mode = profile.mode,
                textFonts = profile.textFonts,
                textColors = profile.textColors,
                elementColors = profile.elementColors,
                selectedProfileId = profile.id,
                dirty = false,
            )
    }
}
