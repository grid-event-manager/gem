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
    data object ThemePreferenceUnavailable : GemTextKey
    data object ThemePreferenceSaveFailed : GemTextKey
    data object NoAttachmentsAdded : GemTextKey
    data object ClearAttachment : GemTextKey
    data object SendingNotices : GemTextKey
    data object NoticesSent : GemTextKey
    data object SomeNoticesFailed : GemTextKey
    data object SomeNoticesUnconfirmed : GemTextKey
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
            ThemePreferenceUnavailable,
            ThemePreferenceSaveFailed,
            NoAttachmentsAdded,
            ClearAttachment,
            SendingNotices,
            NoticesSent,
            SomeNoticesFailed,
            SomeNoticesUnconfirmed,
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
        GemTextKey.ThemePreferenceUnavailable -> "Theme preference unavailable"
        GemTextKey.ThemePreferenceSaveFailed -> "Theme preference could not be saved"
        GemTextKey.NoAttachmentsAdded -> "No attachments added"
        GemTextKey.ClearAttachment -> "Clear attachment"
        GemTextKey.SendingNotices -> "Sending notices"
        GemTextKey.NoticesSent -> "Notices sent"
        GemTextKey.SomeNoticesFailed -> "Some notices failed"
        GemTextKey.SomeNoticesUnconfirmed -> "Some notices unconfirmed"
        is GemTextKey.SelectedCount -> "${key.count} selected"
    }
}
