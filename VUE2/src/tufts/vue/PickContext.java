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


/**
 *
 * This class essentially just a parameter block for picking LWTraversals.
 *
 * @version $Revision: 1.1 $ / $Date: 2006-11-30 16:48:40 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class PickContext
{
    /** Where in the tree to start picking (usually the top-level LWMap, but sometimes a sub-focal) */
    public LWComponent root;

    /** Max layer to access -- determined by the UI/viewer (currently a hack till real layers) */
    public int maxLayer = Short.MAX_VALUE;

    /** 1 means top level only, 0 will pick nothing.  Default is only a loop-preventing Short.MAX_VALUE. */
    public int maxDepth = Short.MAX_VALUE;    // we could use that to allow only root picking?

    /** If true, don't pick anything that's already selected */
    public boolean ignoreSelected = false;

    /** Don't ever pick this one particular node */
    public LWComponent excluded = null;

    /** Only pick instances of this type (some LWComponent subclass) */
    public Class pickType = null;

    /** A modifier for how deep to allow the pick: mediated by the LWComponent hierarcy with pickChild/defaultPick */
    public int pickDepth = 0;

    public PickContext() {}

    public String toString() {
        return "PickContext["
            + "root=" + root
            + " exclued=" + excluded
            + " ignoreSelected=" + ignoreSelected
            + " pickDepth=" + pickDepth
            + " maxLayer=" + maxLayer
            + " maxDepth=" + maxDepth
            + " pickType=" + pickType
            + "]";
    }

}
