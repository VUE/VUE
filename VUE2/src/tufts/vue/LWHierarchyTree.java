/*
 * HierarchyTree.java
 *
 * Created on September 12, 2003, 12:55 AM
 */

package tufts.vue;

import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class that displays the hierarchy of nodes in a tree*/
public class LWHierarchyTree extends InspectorWindow 
{
    private DisplayAction displayAction = null;
    private JTree tree;
    private HierarchyTreeModel treeModel;
    
    /** Creates a new instance of HierarchyTreeWindow */
    public LWHierarchyTree(JFrame parent) 
    {
        super(parent, "Hierarchy Tree");
        setSize(500, 300);
        
        /**creating a hierarchy tree*/
        tree = new JTree();
        tree.setModel(null);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        //mouse listener to let the user open up the resource associated with the selected tree node
        tree.addMouseListener
        (
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent me)
                {
                    //determines the row and the path of the selected node
                    int selRow = tree.getRowForLocation(me.getX(), me.getY());
                    TreePath selPath = tree.getPathForLocation(me.getX(), me.getY());
                
                    //if there is a selected row
                    if(selRow != -1) 
                    {       
                        //if the mouse click is a double click, then display the resource associated with the node
                        if(me.getClickCount() == 2) 
                        {
                            LWNode clickedNode = 
                               (LWNode)((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
                            clickedNode.getResource().displayContent();
                        }
                    }
                }
            }
        );
        
        //tree selection listener to keep track of the selected node 
        tree.addTreeSelectionListener(
            new TreeSelectionListener() 
            {
                public void valueChanged(TreeSelectionEvent e) 
                {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        
                    if (treeNode == null) 
                      return;

                    else
                      treeModel.setSelectedNode((LWNode)treeNode.getUserObject());
                }
            }
        );

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
    
    /**Sets the model of the tree to the given hierarchy tree model
       Also stores the reference to the model*/
    public void setTree(HierarchyTreeModel treeModel)
    {
        this.treeModel = treeModel;
        tree.setModel(treeModel.getModel());
    }
    
    /**A method used by VUE to display the tree*/
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Hierarchy Tree");
        
        return (Action)displayAction;
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
