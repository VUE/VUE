/*
 * HierarchyTraversalInfo.java
 *
 * Created on October 5, 2003, 7:17 PM
 */

package VUEDevelopment.src.tufts.oki.hierarchy;

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
