package tufts.vue;

import java.util.Iterator;
import java.awt.Event;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;

/**
 * VUE application actions (file, viewer/component, etc)
 *
 * @author Scott Fraize
 * @version 6/19/03
 */
class Actions {
    static final int META = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
    static final int CTRL = Event.CTRL_MASK;
    static final int SHIFT = Event.SHIFT_MASK;
    
    static final private KeyStroke keyStroke(int vk, int mod) {
        return KeyStroke.getKeyStroke(vk, mod);
    }
    static final private KeyStroke keyStroke(int vk) {
        return keyStroke(vk, 0);
    }

    //-------------------------------------------------------
    // selection actions
    //-------------------------------------------------------
    
    static final Action SelectAll =
        new MapAction("Select All", keyStroke(KeyEvent.VK_A, META)) {
            boolean enabledFor(LWSelection l) { return true; }
            public void act()
            {
                VUE.ModelSelection.setTo(VUE.getActiveViewer().getMap().getChildIterator());
            }
        };
    static final Action DeselectAll =
        new MapAction("Deselect All", keyStroke(KeyEvent.VK_A, SHIFT+META)) {
            boolean enabledFor(LWSelection l) { return l.size() > 0; }
            public void act()
            {
                VUE.ModelSelection.clear();
            }
        };
    static final Action Cut =
        new MapAction("Cut", keyStroke(KeyEvent.VK_X, META)) {
            void Xact(LWComponent c) {
                
            }
        };
    static final Action Copy =
        new MapAction("Copy", keyStroke(KeyEvent.VK_C, META)) {
            void Xact(LWComponent c) {
                
            }
        };
    static final Action Paste =
        new MapAction("Paste", keyStroke(KeyEvent.VK_V, META)) {
            void Xact(LWComponent c) {
                
            }
        };

    //-------------------------------------------------------
    // Group/Ungroup
    //-------------------------------------------------------
    
