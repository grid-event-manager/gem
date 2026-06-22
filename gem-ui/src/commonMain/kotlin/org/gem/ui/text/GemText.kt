package org.gem.ui.text

sealed interface GemTextKey {
    data object AppName : GemTextKey
    data object BrandInitials : GemTextKey
    data object BrandSubtitle : GemTextKey
    data object Username : GemTextKey
    data object Password : GemTextKey
    data object Show : GemTextKey
    data object Hide : GemTextKey
    data object SavedLoginPlaceholder : GemTextKey
    data object Login : GemTextKey
    data object SecondLifeTimePrefix : GemTextKey
    data object MeridiemAm : GemTextKey
    data object MeridiemPm : GemTextKey
    data object Menu : GemTextKey
    data object LogOut : GemTextKey
    data object Exit : GemTextKey
    data object AddNewLogin : GemTextKey
    data object SaveAndLogin : GemTextKey
    data object Accounts : GemTextKey
    data object Settings : GemTextKey
    data object About : GemTextKey
    data object AboutProductLine : GemTextKey
    data object AboutCopyright : GemTextKey
    data object AboutLicense : GemTextKey
    data object AboutHelpSupport : GemTextKey
    data object Language : GemTextKey
    data object ChooseLanguage : GemTextKey
    data object SystemLanguage : GemTextKey
    data object LanguagePreferenceUnavailable : GemTextKey
    data object LanguagePreferenceSaveFailed : GemTextKey
    data object EditAccount : GemTextKey
    data object SavePassword : GemTextKey
    data object AddNewAccount : GemTextKey
    data object SaveNewAccount : GemTextKey
    data object DeleteAccount : GemTextKey
    data object Delete : GemTextKey
    data object DeleteConfirmation : GemTextKey
    data object Ok : GemTextKey
    data object Cancel : GemTextKey
    data object Back : GemTextKey
    data object Subject : GemTextKey
    data object Body : GemTextKey
    data class DraftCharCount(val count: Int) : GemTextKey
    data object Inventory : GemTextKey
    data object Landmarks : GemTextKey
    data object Textures : GemTextKey
    data object None : GemTextKey
    data object Groups : GemTextKey
    data object AddAll : GemTextKey
    data object AddGroups : GemTextKey
    data object SendNotices : GemTextKey
    data object Ready : GemTextKey
    data object MissingSubject : GemTextKey
    data object MissingBody : GemTextKey
    data object MissingGroups : GemTextKey
    data object SavingLogin : GemTextKey
    data object SendingLoginDetails : GemTextKey
    data object LoggingIn : GemTextKey
    data object LoggingOut : GemTextKey
    data object RezzingWorld : GemTextKey
    data object LoadingAvatar : GemTextKey
    data object PreparingAvatar : GemTextKey
    data object LoadingGroups : GemTextKey
    data object LoadingInventory : GemTextKey
    data object InventoryEmpty : GemTextKey
    data object InventoryUnavailable : GemTextKey
    data object GroupsEmpty : GemTextKey
    data object GroupsUnavailable : GemTextKey
    data object LoginFailed : GemTextKey
    data object RemovingFailedLogin : GemTextKey
    data object Online : GemTextKey
    data object Offline : GemTextKey
    data object BlankStatus : GemTextKey
    data object Light : GemTextKey
    data object Dark : GemTextKey
    data object GemDefault : GemTextKey
    data object ThemePreferenceUnavailable : GemTextKey
    data object ThemePreferenceSaveFailed : GemTextKey
    data object Customise : GemTextKey
    data object Themes : GemTextKey
    data object SaveTheme : GemTextKey
    data object ResetToDefault : GemTextKey
    data object EnterNewThemeName : GemTextKey
    data object Save : GemTextKey
    data object ChooseTheme : GemTextKey
    data object Text : GemTextKey
    data object Fonts : GemTextKey
    data object Element : GemTextKey
    data object AppearanceTextTitleBar : GemTextKey
    data object AppearanceTextTitleSubtitle : GemTextKey
    data object AppearanceTextLogo : GemTextKey
    data object AppearanceTextSectionHeadings : GemTextKey
    data object AppearanceTextMainBody : GemTextKey
    data object AppearanceTextFieldText : GemTextKey
    data object AppearanceTextSmallLabels : GemTextKey
    data object AppearanceTextButtonLabels : GemTextKey
    data object AppearanceTextMenuLabels : GemTextKey
    data object AppearanceTextSltClock : GemTextKey
    data object AppearanceTextBackButton : GemTextKey
    data object AppearanceTextThemeToggleLabels : GemTextKey
    data object AppearanceElementPageBackground : GemTextKey
    data object AppearanceElementCardBackground : GemTextKey
    data object AppearanceElementPanelBackground : GemTextKey
    data object AppearanceElementFieldBackground : GemTextKey
    data object AppearanceElementFieldControlBorders : GemTextKey
    data object AppearanceElementTitleBar : GemTextKey
    data object AppearanceElementTitleButton : GemTextKey
    data object AppearanceElementTitleButtonBorder : GemTextKey
    data object AppearanceElementHamburgerBackground : GemTextKey
    data object AppearanceElementHamburgerBorder : GemTextKey
    data object AppearanceElementHamburgerBars : GemTextKey
    data object AppearanceElementThemeToggleTrack : GemTextKey
    data object AppearanceElementThemeToggleSelectedTrack : GemTextKey
    data object AppearanceElementThemeToggleKnob : GemTextKey
    data object AppearanceElementAccentText : GemTextKey
    data object AppearanceElementErrorText : GemTextKey
    data object AppearanceElementStatusText : GemTextKey
    data object AppearanceElementMenuDisabledText : GemTextKey
    data object AppearanceElementInteractiveHoverText : GemTextKey
    data object AppearanceElementPrimaryButtonFill : GemTextKey
    data object AppearanceElementSelectedItemFill : GemTextKey
    data object AppearanceElementMenuBackground : GemTextKey
    data object AppearanceElementMenuHover : GemTextKey
    data object AppearanceElementStatusPill : GemTextKey
    data object AppearanceElementRulesAndSeparators : GemTextKey
    data object NoAttachmentsAdded : GemTextKey
    data object ClearAttachment : GemTextKey
    data object SendingNotices : GemTextKey
    data object NoticesSent : GemTextKey
    data object SomeNoticesFailed : GemTextKey
    data object SendFailureCannotSendNotices : GemTextKey
    data object SendFailureRejected : GemTextKey
    data object SendFailureSenderUnavailable : GemTextKey
    data object SendFailureRequestInvalid : GemTextKey
    data object SendFailureSessionNotReady : GemTextKey
    data class SendFailureDetailLine(val groupName: String, val reason: String) : GemTextKey
    data class SelectedCount(val count: Int) : GemTextKey

