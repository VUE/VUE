package tufts.macosx;

import java.awt.Desktop;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.QuitHandler;
import java.io.File;

/**
 * Mac OS X application integration (Finder "open file", the application menu's
 * About / Preferences / Quit items).
 *
 * Historically this used the Apple-specific com.apple.eawt.ApplicationListener API,
 * which was removed from the JDK in Java 9.  It now uses the standard
 * java.awt.Desktop handler API (Java 9+), which works on every platform that
 * supports the corresponding actions, so this class is safe to load anywhere.
 */
public class MacOSX16Safe {
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MacOSX.class);
    protected static boolean DEBUG = false;

    public interface ApplicationListener {
        public boolean handleOpenFile(String filename);
        public boolean handleQuit();
        public boolean handleAbout();
        public boolean handlePreferences();
    }

    public static void registerApplicationListener(final ApplicationListener listener) {
        if (!Desktop.isDesktopSupported()) {
            errout("java.awt.Desktop not supported on this platform: no application handlers installed");
            return;
        }
        final Desktop desktop = Desktop.getDesktop();

        if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
            desktop.setOpenFileHandler(new OpenFilesHandler() {
                public void openFiles(java.awt.desktop.OpenFilesEvent e) {
                    for (File f : e.getFiles())
                        listener.handleOpenFile(f.getPath());
                }
            });
        } else if (DEBUG) out("APP_OPEN_FILE not supported");

        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler(new QuitHandler() {
                public void handleQuitRequestWith(java.awt.desktop.QuitEvent e, java.awt.desktop.QuitResponse response) {
                    // If the listener reports the quit as handled, the OS performs the quit
                    // for us; otherwise the listener is responsible for exiting (or has
                    // cancelled), so we tell the OS not to quit.
                    if (listener.handleQuit())
                        response.performQuit();
                    else
                        response.cancelQuit();
                }
            });
        } else if (DEBUG) out("APP_QUIT_HANDLER not supported");

        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(new AboutHandler() {
                public void handleAbout(java.awt.desktop.AboutEvent e) {
                    listener.handleAbout();
                }
            });
        } else if (DEBUG) out("APP_ABOUT not supported");

        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler(new PreferencesHandler() {
                public void handlePreferences(java.awt.desktop.PreferencesEvent e) {
                    listener.handlePreferences();
                }
            });
        } else if (DEBUG) out("APP_PREFERENCES not supported");
    }

    protected static void out(String s) {
        Log.debug(s);
    }

    protected static void errout(String s) {
        Log.warn(s);
    }
}
