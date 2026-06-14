Option Explicit

Dim processMarkers
Dim service
Dim processes
Dim process
Dim found

processMarkers = Array( _
    "org.gem.apps.desktop.GemDesktopAppKt", _
    "gem-windows-launch.args", _
    "\Program Files\gema\", _
    "\gema.exe", _
    "org.hostess.apps.desktop.HostessDesktopAppKt", _
    "\Program Files\Hostess\", _
    "\hostess.exe" _
)

found = False
On Error Resume Next
Session.Property("GEM_RUNNING_INSTANCE_FOUND") = ""
Set service = GetObject("winmgmts:\\.\root\cimv2")
If Err.Number = 0 Then
    Set processes = service.ExecQuery("SELECT CommandLine, ExecutablePath FROM Win32_Process")
    If Err.Number = 0 Then
        For Each process In processes
            If HasGemMarker(process.CommandLine) Or HasGemMarker(process.ExecutablePath) Then
                found = True
                Exit For
            End If
        Next
    End If
End If
On Error GoTo 0

If found Then
    Session.Property("GEM_RUNNING_INSTANCE_FOUND") = "1"
End If

Function HasGemMarker(candidate)
    Dim marker
    HasGemMarker = False
    If IsNull(candidate) Or IsEmpty(candidate) Then
        Exit Function
    End If
    For Each marker In processMarkers
        If InStr(1, CStr(candidate), marker, vbTextCompare) > 0 Then
            HasGemMarker = True
            Exit Function
        End If
    Next
End Function
