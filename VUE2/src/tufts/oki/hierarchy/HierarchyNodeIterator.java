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
