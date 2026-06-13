Option Explicit

Const MsiOpenDatabaseModeTransact = 1

If WScript.Arguments.Count <> 3 Then
    WScript.Echo "Usage: patch-msi-display.vbs <msi-path> <display-name> <icon-path>"
    WScript.Quit 64
End If

Dim msiPath
Dim displayName
Dim iconPath
msiPath = WScript.Arguments(0)
displayName = WScript.Arguments(1)
iconPath = WScript.Arguments(2)

If InStr(displayName, "'") > 0 Then
    WScript.Echo "Display name must not contain a single quote."
    WScript.Quit 65
End If

Dim fileSystem
Set fileSystem = CreateObject("Scripting.FileSystemObject")
If Not fileSystem.FileExists(iconPath) Then
    WScript.Echo "Icon path does not exist: " & iconPath
    WScript.Quit 66
End If

Dim installer
Dim database
Set installer = CreateObject("WindowsInstaller.Installer")
Set database = installer.OpenDatabase(msiPath, MsiOpenDatabaseModeTransact)

UpsertIcon installer, database, "JpARPPRODUCTICON", iconPath
ExecuteSql database, "UPDATE `Property` SET `Value`='" & displayName & "' WHERE `Property`='ProductName'"
ExecuteSql database, "UPDATE `Property` SET `Value`='JpARPPRODUCTICON' WHERE `Property`='ARPPRODUCTICON'"
ExecuteSql database, "UPDATE `Shortcut` SET " & _
    "`Name`='" & displayName & "', " & _
    "`Target`='[INSTALLDIR]app\resources\gem-windows-launch.vbs', " & _
    "`Arguments`='', " & _
    "`Icon_`='JpARPPRODUCTICON', " & _
    "`IconIndex`=0, " & _
    "`WkDir`='INSTALLDIR' " & _
    "WHERE `Name`='gem' OR `Name`='gema' OR `Name`='" & displayName & "'"
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
