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