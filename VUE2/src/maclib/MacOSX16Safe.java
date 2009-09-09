package tufts.macosx;


import com.apple.eawt.ApplicationEvent;


/**
 * This class is safe to load on all Java VM's, both 32-bit and 64-bit
 * (for both Java5 and Java6).  Its subclass, MacOSX, is NOT safe to
 * load on any 64-bit JVM, as there is no native 64-bit library for
 * the deprecated java-cocoa bridge, and any code that references
 * those classes may fail to load (e.g., NSWindow.class).
 * Technically, MacOSX.class can load in 64-bit as long as none of the
 * native calls are made, but these classes are not present in Snow
 * Leopard, and so any reference to them at all will cause a
 * class-loading failure.
 */
public class MacOSX16Safe { // technically, Snow-Leopard safe
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MacOSX.class);
    protected static boolean DEBUG = false;
    
    public interface ApplicationListener {
        public boolean handleOpenFile(String filename);
        public boolean handleQuit();
        public boolean handleAbout();
        public boolean handlePreferences();
    }

	 public static void registerApplicationListener(final ApplicationListener listener) {
	        final com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();

	        application.addPreferencesMenuItem();
	        application.setEnabledPreferencesMenu(true);

	        application.addApplicationListener(new com.apple.eawt.ApplicationListener() {
	                public void handleOpenFile(ApplicationEvent e) {
	                    e.setHandled(listener.handleOpenFile(e.getFilename()));
	                }
	                public void handleQuit(ApplicationEvent e) {
	                    // Note: if handled is set to true, Apple code will quit the app when this returns.
	                    e.setHandled(listener.handleQuit());
	                }
	                public void handleAbout(ApplicationEvent e) {
	                    e.setHandled(listener.handleAbout());
	                }
	                public void handlePreferences(ApplicationEvent e) {
	                    e.setHandled(listener.handlePreferences());
	                }
	                
	                public void handleOpenApplication(ApplicationEvent e) {
	                    if (DEBUG) out("OSX APPLCATION OPEN " + e);
	                }
	                public void handleReOpenApplication(ApplicationEvent e) {
	                    out("OSX APPLICATION RE-OPEN " + e);
	                }
	                public void handlePrintFile(ApplicationEvent e) {
	                    out("OSX APPLICATION PRINT FILE " + e);
	                }
	            });
	    }
	 
	 	protected static void out(String s) {
	        //System.out.println("MacOSX lib: " + s);
	        Log.debug(s);
	    }

	    protected static void errout(String s) {
	        //System.err.println("MacOSX lib: " + s);
	        Log.warn(s);
	    }
}
