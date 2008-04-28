/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import tufts.vue.gui.GUI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.border.*;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Code for handling a tabbed pane of MapViewer's: adding, removing,
 * keeping tab labels current & custom appearance tweaks.
 *
 * @version $Revision: 1.47 $ / $Date: 2008-04-28 04:05:56 $ / $Author: sfraize $ 
 */

// todo: need to figure out how to have the active map grab
// the focus if no other map has focus: switching tabs
// changes the map you're looking it, and it's set to
// the active map, but it doesn't get focus unless you click on it!
public class MapTabbedPane extends JTabbedPane
    implements LWComponent.Listener, FocusListener, MapViewer.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapTabbedPane.class);
    
    private final String name;
    private final Color BgColor;

//     //private final boolean isLeftViewer;
//     private static MapTabbedPane leftTabs;
//     private static MapTabbedPane rightTabs;
    
    MapTabbedPane(String name, boolean isLeft) {
        this.name = name;
//         //this.isLeftViewer = isLeft;
//         if (isLeft)
//             leftTabs = this;
//         else
//             rightTabs = this;
        setName("mapTabs-" + name);
        setFocusable(false);
        BgColor = GUI.getToolbarColor();
        setTabPlacement(javax.swing.SwingConstants.TOP);
        //if (DEBUG.Enabled)
        //setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT); // appears to have no effect in Aqua on Mac
        setPreferredSize(new Dimension(300,400));
        //VUE.addActiveListener(LWMap.class, this);

        /*//getModel().
        addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    if (DEBUG.Enabled) out("stateChanged: selectedIndex=" + getSelectedIndex());
                }
                });*/
    }

