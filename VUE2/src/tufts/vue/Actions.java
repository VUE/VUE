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
import javax.swing.Icon;

/**
 * VUE application actions (file, viewer/component, etc)
 *
 * @author Scott Fraize
 * @version March 2004
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


    //-------------------------------------------------------
    // Selection actions
    //-------------------------------------------------------
    
    static final Action SelectAll =
        new VueAction("Select All", keyStroke(KeyEvent.VK_A, COMMAND)) {
            boolean undoable() { return false; }
            public void act() {
                VUE.getSelection().setTo(VUE.getActiveMap().getAllDescendentsGroupOpaque().iterator());
            }
        };
    static final Action DeselectAll =
        new LWCAction("Deselect", keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
            boolean undoable() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            public void act() {
                VUE.getSelection().clear();
            }
        };

    static final Action AddPathwayItem = 
        new LWCAction("Add to Pathway")
        {
            public void act(Iterator i) {
                VUE.getActivePathway().add(i);
            }
            public boolean enabledFor(LWSelection s) {
                // items can be added to pathway as many times as you want
                return VUE.getActivePathway() != null && s.size() > 0;
            }
        };
        
    static final Action RemovePathwayItem = 
        new LWCAction("Remove from Pathway")
        {
            public void act(Iterator i) {
                VUE.getActivePathway().remove(i);
            }
            public boolean enabledFor(LWSelection s) {
                LWPathway p = VUE.getActivePathway();
                return p != null && s.size() > 0 && (s.size() > 1 || p.contains(s.first()));
            }
        }; 
    
    //-------------------------------------------------------
    // Alternative View actions
    //-------------------------------------------------------

    /**Addition by Daisuke Fujiwara*/
    
    static final Action HierarchyView = 
        new LWCAction("Hierarchy View")
        {
            boolean undoable() { return false; }
            public void act()
            {
                LWNode rootNode = (LWNode)(VUE.getSelection().get(0));
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
            
            public boolean enabledFor(LWSelection s) {
                return s.size() == 1 && s.first() instanceof LWNode;
            }
        };
        
    /**End of Addition by Daisuke Fujiwara*/

    //-----------------------------------------------------------------------------
    // Link actions
    //-----------------------------------------------------------------------------
        
    static final LWCAction LinkMakeStraight =
        new LWCAction("Make Straight") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 0 : true;
            }
            public void act(LWLink c) { c.setControlCount(0); }
        };
    static final LWCAction LinkMakeQuadCurved =
        new LWCAction("Make Curved (1 point)") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 1 : true;
            }
            public void act(LWLink c) { c.setControlCount(1); }
        };
    static final LWCAction LinkMakeCubicCurved =
        new LWCAction("Make Curved (2 points)") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 2 : true;
            }
            public void act(LWLink c) { c.setControlCount(2); }
        };
    static final Action LinkArrows =
        new LWCAction("Arrows", keyStroke(KeyEvent.VK_L, COMMAND)) {
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

    static final LWCAction NodeMakeAutoSized =
        new LWCAction("Set Auto-Sized") {
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWNode.class))
                    return false;
                return s.size() == 1 ? ((LWNode)s.first()).isAutoSized() == false : true;
            }
            public void act(LWNode c) {
                c.setAutoSized(true);
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
            if (c.getParent() != null && c.getParent().isSelected()) {
                // Duplicate is hierarchical action: don't dupe if
                // parent is going to do it for us.  Note that when we
                // call this on paste, c.getParent() will always be
                // null as these are orphans in the cut buffer, but we
                // culled them here when we put them in, so we're all
                // set.
                continue;
            }
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
            
            
    static final LWCAction Duplicate =
        new LWCAction("Duplicate", keyStroke(KeyEvent.VK_D, COMMAND)) {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            // hierarchicalAction set to true: if parent being duplicated, don't duplicate
            // any selected children, creating extra siblings.
            boolean hierarchicalAction() { return true; } 

            // TODO: preserve layering order of components -- don't
            // just leave in the arbitrary selection order!
            void act(Iterator i) {
                sCopies.clear();
                sOriginals.clear();
                super.act(i);
                reconnectLinks();
                VUE.getSelection().setTo(sCopies.values().iterator());
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
        new LWCAction("Cut", keyStroke(KeyEvent.VK_X, COMMAND)) {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            void act(LWSelection selection) {
                Copy.act(selection);
                Delete.act(selection);
                ScratchMap = null;  // okay to paste back in same location
            }
        };

    static final LWCAction Copy =
        new LWCAction("Copy", keyStroke(KeyEvent.VK_C, COMMAND)) {
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
        new VueAction("Paste", keyStroke(KeyEvent.VK_V, COMMAND)) {
            //public boolean isEnabled() //would need to listen for scratch buffer fills

            void act() {
                LWContainer parent = VUE.getActiveViewer().getMap();
                if (parent == ScratchMap) {
                    // unless this was from a cut or it came from a
                    // different map, or we already pasted this,
                    // offset the location.  This is conveniently
                    // cumulative since we're translating the actual
                    // components in the cut buffer, which are then
                    // duplicated each time we paste.
                    Iterator i = ScratchBuffer.iterator();
                    while (i.hasNext()) {
                        LWComponent c = (LWComponent) i.next();
                        c.translate(sCopyOffset, sCopyOffset);
                    }
                } else
                    ScratchMap = parent; // pastes again to this map will be offset

                Collection pasted = duplicatePreservingLinks(ScratchBuffer.iterator());
                parent.addChildren(pasted.iterator());
                VUE.getSelection().setTo(pasted.iterator());
            }

            void act_system() {
                Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                VUE.getActiveViewer().getMapDropTarget().processTransferable(clipboard.getContents(this), null);
            }
            
        };

    static final LWCAction Delete =
        new LWCAction("Delete", keyStroke(KeyEvent.VK_DELETE))
        {
            // hierarchicalAction is true: if parent being deleted,
            // let it handle deleting the children (ignore any
            // children in selection who's parent is also in selection)
            boolean hierarchicalAction() { return true; }
            boolean mayModifySelection() { return true; }
            
            void act(Iterator i) {
                super.act(i);

                // LWSelection does NOT listen for events among what's
                // selected (an optimization & we don't want the
                // selection updating iself and issuing selection
                // change events AS a delete takes place for each
                // component as it's deleted) -- it only needs to know
                // about deletions, so they're handled special case.
                // Here, all we need to do is clear the selection as
                // we know everything in it has just been deleted.

                VUE.getSelection().clear();
            }
            void act(LWComponent c) {
                LWContainer parent = c.getParent();
                if (parent == null) {
                    // right now this can happen because links are auto-deleted if
                    // both their endpoints are deleted
                    System.out.println("DELETE: " + c + " skipping: null parent (already deleted)");
                } else if (c.isDeleted()) {
                    System.out.println("DELETE: " + c + " skipping (already deleted)");
                } else if (parent.isDeleted()) {
                    System.out.println("DELETE: " + c + " skipping (parent already deleted)"); // parent will call deleteChildPermanently
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
        new LWCAction("Group", keyStroke(KeyEvent.VK_G, COMMAND))
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
                VUE.getSelection().setTo(group);
                // setting selection here is slightly sketchy in that it's
                // really a UI policy that belongs to the viewer
                // todo: could handle in viewer via "created" LWCEvent
            }
        };
    static final Action Ungroup =
        new LWCAction("Ungroup", keyStroke(KeyEvent.VK_G, COMMAND+SHIFT))
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
                VUE.getSelection().setTo(dispersedChildren.iterator());
            }
        };
    static final Action Rename =
        new LWCAction("Rename", keyStroke(KeyEvent.VK_F2))
        {
            boolean undoable() { return false; } // label editor handles the undo
            boolean enabledFor(LWSelection s) {
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

    static final LWCAction BringToFront =
        new LWCAction("Bring to Front",
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
    static final LWCAction SendToBack =
        new LWCAction("Send to Back",
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
    static final LWCAction BringForward =
        new LWCAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND))
        {
            boolean enabledFor(LWSelection s) { return BringToFront.enabledFor(s); }
            void act(LWSelection selection) {
                LWContainer.bringForward(selection);
            }
        };
    static final LWCAction SendBackward =
        new LWCAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND))
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
        new LWCAction("Font Smaller", keyStroke(KeyEvent.VK_MINUS, COMMAND))
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
        new LWCAction("Font Bigger", keyStroke(KeyEvent.VK_EQUALS, COMMAND))
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
        new LWCAction("Font Bold", keyStroke(KeyEvent.VK_B, COMMAND))
        {
            void act(LWComponent c) {
                //System.out.println("BOLDING " + c);
                Font f = c.getFont();
                int newStyle = f.getStyle() ^ Font.BOLD;;
                c.setFont(new Font(f.getName(), newStyle, f.getSize()));
            }
        };
    static final Action FontItalic =
        new LWCAction("Font Italic", keyStroke(KeyEvent.VK_I, COMMAND))
        {
            void act(LWComponent c) {
                Font f = c.getFont();
                int newStyle = f.getStyle() ^ Font.ITALIC;;
                c.setFont(new Font(f.getName(), newStyle, f.getSize()));
            }
        };

    //-------------------------------------------------------
    // Arrange actions
    //
    // todo bug: if items have a stroke width, there is an
    // error in adjustment such that repeated adjustments
    // nudge all the nodes by what looks like half the stroke width!
    // (error occurs even in first adjustment, but easier to notice
    // in follow-ons)
    //-------------------------------------------------------
    abstract static class ArrangeAction extends LWCAction
    {
        static float minX, minY;
        static float maxX, maxY;
        static float centerX, centerY;
        static float totalWidth, totalHeight; // added width/height of all in selection
        // obviously not thread-safe here
  
        private ArrangeAction(String name, KeyStroke keyStroke)
        {
            super(name, keyStroke);
        }
        private ArrangeAction(String name, int keyCode)
        {
            super(name, keyStroke(keyCode, COMMAND+SHIFT));
        }
        private ArrangeAction(String name)
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
    
            Rectangle2D.Float r = (Rectangle2D.Float) selection.getBounds();
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
            arrange(selection);
        }
        void arrange(LWSelection selection) {
            Iterator i = selection.iterator();
            while (i.hasNext())
                arrange((LWComponent) i.next());
        }
        void arrange(LWComponent c) { throw new RuntimeException("unimplemented arrange action"); }

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

    static final Action FillWidth = new ArrangeAction("Fill Width") {
            void arrange(LWComponent c) {
                c.setFrame(minX, c.getY(), maxX - minX, c.getHeight());
            }
        };
    static final Action FillHeight = new ArrangeAction("Fill Height") {
            void arrange(LWComponent c) {
                c.setFrame(c.getX(), minY, c.getWidth(), maxY - minY);
            }
        };

    // todo: java isn't allowing unmodifed arrow keys as actions! (they get ignored -- figure
    // out how to turn off whoever's grabbing them)
    static final Action NudgeUp = new LWCAction("Nudge Up", keyStroke(KeyEvent.VK_UP, SHIFT)) {
            void act(LWComponent c) {
                float unit = (float) (1.0 / VUE.getActiveViewer().getZoomFactor());
                c.translate(0, -unit);
            }
        };
    static final Action NudgeDown = new LWCAction("Nudge Down", keyStroke(KeyEvent.VK_DOWN, SHIFT)) {
            void act(LWComponent c) {
                float unit = (float) (1.0 / VUE.getActiveViewer().getZoomFactor());
                c.translate(0, unit);
            }
        };
    static final Action NudgeLeft = new LWCAction("Nudge Left", keyStroke(KeyEvent.VK_LEFT, SHIFT)) {
            void act(LWComponent c) {
                float unit = (float) (1.0 / VUE.getActiveViewer().getZoomFactor());
                c.translate(-unit, 0);
            }
        };
    static final Action NudgeRight = new LWCAction("Nudge Right", keyStroke(KeyEvent.VK_RIGHT, SHIFT)) {
            void act(LWComponent c) {
                float unit = (float) (1.0 / VUE.getActiveViewer().getZoomFactor());
                c.translate(unit, 0);
            }
        };

    static final Action AlignLeftEdges = new ArrangeAction("Align Left Edges", KeyEvent.VK_LEFT) {
            void arrange(LWComponent c) { c.setLocation(minX, c.getY()); }
        };
    static final Action AlignRightEdges = new ArrangeAction("Align Right Edges", KeyEvent.VK_RIGHT) {
            void arrange(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
        };
    static final Action AlignTopEdges = new ArrangeAction("Align Top Edges", KeyEvent.VK_UP) {
            void arrange(LWComponent c) { c.setLocation(c.getX(), minY); }
        };
    static final Action AlignBottomEdges = new ArrangeAction("Align Bottom Edges", KeyEvent.VK_DOWN) {
            void arrange(LWComponent c) { c.setLocation(c.getX(), maxY - c.getHeight()); }
        };
    static final ArrangeAction AlignCentersRow = new ArrangeAction("Align Centers in Row", KeyEvent.VK_R) {
            void arrange(LWComponent c) { c.setLocation(c.getX(), centerY - c.getHeight()/2); }
        };
    static final ArrangeAction AlignCentersColumn = new ArrangeAction("Align Centers in Column", KeyEvent.VK_C) {
            void arrange(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
        };
    static final ArrangeAction MakeRow = new ArrangeAction("Make Row", keyStroke(KeyEvent.VK_R, ALT)) {
            // todo bug: an already made row is shifting everything to the left
            // (probably always, actually)
            void arrange(LWSelection selection) {
                AlignCentersRow.arrange(selection);
                maxX = minX + totalWidth;
                DistributeHorizontally.arrange(selection);
            }
        };
    static final ArrangeAction MakeColumn = new ArrangeAction("Make Column", keyStroke(KeyEvent.VK_C, ALT)) {
            void arrange(LWSelection selection) {
                AlignCentersColumn.arrange(selection);
                maxY = minY + totalHeight;
                DistributeVertically.arrange(selection);
            }
        };
    static final ArrangeAction DistributeVertically = new ArrangeAction("Distribute Vertically", KeyEvent.VK_V) {
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            // use only *2* in selection if use our minimum layout region setting
            void arrange(LWSelection selection)
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

    static final ArrangeAction DistributeHorizontally = new ArrangeAction("Distribute Horizontally", KeyEvent.VK_H) {
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            void arrange(LWSelection selection)
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
    public static final Action[] ARRANGE_MENU_ACTIONS = {
        AlignLeftEdges,
        AlignRightEdges,
        AlignTopEdges,
        AlignBottomEdges,
        null,
        AlignCentersRow,
        AlignCentersColumn,
        null,
        FillWidth,
        FillHeight,
        null,
        MakeRow,
        MakeColumn,
        null,
        DistributeVertically,
        DistributeHorizontally,
        null,
        NudgeUp,
        NudgeDown,
        NudgeLeft,
        NudgeRight
    };
    public static final Action[] ARRANGE_SINGLE_MENU_ACTIONS = {
        NudgeUp,
        NudgeDown,
        NudgeLeft,
        NudgeRight
    };
        
    //-----------------------------------------------------------------------------
    // VueActions
    //-----------------------------------------------------------------------------
    static final Action NewMap =
        new VueAction("New Map")
        {
            private int count = 1;
            boolean undoable() { return false; }
            boolean enabled() { return true; }
            void act() {
                VUE.displayMap(new LWMap("New Map " + count++));
            }
        };
    static final Action CloseMap =
        new VueAction("Close", keyStroke(KeyEvent.VK_W, COMMAND))
        {
            // todo: listen to map viewer display event to tag
            // with currently displayed map name\
            boolean undoable() { return false; }
            public void act() {
                VUE.closeMap(VUE.getActiveMap());
            }
        };
    static final Action Undo =
        new VueAction("Undo", keyStroke(KeyEvent.VK_Z, COMMAND))
        {
            boolean undoable() { return false; }
            public void act() { VUE.getUndoManager().undo(); }

        };
    static final Action Redo =
        new VueAction("Redo", keyStroke(KeyEvent.VK_Z, COMMAND+SHIFT))
        {
            boolean undoable() { return false; }
            public void act() { VUE.getUndoManager().redo(); }
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
                // for 250ms or something
                VUE.getActiveViewer().activateLabelEdit(node);
                
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
                //VUE.getSelection().setTo(node); // also important so will be repainted (repaint optimziation only)
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
            boolean undoable() { return false; }
        };
    static final Action ZoomOut =
        new VueAction("Zoom Out", keyStroke(KeyEvent.VK_MINUS, COMMAND+SHIFT)) {
            public void act() {
                ZoomTool.setZoomSmaller(null);
            }
            boolean undoable() { return false; }
        };
    static final Action ZoomFit =
        new VueAction("Zoom Fit", keyStroke(KeyEvent.VK_0, COMMAND+SHIFT)) {
            public void act() {
                ZoomTool.setZoomFit();
            }
            boolean undoable() { return false; }
        };
    static final Action ZoomActual =
        new VueAction("Zoom 100%", keyStroke(KeyEvent.VK_1, COMMAND+SHIFT)) {
            // no way to listen for zoom change events to keep this current
            //boolean enabled() { return VUE.getActiveViewer().getZoomFactor() != 1.0; }
            public void act() {
                ZoomTool.setZoom(1.0);
            }
            boolean undoable() { return false; }
        };


    static class NewItemAction extends VueAction
    {
        static LWComponent lastItem = null;
        static Point lastMousePress = null;
        static Point2D lastLocation = null;
        
        NewItemAction(String name, KeyStroke keyStroke) {
            super(name, null, keyStroke, null);
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
    
    /**
     * LWCAction: actions that operate on one or more LWComponents
     * Provides a number of convenience methods to allow code in
     * each action to be tight & focused.
     */
    public static class LWCAction extends VueAction
        implements LWSelection.Listener
    {
        LWCAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon)
        {
            super(name, shortDescription, keyStroke, icon);
            VUE.getSelection().addListener(this);
        }
        LWCAction(String name, String shortDescription, KeyStroke keyStroke) {
            this(name, shortDescription, keyStroke, null);
        }
        LWCAction(String name) {
            this(name, null, null, null);
        }
        LWCAction(String name, Icon icon) {
            this(name, null, null, icon);
        }
        LWCAction(String name, KeyStroke keyStroke) {
            this(name, null, keyStroke, null);
        }
        void act()
        {
            LWSelection selection = VUE.getSelection();
            //System.out.println("LWCAction: " + getActionName() + " n=" + selection.size());
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

        boolean enabled() { return VUE.openMapCount() > 0 && enabledFor(VUE.getSelection()); }

        public void selectionChanged(LWSelection selection) {
            if (VUE.openMapCount() < 1)
                setEnabled(false);
            else
                setEnabled(enabledFor(selection));
        }
        void checkEnabled() {
            selectionChanged(VUE.getSelection());
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
         * to apply the action to any nodes that are children of
         * other nodes. If the child is already in selection (e.g.
         * a select all was done) be sure NOT to act on it, otherwise
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
                if (DEBUG.SELECTION) System.out.println("LWCAction: ignoring " + getActionName() + " on " + c);
            
        }
        void act(LWLink c) {
            if (DEBUG.SELECTION) System.out.println("LWCAction(link): ignoring " + getActionName() + " on " + c);
        }
        void act(LWNode c) {
            if (DEBUG.SELECTION) System.out.println("LWCAction(node): ignoring " + getActionName() + " on " + c);
        }
        
        public String toString() { return "LWCAction[" + getActionName() + "]"; }
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
                VUE.getSelection().clear();
                */
