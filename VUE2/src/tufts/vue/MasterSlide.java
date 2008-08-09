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
import tufts.vue.NodeTool.NodeModeTool;

import java.util.Collection;
import java.awt.Color;
import java.awt.Font;

/**
 * A subclass of LWSlide that handles special behaviour for MasterSlides,
 * such as providing editable slide style objects when the MasterSlide
 * is the the current focal.
 *
 * @author Scott Fraize
 * @version $Revision: 1.16 $ / $Date: 2008-08-09 21:25:16 $ / $Author: sfraize $ 
 */
public final class MasterSlide extends LWSlide
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MasterSlide.class);
    
    final static String TitleLabel = "Slide Title Style";
    final static String TextLabel = "Slide Text Style";
    final static String URLLabel = "Link";
    private LWComponent headerStyle;
    private LWComponent textStyle;
    private LWComponent linkStyle;


    /** for castor persistance */
    public MasterSlide() {
        initMaster();
    }

    //         /** @return false: prevents every drawing selection on map */
    //         @Override
    //         public boolean isMapVirtual() {
    //             return true;
    //         }
        
    /** @return null -- don't create a style type for master slides */
    @Override
    public Object getTypeToken() {
        return null;
    }
    
    	

    @Override
    public final void setParent(LWContainer parent) {
        if (parent instanceof LWPathway) {
            super.setParent(parent);
            setPathwayEntry(((LWPathway)parent).asEntry());
        } else
            Util.printStackTrace(this + " master slide can't set parent to non pathway: " + parent);
    }

    @Override
    public double getScale() { return 1.0; }

    @Override
    public void takeScale(double s) {}

    @Override
    public boolean isMoveable() { return false; }

//     /** @return true */
//     @Override
//     public boolean isPathwayOwned() {
//         return true;
//     }
    

    void completeXMLRestore() {

        if (true) {
            // just in case -- some save file versions could get these messed up
            setX(0);
            setY(0);
            super.takeScale(1.0);
            takeSize(LWSlide.SlideWidth, LWSlide.SlideHeight);
        }
            

        if (headerStyle == null) {
            // all the style objects will be null for old-style (pre 2007-09-07) master slides
        
            for (LWComponent c : getChildren()) {
                if (!c.hasLabel())
                    continue;
                final String label = c.getLabel();
                
                if ("Sample Text".equals(c.getLabel()) || label.startsWith("Slide Text")) {
                    textStyle = c;
                    if (DEBUG.IO||DEBUG.PRESENT) out("FOUND OLD TEXT STYLE " + c);
                } else if (label.startsWith("Slide Title")) {
                    //} else if ("Header Text".equals(label) || label.startsWith("Slide Title")) {
                    if (DEBUG.IO||DEBUG.PRESENT) out("FOUND OLD TITLE STYLE " + c);
                    headerStyle = c;
                } else if (label.startsWith("http:")) {
                    if (DEBUG.IO||DEBUG.PRESENT) out("FOUND OLD URL STYLE " + c);
                    linkStyle = c;
                } else if (c.isStyle()) {
                    Util.printStackTrace(this + "; Missed old-master-slide style object: " + c);
                }
            }

            // old-style master slide persisted this as children: no longer -- remove them
            if (headerStyle != null) mChildren.remove(headerStyle);
            if (textStyle != null) mChildren.remove(textStyle);
            if (linkStyle != null) mChildren.remove(linkStyle);
            
            ensureStylesCreated();
            // in case we somehow missed finding the old-type style objects
        }
        
        initStyles();
        // todo for recent back compat: if styles not on master slide, add them
    }

    private void initMaster() {
        enableProperty(LWKey.Label);
        setFlag(Flag.LOCKED);
        setFlag(Flag.FIXED_LOCATION);
    }


    /** create a new master slide for a newly create pathway */
    MasterSlide(final LWPathway owner)
    {
        initMaster();
        setStrokeWidth(0);
        setFillColor(Color.black);
        setSize(SlideWidth, SlideHeight);
        setPathwayEntry(owner.asEntry());
        //if (owner != null) setFillColor(owner.getStrokeColor()); // debug: use the pathway stroke as slide color

        // Create the default items for the master slide:
            
        if (owner != null) {
            setParent(owner); // must do before initStyles
            ensureID(this);
        }
        
        ensureStylesCreated();
        initStyles();
    }

    private void ensureStylesCreated() {
        if (headerStyle == null) {
            headerStyle = NodeModeTool.buildTextNode(TitleLabel);
            headerStyle.setFont(new Font("Gill Sans", Font.PLAIN, 36));
            headerStyle.setTextColor(Color.white);
            
        }
        if (textStyle == null) {
            textStyle = NodeModeTool.buildTextNode(TextLabel);
            textStyle.setFont(new Font("Gill Sans", Font.PLAIN, 22));
            textStyle.setTextColor(Color.white);
        }
        if (linkStyle == null) {
            linkStyle = NodeModeTool.buildTextNode(URLLabel);
            linkStyle.setFont(new Font("Gill Sans", Font.PLAIN, 18));
            linkStyle.setTextColor(new Color(179, 191, 227));
        }

    }

    private void initStyles() {
        //headerStyle.takeLocation(40,30);
        headerStyle.takeLocation(0,0);
        headerStyle.setLabel("Header");
        initStyle(headerStyle);
            
        //textStyle.takeLocation(45,110);
        textStyle.takeLocation(0,0);
        textStyle.setLabel("Slide Text");
        initStyle(textStyle);
            
        //linkStyle.takeLocation(45,180);
        linkStyle.takeLocation(0,0);
        linkStyle.setLabel("Links");
        initStyle(linkStyle);

        //mFillColor.setAllowAlpha(false);

        final LWSelection s = new LWSelection(headerStyle);
        s.setTo(headerStyle);
        Actions.AlignCentersRow.act(s);
        Actions.AlignCentersColumn.act(s);

        s.setTo(textStyle);
        Actions.AlignCentersRow.act(s);
        Actions.AlignCentersColumn.act(s);
            
        s.setTo(linkStyle);
        Actions.AlignCentersRow.act(s);
        Actions.AlignCentersColumn.act(s);
            
        headerStyle.translate(0, -100);
        linkStyle.translate(0, +100);
    }

    private void initStyle(LWComponent style) {
        style.setPersistIsStyle(Boolean.TRUE);
        style.setFlag(Flag.FIXED_LOCATION);
        style.disableProperty(LWKey.Label);
        style.setMoveable(false);
        style.setParent(this); // styles are "viritual children" -- not in parent child list
        this.ensureID(style);
    }
    

        
