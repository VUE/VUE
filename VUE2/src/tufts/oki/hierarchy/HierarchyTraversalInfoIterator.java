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
 * HierarchyTraversalInfoIterator.java
 *
 * Created on October 5, 2003, 7:13 PM
 */

package tufts.oki.hierarchy;

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
