/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package tufts.vue;

import java.awt.*;
import java.applet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.*;

import tufts.vue.gui.GUI;
import tufts.vue.gui.VueMenuBar;

// To get to run from classpath: had to be sure to create a symlink to
// VueResources.properties
// from the build/classes tree to the source tree.

// Of course, now need all the damn support libraries...

/**
 * Experimental VUE applet.
 * 
 * @version $Revision: 1.11 $ / $Date: 2008-11-03 14:12:26 $ / $Author: mike $
 */
public class VueApplet extends JApplet implements Runnable {

	private JLabel loadLabel;
	private static boolean firstInit = true;
	private static MapViewer viewer = null;
	private JPanel toolbarPanel = null;
	private JComponent toolbar = null;
	// If want to have viewer left in same state when go forwrd/back in browser,
	// will need javacript assist to associate the viewer with the instance of a
	// web browser page:
	// new applet & context objects are created even when you go forward/back
	// pages.
	
	private static MapTabbedPane mMapTabbedPane = null;

	// applet parameters
	/*
	 * Valid values for zoteroPlugin : true , false default value: false
	 */
	private static final String zoteroPlugin = "zoteroPlugin";
	public static JApplet instance = null;
	public void init() {
		instance =this;
		VUE.setAppletContext(this.getAppletContext());
		processAppletParameters();
		getContentPane().setLayout(new BorderLayout());
		
		msg("init\n\tapplet=" + Integer.toHexString(hashCode())
				+ "\n\tcontext=" + getAppletContext());
		if (!GUI.isGUIInited())
		{	VUE.initUI();		
			VUE.initApplication();
		}
	
	//	new Thread(this).start();
		loadViewer();
		msg("init completed");
	}

	private final void processAppletParameters() {
		String zoteroPlugin = this.getParameter(this.zoteroPlugin);
		if (zoteroPlugin != null)
			zoteroPlugin = zoteroPlugin.toLowerCase();
		if (zoteroPlugin != null && zoteroPlugin.equals("true")) {
			tufts.vue.action.TextOpenAction.setZoteroPrototypeEnabled(true);
		}

	}

	// Load the MapViewer, triggering massive class loading...
	public void run() {
		msg("load thread started...");
		try {
			loadViewer();
		} catch (Throwable t) {
			loadLabel.setText(t.toString());
		}
		msg("load thread complete");
	}

	public void destroy() {
		// super.destroy();
		msg("destroy");
	}

	public void start() {
	

	//	loadViewer();
		if (!VUE.getLeftTabbedPane().isEnabled())
	       	VUE.getLeftTabbedPane().setEnabled(true);
		msg("start");

	}

	public void stop() {
		// super.stop();
		tufts.vue.gui.DockWindow.HideAllWindows();
		// this.removeAll();
		VUE.finalizeDocks();

		// map = null;
		// mMapTabbedPane = null;
		// viewer= null;
		// getContentPane().removeAll();
		System.gc();
		// System.runFinalization();
		msg("stop");
	}

	public static String getActiveMapItems()
    {
        LWMap active = VUE.getActiveMap();
        java.util.Iterator it = active.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        String items = "";
        do
        {
            if(!it.hasNext())
                break;
            LWComponent n = (LWComponent)it.next();
            if(n instanceof LWNode)
            {
                items = (new StringBuilder()).append(items).append(n.getLabel()).append(",").toString();
                String id = null;
                try
                {
                    id = n.getResource().getProperty("Zotero.id");
                }
                catch(Exception e)
                {
                    System.out.println("Exception in zotero import from vue: no zotero id");
                }
                if(id == null)
                    items = (new StringBuilder()).append(items).append("none,").toString();
                else
                    items = (new StringBuilder()).append(items).append(id).append(",").toString();
                String url = null;
                try
                {
                    url = n.getResource().getSpec();
                }
                catch(Exception e)
                {
                    System.out.println("Exception in zotero import from vue: no zotero url");
                }
                if(url == null)
                    items = (new StringBuilder()).append(items).append("none").toString();
                else
                    items = (new StringBuilder()).append(items).append(url).toString();
                items = (new StringBuilder()).append(items).append("\n").toString();
            }
        } while(true);
        return items;
    }

