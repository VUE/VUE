
@echo off
REM Change the following two lines to set your JDK path and vue home path

<<<<<<< vue.bat
set JAVA_HOME=C:\forte_jdk\j2sdk1.4.0
set VUE_HOME=C:\Daisuke\VUEDevelopment\src
=======
set JAVA_HOME=C:\j2sdk1.4.0_03
set VUE_HOME=C:\anoop\euclid\VUEDevelopment\src
>>>>>>> 1.4

set JAVA=%JAVA_HOME%\bin\java
set JAVAC=%JAVA_HOME%\bin\javac

@echo Create the classpath
set CP=%VUE_HOME%;%VUE_HOME%\..\lib\castor-0.9.4.3.jar;%VUE_HOME%\..\lib\castor-0.9.4.3-xml.jar;%VUE_HOME%\..\lib\batik-svggen.jar;%VUE_HOME%\..\lib\batik-awt-util.jar;%VUE_HOME%\tufts\dr;%VUE_HOME%\..\lib\batik-util.jar;%VUE_HOME%\..\lib\fop.jar;%VUE_HOME%\..\lib\osid.jar
 
%JAVA% -cp %CP% tufts.vue.VUE