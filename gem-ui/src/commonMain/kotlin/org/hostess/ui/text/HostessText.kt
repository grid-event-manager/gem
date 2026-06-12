package org.hostess.ui.text

sealed interface HostessTextKey {
    data object AppName : HostessTextKey
    data object BrandInitials : HostessTextKey
    data object BrandSubtitle : HostessTextKey
    data object Username : HostessTextKey
    data object Password : HostessTextKey
    data object Show : HostessTextKey
    data object Hide : HostessTextKey
    data object SavedLoginPlaceholder : HostessTextKey
    data object Login : HostessTextKey
    data object SecondLifeTimePrefix : HostessTextKey
    data object MeridiemAm : HostessTextKey
    data object MeridiemPm : HostessTextKey
    data object Menu : HostessTextKey
    data object LogOut : HostessTextKey
    data object AddNewLogin : HostessTextKey
    data object SaveAndLogin : HostessTextKey
    data object Settings : HostessTextKey
    data object EditAccount : HostessTextKey
    data object SavePassword : HostessTextKey
    data object AddNewAccount : HostessTextKey
    data object SaveNewAccount : HostessTextKey
    data object DeleteAccount : HostessTextKey
    data object Delete : HostessTextKey
    data object DeleteConfirmation : HostessTextKey
    data object Ok : HostessTextKey
    data object Cancel : HostessTextKey
    data object Back : HostessTextKey
    data object Subject : HostessTextKey
    data object Body : HostessTextKey
    data class DraftCharCount(val count: Int) : HostessTextKey
    data object Inventory : HostessTextKey
    data object Landmarks : HostessTextKey
    data object Textures : HostessTextKey
    data object None : HostessTextKey
    data object Groups : HostessTextKey
    data object AddAll : HostessTextKey
    data object AddGroups : HostessTextKey
    data object CanSendNotices : HostessTextKey
    data object SendNotices : HostessTextKey
    data object Ready : HostessTextKey
    data object MissingSubject : HostessTextKey
    data object MissingBody : HostessTextKey
    data object MissingGroups : HostessTextKey
    data object SavingLogin : HostessTextKey
    data object SendingLoginDetails : HostessTextKey
    data object LoggingIn : HostessTextKey
    data object LoggingOut : HostessTextKey
    data object RezzingWorld : HostessTextKey
    data object LoadingAvatar : HostessTextKey
    data object PreparingAvatar : HostessTextKey
    data object LoadingGroups : HostessTextKey
    data object LoadingInventory : HostessTextKey
    data object InventoryEmpty : HostessTextKey
    data object InventoryUnavailable : HostessTextKey
    data object GroupsEmpty : HostessTextKey
    data object GroupsUnavailable : HostessTextKey
    data object LoginFailed : HostessTextKey
    data object RemovingFailedLogin : HostessTextKey
    data object Online : HostessTextKey
    data object Offline : HostessTextKey
    data object BlankStatus : HostessTextKey
    data object Light : HostessTextKey
    data object Dark : HostessTextKey
    data object ThemePreferenceUnavailable : HostessTextKey
    data object ThemePreferenceSaveFailed : HostessTextKey
    data object NoAttachmentsAdded : HostessTextKey
    data object ClearAttachment : HostessTextKey
    data object SendingNotices : HostessTextKey
    data object NoticesSent : HostessTextKey
    data object SomeNoticesFailed : HostessTextKey
    data object SomeNoticesUnconfirmed : HostessTextKey
    data class SelectedCount(val count: Int) : HostessTextKey

