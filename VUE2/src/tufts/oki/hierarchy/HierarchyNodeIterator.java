/*
 * HierarchyNodeIterator.java
 *
 * Created on October 5, 2003, 6:54 PM
 */

package tufts.oki.hierarchy;
import java.util.Vector;

/**
 *
 * @author  ptadministrator
 */
public class HierarchyNodeIterator implements osid.hierarchy.NodeIterator
{
    private Vector vector = null;
    private int index;
    
    /** Creates a new instance of HierarchyNodeIterator */
    public HierarchyNodeIterator(Vector vector) 
    {
        this.vector = vector;     
        index = 0;
    }
    
    public boolean hasNext() throws osid.hierarchy.HierarchyException
    {
        return (index < vector.size());
    }
    
    public osid.hierarchy.Node next() throws osid.hierarchy.HierarchyException
    {
        if (index >= vector.size()) 
        {
            throw new osid.hierarchy.HierarchyException("No more nodes");
        }

        return (osid.hierarchy.Node) vector.elementAt(index++);

    }
}
