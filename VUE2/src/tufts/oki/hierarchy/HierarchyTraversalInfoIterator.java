/*
 * HierarchyTraversalInfoIterator.java
 *
 * Created on October 5, 2003, 7:13 PM
 */

package VUEDevelopment.src.tufts.oki.hierarchy;

import java.util.Vector;
/**
 *
 * @author  ptadministrator
 */
public class HierarchyTraversalInfoIterator implements osid.hierarchy.TraversalInfoIterator
{
    private Vector vector = null;
    private int index;
    
    /** Creates a new instance of HierarchyNodeIterator */
    public HierarchyTraversalInfoIterator(Vector vector) 
    {
        this.vector = vector;     
        index = 0;
    }
    
    public boolean hasNext() throws osid.hierarchy.HierarchyException
    {
        return (index < vector.size());
    }
    
    public osid.hierarchy.TraversalInfo next() throws osid.hierarchy.HierarchyException
    {
        if (index >= vector.size()) 
        {
            throw new osid.hierarchy.HierarchyException("No more traversal info");
        }

        return (osid.hierarchy.TraversalInfo) vector.elementAt(index++);
    }
}
