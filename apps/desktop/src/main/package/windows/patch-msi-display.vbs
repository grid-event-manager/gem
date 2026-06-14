Option Explicit

Const MsiOpenDatabaseModeTransact = 1

If WScript.Arguments.Count <> 9 Then
    WScript.Echo "Usage: patch-msi-display.vbs <msi-path> <display-name> <welcome-title> <launch-after-install-text> <icon-path> <dialog-bmp-path> <banner-bmp-path> <downgrade-message> <running-instance-message>"
    WScript.Quit 64
End If

Dim msiPath
Dim displayName
Dim welcomeTitle
Dim launchAfterInstallText
Dim iconPath
Dim dialogBitmapPath
Dim bannerBitmapPath
Dim downgradeMessage
Dim runningInstanceMessage
Dim welcomeTitleText
msiPath = WScript.Arguments(0)
displayName = WScript.Arguments(1)
welcomeTitle = WScript.Arguments(2)
launchAfterInstallText = WScript.Arguments(3)
iconPath = WScript.Arguments(4)
dialogBitmapPath = WScript.Arguments(5)
bannerBitmapPath = WScript.Arguments(6)
downgradeMessage = WScript.Arguments(7)
runningInstanceMessage = WScript.Arguments(8)
welcomeTitleText = "{\WixUI_Font_Title}" & welcomeTitle

If InStr(displayName, "'") > 0 Then
    WScript.Echo "Display name must not contain a single quote."
    WScript.Quit 65
End If
If InStr(welcomeTitle, "'") > 0 Then
    WScript.Echo "Welcome title must not contain a single quote."
    WScript.Quit 65
End If
If InStr(launchAfterInstallText, "'") > 0 Then
    WScript.Echo "Launch-after-install text must not contain a single quote."
    WScript.Quit 65
End If
If InStr(downgradeMessage, "'") > 0 Then
    WScript.Echo "Downgrade message must not contain a single quote."
    WScript.Quit 65
End If
If InStr(runningInstanceMessage, "'") > 0 Then
    WScript.Echo "Running-instance message must not contain a single quote."
    WScript.Quit 65
End If

Dim fileSystem
Dim scriptDir
Dim runningInstanceCheckPath
Set fileSystem = CreateObject("Scripting.FileSystemObject")
scriptDir = fileSystem.GetParentFolderName(WScript.ScriptFullName)
runningInstanceCheckPath = fileSystem.BuildPath(scriptDir, "gem-running-instance-check.vbs")
If Not fileSystem.FileExists(iconPath) Then
    WScript.Echo "Icon path does not exist: " & iconPath
    WScript.Quit 66
End If
If Not fileSystem.FileExists(dialogBitmapPath) Then
    WScript.Echo "Dialog bitmap path does not exist: " & dialogBitmapPath
    WScript.Quit 67
End If
If Not fileSystem.FileExists(bannerBitmapPath) Then
    WScript.Echo "Banner bitmap path does not exist: " & bannerBitmapPath
    WScript.Quit 68
End If
If Not fileSystem.FileExists(runningInstanceCheckPath) Then
    WScript.Echo "Running-instance check script does not exist: " & runningInstanceCheckPath
    WScript.Quit 69
End If

Dim installer
Dim database
Set installer = CreateObject("WindowsInstaller.Installer")
Set database = installer.OpenDatabase(msiPath, MsiOpenDatabaseModeTransact)

UpsertIcon installer, database, "JpARPPRODUCTICON", iconPath
UpsertBinary installer, database, "WixUI_Bmp_Dialog", dialogBitmapPath
UpsertBinary installer, database, "WixUI_Bmp_Banner", bannerBitmapPath
UpsertBinary installer, database, "GemRunningInstanceCheckScript", runningInstanceCheckPath
ExecuteSql database, "UPDATE `Property` SET `Value`='" & displayName & "' WHERE `Property`='ProductName'"
ExecuteSql database, "UPDATE `Control` SET `Text`='" & welcomeTitleText & "' WHERE `Dialog_`='WelcomeDlg' AND `Control`='Title'"
ExecuteSql database, "UPDATE `Property` SET `Value`='JpARPPRODUCTICON' WHERE `Property`='ARPPRODUCTICON'"
ExecuteSql database, "DELETE FROM `Property` WHERE `Property`='GEM_LAUNCH_AFTER_INSTALL'"
ExecuteSql database, "DELETE FROM `Property` WHERE `Property`='WixShellExecTarget'"
ExecuteSql database, "INSERT INTO `Property` (`Property`, `Value`) VALUES ('WixShellExecTarget', '[INSTALLDIR]app\resources\gem-windows-launch.vbs')"
ExecuteSql database, "UPDATE `Shortcut` SET " & _
    "`Name`='" & displayName & "', " & _
    "`Target`='[INSTALLDIR]app\resources\gem-windows-launch.vbs', " & _
    "`Arguments`='', " & _
    "`Icon_`='JpARPPRODUCTICON', " & _
    "`IconIndex`=0, " & _
    "`WkDir`='INSTALLDIR' " & _
    "WHERE `Name`='gem' OR `Name`='gema' OR `Name`='" & displayName & "'"
