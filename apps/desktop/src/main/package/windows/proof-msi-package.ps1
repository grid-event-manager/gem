param(
    [Parameter(Mandatory=$true)]
    [string]$MsiPath,

    [Parameter(Mandatory=$true)]
    [string]$Version,

    [Parameter(Mandatory=$true)]
    [string]$ExtractRoot
)

Set-StrictMode -Version 3.0
$ErrorActionPreference = "Stop"
trap {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}

function Fail([string]$Message) {
    throw "MSI proof failed: $Message"
}

function Assert-Equal([string]$Name, [string]$Expected, [string]$Actual) {
    if ($Actual -ne $Expected) {
        Fail "$Name expected '$Expected' but found '$Actual'"
    }
}

function Assert-File([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Fail "missing file '$Path'"
    }
}

function Get-MsiRows($Database, [string]$Query, [string[]]$Columns) {
    $view = $Database.OpenView($Query)
    [void]$view.Execute()
    $rows = New-Object System.Collections.ArrayList
    while ($record = $view.Fetch()) {
        $row = [ordered]@{}
        for ($index = 0; $index -lt $Columns.Count; $index++) {
            $row[$Columns[$index]] = $record.StringData($index + 1)
        }
        [void]$rows.Add([pscustomobject]$row)
    }
    [void]$view.Close()
    foreach ($row in $rows) {
        Write-Output $row
    }
}

function Get-PackagingText([string]$Key) {
    $cataloguePath = Join-Path $PSScriptRoot "..\packaging-text.properties"
    $prefix = "$Key="
    $line = Get-Content -LiteralPath $cataloguePath | Where-Object { $_.StartsWith($prefix) } | Select-Object -First 1
    if ($null -eq $line) {
        Fail "missing packaging text key '$Key'"
    }
    return $line.Substring($prefix.Length)
}

function Expand-VersionText([string]$Template) {
    return $Template.Replace("{version}", $Version)
}

function Require-Row($Rows, [scriptblock]$Predicate, [string]$Name) {
    $row = $Rows | Where-Object $Predicate | Select-Object -First 1
    if ($null -eq $row) {
        Fail "$Name missing"
    }
    return $row
}

function Assert-SequenceRow($Rows, [string]$TableName, [string]$Action, [string]$Condition, [string]$Sequence) {
    $row = Require-Row $Rows { $_.Action -eq $Action } "$TableName row $Action"
    Assert-Equal "$TableName $Action condition" $Condition $row.Condition
    Assert-Equal "$TableName $Action sequence" $Sequence $row.Sequence
}

$resolvedMsi = (Resolve-Path -LiteralPath $MsiPath).Path
$displayName = Expand-VersionText (Get-PackagingText "windows.displayName")
$shortcutTitle = Get-PackagingText "launcher.displayName"
$welcomeTitle = Expand-VersionText (Get-PackagingText "windows.welcomeTitle")
$welcomeTitleText = "{\WixUI_Font_Title}$welcomeTitle"
$launchAfterInstallText = Get-PackagingText "windows.launchAfterInstall"
$downgradeMessage = Get-PackagingText "windows.downgradeMessage"
$runningInstanceMessage = Get-PackagingText "windows.runningInstanceMessage"
$installer = New-Object -ComObject WindowsInstaller.Installer
$database = $installer.OpenDatabase($resolvedMsi, 0)

$properties = @{}
Get-MsiRows $database 'SELECT `Property`, `Value` FROM `Property`' @("Property", "Value") | ForEach-Object {
    $properties[$_.Property] = $_.Value
}

