/*
 * LWOutlineView.java
 *
 * Created on December 22, 2003, 6:28 PM
 */

package tufts.vue;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import java.awt.Color;
/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWOutlineView extends InspectorWindow 
{
    //private DisplayAction displayAction = null;
    private OutlineViewTree tree = null;

    /** Creates a new instance of LWOutlineView */
    public LWOutlineView(JFrame parent) 
    {
        super(parent, "Outline View");
        setSize(500, 300);
        
        tree = new OutlineViewTree();
        JScrollPane scrollPane = new JScrollPane(tree);
        
        getContentPane().add(scrollPane);
        getContentPane().setBackground(Color.white);

        /*
        super.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e) 
                {
                    displayAction.setButton(false);
                }
            }
        );
        */
    }

    public OutlineViewTree getTree() 
    {
        return tree;
    }
    

    /**A method used by VUE to display the tree*/
    /*
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Outline View");
        
        return (Action)displayAction;
    }
    */
    /**A class which controls the visibility of the tree */
    /*
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
    */
}