    companion object {
        val fixedKeys: Set<HostessTextKey> = setOf(
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
            AddNewLogin,
            SaveAndLogin,
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
            CanSendNotices,
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

interface HostessTextCatalogue {
    fun text(key: HostessTextKey): String
}

object EnglishHostessTextCatalogue : HostessTextCatalogue {
    override fun text(key: HostessTextKey): String = when (key) {
        HostessTextKey.AppName -> "Grid Event Manager"
        HostessTextKey.BrandInitials -> "GEM"
        HostessTextKey.BrandSubtitle -> "GRID EVENT MANAGER"
        HostessTextKey.Username -> "User Name"
        HostessTextKey.Password -> "Password"
        HostessTextKey.Show -> "Show"
        HostessTextKey.Hide -> "Hide"
        HostessTextKey.SavedLoginPlaceholder -> "\u2014 select \u2014"
        HostessTextKey.Login -> "Login"
        HostessTextKey.SecondLifeTimePrefix -> "SLT"
        HostessTextKey.MeridiemAm -> "AM"
        HostessTextKey.MeridiemPm -> "PM"
        HostessTextKey.Menu -> "Menu"
        HostessTextKey.LogOut -> "Log out"
        HostessTextKey.AddNewLogin -> "Add new login..."
        HostessTextKey.SaveAndLogin -> "Save and login"
        HostessTextKey.Settings -> "Settings"
        HostessTextKey.EditAccount -> "Edit account"
        HostessTextKey.SavePassword -> "Save password"
        HostessTextKey.AddNewAccount -> "Add new account"
        HostessTextKey.SaveNewAccount -> "Save new account"
        HostessTextKey.DeleteAccount -> "Delete account"
        HostessTextKey.Delete -> "Delete"
        HostessTextKey.DeleteConfirmation -> "Are you sure you want to delete these accounts?"
        HostessTextKey.Ok -> "OK"
        HostessTextKey.Cancel -> "Cancel"
        HostessTextKey.Back -> "BACK"
        HostessTextKey.Subject -> "Subject"
        HostessTextKey.Body -> "Body"
        is HostessTextKey.DraftCharCount -> "${key.count} chars"
        HostessTextKey.Inventory -> "Inventory"
        HostessTextKey.Landmarks -> "Landmarks"
        HostessTextKey.Textures -> "Textures"
        HostessTextKey.None -> "None"
        HostessTextKey.Groups -> "Groups"
        HostessTextKey.AddAll -> "Add all"
        HostessTextKey.AddGroups -> "Select..."
        HostessTextKey.CanSendNotices -> "Can send notices"
        HostessTextKey.SendNotices -> "Send notices"
        HostessTextKey.Ready -> "Ready"
        HostessTextKey.MissingSubject -> "Subject required"
        HostessTextKey.MissingBody -> "Body required"
        HostessTextKey.MissingGroups -> "Select groups"
        HostessTextKey.SavingLogin -> "Saving login"
        HostessTextKey.SendingLoginDetails -> "Sending login details"
        HostessTextKey.LoggingIn -> "Logging in"
        HostessTextKey.LoggingOut -> "Logging out"
        HostessTextKey.RezzingWorld -> "Rezzing world"
        HostessTextKey.LoadingAvatar -> "Loading avatar"
        HostessTextKey.PreparingAvatar -> "Preparing avatar"
        HostessTextKey.LoadingGroups -> "Loading groups"
        HostessTextKey.LoadingInventory -> "Loading inventory folders"
        HostessTextKey.InventoryEmpty -> "No inventory"
        HostessTextKey.InventoryUnavailable -> "Inventory unavailable"
        HostessTextKey.GroupsEmpty -> "No groups"
        HostessTextKey.GroupsUnavailable -> "Groups unavailable"
        HostessTextKey.LoginFailed -> "Login failed"
        HostessTextKey.RemovingFailedLogin -> "Removing failed login"
        HostessTextKey.Online -> "Online"
        HostessTextKey.Offline -> "Offline"
        HostessTextKey.BlankStatus -> ""
        HostessTextKey.Light -> "Light"
        HostessTextKey.Dark -> "Dark"
        HostessTextKey.ThemePreferenceUnavailable -> "Theme preference unavailable"
        HostessTextKey.ThemePreferenceSaveFailed -> "Theme preference could not be saved"
        HostessTextKey.NoAttachmentsAdded -> "No attachments added"
        HostessTextKey.ClearAttachment -> "Clear attachment"
        HostessTextKey.SendingNotices -> "Sending notices"
        HostessTextKey.NoticesSent -> "Notices sent"
        HostessTextKey.SomeNoticesFailed -> "Some notices failed"
        HostessTextKey.SomeNoticesUnconfirmed -> "Some notices unconfirmed"
        is HostessTextKey.SelectedCount -> "${key.count} selected"
    }
}
