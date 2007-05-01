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
import java.util.*;
import java.awt.Color;
import java.awt.geom.*;

/**
 *
 * Container for displaying slides.
 *
 * @author Scott Fraize
 * @version $Revision: 1.26 $ / $Date: 2007-05-01 18:19:24 $ / $Author: mike $
 */
public class LWSlide extends LWContainer
{
    public static final int SlideWidth = 800;
    public static final int SlideHeight = 600;
    protected static final int SlideMargin = 30;

    private LWComponent mSourceNode; // the node this slide was created from -- todo: restore on persist

    int mLayer = 0;
    
    /** public only for persistance */
    public LWSlide() {
        disableProperty(LWKey.Label);
        disableProperty(LWKey.TextColor);
        disableProperty(LWKey.StrokeWidth);
        disableProperty(LWKey.StrokeColor);
        disableProperty(LWKey.StrokeStyle);
        disableProperty(LWKey.Font);
        disableProperty(LWKey.FontSize);
        disableProperty(LWKey.FontName);
        disableProperty(LWKey.FontStyle);
    }

    LWComponent getSourceNode() {
        return mSourceNode;
    }
    void setSourceNode(LWComponent node) {
        mSourceNode = node;
    }

    public String getLabel() {
        final LWContainer parent = getParent();
        if (false && parent instanceof LWPathway)
            return super.getLabel();
        else if (parent != null) {
            if (mSourceNode == null)
                return "Slide in " + getParent().getDisplayLabel();
            else
                return "Slide for " + mSourceNode.getDisplayLabel() + " in " + getParent().getDisplayLabel();
        } else
            return "<LWSlide w/null parent>"; // true during persist restore
    }

    /** create a default LWSlide */
    public static LWSlide Create()
    {
        final LWSlide s = new LWSlide();
        s.setFillColor(new Color(0,0,0,64));
        s.setStrokeWidth(1);
        s.setStrokeColor(Color.black);
        s.setSize(SlideWidth, SlideHeight);
        //setAspect(((float)GUI.GScreenWidth) / ((float)GUI.GScreenHeight));
        s.setAspect(s.getWidth() / s.getHeight());
        return s;
    }

    public boolean isMoveable() {
        return false;
    }

    public static LWSlide CreatePathwaySlide()
    {
        final LWSlide s = Create();
        s.setStrokeWidth(0f);
        s.setFillColor(null);
        return s;
    }
    
    public static LWSlide CreateForPathway(LWPathway pathway, LWComponent node)
    {
        final LWSlide slide = CreatePathwaySlide();
        final LWNode title = NodeModeTool.buildTextNode(node.getDisplayLabel());
        final LWPathway.MasterSlide master = pathway.getMasterSlide();
        final CopyContext cc = new CopyContext(false);
        final LinkedList<LWComponent> toLayout = new java.util.LinkedList();

        slide.mSourceNode = node;
        title.setStyle(master.titleStyle);
        title.setSyncSource(node);

        for (LWComponent c : node.getAllDescendents()) {
            final LWComponent slideCopy = c.duplicate(cc);
            slideCopy.setScale(1);
            applyMasterStyle(master, slideCopy);
            slideCopy.setSyncSource(c);
            toLayout.add(slideCopy);
        }

        toLayout.addFirst(title);
        
        slide.setParent(pathway); // must do before import
        slide.importAndLayout(toLayout);
        pathway.ensureID(slide);
        
        //slide.setLocked(true);
        
        return slide;
    }

    /** slides never considered translucent: they're not on the map needing backfill when they're the focal */
    public boolean isTranslucent() {
        return false;
    }

    protected void drawImpl(DrawContext dc)
    {
        final LWSlide master = getMasterSlide();

        // We only want to fill one background.  Normally use the
        // master background, unless we have one.  Then draw the
        // master children (no fill), then draw our children (no
        // fill).
        
        if (isTransparent()) {
            dc.g.setColor(master.getFillColor());
        } else {
            dc.g.setColor(getFillColor());
        }
        dc.g.fill(getLocalShape());

            // When just filling the background with the master, only draw
            // what's in the containment box
            //dc.g.setClip(master.getBounds());
            //master.drawChildren(dc);
            //dc.g.setClip(curClip);
        
        if (master != null)
            master.drawChildren(dc);
        drawChildren(dc);
    }
    
    public void rebuild() {
        
    }

    private void applyMasterStyle(LWComponent c) {
        applyMasterStyle(getMasterSlide(), c);
    }
            
    private static void applyMasterStyle(LWPathway.MasterSlide master, LWComponent c) {
        if (master == null) {
            VUE.Log.error("null master slide applying master style to " + c);
            return;
        }
        if (c.hasResource())
            c.setStyle(master.urlStyle);
        else
            c.setStyle(master.textStyle);
    }

