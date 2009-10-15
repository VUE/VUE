/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.vue.NodeTool.NodeModeTool;

import java.util.*;
import java.awt.Color;
import java.awt.Composite;
import java.awt.BasicStroke;
import java.awt.geom.*;

/**
 *
 * Container for displaying slides.
 *
 * @author Scott Fraize
 * @version $Revision: 1.111 $ / $Date: 2009-10-15 18:49:10 $ / $Author: sfraize $
 */
public class LWSlide extends LWContainer
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWSlide.class);
    
    public static final int SlideWidth = 800;
    public static final int SlideHeight = 600;
    // 640/480 == 1024/768 == 1.333...
    // Keynote uses 800/600 (1.3)
    // PowerPoint defaut 720/540  (1.3)  Based on 72dpi?  (it has a DPI option)
    public static final float SlideAspect = ((float)SlideWidth) / ((float)SlideHeight);
    protected static final int SlideMargin = 30;

    private transient LWPathway.Entry mEntry;

    /** public only for persistance */
    public LWSlide() {
        disableProperty(LWKey.Label);
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.FillColor);
        takeSize(SlideWidth, SlideHeight);
        takeScale(LWComponent.SlideIconScale);
    }

    /** set a runtime entry marker for this slide for use in presentation navigation */
    protected void setPathwayEntry(LWPathway.Entry e) {
        mEntry = e;
    }
    
    /** get the LWPathway.Entry for this slide if it is a pathway slide, otherwise null */
    LWPathway.Entry getPathwayEntry() {
        return mEntry;
    }

    public LWPathway.Entry getEntry() {
        return mEntry;
    }

    /** always just return child list -- slides never have extra picks (e.g., slide icons) */
    @Override
    public List<LWComponent> getPickList(PickContext pc, List<LWComponent> stored) {
        return (List) getChildren();
    }

    @Override
    public void setNotes(String s) {
        if (mEntry == null) {
            super.setNotes(s);
        } else {
            mEntry.setNotes(s);
            // may need to trigger an event so any listeners will know if this updated, tho
            // current only a single notes panel has access to this, so we can skip it.
        }
    }
    
    @Override
    public String getNotes() {
        if (mEntry == null) {
            return super.getNotes();
        } else {
            return mEntry.getNotes();
        }
    }

    /** @return false -- slides not currently allowed to have a resource */
    @Override
    public boolean hasResource() {
        return false;
    }
    
    /** do nothing */
    @Override
    public void setResource(Resource r) {}
    
    /** @return null -- sides currently not allowed to have a resource */
    @Override
    public Resource getResource() {
        return null;
    }
    
    @Override
    public boolean supportsUserResize() {
        return isMoveable() && DEBUG.Enabled;
    }

    /** @return false: slides can't be selected with anything else */
    public boolean supportsMultiSelection() {
        return isMoveable();
    }

    /** @return false: slides can never have slides */
    @Override
    public final boolean supportsSlide() {
        return false;
    }
 	
    /** @return true: slides never have slide-icon entries of their own */
    @Override
    public final boolean hasEntries() {
        return false;
    }

    @Override
    public final boolean fullyContainsChildren() {
        return true;
    }

    @Override
    public boolean isVisible() {
        if (mEntry != null)
            return super.isVisible() && mEntry.pathway.isShowingSlides();
        else
            return super.isVisible();
    }
    

    /** @return true only if we're not a pathway generated slide */
    public boolean supportsReparenting() {
        if (!super.supportsReparenting())
            return false;
        else
            return mEntry == null;
    }
    
    @Override
    public boolean isPathwayOwned() {
        return mEntry != null;
    }
    
    
    @Override
    public boolean isMoveable() {
        return isPathwayOwned() == false;
    }

    /** @return false */
    @Override
    public boolean canLinkToImpl(LWComponent target) {
        return isMoveable();
    }
    
    /** slides never considered translucent: they're not on the map needing backfill when they're the focal */
    @Override
    public boolean isTranslucent() {
        return false;
    }

    // todo: won't be able to use this when we allow variable sized slides...
    private static final Rectangle2D SlideZeroShape = new Rectangle2D.Float(0, 0, SlideWidth, SlideHeight);

    @Override
    public final java.awt.Shape getZeroShape() {
        if (mEntry == null)
            return super.getZeroShape(); // if entry is null, is an on-map slide that may have been resized
        else
            return SlideZeroShape; // as slides all same size for now, can use a constant zeroShape
    }

//     /** @return false -- slides themseleves never have slide icons: only nodes that own them */
//     @Override
//     public final boolean isDrawingSlideIcon() {
//         return false;
//     }

