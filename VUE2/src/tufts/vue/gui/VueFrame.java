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
package tufts.vue.gui;

import tufts.vue.VUE;
import tufts.vue.MapViewer;
import tufts.vue.ActiveListener;
import tufts.vue.ActiveEvent;
import tufts.vue.VueResources;
import tufts.vue.DEBUG;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;

import javax.swing.SwingUtilities;

import edu.tufts.vue.preferences.implementations.WindowPropertiesPreference;


/**
 * The top-level VUE application Frame.
 *
 * Set's the icon-image for the vue application and set's the window title.
 *
 * @version $Revision: 1.25 $ / $Date: 2009-11-04 18:44:20 $ / $Author: mike $ 
 */
public class VueFrame extends javax.swing.JFrame
    implements ActiveListener<MapViewer>, WindowListener, WindowStateListener, WindowFocusListener               
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueFrame.class);
    
    private static int sNameIndex = 0;
    private WindowPropertiesPreference wpp = null;
    
    public VueFrame() {
        this(VueResources.getString("application.title"));
    }
    
    public VueFrame(String title) {
        super(title);
        
        wpp = WindowPropertiesPreference.create(
        		"windows",
        		"windowVueMainFrame",
        		//"window" + title.replace(" ", ""),
        		VueResources.getString("preferences.savewindowstates.title"), 
        		VueResources.getString("prefernces.savewindowstates.description"),
        		true);
        
        this.setDefaultCloseOperation(this.DO_NOTHING_ON_CLOSE);
        setName("VueFrame" + sNameIndex++);
        GUI.setRootPaneNames(this, getName());
        if (GUI.isMacAqua()) {
            // note that this actually has no effect on MacOSX
            setIconImage(VueResources.getImage("vueIcon128"));
        } else {
            setIconImage(VueResources.getImage("vueIcon32"));
        }
        if (GUI.ControlMaxWindow)
            setMaximizedBounds(GUI.getMaximumWindowBounds());

        // we need this to make sure kdb input
        //setJMenuBar(new VueMenuBar());        
        
        // JIDE ENABLE getDockableBarManager().getMainContainer().setLayout(new BorderLayout());            
        //addMouseWheelListener(this); this causing stack overflows in JVM 1.4 & 1.5, and only works for unclaimed areas
        // (e.g., not mapviewer, even if it hasn't registered a wheel listener)

        addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentMoved(ComponentEvent e) {
                    //out("MOVED " + e);
                    GUI.refreshGraphicsInfo();
                }
            });
        

        addWindowListener(this);
        addWindowStateListener(this);
        addWindowFocusListener(this);
        
        VUE.addActiveListener(MapViewer.class, this);
    }


    public void windowOpened(WindowEvent e) {
        Log.debug("opened; " + e);
    }

    public void windowClosing(WindowEvent e) {
        Log.warn(e);
        tufts.vue.action.ExitAction.exitVue();

        // If we get here, it means the exit was aborted by the user (something
        // wasn't saved & they decided to cancel or there was an error during
        // the save)

        //frame.show(); (doesn't work)  How to cancel this windowClose?  According
        // to WindowEvent.java & WindowAdapter.java, canceling this
        // windowClosing is supposed to be possible, but they don't mention
        // how. Anyway, we've overriden setVisible on VueFrame to make it
        // impossible to hide it, and that works, so this event just becomes the
        // they've pressed on the close button event.
    }
                
    public void windowClosed(WindowEvent e) {
        // I've never see us even get this event...
        Log.fatal("Too late: window disposed: exiting. " + e);
        System.exit(-1);
    }
    
    public void windowStateChanged(WindowEvent e) {
        Log.debug("windowStateChanged: " + e);
        if (VUE.getActiveViewer() != null) 
            VUE.getActiveViewer().setFastPaint("windowStateChanged");
    }
    
    public void windowActivated(WindowEvent e) {
        if (DEBUG.FOCUS) Log.debug("activated; " + e);
        if (VUE.getActiveViewer() != null) 
            VUE.getActiveViewer().setFastPaint("windowActivated");
        DockWindow.ShowPreviouslyHiddenWindows();
        if (LastOpenedResource != null) {
            try {
                if (DEBUG.Enabled) Log.debug("resource check: " + LastOpenedResource);
                tufts.vue.VUE.checkForAndHandleResourceUpdate(LastOpenedResource);
                        
                // TODO: skip clearing to null to handle repeated editing
                // bad idea of now until this is threaded, as if happened to
                // be local network resource that went offline, we could
                // hang -- so at least this would only happen the first time.
                // Once this is in a thread, the check could just run entirely
                // at low priority in the background checking everything once
                // VUE gains focus again.
                LastOpenedResource = null; 

            } catch (Throwable t) {
                Log.error("resource check: " + LastOpenedResource, t);
                LastOpenedResource = null;
            }
        }
    }

    public void windowGainedFocus(WindowEvent e) {
        if (DEBUG.FOCUS) Log.debug("focus-gained; " + e);
    }
    public void windowLostFocus(WindowEvent e) {
        if (DEBUG.FOCUS) Log.debug("focus-lost; " + e);
        MapViewer v = VUE.getActiveViewer();
        if (v !=null)
        {
        	v.clearTip();
        }
    }
    
    public void windowDeactivated(WindowEvent e) {
        if (DEBUG.FOCUS) Log.debug("deactivated; " + e);
        MapViewer v = VUE.getActiveViewer();
        if (v !=null)
        {
        	v.clearTip();
        } 
//         if (!tufts.Util.isUnixPlatform()) {
//             // this causes VueFrame de-activate/activate loop on Ubuntu 8.04 / JVM 1.6
//             // Works nicely on Leopard & XP, but may not be what we want: a user
//             // might want to drag text into notes panel, which would be impossible
//             // if we do this.  Could make this a preference.
//             DockWindow.DeactivateAllWindows();
//         }

    }


    public void windowIconified(WindowEvent e) {
        Log.debug("iconfied; " + e);
    }
    public void windowDeiconified(WindowEvent e) {
        Log.debug("de-iconfied; " + e);
    }
    

    private static tufts.vue.Resource LastOpenedResource;
    
    public static void setLastOpenedResource(tufts.vue.Resource r) {
        LastOpenedResource = r;
    }

    public void activeChanged(ActiveEvent<MapViewer> e) {
        setTitleFromViewer(e.active);
        
        if (tufts.Util.isMacPlatform()) {
            
            if (e.active != null && e.active.getMap() != null) {
                
                final tufts.vue.LWMap map = e.active.getMap();
                final javax.swing.JRootPane rootPane = getRootPane();
                    
                // If the below documentModified is TRUE, the icon in the title-bar will appear, but
                // will be grayed out (as the file is considered to be in an indeterminate state).
                // Note that putting null values for client properties removes the property completely.

                // These client properties only have effect in Java 1.5 on Mac OS X Leopard:
                
                rootPane.putClientProperty("Window.documentFile", map.getFile());
            
                //if (tufts.Util.isMacLeopard()) {

                if (DEBUG.Enabled)  {
                    // will also need this update on ANY change to the map's status (from either edits or undo's)
                    // in order to keep this up to date.
                    
                    // Technically, I'm guessing that the Mac guidelines would say this dirty bit should be
                    // set if ANY open document is modified (as it's feedback that says if you click on the
                    // dirty red close icon, you're going to get dialogs to confirm this).  Anyway,
                    // for the moment it's more handy if it displays for the active viewer, and the user
                    // is going to be asked anyway.
                    rootPane.putClientProperty("Window.documentModified", Boolean.valueOf(map.isModified()));
                }
            }
        }
    }
    
    /*
    final static int TitleChangeMask =
        MapViewerEvent.DISPLAYED |
        MapViewerEvent.FOCUSED;
    //MapViewerEvent.ZOOM;        // title includes zoom
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0)
            setTitleFromViewer(e.getMapViewer());
    }
    */
    
    public boolean isPointFullyOnScreen(Point p, Dimension size)
    {
    	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    	int rightCorner =  (int)p.getX() + (int)size.getWidth();
    	int bottomCorner = (int)p.getY() + (int)size.getHeight();
    	
    	if ((rightCorner <= screenSize.getWidth()) && (bottomCorner <= screenSize.getHeight()))
    		return true;
    	else 
    		return false;
    }
    
    public void saveWindowProperties()
    {   
     	Dimension size = getSize();
     	Point p;     	

   		p = getLocationOnScreen();

     	wpp.updateWindowProperties(true, (int)size.getWidth(), (int)size.getHeight(), (int)p.getX(), (int)p.getY(),false);
    }

    public void positionWindowFromProperties()
    {
    	if (wpp.isEnabled())
    	{
    		Point p = wpp.getWindowLocationOnScreen();
    		Dimension size = wpp.getWindowSize();
    		
    		if (((int)p.getX()) > -1)
    			setBounds((int)p.getX(),(int)p.getY(),(int)size.getWidth(),(int)size.getHeight());	
    		else
    			setSize(size);
    		
    		setVisible(wpp.isWindowVisible());
    	}    	
    }
    public WindowPropertiesPreference getWindowProperties()
    {
    	return wpp;
    }
    
    public void setMaximizedBounds(Rectangle r) {
        if (DEBUG.INIT) out("SETMAX " + r);
        super.setMaximizedBounds(r);
    }
    public void setVisible(boolean visible) {
        if (DEBUG.INIT) out("SET-VISIBLE " + visible);
        super.setVisible(visible);
    }
    public void XsetExtendedState(int state) {
        out("SET-STATE: " + state + (state == MAXIMIZED_BOTH ? " MAX" : " other"));
        super.setExtendedState(state);
    }
    public void XsetLocation(int x, int y) {
        out("setLocation: " + x + "," + y);
        super.setLocation(x, y);
    }
    
    //public void mouseWheelMoved(MouseWheelEvent e) { System.err.println("VUE MSW"); }
        
    protected void X_processEvent(java.awt.AWTEvent e) {
        // if (e instanceof MouseWheelEvent) System.err.println("VUE MSM PE"); only works w/listener, which has problem as above
        // try a generic AWT event queue listener?
            
        if (e instanceof java.awt.event.MouseEvent) {
            super.processEvent(e);
            return;
        }
        
        if (DEBUG.FOCUS) System.out.println("VueFrame: processEvent " + e);
        // todo: if frame is getting key events, handle them
        // and/or pass off to MapViewer (e.g., tool switch events!)
        // or: put tool's w/action events in vue menu
        if (e instanceof WindowEvent) {
            switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING:
            case WindowEvent.WINDOW_CLOSED:
            case WindowEvent.WINDOW_ICONIFIED:
                //case WindowEvent.WINDOW_DEACTIVATED:
                super.processEvent(e);
                return;
                /*
                  case WindowEvent.WINDOW_ACTIVATED:
                  tufts.macosx.Screen.dumpWindows();
                  case WindowEvent.WINDOW_OPENED:
                  case WindowEvent.WINDOW_DEICONIFIED:
                  case WindowEvent.WINDOW_GAINED_FOCUS:
                  if (VUE.getRootWindow() != VUE.getMainWindow())
                  VUE.getRootWindow().toFront();
                */
            }
        }

        // why do we do this?  Must have to do with full-screen or something...
        if (VUE.getRootWindow() != VUE.getMainWindow()) {
            Log.warn("VueFrame: processEvent: root != main: forcing root visible & front");
            VUE.getRootWindow().setVisible(true);
            VUE.getRootWindow().toFront();
        }
        super.processEvent(e);
    }

    public void addComp(java.awt.Component c, String constraints) {
        getContentPane().add(c, constraints);
        // JIDE ENABLE getDockableBarManager().getMainContainer().add(c, constraints);
    }

    /*
      public void show() {
      out("VueFrame: show");
      super.show();
      pannerTool.toFront();
      }
      public void toFront()
      {
      //if (DEBUG.FOCUS)
      out("VueFrame: toFront");
      super.toFront();
      }
    */

    /** never let the frame be hidden -- always ignored */
    public void X_setVisible(boolean tv) {
        //System.out.println("VueFrame setVisible " + tv + " OVERRIDE");
            
        // The frame should never be "hidden" -- iconification
        // doesn't trigger that (nor Mac os "hide") -- so if we're
        // here the OS window manager is attempting to hide us
        // (the 'x' button on the window frame).
            
        //super.setVisible(true);
        super.setVisible(tv);
    }
        
    private void setTitleFromViewer(MapViewer viewer) {
        String title = VUE.getName();
        final tufts.vue.LWMap map = viewer == null ? null : viewer.getMap();
        if (map != null) {	
            if (DEBUG.Enabled) title += ("[" + map.getSaveFileModelVersion() + "]");
            title += ": " + map.getLabel();
        }
        //if (viewer.getMap().isCurrentlyFiltered())
        // will need to listen to map for filter change state or this gets out of date
        //    title += " (Filtered)";
        setTitle(title);
        //setTitle("VUE: " + getViewerTitle(viewer));
    }
        
    private String getViewerTitle(MapViewer viewer) {
        String title = viewer.getMap().getLabel();
            
        int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
        // Present the zoom factor as a percentange
        // truncated down to 2 digits
        title += " (";
        if ((displayZoom / 100) * 100 == displayZoom)
            title += (displayZoom / 100) + "%";
        else
            title += (((float) displayZoom) / 100f) + "%";
        title += ")";
        return title;
    }

    public void setTitle(String title)
    {
       if (title.endsWith(".vpk") || title.endsWith(".vue"))
    	   super.setTitle(title);
    }
    private void out(String s) {
        System.out.println("VueFrame: " + s);
     
    }

    public String toString() {
        return "VueFrame[" + getTitle() + "]";
    }
}
    