	public synchronized void loadViewer() {
		// viewer = getMapViewer();
	
		

		msg("got viewer");
		msg("is applet ? " + VUE.isApplet());

		msg("setting menu bar...");
		VueMenuBar vmb = new VueMenuBar();
		setJMenuBar(vmb);

		final VueToolbarController tbc = VueToolbarController.getController();

		if (toolbar == null)
			toolbar = tbc.getToolbar().getMainToolbar();

		if (toolbarPanel == null)
			toolbarPanel = VUE.constructToolPanel(toolbar);

		getContentPane().setBackground(toolbarPanel.getBackground());
		getContentPane().add(toolbarPanel, BorderLayout.NORTH);
		//getContentPane().add(mMapTabbedPane, BorderLayout.CENTER);
		if (!firstInit)
		{
			VUE.createDockWindows();
			vmb.rebuildWindowsMenu();
		}

		getContentPane().add(VUE.getLeftTabbedPane(),BorderLayout.CENTER);	
		validate();
		
	//	if (firstInit)
		VUE.displayMap(new LWMap("New Map"));
	
		msg("validating...");

	

		msg("loading complete");
		// initialize enabled state of actions via a selection set:
		if (!firstInit)
			EditorManager.refresh();
        
		VUE.getSelection().clearAndNotify();

		VUE.setActive(MapViewer.class, this, null); // no open viewers
        firstInit=false;
        System.out.println("A " +VUE.getLeftTabbedPane().isEnabled());
        System.out.println("B " +VUE.getLeftTabbedPane().isValid());
        
  
	}

	private void msg(String s) {
		System.out.println("VueApplet: " + s);

		// showStatus("VueApplet: " + s);
	}

	@SuppressWarnings("unchecked")
	public static void displayZoteroExport(final String urlString) {
		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				try {
					File tempFile = null;
					tempFile = File.createTempFile(new Long(System
							.currentTimeMillis()).toString(), null);
					InputStream io = null;

					io = getInputStream(urlString);
					byte[] buf = new byte[256];
					int read = 0;
					java.io.FileOutputStream fos = null;
					fos = new java.io.FileOutputStream(tempFile);

					while ((read = io.read(buf)) > 0) {
						fos.write(buf, 0, read);
					}
					tufts.vue.action.TextOpenAction.displayMap(tempFile);

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

		});

	}

	public static void displayMap(String urlString) {
		innerDisplayMap(urlString);
	}

	@SuppressWarnings("unchecked")
	private static void innerDisplayMap(final String urlString) {

		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				/*
				 * try { File tempFile = null; tempFile =
				 * File.createTempFile(new
				 * Long(System.currentTimeMillis()).toString(), null);
				 * InputStream io = null;
				 * 
				 * io = getInputStream(urlString); byte[] buf = new byte[256];
				 * int read = 0; java.io.FileOutputStream fos = null; fos = new
				 * java.io.FileOutputStream(tempFile);
				 * 
				 * while ((read = io.read(buf)) > 0) { fos.write(buf, 0, read); }
				 */
				URL url;
				try {
					url = new URL(urlString);
					tufts.vue.action.OpenURLAction.displayMap(url);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * tufts.vue.action.OpenAction.displayMap(tempFile);
				 *  } catch(Exception e) { e.printStackTrace(); }
				 */
				return null;
			}

		});

	}

	public static InputStream getInputStream(String fileName)
			throws IOException {
		InputStream input;

		if (fileName.startsWith("http:")) {
			URL url = new URL(fileName);
			URLConnection connection = url.openConnection();
			input = connection.getInputStream();
		} else {
			input = new FileInputStream(fileName);
		}

		return input;
	}

	protected static MapViewer getMapViewer() {

		// VUE.installExampleMap(map);

		return viewer;
	}

	protected static MapTabbedPane getMapTabbedPane() {

		// VUE.installExampleMap(map);

		return mMapTabbedPane;
	}

}