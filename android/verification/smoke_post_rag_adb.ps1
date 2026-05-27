# Post-RAG Sarathi smoke (PowerShell 5.1+). Run from repo root optional.
# Saves screenshots under verification/screenshots/post_rag/ and logcat to verification/post_rag_logcat.txt
$ErrorActionPreference = "Stop"
$adb = "C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.sarathi.app"
$shotDir = Join-Path $PSScriptRoot "screenshots\post_rag"
$repoRoot = $PSScriptRoot | Split-Path -Parent | Split-Path -Parent
$logOut = Join-Path $PSScriptRoot "post_rag_logcat.txt"

function Save-Png([string]$path) {
    $null = cmd /c "`"$adb`" exec-out screencap -p > `"$path`""
    if (-not (Test-Path $path) -or ((Get-Item $path).Length -lt 100)) {
        throw "screencap failed or too small: $path"
    }
}

function Get-UiDump {
    & $adb shell uiautomator dump /sdcard/window_dump.xml 2>$null | Out-Null
    $raw = & $adb exec-out cat /sdcard/window_dump.xml
    if ($raw -is [byte[]]) {
        $utf8 = New-Object System.Text.UTF8Encoding $false
        return $utf8.GetString($raw)
    }
    return [string]$raw
}

function Tap-Node {
    param([string]$Xml, [scriptblock]$Predicate)
    foreach ($m in [regex]::Matches($Xml, "<node[^>]*>")) {
        $n = $m.Value
        if (-not (& $Predicate $n)) { continue }
        if ($n -match 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
            $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]; $x2 = [int]$Matches[3]; $y2 = [int]$Matches[4]
            $x = [int](($x1 + $x2) / 2); $y = [int](($y1 + $y2) / 2)
            & $adb shell input tap $x $y
            return $true
        }
    }
    return $false
}

function Tap-TextExact([string]$text) {
    $xml = Get-UiDump
    $escaped = [regex]::Escape($text)
    return Tap-Node $xml { param($n) $n -match "text=`"$escaped`"" }
}

function Tap-TextContains([string]$sub) {
    $xml = Get-UiDump
    return Tap-Node $xml {
        param($n)
        if ($n -notmatch 'text="([^"]*)"') { return $false }
        $t = [System.Net.WebUtility]::HtmlDecode($Matches[1])
        return $t -like "*$sub*"
    }
}

function Tap-ContentDesc([string]$desc) {
    $xml = Get-UiDump
    $e = [regex]::Escape($desc)
    return Tap-Node $xml { param($n) $n -match "content-desc=`"$e`"" }
}

function Wait-UiContains([string]$sub, [int]$timeoutSec = 25) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $xml = Get-UiDump
        if ($xml -like "*$sub*") { return $true }
        Start-Sleep -Milliseconds 400
    }
    return $false
}