    public LWPathway.MasterSlide getMasterSlide() {
        if (!(getParent() instanceof LWPathway)) {
            // old on-map slides could do this
            return null;
        }
        LWPathway pathway = (LWPathway) getParent();
        if (pathway == null) {
            tufts.Util.printStackTrace("null parent attempting to get master slide");
            return null;
        } 
        return pathway.getMasterSlide();
    }

    
    protected void addChildImpl(LWComponent c)
    {
        super.addChildImpl(c);

        // TODO: need to apply proper style w/generic code
        
        if (this instanceof LWPathway.MasterSlide)
            return;
        if (DEBUG.PRESENT || DEBUG.STYLE) out("addChildImpl " + c);
        if (c.getStyle() == null)
            applyMasterStyle(c);
        
        /*
        LWPathway pathway = (LWPathway) getParent();
        if (pathway != null && c.getStyle() == null)
            c.setStyle(pathway.getMasterSlide().textStyle);
        */
    }

    // This will prevent the object from ever being drawn on the map.
    // But this bit isn't checked at the top level if this is the top
    // level object requested to draw, so it will still work on the slide viewer.
    //public boolean isDrawn() { return false; }
    //public boolean isFiltered() { return true; }
    //public boolean isHidden() { return true; }

    private void importAndLayout(List<LWComponent> nodes)
    {
        //java.util.Collections.reverse(nodes);
        final LWSelection selection = new LWSelection(nodes);

        //tufts.Util.printStackTrace("SLIDE CONTENT BOUNDS " + selection.getBounds());
        // Must import before MakeRow, as arrange actions will remove all nodes
        // parented to other nodes (auto-laid-out) before doing an arrange
        //super.importNodes(nodes);
        // prob need to layout all the children once, so they pickup layout
        // based on the fact their now in a presentation context...
        // (make row sizes are sometimes being off...)
        //Actions.MakeRow.act(selection);

        for (LWComponent c : nodes) addChildImpl(c);

        //             int x = 1, y = 1;
        //             for (LWComponent c : slide.getChildList())
        //                 c.takeLocation(x += 5, y += 5);
        int x = SlideMargin;
        int y = SlideMargin;
        // Give them crude positioning so that arrange action can figure out
        // the crude ordering based on x/y values.
        for (LWComponent c : getChildList())
            c.takeLocation(x++,y++);

        // TODO: need to know master slide at this point, or at least
        // the master slide styles...
        if (DEBUG.PRESENT || DEBUG.STYLE) out("LAYING OUT CHILDREN, parent=" + getParent());

        setSize(SlideWidth, SlideHeight);
        
        selection.setSize(SlideWidth - SlideMargin*2, SlideHeight - SlideMargin*4);
        Actions.DistributeVertically.act(selection);
        //Actions.MakeColumn.act(selection);

        /*
        Rectangle2D bounds = LWMap.getBounds(getChildIterator());
        out("slide content bounds: " + bounds);
        setSize((float)bounds.getWidth(),
                (float)bounds.getHeight());
        */


        // 640/480 == 1024/768 == 1.333...
        // Keynote uses 800/600 (1.3)
        // PowerPoint defaut 720/540  (1.3)  Based on 72dpi?  (it has a DPI option)
            
        // need to set child coords relative to the 0,0 location in virtual map space of the slide
        // but hey, when we want to show the slide on the map, it's going to have to have it's
        // map coords set to be displayed, and then all it's CHILDREN would need to get updated...
        // Sigh, maybe we need to move to relative coordinates... (if do so, need to handle old
        // save files w/absolute coords)

        // In any case, seems like it would be convenient to have the slide track the
        // location of it's parent node... actually, who cares: when go into slide mode,
        // can translate and draw the slide instead of drawing the node.
            
        //slide.setSizeFromChildren();
        
    }


    /*
    void createForNode(LWComponent node) {
        LWComponent dupeChildren = node.duplicate(); // just for children: rest of node thrown away
        java.util.List toLayout = new java.util.ArrayList();
        LWNode title = NodeTool.buildTextNode(node.getLabel()); // need to "sync" this...=
        title.setFont(node.getFont().deriveFont(java.awt.Font.BOLD));
        title.setFontSize(48);
        toLayout.add(title);
        toLayout.addAll(dupeChildren.getChildList());
        importAndLayout(toLayout);
    }
    */

    public void setLayer(int layer) {
        mLayer = layer;
    }
    
    public int getLayer() {
        //out("returning layer " + mLayer);
        return mLayer;
    }

    public boolean isPresentationContext() {
        return true;
    }
    

    /** @return false -- slides themseleves never have slide icons: only nodes that own them */
    protected boolean isDrawingSlideIcon() {
        return false;
    }

    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        if (DEBUG.PRESENT) out("PICKING CHILD: " + c);
        return c;
    }

    /** @return the slide */
    protected LWComponent defaultPick(PickContext pc) {
        //if (DEBUG.PRESENT) out("DEFAULT PICK: THIS");
        return this;
//         LWComponent dp = (pc.dropping == null ? null : this);
//         out("DEFAULT PICK: " + dp);
//         return dp;
    }

    protected LWComponent defaultDropTarget(PickContext pc) {
        LWComponent c = super.defaultDropTarget(pc);
        if (DEBUG.PRESENT) out("DEFAULT DROP TARGET: " + c);
        return c;
    }
    
}
    
    