    static final Action Group =
        new MapAction("Group", keyStroke(KeyEvent.VK_G, META))
        {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection l)
            {
                // enable only when two or more objects in selection,
                // and all share the same parent
                return l.size() >= 2 && l.allHaveSameParent();
            }
            void act(LWSelection selection)
            {
                LWContainer parent = selection.first().getParent(); // all have same parent
                LWGroup group = LWGroup.create(selection);
                parent.addChild(group);
                VUE.ModelSelection.setTo(group);
                // setting selection here is slightly sketchy in that it's
                // really a UI policy that belongs to the viewer
                // todo: could handle in viewer via "created" LWCEvent
            }
        };
    static final Action Ungroup =
        new MapAction("Ungroup", keyStroke(KeyEvent.VK_G, META+SHIFT))
        {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection l)
            {
                return l.countTypes(LWGroup.class) > 0;
            }
            void act(LWComponent c)
            {
                if (c instanceof LWGroup) {
                    System.out.println("dispersing group " + c);
                    ((LWGroup)c).disperse();
                }
            }
        };
    static final Action Rename =
        new MapAction("Rename", keyStroke(KeyEvent.VK_F2))
        {
            boolean enabledFor(LWSelection l)
            {
                return l.size() == 1 && !(l.first() instanceof LWGroup);
            }
            void act(LWComponent c) {
                VUE.getActiveViewer().activateLabelEdit(c);
            }
        };
    static final Action Delete =
        new MapAction("Delete", keyStroke(KeyEvent.VK_DELETE))
        {
            boolean mayModifySelection() { return true; }
            void act(LWComponent c) {
                c.getParent().deleteChild(c);
            }
        };
    
    //-------------------------------------------------------
    // Arrange actions
    //-------------------------------------------------------

    static final MapAction BringToFront =
        new MapAction("Bring to Front",
                      "Raise object to the top, completely unobscured",
                      keyStroke(KeyEvent.VK_CLOSE_BRACKET, META+SHIFT))
        {
            boolean enabledFor(LWSelection l)
            {
                if (l.size() == 1)
                    return !l.first().getParent().isOnTop(l.first());
                return l.size() >= 2;
            }
            void act(LWSelection selection) {
                LWContainer.bringToFront(selection);
                checkEnabled();
            }
            void checkEnabled()
            {
                super.checkEnabled();
                BringForward.checkEnabled();
                SendToBack.checkEnabled();
                SendBackward.checkEnabled();
            }
        };
    static final MapAction SendToBack =
        new MapAction("Send to Back",
                      "Make sure this object doesn't obscure any other object",
                      keyStroke(KeyEvent.VK_OPEN_BRACKET, META+SHIFT))
        {
            boolean enabledFor(LWSelection l)
            {
                if (l.size() == 1)
                    return !l.first().getParent().isOnBottom(l.first());
                return l.size() >= 2;
            }
            void act(LWSelection selection) {
                LWContainer.sendToBack(selection);
                BringToFront.checkEnabled();
            }
        };
    static final MapAction BringForward =
        new MapAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, META))
        {
            boolean enabledFor(LWSelection l) { return BringToFront.enabledFor(l); }
            void act(LWSelection selection) {
                LWContainer.bringForward(selection);
                BringToFront.checkEnabled();
            }
        };
    static final MapAction SendBackward =
        new MapAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, META))
        {
            boolean enabledFor(LWSelection l) { return SendToBack.enabledFor(l); }
            void act(LWSelection selection) {
                LWContainer.sendBackward(selection);
                BringToFront.checkEnabled();
            }
        };
    static final Action NewNode =
        new MapAction("New Node", keyStroke(KeyEvent.VK_N, META))
        {
            LWNode lastNode = null;
            Point lastMousePress = null;
            Point2D lastNodeLocation = null;
            
            boolean enabledFor(LWSelection l) { return true; }
            
            public void actionPerformed(ActionEvent ae)
            {
                System.out.println(ae.getActionCommand());
                // todo: this is where we'll get the active NodeTool
                // and have it create the new node based on it's current
                // settings -- move this logic to NodeTool
                
                MapViewer viewer = VUE.getActiveViewer();
                LWNode node = new LWNode("new node");
                Point mousePress = viewer.getLastMousePoint();
                Point2D newNodeLocation = viewer.screenToMapPoint(mousePress);
                
                if (mousePress.equals(lastMousePress) &&
                    lastNode.getLocation().equals(lastNodeLocation))
                {
                    newNodeLocation.setLocation(lastNodeLocation.getX() + 10,
                                                lastNodeLocation.getY() + 10);
                }
                
                node.setLocation(newNodeLocation);
                viewer.getMap().addNode(node);

                //better: run a timer and do this if no activity (e.g., node creation)
                // for 250ms or something -- todo bug: every other new node not activating label edit
                viewer.paintImmediately(viewer.getBounds());
                viewer.activateLabelEdit(node);

                lastNode = node;
                lastNodeLocation = newNodeLocation;
                lastMousePress = mousePress;
            }
            
        };

        
    abstract static class AlignAction extends MapAction
    {
        static float minX, minY;
        static float maxX, maxY;
        static float centerX, centerY;
        static float totalWidth, totalHeight;
        static boolean allHaveSameX, allHaveSameY;
        // obviously not thread-safe here
        
        private AlignAction(String name) { super(name); }
        boolean enabledFor(LWSelection l) { return l.size() >= 2; }
        void act(LWSelection selection)
        {
            Rectangle2D.Float r = (Rectangle2D.Float) LWMap.getBounds(selection.iterator());
            minX = r.x;
            minY = r.y;
            maxX = r.x + r.width;
            maxY = r.y + r.height;
            centerX = (minX + maxX) / 2;
            centerY = (minY + maxY) / 2;
            Iterator i = selection.iterator();
            totalWidth = totalHeight = 0;
            float x = selection.first().getX();
            float y = selection.first().getY();
            allHaveSameX = allHaveSameY = true;
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.getX() != x)
                    allHaveSameX = false;
                if (c.getY() != y)
                    allHaveSameY = false;
                totalWidth += c.getWidth();
                totalHeight += c.getHeight();
            }
            align(selection);
        }
        void align(LWSelection selection) {
            Iterator i = selection.iterator();
            while (i.hasNext())
                align((LWComponent) i.next());
        }
        void align(LWComponent c) { throw new RuntimeException("unimplemented align action"); }

        LWComponent[] sortByX(LWSelection selection)
        {
            LWComponent[] array = new LWComponent[selection.size()];
            selection.toArray(array);
            return sortByX(array);
        }
        LWComponent[] sortByX(LWComponent[] array)
        {
            java.util.Arrays.sort(array, new java.util.Comparator() {
                    public int compare(Object o1, Object o2) {
                        return (int) (((LWComponent)o1).getX() - ((LWComponent)o2).getX());
                    }});
            return array;
        }
        LWComponent[] sortByY(LWSelection selection)
        {
            LWComponent[] array = new LWComponent[selection.size()];
            selection.toArray(array);
            return sortByY(array);
        }
        LWComponent[] sortByY(LWComponent[] array)
        {
            java.util.Arrays.sort(array, new java.util.Comparator() {
                    public int compare(Object o1, Object o2) {
                        return (int) (((LWComponent)o1).getY() - ((LWComponent)o2).getY());
                    }});
            return array;
        }
    };
        
    private static final Action[] ALIGN_ACTIONS = {
        new AlignAction("Align Left Edges") {
            void align(LWComponent c) { c.setLocation(minX, c.getY()); }
        },
        new AlignAction("Align Right Edges") {
            void align(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
        }
    };
    static final Action AlignLeftEdges = new AlignAction("Align Left Edges") {
            void align(LWComponent c) { c.setLocation(minX, c.getY()); }
        };
    static final Action AlignRightEdges = new AlignAction("Align Right Edges") {
            void align(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
        };
    static final Action AlignTopEdges = new AlignAction("Align Top Edges") {
            void align(LWComponent c) { c.setLocation(c.getX(), minY); }
        };
    static final Action AlignBottomEdges = new AlignAction("Align Bottom Edges") {
            void align(LWComponent c) { c.setLocation(c.getX(), maxY - c.getHeight()); }
        };
    static final Action AlignCentersColumn = new AlignAction("Align Centers in Column") {
            void align(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
        };
    static final Action AlignCentersRow = new AlignAction("Align Centers in Row") {
            void align(LWComponent c) { c.setLocation(c.getX(), centerY - c.getHeight()/2); }
        };
    static final Action DistributeVertically = new AlignAction("Distribute Vertically") {
            //boolean enabledFor(LWSelection l) { return l.size() >= 3; }
            // only 2 in selection is useful with our minimum layout region setting
            void align(LWSelection selection)
            {
                LWComponent[] comps = sortByY(sortByX(selection));
                float layoutRegion = maxY - minY;
                if (layoutRegion < totalHeight)
                    layoutRegion = totalHeight;
                float verticalGap = (layoutRegion - totalHeight) / (selection.size() - 1);
                float y = minY;
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(c.getX(), y);
                    y += c.getHeight() + verticalGap;
                }
            }
        };

    static final Action DistributeHorizontally = new AlignAction("Distribute Horizontally") {
            //boolean enabledFor(LWSelection l) { return l.size() >= 3; }
            void align(LWSelection selection)
            {
                LWComponent[] comps = sortByX(sortByY(selection));
                float layoutRegion = maxX - minX;
                if (layoutRegion < totalWidth)
                    layoutRegion = totalWidth;
                float horizontalGap = (layoutRegion - totalWidth) / (selection.size() - 1);
                float x = minX;
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(x, c.getY());
                    x += c.getWidth() + horizontalGap;
                }
            }
        };

        
    //-----------------------------------------------------------------------------
    // VueActions
    //-----------------------------------------------------------------------------
    static final Action NewMap =
        new VueAction("New")
        {
            int count = 1;
            public void act()
            {
                LWMap map = new LWMap("New Map " + count++);
                VUE.displayMap(map);
            }
        };
    static final Action CloseMap =
        new VueAction("Close", keyStroke(KeyEvent.VK_W, META))
        {
            // todo: listen to map viewer display event to tag
            // with currently displayed map name\
            boolean enabled() { return VUE.openMapCount() > 0; }
            public void act()
            {
                VUE.closeViewer(VUE.getActiveViewer());
            }
        };
    static final Action Undo =
        new VueAction("Undo", keyStroke(KeyEvent.VK_Z, META))
        {
            public boolean isEnabled() { return false; }

        };
    static final Action Redo =
        new VueAction("Redo", keyStroke(KeyEvent.VK_Z, META+SHIFT))
        {
            public boolean isEnabled() { return false; }
        };


    
    //-------------------------------------------------------
    // Zoom actions
    // Consider having the ZoomTool own these actions -- any
    // other way to have mutiple key values trigger an action?
    // Something about this feels kludgy.
    //-------------------------------------------------------
        
    static final Action ZoomIn =
        //new VueAction("Zoom In", keyStroke(KeyEvent.VK_PLUS, META)) {
        new VueAction("Zoom In", keyStroke(KeyEvent.VK_EQUALS, META)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomBigger();
            }
        };
    static final Action ZoomOut =
        new VueAction("Zoom Out", keyStroke(KeyEvent.VK_MINUS, META)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomSmaller();
            }
        };
    static final Action ZoomFit =
        new VueAction("Zoom Fit", keyStroke(KeyEvent.VK_0, META)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomFit();
            }
        };
    static final Action ZoomActual =
        new VueAction("Zoom 100%", keyStroke(KeyEvent.VK_1, META)) {
            boolean enabled() { return VUE.getActiveViewer().getZoomFactor() != 1.0; }
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoom(1.0);
            }
        };


    
    //-----------------------------------------------------------------------------
    // VueAction: actions that don't depend on the selection
    //-----------------------------------------------------------------------------
    static class VueAction extends javax.swing.AbstractAction
    {
        VueAction(String name, String shortDescription, KeyStroke keyStroke)
        {
            super(name);
            if (shortDescription != null)
                putValue(SHORT_DESCRIPTION, shortDescription);
            if (keyStroke != null)
                putValue(ACCELERATOR_KEY, keyStroke);
        }
        VueAction(String name, KeyStroke keyStroke) {
            this(name, null, keyStroke);
        }
        VueAction(String name) {
            this(name, null, null);
        }
        public String getActionName()
        {
            return (String) getValue(Action.NAME);
        }
        public void actionPerformed(ActionEvent ae)
        {
            LWSelection selection = VUE.ModelSelection;
            //System.out.println(ae);
            System.out.println("VueAction: " + ae.getActionCommand());
            if (enabled()) {
                act();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not currently enabled");
            }
        }
        boolean enabled() { return true; }

        void act() {
            System.err.println("Unhandled VueAction: " + getActionName());
        }
        void Xact() {}// for commenting convenience
    }


    //-----------------------------------------------------------------------------
    // MapAction: actions that depend on the selection in the map viewer
    //-----------------------------------------------------------------------------
    static class MapAction extends VueAction
        implements LWSelection.Listener
    {
        MapAction(String name, String shortDescription, KeyStroke keyStroke)
        {
            super(name, shortDescription, keyStroke);
            VUE.ModelSelection.addListener(this);
        }
        MapAction(String name) {
            this(name, null, null);
        }
        MapAction(String name, KeyStroke keyStroke) {
            this(name, null, keyStroke);
        }
        void act()
        {
            LWSelection selection = VUE.ModelSelection;
            System.out.println("MapAction: " + getActionName() + " " + selection);
            if (enabledFor(selection)) {
                if (mayModifySelection())
                    selection = (LWSelection) selection.clone();
                act(selection);
                VUE.getActiveViewer().repaint();
            } else {
                // This shouldn't happen as actions should already
                // be disabled if they're not  appropriate.
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not enabled given this selection: " + selection);
            }
        }

        public void selectionChanged(LWSelection selection) {
            setEnabled(enabledFor(selection));
        }
        void checkEnabled() {
            selectionChanged(VUE.ModelSelection);
        }
        
        /** Is this action enabled given this selection? */
        boolean enabledFor(LWSelection l) { return l.size() > 0; }
        
        /** the action may result in an event that has the viewer
         * change what's in the current selection (e.g., on delete,
         * the viewer makes sure the deleted object is no longer
         * in the selection group */
        boolean mayModifySelection() { return false; }
        
        void act(LWSelection selection)
        {
            act(selection.iterator());
        }
        // automatically apply the action serially to everything in the
        // selection -- override if this isn't what the action
        // needs to do.
        void act(java.util.Iterator i)
        {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                act(c);
            }
        }
        void act(LWComponent c)
        {
            System.err.println("Unhandled MapAction: " + getActionName() + " on " + c);
        }
        void Xact(LWComponent c) {}// for commenting convenience
    }
    
}

