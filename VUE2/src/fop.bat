@ECHO OFF

rem %~dp0 is the expanded pathname of the current script under NT
rem set LOCAL_FOP_HOME=
rem if "%OS%"=="Windows_NT" set LOCAL_FOP_HOME=%~dp0

set LIBDIR=..\lib
set LOCALCLASSPATH=%LIBDIR%\fop.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xml-apis.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xercesImpl-2.2.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xalan-2.4.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\batik.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\avalon-framework-cvs-20020806.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jimi-1.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jai_core.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jai_codec.jar
ECHO LOCALCLASSPATH
java -cp %LOCALCLASSPATH% org.apache.fop.apps.Fop %1 %2 %3 %4 %5 %6