function Focus-ChatInput {
    $xml3 = Get-UiDump
    if ($xml3 -match "class=`"android.widget.EditText`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"") {
        $ex1 = [int]$Matches[1]; $ey1 = [int]$Matches[2]; $ex2 = [int]$Matches[3]; $ey2 = [int]$Matches[4]
        & $adb shell input tap ([int](($ex1 + $ex2) / 2)) ([int](($ey1 + $ey2) / 2))
    } else {
        & $adb shell input tap 400 2500
    }
    Start-Sleep -Milliseconds 350
}

function Send-ChatLine([string]$textAdb) {
    Focus-ChatInput
    & $adb shell input text $textAdb
    Start-Sleep -Milliseconds 500
    if (-not (Tap-ContentDesc "Send")) { throw "Send button not found" }
    Start-Sleep -Seconds 4
}

New-Item -ItemType Directory -Force -Path $shotDir | Out-Null

Write-Host "== pm clear + launch =="
& $adb shell pm clear $pkg | Out-Host
& $adb logcat -c | Out-Null
& $adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 3
Save-Png (Join-Path $shotDir "01_splash.png")

if (-not (Wait-UiContains "Welcome, dear one")) { throw "Splash text not found" }
if (-not (Tap-TextExact "Begin")) { throw "Begin not tappable" }
Start-Sleep -Seconds 1

Write-Host "== empty name validation =="
if (-not (Tap-TextContains "Offer my name")) { throw "Offer my name not found" }
Start-Sleep -Milliseconds 600
$xml = Get-UiDump
if ($xml -notlike "*Please enter your name*") { throw "Expected validation text after empty name" }

Write-Host "== enter Pruthvi =="
if (-not (Tap-TextContains "Your name")) { Tap-TextContains "devotee" | Out-Null }
Start-Sleep -Milliseconds 300
$xml2 = Get-UiDump
if ($xml2 -match "class=`"android.widget.EditText`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"") {
    $ex1 = [int]$Matches[1]; $ey1 = [int]$Matches[2]; $ex2 = [int]$Matches[3]; $ey2 = [int]$Matches[4]
    & $adb shell input tap ([int](($ex1 + $ex2) / 2)) ([int](($ey1 + $ey2) / 2))
} else {
    & $adb shell input tap 640 1200
}
Start-Sleep -Milliseconds 400
& $adb shell input text "Pruthvi"
Start-Sleep -Milliseconds 400
if (-not (Tap-TextContains "Offer my name")) { throw "Offer my name (2) not found" }
Start-Sleep -Seconds 1

Write-Host "== tone Poetic or Gentle =="
foreach ($tone in @("Gentle", "Direct", "Poetic", "Scriptural")) {
    if (-not (Tap-TextExact $tone)) { Write-Warning "Tone $tone tap missed" }
    Start-Sleep -Milliseconds 350
}
if (-not (Tap-TextExact "Poetic")) { Tap-TextContains "Poetic" | Out-Null }
Start-Sleep -Milliseconds 300
if (-not (Tap-TextExact "Continue")) { throw "Continue not found" }
Start-Sleep -Seconds 1

Write-Host "== blessing -> chat =="
if (-not (Wait-UiContains "Enter the chariot")) { throw "Blessing CTA missing" }
if (-not (Tap-TextContains "Enter the chariot")) { throw "Enter the chariot not found" }
Start-Sleep -Seconds 2

Write-Host "== RAG chat prompts (mock-friendly) =="
Send-ChatLine "I%sworked%shard%sbut%sfailed.%sWhat%sdoes%sthe%sGita%ssay?"
Save-Png (Join-Path $shotDir "02_chat_rag_failed_gita.png")
Send-ChatLine "I%sam%safraid%sof%sthe%sfuture."
Send-ChatLine "Tell%sme%sabout%sdharma."

Write-Host "== verse =="
if (-not (Tap-ContentDesc "Menu")) { throw "Menu not found" }
Start-Sleep -Milliseconds 600
if (-not (Tap-TextExact "Verse of the Day")) { throw "Verse nav not found" }
Start-Sleep -Seconds 1
Save-Png (Join-Path $shotDir "03_verse_rag.png")
if (-not (Tap-TextContains "Reflect")) { Write-Warning "Reflect not tapped" }
Start-Sleep -Seconds 2

Write-Host "== when I feel =="
if (-not (Tap-ContentDesc "Menu")) { throw "Menu (2) not found" }
Start-Sleep -Milliseconds 500
if (-not (Tap-TextExact "When I Feel")) { throw "When I Feel nav not found" }
Start-Sleep -Seconds 1
Save-Png (Join-Path $shotDir "04_feel_rag.png")
if (-not (Tap-TextExact "Afraid")) { throw "Afraid emotion not found" }
Start-Sleep -Milliseconds 600
if (-not (Tap-TextContains "Continue with this feeling")) { throw "Feel continue CTA missing" }
Start-Sleep -Seconds 2

Write-Host "== my dharma =="
if (-not (Tap-ContentDesc "Menu")) { throw "Menu (3) not found" }
Start-Sleep -Milliseconds 500
if (-not (Tap-TextExact "My Dharma")) { throw "My Dharma nav not found" }
Start-Sleep -Seconds 1
Save-Png (Join-Path $shotDir "05_dharma_rag.png")
$xml4 = Get-UiDump
if ($xml4 -match "class=`"android.widget.EditText`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"") {
    $ex1 = [int]$Matches[1]; $ey1 = [int]$Matches[2]; $ex2 = [int]$Matches[3]; $ey2 = [int]$Matches[4]
    & $adb shell input tap ([int](($ex1 + $ex2) / 2)) ([int](($ey1 + $ey2) / 2))
} else {
    & $adb shell input tap 640 1400
}
Start-Sleep -Milliseconds 400
& $adb shell input text "I%sam%spostponing%san%simportant%sproject."
Start-Sleep -Milliseconds 500
if (-not (Tap-TextContains "Save privately")) { throw "Save privately not found" }
Start-Sleep -Seconds 1
if (-not (Tap-TextContains "Reflect with Krishna")) { Write-Warning "Reflect with Krishna not tapped" }
Start-Sleep -Seconds 2

Write-Host "== settings =="
if (-not (Tap-ContentDesc "Menu")) { throw "Menu (4) not found" }
Start-Sleep -Milliseconds 500
if (-not (Tap-TextExact "Settings")) { throw "Settings not found" }
Start-Sleep -Seconds 1
Save-Png (Join-Path $shotDir "06_settings_rag.png")

Write-Host "== logcat =="
& $adb logcat -d > $logOut

Write-Host "POST_RAG_SMOKE_DONE repoRoot=$repoRoot log=$logOut shots=$shotDir"
