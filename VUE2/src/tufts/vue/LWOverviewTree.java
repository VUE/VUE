/*
 * LWOverviewTree.java
 *
 * Created on September 23, 2003, 5:18 PM
 */

package tufts.vue;

import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.Iterator;
import java.util.ArrayList;
/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWOverviewTree extends InspectorWindow implements LWContainer.LWContainerListener
{    
    private DisplayAction displayAction = null;
    private JTree tree;
    private LWMap currentMap;
    
    /** Creates a new instance of LWOverviewTree */
    public LWOverviewTree(JFrame parent) 
    {
        super(parent, "Overview Tree");
        setSize(500, 300);
     
        currentMap = null;
        
        tree = new JTree();
        tree.setModel(null);
        
        JScrollPane scrollPane = new JScrollPane(tree);
        
        getContentPane().add(scrollPane);
        getContentPane().setBackground(Color.white);
        
        super.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e) 
                {
                    displayAction.setButton(false);
                }
            }
        );
    }
    
    public LWOverviewTree(JFrame parent, LWMap map)
    {
        this(parent);
        setMap(map.getChildList(), map.getLabel());
    }
    
    public void switchMap(LWMap newMap)
    {
        if (currentMap != null)
          currentMap.removeLWContainerListener(this);
        
        currentMap = newMap;
        currentMap.addLWContainerListener(this);
        
        setMap(currentMap.getChildList(), currentMap.getLabel());
    }
    
    public void setMap(ArrayList childList, String label)
    {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(label);
        
        for(Iterator i = childList.iterator(); i.hasNext();)
        {
            LWComponent component = (LWComponent)i.next();
            
            if (component instanceof LWNode)
              rootNode.add(setUpOverview((LWNode)component));
            
            else if (component instanceof LWLink)
              rootNode.add(new DefaultMutableTreeNode((LWLink)component));
            
            else
              System.err.println("something is wrong in overview");
        }
        
        tree.setModel(new DefaultTreeModel(rootNode));
    }
    
    public DefaultMutableTreeNode setUpOverview(LWNode node)
    {   
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        
        if (node.getChildList().size() > 0)
        {
             //calls itself on the children nodes
            for (Iterator i = node.getChildIterator(); i.hasNext();)
            {
                LWComponent component = (LWComponent)i.next();
            
                if (component instanceof LWNode)
                  treeNode.add(setUpOverview((LWNode)component));
                
                else if (component instanceof LWLink)
                  treeNode.add(new DefaultMutableTreeNode((LWLink)component));
                
                else
                   System.err.println("something is wrong in overview");
            } 
        }
        
        return treeNode;
    }
     
    /**A method used by VUE to display the tree*/
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Overview Tree");
        
        return (Action)displayAction;
    }
    
    public void LWContainerEventRaised(LWContainerEvent e) 
    {
        setMap(e.getLWContainer().getChildList(), e.getLWContainer().getLabel());
    }
    
    /**A class which controls the visibility of the tree */
    private class DisplayAction extends AbstractAction
    {
        private AbstractButton aButton;
        
        public DisplayAction(String label)
        {
            super(label);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            aButton = (AbstractButton) e.getSource();
            setVisible(aButton.isSelected());
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
}
