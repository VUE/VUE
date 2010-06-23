                              README

                Java(TM) Platform, Standard Edition
                        Runtime Environment
                             Version 6


The Java(TM) Platform, Standard Edition Runtime Environment (JRE(TM)),
excluding the JavaFX(TM) runtime, is intended for software developers 
and vendors to redistribute with their applications.

The Java SE Runtime Environment contains the Java virtual machine,
runtime class libraries, and Java application launcher that are
necessary to run programs written in the Java programming language.
It is not a development environment and does not contain development
tools such as compilers or debuggers.  For development tools, see the
Java SE Development Kit (JDK(TM)). The JRE installation triggers the 
download of the JavaFX runtime. The JavaFX runtime is also available 
separately, and is not part of the JRE. For information on JavaFX, and 
how to make changes, go to: http://java.com/javafx


=======================================================================
     Deploying Applications with the Java SE Runtime Environment
=======================================================================

When you deploy an application written in the Java programming
language, your software bundle will probably consist of the following
parts:

    Your own class, resource, and data files.
    The Java SE Runtime Environment.
    An installation procedure or program.

You already have the first part, of course. The remainder of this
document covers the other two parts. See also the Notes for Developers
page on the Java Software website:

    http://java.sun.com/javase/6/webnotes/runtime.html

-----------------------------------------------------------------------
Java Platform, Standard Edition Runtime Environment (JRE)
-----------------------------------------------------------------------

To run your application, a user needs the Java SE Runtime Environment,
which is freely available from Sun. Or, you can redistribute the
Java SE Runtime Environment for free with your application, according
to the terms of the Java SE Platform Runtime Environment's license. The 
JavaFX runtime is only required to run applications written in the 
JavaFX scripting language; it is freely available from Sun, and must
not be redistributed with your application.

The final step in the deployment process occurs when the software is
installed on an individual user's system. Installation consists of copying
software onto the user's system, then configuring the user's system
to support that software.  You should ensure that your installation
procedure does not overwrite existing JRE installations, as they may
be required by other applications.


=======================================================================
      Redistribution of the Java SE Runtime Environment (JRE)
=======================================================================

    --------------------------------------------------------
    NOTE - The license for this software does not allow the
    redistribution of beta and other pre-release versions.
    --------------------------------------------------------

Subject to the terms and conditions of the Software License
Agreement and the obligations, restrictions, and exceptions set
forth below, You may reproduce and distribute the Software (and
also portions of Software identified below as Redistributable),
provided that:

(a) you distribute the Software complete and unmodified and only
    bundled as part of your applets and applications ("Programs"),

(b) your Programs add significant and primary functionality to the
    Software,

(c) your Programs are only intended to run on Java-enabled general
    purpose desktop computers and servers,

(d) you distribute Software for the sole purpose of running your
    Programs,

(e) you do not distribute additional software intended to replace
    any component(s) of the Software,

(f) you do not remove or alter any proprietary legends or notices
    contained in or on the Software,

(g) you only distribute the Software subject to a license agreement
    that protects Sun's interests consistent with the terms
    contained in this Agreement, and