//     @Override
//     protected final void addEntryRef(LWPathway.Entry e) {
//         Util.printStackTrace(this + " slides can't addEntryRef " + e);
//     }
//     @Override
//     protected final void removeEntryRef(LWPathway.Entry e) {
//         Util.printStackTrace(this + " slides can't removeEntryRef " + e);
//     }

    @Override
    public int getFocalMargin() {
        return 0;
    }

    
    @Override
    protected void setParent(LWContainer parent) {
        super.setParent(parent);
        if (mEntry == null && parent instanceof LWPathway == false) { // parent will be pathway for master slides
            // if no longer a slide icon (is directly part of a map), set a fill if we didn't have one
            if (getFillColor() == null)
                setFillColor(Color.gray);
            takeScale(0.5);
            enableProperty(LWKey.Label);            
        }
        
//         if (parent instanceof LWPathway) {
//             ; // default
//         } else {
//             // This should only ever happen if a slide is drag-copied onto the map
//             if (getFillColor() == null)
//                 setFillColor(Color.black);
//         }
    }

    public LWComponent getSourceNode() {
        return mEntry == null ? null : mEntry.node;
    }
    
    
    @Override
    public String getLabel() {

        if (supportsProperty(LWKey.Label))
            return super.getLabel();
        
        if (mEntry != null) {
            if (getSourceNode() == null)
                return "Slide in " + mEntry.pathway.getDisplayLabel();
            else
                return "Slide for " + getSourceNode().getDisplayLabel() + " in " + mEntry.pathway.getDisplayLabel();
        } else
            return "<LWSlide:initializing>"; // true during persist restore
    }

    /** create a default LWSlide */
    public static LWSlide instance()
    {
        final LWSlide s = new LWSlide();
        //s.setFillColor(new Color(0,0,0,64));
        s.setFillColor(Color.black);
        s.setStrokeWidth(0);
        //s.setStrokeColor(Color.black);
        s.setSize(SlideWidth, SlideHeight);
        //setAspect(((float)GUI.GScreenWidth) / ((float)GUI.GScreenHeight));
        s.setAspect(SlideAspect);
        return s;
    }

    public boolean canSync() {
        return mEntry != null && !mEntry.isMapView();
    }

    //public void rebuild() {}

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


    private static abstract class SlideStylingTraversal extends LWTraversal {
        SlideStylingTraversal() {
            super(null);
        }

        @Override
        public boolean acceptChildren(LWComponent c) {
            return c instanceof LWGroup == false && c.hasChildren();
        }

        @Override
        public boolean acceptTraversal(LWComponent c) {
            return true;
        }        
        
        @Override
        public boolean accept(LWComponent c) {
            return c instanceof LWGroup == false && c instanceof LWSlide == false;
        }

        abstract public void visit(LWComponent c);
//         @Override
//         public void visit(LWComponent c) {
//         }

        
        
    }


    @Override
    public void XML_completed(Object context) {

        // We shouldn't need this anymore now that we persist slide style,
        // tho we're leaving it in for at least a while to catch recently
        // made presentations -- SMF 2007-11-16
        
        new SlideStylingTraversal() {
            public void visit(LWComponent c) {
                c.setFlag(Flag.SLIDE_STYLE);
                //out("slide bit: " + c);
            }
        }.traverse(this);
        super.XML_completed(context);
    }
    

    private void applyMasterStyle(LWComponent node) {
        final MasterSlide master = getMasterSlide();

        new SlideStylingTraversal() {
            public void visit(LWComponent c) {
                applyMasterStyle(master, c);
            }
        }.traverse(node);
    }
            
    private static void applyMasterStyle(MasterSlide master, LWComponent c) {
        if (master == null) {
            Log.error("NULL MASTER SLIDE: can't apply master style to " + c);
            return;
        }
        
        c.setFlag(Flag.SLIDE_STYLE);
        c.mAlignment.set(Alignment.LEFT);
        
        //if (c.hasResource() && !c.hasChildren())
        if (c.hasResource() && !c.getResource().isImage())
            c.setStyle(master.getLinkStyle());
        else if (c instanceof LWPortal)
            ; // do nothing
        else if (c instanceof LWText)
            c.setStyle(master.getTextStyle());
        else if (c instanceof LWNode)
            c.setStyle(master.getTextStyle());

        if (DEBUG.Enabled) {
            if (c.getStyle() == null)
                track("skip-style", String.format("%-14s %s",
                                               "(" + (c.getTypeToken() instanceof Class ? ((Class)c.getTypeToken()).getSimpleName() : c.getTypeToken()) + ")",
                                               c));
            else
                track("styled-as", String.format("%-14s %s", c.getStyle().getLabel()+";", c));
        }
    }


    /** @return true if adjusted */
    boolean applyStyle(LWComponent c) {
        
        //if (DEBUG.PRESENT || DEBUG.STYLE)
        //track("styling", c + "; curStyle=" + c.getStyle());
        
        c.setFlag(Flag.SLIDE_STYLE);
        if (c.getStyle() == null)
            applyMasterStyle(c);

//         if (LWNode.isImageNode(c))
//             c.mAlignment.set(Alignment.RIGHT);

       return true;
    }

    private static void track(String where, Object o) {
        if (DEBUG.Enabled)
            Log.debug(String.format("%16s: %s",
                                    where,
                                    o instanceof LWComponent ? o : (o instanceof String ? o : Util.tags(o))));
    }
    