    companion object {
        val fixedKeys: Set<GemTextKey> = setOf(
            AppName,
            BrandInitials,
            BrandSubtitle,
            Username,
            Password,
            Show,
            Hide,
            SavedLoginPlaceholder,
            Login,
            SecondLifeTimePrefix,
            MeridiemAm,
            MeridiemPm,
            Menu,
            LogOut,
            Exit,
            AddNewLogin,
            SaveAndLogin,
            Accounts,
            Settings,
            About,
            AboutProductLine,
            AboutCopyright,
            AboutLicense,
            AboutHelpSupport,
            Language,
            ChooseLanguage,
            SystemLanguage,
            LanguagePreferenceUnavailable,
            LanguagePreferenceSaveFailed,
            EditAccount,
            SavePassword,
            AddNewAccount,
            SaveNewAccount,
            DeleteAccount,
            Delete,
            DeleteConfirmation,
            Ok,
            Cancel,
            Back,
            Subject,
            Body,
            Inventory,
            Landmarks,
            Textures,
            None,
            Groups,
            AddAll,
            AddGroups,
            SendNotices,
            Ready,
            MissingSubject,
            MissingBody,
            MissingGroups,
            SavingLogin,
            SendingLoginDetails,
            LoggingIn,
            LoggingOut,
            RezzingWorld,
            LoadingAvatar,
            PreparingAvatar,
            LoadingGroups,
            LoadingInventory,
            InventoryEmpty,
            InventoryUnavailable,
            GroupsEmpty,
            GroupsUnavailable,
            LoginFailed,
            RemovingFailedLogin,
            Online,
            Offline,
            BlankStatus,
            Light,
            Dark,
            GemDefault,
            ThemePreferenceUnavailable,
            ThemePreferenceSaveFailed,
            Customise,
            Themes,
            SaveTheme,
            ResetToDefault,
            EnterNewThemeName,
            Save,
            ChooseTheme,
            Text,
            Fonts,
            Element,
            AppearanceTextTitleBar,
            AppearanceTextTitleSubtitle,
            AppearanceTextLogo,
            AppearanceTextSectionHeadings,
            AppearanceTextMainBody,
            AppearanceTextFieldText,
            AppearanceTextSmallLabels,
            AppearanceTextButtonLabels,
            AppearanceTextMenuLabels,
            AppearanceTextSltClock,
            AppearanceTextBackButton,
            AppearanceTextThemeToggleLabels,
            AppearanceElementPageBackground,
            AppearanceElementCardBackground,
            AppearanceElementPanelBackground,
            AppearanceElementFieldBackground,
            AppearanceElementFieldControlBorders,
            AppearanceElementTitleBar,
            AppearanceElementTitleButton,
            AppearanceElementTitleButtonBorder,
            AppearanceElementHamburgerBackground,
            AppearanceElementHamburgerBorder,
            AppearanceElementHamburgerBars,
            AppearanceElementThemeToggleTrack,
            AppearanceElementThemeToggleSelectedTrack,
            AppearanceElementThemeToggleKnob,
            AppearanceElementAccentText,
            AppearanceElementErrorText,
            AppearanceElementStatusText,
            AppearanceElementMenuDisabledText,
            AppearanceElementInteractiveHoverText,
            AppearanceElementPrimaryButtonFill,
            AppearanceElementSelectedItemFill,
            AppearanceElementMenuBackground,
            AppearanceElementMenuHover,
            AppearanceElementStatusPill,
            AppearanceElementRulesAndSeparators,
            NoAttachmentsAdded,
            ClearAttachment,
            SendingNotices,
            NoticesSent,
            SomeNoticesFailed,
            SendFailureCannotSendNotices,
            SendFailureRejected,
            SendFailureSenderUnavailable,
            SendFailureRequestInvalid,
            SendFailureSessionNotReady,
        )
    }
}

interface GemTextCatalogue {
    fun text(key: GemTextKey): String
}
