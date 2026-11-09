@echo off
chcp 65001 > nul
title BINGO - HOST
cd /d "%~dp0"
set JAVA_EXE="C:\Users\DAM1\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.12.v20260826-1216\jre\bin\java.exe"
%JAVA_EXE% -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin host.HostMain
pause