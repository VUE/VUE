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
