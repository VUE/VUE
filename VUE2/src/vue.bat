
@echo off
REM Change the following two lines to set your JDK path and vue home path

set JAVA_HOME=C:\j2sdk1.4.0_03
set VUE_HOME=C:\vueproject\VUEDevelopment\src

set JAVA=%JAVA_HOME%\bin\java
set JAVAC=%JAVA_HOME%\bin\javac

@echo Create the classpath
set CP=%VUE_HOME%;%VUE_HOME%\..\lib\castor-0.9.4.3.jar;%VUE_HOME%\..\lib\castor-0.9.4.3-xml.jar;%VUE_HOME%\..\lib\batik-svggen.jar;%VUE_HOME%\..\lib\xercesImpl.jar;%VUE_HOME%\..\lib\jakarta-regexp-1.2.jar;%VUE_HOME%\..\lib\batik-awt-util.jar;%VUE_HOME%\..\lib\batik-ext.jar;%VUE_HOME%\..\lib\batik-util.jar;%VUE_HOME%\tufts\dr;
 
%JAVA% -cp %CP% tufts.vue.VUE