Assert-Equal "ProductName" $displayName $properties["ProductName"]
Assert-Equal "ProductVersion" $Version $properties["ProductVersion"]
Assert-Equal "Manufacturer" "ANVLL" $properties["Manufacturer"]
Assert-Equal "UpgradeCode" "{1676B6E4-40FB-3524-B5FE-C2AF2836AE7E}" $properties["UpgradeCode"]
Assert-Equal "ARPPRODUCTICON" "JpARPPRODUCTICON" $properties["ARPPRODUCTICON"]
Assert-Equal "WixShellExecTarget" "[INSTALLDIR]app\resources\gem-windows-launch.vbs" $properties["WixShellExecTarget"]
if ($properties.ContainsKey("GEM_LAUNCH_AFTER_INSTALL")) {
    Fail "launch-after-install checkbox must be unchecked by default"
}
if ($properties["ProductName"] -like "*GEMA*") {
    Fail "ProductName contains raw technical label GEMA"
}
if ($properties["Manufacturer"] -like "*GEMA*" -or $properties["Manufacturer"] -like "*Grid Event Manager*") {
    Fail "Manufacturer contains a product label instead of publisher ANVLL"
}

$icons = Get-MsiRows $database 'SELECT `Name` FROM `Icon`' @("Name")
if (-not ($icons | Where-Object { $_.Name -eq "JpARPPRODUCTICON" })) {
    Fail "brand icon row JpARPPRODUCTICON missing"
}

$binaries = Get-MsiRows $database 'SELECT `Name` FROM `Binary`' @("Name")
if (-not ($binaries | Where-Object { $_.Name -eq "WixUI_Bmp_Dialog" })) {
    Fail "installer dialog bitmap row missing"
}
if (-not ($binaries | Where-Object { $_.Name -eq "WixUI_Bmp_Banner" })) {
    Fail "installer banner bitmap row missing"
}
if (-not ($binaries | Where-Object { $_.Name -eq "GemRunningInstanceCheckScript" })) {
    Fail "running-instance custom action script row missing"
}

$shortcuts = Get-MsiRows $database 'SELECT `Shortcut`, `Name`, `Target`, `Arguments`, `Icon_`, `WkDir` FROM `Shortcut`' @("Shortcut", "Name", "Target", "Arguments", "Icon", "WorkingDirectory")
$visibleShortcuts = $shortcuts | Where-Object { $_.Name -eq $shortcutTitle }
if ($visibleShortcuts.Count -lt 1) {
    Fail "no visible shortcut named '$shortcutTitle'"
}
if ($shortcuts | Where-Object { $_.Name -eq "gem" -or $_.Name -eq "gema" -or $_.Name -eq "GEMA" -or $_.Name -eq $displayName }) {
    Fail "visible shortcut still uses raw technical name"
}
foreach ($shortcut in $visibleShortcuts) {
    Assert-Equal "shortcut target" "[INSTALLDIR]app\resources\gem-windows-launch.vbs" $shortcut.Target
    Assert-Equal "shortcut arguments" "" $shortcut.Arguments
    Assert-Equal "shortcut icon" "JpARPPRODUCTICON" $shortcut.Icon
    Assert-Equal "shortcut working directory" "INSTALLDIR" $shortcut.WorkingDirectory
}

$checkboxes = Get-MsiRows $database 'SELECT `Property`, `Value` FROM `CheckBox`' @("Property", "Value")
$launchCheckbox = $checkboxes | Where-Object { $_.Property -eq "GEM_LAUNCH_AFTER_INSTALL" } | Select-Object -First 1
if ($null -eq $launchCheckbox) {
    Fail "launch-after-install checkbox value row missing"
}
Assert-Equal "launch checkbox value" "1" $launchCheckbox.Value

$controls = Get-MsiRows $database 'SELECT `Dialog_`, `Control`, `Type`, `X`, `Y`, `Width`, `Height`, `Attributes`, `Property`, `Text`, `Control_Next` FROM `Control`' @("Dialog", "Control", "Type", "X", "Y", "Width", "Height", "Attributes", "Property", "Text", "Next")
$welcomeTitleControl = $controls | Where-Object { $_.Dialog -eq "WelcomeDlg" -and $_.Control -eq "Title" } | Select-Object -First 1
if ($null -eq $welcomeTitleControl) {
    Fail "welcome title control missing from WelcomeDlg"
}
Assert-Equal "welcome title text" $welcomeTitleText $welcomeTitleControl.Text

