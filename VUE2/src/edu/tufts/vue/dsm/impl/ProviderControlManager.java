package edu.tufts.vue.dsm.impl;

import java.io.File;
import java.util.Properties;
import org.apache.log4j.Level;

/**
 * This exists to provide an org.osid.provider.ProviderControlManager with the ability to change the
 * reported value of the "root" property from osid.properties dynamically at runtime w/out having to
 * re-write a bunch of the low-level installer code (which currently exist only in jar files,
 * and is not part of our source tree -- see TuftsOsidProivder.jar and MIT-OTS-NO_PROVIDER.jar).
 *
 * We need to do this because the directory /Library on Mac OS X changed to be default unwriteable
 * (at least as of Lion, possibly prior).
 *
 * We override getConfiguration("root") to return our desired value.  This also means we could,
 * if we wish in the future, make it a per-user directory opposed to a system-wide
 * shared directory.
 *
 * Note that this class MUST be have a base-name of "ProviderControlManager", as this will be
 * instanced, initialized and returned by a call to edu.mit.osidimpl.OsidLoader.getManager(...)  in
 * VueOsidFactory.
 *
 * We override the ProviderControlManager implementation that's been in use for years:
 * @see edu.mit.osidimpl.provider.repository.ProviderControlManager
 *
 * S.Fraize 2012 June
 *
 * @see http://stuff.mit.edu/afs/athena/project/okidev/okiproject/impl/seussville/src/edu/mit/osidimpl/manager
 * @see http://stuff.mit.edu/afs/athena/project/okidev/okiproject/impl/seussville/src/edu/mit/osidimpl/provider
 */

