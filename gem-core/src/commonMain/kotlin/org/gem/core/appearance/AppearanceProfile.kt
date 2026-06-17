package org.gem.core.appearance

data class AppearanceProfile(
    val id: AppearanceProfileId,
    val name: AppearanceProfileName,
    val mode: AppearanceMode,
    val source: AppearanceProfileSource,
    val textFonts: Map<AppearanceTextTarget, AppearanceFontFamily>,
    val textColors: Map<AppearanceTextTarget, AppearanceColor>,
    val elementColors: Map<AppearanceElementTarget, AppearanceColor>,
) {
    init {
        require(textFonts.keys == AppearanceTextTarget.entries.toSet()) {
            "AppearanceProfile textFonts must cover every text target."
        }
        require(textColors.keys == AppearanceTextTarget.entries.toSet()) {
            "AppearanceProfile textColors must cover every text target."
        }
        require(elementColors.keys == AppearanceElementTarget.entries.toSet()) {
            "AppearanceProfile elementColors must cover every element target."
        }
    }
}