//     @Override
//     public void dropChildren(Iterable<LWComponent> iterable) {
//         track("dropChildren", iterable);

//         // TODO: this is being called twice during drops: a MapDropTarget bug, which has
//         // probably been there a long time... as drop operations are generally
//         // idempotent once the new nodes have been created, it was hard to notice, tho
//         // it makes debugging confusing, and it's bound to break something at some
//         // point.
            
//         if (DEBUG.DND) new Throwable("dropChildren " + iterable).printStackTrace();
//         pasteChildren(iterable);
//     }
//     @Override
//     public void pasteChildren(Iterable<LWComponent> iterable) {

//         track("pasteChildren", iterable);

//         for (LWComponent c : iterable) {

//             if (!applyStyle(c))
//                 continue;
            
//             // TODO: need a size request for LWImage, as the image itself
//             // may not be loaded yet (or just auto-handle this in userSetSize,
//             // or setSize or something.
//             if (c instanceof LWImage) {
//                 ((LWImage)c).userSetSize(SlideWidth / 4, SlideWidth / 4, null);
//                 track("resized", c);
//                 // todo: we actually want this to happen after we're sure we know the image's aspect,
//                 // which we won't if it's slowly loading -- perhaps we can handle this via a cleanup task.
//             }
//         }

//         super.pasteChildren(iterable);
//     }

    @Override
    protected void addChildImpl(LWComponent c, Object context)
    {
        track("addChildImpl/"+context, c);

        if (context == ADD_PASTE || context == ADD_DROP) {

            applyStyle(c);

            // TODO: need a size request for LWImage, as the image itself
            // may not be loaded yet (or just auto-handle this in userSetSize,
            // or setSize or something.
            if (c instanceof LWImage && !(c.getClientData(LWKey.OLD_PARENT) instanceof LWSlide)) {
                ((LWImage)c).userSetSize(SlideWidth / 4, SlideWidth / 4, null);
                track("resized", c);
                // todo: we actually want this to happen after we're sure we know the image's aspect,
                // which we won't if it's slowly loading -- perhaps we can handle this via a cleanup task.
            }
            
        }
        
        super.addChildImpl(c, context);
    }

