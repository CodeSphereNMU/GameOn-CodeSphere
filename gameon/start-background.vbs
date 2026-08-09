' ============================================================
' GameOn - Background Starter (Hidden Window)
' This script starts the GameOn server in the background
' without showing a command window.
' ============================================================
Set WshShell = CreateObject("WScript.Shell")
Dim scriptDir
scriptDir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir
WshShell.Run "cmd /c """ & scriptDir & "\run.cmd""", 0, False
Set WshShell = Nothing
