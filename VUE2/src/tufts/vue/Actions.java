package tufts.vue;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Event;
import java.awt.Point;
import java.awt.Font;
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
    static final int COMMAND = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
    static final int CTRL = Event.CTRL_MASK;
    static final int SHIFT = Event.SHIFT_MASK;
    static final int ALT = Event.ALT_MASK;
    static final int CTRL_ALT = VueUtil.isMacPlatform() ? CTRL+COMMAND : CTRL+ALT;
    
    static final private KeyStroke keyStroke(int vk, int mod) {
        return KeyStroke.getKeyStroke(vk, mod);
    }
    static final private KeyStroke keyStroke(int vk) {
        return keyStroke(vk, 0);
    }

    //-------------------------------------------------------
    // Selection actions
    //-------------------------------------------------------
    
    static final Action SelectAll =
        new MapAction("Select All", keyStroke(KeyEvent.VK_A, COMMAND)) {
            boolean enabledFor(LWSelection s) { return true; }
            public void act()
            {
                VUE.ModelSelection.setTo(VUE.getActiveMap().getChildIterator());
            }
        };
    static final Action DeselectAll =
        new MapAction("Deselect All", keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            public void act()
            {
                VUE.ModelSelection.clear();
            }
        };

    /**Addition by Daisuke Fujiwara*/
    
    static final Action AddPathwayNode = 
        new MapAction("Add to the Pathway")
        {
            public void act()
            {
                LWComponent[] array = VUE.ModelSelection.getArray();
                
                //adds the elements to the current pathway associated with the map
                for (int i = 0; i < array.length; i++)
                    VUE.getPathwayInspector().getPathway().addElement(array[i]);
                
                //updates the inspector's pathwayTab
                VUE.getPathwayInspector().notifyPathwayTab();
                
                //updates the control panel
                VUE.getPathwayControl().updateControlPanel();
            }
            
            public boolean enabled()
            {
              if (VUE.getPathwayInspector().getPathway() == null)
                  return false;
              
              else
                  return true;
            }
        };
        
    /**End of Addition by Daisuke Fujiwara*/
       
    /** Added by Jay Briedis */
        
    static final Action DeletePathwayNode = 
        new MapAction("Delete from the Pathway")
        {
            public void act()
            {
                LWComponent[] array = VUE.ModelSelection.getArray();
                System.out.println("deleting the current node for the pathway...");
                //adds the elements to the current pathway associated with the map
                for (int i = 0; i < array.length; i++)
                    VUE.getPathwayInspector().getPathway().removeElement(array[i]);

                //updates the inspector's pathwayTab
                VUE.getPathwayInspector().notifyPathwayTab();

                //updates the control panel
                VUE.getPathwayControl().updateControlPanel();
            }

            public boolean enabled()
            {
              if (VUE.getPathwayInspector().getPathway() == null)
                  return false;

              else
                  return true;
            }
    }; 
    
    /** End of Jay's Addition*/
        
    //-------------------------------------------------------
    // Edit actions
    //-------------------------------------------------------

    static final Action Cut =
        new MapAction("Cut", keyStroke(KeyEvent.VK_X, COMMAND)) {
            public boolean isEnabled() { return false; }
            void Xact(LWComponent c) {
            }
        };

    static final Action Copy =
        new MapAction("Copy", keyStroke(KeyEvent.VK_C, COMMAND)) {
            public boolean isEnabled() { return false; }
            void Xact(LWComponent c) {
            }
        };
    static final Action Paste =
        new MapAction("Paste", keyStroke(KeyEvent.VK_V, COMMAND)) {
            public boolean isEnabled() { return false; }
            void Xact(LWComponent c) {
            }
        };

    static final Action Duplicate =
        // todo: call this duplicate?
        new MapAction("Duplicate", keyStroke(KeyEvent.VK_D, COMMAND)) {
            List newCopies = new java.util.ArrayList();
            // boolean mayModifySelection() { return true; } // only matters if CONCURRENTLY: todo rename
            boolean enabledFor(LWSelection s)
            {
                return s.size() > 0 && !s.allOfType(LWLink.class);
            }
            void act(Iterator i) {
                newCopies.clear();
                super.act(i);
                VUE.ModelSelection.setTo(newCopies.iterator());
            }
            void act(LWComponent c) {
                // doesn't currently make sense to duplicate links seperately
                // -- will need to handle via LWContainer duplicate
                if (c instanceof LWLink) {
                    //todo: implement cut/copy/paste before making this
                    // more sophisitcated -- will have to work out
                    // separte code to compute what links to add to
                    // to cut group beforehand, and then other code
                    // of how to reproduce them later... (e.g.,
                    // process all links first). -- ACTUALLY --
                    // should put everything in cut buffer first,
                    // and figure out the link grabs at paste time,
                    // which could theoretically depend on the paste context.
                    LWLink l = (LWLink) c;
                    //if (!l.bothEndsInSelection()) {
                    return;
                }
                LWComponent copy = c.duplicate();
                copy.setLocation(c.getX()+10, c.getY()+10);
                c.getParent().addChild(copy);
                newCopies.add(copy);
            }
        };

    //-------------------------------------------------------
    // Group/Ungroup
    //-------------------------------------------------------
    
    static final Action Group =
        new MapAction("Group", keyStroke(KeyEvent.VK_G, COMMAND))
        {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s)
            {
                // enable only when two or more objects in selection,
                // and all share the same parent
                return (s.size() - s.countTypes(LWLink.class)) >= 2 && s.allHaveSameParent();
                //return s.size() >= 2 && s.allHaveSameParent();
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
        new MapAction("Ungroup", keyStroke(KeyEvent.VK_G, COMMAND+SHIFT))
        {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s)
            {
                return s.countTypes(LWGroup.class) > 0;
            }
            void act(java.util.Iterator i)
            {
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c instanceof LWGroup)
                        ((LWGroup)c).disperse();
                }
            }
        };
    static final Action Rename =
        new MapAction("Rename", keyStroke(KeyEvent.VK_F2))
        {
            boolean enabledFor(LWSelection s)
            {
                return s.size() == 1 && !(s.first() instanceof LWGroup);
            }
            void act(LWComponent c) {
                // todo: throw interal exception if c not in active map
                VUE.getActiveViewer().activateLabelEdit(c);
            }
        };
    static final Action Delete =
        new MapAction("Delete", keyStroke(KeyEvent.VK_DELETE))
        {
            boolean mayModifySelection() { return true; }
            void act(LWComponent c) {
                //have to update pathways - jay briedis
                Iterator iter = VUE.getActiveViewer()
                    .getMap()
                    .getPathwayManager()
                    .getPathwayIterator();
                while(iter.hasNext()){
                    LWPathway pathway = (LWPathway)iter.next();
                    if(pathway.contains(c)){
                        if(c instanceof LWNode){
                            LWNode node = (LWNode)c;
                            ArrayList links = (ArrayList)c.getLinks(); // may be modified concurrently
                            if(links != null){
                                for (int i = 0; i < links.size(); i++) {
                                    LWLink l = (LWLink) links.get(i);
                                    if(pathway.contains(l)){
                                        pathway.removeElement(l);
                                    }
                                }
                            }
                        }
                        pathway.removeElement(c);                        
                    }
                }
                
                c.getParent().deleteChild(c);
            }
        };
    
    //-------------------------------------------------------
    // Arrange actions
    //-------------------------------------------------------

    static final MapAction BringToFront =
        new MapAction("Bring to Front",
                      "Raise object to the top, completely unobscured",
                      keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND+SHIFT))
        {
            boolean enabledFor(LWSelection s)
            {
                if (s.size() == 1)
                    return !s.first().getParent().isOnTop(s.first());
                return s.size() >= 2;
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
                      keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND+SHIFT))
        {
            boolean enabledFor(LWSelection s)
            {
                if (s.size() == 1)
                    return !s.first().getParent().isOnBottom(s.first());
                return s.size() >= 2;
            }
            void act(LWSelection selection) {
                LWContainer.sendToBack(selection);
                BringToFront.checkEnabled();
            }
        };
    static final MapAction BringForward =
        new MapAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND))
        {
            boolean enabledFor(LWSelection s) { return BringToFront.enabledFor(s); }
            void act(LWSelection selection) {
                LWContainer.bringForward(selection);
                BringToFront.checkEnabled();
            }
        };
    static final MapAction SendBackward =
        new MapAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND))
        {
            boolean enabledFor(LWSelection s) { return SendToBack.enabledFor(s); }
            void act(LWSelection selection) {
                LWContainer.sendBackward(selection);
                BringToFront.checkEnabled();
            }
        };

    //-------------------------------------------------------
    // Font/Text Actions
    //-------------------------------------------------------
        
    static final Action FontSmaller =
        new MapAction("Font Smaller", keyStroke(KeyEvent.VK_MINUS, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                if (f.getSize() > 1)
                    c.setFont(new Font(f.getName(), f.getStyle(), f.getSize()-1));
            }
        };
    static final Action FontBigger =
        new MapAction("Font Bigger", keyStroke(KeyEvent.VK_EQUALS, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                c.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+1));
            }
        };
    static final Action FontBold =
        new MapAction("Font Bold", keyStroke(KeyEvent.VK_B, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                int newStyle = f.getStyle() ^ Font.BOLD;;
                c.setFont(new Font(f.getName(), newStyle, f.getSize()));
            }
        };
    static final Action FontItalic =
        new MapAction("Font Italic", keyStroke(KeyEvent.VK_I, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                int newStyle = f.getStyle() ^ Font.ITALIC;;
                c.setFont(new Font(f.getName(), newStyle, f.getSize()));
            }
        };

    //-------------------------------------------------------
    // Align actions
    //-------------------------------------------------------
    abstract static class AlignAction extends MapAction
    {
        static float minX, minY;
        static float maxX, maxY;
        static float centerX, centerY;
        static float totalWidth, totalHeight; // added width/height of all in selection
        // obviously not thread-safe here
  
        private AlignAction(String name)
        {
            super(name);
        }
        private AlignAction(String name, int keyCode)
        {
            super(name, keyStroke(keyCode, COMMAND+SHIFT));
        }
        boolean enabledFor(LWSelection s) { return s.size() >= 2; }
        void act(LWSelection selection)
        {
            // todo: remove/ignore any links in the selection!
            Rectangle2D.Float r = (Rectangle2D.Float) LWMap.getBounds(selection.iterator());
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
                if (c instanceof LWLink)
                    continue;
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

        LWComponent[] sortByX(LWComponent[] array)
        {
            java.util.Arrays.sort(array, new java.util.Comparator() {
                    public int compare(Object o1, Object o2) {
                        return (int) (((LWComponent)o1).getX() - ((LWComponent)o2).getX());
                    }});
            return array;
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

    static final Action AlignLeftEdges = new AlignAction("Align Left Edges", KeyEvent.VK_LEFT) {
            void align(LWComponent c) { c.setLocation(minX, c.getY()); }
        };
    static final Action AlignRightEdges = new AlignAction("Align Right Edges", KeyEvent.VK_RIGHT) {
            void align(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
        };
    static final Action AlignTopEdges = new AlignAction("Align Top Edges", KeyEvent.VK_UP) {
            void align(LWComponent c) { c.setLocation(c.getX(), minY); }
        };
    static final Action AlignBottomEdges = new AlignAction("Align Bottom Edges", KeyEvent.VK_DOWN) {
            void align(LWComponent c) { c.setLocation(c.getX(), maxY - c.getHeight()); }
        };
    static final AlignAction AlignCentersColumn = new AlignAction("Align Centers in Column") {
            void align(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
        };
    static final AlignAction AlignCentersRow = new AlignAction("Align Centers in Row") {
            void align(LWComponent c) { c.setLocation(c.getX(), centerY - c.getHeight()/2); }
        };
    static final AlignAction MakeRow = new AlignAction("Make Row", KeyEvent.VK_R) {
            void align(LWSelection selection) {
                AlignCentersRow.align(selection);
                maxX = minX + totalWidth;
                DistributeHorizontally.align(selection);
            }
        };
    static final AlignAction MakeColumn = new AlignAction("Make Column", KeyEvent.VK_C) {
            void align(LWSelection selection) {
                AlignCentersColumn.align(selection);
                maxY = minY + totalHeight;
                DistributeVertically.align(selection);
            }
        };
    static final AlignAction DistributeVertically = new AlignAction("Distribute Vertically", KeyEvent.VK_V) {
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            // use only *2* in selection if use our minimum layout region setting
            void align(LWSelection selection)
            {
                LWComponent[] comps = sortByY(sortByX(selection.getArray()));
                float layoutRegion = maxY - minY;
                //if (layoutRegion < totalHeight)
                //  layoutRegion = totalHeight;
                float verticalGap = (layoutRegion - totalHeight) / (selection.size() - 1);
                float y = minY;
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(c.getX(), y);
                    y += c.getHeight() + verticalGap;
                }
            }
        };

    static final AlignAction DistributeHorizontally = new AlignAction("Distribute Horizontally", KeyEvent.VK_H) {
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            void align(LWSelection selection)
            {
                LWComponent[] comps = sortByX(sortByY(selection.getArray()));
                float layoutRegion = maxX - minX;
                //if (layoutRegion < totalWidth)
                //  layoutRegion = totalWidth;
                float horizontalGap = (layoutRegion - totalWidth) / (selection.size() - 1);
                float x = minX;
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(x, c.getY());
                    x += c.getWidth() + horizontalGap;
                }
            }
        };


    public static final Action[] ALIGN_MENU_ACTIONS = {
        AlignLeftEdges,
        AlignRightEdges,
        AlignTopEdges,
        AlignBottomEdges,
        null,
        AlignCentersColumn,
        AlignCentersRow,
        null,
        MakeRow,
        MakeColumn,
        null,
        DistributeVertically,
        DistributeHorizontally
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
        new VueAction("Close", keyStroke(KeyEvent.VK_W, COMMAND))
        {
            // todo: listen to map viewer display event to tag
            // with currently displayed map name\
            boolean enabled() { return VUE.openMapCount() > 0; }
            public void act()
            {
                VUE.closeViewer(VUE.getActiveViewer());
                //VUE.closeMap(VUE.getActiveMap());
            }
        };
    static final Action Undo =
        new VueAction("Undo", keyStroke(KeyEvent.VK_Z, COMMAND))
        {
            public boolean isEnabled() { return false; }

        };
    static final Action Redo =
        new VueAction("Redo", keyStroke(KeyEvent.VK_Z, COMMAND+SHIFT))
        {
            public boolean isEnabled() { return false; }
        };

    static final Action NewNode =
        new NewItemAction("New Node", keyStroke(KeyEvent.VK_N, COMMAND))
        {
            LWComponent createNewItem(Point2D newLocation)
            {
                // todo: this is where we'll get the active NodeTool
                // and have it create the new node based on it's current
                // settings -- move this logic to NodeTool
                LWNode node = new LWNode("new node");
                node.setLocation(newLocation);
                VUE.getActiveMap().addNode(node);
                VUE.ModelSelection.setTo(node);

                //better: run a timer and do this if no activity (e.g., node creation)
                // for 250ms or something -- todo bug: every other new node not activating label edit

                // todo hack: we need to paint right away so the node can compute it's size,
                // so that the label edit will show up in the right place..
                MapViewer viewer = VUE.getActiveViewer();
                viewer.paintImmediately(viewer.getBounds());//todo opt: could do this off screen?
                viewer.activateLabelEdit(node);
                
                return node;
            }
        };

    static final Action NewText =
        new NewItemAction("New Text", keyStroke(KeyEvent.VK_T, COMMAND))
        {
            LWComponent createNewItem(Point2D newLocation)
            {
                LWNode node = LWNode.createTextNode("new text");
                node.setLocation(newLocation);
                VUE.getActiveMap().addNode(node);
                VUE.ModelSelection.setTo(node); // also important so will be repainted
                return node;
            }
        };

    
    //-------------------------------------------------------
    // Zoom actions
    // Consider having the ZoomTool own these actions -- any
    // other way to have mutiple key values trigger an action?
    // Something about this feels kludgy.
    //-------------------------------------------------------
        
    static final Action ZoomIn =
        //new VueAction("Zoom In", keyStroke(KeyEvent.VK_PLUS, COMMAND)) {
        new VueAction("Zoom In", keyStroke(KeyEvent.VK_EQUALS, COMMAND+SHIFT)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomBigger();
            }
        };
    static final Action ZoomOut =
        new VueAction("Zoom Out", keyStroke(KeyEvent.VK_MINUS, COMMAND+SHIFT)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomSmaller();
            }
        };
    static final Action ZoomFit =
        new VueAction("Zoom Fit", keyStroke(KeyEvent.VK_0, COMMAND+SHIFT)) {
            public void act()
            {
                VUE.getActiveViewer().zoomTool.setZoomFit();
            }
        };
    static final Action ZoomActual =
        new VueAction("Zoom 100%", keyStroke(KeyEvent.VK_1, COMMAND+SHIFT)) {
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
            try {
                String msg = "VueAction: " + getActionName();
                if (!ae.getActionCommand().equals(getActionName()))
                    msg += " (" + ae.getActionCommand() + ")";
                msg += " n=" + VUE.ModelSelection.size();
                System.out.println(msg);
                if (enabled()) {
                    act();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    System.err.println(getActionName() + ": Not currently enabled");
                }
            } catch (Exception e) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                e.printStackTrace();
                System.err.println("*** VueAction: exception during action [" + getActionName() + "]");
                System.err.println("*** VueAction: selection is " + VUE.ModelSelection);
                System.err.println("*** VueAction: event was " + ae);
            }
        }
        boolean enabled() { return true; }

        void act() {
            System.err.println("Unhandled VueAction: " + getActionName());
        }
        void Xact() {}// for commenting convenience
    }

    static class NewItemAction extends VueAction
    {
        static LWComponent lastItem = null;
        static Point lastMousePress = null;
        static Point2D lastLocation = null;
        
        NewItemAction(String name, KeyStroke keyStroke) {
            super(name, null, keyStroke);
        }
            
        void act()
        {
            MapViewer viewer = VUE.getActiveViewer();
            Point mousePress = viewer.getLastMousePoint();
            Point2D newLocation = viewer.screenToMapPoint(mousePress);
                
            if (mousePress.equals(lastMousePress) && lastItem.getLocation().equals(lastLocation)) {
                newLocation.setLocation(lastLocation.getX(),
                                        lastLocation.getY() + lastItem.getBoundsHeight());
            }

            lastItem = createNewItem(newLocation);
            lastLocation = newLocation;
            lastMousePress = mousePress;
        }

        LWComponent createNewItem(Point2D p)
        {
            throw new UnsupportedOperationException("NewItemAction: unimplemented create");
        }
        
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
            //System.out.println("MapAction: " + getActionName() + " n=" + selection.size());
            if (enabledFor(selection)) {
                if (mayModifySelection())
                    selection = (LWSelection) selection.clone();
                act(selection);
                VUE.getActiveViewer().repaintSelection();
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
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        
        /** the action may result in an event that has the viewer
         * change what's in the current selection (e.g., on delete,
         * the viewer makes sure the deleted object is no longer
         * in the selection group */
        boolean mayModifySelection() { return false; }
        
        void act(LWSelection selection)
        {
            act(selection.iterator());
        }

        /**
         * Automatically apply the action serially to everything in the
         * selection -- override if this isn't what the action
         * needs to do.
         *
         * Note that the default is to descend into instances of LWGroup
         * and apply the action seperately to each child, and NOT
         * do apply the action to any nodes that are children of
         * other nodes.
         */
        void act(java.util.Iterator i)
        {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                act(c);
                if (c instanceof LWGroup)
                    act(((LWGroup)c).getChildIterator());
            }
        }
        void act(LWComponent c)
        {
            System.err.println("Unhandled MapAction: " + getActionName() + " on " + c);
        }
        void Xact(LWComponent c) {}// for commenting convenience
    }
    
}

