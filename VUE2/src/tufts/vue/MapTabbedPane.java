package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

/**
 * Code for handling a tabbed pane of MapViewer's: adding, removing,
 * keeping tab labels current & custom appearance tweaks.
 */

public class MapTabbedPane extends JTabbedPane
    implements LWComponent.Listener, FocusListener, MapViewer.Listener
{
    final int TitleChangeMask =
        MapViewerEvent.DISPLAYED |
        MapViewerEvent.FOCUSED |
        MapViewerEvent.ZOOM;        // title includes zoom
    
    private static final Color BgColor = VueResources.getColor("toolbar.background");
        
    private String name;
    MapTabbedPane(String name) {
        this.name = name;
    }
        
    private int mWasSelected = -1;
    protected void fireStateChanged() {
        try {
            super.fireStateChanged();
        } catch (ArrayIndexOutOfBoundsException e) {
            // this is happening after we close everything and then
            // open another map -- no idea why, but this successfully
            // ignores it.
            System.err.println(this + " JTabbedPane.fireStateChanged: " + e);
        }
        if (!VueUtil.isMacAquaLookAndFeel()) { // don't mess w/aqua
            int selected = getModel().getSelectedIndex();
            if (mWasSelected >= 0) {
                setForegroundAt(mWasSelected, Color.darkGray);
            }
            if (selected >= 0) {
                setForegroundAt(selected, Color.black);
                setBackgroundAt(selected, BgColor);
            }
            mWasSelected = selected;
        }
    }
        
    public void reshape(int x, int y, int w, int h) {
        boolean ignore =
            getX() == x &&
            getY() == y &&
            getWidth() == w &&
            getHeight() == h;
            
        // if w or h <= 0 we can know we're being hidden
        //System.out.println(this + " reshape " + x + "," + y + " " + w + "x" + h + (ignore?" (IGNORING)":""));
        super.reshape(x,y, w,h);
    }
        
    public void focusGained(FocusEvent e) {
        System.out.println(this + " focusGained (from " + e.getOppositeComponent() + ")");
    }
    public void focusLost(FocusEvent e) {
        System.out.println(this + " focusLost (to " + e.getOppositeComponent() + ")");
    }
    public void addNotify() {
        super.addNotify();
        if (!VueUtil.isMacPlatform()) {
            setForeground(Color.darkGray);
            setBackground(BgColor);
        }
        addFocusListener(this); // hope not to hear anything...
        // don't let us be focusable or sometimes you can select
        // & activate a new map for interaction, but we keep
        // the focus here in the tabbed instead of giving to
        // the component in the tab.
        setFocusable(false);
    }
        

    /*
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
    */

    private String mapToTabTitle(LWMap map) {
        String title = map.getLabel();
        if (title.toLowerCase().endsWith(".vue") && title.length() > 4)
            title = title.substring(0, title.length() - 4);
        return title;
    }
    
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0)
            ;//setTitleFromViewer(e.getMapViewer());
    }
    
    public void LWCChanged(LWCEvent e) {
        Object src = e.getSource();
        if (src instanceof LWMap && e.getWhat() == LWKey.Label) {
            //System.out.println("MapTabbedPane " + e);
            LWMap map = (LWMap) src;
            int i = findTabWithMap(map);
            if (i >= 0) {
                setTitleAt(i, mapToTabTitle(map));
                if (map.getFile() != null)
                    setToolTipTextAt(i, map.getFile().toString());
            }
        }
    }
        
    public void addViewer(MapViewer viewer) {
        Component c = viewer;
        if (!this.name.startsWith("*"))
            c = new JScrollPane(viewer);
        LWMap map = viewer.getMap();
        addTab(mapToTabTitle(map), c);
        map.addLWCListener(this);
        // todo perf: we should be able to ask to listen only
        // for events from this object directly (that we don't
        // care to hear from it's children), and even that
        // we'd only like to see, e.g., LABEL events.
        // -- create bit masks in LWCEvent
        if (map.getFile() != null)
            setToolTipTextAt(indexOfComponent(c), map.getFile().toString());
    }
        
    /*
    // put BACKINGSTORE mode on a diag switch and test
    // performance difference -- the obvious difference is
    // vastly better performance if an inspector window is
    // obscuring any part of the canvas (or any other window
    // for that mater), which kills off a huge chunk of
    // BLIT_SCROLL_MODE's optimization.  However, using
    // backing store completely fucks up if we start
         
    // hand-panning the map, tho I'm presuming that's because
    // the hand panning isn't being done thru the viewport
    // yet.
    //
    //sp.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
         
    public void addTab(LWMap pMap, Component c)
    {
    //scroller.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
    //super.addTab(pMap.getLabel(), c instanceof JScrollPane ? c : new JScrollPane(c));
    super.addTab(pMap.getLabel(), c);
    pMap.addLWCListener(this);
    if (pMap.getFile() != null)
    setToolTipTextAt(indexOfComponent(c), pMap.getFile().toString());
    }
    */
        

    /*
     * Will find either the component index (default superclass
     * behavior), or, if the component found at any location
     * is a JScrollPane, look within it at the JViewport's
     * view, and if it matches the component sought, return that index.
     */
        
    public int indexOfComponent(Component component) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if ((c != null && c.equals(component)) ||
                (c == null && c == component)) {
                return i;
            }
            if (c instanceof JScrollPane) {
                if (component == ((JScrollPane)c).getViewport().getView())
                    return i;
            }
        }
        return -1;
    }
        
    public MapViewer getViewerAt(int index) {
        Object c = getComponentAt(index);
        MapViewer viewer = null;
        if (c instanceof MapViewer)
            viewer = (MapViewer) c;
        else if (c instanceof JScrollPane)
            viewer = (MapViewer) ((JScrollPane)c).getViewport().getView();
        return viewer;
    }
        
    public LWMap getMapAt(int index) {
        MapViewer viewer = getViewerAt(index);
        LWMap map = null;
        if (viewer != null)
            map = viewer.getMap();
        //System.out.println(this + " map at index " + index + " is " + map);
        return map;
    }
        
    private int findTabWithMap(LWMap map) {
        int tabs = getTabCount();
        for (int i = 0; i < tabs; i++) {
            LWMap m = getMapAt(i);
            if (m != null && m == map) {
                //System.out.println(this + " found map " + map + " at index " + i);
                return i;
            }
        }
        System.out.println(this + " failed to find map " + map);
        return -1;
    }
        
    public void closeMap(LWMap map) {
        System.out.println(this + " closing " + map);
        int mapTabIndex = findTabWithMap(map);
        MapViewer viewer = getViewerAt(mapTabIndex);
        if (viewer == VUE.getActiveViewer()) {
            // be sure to clear active viewer -- it was probably us.
            // If there are other maps open, one of them will shortly get a
                
            VUE.setActiveViewer(null);
            // we might want to force notification even if selection is already empty:
            // we want all listeners, particularly the actions, to
            // update in case this is last map open
            VUE.getSelection().clear();
                
                
            if (mapTabIndex >= 1)
                VUE.setActiveViewer(getViewerAt(mapTabIndex - 1));
            else if (getTabCount() > 1)
                VUE.setActiveViewer(getViewerAt(mapTabIndex + 1));
            else
                VUE.setActiveViewer(null); // TODO: this really isn't supported... NPE's everywhere.
            //VUE.getSelection().clearAndNotify();
        }
        remove(mapTabIndex);
    }
        
    public void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                         java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }
        
    public String toString() {
        return "MapTabbedPane<"+name+">";
    }
        
}
