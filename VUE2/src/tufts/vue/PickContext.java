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


/**
 *
 * This class essentially just a parameter block for picking LWTraversals.
 *
 * @version $Revision: 1.12 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * @author Scott Fraize
 *
 */
public class PickContext
{
    public interface Acceptor {
        public boolean accept(PickContext pc, LWComponent c);
    }
        
    
    /** Where in the tree to start picking.  Usually this is top-level object in the MapViewer (the focal), but sometimes a sub-focal. */
    public LWComponent root;
    
    /** the DrawContext that produced the current display */
    public final DrawContext dc;

    /** Max layer to access -- determined by the UI/viewer (currently a hack till real layers) */
    public int maxLayer = Short.MAX_VALUE;

    /** 1 means top level only, 0 will pick nothing.  Default is only a loop-preventing Short.MAX_VALUE. */
    public int maxDepth = Short.MAX_VALUE;    // we could use that to allow only root picking?

    /** If true, don't pick anything that's already selected */
    public boolean ignoreSelected = false;

    /** If non-null, we're picking for a valid drop target for this object */
    public Object dropping;

    /** Don't ever pick this one particular node */
    public LWComponent excluded = null;

    /** If true, special picking rules for the zoomed rollover pick */
    public boolean isZoomRollover;

    /** Only pick instances of this type (some LWComponent subclass) */
    //public Class pickType = null;
    public Acceptor acceptor = null;

    /** A modifier for how deep to allow the pick: mediated by the LWComponent hierarcy with pickChild/defaultPick */
    public int pickDepth = 0;

    /** An optional zoom scale can be provided for more accurately adjusting for slop (near misses), etc. */
    public float zoom = 1f;
    
    /** The location being picked, or start of region for region pick. */
    public final float x, y;

    /** If this is a region-pick, the size of the region */
    public final float width, height;


    public PickContext(DrawContext dc, float x, float y) {
        this.dc = dc;
        this.x = x;
        this.y = y;
        width = height = 0;
    }
//     public PickContext() {
//         this(Float.NaN,Float.NaN);
//     }
    public PickContext(DrawContext dc, java.awt.geom.Rectangle2D.Float r) {
        this.dc = dc;
        x = r.x;
        y = r.y;
        width = r.width;
        height = r.height;
    }
    public PickContext(DrawContext dc, java.awt.geom.Rectangle2D r) {
        this.dc = dc;
        x = (float) r.getX();
        y = (float) r.getY();
        width = (float) r.getWidth();
        height = (float) r.getHeight();
    }

    public boolean isPointPick() {
        return width <= 0 || height <= 0;
    }
    
    public boolean isRegionPick() {
        return width > 0 && height > 0;
    }

    public String toString() {
        return String.format("PickContext[%.1f,%.1f", x, y)
            + " root=" + root
            + " exclued=" + excluded
            + " dropping=" + dropping
            + " ignoreSelected=" + ignoreSelected
            + " pickDepth=" + pickDepth
            + " maxLayer=" + maxLayer
            + " maxDepth=" + maxDepth
            + " acceptor=" + acceptor
            //+ " pickType=" + pickType
            + "]";
    }

}