public class ProviderControlManager
    extends edu.mit.osidimpl.provider.repository.ProviderControlManager
{
    public static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VuePCM.class);

    private static final String OldLibraryDir = "/Library/OsidProviders";
    private static final String NewLibraryDir = "/Library/Caches/OsidProviders"; // mac only
    
    private String runtimeRoot = null;

    private static boolean DEBUG = false;

    //private void trace(String s) { Log.info(System.identityHashCode(this) + ": " + s); }
    private void trace(String s) { Log.info(s); }

    @Override public void assignConfiguration(java.util.Properties conf) throws org.osid.OsidException {
        if (DEBUG) trace("assignConfiguration " + conf + " ...");
        // This will load the configuration, at which point getConfiguration(someKey) will work,
        // and then call initialize:
        super.assignConfiguration(conf);
        if (DEBUG) trace("assignConfiguration done.");
    }

    // Note that in super.initialize, "root" is pulled from config, and then
    // an installer is instanced:
    // this.installer = new edu.mit.osidimpl.provider.installer.firstcut.Installer(this.rootDir);
    // It ALSO initializes a classloader with getClass().getClassLoader(), which means
    // the class loader COULD be different from the one we were getting out of the
    // jar file, tho probably not -- at deploy time, it's all coming from one archive...
    @Override protected void initialize() throws org.osid.OsidException {
        trace("initialize...");
        if (tufts.Util.isMacPlatform()) {
            try {
                runtimeRoot = checkForRootDirRuntimeConfiguration();
                if (runtimeRoot != null)
                    Log.info("Runtime configured OSID provider install dir: " + runtimeRoot); 
            } catch (Throwable t) {
                Log.warn("checking for runtime root", t);
            }
        }
        
        super.initialize();

        final String installRoot = runtimeRoot == null ? getActiveRoot() : runtimeRoot;
        final File installDir = new File(installRoot);

        if (runtimeRoot == null) {
            // will just be whatever was in osid.properties:
            Log.info("Configured OSID provider install dir: " + installDir);
        }
        
        // Make sure the shared install dir is readable & writeable to all users.
        // (skip this if we move to a per-user home directory based provider install dir)

        try {
            // Make readable & writeable to everyone [these only since Java 1.6]
            //installDir.setReadable(true, false);
            //installDir.setWritable(true, false);
            final Object[] args = { Boolean.TRUE, Boolean.FALSE };
            tufts.Util.execute(installDir, "java.io.File", "setReadable", args);
            tufts.Util.execute(installDir, "java.io.File", "setWritable", args);
        } catch (Throwable t) {
            Log.warn("Couldn't set modes on install dir: " + installDir + "; " + t);
        }
        
        if (DEBUG) trace("initialize done.");
    }
    
    @Override protected void loadConfiguration() throws org.osid.OsidException {
        trace("loadConfiguration...");
        super.loadConfiguration();
        if (DEBUG) trace("loadConfiguration done.");
    }

    /** this isn't called on startup, but can be called later if updating or adding repositories */
    @Override protected void initializeRepository() throws org.osid.provider.ProviderException {
        trace("initializeRepository...");
        super.initializeRepository();
        if (DEBUG) trace("initializeRepository done.");
    }

    // Note that getConfiguration in edu.mit.osidimpl.OsidLoader is private.
    // Fortunately, in a parent class it's protected.
    
    @Override protected String getConfiguration(final String key) {
        String value = super.getConfiguration(key);
        if (runtimeRoot != null && "root".equalsIgnoreCase(key)) {
            trace("getConfig " + key + " = " + value);
            value = runtimeRoot;
            trace(" override " + key + " = " + value);
        } else {
            if (DEBUG) trace("getConfig " + key + " = " + value);
        }
        return value;
    }

    String getActiveRoot() {
        return getConfiguration("root");
    }


    private String checkForRootDirRuntimeConfiguration()
    {
        final File oldDir = new File(OldLibraryDir);
        final File newDir = new File(NewLibraryDir);

        if (oldDir.exists() && oldDir.isDirectory() && oldDir.canRead()) {
            final String[] oldContents = oldDir.list();
            Log.info("Found old OsidProviders dir: " + oldDir + " contains " + oldContents.length + " files; canWrite=" + oldDir.canWrite());
            if (DEBUG) tufts.Util.dumpArray(oldContents);

            if (newDir.exists() && newDir.isDirectory() && newDir.canRead()) {
                // In case an OLD version of VUE was later run which re-created the original dir
                // somehow, prioritize the new directory if it has more contents than the old,
                // or, of course, if the old isn't writeable.
                final String newContents[] = newDir.list();
                Log.info("Found new OsidProviders dir: " + newDir + " contains " + newContents.length + " files");
                if (oldDir.canWrite() && newContents.length > oldContents.length) {
                    Log.info("prioritizing new dir over old based on contents: " + NewLibraryDir);
                    return NewLibraryDir;
                }
            }

            if (oldDir.canWrite()) {
                Log.info("old dir still writeable, keep using: " + OldLibraryDir);
                // Leave it be -- keep using old location.  Even if the user upgrades to a new
                // version of OS X where /Library is no longer writeable, /Library/OsidProviders
                // should already be there -- "grandfathered" in.

                // Note: did sharing /Library/OsidProviders amonst users ever actually work?  It
                // only could have if when it was created initially it was made readable and
                // writeable to all, and I don't see any code for that in the Installer...  If
                // that's the case, this is still a problem going forward, and we should 
                // consider putting this in the users home .vue_2 config directory...
                return null;
                
            } else if (oldContents.length > 0)
                Log.warn("Old providers dir exists, but we cannot write to it: " + oldContents.length + " providers may need re-download for " + NewLibraryDir);
        }

        // Note that our installer, edu.mit.osidimpl.provider.installer.firstcut.Installer,
        // will do File.mkdirs() to create the provider install location.
        
        return NewLibraryDir;
    }

    private void inspectProperties()
    {
        try {
            // ANY class should do using an absolute (starts with '/') resource name...
            final java.io.InputStream in = VueOsidFactory.class.getResourceAsStream("/osid.properties");
            Log.debug("Got stream for inspecting osid.properties: " + in);
            if (in != null) {
                final Properties osidProps = new Properties();
                osidProps.load(in);
                //Log.debug("Got osid.properties: " + osidProps);
                tufts.Util.dump(osidProps.entrySet());
            }
        } catch (Exception e) {
            Log.warn(e);
        }
        //Log.debug("root=" + VueResources.getString("root"));
    }
    
    // /**
    //  * Note that the values of OldLibraryDir and NewLibraryDir depend upon the past (and future) values of the
    //  * property "root" from osid.properties.  Worst case: if we cannot move the old providers dir to the new
    //  * location, the ProviderInstallationManager, when it pulls the value of "root", will create a new empty
    //  * providers dir, and the user will have to re-update / re-download their providers.
    //  *
    //  * ALSO note that this is all much more complicated than if we had access to the ProviderInstallationManger
    //  * SOURCE.  We have one property from osid.properties that it is pulling ("root"), where providers will
    //  * both be found and installed.  This property will be same for all platforms (tho technically we could
    //  * tweak our build process to include different osid.properties files for different platforms), and
    //  * everything must already be in place before the property is polled.
    //  *
    //  */
    // private static void checkAndUpdateProviderInstallLocation()
    // {
    //     if (true) {
    //         // TODO: Maybe only char osid.properties root for MAC platform in a mac-only config file?
    //         // Oh, no, can't work -- I doubt the opaque osid load code makes use of a localization scheme.
    //         // SO PROBLEM: changing osid.properties:root will affect ALL platforms...  And we can't
    //         // just punt is /Library/Caches probably only exists on the mac.
    //         try {
    //             //final java.io.InputStream in = edu.mit.osidimpl.OsidLoader.class.getResourceAsStream("osid.properties");
    //             //final java.io.InputStream in = tufts.vue.VUE.getResourceAsStream("osid.properties");
    //             //final java.io.InputStream in = tufts.Util.class.getResourceAsStream("/osid.properties");
    //             // ANY class should do using an absolute (starts with '/') resource name...
    //             final java.io.InputStream in = VueOsidFactory.class.getResourceAsStream("/osid.properties");
    //             Log.debug("Got stream for inspecting osid.properties: " + in);
    //             if (in != null) {
    //                 final Properties osidProps = new Properties();
    //                 osidProps.load(in);
    //                 //Log.debug("Got osid.properties: " + osidProps);
    //                 tufts.Util.dump(osidProps.entrySet());
    //             }
    //         } catch (Exception e) {
    //             Log.warn(e);
    //         }
    //         //Log.debug("root=" + VueResources.getString("root"));
    //     }

                
    //     final String OldLibraryDir = "/Library/OsidProviders";
    //     final String NewLibraryParentDir = "/Library/Caches";
    //     final String NewLibraryDir = NewLibraryParentDir + "/OsidProviders";
    
    //     final File oldDir = new File(OldLibraryDir);
    //     final File newDir = new File(NewLibraryDir);

    //     boolean foundOld = false;
    //     boolean foundNew = false;

    //     if (oldDir.exists()) {
    //         foundOld = true;
    //         Log.info("Found old OsidProviders dir: " + oldDir);
    //         tufts.Util.dumpArray(oldDir.listFiles());
    //     }
    //     if (newDir.exists()) {
    //         foundNew = true;
    //         Log.info("Found new OsidProviders dir: " + newDir);
    //         if (foundOld)
    //             Log.warn("Found BOTH old and new OsidProviders directories -- use of any old providers may require data source update(s) / re-download(s).");
    //     }

    //     if (foundOld && !foundNew) {
    //         Log.info(" moving " + OldLibraryDir + " to " + NewLibraryDir + "...");

    //         final File newParent = new File(NewLibraryParentDir);
    //         if (!newParent.exists()) {
    //             // shouldn't happen on mac (/Library/Caches should always exist), but
    //             // will probably happen on Windows & Linux.
    //             try {
    //                 if (newParent.mkdirs())
    //                     Log.info("Created " + newParent);
    //                 else
    //                     Log.warn("Failed to create " + newParent);
    //             } catch (Exception e) {
    //                 Log.warn("Failed to create " + newParent, e);
    //             }
    //         } else {
    //             Log.info(newParent + " already exists.");
    //         }
            
    //         Exception ex = null;
    //         boolean success = false;
    //         try {
    //             success = oldDir.renameTo(newDir);
    //         } catch (Exception e) {
    //             ex = e;
    //         }
    //         if (success) {
    //             Log.info("  moved " + OldLibraryDir + " to " + NewLibraryDir);
    //         } else {
    //             Log.warn("move of " + oldDir + " to " + newDir + ": FAILED");
    //             if (ex != null)
    //                 Log.warn(ex);
    //         }
    //     }
    // }
    
}

/** for log tagging to differentiate from classes with same name in other impl dirs */
class VuePCM {}

	
