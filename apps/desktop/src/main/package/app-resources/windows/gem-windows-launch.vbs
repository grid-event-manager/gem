Option Explicit

Dim fileSystem
Dim resourcesDir
Dim appDir
Dim installDir
Dim javawPath
Dim shell

Set fileSystem = CreateObject("Scripting.FileSystemObject")
resourcesDir = fileSystem.GetParentFolderName(WScript.ScriptFullName)
appDir = fileSystem.GetParentFolderName(resourcesDir)
installDir = fileSystem.GetParentFolderName(appDir)
javawPath = fileSystem.BuildPath(installDir, "runtime\bin\javaw.exe")

Set shell = CreateObject("WScript.Shell")
shell.CurrentDirectory = installDir
shell.Run """" & javawPath & """ @app\resources\gem-windows-launch.args", 0, False
