echo off
del .\output\*.xml /Q
del .\output\*.html /Q
del .\output\*.png /Q

set CLASSPATH="%~dp0\lib\itpa-keywords-1.0.jar;%CLASSPATH%"

echo %CLASSPATH%

ride.py .\Test_CA_AGILECENTRAL
