@echo OFF

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_JCSC_HOME=%~dp0..

if "%JCSC_HOME%"=="" set JCSC_HOME=%DEFAULT_JCSC_HOME%
set DEFAULT_JCSC_HOME=

set CP_JCSC=%JCSC_HOME%\lib\JCSC.jar;%JCSC_HOME%\lib\gnu-regexp.jar
set CP_JCSC=%CP_JCSC%;%JCSC_HOME%\lib\xercesImpl.jar;%JCSC_HOME%\lib\xml-apis.jar
set CP_JCSC=%CP_JCSC%;%JCSC_HOME%\rules
java -Xmx64m -cp %CP_JCSC% -Djcsc.home=%JCSC_HOME% rj.tools.jcsc.ui.RulesDialog %1 %2 %3 %4 %5 %6 %7 %8 %9
echo ON