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


// To get to run from classpath: had to be sure to create a symlink to VueResources.properties
// from the build/classes tree to the source tree.

// Of course, now need all the damn support libraries...

/**
 * Experimental VUE applet.
 *
 * @version $Revision: 1.6 $ / $Date: 2008-08-21 13:46:38 $ / $Author: mike $ 
 */
public class VueApplet extends JApplet implements Runnable {

    private JLabel loadLabel;
    private boolean buttonPushed =false;
    private static MapViewer viewer = null;
    // If want to have viewer left in same state when go forwrd/back in browser,
    // will need javacript assist to associate the viewer with the instance of a web browser page:
    // new applet & context objects are created even when you go forward/back pages.  
    private LWMap map = null;
    static MapTabbedPane mMapTabbedPane = null;
    
    public void init() {
    	
    		System.out.println("APPLET WIDTH : " + this.getWidth());
    		System.out.println("APPLET HEIGHT : " + this.getHeight());
    		VUE.setAppletContext(this.getAppletContext());
        	msg("init\n\tapplet=" + Integer.toHexString(hashCode()) + "\n\tcontext=" + getAppletContext());
        	if (!GUI.isGUIInited())
        	{	
        		VUE.initUI();
        		
        		mMapTabbedPane = new MapTabbedPane("*left", true);
        		map = new LWMap("Applet Map");
        	
        		VUE.initApplication();
        	}
        	if (viewer != null) {
        		loadViewer();
        		//setContentPane(viewer);
        	} else {
        		loadLabel = new JLabel("Loading VUE...");
        		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        		getContentPane().add(Box.createHorizontalGlue());
        		getContentPane().add(loadLabel);
        		getContentPane().add(Box.createHorizontalGlue());
        		new Thread(this).start();
        	}
        	msg("init completed");
    	
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
    	super.destroy();
    	msg("destroy");
    }
    public void start() {
    	super.start();
        msg("start");
    }
    
    public void stop() {
    	super.stop();	
    //	VUE.finalizeDocks();
        msg("stop");
    }
    private JPanel toolbarPanel = null;
    
    public void loadViewer() {
      //  viewer = getMapViewer();
        msg("got viewer");
        
   /*     if (false) {
            JScrollPane scrollPane = new JScrollPane(viewer);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setContentPane(scrollPane);
        } else {
            setContentPane(viewer);
        }
     */   
        msg("contentPane set");
        msg("setting menu bar...");
        
        setJMenuBar(new VueMenuBar());
        //setContentPane(label);
      
        if (viewer == null)
        {
        	viewer = new MapViewer(map);
        	mMapTabbedPane.addViewer(viewer);
       	   
           
        }
        
        VueToolbarController tbc = VueToolbarController.getController();   
        JComponent toolbar = tbc.getToolbar().getMainToolbar();
        toolbarPanel = VUE.constructToolPanel(toolbar);
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        
   	    this.getContentPane().add(toolbarPanel,BorderLayout.NORTH);
   	    getContentPane().add(mMapTabbedPane,BorderLayout.CENTER);
        msg("validating...");
        validate();
        msg("loading complete");
        //getContentPane().add(viewer, BorderLayout.CENTER);
    }

    private void msg(String s) {
        System.out.println("VueApplet: " + s);
        //showStatus("VueApplet: " + s);
    }
    
    @SuppressWarnings("unchecked")
	public static void displayZoteroExport(final String urlString)
    {
    	AccessController.doPrivileged(new PrivilegedAction()
		{
			
			public Object run() {
				try 
				{
				File tempFile = null;
				tempFile = File.createTempFile(new Long(System.currentTimeMillis()).toString(), null);
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
			
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
		    	return null;
			}
			
		});
    	
    }

    public static void displayMap(String urlString)
    {
    	innerDisplayMap(urlString);
    }
    
    @SuppressWarnings("unchecked")
	private static void innerDisplayMap(final String urlString)
    {
    
	
			AccessController.doPrivileged(new PrivilegedAction()
			{
				
				public Object run() {
					try 
					{
					File tempFile = null;
					tempFile = File.createTempFile(new Long(System.currentTimeMillis()).toString(), null);
			    	InputStream io = null;

			    	io = getInputStream(urlString);    	
			    	byte[] buf = new byte[256];
			        int read = 0;
			    	java.io.FileOutputStream fos = null;
					fos = new java.io.FileOutputStream(tempFile);

			    	while ((read = io.read(buf)) > 0) {
					     fos.write(buf, 0, read);
					 }
			    	tufts.vue.action.OpenAction.displayMap(tempFile);
				
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
			    	return null;
				}
				
			});
		
		
    	
    }
    
    public static InputStream getInputStream(String fileName)
	throws IOException
{
	InputStream input;

	if (fileName.startsWith("http:"))
	{
		URL url = new URL( fileName );
		URLConnection connection = url.openConnection();
		input = connection.getInputStream();
	}
	else
	{
		input = new FileInputStream( fileName );
	}

	return input;
}


    
    protected static MapViewer getMapViewer() {
        
        //VUE.installExampleMap(map);

        return viewer;
    }
    
    protected static MapTabbedPane getMapTabbedPane() {
        
        //VUE.installExampleMap(map);

        return mMapTabbedPane;
    }
    
        

}