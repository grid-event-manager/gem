Option Explicit

Const MsiOpenDatabaseModeTransact = 1

If WScript.Arguments.Count <> 2 Then
    WScript.Echo "Usage: patch-msi-display.vbs <msi-path> <display-name>"
    WScript.Quit 64
End If

Dim msiPath
Dim displayName
msiPath = WScript.Arguments(0)
displayName = WScript.Arguments(1)

If InStr(displayName, "'") > 0 Then
    WScript.Echo "Display name must not contain a single quote."
    WScript.Quit 65
End If

Dim installer
Dim database
Set installer = CreateObject("WindowsInstaller.Installer")
Set database = installer.OpenDatabase(msiPath, MsiOpenDatabaseModeTransact)

ExecuteSql database, "UPDATE `Shortcut` SET `Name`='" & displayName & "' WHERE `Name`='gem'"
ExecuteSql database, "UPDATE `Shortcut` SET `Name`='" & displayName & "' WHERE `Name`='gema'"
ExecuteSql database, "UPDATE `Property` SET `Value`='" & displayName & "' WHERE `Property`='ProductName'"
database.Commit

Sub ExecuteSql(databaseHandle, sql)
    Dim view
    Set view = databaseHandle.OpenView(sql)
    view.Execute
    view.Close
End Sub
