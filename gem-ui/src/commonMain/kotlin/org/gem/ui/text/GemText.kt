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
        )
    }
}

interface GemTextCatalogue {
    fun text(key: GemTextKey): String
}

object EnglishGemTextCatalogue : GemTextCatalogue {
    override fun text(key: GemTextKey): String = when (key) {
        GemTextKey.AppName -> "Grid Event Manager"
        GemTextKey.BrandInitials -> "GEM"
        GemTextKey.BrandSubtitle -> "GRID EVENT MANAGER"
        GemTextKey.Username -> "User Name"
        GemTextKey.Password -> "Password"
        GemTextKey.Show -> "Show"
        GemTextKey.Hide -> "Hide"
        GemTextKey.SavedLoginPlaceholder -> "\u2014 select \u2014"
        GemTextKey.Login -> "Login"
        GemTextKey.SecondLifeTimePrefix -> "SLT"
        GemTextKey.MeridiemAm -> "AM"
        GemTextKey.MeridiemPm -> "PM"
        GemTextKey.Menu -> "Menu"
        GemTextKey.LogOut -> "Log out"
        GemTextKey.Exit -> "Exit"
        GemTextKey.AddNewLogin -> "Add new login..."
        GemTextKey.SaveAndLogin -> "Save and login"
        GemTextKey.Accounts -> "Accounts"
        GemTextKey.Settings -> "Settings"
        GemTextKey.EditAccount -> "Edit account"
        GemTextKey.SavePassword -> "Save password"
        GemTextKey.AddNewAccount -> "Add new account"
        GemTextKey.SaveNewAccount -> "Save new account"
        GemTextKey.DeleteAccount -> "Delete account"
        GemTextKey.Delete -> "Delete"
        GemTextKey.DeleteConfirmation -> "Are you sure you want to delete these accounts?"
        GemTextKey.Ok -> "OK"
        GemTextKey.Cancel -> "Cancel"
        GemTextKey.Back -> "BACK"
        GemTextKey.Subject -> "Subject"
        GemTextKey.Body -> "Body"
        is GemTextKey.DraftCharCount -> "${key.count} chars"
        GemTextKey.Inventory -> "Inventory"
        GemTextKey.Landmarks -> "Landmarks"
        GemTextKey.Textures -> "Textures"
        GemTextKey.None -> "None"
        GemTextKey.Groups -> "Groups"
        GemTextKey.AddAll -> "Add all"
        GemTextKey.AddGroups -> "Select..."
        GemTextKey.SendNotices -> "Send notices"
        GemTextKey.Ready -> "Ready"
        GemTextKey.MissingSubject -> "Subject required"
        GemTextKey.MissingBody -> "Body required"
        GemTextKey.MissingGroups -> "Select groups"
        GemTextKey.SavingLogin -> "Saving login"
        GemTextKey.SendingLoginDetails -> "Sending login details"
        GemTextKey.LoggingIn -> "Logging in"
        GemTextKey.LoggingOut -> "Logging out"
        GemTextKey.RezzingWorld -> "Rezzing world"
        GemTextKey.LoadingAvatar -> "Loading avatar"
        GemTextKey.PreparingAvatar -> "Preparing avatar"
        GemTextKey.LoadingGroups -> "Loading groups"
        GemTextKey.LoadingInventory -> "Loading inventory folders"
        GemTextKey.InventoryEmpty -> "No inventory"
        GemTextKey.InventoryUnavailable -> "Inventory unavailable"
        GemTextKey.GroupsEmpty -> "No groups"
        GemTextKey.GroupsUnavailable -> "Groups unavailable"
        GemTextKey.LoginFailed -> "Login failed"
        GemTextKey.RemovingFailedLogin -> "Removing failed login"
        GemTextKey.Online -> "Online"
        GemTextKey.Offline -> "Offline"
        GemTextKey.BlankStatus -> ""
        GemTextKey.Light -> "Light"
        GemTextKey.Dark -> "Dark"
        GemTextKey.GemDefault -> "GEM Default"
        GemTextKey.ThemePreferenceUnavailable -> "Theme preference unavailable"
        GemTextKey.ThemePreferenceSaveFailed -> "Theme preference could not be saved"
        GemTextKey.Customise -> "Customise"
        GemTextKey.Themes -> "Themes"
        GemTextKey.SaveTheme -> "Save theme"
        GemTextKey.ResetToDefault -> "Reset to default"
        GemTextKey.EnterNewThemeName -> "Enter a new theme name"
        GemTextKey.Save -> "Save"
        GemTextKey.ChooseTheme -> "\u2014 choose theme \u2014"
        GemTextKey.Text -> "Text"
        GemTextKey.Fonts -> "Fonts"
        GemTextKey.Element -> "Element"
        GemTextKey.AppearanceTextTitleBar -> "Title bar"
        GemTextKey.AppearanceTextTitleSubtitle -> "Title subtitle"
        GemTextKey.AppearanceTextLogo -> "Logo"
        GemTextKey.AppearanceTextSectionHeadings -> "Section headings"
        GemTextKey.AppearanceTextMainBody -> "Main body"
        GemTextKey.AppearanceTextFieldText -> "Field text"
        GemTextKey.AppearanceTextSmallLabels -> "Small labels"
        GemTextKey.AppearanceTextButtonLabels -> "Button labels"
        GemTextKey.AppearanceTextMenuLabels -> "Menu labels"
        GemTextKey.AppearanceTextSltClock -> "SLT clock"
        GemTextKey.AppearanceTextBackButton -> "Back button"
        GemTextKey.AppearanceTextThemeToggleLabels -> "Theme toggle labels"
        GemTextKey.AppearanceElementPageBackground -> "Page background"
        GemTextKey.AppearanceElementCardBackground -> "Card background"
        GemTextKey.AppearanceElementPanelBackground -> "Panel background"
        GemTextKey.AppearanceElementFieldBackground -> "Field background"
        GemTextKey.AppearanceElementFieldControlBorders -> "Field/control borders"
        GemTextKey.AppearanceElementTitleBar -> "Title bar"
        GemTextKey.AppearanceElementTitleButton -> "Title button"
        GemTextKey.AppearanceElementTitleButtonBorder -> "Title button border"
        GemTextKey.AppearanceElementHamburgerBackground -> "Hamburger background"
        GemTextKey.AppearanceElementHamburgerBorder -> "Hamburger border"
        GemTextKey.AppearanceElementHamburgerBars -> "Hamburger bars"
        GemTextKey.AppearanceElementThemeToggleTrack -> "Theme toggle track"
        GemTextKey.AppearanceElementThemeToggleSelectedTrack -> "Theme toggle selected track"
        GemTextKey.AppearanceElementThemeToggleKnob -> "Theme toggle knob"
        GemTextKey.AppearanceElementAccentText -> "Accent text"
        GemTextKey.AppearanceElementErrorText -> "Error text"
        GemTextKey.AppearanceElementStatusText -> "Status text"
        GemTextKey.AppearanceElementMenuDisabledText -> "Menu disabled text"
        GemTextKey.AppearanceElementInteractiveHoverText -> "Interactive hover text"
        GemTextKey.AppearanceElementPrimaryButtonFill -> "Primary button fill"
        GemTextKey.AppearanceElementSelectedItemFill -> "Selected item fill"
        GemTextKey.AppearanceElementMenuBackground -> "Menu background"
        GemTextKey.AppearanceElementMenuHover -> "Menu hover"
        GemTextKey.AppearanceElementStatusPill -> "Status pill"
        GemTextKey.AppearanceElementRulesAndSeparators -> "Rules and separators"
        GemTextKey.NoAttachmentsAdded -> "No attachments added"
        GemTextKey.ClearAttachment -> "Clear attachment"
        GemTextKey.SendingNotices -> "Sending notices"
        GemTextKey.NoticesSent -> "Notices sent"
        GemTextKey.SomeNoticesFailed -> "Some notices failed"
        is GemTextKey.SelectedCount -> "${key.count} selected"
    }
}
