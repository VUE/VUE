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

/*
 * HierarchyMap.java
 *
 * Created on August 1, 2003, 2:51 PM
 */

package tufts.vue;

/**
 *
 * @author Daisuke Fujiwara
 */
import java.util.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**a class which creates a hierarchy view of a map from a given node*/
public class LWHierarchyMap extends LWMap
{
    private tufts.oki.hierarchy.HierarchyViewHierarchyModel hierarchyModel = null;
    
    /** Creates a new instance of HierarchyMap */
    public LWHierarchyMap(tufts.oki.hierarchy.HierarchyViewHierarchyModel hierarchyModel, String label)
    {
        super(label);
        this.hierarchyModel = hierarchyModel;
        addAllComponents();
    }
    
    public LWHierarchyMap(String label)
    {
        super(label);
    }
    
    public void setHierarchyModel(tufts.oki.hierarchy.HierarchyViewHierarchyModel hierarchyModel)
    {
        this.hierarchyModel = hierarchyModel;
    }
         
    public tufts.oki.hierarchy.HierarchyViewHierarchyModel getHierarchyModel()
    {
        return hierarchyModel;
    }
    
    public void addAllComponents()
    {
        tufts.oki.hierarchy.HierarchyNode hierarchyNode = null;
        
        try
        {
            for(osid.hierarchy.NodeIterator i = hierarchyModel.getAllNodes(); i.hasNext();)
            {
                hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)i.next();
            
                LWComponent component = hierarchyNode.getLWComponent();
                addNode((LWNode)component);
            
                for (Iterator li = component.getLinks().iterator(); li.hasNext();)
                {
                    LWLink link = (LWLink)li.next();
                
                    //gotta think about adding a link only one time
                    if(!getChildList().contains(link)) 
                      addLink(link);
                }
            }
        }
        
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println("failed to add to the hierarchy map");
        }
    }
}