(h) you agree to defend and indemnify Sun and its licensors from
    and against any damages, costs, liabilities, settlement amounts
    and/or expenses (including attorneys' fees) incurred in
    connection with any claim, lawsuit or action by any third party
    that arises or results from the use or distribution of any and
    all Programs and/or Software.

The term "vendors" used here refers to licensees, developers, and
independent software vendors (ISVs) who license and distribute the
Java SE Runtime Environment with their programs.

Vendors must follow the terms of the Java SE Runtime Environment Binary
Code License agreement.

-----------------------------------------------------------------------
Required vs. Optional Files
-----------------------------------------------------------------------

The files that make up the Java SE Runtime Environment are divided into
two categories: required and optional.  Optional files may be excluded
from redistributions of the Java SE Runtime Environment at the
vendor's discretion.

The following section contains a list of the files and directories that
may optionally be omitted from redistributions with the Java SE Runtime
Environment.  All files not in these lists of optional files must be
included in redistributions of the runtime environment.

-----------------------------------------------------------------------
Optional Files and Directories
-----------------------------------------------------------------------

The following files may be optionally excluded from redistributions.
These files are located in the jre1.6.0_<version> directory, where
<version> is the update version number.  Solaris and Linux filenames
and separators are shown. Windows executables have the ".exe" suffix.
Corresponding files with _g in the name can also be excluded.
The corresponding man pages should be excluded for any excluded
executables (with paths listed below beginning with bin/ , 
for the Solaris(TM) Operating System and Linux). 

    lib/charsets.jar
	Character conversion classes
    lib/ext/
	sunjce_provider.jar - the SunJCE provider for Java
	  Cryptography APIs
	localedata.jar - contains many of the resources
	  needed for non US English locales
	ldapsec.jar - contains security features supported
	  by the LDAP service provider
	dnsns.jar - for the InetAddress wrapper of JNDI DNS provider
    bin/rmid
	Java RMI Activation System Daemon
    bin/rmiregistry
	Java Remote Object Registry
    bin/tnameserv
	Java IDL Name Server
    bin/keytool
	Key and Certificate Management Tool
    bin/kinit
	Used to obtain and cache Kerberos ticket-granting tickets
    bin/klist
	Kerberos display entries in credentials cache and keytab
    bin/ktab
	Kerberos key table manager
    bin/policytool
	Policy File Creation and Management Tool
    bin/orbd
	Object Request Broker Daemon
    bin/servertool
	Java IDL Server Tool
    bin/javaws, lib/javaws/ and lib/javaws.jar
	Java Web Start

When redistributing the JRE on Microsoft Windows as a private
application runtime (not accessible by other applications)
with a custom launcher, the following files are also
optional.  These are libraries and executables that are used
for Java support in Internet Explorer and Mozilla family browsers;
these files are not needed in a private JRE redistribution.

    bin\java.exe
    bin\javaw.exe
    bin\javaws.exe
    bin\javacpl.exe
    bin\jucheck.exe
    bin\jusched.exe

    bin\wsdetect.dll
    bin\NPJPI*.dll   (The filename changes in every release)
    bin\NPJava11.dll
    bin\NPJava12.dll
    bin\NPJava13.dll
    bin\NPJava14.dll
    bin\NPJava32.dll
    bin\NPOJI610.dll
    bin\RegUtils.dll
    bin\axbridge.dll
    bin\deploy.dll
    bin\jpicom.dll
    bin\javacpl.cpl
    bin\jpiexp.dll
    bin\jpinscp.dll
    bin\jpioji.dll
    bin\jpishare.dll
    lib\deploy.jar
    lib\plugin.jar
    lib\javaws.jar
    lib\javaws\messages.properties
    lib\javaws\messages_de.properties
    lib\javaws\messages_es.properties
    lib\javaws\messages_fr.properties
    lib\javaws\messages_it.properties
    lib\javaws\messages_ja.properties
    lib\javaws\messages_ko.properties
    lib\javaws\messages_sv.properties
    lib\javaws\messages_zh_CN.properties
    lib\javaws\messages_zh_HK.properties
    lib\javaws\messages_zh_TW.properties
    lib\javaws\miniSplash.jpg


-----------------------------------------------------------------------
Redistributable JDK(TM) Files
-----------------------------------------------------------------------

The limited set of files from the Java SE Development Kit (JDK)
listed below may be included in vendor redistributions of the Java SE 
Runtime Environment.  All paths are relative to the top-level
directory of the JDK. The corresponding man pages should be included for
any included executables (with paths listed below beginning with bin/ ,
for the Solaris(TM) Operating System and Linux). 

    jre/lib/cmm/PYCC.pf
        Color profile.  This file is required only if one wishes to
        convert between the PYCC color space and another color space.

    All .ttf font files in the jre/lib/fonts directory. 
        Note that the LucidaSansRegular.ttf font is already contained 
        in the Java SE Runtime Environment, so there is no need to 
        bring that file over from the JDK.

    jre/lib/audio/soundbank.gm
        This MIDI soundbank is present in the JDK, but it has
        been removed from the Java SE Runtime Environment in order to
        reduce the size of the Runtime Environment's download bundle.
        However, a soundbank file is necessary for MIDI playback, and
        therefore the JDK's soundbank.gm file may be included in
        redistributions of the Runtime Environment at the vendor's
        discretion. Several versions of enhanced MIDI soundbanks are
        available from the Java Sound web site:
        http://java.sun.com/products/java-media/sound/
        These alternative soundbanks may be included in redistributions
        of the Java SE Runtime Environment.

    The javac bytecode compiler, consisting of the following files:
        bin/javac           [Solaris(TM) Operating System
                             and Linux]
        bin/sparcv9/javac   [Solaris Operating System
                             (SPARC(R) Platform Edition)]
	bin/amd64/javac     [Solaris Operating System (AMD)]
        bin/javac.exe       [Microsoft Windows]
        lib/tools.jar       [All platforms]

    The Annotation Processing Tool, consisting of the following files:
        lib/tools.jar       [All platforms]  
        bin/apt             [Solaris(TM) Operating System
                             and Linux]
        bin/sparcv9/apt     [Solaris Operating System
                             (SPARC(R) Platform Edition)]
	bin/amd64/apt       [Solaris Operating System (AMD)]
        bin/apt.exe         [Microsoft Windows]

    lib/jconsole.jar
        The Jconsole application.  NOTE: The Jconsole application requires
        the dynamic attach mechanism.

    The dynamic attach mechanism consisting of the following files:
        lib/tools.jar       [All platforms]
        jre/lib/sparc/libattach.so 
          [Solaris(TM) Operating System (SPARC(R) Platform Edition) and Linux]
        jre/lib/sparcv9/libattach.so 
          [Solaris(TM) Operating System (SPARC(R) Platform Edition) and Linux]
        jre/lib/i386/libattach.so 
          [Solaris(TM) Operating System (x86) and Linux]
        jre/lib/amd64/libattach.so 
          [Solaris(TM) Operating System (AMD) and Linux]
        jre\bin\attach.dll  [Microsoft Windows]

    The Java Platform Debugger Architecture implementation consisting of the
    files shown in the dynamic attach section above, and the following files:
        lib/tools.jar       [All platforms]
        lib/sa-jdi.jar      [All platforms]
        jre/lib/sparc/libsaproc.so 
          [Solaris(TM) Operating System (SPARC(R) Platform Edition) and Linux]
        jre/lib/sparcv9/libsaproc.so 
          [Solaris(TM) Operating System (SPARC(R) Platform Edition) and Linux]
        jre/lib/i386/libsaproc.so 
          [Solaris(TM) Operating System (x86) and Linux]
        jre/lib/amd64/libsaproc.so 
          [Solaris(TM) Operating System (AMD) and Linux]

    jre\bin\server\
        On Microsoft Windows platforms, the JDK includes both
        the Java HotSpot(TM) Server VM and Java HotSpot Client VM.
        However, the Java SE Runtime Environment for Microsoft Windows
        platforms includes only the Java HotSpot Client VM. Those wishing
        to use the Java HotSpot Server VM with the Java SE Runtime
        Environment may copy the JDK's jre\bin\server folder to a 
        bin\server directory in the Java SE Runtime Environment. Software
        vendors may redistribute the Java HotSpot Server VM with their
        redistributions of the Java SE Runtime Environment.


-----------------------------------------------------------------------
Unlimited Strength Java Cryptography Extension
-----------------------------------------------------------------------

Due to import control restrictions for some countries, the Java
Cryptography Extension (JCE) policy files shipped with the Java SE
Development Kit and the Java SE Runtime Environment allow strong but
limited cryptography to be used.  These files are located at

     <java-home>/lib/security/local_policy.jar
     <java-home>/lib/security/US_export_policy.jar

where <java-home> is the jre directory of the JDK or the
top-level directory of the Java SE Runtime Environment.

An unlimited strength version of these files indicating no restrictions
on cryptographic strengths is available on the JDK web site for
those living in eligible countries.  Those living in eligible countries
may download the unlimited strength version and replace the strong
cryptography jar files with the unlimited strength files.

-----------------------------------------------------------------------
The cacerts Certificates File
-----------------------------------------------------------------------

Root CA certificates may be added to or removed from the Java SE
certificate file located at 

    <java-home>/lib/security/cacerts

For more information, see The cacerts Certificates File section
in the keytool documentation at:

http://java.sun.com/javase/6/docs/tooldocs/solaris/keytool.html#cacerts

=======================================================================
Endorsed Standards Override Mechanism
=======================================================================

From time to time it is necessary to update the Java platform in order
to incorporate newer versions of standards that are created outside of
the Java Community Process(SM) (JCP(SM) http://www.jcp.org/) (Endorsed
Standards), or in order to update the version of a technology included
in the platform to correspond to a later standalone version of that
technology (Standalone Technologies).

The Endorsed Standards Override Mechanism provides a means whereby
later versions of classes and interfaces that implement Endorsed
Standards or Standalone Technologies may be incorporated into the Java
Platform.

For more information on the Endorsed Standards Override Mechanism,
including the list of platform packages that it may be used to
override, see

    http://java.sun.com/javase/6/docs/technotes/guides/standards/ 

-----------------------------------------------------------------------
The Java(TM) Runtime Environment (JRE) and the JavaFX(TM) runtime are 
products of Sun Microsystems(TM), Inc.

Copyright © 2010 Sun Microsystems, Inc.
4150 Network Circle, Santa Clara, California 95054, U.S.A.
All rights reserved.

