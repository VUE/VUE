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
import tufts.vue.NodeTool.NodeModeTool;
import java.util.*;
import java.awt.Color;
import java.awt.geom.*;

/**
 *
 * Container for displaying slides.
 *
 * @author Scott Fraize
 * @version $Revision: 1.47 $ / $Date: 2007-06-05 15:09:57 $ / $Author: sfraize $
 */
public class LWSlide extends LWContainer
{
    public static final int SlideWidth = 800;
    public static final int SlideHeight = 600;
    public static final float SlideAspect = ((float)SlideWidth) / ((float)SlideHeight);
    protected static final int SlideMargin = 30;

    private LWComponent mSourceNode;

    //    int mLayer = 0;
    
    /** public only for persistance */
    public LWSlide() {
        disableProperty(LWKey.Label);
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.FillColor);
    }
    
    @Override
    public boolean supportsUserResize() {
        return isMoveable() && DEBUG.Enabled;
    }
    
    @Override
    public boolean isMoveable() {
        // at moment, if no master, this is an on-map slide (functionality being tested...)
        return getMasterSlide() == null;
    }

    /** @return false */
    @Override
    public boolean canLinkToImpl(LWComponent target) {
        return false;
    }
    

    /** @return false -- slides themseleves never have slide icons: only nodes that own them */
    @Override
    public final boolean isDrawingSlideIcon() {
        return false;
    }

    @Override
    public int getFocalMargin() {
        return 0;
    }
    
    
    @Override
    void setParent(LWContainer parent) {
        super.setParent(parent);
        if (parent instanceof LWPathway)
            ; // default
        else
            ; // testing -- can renable some properties
    }


    LWComponent getSourceNode() {
        return mSourceNode;
    }
    void setSourceNode(LWComponent node) {
        mSourceNode = node;
    }

    
    /** implemented to return the bg color of the master slide (for proper on-slide text edit fill color) */
    @Override
    public Color getRenderFillColor(DrawContext dc) {
         if (mFillColor.isTransparent()) {
             final LWSlide master = getMasterSlide();
             if (master == null)
                 return DEBUG.Enabled ? getFillColor() : Color.red;
             else
                 return master.getFillColor();
         } else
            return getFillColor();
    }

    @Override
    public String getLabel() {

        if (supportsProperty(LWKey.Label))
            return super.getLabel();
        
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
        s.setAspect(SlideAspect);
        return s;
    }

    public static LWSlide CreatePathwaySlide()
    {
        final LWSlide s = Create();
        s.setStrokeWidth(0f);
        s.setFillColor(null);
        return s;
    }
    
    public static LWSlide CreateForPathway(LWPathway pathway, LWComponent node) {
        return CreateForPathway(pathway, node.getDisplayLabel(), node, node.getAllDescendents(), false);
    }
        
    public static LWSlide CreateForPathway(LWPathway pathway, String titleText, LWComponent sourceNode, Iterable<LWComponent> contents, boolean syncTitle) 
    {
        final LWSlide slide = CreatePathwaySlide();
        final LWNode title = NodeModeTool.buildTextNode(titleText);
        final LWPathway.MasterSlide master = pathway.getMasterSlide();
        final CopyContext cc = new CopyContext(false);
        final LinkedList<LWComponent> toLayout = new java.util.LinkedList();

        slide.mSourceNode = sourceNode;
        title.setStyle(master.titleStyle);
        // if (syncTitle) title.setSyncSource(slide); doesn't seem to work in this direction (reverse is okay, but not what we want)
        if (sourceNode != null)
            title.setSyncSource(sourceNode);

        for (LWComponent c : contents) {
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
    @Override
    public boolean isTranslucent() {
        return false;
    }

    @Override
    protected void drawImpl(DrawContext dc)
    {
        final LWSlide master = getMasterSlide();
        final Color fillColor = getRenderFillColor(dc);

        if (dc.focal != this || fillColor != dc.getFill() || !fillColor.equals(dc.getFill())) {
            dc.g.setColor(fillColor);
            dc.g.fill(getLocalShape());
        }

        // When just filling the background with the master, only draw
        // what's in the containment box
        //dc.g.setClip(master.getBounds());
        //master.drawChildren(dc);
        //dc.g.setClip(curClip);
        
        // We only draw the master's children, as we've already
        // done our background fill:
        if (master != null)
            master.drawChildren(dc);

        // Now draw the slide contents:
        drawChildren(dc);
    }


    /**
     * Special case for squeezing a slide into a particular
     * frame draw context -- e.g., used for drawing a master
     * slide as the background to an arbitrary context.
     */
    public void drawIntoFrame(DrawContext dc)
    {
        dc = dc.create();
        dc.setFrameDrawing();
        final Point2D.Float offset = new Point2D.Float();
        final double zoom = ZoomTool.computeZoomFit(dc.frame.getSize(), 0, getBounds(), offset);
        dc.g.translate(-offset.x, -offset.y);
        dc.g.scale(zoom, zoom);
        if (DEBUG.BOXES || DEBUG.PRESENT || DEBUG.CONTAINMENT) {
            dc.g.setColor(Color.green);
            dc.g.setStroke(VueConstants.STROKE_TWO);
            dc.g.draw(getLocalShape());
        }
        drawRaw(dc);
    }
    
    public void rebuild() {
        
    }

    public void revertToMasterStyle() {
        //out("REVERTING TO MASTER STYLE");
        for (LWComponent c : getAllDescendents()) {
            LWComponent style = c.getStyle();
            if (style != null) {
                c.copyStyle(style);
            } else {
                // TODO: shouldn't really need this, but if anything gets detached from it's
                // style, this should re-attach it, tho we need a bit in the node
                // to know if we never want to do this: e.g. we always want a node
                // to stay "regular" node on the slide.
                applyMasterStyle(c);
            }
        }
        setFillColor(null); // this is how we revert a slide's bg color to that of the master slide
    }

    @Override
    protected void addChildImpl(final LWComponent c)
    {
        // TODO: need to apply proper style w/generic code
        
        if (this instanceof LWPathway.MasterSlide) {
            super.addChildImpl(c);
            return;
        }
        
        if (DEBUG.PRESENT || DEBUG.STYLE) out("addChildImpl " + c);

        if (c.getStyle() == null)
            applyMasterStyle(c);

        super.addChildImpl(c);

        // TODO: need a size request for LWImage, as the image itself
        // may not be loaded yet (or just auto-handle this in userSetSize,
        // or setSize or something.
        if (c instanceof LWImage) {
            ((LWImage)c).userSetSize(SlideWidth / 3, SlideWidth / 3, null); // more forgiving when image aspect close to slide aspect
//             addCleanupTask(new Runnable() { public void run() {
//                 ((LWImage)c).userSetSize(SlideWidth / 3, SlideWidth / 3, null); // more forgiving when image aspect close to slide aspect
//             }});
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                     ((LWImage)c).userSetSize(SlideWidth / 3, SlideWidth / 3, null); // more forgiving when image aspect close to slide aspect
//                   //((LWImage)c).userSetSize(SlideWidth / 3, SlideHeight / 3, null);
//             }});
        }
        
        /*
        LWPathway pathway = (LWPathway) getParent();
        if (pathway != null && c.getStyle() == null)
            c.setStyle(pathway.getMasterSlide().textStyle);
        */
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
        //else if (c instanceof LWNode && ((LWNode)c).isTextNode())
        else if (c instanceof LWNode)
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
        
        Actions.MakeColumn.act(selection);
        Actions.AlignLeftEdges.act(selection); // make column centers -- align left
        //selection.setSize(SlideWidth - SlideMargin*2, SlideHeight - SlideMargin*4);

        // now re-distribute with space between components:
        Rectangle2D bounds = selection.getBounds();
        selection.setSize((int) bounds.getWidth(),
                          (int) bounds.getHeight() + 10 * selection.size());
        Actions.DistributeVertically.act(selection);


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

//     public void setLayer(int layer) {
//         mLayer = layer;
//         throw new Error();
//     }
    
//     public int getLayer() {
//         //out("returning layer " + mLayer);
//         if(true)throw new Error();
//         return mLayer;
//     }

    public boolean isPresentationContext() {
        return true;
    }
    

    @Override
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        if (DEBUG.PICK) out("PICKING CHILD: " + c);
        return c;
    }

    /** @return this slide */
    @Override
    protected LWComponent defaultPick(PickContext pc) {
        //if (DEBUG.PRESENT) out("DEFAULT PICK: THIS");
        return this;
//         LWComponent dp = (pc.dropping == null ? null : this);
//         out("DEFAULT PICK: " + dp);
//         return dp;
    }

    @Override
    protected LWComponent defaultDropTarget(PickContext pc) {
        LWComponent c = super.defaultDropTarget(pc);
        if (DEBUG.PRESENT) out("DEFAULT DROP TARGET: " + c);
        return c;
    }
    
}
    
    
