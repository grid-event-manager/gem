package org.hostess.ui.text

sealed interface HostessTextKey {
    data object AppName : HostessTextKey
    data object Username : HostessTextKey
    data object Password : HostessTextKey
    data object Show : HostessTextKey
    data object Hide : HostessTextKey
    data object SavedLoginPlaceholder : HostessTextKey
    data object Login : HostessTextKey
    data object Menu : HostessTextKey
    data object LogOut : HostessTextKey
    data object AddNewLogin : HostessTextKey
    data object SaveAndLogin : HostessTextKey
    data object Settings : HostessTextKey
    data object AddNewAccount : HostessTextKey
    data object SaveNewAccount : HostessTextKey
    data object DeleteAccount : HostessTextKey
    data object Delete : HostessTextKey
    data object DeleteConfirmation : HostessTextKey
    data object Ok : HostessTextKey
    data object Cancel : HostessTextKey
    data object Back : HostessTextKey
    data object Notice : HostessTextKey
    data object Subject : HostessTextKey
    data object Body : HostessTextKey
    data class DraftCharCount(val count: Int) : HostessTextKey
    data object Inventory : HostessTextKey
    data object Folder : HostessTextKey
    data object Landmarks : HostessTextKey
    data object Textures : HostessTextKey
    data object Selected : HostessTextKey
    data object Select : HostessTextKey
    data object Open : HostessTextKey
    data object None : HostessTextKey
    data object Landmark : HostessTextKey
    data object Texture : HostessTextKey
    data object Groups : HostessTextKey
    data object AddAll : HostessTextKey
    data object AddGroups : HostessTextKey
    data object CanSendNotices : HostessTextKey
    data object SendNotices : HostessTextKey
    data object Ready : HostessTextKey
    data object Sending : HostessTextKey
    data object SavingLogin : HostessTextKey
    data object LoggingIn : HostessTextKey
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
    data class SelectedCount(val count: Int) : HostessTextKey

    companion object {
        val fixedKeys: Set<HostessTextKey> = setOf(
            AppName,
            Username,
            Password,
            Show,
            Hide,
            SavedLoginPlaceholder,
            Login,
            Menu,
            LogOut,
            AddNewLogin,
            SaveAndLogin,
            Settings,
            AddNewAccount,
            SaveNewAccount,
            DeleteAccount,
            Delete,
            DeleteConfirmation,
            Ok,
            Cancel,
            Back,
            Notice,
            Subject,
            Body,
            Inventory,
            Folder,
            Landmarks,
            Textures,
            Selected,
            Select,
            Open,
            None,
            Landmark,
            Texture,
            Groups,
            AddAll,
            AddGroups,
            CanSendNotices,
            SendNotices,
            Ready,
            Sending,
            SavingLogin,
            LoggingIn,
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
        )
    }
}

interface HostessTextCatalogue {
    fun text(key: HostessTextKey): String
}

object EnglishHostessTextCatalogue : HostessTextCatalogue {
    override fun text(key: HostessTextKey): String = when (key) {
        HostessTextKey.AppName -> "Hostess"
        HostessTextKey.Username -> "User Name"
        HostessTextKey.Password -> "Password"
        HostessTextKey.Show -> "Show"
        HostessTextKey.Hide -> "Hide"
        HostessTextKey.SavedLoginPlaceholder -> "\u2014 select \u2014"
        HostessTextKey.Login -> "Login"
        HostessTextKey.Menu -> "Menu"
        HostessTextKey.LogOut -> "Log out"
        HostessTextKey.AddNewLogin -> "Add new login..."
        HostessTextKey.SaveAndLogin -> "Save and login"
        HostessTextKey.Settings -> "Settings"
        HostessTextKey.AddNewAccount -> "Add new account"
        HostessTextKey.SaveNewAccount -> "Save new account"
        HostessTextKey.DeleteAccount -> "Delete account"
        HostessTextKey.Delete -> "Delete"
        HostessTextKey.DeleteConfirmation -> "Are you sure you want to delete these accounts?"
        HostessTextKey.Ok -> "OK"
        HostessTextKey.Cancel -> "Cancel"
        HostessTextKey.Back -> "BACK"
        HostessTextKey.Notice -> "Notice"
        HostessTextKey.Subject -> "Subject"
        HostessTextKey.Body -> "Body"
        is HostessTextKey.DraftCharCount -> "${key.count} chars"
        HostessTextKey.Inventory -> "Inventory"
        HostessTextKey.Folder -> "Folder"
        HostessTextKey.Landmarks -> "Landmarks"
        HostessTextKey.Textures -> "Textures"
        HostessTextKey.Selected -> "Selected"
        HostessTextKey.Select -> "Select"
        HostessTextKey.Open -> "Open"
        HostessTextKey.None -> "None"
        HostessTextKey.Landmark -> "Landmark"
        HostessTextKey.Texture -> "Texture"
        HostessTextKey.Groups -> "Groups"
        HostessTextKey.AddAll -> "Add all"
        HostessTextKey.AddGroups -> "Add Groups"
        HostessTextKey.CanSendNotices -> "Can send notices"
        HostessTextKey.SendNotices -> "Send notices"
        HostessTextKey.Ready -> "Ready"
        HostessTextKey.Sending -> "Sending"
        HostessTextKey.SavingLogin -> "Saving login"
        HostessTextKey.LoggingIn -> "Logging in"
        HostessTextKey.PreparingAvatar -> "Preparing avatar"
        HostessTextKey.LoadingGroups -> "Loading groups"
        HostessTextKey.LoadingInventory -> "Loading inventory"
        HostessTextKey.InventoryEmpty -> "No inventory"
        HostessTextKey.InventoryUnavailable -> "Inventory unavailable"
        HostessTextKey.GroupsEmpty -> "No groups"
        HostessTextKey.GroupsUnavailable -> "Groups unavailable"
        HostessTextKey.LoginFailed -> "Login failed"
        HostessTextKey.RemovingFailedLogin -> "Removing failed login"
        HostessTextKey.Online -> "Online"
        HostessTextKey.Offline -> "Offline"
        HostessTextKey.BlankStatus -> ""
        is HostessTextKey.SelectedCount -> "${key.count} selected"
    }
}
