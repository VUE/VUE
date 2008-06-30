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
 * HierarchyTraversalInfo.java
 *
 * Created on October 5, 2003, 7:17 PM
 */

package tufts.oki.hierarchy;

/**
 *
 * @author  ptadministrator
 */
public class HierarchyTraversalInfo implements osid.hierarchy.TraversalInfo 
{
    //should i just implement this in node form?
    private osid.shared.Id id;
    private String name;
    private int level;
    
    /** Creates a new instance of HierarchyTraversalInfo */
    public HierarchyTraversalInfo(osid.shared.Id id, String name, int level)
    {
        this.id = id;
        this.name = name;
        this.level = level;
    }
    
    public osid.shared.Id getNodeId() throws osid.hierarchy.HierarchyException
    {
       return id; 
    }
    
    public java.lang.String getDisplayName() throws osid.hierarchy.HierarchyException
    {
       return name;
    }
    
    public int getLevel() throws osid.hierarchy.HierarchyException
    {
       return level;
    }
}
