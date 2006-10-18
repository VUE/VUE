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
 * @version $Revision: 1.1 $ / $Date: 2006-10-18 17:59:43 $ / $Author: sfraize $
 */
public class LWSlide extends LWGroup
{
    public LWSlide() {
        setFillColor(new Color(0,0,0,64));
        setStrokeWidth(1);
        setStrokeColor(Color.black);
        //setAspect(((float)GUI.GScreenWidth) / ((float)GUI.GScreenHeight));
        setSize(320,240);
    }

    static LWSlide createFromList(java.util.List nodes)
    {
        LWSlide slide = new LWSlide();

        if (nodes != null && nodes.size() > 0) {
            LWSelection selection = new LWSelection(nodes);
            //tufts.Util.printStackTrace("SLIDE CONTENT BOUNDS " + selection.getBounds());
            // Must import before MakeRow, as arrange actions will remove all nodes
            // parented to other nodes (auto-laid-out) before doing an arrange
            slide.importNodes(nodes);
            // prob need to layout all the children once, so they pickup layout
            // based on the fact their now in a presentation context...
            // (make row sizes are sometimes being off...)
            Actions.MakeRow.act(selection);
            slide.setSizeFromChildren();            
        }


        return slide;
    }

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
    
    /** groups are transparent -- defer to parent for background fill color */
    public java.awt.Color getFillColor()
    {
        //return getParent() == null ? null : getParent().getFillColor();
        //return LWComponent.this.getFillColor();
        return super.fillColor;
    }

    /** @return the slide */
    protected LWComponent defaultHitComponent() {
        return this;
    }
}
    
    