//     /** Return true if our parent is the given pathway (as slides are currently owned by the pathway).
//      * If our parent is NOT a pathway, use the default impl.
//      */
    @Override
    public boolean inPathway(LWPathway p)
    {
        if (mEntry == null)
            return super.inPathway(p);
        else
            return mEntry.pathway == p;
//         if (getParent() instanceof LWPathway)
//             return getParent() == p;
//         else
//             return super.inPathway(p);
    }
    

    public MasterSlide getMasterSlide() {
        if (mEntry == null)
            return null;
        else
            return mEntry.pathway.getMasterSlide();
    }


    @Override
    public boolean isPresentationContext() {
        return true;
    }

    /** implemented to return the bg color of the master slide (for proper on-slide text edit fill color) */
    @Override
    public Color getRenderFillColor(DrawContext dc) {
         if (mFillColor.isTransparent()) {
             final LWSlide master = getMasterSlide();
             if (master == null)
                 return getFillColor();
             else
                 return master.getFillColor();
         } else
            return getFillColor();
    }

    @Override
    public Color getFinalFillColor(DrawContext dc) {
        Color c = getRenderFillColor(dc);
        return c == null ? super.getFinalFillColor(dc) : c;
    }

    public boolean isSlideIcon() {
        return mEntry != null;
    }

    @Override
    protected void drawImpl(DrawContext dc)
    {
        boolean drewBorder = false;
        
        final boolean onMapSlideIcon = isSlideIcon() && (dc.focal != this || dc.focused == this);
            
        if (onMapSlideIcon && dc.drawPathways()) {
            // we have an entry: draw a pathway hilite
            if (dc.isPresenting() || getEntry() == VUE.getActiveEntry()) {
                dc.g.setColor(getEntry().pathway.getColor());
                dc.g.setStroke(SlideIconPathwayStroke);
                dc.g.draw(getZeroShape());
                drewBorder = true;
            }
        } else if (dc.focal != this) {
            if (isSelected() && dc.isInteractive()) {
                // for on-map slides only: draw regular selection border if selection
                dc.g.setColor(COLOR_HIGHLIGHT);
                dc.setAbsoluteStroke(getStrokeWidth() + SelectionStrokeWidth);
                dc.g.draw(getZeroShape());
                drewBorder = true;
            }
        }

        final LWSlide master = getMasterSlide();
        final Color fillColor = getRenderFillColor(dc);

        if (fillColor == null) {
            if (DEBUG.Enabled) Util.printStackTrace("null fill " + this);
        }
//         else if (!dc.isClipOptimized()) {

//             // if we're doing a zoomed-rollover of a slide, and the slide fill happens
//             // to be the same as the map fill, (e.g., white), this will have no effect,
//             // and the zoomed slide will zoom up with no fill (see-through).  This is a
//             // bit of a hack in that we're depending on knowing that the zoomed rollover
//             // is drawn with clip optimization off.
            
//             dc.g.setColor(fillColor);
//             dc.g.fill(getZeroShape());
            
//         }
        else {

            // This should be an even safer test...  tho why are white text slide
            // titles on black slides *sometimes* dissapearing when auto-zoomed?
            // Could this have anything to do with our hack to auto-patch-up
            // text colors on matching backgrounds?
            if (fillColor.equals(dc.getBackgroundFill())) {
                dc.g.setColor(fillColor);
                dc.g.fill(getZeroShape());
            } else {
                dc.fillArea(getZeroShape(), fillColor);
            }
        }
        
        if (master != null) {
            // As the master slide isn't in the model, sit's children can't succesfully know
            // their bounds anyway, so we can't clip-optimize further when we draw it.
            // (It would be of little help anyway)
            final DrawContext masterDC = dc.push();
            masterDC.setClipOptimized(false);
            // We only draw the master's children, as we've already
            // done our background fill:
            master.drawChildren(masterDC);
            dc.pop();
        }

        if (DEBUG.BOXES) {
            dc.g.setColor(Color.red);
            dc.setAbsoluteStroke(1);
            dc.g.draw(getZeroShape());
        } else if (onMapSlideIcon && !drewBorder /*&& !dc.isAnimating()*/) {
            // force a basic slide-icon border in case fill has no contrast w/background
            dc.g.setColor(Color.darkGray);
            dc.g.setStroke(STROKE_FIVE);
            dc.g.draw(getZeroShape());
        }
        
        if (dc.focal != this)
            dc.g.clip(getZeroShape());

        // Now draw the slide contents:
        drawChildren(dc);
    }
    
    @Override
    public boolean canDuplicate() {
        return DEBUG.META; // testing only
    }
    
    @Override
    public LWSlide duplicate(CopyContext cc)
    {
        if (!DEBUG.Enabled)
            return null;
        
        LWSlide newSlide = (LWSlide) super.duplicate(cc);

        if (newSlide.mEntry == null && newSlide.isTransparent() && getMasterSlide() != null) {
            // a dupe of an on-pathway slide will have no fill -- if it ended up
            // w/out an entry (the default), grab it's last fill color.
            newSlide.setFillColor(getMasterSlide().getFillColor());
        }

        return newSlide;
    }
    
    @Override
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        // todo: a VueAction or LWCAction fire call that takes an InputEvent and an LWComponent so we
        // can track the source of these events

        
        if (e == null) { // special case message from MapViewer

            // can't use SHIFT, as 2nd click de-selects, option/command also seem problematic on Mac

            // TODO: this action depends on this slide having been selected by
            // the first click, and the pathway entrie being made the active entry
            // before the action fires -- we should be passing in the LWSlide.

            // Too hairy for now: leaves the hand tool selected;
            //VUE.getSelection().setTo(this);
            if (DEBUG.Enabled) 
                Actions.LaunchPresentation.fire("MapViewerHandToolDoubleClick");
            //Actions.LaunchPresentation.fire(e);
            
        } else {
            Actions.EditSlide.act(this);
        }
        return true;
    }
    

    @Override
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        //if (DEBUG.PICK) out("PICKING CHILD: " + c);
        if (pc.root == this || (!SwapFocalOnSlideZoom && pc.dc.zoom > 4))
            return c;
        else
            return this;
    }

//     /** @return this slide */
//     @Override
//     protected LWComponent defaultPick(PickContext pc) {
//         //if (DEBUG.PRESENT) out("DEFAULT PICK: THIS");
//         return this;
// //         LWComponent dp = (pc.dropping == null ? null : this);
// //         out("DEFAULT PICK: " + dp);
// //         return dp;
//     }

    @Override
    protected LWComponent defaultDropTarget(PickContext pc) {
        LWComponent c = super.defaultDropTarget(pc);
        if (DEBUG.PRESENT) out("DEFAULT DROP TARGET: " + c);
        return c;
    }
    
}
    
    
