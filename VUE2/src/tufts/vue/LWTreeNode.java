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
    /** Creates a new instance of LWTreeNode */
    public LWTreeNode(LWComponent component)
    {
        super(component);
    }
    
    public LWTreeNode(String label)
    {
        super(label);
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
            {
                if (component.getLabel() == null)
                  treeNodeString = new String("Link:" + component.getID());
       
                else
                  treeNodeString = component.getLabel();
            }
            
            else
                System.err.println("error in changing to string in LWTreeNode");
       }
       
       return treeNodeString;
    }
}