ExecuteSql database, "DELETE FROM `CheckBox` WHERE `Property`='GEM_LAUNCH_AFTER_INSTALL'"
ExecuteSql database, "INSERT INTO `CheckBox` (`Property`, `Value`) VALUES ('GEM_LAUNCH_AFTER_INSTALL', '1')"
ExecuteSql database, "DELETE FROM `Control` WHERE `Dialog_`='ShortcutPromptDlg' AND `Control`='LaunchAfterInstall'"
ExecuteSql database, "INSERT INTO `Control` (`Dialog_`, `Control`, `Type`, `X`, `Y`, `Width`, `Height`, `Attributes`, `Property`, `Text`, `Control_Next`) VALUES ('ShortcutPromptDlg', 'LaunchAfterInstall', 'CheckBox', 20, 210, 240, 17, 3, 'GEM_LAUNCH_AFTER_INSTALL', '" & launchAfterInstallText & "', 'Next')"
ExecuteSql database, "UPDATE `Control` SET `Control_Next`='LaunchAfterInstall' WHERE `Dialog_`='ShortcutPromptDlg' AND `Control`='InstallStartMenuShortcut'"
ExecuteSql database, "DELETE FROM `CustomAction` WHERE `Action`='GemLaunchAfterInstall'"
ExecuteSql database, "INSERT INTO `CustomAction` (`Action`, `Type`, `Source`, `Target`) VALUES ('GemLaunchAfterInstall', 65, 'WixCA', 'WixShellExec')"
ExecuteSql database, "DELETE FROM `ControlEvent` WHERE `Dialog_`='ExitDialog' AND `Control_`='Finish' AND `Event`='DoAction' AND `Argument`='GemLaunchAfterInstall'"
ExecuteSql database, "INSERT INTO `ControlEvent` (`Dialog_`, `Control_`, `Event`, `Argument`, `Condition`, `Ordering`) VALUES ('ExitDialog', 'Finish', 'DoAction', 'GemLaunchAfterInstall', 'GEM_LAUNCH_AFTER_INSTALL=""1"" AND NOT Installed', 998)"
UpsertCustomAction database, "GemCheckRunningInstances", 6, "GemRunningInstanceCheckScript", ""
UpsertCustomAction database, "GemDowngradeBlocked", 19, "", downgradeMessage
UpsertCustomAction database, "GemRunningInstanceBlocked", 19, "", runningInstanceMessage
UpsertSequence database, "InstallUISequence", "GemDowngradeBlocked", "JP_DOWNGRADABLE_FOUND", 26
UpsertSequence database, "InstallUISequence", "GemCheckRunningInstances", "NOT REMOVE", 27
UpsertSequence database, "InstallUISequence", "GemRunningInstanceBlocked", "GEM_RUNNING_INSTANCE_FOUND", 28
UpsertSequence database, "InstallExecuteSequence", "GemDowngradeBlocked", "JP_DOWNGRADABLE_FOUND", 26
UpsertSequence database, "InstallExecuteSequence", "GemCheckRunningInstances", "NOT REMOVE", 27
UpsertSequence database, "InstallExecuteSequence", "GemRunningInstanceBlocked", "GEM_RUNNING_INSTANCE_FOUND", 28
database.Commit

Sub ExecuteSql(databaseHandle, sql)
    Dim view
    Set view = databaseHandle.OpenView(sql)
    view.Execute
    view.Close
End Sub

Sub UpsertIcon(installerHandle, databaseHandle, iconName, sourcePath)
    ExecuteSql databaseHandle, "DELETE FROM `Icon` WHERE `Name`='" & iconName & "'"

    Dim record
    Dim view
    Set record = installerHandle.CreateRecord(2)
    record.StringData(1) = iconName
    record.SetStream 2, sourcePath

    Set view = databaseHandle.OpenView("INSERT INTO `Icon` (`Name`, `Data`) VALUES (?, ?)")
    view.Execute record
    view.Close
End Sub

Sub UpsertBinary(installerHandle, databaseHandle, binaryName, sourcePath)
    ExecuteSql databaseHandle, "DELETE FROM `Binary` WHERE `Name`='" & binaryName & "'"

    Dim record
    Dim view
    Set record = installerHandle.CreateRecord(2)
    record.StringData(1) = binaryName
    record.SetStream 2, sourcePath

    Set view = databaseHandle.OpenView("INSERT INTO `Binary` (`Name`, `Data`) VALUES (?, ?)")
    view.Execute record
    view.Close
End Sub

Sub UpsertCustomAction(databaseHandle, actionName, actionType, actionSource, actionTarget)
    ExecuteSql databaseHandle, "DELETE FROM `CustomAction` WHERE `Action`='" & actionName & "'"
    ExecuteSql databaseHandle, "INSERT INTO `CustomAction` (`Action`, `Type`, `Source`, `Target`) VALUES ('" & actionName & "', " & actionType & ", '" & actionSource & "', '" & actionTarget & "')"
End Sub

Sub UpsertSequence(databaseHandle, tableName, actionName, actionCondition, actionSequence)
    ExecuteSql databaseHandle, "DELETE FROM `" & tableName & "` WHERE `Action`='" & actionName & "'"
    ExecuteSql databaseHandle, "INSERT INTO `" & tableName & "` (`Action`, `Condition`, `Sequence`) VALUES ('" & actionName & "', '" & actionCondition & "', " & actionSequence & ")"
End Sub
