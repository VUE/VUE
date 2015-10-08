ChangeLog
---------

### v0.9.0 (2015-05-15)
* added support for `JavaX` Plist key (fixes #9)

### v0.8.1 (2015-03-26)
* Bugfix for `JVMVersion` key present but no JVMs in `/usr/libexec/java_home`

### v0.8.0 (2015-02-22)
* support for `JVMVersion` key (fixes #13, Thanks to @Dylan-M for his contribution)
* use `$HOME` instead of `~` to set the users home directory (fixes #11)
* WorkingDirectory: improved substitution of variables ($JAVAROOT, $APP_PACKAGE, $USER_HOME) (fixes #12)
* use different non-zero exit codes

### v0.7.0 (2014-10-12)
* read ClassPath from ApplePlist in either Array or String style (PR #5, Thanks to Philipp Holzschneider for his contribution)
* read StartOnMainThread (issue #4, Thanks to @wrstlbrnft for his contribution)

### v0.6.3 (2014-07-31)
* check Info.plist for Apple style Java keys. Better indicator to distinguish between Apple or Oracle parsing...

### v0.6.2 (2014-07-28)
* minor code refactoring and bugfixes

### v0.6.1 (2014-07-27)
* Standard Working Directory for Apple PList apparently is the AppRoot directory

### v0.6 *(2014-07-12)*
* also catch fixed paths for Plist key `JVMWorkDir` *(thanks @dpolivaev)*

### v0.5 *(2014-06-30)*
* bugfix for pathes / App bundles containing spaces (#2)

### v0.4 *(2014-06-30)*
* read and set WorkingDirectory based on the key in `Info.plist` (#1)
 * interpret the 3 different values $JAVAROOT, $APP_PACKAGE, $USER_HOME
 * fallback to root / as standard

### v0.3 *(2014-03-16)*
* enable drag&drop to the dock icon

### v0.2 *(2014-03-16)*
* trim whitespace from variables and commandline
* don't show errors in output for Info.plist querying

### v0.1 *(2014-03-09)*
* initial release of 'universalJavaApplicationStub'
