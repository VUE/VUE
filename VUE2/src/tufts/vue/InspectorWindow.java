/*
 * InspectorWindow.java
 *
 * Created on June 25, 2003, 10:27 AM
 *
 * This class can be used to create a stand alone window thats always
 * on top of the main app. It can be used instead of the ToolWindow class
 * to allow editing of fields and the ability for the user to close the
 * window.
 */

package tufts.vue;

import javax.swing.*;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;

/**
 *
 * @author  Jay Briedis
 */
public class InspectorWindow extends JDialog {
    
    private JFrame owner = null;
    private Point location = new Point(300, 300);
    private Dimension size = new Dimension(200, 200);

    /**handles opening and closing inspector*/
    private AbstractButton mButton = null;
    
    
    public InspectorWindow() {
        this((JFrame)null, "Default Window");
    }
    
    public InspectorWindow(JFrame owner, String title, Point loc){
        this(owner, title);
        this.location = loc;
    }
    
    public InspectorWindow(JFrame owner, String title) {
        super(owner, title);
        this.owner = owner;
        this.setModal(false);
        this.setLocation(location);
        this.setSize(size);

        /**unselects checkbox in VUE window menu on closing*/
        super.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {setButton(false);}});
        
    }

    public void setButton(boolean state){
        mButton.setSelected(state);
    }

    /**handles opening and closing window from menu list*/
    class DisplayAction extends AbstractAction
    {
        public DisplayAction(String label)
        {
            super(label);
        }
        public void actionPerformed(ActionEvent e)
        {
            mButton = (AbstractButton) e.getSource();
            setVisible(mButton.isSelected());
        }
    }
    
    Action displayAction = null;
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction(getTitle() == null ? getClass().toString() : getTitle());
        return displayAction;
    }
   
    
}
