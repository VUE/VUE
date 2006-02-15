/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.gui.GUI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.border.*;

/**
 * Code for handling a tabbed pane of MapViewer's: adding, removing,
 * keeping tab labels current & custom appearance tweaks.
 *
 * @version $Revision: 1.27 $ / $Date: 2006-02-15 17:50:37 $ / $Author: sfraize $ 
 */

// todo: need to figure out how to have the active map grab
// the focus if no other map has focus: switching tabs
// changes the map you're looking it, and it's set to
// the active map, but it doesn't get focus unless you click on it!
public class MapTabbedPane extends JTabbedPane
    implements LWComponent.Listener, FocusListener, MapViewer.Listener
{
    private Color BgColor;
    private String name;
    
    MapTabbedPane(String name) {
        this.name = name;
        setName("mapTabs-" + name);
        setFocusable(false);
        BgColor = GUI.getToolbarColor();
        setTabPlacement(javax.swing.SwingConstants.TOP);
        setPreferredSize(new Dimension(300,400));
    }
        
    private int mWasSelected = -1;
    protected void fireStateChanged() {
        try {
            if (DEBUG.FOCUS) System.out.println(this + " fireStateChanged");
            super.fireStateChanged();
        } catch (ArrayIndexOutOfBoundsException e) {
            // this is happening after we close everything and then
            // open another map -- no idea why, but this successfully
            // ignores it.
            System.err.println(this + " JTabbedPane.fireStateChanged: " + e);
        }
        if (!GUI.isMacAqua()) { // don't mess w/aqua
            int selected = getModel().getSelectedIndex();
            if (mWasSelected >= 0) {
                setForegroundAt(mWasSelected, Color.darkGray);
            }
            if (selected >= 0) {
                setForegroundAt(selected, Color.black);
                setBackgroundAt(selected, BgColor);
            }
            mWasSelected = selected;
            Component viewer = getSelectedViewer();
            if (viewer != null)
                viewer.requestFocus();
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

    public MapViewer getSelectedViewer() {
        return getViewerAt(getSelectedIndex());
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
        //addFocusListener(this); // hope not to hear anything...
        // don't let us be focusable or sometimes you can select
        // & activate a new map for interaction, but we keep
        // the focus here in the tabbed instead of giving to
        // the component in the tab.
        //setFocusable(false); // in constructor
    }
        
    private String mapToTabTitle(LWMap map) {
        String title = map.getLabel();
        if (title.toLowerCase().endsWith(".vue") && title.length() > 4)
            title = title.substring(0, title.length() - 4);
        if (map.isCurrentlyFiltered())
            title += "*";
        return title;
    }

    private String viewerToTabTitle(MapViewer viewer) {
        String title = mapToTabTitle(viewer.getMap());

        // Present the zoom factor as a percentange
        
        title += " (";
        double zoomPct = viewer.getZoomFactor() * 100;
        if (zoomPct < 10) {
            // if < 10% zoom, show with 1 digit of decimal value if it would be non-zero
            title += VueUtil.oneDigitDecimal(zoomPct);
        } else {
            //title += (int) Math.round(zoomPct);
            title += (int) Math.floor(zoomPct + 0.49);
        }
        return title + "%)";
    }

    private void updateTitleTextAt(int i) {
        if (i >= 0) {
            MapViewer viewer = getViewerAt(i);
            setTitleAt(i, viewerToTabTitle(viewer));
            LWMap map = viewer.getMap();
            String tooltip = null;
            if (map.getFile() != null)
                tooltip = map.getFile().toString();
            else
                tooltip = "(Unsaved)";
            if (map.isCurrentlyFiltered())
                tooltip += " (filtered)";
            setToolTipTextAt(i, tooltip);
        }
    }
    
    static final int TitleChangeMask =
        MapViewerEvent.DISPLAYED |
        MapViewerEvent.FOCUSED |
        MapViewerEvent.ZOOM;        // title includes zoom

    private static MapTabbedPane lastFocusPane;
    private static int lastFocusIndex = -1;
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0) {
            int i = indexOfComponent(e.getMapViewer());
            if (i >= 0) {
                updateTitleTextAt(i);
                if (e.getID() == MapViewerEvent.FOCUSED) {
                    if (lastFocusPane != null) {
                        //lastFocusPane.setIconAt(lastFocusIndex, null);
                        lastFocusPane = null;
                    }
                    if (VUE.multipleMapsVisible()) {
                        lastFocusPane = this;
                        lastFocusIndex = i;
                        //setIconAt(i, new BlobIcon(5,5, Color.green));
                    }
                }
            }
        }
    }
    
    public void LWCChanged(LWCEvent e) {
        if (e.getSource() instanceof LWMap)
            updateTitleTextAt(findTabWithMap((LWMap)e.getSource()));
    }
        
    public void addViewer(MapViewer viewer) {
        
        Component c = new tufts.vue.gui.MapScrollPane(viewer);
        
        if (false) {
            String tabTitle = viewerToTabTitle(viewer);
            if (tabTitle == null)
                tabTitle = "unknown";
            System.out.println("Adding tab '" + tabTitle + "' component=" + c);
            addTab(tabTitle, c);
        } else {
            addTab(viewerToTabTitle(viewer), c);
        }
        
        LWMap map = viewer.getMap();
        map.addLWCListener(this, new Object[] { LWKey.MapFilter, LWKey.Label } );
        // todo perf: we should be able to ask to listen only
        // for events from this object directly (that we don't
        // care to hear from it's children)
        updateTitleTextAt(indexOfComponent(c)); // first time just needed for tooltip
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
        

    /**
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
        Object c;
        try {
            c = getComponentAt(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
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
        if (viewer == null && VUE.inFullScreen()) // hack, but works for now: todo: cleaner
            return VUE.getActiveMap();
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
