/*
 * HierarchyTreeWindow.java
 *
 * Created on September 12, 2003, 12:55 AM
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

/**
 *
 * @author  Daisuke Fujiwara
 */
public class HierarchyTreeWindow extends InspectorWindow
{
    private DisplayAction displayAction = null;
    private JScrollPane scrollPane;
    
    /** Creates a new instance of HierarchyTreeWindow */
    public HierarchyTreeWindow(JFrame parent) 
    {
        super(parent, "Hierarchy Tree");
        setSize(500, 100);
        
        scrollPane = new JScrollPane();
        
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
    
    public void setTree()
    {
        //do something here possibly with viewport?
    }
    
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Hierarchy Tree");
        
        return (Action)displayAction;
    }
    
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
            //System.out.println("setting pathway manager in actionPerformed-DisplayAction for Map: "+VUE.getActiveViewer().getMap().getLabel());
            //setPathwayManager(VUE.getActiveViewer().getMap().getPathwayManager());
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
    
}