//     private void createDefaultComponents() {
//         LWComponent header = NodeModeTool.buildTextNode("Header Text");
//         header.setFont(headerStyle.getFont().deriveFont(16f));
//         header.setTextColor(VueResources.makeColor("#b3bfe3"));

//         LWComponent footer = header.duplicate();
//         footer.setLabel("Footer Text");
//         // Now that the footer is parented, move it to lower right in it's parent
//         LWSelection s = new LWSelection(header);
//         s.setTo(header);
//         Actions.AlignRightEdges.act(s);
//         Actions.AlignTopEdges.act(s);
//         s.setTo(footer);
//         Actions.AlignRightEdges.act(s);
//         Actions.AlignBottomEdges.act(s);

//     }

    public LWComponent getTitleStyle() {
        return headerStyle;
    }
    public void setTitleStyle(LWComponent style) {
        if (DEBUG.IO||DEBUG.PRESENT) Log.debug("setHeaderStyle " + style);
        headerStyle = style;
    }
    public LWComponent getTextStyle() {
        return textStyle;
    }
    public void setTextStyle(LWComponent style) {
        if (DEBUG.IO||DEBUG.PRESENT) Log.debug("setTextStyle " + style);
        textStyle = style;
    }

    /** @return the style for hyper-links (e.g., URL's) */
    public LWComponent getLinkStyle() {
        return linkStyle;
    }
    
    public void setLinkStyle(LWComponent style) {
        if (DEBUG.IO||DEBUG.PRESENT) Log.debug("setLinkStyle " + style);
        linkStyle = style;
    }

    /** @return false: master slide can never sync */
    @Override
    public boolean canSync() {
        return false;
    }