//     public void activeChanged(ActiveEvent e, LWMap map) {
//         if (!isLeftViewer || e.hasSourceOfType(MapTabbedPane.class)) {
//             // ignore change events from the other tab-pane
//             return;
//         }
//         int mapIndex = findTabWithMap(map);
//         if (mapIndex >= 0)
//             setSelectedIndex(mapIndex);
//     }
        
    private int mWasSelected = -1; // non-aqua use only
    @Override
    protected void fireStateChanged() {
        try {
            if (DEBUG.FOCUS) out("fireStateChanged, selectedIndex=" +getSelectedIndex() + "; viewerAtIndex=" + getViewerAt(getSelectedIndex()));;
            super.fireStateChanged();
        } catch (ArrayIndexOutOfBoundsException e) {
            // this is happening after we close everything and then
            // open another map -- no idea why, but this successfully
            // ignores it.
            System.err.println(this + " JTabbedPane.fireStateChanged: " + e);
        }
        
        
        if (GUI.isMacAqua() == false) { 
            int selected = getModel().getSelectedIndex();

            // for non-aqua UI's, we change the selected tab color
            
            if (mWasSelected >= 0)
                setForegroundAt(mWasSelected, Color.darkGray);
            
            if (selected >= 0) {
                setForegroundAt(selected, Color.black);
                setBackgroundAt(selected, BgColor);
            }

            mWasSelected = selected;
            
        }
    }

    @Override
    public void setSelectedIndex(final int index) {

//         if (index < 0) {
//             Log.debug("invalid tab index: " + index);
//             return;
//         }
        
	super.setSelectedIndex(index);
        
        final MapViewer viewer = getViewerAt(index);
        if (viewer != null /*&& !VUE.isStartupUnderway()*/) {
            if (DEBUG.FOCUS) out("ATTEMPTING FOCUS TRANSFER TO " + viewer);
            
            // For some reason, that I think has to do with having lots of maps open and
            // using the drop-down menu for selected an open map (on the mac), focus
            // diagnostics show what is presumably the menu going hidden (a pop-up
            // heavy-weight window anyway) after a menu item is selected, and then after
            // this, the OLD viewer in the de-activating tab-pane first gets the focus
            // back, even if we have the newly selected viewer request focus here.  By
            // putting the focus request at the end of the AWT event queue, all this
            // crap can happen, and then we can change the focus like we want.

            GUI.invokeAfterAWT(new Runnable() { public void run() {
                if (DEBUG.FOCUS) out("after AWT focus jibber-jabber (now at end of AWT event queue), focus is being demanded by: " + viewer);

                //viewer.requestFocus(); // do NOT use this, as the viewers may be temporaily, deliberately, unfocusable while we re-route focus

                // this can work (viewer can listen for itself going active and grab), but
                // we'd need to disable focusability on the viewer in the non active tab
                // pane first, as it's still grabbing focus back.
                //VUE.setActive(MapViewer.class, this, viewer); 

                // We must call this directly as it will ensure focusability has been restored
                // on the viewer, as new viewers have this turned off by default initially,
                // so we can direct focus as desired (even non-visible right-viewers will
                // delightfully grab the focus when created if you let them).
                viewer.grabVueApplicationFocus(toString() + ".setSelectedIndex="+index + " " + viewer, null);
            }});
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

    public void advanceToNextTab() {
        if (getSelectedIndex() >= getTabCount() - 1)
            setSelectedIndex(0);
        else
            setSelectedIndex(getSelectedIndex() + 1);
    }

    public void focusGained(FocusEvent e) {
        out("focusGained (from " + e.getOppositeComponent() + ")");
    }
    public void focusLost(FocusEvent e) {
        out("focusLost (to " + e.getOppositeComponent() + ")");
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

        if (DEBUG.WORK)
            return mapToTabTitle(viewer.getMap());
        else
            return mapToTabTitle(viewer.getMap()) + " (" + ZoomTool.prettyZoomPercent(viewer.getZoomFactor()) + ")";
        /*
        
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
        */
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
                tooltip = "<html>&nbsp;<i>Unsaved</i> &nbsp;";
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
        map.addLWCListener(this, LWKey.MapFilter, LWKey.Label);
        // todo perf: we should be able to ask to listen only
        // for events from this object directly (that we don't
        // care to hear from it's children)
        updateTitleTextAt(indexOfComponent(c)); // first time just needed for tooltip
    }
        
    /*
    // put BACKINGSTORE mode on a diag switch and test performance difference -- the
    // obvious difference is vastly better performance if an inspector window is
    // obscuring any part of the canvas (or any other window for that mater), which
    // kills off a huge chunk of BLIT_SCROLL_MODE's optimization.  However, using
    // backing store completely fucks up if we start hand-panning the map, tho I'm
    // presuming that's because the hand panning isn't being done thru the viewport yet.
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
//         if (viewer == null && VUE.inFullScreen()) // hack, but works for now: todo: cleaner
//             return VUE.getActiveMap();
        if (viewer != null)
            map = viewer.getMap();
        //System.out.println(this + " map at index " + index + " is " + map);
        return map;
    }
    
    public Iterator<LWMap> getAllMaps() {
        int tabs = getTabCount();
        ArrayList<LWMap> list = new ArrayList();
        for(int i= 0;i< tabs;i++){
            LWMap m = getMapAt(i);
            list.add(m);
        }
        return list.iterator();
    }

    public MapViewer getViewerWithMap(LWMap map) {
        return getViewerAt(findTabWithMap(map));
    }

    public void setSelectedMap(LWMap map) {
        setSelectedIndex(findTabWithMap(map));
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
        Log.error(this + ": failed to find map " + map + " at any index");
        return -1;
    }

    public void closeMap(LWMap map) {
        if (DEBUG.FOCUS) out("closeMap " + map);
        
        int mapTabIndex = findTabWithMap(map);
        MapViewer viewer = getViewerAt(mapTabIndex);

        if (DEBUG.FOCUS) out("closeMap"
                             + "\n\t   indexOfMap=" + mapTabIndex
                             + "\n\tviewerAtIndex=" + viewer
                             + "\n\t activeViewer=" + VUE.getActiveViewer());
        
        // Note: if we close out the last tab (while it's selected), the selected index
        // must change, and the JTabbedPane acts sanely, delivers change events, and
        // causes the MapViewer in the previously second-to-last-tab, now in the last
        // tab, to gain focus.  However, if any OTHER tab is removed, technically the
        // selected index can (and does) stay the same, which makes sense, except the
        // selected ITEM is now different, and JTabbedPane is completely ignorant of
        // this, and does nothing, and delivers focus to nothing, so we must handle that
        // manually.  This also reveals a weaknes the DefaultSingleSelectionModel, which
        // has no code to deal with the case of a selected item from change out from
        // under the selected index.  So what we need to do is make sure the right
        // MapViewer forcably grabs the focus.
        
        boolean forceFocusTransfer = false;

        if (viewer == VUE.getActiveViewer()) {

            // If this is the active viewer, we may need to manage
            // a focus transfer.
            
            // Apparently, even sometimes when it's the last tab that changes, JTabbedPane fails
            // to tansfer focus, so we do this always...
            //if (mapTabIndex != getTabCount() - 1)
                forceFocusTransfer = true;
                
            // Immediately make sure nothing can refer this this viewer.
            //VUE.setActive(MapViewer.class, this, null);

            // we might want to force notification even if selection is already empty:
            // we want all listeners, particularly the actions, to
            // update in case this is last map open
            VUE.getSelection().clear();
        }

        
        removeTabAt(mapTabIndex);

        if (forceFocusTransfer) {
            int selectedIndex = getSelectedIndex();
            // the newly selected tab doesn't always get the focus:
            if (DEBUG.FOCUS) out("closeMap force focus transfer to new selected tab index: " + selectedIndex);
            if (selectedIndex >= 0)
                getViewerAt(selectedIndex).grabVueApplicationFocus("closeMap", null);
            else
                VUE.setActive(MapViewer.class, this, null); // no open viewers
                
        }
    }
        
    public void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                         java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }
        
    public String toString() {
        return "MapTabbedPane<"+name+">";
    }

    private void out(String s) {
        System.out.println(this + ": " + s);
    }
        
}
