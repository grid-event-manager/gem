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

$resolvedMsi = (Resolve-Path -LiteralPath $MsiPath).Path
$displayName = "GEM $Version"
$installer = New-Object -ComObject WindowsInstaller.Installer
$database = $installer.OpenDatabase($resolvedMsi, 0)

$properties = @{}
Get-MsiRows $database 'SELECT `Property`, `Value` FROM `Property`' @("Property", "Value") | ForEach-Object {
    $properties[$_.Property] = $_.Value
}

Assert-Equal "ProductName" $displayName $properties["ProductName"]
Assert-Equal "Manufacturer" "ANVLL" $properties["Manufacturer"]
Assert-Equal "ARPPRODUCTICON" "JpARPPRODUCTICON" $properties["ARPPRODUCTICON"]

$icons = Get-MsiRows $database 'SELECT `Name` FROM `Icon`' @("Name")
if (-not ($icons | Where-Object { $_.Name -eq "JpARPPRODUCTICON" })) {
    Fail "brand icon row JpARPPRODUCTICON missing"
}

$shortcuts = Get-MsiRows $database 'SELECT `Shortcut`, `Name`, `Target`, `Arguments`, `Icon_`, `WkDir` FROM `Shortcut`' @("Shortcut", "Name", "Target", "Arguments", "Icon", "WorkingDirectory")
$visibleShortcuts = $shortcuts | Where-Object { $_.Name -eq $displayName }
if ($visibleShortcuts.Count -lt 1) {
    Fail "no visible shortcut named '$displayName'"
}
if ($shortcuts | Where-Object { $_.Name -eq "gem" -or $_.Name -eq "gema" }) {
    Fail "visible shortcut still uses raw technical name"
}
foreach ($shortcut in $visibleShortcuts) {
    Assert-Equal "shortcut target" "[INSTALLDIR]app\resources\gem-windows-launch.vbs" $shortcut.Target
    Assert-Equal "shortcut arguments" "" $shortcut.Arguments
    Assert-Equal "shortcut icon" "JpARPPRODUCTICON" $shortcut.Icon
    Assert-Equal "shortcut working directory" "INSTALLDIR" $shortcut.WorkingDirectory
}

if (Test-Path -LiteralPath $ExtractRoot) {
    Remove-Item -LiteralPath $ExtractRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $ExtractRoot | Out-Null
$extract = Start-Process -FilePath "msiexec.exe" -ArgumentList @("/a", $resolvedMsi, "/qn", "TARGETDIR=$ExtractRoot") -Wait -PassThru
if ($extract.ExitCode -ne 0) {
    Fail "administrative extract exited $($extract.ExitCode)"
}

$installRoot = Join-Path $ExtractRoot "gema"
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

Write-Output "MSI proof passed: $displayName"
