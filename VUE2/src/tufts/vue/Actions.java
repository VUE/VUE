package tufts.vue;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.awt.Event;
import java.awt.Point;
import java.awt.Font;
import java.awt.datatransfer.Clipboard;
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
class Actions
    implements VueConstants
{
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

    private static boolean allIgnored = false;
    /** Set's all action events to be temporarily ignored.
        E.g., used while a TextBox edit is active */
    // todo: may want to allow NewItem actions as they automatically
    // activate an edit, thus preventing a quick series of NewItem
    // actions to be done.
    static void setAllIgnored(boolean tv)
    {
        allIgnored = tv;
    }
        

    //-------------------------------------------------------
    // Selection actions
    //-------------------------------------------------------
    
    static final Action SelectAll =
        new MapAction("Select All", keyStroke(KeyEvent.VK_A, COMMAND)) {
            boolean enabledFor(LWSelection s) { return true; }
            public void act() {
                VUE.ModelSelection.setTo(VUE.getActiveMap().getAllDescendentsGroupOpaque().iterator());
            }
        };
    static final Action DeselectAll =
        new MapAction("Deselect", keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
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
                //VUE.getPathwayControl().updateControlPanel();
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
                //deletes the elements to the current pathway associated with the map
                for (int i = 0; i < array.length; i++)
                    VUE.getPathwayInspector().getPathway().removeElement(array[i]);

                //updates the inspector's pathwayTab
                VUE.getPathwayInspector().notifyPathwayTab();

                //updates the control panel
                //VUE.getPathwayControl().updateControlPanel();
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
    // Alternative View actions
    //-------------------------------------------------------

    /**Addition by Daisuke Fujiwara*/
    
    static final Action HierarchyView = 
        new MapAction("Hierarchy View")
        {
            public void act()
            {
                LWNode rootNode = (LWNode)(VUE.ModelSelection.get(0));
                String name = new String(rootNode.getLabel() + "'s Hierarchy View");
                String description = new String("Hierarchy view model of " + rootNode.getLabel());
             
                LWHierarchyMap hierarchyMap = new LWHierarchyMap(name);
                
                tufts.oki.hierarchy.HierarchyViewHierarchyModel model = 
                      new tufts.oki.hierarchy.HierarchyViewHierarchyModel(rootNode, hierarchyMap, name, description);
                
                hierarchyMap.setHierarchyModel(model);
                hierarchyMap.addAllComponents();
                
                /*
                String mapLabel;
        
                try
                {
                    mapLabel = model.getDisplayName();
                }
        
                catch(osid.hierarchy.HierarchyException he)
                {
                    mapLabel = "Hierarchy Map"; 
                }
                */
                
                // Doesn't compile -- SMF 2004-01-04 12:32.18 Sunday 
                //LWHierarchyMap hierarchyMap = new LWHierarchyMap(model, mapLabel);
                VUE.displayMap((LWMap)hierarchyMap);
            }
            
            public boolean enabled()
            { 
              if (VUE.ModelSelection.size() != 1 && VUE.ModelSelection.get(0) instanceof LWLink)
                  return false;
              
              else
                  return true;
            }
        };
        
    /**End of Addition by Daisuke Fujiwara*/

    //-----------------------------------------------------------------------------
    // Link actions
    //-----------------------------------------------------------------------------
        
    static final MapAction LinkMakeStraight =
        new MapAction("Make Straight") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 0 : true;
            }
            public void act(LWLink c) { c.setControlCount(0); }
        };
    static final MapAction LinkMakeQuadCurved =
        new MapAction("Make Curved (1 point)") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 1 : true;
            }
            public void act(LWLink c) { c.setControlCount(1); }
        };
    static final MapAction LinkMakeCubicCurved =
        new MapAction("Make Curved (2 points)") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 2 : true;
            }
            public void act(LWLink c) { c.setControlCount(2); }
        };
    static final Action LinkArrows =
        new MapAction("Arrows", keyStroke(KeyEvent.VK_L, COMMAND)) {
            boolean enabledFor(LWSelection s) { return s.containsType(LWLink.class); }
            public void act(LWLink c) { c.rotateArrowState(); }
        };

    
    /** Helper for menu creation.  Null's indicate good places
        for menu separators. */
    public static final Action[] LINK_MENU_ACTIONS = {
        LinkMakeStraight,
        LinkMakeQuadCurved,
        LinkMakeCubicCurved,
        LinkArrows
    };


    //-----------------------------------------------------------------------------
    // Node actions
    //-----------------------------------------------------------------------------

    static final MapAction NodeMakeAutoSized =
        new MapAction("Set Auto-Sized") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWNode.class))
                    return false;
                return s.size() == 1 ? ((LWNode)s.first()).isAutoSized() == false : true;
            }
            public void act(LWNode c) {
                c.setAutoSized(true);
                c.layout();
            }
        };

    /** Helper for menu creation.  Null's indicate good places
        for menu separators. */
    public static final Action[] NODE_MENU_ACTIONS = {
        NodeMakeAutoSized
    };
    
    //-------------------------------------------------------
    // Edit actions: Duplicate, Cut, Copy & Paste
    // These actions all make use of the statics
    // below.
    //-------------------------------------------------------

    private static ArrayList ScratchBuffer = new ArrayList();
    private static LWContainer ScratchMap;
            
    private static HashMap sCopies = new HashMap();
    private static HashMap sOriginals = new HashMap();

    private static final int sCopyOffset = 10;

    private static Collection duplicatePreservingLinks(Iterator i)
    {
        sCopies.clear();
        sOriginals.clear();
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            LWComponent copy = c.duplicate();
            sCopies.put(c, copy);
            sOriginals.put(copy, c);
        }
        reconnectLinks();
        return sCopies.values();
    }
    
    private static void reconnectLinks()
    {
        Iterator ic = sCopies.values().iterator();
        while (ic.hasNext()) {
            LWComponent c = (LWComponent) ic.next();
            if (!(c instanceof LWLink))
                continue;
            LWLink copied_link = (LWLink) c;
            LWLink original_link = (LWLink) sOriginals.get(copied_link);
            
            copied_link.setComponent1((LWComponent)sCopies.get(original_link.getComponent1()));
            copied_link.setComponent2((LWComponent)sCopies.get(original_link.getComponent2()));
        }
    }
            
            
    static final MapAction Duplicate =
        new MapAction("Duplicate", keyStroke(KeyEvent.VK_D, COMMAND)) {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            // hierarchicalAction set to true: if parent being duplicated, don't duplicate
            // any selected children, creating extra siblisngs.
            boolean hierarchicalAction() { return true; } 

            // TODO: preserve layering order of components -- don't
            // just leave in the arbitrary selection order!
            void act(Iterator i) {
                sCopies.clear();
                sOriginals.clear();
                super.act(i);
                reconnectLinks();
                VUE.ModelSelection.setTo(sCopies.values().iterator());
            }

            void act(LWComponent c) {
                LWComponent copy = c.duplicate();
                copy.setLocation(c.getX()+sCopyOffset,
                                 c.getY()+sCopyOffset);
                c.getParent().addChild(copy);
                //System.out.println("duplicated:\n\t" + c + "\n\t" + copy);
                sCopies.put(c, copy);
                sOriginals.put(copy, c);
            }

        };

    static final Action Cut =
        new MapAction("Cut", keyStroke(KeyEvent.VK_X, COMMAND)) {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            void act(LWSelection selection) {
                Copy.act(selection);
                Delete.act(selection);
                ScratchMap = null;  // okay to paste back in same location
            }
        };

    static final MapAction Copy =
        new MapAction("Copy", keyStroke(KeyEvent.VK_C, COMMAND)) {
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            void act(Iterator iSelection) {
                ScratchBuffer.clear();
                ScratchBuffer.addAll(duplicatePreservingLinks(iSelection));
                ScratchMap = VUE.getActiveViewer().getMap();
                // paste differs from duplicate in that the new parent is
                // always the top level map -- not the old parent -- so a node
                // that was a child has to have it's scale set back to 1.0
                // todo: do this automatically in removeChild?
                Iterator i = ScratchBuffer.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c.getScale() != 1f)
                        c.setScale(1f);
                }
            }
        };
    static final Action Paste =
        new MapAction("Paste", keyStroke(KeyEvent.VK_V, COMMAND)) {
            boolean enabledFor(LWSelection s) { return true; }
            //boolean enabledFor(LWSelection s)// doesn't get updates from ScratchBuffer!
            //{ return ScratchBuffer.size() > 0; }
            //public boolean isEnabled() { return true; }//listen for scratch buffer fill

            void act() {
                LWContainer parent = VUE.getActiveViewer().getMap();
                if (parent == ScratchMap) {
                    // unless this was from a cut or it came from a
                    // different map, or we already pasted this,
                    // offset the location.  This is conveniently
                    // cumulative since we're offsetting the actual
                    // components in the cut buffer, which are
                    // duplicated each time we paste.
                    Iterator i = ScratchBuffer.iterator();
                    while (i.hasNext()) {
                        LWComponent c = (LWComponent) i.next();
                        c.setLocation(c.getX()+sCopyOffset,
                                      c.getY()+sCopyOffset);
                    }
                } else
                    ScratchMap = parent; // pastes again to this map will be offset

                Collection pasted = duplicatePreservingLinks(ScratchBuffer.iterator());
                parent.addChildren(pasted.iterator());
                VUE.ModelSelection.setTo(pasted.iterator());
            }
            void act_system() {
                
                Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                VUE.getActiveViewer().getMapDropTarget().processTransferable(clipboard.getContents(this), null);
            }
        };

    static final MapAction Delete =
        new MapAction("Delete", keyStroke(KeyEvent.VK_DELETE))
        {
            // hierarchicalAction is true: if parent being deleted,
            // let it handle deleting the children (ignore any
            // children in selection who's parent is also in selection)
            boolean hierarchicalAction() { return true; }
            boolean mayModifySelection() { return true; }
            
            void act(Iterator i) {
                super.act(i);
                VUE.ModelSelection.clear();
            }
            void act(LWComponent c) {
                //have to update pathways - jay briedis (remove this code and add listeners in pathway instead --SMF)
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

                LWContainer parent = c.getParent();
                if (parent == null) {
                    // right now this can happen because links are auto-deleted if
                    // both their endpoints are deleted
                    System.out.println("DELETE: " + c + " skipping: null parent (already deleted)");
                } else if (c.isDeleted()) {
                    // right now this can happen because links are auto-deleted if
                    // both their endpoints are deleted
                    System.out.println("DELETE: " + c + " skipping (already deleted)");
                } else if (parent.isDeleted()) {
                    System.out.println("DELETE: " + c + " skipping (parent already deleted)");
                } else if (parent.isSelected()) { // if parent selected, it will delete it's children
                    System.out.println("DELETE: " + c + " skipping - parent selected & will be deleting");
                } else {
                    parent.deleteChildPermanently(c);
                }
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
            boolean enabledFor(LWSelection s) {
                return s.countTypes(LWGroup.class) > 0;
            }
            void act(Iterator i)
            {
                List dispersedChildren = new ArrayList();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c instanceof LWGroup) {
                        LWGroup group = (LWGroup) c;
                        dispersedChildren.addAll(group.getChildList());
                        group.disperse();
                    }
                }
                VUE.ModelSelection.setTo(dispersedChildren.iterator());
            }
        };
    static final Action Rename =
        new MapAction("Rename", keyStroke(KeyEvent.VK_F2))
        {
            boolean enabledFor(LWSelection s)
            {
                return s.size() == 1 && s.first().supportsUserLabel();
            }
            void act(LWComponent c) {
                // todo: throw interal exception if c not in active map
                VUE.getActiveViewer().activateLabelEdit(c);
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
            }
        };
    static final MapAction BringForward =
        new MapAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND))
        {
            boolean enabledFor(LWSelection s) { return BringToFront.enabledFor(s); }
            void act(LWSelection selection) {
                LWContainer.bringForward(selection);
            }
        };
    static final MapAction SendBackward =
        new MapAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND))
        {
            boolean enabledFor(LWSelection s) { return SendToBack.enabledFor(s); }
            void act(LWSelection selection) {
                LWContainer.sendBackward(selection);
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
                int size = f.getSize();
                if (size > 1) {
                    if (size >= 14 && size % 2 == 0)
                        size -= 2;
                    else
                        size--;
                    c.setFont(new Font(f.getName(), f.getStyle(), size));
                }
            }
        };
    static final Action FontBigger =
        new MapAction("Font Bigger", keyStroke(KeyEvent.VK_EQUALS, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                int size = f.getSize();
                if (size >= 12 && size % 2 == 0)
                    size += 2;
                else
                    size++;
                c.setFont(new Font(f.getName(), f.getStyle(), size));
            }
        };
    static final Action FontBold =
        new MapAction("Font Bold", keyStroke(KeyEvent.VK_B, COMMAND))
        {
            void act(LWComponent c) {
                //System.out.println("BOLDING " + c);
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
  
        private AlignAction(String name, KeyStroke keyStroke)
        {
            super(name, keyStroke);
        }
        private AlignAction(String name, int keyCode)
        {
            super(name, keyStroke(keyCode, COMMAND+SHIFT));
        }
        private AlignAction(String name)
        {
            super(name);
        }
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return s.size() >= 2; }
        void act(LWSelection selection)
        {
            if (!selection.allOfType(LWLink.class)) {
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    // remove all links from our cloned copy of the selection
                    if (c instanceof LWLink)
                        i.remove();
                    // remove all children of nodes or groups, who's parent handles their layout
                    if (!(c.getParent() instanceof LWMap)) // really: c.isLaidOut()
                        i.remove();
                }
            }
    
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
    static final AlignAction AlignCentersRow = new AlignAction("Align Centers in Row", KeyEvent.VK_R) {
            void align(LWComponent c) { c.setLocation(c.getX(), centerY - c.getHeight()/2); }
        };
    static final AlignAction AlignCentersColumn = new AlignAction("Align Centers in Column", KeyEvent.VK_C) {
            void align(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
        };
    static final AlignAction MakeRow = new AlignAction("Make Row", keyStroke(KeyEvent.VK_R, ALT)) {
            // todo bug: an already made row is shifting everything to the left
            // (probably always, actually)
            void align(LWSelection selection) {
                AlignCentersRow.align(selection);
                maxX = minX + totalWidth;
                DistributeHorizontally.align(selection);
            }
        };
    static final AlignAction MakeColumn = new AlignAction("Make Column", keyStroke(KeyEvent.VK_C, ALT)) {
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


    /** Helper for menu creation.  Null's indicate good places
        for menu separators. */
    public static final Action[] ALIGN_MENU_ACTIONS = {
        AlignLeftEdges,
        AlignRightEdges,
        AlignTopEdges,
        AlignBottomEdges,
        null,
        AlignCentersRow,
        AlignCentersColumn,
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
        new VueAction("New Map")
        {
            private int count = 1;
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
                VUE.closeMap(VUE.getActiveMap());
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

    static final VueAction NewNode =
        new NewItemAction("New Node", keyStroke(KeyEvent.VK_N, COMMAND))
        {
            LWComponent createNewItem(Point2D newLocation)
            {
                LWNode node = NodeTool.createNode("new node");
                node.setLocation(newLocation);
                //node.setCenterAt(newLocation); // better but screws up NewItemAction's serial item creation positioning
                VUE.getActiveMap().addNode(node);

                //better: run a timer and do this if no activity (e.g., node creation)
                // for 250ms or something -- todo bug: every other new node not activating label edit

                // todo hack: we need to paint right away so the node can compute it's size,
                // so that the label edit will show up in the right place..
                MapViewer viewer = VUE.getActiveViewer();
                //viewer.paintImmediately(viewer.getBounds());//todo opt: could do this off screen?
                viewer.activateLabelEdit(node);
                
                return node;
            }
        };

    static final VueAction NewText =
        new NewItemAction("New Text", keyStroke(KeyEvent.VK_T, COMMAND))
        {
            LWComponent createNewItem(Point2D newLocation)
            {
                LWNode node = NodeTool.createTextNode("new text");
                node.setLocation(newLocation);
                //node.setCenterAt(newLocation);
                // todo: using setCenter, here and in NewNode action, will have to
                // redo NewItemAction if want to be able to change the creation location
                // automatically of they keep clicking in the same spot
                VUE.getActiveMap().addNode(node);
                //VUE.ModelSelection.setTo(node); // also important so will be repainted (repaint optimziation only)
                MapViewer viewer = VUE.getActiveViewer();
                //viewer.paintImmediately(viewer.getBounds());//todo opt: could do this off screen?
                viewer.activateLabelEdit(node);
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
            public void act() {
                ZoomTool.setZoomBigger(null);
            }
        };
    static final Action ZoomOut =
        new VueAction("Zoom Out", keyStroke(KeyEvent.VK_MINUS, COMMAND+SHIFT)) {
            public void act() {
                ZoomTool.setZoomSmaller(null);
            }
        };
    static final Action ZoomFit =
        new VueAction("Zoom Fit", keyStroke(KeyEvent.VK_0, COMMAND+SHIFT)) {
            public void act() {
                ZoomTool.setZoomFit();
            }
        };
    static final Action ZoomActual =
        new VueAction("Zoom 100%", keyStroke(KeyEvent.VK_1, COMMAND+SHIFT)) {
            // no way to listen for zoom change events to keep this current
            //boolean enabled() { return VUE.getActiveViewer().getZoomFactor() != 1.0; }
            public void act() {
                ZoomTool.setZoom(1.0);
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
            if (DEBUG_EVENTS) System.out.println("\n-------------------------------------------------------\n"
                                                 + this + " START OF actionPerformed: ActionEvent=" + ae.paramString());
            if (allIgnored) {
                if (DEBUG_EVENTS) System.out.println("ACTIONS DISABLED.");
                return;
            }
            try {
                /*
                String msg = "VueAction: " + getActionName();
                if (!ae.getActionCommand().equals(getActionName()))
                    msg += " (" + ae.getActionCommand() + ")";
                msg += " n=" + VUE.ModelSelection.size();
                System.out.println(msg);
                */
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
            //if (DEBUG_EVENTS) System.out.println("\n" + this + " UPDATING JUST THE ACTION LISTENERS FOR ENABLED STATES");
            updateActionListeners();
            if (DEBUG_EVENTS) System.out.println("\n" + this + " END OF actionPerformed: ActionEvent=" + ae.paramString() + "\n");
            // normally handled by updateActionListeners, but if someone
            // has actually defined "enabled" instead of enabledFor, we'll
            // need this.
            // setEnabled(enabled());
            // Okay, do NOT do this -- enabled sometimes use to just
            // ring the bell when an action is attempted that you
            // can't actually do right now -- problem is if we
            // disable the action based on enabled(), it has
            // no way of ever getting turned back on!
        }

        // To update action's enabled state after an action is performed.
        private void updateActionListeners()
        {
            Iterator i = VUE.ModelSelection.getListeners().iterator();
            while (i.hasNext()) {
                LWSelection.Listener l = (LWSelection.Listener) i.next();
                if (l instanceof javax.swing.Action) {
                    l.selectionChanged(VUE.ModelSelection);
                    //System.out.println("Notifying action " + l);
                }
                //else System.out.println("Skipping listener " + l);
            }
        }

        /** note that overriding this will not update the action's enabled
         * state based on what's in the selection -- you need to use
         * enabledFor(LWSelection s) for that -- it gets called whenever
         * the selection changes and will update the actions enabled
         * state based on the return value.
         */
        boolean enabled() { return true; }

        void act() {
            System.err.println("Unhandled VueAction: " + getActionName());
        }
        void Xact() {}// for commenting convenience

        public String toString() { return "VueAction[" + getActionName() + "]"; }
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
                newLocation.setLocation(lastLocation.getX() + 10,
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
                // be disabled if they're not appropriate, tho
                // if the action depends on something other than
                // the selection and isn't listening for it, we'll
                // get here.
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not enabled given this selection: " + selection);
            }
        }

        /*
          //debug
        public void setEnabled(boolean tv)
        {
            super.setEnabled(tv);
            System.out.println(this + " enabled=" + tv);
        }
        */

        boolean enabled() { return VUE.getActiveMap() != null && enabledFor(VUE.ModelSelection); }

        public void selectionChanged(LWSelection selection) {
            if (VUE.getActiveMap() == null)
                setEnabled(false);
            else
                setEnabled(enabledFor(selection));
        }
        void checkEnabled() {
            selectionChanged(VUE.ModelSelection);
        }
        
        /** Is this action enabled given this selection? */
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        
        /** mayModifySelection: the action may result in an event that
         * has the viewer change what's in the current selection
         * (e.g., on delete, the viewer makes sure the deleted object
         * is no longer in the selection group -- we need this because
         * actions usually iterate thru the selection, and if it might
         * change in the middle of the iteration, we have to clone it
         * before going thru it or we will get conncurrent
         * modification exceptions.  An action does NOT need to
         * declare that it may modification the selection if it just
         * changes the selection at the end of the iteration (e.g., by
         * setting the selection to newly copied nodes or something)
         * */

        boolean mayModifySelection() { return false; }
        
        /** hierarchicalAction: any children in selection who's
         * parent is also in the selection are ignore during
         * iterator -- for actions such as delete where deleting
         * the parent will automatically delete any children.
         */
        boolean hierarchicalAction() { return false; }
        
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
         * other nodes. If the child is already in selection (e.g.
         * a select all was done) be sure NOT do act on it, otherwise
         * the action will be done twice).
         */
        void act(Iterator i)
        {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (hierarchicalAction() && c.getParent().isSelected())
                    continue;
                act(c);
                // it's possible c was deleted by above action,
                // so make sure we don't proceed if that's the case.
                if (c instanceof LWGroup && !hierarchicalAction() && !c.isDeleted()) {
                    Iterator gi = ((LWGroup)c).getChildIterator();
                    while (gi.hasNext()) {
                        LWComponent gc = (LWComponent) gi.next();
                        if (!gc.isSelected())
                            act(gc);
                    }
                }
            }
        }
        void act(LWComponent c)
        {
            if (c instanceof LWLink)
                act((LWLink)c);
            else if (c instanceof LWNode)
                act((LWNode)c);
            else
                System.err.println("Unhandled MapAction: " + getActionName() + " on " + c);
            
        }
        void act(LWLink c)
        {
            System.out.println("Unhandled MapAction: " + getActionName() + " on " + c);
        }
        void act(LWNode c)
        {
            System.out.println("Unhandled MapAction: " + getActionName() + " on " + c);
        }
        
        void Xact(LWComponent c) {}// for commenting convenience
        public String toString() { return "MapAction[" + getActionName() + "]"; }
    }
    
}




                /*
                  Cut.act(LWSelection)
                // todo perf: if want to be risky & hairy,
                // could do all this instead:
                ScratchBuffer.clear();
                // we can put the originals into the scratch
                // buffer in the case of Cut, but need to
                // make sure nobody is listening to them, and
                // manually remove them from the map.
                // This is different than doing a "Delete"
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    c.getParent().removeChild(c);
                    c.removeAllLWCListeners();
                    // will also need to disconnect all link
                    // connections that pass over the "selection"
                    // boundry of what's in and out ... not
                    // worth the hassle when we can just Copy
                    // then Delete
                    ScratchBuffer.add(c);
                }
                VUE.ModelSelection.clear();
                */
