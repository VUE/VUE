#!/bin/sh

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
macosx=false;

case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) macosx=true ;;
esac

if [ -z "$JCSC_HOME" ] ; then
  # try to find JCSC
  if [ -d /opt/jcsc ] ; then 
    JCSC_HOME=/opt/jcsc
  fi

  if [ -d "${HOME}/opt/jcsc" ] ; then 
    JCSC_HOME="${HOME}/opt/jcsc"
  fi

  ## resolve links - $0 may be a link to ant's home
  PRG="$0"
  progname=`basename "$0"`
  saveddir=`pwd`

  # need this for relative symlinks
  cd `dirname "$PRG"`
  
  JCSC_HOME=`dirname "$PRG"`/..

  cd "$saveddir"

  # make it fully qualified
  JCSC_HOME=`cd "$JCSC_HOME" && pwd`
fi

CP_JCSC="$JCSC_HOME/lib/JCSC.jar"
CP_GNU_REGEXP="$JCSC_HOME/lib/gnu-regexp.jar"
CP_XML_IMPL="$JCSC_HOME/lib/xercesImpl.jar"
CP_XML_API="$JCSC_HOME/lib/xml-apis.jar"
CP_RULES="$JCSC_HOME/rules"

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  #JCSC_HOME=`cygpath --unix "$JCSC_HOME"`
  CP_JCSC=`cygpath --path --unix "$CP_JCSC"`
  CP_GNU_REGEXP=`cygpath --path --unix "$CP_GNU_REGEXP"`
  CP_XML_IMPL=`cygpath --path --unix "$CP_XML_IMPL"`
  CP_XML_API=`cygpath --path --unix "$CP_XML_API"`
  CP_RULES=`cygpath --path --unix "$CP_RULES"`
fi

# Now put them together
CLASSPATH=$CP_JCSC:$CP_GNU_REGEXP:$CP_XML_IMPL:$CP_XML_API:$CP_LOG4J:$CP_ASPECTJ:$CP_RULES

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then  
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

# Mac OS X special settings
if $macosx; then
  MAC_TITLE=-Xdock:name="JCSC"
  MAC_MENUBAR=-Dcom.apple.macos.useScreenMenuBar=true
  MAC_RESIZE=-Dcom.apple.mrj.application.live-resize=true
  MAC_TABSIZE=-Dcom.apple.macos.smallTabs=true
  MAC_INTRUDE=-Dcom.apple.mrj.application.growbox.intrudes=true
  ALL_MAC="$MAC_TITLE $MAC_MENUBAR $MAC_RESIZE $MAC_TABSIZE $MAC_INTRUDE"
fi

# now bring it up
#echo $CLASSPATH
#echo $JCSC_HOME

java -Xmx64m -cp "$CLASSPATH" $ALL_MAC -Djcsc.home="$JCSC_HOME" rj.tools.jcsc.ui.RulesDialog "$@"