/*
 * LWOutlineView.java
 *
 * Created on December 22, 2003, 6:28 PM
 */

package tufts.vue;

import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.BorderLayout;
import javax.swing.border.*;

/**
 * @author  Daisuke Fujiwara
 */
public class LWOutlineView extends ToolWindow
{
    private OutlineViewTree tree = null;
    
    /** Creates a new instance of LWOutlineView */
    public LWOutlineView(JFrame parent) 
    {
        super("Outline View", parent);
        
        tree = new OutlineViewTree();
        VUE.getSelection().addListener(tree);
        
        //tree.setBorder(new EmptyBorder(4,4,4,4));

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(null);
        //scrollPane.setBorder(new EmptyBorder(0,0,0,0));
        //scrollPane.addFocusListener(tree);
        
        if (true) {
            // hack to allow resize corner hits till ToolWindow can catch & redispatch mouse events:
            super.mContentPane.contentPanel.setBorder(new EmptyBorder(5,5,5,5));
            super.mContentPane.contentPanel.setBackground(Color.white);
            scrollPane.setBackground(Color.white);
        }
        
        addTool(scrollPane);
        setSize(500, 300);
    }
 
    // TODO: change tree to active map listener, and get rid
    // of this class (use ToolWindow and just add the OutlineTreeView)
    public void switchMap(LWMap map)
    {
        tree.switchContainer(map);
        //outlineLabel.setText("Map: " + map.getLabel());
    }
}
