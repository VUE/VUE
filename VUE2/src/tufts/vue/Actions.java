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
    
    static final MapAction SelectAll =
        new MapAction("Select All", keyStroke(KeyEvent.VK_A, META)) {
            boolean enabledFor(LWSelection l) { return true; }
            public void actionPerformed(ActionEvent ae)
            {
                VUE.ModelSelection.add(VUE.getActiveViewer().getMap().getChildIterator());
            }
        };
    static final MapAction DeselectAll =
        new MapAction("Deselect All", keyStroke(KeyEvent.VK_A, SHIFT+META)) {
            boolean enabledFor(LWSelection l) { return VUE.ModelSelection.size() > 0; }
            public void actionPerformed(ActionEvent ae)
            {
                VUE.ModelSelection.clear();
            }
        };
    static final MapAction Cut =
        new MapAction("Cut", keyStroke(KeyEvent.VK_X, META)) {
            void Xact(LWComponent c) {
                
            }
        };
    static final MapAction Copy =
        new MapAction("Copy", keyStroke(KeyEvent.VK_C, META)) {
            void Xact(LWComponent c) {
                
            }
        };
    static final MapAction Paste =
        new MapAction("Paste", keyStroke(KeyEvent.VK_V, META)) {
            void Xact(LWComponent c) {
                
            }
        };
    static final MapAction Group =
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
            }
        };
    static final MapAction Ungroup =
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
    static final MapAction Rename =
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
    static final MapAction Delete =
        new MapAction("Delete", keyStroke(KeyEvent.VK_DELETE))
        {
            boolean mayModifySelection() { return true; }
            void act(LWComponent c) {
                c.getParent().deleteChild(c);
            }
        };
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
    static final MapAction NewNode =
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

        
        // okay, what about zoom actions?  Don't need to 
    static final MapAction ZoomIn =
        new MapAction("Zoom In", keyStroke(KeyEvent.VK_PLUS, META))
        {
            boolean enabledFor(LWSelection l) { return true; }
            public void actionPerformed(ActionEvent ae)
            {
                //getActiveViewer().getZoomTool().setZoomBigger();
            }
        };

        
    abstract static class AlignAction extends MapAction
    {
        protected static float minX, minY;
        protected static float maxX, maxY;
        protected static float centerX, centerY;
        protected static float totalWidth, totalHeight;
        // obviously not thread-safe here
        
        AlignAction(String name) { super(name); }
        boolean enabledFor(LWSelection l) { return l.size() >= 2; }
        void align(LWComponent c){}
        void act(LWSelection selection)
        {
            Rectangle2D.Float r = (Rectangle2D.Float) Vue2DMap.getBounds(selection.iterator());
            minX = r.x;
            minY = r.y;
            maxX = r.x + r.width;
            maxY = r.y + r.height;
            centerX = (minX + maxX) / 2;
            centerY = (minY + maxY) / 2;
            Iterator i = selection.iterator();
            totalWidth = totalHeight = 0;
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                totalWidth += c.getWidth();
                totalHeight += c.getHeight();
            }
            i = selection.iterator();
            while (i.hasNext())
                align((LWComponent) i.next());
        }
    };
        
    static final MapAction AlignLeftEdges = new AlignAction("Align Left Edges") {
            void align(LWComponent c) {
                c.setLocation(minX, c.getY());
            }
        };
    static final MapAction AlignRightEdges = new AlignAction("Align Right Edges") {
            void align(LWComponent c) {
                c.setLocation(maxX - c.getWidth(), c.getY());
            }
        };
    static final MapAction AlignTopEdges = new AlignAction("Align Top Edges") {
            void align(LWComponent c) {
                c.setLocation(c.getX(), minY);
            }
        };
    static final MapAction AlignBottomEdges = new AlignAction("Align Bottom Edges") {
            void align(LWComponent c) {
                c.setLocation(c.getX(), maxY - c.getHeight());
            }
        };
        
    static final MapAction AlignCentersColumn = new AlignAction("Align Centers Column") {
            void align(LWComponent c) {
                c.setLocation(centerX - c.getWidth()/2, c.getY());
            }
        };
    static final MapAction AlignCentersRow = new AlignAction("Align Centers Row") {
            void align(LWComponent c) {
                c.setLocation(c.getX(), centerY - c.getHeight()/2);
            }
        };

        
    //-----------------------------------------------------------------------------
    // VueActions
    //-----------------------------------------------------------------------------
    static final MapAction NewMap =
        new MapAction("New")
        {
            private int count = 1;
            boolean enabledFor(LWSelection l) { return true; }
            public void actionPerformed(ActionEvent ae)
            {
                Vue2DMap map = new Vue2DMap("New Map " + count++);
                VUE.displayMap(map);
                //MapViewer viewer = new tufts.vue.MapViewer(map);
                //VUE.addViewer(viewer);                
            }
        };
        
    //-------------------------------------------------------
    // VueAction
    //-------------------------------------------------------
    static final MapAction CloseMap =
        new MapAction("Close")
        {
            boolean enabledFor(LWSelection l) { return true; }
            /*
              boolean enabled()
              {
              return VUE.openMapCount() > 0;
              }
            */
            public void actionPerformed(ActionEvent ae)
            {
                VUE.closeViewer(VUE.getActiveViewer());
            }
        };
    
    //componentaction? MapSelectionAction?
    static class MapAction extends javax.swing.AbstractAction
        implements LWSelection.Listener
    {
        MapAction(String name, String shortDescription, KeyStroke keyStroke)
        {
            super(name);
            if (shortDescription != null)
                putValue(SHORT_DESCRIPTION, shortDescription);
            if (keyStroke != null)
                putValue(ACCELERATOR_KEY, keyStroke);
            VUE.ModelSelection.addListener(this);
        }
        MapAction(String name)
        {
            this(name, null, null);
        }
        MapAction(String name, KeyStroke keyStroke)
        {
            this(name, null, keyStroke);
        }
        public String getActionName()
        {
            return (String) getValue(Action.NAME);
        }
        public void actionPerformed(ActionEvent ae)
        {
            LWSelection selection = VUE.ModelSelection;
            //todo: if no active viewer, try a static MapViewer in case it's running alone
            System.out.println(ae);
            System.out.println(ae.getActionCommand() + " " + selection);
            if (enabledFor(selection)) {
                if (mayModifySelection())
                    selection = (LWSelection) selection.clone();
                act(selection);
                VUE.getActiveViewer().repaint();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.out.println("Not enabled given this selection.");//todo: disable action
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
            System.out.println("unhandled MapAction: " + getActionName() + " on " + c);
        }
        void Xact(LWComponent c) {}// for commenting convenience
    }
}

