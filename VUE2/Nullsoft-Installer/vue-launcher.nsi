; Java Launcher
;--------------

Name "VUE"
Caption "Java Launcher"
Icon ".\VUE.ico"
OutFile "VUE-launcher.exe"

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

!define CLASSPATH ".;.\VUE.jar"
!define CLASS "tufts.vue.VUE"

Section ""
  Call GetJRE
  Pop $R0

  ; change for your purpose (-jar etc.)
  StrCpy $0 '"$R0" -Xmx1024m -jar VUE.jar'

  SetOutPath $EXEDIR
  Exec $0
SectionEnd

Function GetJRE
;
;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH

  Push $R0
  Push $R1

  ; use javaw.exe to avoid dosbox.
  ; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"

  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 1) found it locally
  StrCpy $R0 ""

  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfErrors 0 JreFound  ;; 2) found it in JAVA_HOME

  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"

  IfErrors 0 JreFound  ;; 3) found it in the registry
  StrCpy $R0 "${JAVAEXE}"  ;; 4) wishing you good luck

 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd