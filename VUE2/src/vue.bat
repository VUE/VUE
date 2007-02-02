@echo off
REM Change the following two lines to set your JDK path and vue home path
set JAVA_HOME=C:\Progra~1\j2sdk_nb\j2sdk1.4.2


REM values being set 
@echo setting the parameters
set VUE_HOME=C:\Vue\VUE2\src\
set VUE_LIB=%VUE_HOME%\..\lib\
set JAVA=%JAVA_HOME%\bin\java
set JAVAC=%JAVA_HOME%\bin\javac

REM setting classpath
@echo Create the classpath
set CP=%VUE_HOME%;%VUE_LIB%castor-0.9.4.3.jar;%VUE_LIB%castor-0.9.4.3-xml.jar;%VUE_LIB%batik-svggen.jar;%VUE_LIB%batik-awt-util.jar;%VUE_LIB%batik-util.jar;%VUE_LIB%fop.jar;%VUE_LIB%okiSID_rc6_1.jar;%VUE_LIB%fedorautilities.jar;%VUE_LIB%fedoragentypes.jar;%VUE_LIB%fedora-client.jar;%VUE_LIB%fedora-server.jar;%VUE_LIB%axis.jar;%VUE_LIB%axis-ant.jar;%VUE_LIB%commons-discovery.jar;%VUE_LIB%commons-logging.jar;%VUE_LIB%jaxrpc.jar;%VUE_LIB%log4j-1.2.4.jar;%VUE_LIB%saaj.jar;%VUE_LIB%xerces-2_4_0/xercesImpl.jar;%VUE_LIB%xerces-2_4_0/xml-apis.jar;

REM set CP=%VUE_HOME%;"VUE_LIB%vue-lib.zip;
@echo running VUE
%JAVA%  -cp  %CP% tufts.vue.VUE