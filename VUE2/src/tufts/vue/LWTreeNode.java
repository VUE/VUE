/*
 * LWTreeNode.java
 *
 * Created on September 28, 2003, 1:50 PM
 */

package tufts.vue;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWTreeNode extends DefaultMutableTreeNode 
{
    private boolean isLink;
    
    /** Creates a new instance of LWTreeNode */
    public LWTreeNode(LWNode node) 
    {
        super(node);
        isLink = false;
    }
    
    public LWTreeNode(LWLink link)
    {
        super(link);
        isLink = true;
    }
    
    public LWTreeNode(LWComponent component)
    {
        super(component);
        
        if (component instanceof LWLink)
            isLink = true;
    }
    
    public LWTreeNode(String label)
    {
        super(label);
        isLink = false;
    }
    
    public String toString() 
    {
       String treeNodeString = null;
       
       if (getUserObject() instanceof String)
         treeNodeString = (String)getUserObject();
       
       else
       {
            LWComponent component = (LWComponent)getUserObject();
       
            if (component instanceof LWNode)
                treeNodeString = component.getLabel();
       
            else if (component instanceof LWLink)
                treeNodeString = new String("Link:" + component.getID());
       
            else
                System.err.println("error in changing to string in LWTreeNode");
       }
       
       return treeNodeString;
    }
}
