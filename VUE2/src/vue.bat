
@echo off
REM Change the following line to set your JDK path
set JAVA_HOME=%JAVA_HOME%
set JAVA=%JAVA_HOME%\bin\java
set JAVAC=%JAVA_HOME%\bin\javac

@echo Create the classpath
set CP=c:\anoop\lib\castor-0.9.4.3.jar;c:\anoop\lib\castor-0.9.4.3-xml.jar;c:\anoop\lib\xercesImpl.jar;c:\anoop\castor\castor-0.9.4.3\examples\SourceGenerator\;c:\anoop\lib\jakarta-regexp-1.2.jar;c:\anoop\dr;c:\anoop\euclid\VUEDevelopment\src;c:\anoop\lib\src\

set CP=%CP%;..\..\..\build\classes;%JDK_BIN%\lib\tools.jar

 
%JAVA% -cp %CP% tufts.vue.VUE