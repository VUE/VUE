package tufts.macosx;


import com.apple.eawt.ApplicationEvent;


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