//     /** @return false: master slide applies no styles of it's own */
//     @Override
//     boolean applyStyle(LWComponent c) {
//         return false;
//     }
    
    
//     @Override
//     public void synchronizeResources(Sync type) {
//         Util.printStackTrace("Cannot sync a MasterSlide: " + this + " type(" + type + ")");
//     }
    

    @Override
    public Color getRenderFillColor(DrawContext dc) {
        return getFillColor();
    }

    //private static final Font EditLabelFont = new Font("SansSerif", Font.PLAIN, 72);
    private static final Font EditLabelFont = new Font("Gill Sans", Font.PLAIN, 72);
    private static final Color EditLabelColorDarkBG = new Color(255,255,255,64);
    private static final Color EditLabelColorLightBG = new Color(0,0,0,64);

    // skip fancy LWComponent stuff, and draw background
    @Override
    public void draw(DrawContext dc) {
            
        // TODO: this is now over-drawn when in presentation mode
        // and even for node icons I think...  (because the master
        // slide is never the focal, and because we can't just check
        // the focal for being a slide or portal, as it could be
        // a map-view node)
        // Actually, totally recheck this.  Good enough for now tho.

        //Util.printStackTrace("DRAWING in " + dc);

        final Color bgFill = dc.getBackgroundFill();
            
        if (!getFillColor().equals(bgFill)) {
            dc.g.setColor(getFillColor());
            dc.g.fill(getZeroShape());
        }

        if (dc.focal == this) {
            dc.g.setFont(EditLabelFont);
            if (mFillColor.brightness() < 0.5)
                dc.g.setColor(EditLabelColorDarkBG);
            else
                dc.g.setColor(EditLabelColorLightBG);
            dc.g.drawString("Master Slide", 72/4, 72);
            if (true||DEBUG.Enabled) {
                //dc.g.setColor(Color.red);
                dc.g.drawString(getEntry().pathway.getLabel(), 72/4, getHeight() - 72/4);
            }
        }
        
        drawImpl(dc);
                
    }
        

//     // we could not have a special master slide object if we could handle
//     // this draw-skipping in some other way (and arbitrary nodes can be style master's)
//     // Tho having a special master-slide object isn't really that big a deal.
//     @Override
//     protected void drawChild(LWComponent child, DrawContext dc) {
//         if (dc.focal != this && !child.isMoveable())
//             return;
//         else
//             super.drawChild(child, dc);
//     }

        
    @Override
    public String getLabel() {
        if (mXMLRestoreUnderway)
            return null;
        else
        {
        	if (getEntry() != null && getEntry().pathway != null)
        		return getEntry().pathway.getLabel();
        	else
        		return null;
        }
    }

    @Override
    public void setLabel(String label) {
        if (mXMLRestoreUnderway)
            ; // ignore
        else
            getEntry().pathway.setLabel(label);
    }
        
    @Override
    public boolean hasLabel() { return true; }

    /** @return true -- always has picks, even if no proper children (the slide style objects) */
    @Override
    public boolean hasPicks() {
        return true;
    }

    // override LWSlide impl that tries to draw master slide -- only draw children -- no fill
    @Override
    protected void drawImpl(DrawContext dc) {
        drawChildren(dc);
        if (dc.focal == this) {
            headerStyle.drawLocal(dc.push()); dc.pop();
              textStyle.drawLocal(dc.push()); dc.pop();
              linkStyle.drawLocal(dc.push()); dc.pop();
        }
    }
    
    @Override
    public java.util.List<LWComponent> getPickList(PickContext pc, java.util.List<LWComponent> stored)
    {
        stored.clear();
        stored.addAll(getChildren());
        stored.add(headerStyle);
        stored.add(textStyle);
        stored.add(linkStyle);
        return stored;
    }

    
    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag, Order order)
    {
        if (kind == ChildKind.ANY) {
            bag.add(headerStyle);
            bag.add(textStyle);
            bag.add(linkStyle);
        }
        return super.getAllDescendents(kind, bag, order);
    }
    
    
    /** @return true -- is never considered "empty" */
    @Override
    public boolean hasContent() {
        return true;
    }

    /** @return true */
    @Override
    public boolean isDrawn() {
        return true;
    }

    /** @return true */
    @Override
    public boolean isVisible() {
        return true;
    }

    // For backward compatability with old save files, we
    // re-route notes to the notes stored for the pathway
    // itself.  The notes for the MasterSlide object itself
    // will remain unused.
        
    @Override
    public String getNotes() {
        return getEntry().pathway.getNotes();
    }
    @Override
    public boolean hasNotes() {
        return getEntry().pathway.hasNotes();
    }
        
    //         public String getLabel() {
    //             return "Master Slide: " + (getParent() == null ?" <unowned>" : getParent().getDisplayLabel());
    //         }
        
    public String getComponentTypeLabel() { return "Pathway/Master"; }
    //public String getComponentTypeLabel() { return "Slide<Master>"; }

}
