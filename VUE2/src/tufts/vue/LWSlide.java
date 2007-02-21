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
import java.awt.Color;
import java.awt.geom.*;

/**
 *
 * Sublcass (for now) of LWGroup for slide features.
 *
 * @author Scott Fraize
 * @version $Revision: 1.7 $ / $Date: 2007-02-21 00:24:48 $ / $Author: sfraize $
 */
public class LWSlide extends LWGroup
{
    protected static final int SlideWidth = 800;
    protected static final int SlideHeight = 600;
    protected static final int SlideMargin = 30;

    int mLayer = 0;
    
    /** for persistance */
    public LWSlide() {}

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

    public static LWSlide CreatePathwaySlide()
    {
        final LWSlide s = Create();
        s.setStrokeWidth(0f);
        s.setFillColor(null);
        return s;
    }
    

    /*
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        if (pc.pickDepth > 0)
            return c;
        else
            return this;
    }
    */
    
    public String getLabel() {
        final LWContainer parent = getParent();
        if (parent instanceof LWPathway)
            return super.getLabel();
        else if (parent != null)
            return "Slide for " + getParent().getLabel();
        else
            return "<LWSlide w/null parent>"; // true during persist restore
    }

    public void setLayer(int layer) {
        mLayer = layer;
    }
    
    public int getLayer() {
        //out("returning layer " + mLayer);
        return mLayer;
    }

    public LWSlide getSlide() {
        return null;
    }

    public static LWSlide CreateForPathway(LWPathway pathway, LWComponent node)
    {
        final LWSlide slide = CreatePathwaySlide();

        java.util.List<LWComponent> toLayout = new java.util.ArrayList();
        LWNode title = NodeTool.buildTextNode(node.getDisplayLabel()); // need to "sync" this...=

        
        LWComponent dupeChildren = node.duplicate(); // just for children: rest of node thrown away
        toLayout.add(title);
        toLayout.addAll(dupeChildren.getChildList());

        for (LWComponent c : toLayout)
            c.setParentStyle(pathway.getMasterSlide().textStyle);

        slide.importAndLayout(toLayout);
        slide.setParent(pathway);
        
        //slide.setLocation(getX(), getY() + getHeight() + 20);
        //slide.setScale(0.125f);
        //slide.setLocation(Short.MAX_VALUE, Short.MAX_VALUE);
        //slide.setLocked(true);
        //slide.setLayer(1); // effective make invisible on the map map
        //getMap().addChild(slide);
        return slide;
    }

    
    // This will prevent the object from ever being drawn on the map.
    // But this bit isn't checked at the top level if this is the top
    // level object requested to draw, so it will still work on the slide viewer.
    //public boolean isDrawn() { return false; }
    //public boolean isFiltered() { return true; }
    //public boolean isHidden() { return true; }

    static LWSlide CreateFromList(java.util.List<LWComponent> nodes)
    {
        final LWSlide slide = Create();

        if (nodes != null && nodes.size() > 0)
            slide.importAndLayout(nodes);

        return slide;
    }
    
    void importAndLayout(java.util.List<LWComponent> nodes)
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

        for (LWComponent c : nodes) {
            addChildImpl(c);
        }

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
        if (DEBUG.Enabled) out("LAYING OUT CHILDREN, parent=" + getParent());

        selection.setSize(SlideWidth - SlideMargin*2,
                          SlideHeight - SlideMargin*2);
        Actions.MakeColumn.act(selection);

        /*
        Rectangle2D bounds = LWMap.getBounds(getChildIterator());
        out("slide content bounds: " + bounds);
        setSize((float)bounds.getWidth(),
                (float)bounds.getHeight());
        */

        setSize(SlideWidth, SlideHeight);

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

    public boolean isPresentationContext() {
        return true;
    }
    
    void setScale(float scale)
    {
        // for now: LWGroup disables
        this.scale = scale;
        notify(LWKey.Scale);
    }

    public boolean intersects(Rectangle2D rect) {
        return rect.intersects(getBounds());
    }

    public boolean supportsUserResize() {
        return true;
    }
    
    /* groups were transparent -- restore use of fill color: undo group override */
    /*
    public java.awt.Color getFillColor()
    {
        //return getParent() == null ? null : getParent().getFillColor();
        //return LWComponent.this.getFillColor();
        return super.fillColor;
    }
    */

    /** @return the slide */
    protected LWComponent defaultPick(PickContext pc) {
        return this;
    }
}
    
    