$launchControl = $controls | Where-Object { $_.Dialog -eq "ShortcutPromptDlg" -and $_.Control -eq "LaunchAfterInstall" } | Select-Object -First 1
if ($null -eq $launchControl) {
    Fail "launch-after-install control missing from ShortcutPromptDlg"
}
Assert-Equal "launch checkbox type" "CheckBox" $launchControl.Type
Assert-Equal "launch checkbox property" "GEM_LAUNCH_AFTER_INSTALL" $launchControl.Property
Assert-Equal "launch checkbox text" $launchAfterInstallText $launchControl.Text
Assert-Equal "launch checkbox next control" "Next" $launchControl.Next
$startMenuControl = $controls | Where-Object { $_.Dialog -eq "ShortcutPromptDlg" -and $_.Control -eq "InstallStartMenuShortcut" } | Select-Object -First 1
Assert-Equal "start menu checkbox next control" "LaunchAfterInstall" $startMenuControl.Next

$customActions = Get-MsiRows $database 'SELECT `Action`, `Type`, `Source`, `Target` FROM `CustomAction`' @("Action", "Type", "Source", "Target")
$launchAction = Require-Row $customActions { $_.Action -eq "GemLaunchAfterInstall" } "launch-after-install custom action"
Assert-Equal "launch custom action type" "65" $launchAction.Type
Assert-Equal "launch custom action source" "WixCA" $launchAction.Source
Assert-Equal "launch custom action target" "WixShellExec" $launchAction.Target
$runningCheckAction = Require-Row $customActions { $_.Action -eq "GemCheckRunningInstances" } "running-instance check custom action"
Assert-Equal "running check custom action type" "6" $runningCheckAction.Type
Assert-Equal "running check custom action source" "GemRunningInstanceCheckScript" $runningCheckAction.Source
Assert-Equal "running check custom action target" "" $runningCheckAction.Target
$downgradeAction = Require-Row $customActions { $_.Action -eq "GemDowngradeBlocked" } "downgrade block custom action"
Assert-Equal "downgrade block custom action type" "19" $downgradeAction.Type
Assert-Equal "downgrade block custom action source" "" $downgradeAction.Source
Assert-Equal "downgrade block custom action target" $downgradeMessage $downgradeAction.Target
$runningBlockAction = Require-Row $customActions { $_.Action -eq "GemRunningInstanceBlocked" } "running-instance block custom action"
Assert-Equal "running block custom action type" "19" $runningBlockAction.Type
Assert-Equal "running block custom action source" "" $runningBlockAction.Source
Assert-Equal "running block custom action target" $runningInstanceMessage $runningBlockAction.Target

$controlEvents = Get-MsiRows $database 'SELECT `Dialog_`, `Control_`, `Event`, `Argument`, `Condition`, `Ordering` FROM `ControlEvent`' @("Dialog", "Control", "Event", "Argument", "Condition", "Ordering")
$launchEvent = $controlEvents | Where-Object { $_.Dialog -eq "ExitDialog" -and $_.Control -eq "Finish" -and $_.Event -eq "DoAction" -and $_.Argument -eq "GemLaunchAfterInstall" } | Select-Object -First 1
if ($null -eq $launchEvent) {
    Fail "launch-after-install Finish event missing"
}
Assert-Equal "launch Finish condition" "GEM_LAUNCH_AFTER_INSTALL=""1"" AND NOT Installed" $launchEvent.Condition
Assert-Equal "launch Finish ordering" "998" $launchEvent.Ordering

$upgradeRows = Get-MsiRows $database 'SELECT `UpgradeCode`, `VersionMin`, `VersionMax`, `Attributes`, `ActionProperty` FROM `Upgrade`' @("UpgradeCode", "VersionMin", "VersionMax", "Attributes", "ActionProperty")
$upgradable = Require-Row $upgradeRows { $_.ActionProperty -eq "JP_UPGRADABLE_FOUND" } "Upgrade table JP_UPGRADABLE_FOUND"
Assert-Equal "upgradable UpgradeCode" "{1676B6E4-40FB-3524-B5FE-C2AF2836AE7E}" $upgradable.UpgradeCode
$downgradable = Require-Row $upgradeRows { $_.ActionProperty -eq "JP_DOWNGRADABLE_FOUND" } "Upgrade table JP_DOWNGRADABLE_FOUND"
Assert-Equal "downgradable UpgradeCode" "{1676B6E4-40FB-3524-B5FE-C2AF2836AE7E}" $downgradable.UpgradeCode

