universalJavaApplicationStub
=====================

A shellscript JavaApplicationStub for Java Apps on Mac OS X that works with both Apple's and Oracle's plist format. It is released under the MIT License.


Why
---

Whilst developing some Java apps for Mac OS X I was facing the problem of supporting two different Java versions – the "older" Apple versions and the "newer" Oracle versions.

**Is there some difference, you might ask?** Yes, there is!

1. The spot in the file system where the JRE or JDK is stored is different:
  * Apple Java 1.5/1.6: `/System/Library/Java/JavaVirtualMachines/`
  * Oracle JRE 1.7/1.8: `/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/`
  * Oracle JDK 1.7/1.8: `/System/Library/Java/JavaVirtualMachines/`
 
2. Mac Apps built with tools designed for Apple's Java (like Apple's JarBundler or the [ANT task "Jarbundler"](http://informagen.com/JarBundler/)) won't work on Macs with Oracle Java 7 and no Apple Java installed.
  * This is because the Apple `JavaApplicationStub` only works for Apple's Java and their `Info.plist` style to store Java properties.
  * To support Oracle Java 7 you would need to built a separate App package with Oracles [ANT task "Appbundler"](https://java.net/projects/appbundler).
  * Thus you would need the user to know which Java distribution he has installed on his Mac. Not very user friendly...
 
3. Oracle uses a different syntax to store Java properties in the applications `Info.plist` file. A Java app packaged as a Mac app with Oracles Appbundler also needs a different `JavaApplicationStub` and therefore won't work on systems with Apple's Java...

4. Starting with Mac OS X 10.10 *(Yosemite)*, app packages won't open up anymore if they contain the *deprecated* Plist `Java` dictionary. This isn't confirmed by Apple, but [issue #9](https://github.com/tofi86/universalJavaApplicationStub/issues/9) leads to this assumption:
  * Apple seems to declare the `Java` dictionary as *deprecated* and requires the old Apple Java 6 to be installed. Otherwise the app doesn't open.
  * If Java 7/8 is installed, Apple doesn't accept those java versions as suitable
  * Apple prompts for JRE 6 download even before the `JavaApplicationStub` is executed. This is why we can't intercept at this level and need to replace the `Java` dictionary by a `JavaX` dictionary.
  * This requires the use of my JarBundler fork (see below for more details)

*So why, oh why, couldn't Oracle just use the old style of storing Java properties in `Info.plist` and offer a universal JavaApplicationStub?!* :rage:

Well, since I can't write such a script in C, C# or whatever fancy language, I wrote it as a shell script. And it works! ;-)

How it works
------------

You don't need a native `JavaApplicationStub` file anymore...

The shell script reads JVM properties from `Info.plist` regardless of which format they have, Apple or Oracle, and feeds it to a commandline `java` call:

```Bash
# execute Java and set
#	- classpath
#	- dock icon
#	- application name
#	- JVM options
#	- JVM default options
#	- main class
#	- JVM arguments
	exec "$JAVACMD" \
		-cp "${JVMClassPath}" \
		-Xdock:icon="${ResourcesFolder}/${CFBundleIconFile}" \
		-Xdock:name="${CFBundleName}" \
		${JVMOptions:+$JVMOptions }\
		${JVMDefaultOptions:+$JVMDefaultOptions }\
		${JVMMainClass}\
		${JVMArguments:+ $JVMArguments}
```

It sets the classpath, the dock icon, the *AboutMenuName* (in Xdock style) and then every *JVMOptions*, *JVMDefaultOptions* or *JVMArguments* found in the `Info.plist` file.

The WorkingDirectory is either retrieved from Apple's `Info.plist` key `Java/WorkingDirectory` or set to the JavaRoot directory within the app bundle.

The name of the *main class* is also retrieved from `Info.plist`. If no *main class* could be found, an applescript error dialog is shown and the script exits with *exit code 1*.

Also, there is some *foo* happening to determine which Java version is installed. Here's the list in which order system properties are checked:

1. system variable `$JAVA_HOME`
2. `/usr/libexec/java_home` symlinks
3. symlink for old Apple Java: `/Library/Java/Home/bin/java`
4. hardcoded fallback to Oracle's JRE Plugin: `/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java`

If none of these could be found or executed, an applescript error dialog is shown saying that Java need to be installed.

What you need to do
-------------------

Use whichever ANT task you like:
* the opensource ["Jarbundler"](http://informagen.com/JarBundler/) *(not recommended anymore)*
* my JarBundler [fork on github](https://github.com/tofi86/Jarbundler) which supports the newly introduced `JavaX` key *(recommended)*
* Oracle's opensource ["Appbundler"](https://java.net/projects/appbundler)
  * or [*infinitekind*'s fork](https://bitbucket.org/infinitekind/appbundler/overview)

### Original JarBundler (v2.3) example
*Might lead to compatibility issues. See below for details...*

Just place the `universalJavaApplicationStub` from this repo in your build resources folder and link it in your ANT task (attribute `stubfile`):
```XML
<jarbundler
	name="Your-App"
	shortname="Your Application"
	icon="${resources.dir}/icon.icns"
	stubfile="${resources.dir}/universalJavaApplicationStub"
	... >
	
</jarbundler>
```

The ANT task will care about the rest...

You should get a functional Mac Application Bundle working with both Java distributions from Apple and Oracle **but with possible incompatibilities to Mac OS X 10.10:**

:exclamation: **Attention:**
> Using the "old" JarBundler <= v2.3 might result in [issue #9](https://github.com/tofi86/universalJavaApplicationStub/issues/9) *(Mac OS X 10.10 asking to install deprecated Apple JRE 6 instead of using a newer Java version)*
> 
> If you don't want to care about compatibility issues between OS X and Java versions, better use my JarBundler fork (see next example).

### My JarBundler fork (v2.4) example
Download the latest release of my JarBundler fork [from it's github repo](https://github.com/tofi86/Jarbundler) and replace your old JarBundler library with the new one.

Then place the `universalJavaApplicationStub` from this repo in your build resources folder and link it in your ANT task (attribute `stubfile`). Don't forget to set the newly introduced `useJavaXKey` option:
```XML
<jarbundler
	name="Your-App"
	shortname="Your Application"
	icon="${resources.dir}/icon.icns"
	stubfile="${resources.dir}/universalJavaApplicationStub"
	useJavaXKey="true"
	... >
	
</jarbundler>
```

The ANT task will care about the rest...

You should get a fully functional Mac Application Bundle working with both Java distributions from Apple and Oracle and all Mac OS X versions.

### Appbundler example
Just place the `universalJavaApplicationStub` from this repo in your build resources folder and link it in your ANT task (attribute `executableName` from [*infinitekind*'s fork](https://bitbucket.org/infinitekind/appbundler/overview)):
```XML
<appbundler
	name="Your-App"
	displayname="Your Application"
	icon="${resources.dir}/icon.icns"
	executableName="${resources.dir}/universalJavaApplicationStub"
	... >
	
</appbundler>
```


The ANT task will care about the rest...

You should get a fully functional Mac Application Bundle working with both Java distributions from Apple and Oracle and all Mac OS X versions.


Missing Features
----------------

At the moment, there's no support for
* required JVM architecture (like `x86_64`, etc.)


License
-------

*universalJavaApplicationStub* is released under the MIT License.
