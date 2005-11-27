package tufts.vue.gui;

import tufts.vue.VUE;
import tufts.vue.MapViewer;
import tufts.vue.MapViewerEvent;
import tufts.vue.VueResources;
import tufts.vue.DEBUG;

import java.awt.event.WindowEvent;

/**
 * The top-level VUE application Frame.
 *
 * Set's the icon-image for the vue application and set's the window title.
 *
 * @version $Revision: 1.1 $ / $Date: 2005-11-27 16:15:38 $ / $Author: sfraize $ 
 */
public class VueFrame extends javax.swing.JFrame
//public class VueFrame extends com.jidesoft.docking.DefaultDockableHolder
//public class VueFrame extends com.jidesoft.action.DefaultDockableBarDockableHolder // JIDE ENABLE
    implements MapViewer.Listener //, MouseWheelListener
{
    final static int TitleChangeMask =
        MapViewerEvent.DISPLAYED |
        MapViewerEvent.FOCUSED;
    //MapViewerEvent.ZOOM;        // title includes zoom
        
    public VueFrame() {
        super(VueResources.getString("application.title"));
        setIconImage(VueResources.getImageIcon("vueIcon32x32").getImage());
        // JIDE ENABLE getDockableBarManager().getMainContainer().setLayout(new BorderLayout());            
        //addMouseWheelListener(this); this causing stack overflows in JVM 1.4 & 1.5, and only works for unclaimed areas
        // (e.g., not mapviewer, even if it hasn't registered a wheel listener)
    }

    //public void mouseWheelMoved(MouseWheelEvent e) { System.err.println("VUE MSW"); }
        
    protected void processEvent(java.awt.AWTEvent e) {
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
            VUE.Log.warn("VueFrame: processEvent: root != main: forcing root visible & front");
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
    public void setVisible(boolean tv) {
        //System.out.println("VueFrame setVisible " + tv + " OVERRIDE");
            
        // The frame should never be "hidden" -- iconification
        // doesn't trigger that (nor Mac os "hide") -- so if we're
        // here the OS window manager is attempting to hide us
        // (the 'x' button on the window frame).
            
        //super.setVisible(true);
        super.setVisible(tv);
    }
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0)
            setTitleFromViewer(e.getMapViewer());
    }
        
    private void setTitleFromViewer(MapViewer viewer) {
        String title = VUE.NAME + ": " + viewer.getMap().getLabel();
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
}
    