$installUiRows = Get-MsiRows $database 'SELECT `Action`, `Condition`, `Sequence` FROM `InstallUISequence`' @("Action", "Condition", "Sequence")
Assert-SequenceRow $installUiRows "InstallUISequence" "GemDowngradeBlocked" "JP_DOWNGRADABLE_FOUND" "26"
Assert-SequenceRow $installUiRows "InstallUISequence" "GemCheckRunningInstances" "NOT REMOVE" "27"
Assert-SequenceRow $installUiRows "InstallUISequence" "GemRunningInstanceBlocked" "GEM_RUNNING_INSTANCE_FOUND" "28"

$installExecuteRows = Get-MsiRows $database 'SELECT `Action`, `Condition`, `Sequence` FROM `InstallExecuteSequence`' @("Action", "Condition", "Sequence")
Assert-SequenceRow $installExecuteRows "InstallExecuteSequence" "GemDowngradeBlocked" "JP_DOWNGRADABLE_FOUND" "26"
Assert-SequenceRow $installExecuteRows "InstallExecuteSequence" "GemCheckRunningInstances" "NOT REMOVE" "27"
Assert-SequenceRow $installExecuteRows "InstallExecuteSequence" "GemRunningInstanceBlocked" "GEM_RUNNING_INSTANCE_FOUND" "28"

if (Test-Path -LiteralPath $ExtractRoot) {
    Remove-Item -LiteralPath $ExtractRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $ExtractRoot | Out-Null
$resolvedExtractRoot = (Resolve-Path -LiteralPath $ExtractRoot).Path
$extractLog = Join-Path $resolvedExtractRoot "admin-extract.log"
$extract = Start-Process -FilePath "msiexec.exe" -ArgumentList @("/a", $resolvedMsi, "/qn", "TARGETDIR=$resolvedExtractRoot", "/l*v", $extractLog) -Wait -PassThru
if ($extract.ExitCode -ne 0) {
    if (Test-Path -LiteralPath $extractLog -PathType Leaf) {
        Get-Content -LiteralPath $extractLog -Tail 80 | ForEach-Object { [Console]::Error.WriteLine($_) }
    }
    Fail "administrative extract exited $($extract.ExitCode)"
}

$installRoot = Join-Path $resolvedExtractRoot "gema"
if (-not (Test-Path -LiteralPath $installRoot -PathType Container)) {
    Fail "administrative extract did not create expected gema install root"
}

$javaw = Join-Path $installRoot "runtime\bin\javaw.exe"
$argFile = Join-Path $installRoot "app\resources\gem-windows-launch.args"
$launcherFile = Join-Path $installRoot "app\resources\gem-windows-launch.vbs"
Assert-File $javaw
Assert-File $argFile
Assert-File $launcherFile

$argLines = Get-Content -LiteralPath $argFile
if (-not ($argLines -contains "-Djpackage.app-version=$Version")) {
    Fail "launch argfile does not pin version $Version"
}
if (-not ($argLines -contains "-Dcompose.application.resources.dir=app\resources")) {
    Fail "launch argfile missing Compose resources directory"
}
if (-not ($argLines -contains "-Dskiko.library.path=app")) {
    Fail "launch argfile missing Skiko library path"
}
if (-not ($argLines -contains "app\*")) {
    Fail "launch argfile missing wildcard classpath"
}
if (-not ($argLines -contains "org.gem.apps.desktop.GemDesktopAppKt")) {
    Fail "launch argfile missing desktop main class"
}

$launcherText = Get-Content -LiteralPath $launcherFile -Raw
if ($launcherText -notmatch "runtime\\bin\\javaw\.exe") {
    Fail "launch wrapper missing javaw route"
}
if ($launcherText -notmatch "app\\resources\\gem-windows-launch\.args") {
    Fail "launch wrapper missing argfile route"
}

Write-Output "MSI proof passed: $displayName / shortcut $shortcutTitle"
