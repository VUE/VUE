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
import javax.swing.border.EmptyBorder;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWOutlineView extends InspectorWindow 
{
    private OutlineViewTree tree = null;
    //private JLabel outlineLabel = null;
    
    /** Creates a new instance of LWOutlineView */
    public LWOutlineView(JFrame parent) 
    {
        super(parent, "Outline View");
        setSize(500, 300);
        
        tree = new OutlineViewTree();
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(new EmptyBorder(0,0,0,0));
        
        //outlineLabel = new JLabel();
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        //getContentPane().add(outlineLabel, BorderLayout.NORTH);
        getContentPane().setBackground(Color.white);
    }
 
    public void switchMap(LWMap map)
    {
        tree.switchContainer(map);
        //outlineLabel.setText("Map: " + map.getLabel());
    }
}
