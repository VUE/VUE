/*
 * PathwayControl.java
 *
 * Created on June 19, 2003, 11:33 AM
 */

package tufts.vue;

import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.border.LineBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class that displays the control of pathways*/
public class PathwayControl extends InspectorWindow implements ActionListener, ItemListener
{
    /**Necessary widgets to control pathways*/
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JButton removeButton; //temporarily existing
    private JLabel nodeLabel;
    private JComboBox pathwayList;
    
    //pathway currently being kept track of
    private LWPathwayManager pathwayManager = null;
    
    private final String noPathway = "";
    private final String addPathway = "add a new pathway";
    private final String emptyLabel = "empty";
    
    private DisplayAction displayAction = null;
    
    /** Creates a new instance of PathwayControl */
    public PathwayControl(JFrame parent) 
    {   
        super(parent, "Pathway Control");
        setSize(500, 100);
        
        pathwayList = new JComboBox();
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.white);
        
        firstButton = new JButton("<<");
        lastButton = new JButton(">>");
        forwardButton = new JButton(">");
        backButton = new JButton("<");
        nodeLabel = new JLabel(emptyLabel);
        
        /**
        firstButton.setEnabled(false);
        lastButton.setEnabled(false);
        forwardButton.setEnabled(false);
        backButton.setEnabled(false);
        */
        
        firstButton.addActionListener(this);
        lastButton.addActionListener(this);
        forwardButton.addActionListener(this);
        backButton.addActionListener(this);
        
        //temporarily here
        removeButton = new JButton("Remove Pathway");
        removeButton.addActionListener(this);
        
        pathwayList.setRenderer(new pathwayRenderer());
        pathwayList.addItemListener(this);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.white);
        buttonPanel.add(removeButton);
        buttonPanel.add(firstButton);
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(lastButton);
      
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout());
        descriptionPanel.setBackground(Color.white);
        descriptionPanel.add(new JLabel("Label:"));
        descriptionPanel.add(nodeLabel);
        
        JPanel mainPanel = new JPanel();
        mainPanel.add(pathwayList);
        mainPanel.add(buttonPanel);
        
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(descriptionPanel, BorderLayout.NORTH);
        
        super.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e) 
                {
                    displayAction.setButton(false);
                }
            }
        );
    }
    
    /**A constructor with a pathway manager as an argument*/
    public PathwayControl(JFrame parent, LWPathwayManager pathwayManager)
    {
        this(parent);
        setPathwayManager(pathwayManager);
    }
    
    /**Sets the pathway manager to the given pathway manager*/
    public void setPathwayManager(LWPathwayManager pathwayManager)
    {
        this.pathwayManager = pathwayManager;
        
        //clears the combo box list 
        //come up with a better way
        pathwayList.removeAllItems();
        pathwayList.addItem(noPathway);
        pathwayList.addItem(addPathway);
        
        //iterting through to add existing pathways to the combo box list
        for (Iterator i = pathwayManager.getPathwayIterator(); i.hasNext();)
           pathwayList.addItem((LWPathway)i.next());           
        
        //sets the current pathway to the current pathway of the manager
        if ((pathwayManager.getCurrentPathway() != null) 
            && (pathwayManager.getCurrentPathway().getCurrent() == null) )
          pathwayManager.getCurrentPathway().getFirst();
        pathwayList.setSelectedItem(pathwayManager.getCurrentPathway());
        
        updateControlPanel();
    }
    
    /**Returns the currently associated pathway manager*/
    public LWPathwayManager getPathwayManager()
    {
        return pathwayManager;
    }
    
    /**Adds the given pathway to the combo box list and the pathway manager*/
    public void addPathway(LWPathway newPathway)
    {
        pathwayList.addItem(newPathway);
        pathwayManager.addPathway(newPathway);
        
        //switches to the newly added pathway
        pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);        
    }
    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        this.getPathwayManager().setCurrentPathway(pathway);
        
        //sets to the first node if there is no current node set
        if(pathway != null)
        {
            if ((pathway.getCurrent() == null) && (pathway.getFirst() != null) )
              pathway.getFirst();
        }       
        
        updateControlPanel(); 
    }
    
    /**Returns the currently selected pathway*/
    public LWPathway getCurrentPathway()
    {
        return this.getPathwayManager().getCurrentPathway();
    }
    
    /** sets the active pathway as current position in pathway menu*/
//    public void setCurrentPosition(){
//        if(this.getPathwayManager() != null && 
//            this.getPathwayManager().getCurrentPathway() != null)
//                this.pathwayList.setSelectedItem(this.getPathwayManager().getCurrentPathway().getLabel());
//        //System.out.println("setting the current position to: " + this.getPathwayManager().getCurrentPathway().getLabel());
//    }
    
    /**Saves the current pathway so that it can be restored next time the pathway manager is chosen*/
    /*public void saveCurrentPathway()
    {
         if (pathwayManager != null)
         {
            //System.out.println("setting the current pathway in the manager to : " + currentPathway.getLabel());
            this.setCurrentPathway(currentPathway);
         }
    }*/
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PathwayControl control = new PathwayControl(null);
        control.show();
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        //if there is a pathway currently selected
        if (this.getCurrentPathway() != null)
        {
            LWComponent currentElement = this.getCurrentPathway().getCurrent();
            
            //temporarily here 
            removeButton.setEnabled(true);
            
            //if there is a node in the pathway
            if(currentElement != null)
            {
                //sets the label to the current node's label
                nodeLabel.setText(currentElement.getLabel());
          
                //if it is the first node in the pathway, then disables first and back buttons
                if (this.getCurrentPathway().isFirst())
                {
                    backButton.setEnabled(false);
                    firstButton.setEnabled(false);
                }
          
                else
                {
                    backButton.setEnabled(true);
                    firstButton.setEnabled(true);
                }
          
                //if it is the last node in the pathway, then disables last and forward buttons
                if (this.getCurrentPathway().isLast())
                {
                    forwardButton.setEnabled(false);
                    lastButton.setEnabled(false);
                }
            
                else
                {
                    forwardButton.setEnabled(true);
                    lastButton.setEnabled(true);
                }
            }
            
            //if there is no node in the pathway, disables every button
            else
            {
                firstButton.setEnabled(false);
                lastButton.setEnabled(false);
                forwardButton.setEnabled(false);
                backButton.setEnabled(false);
                nodeLabel.setText(emptyLabel);
            }
        }
        
        //if currently no pathway is selected, disables all buttons and resets the label
        else
        {
            firstButton.setEnabled(false);
            lastButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
            nodeLabel.setText(emptyLabel);
            
            //temporarily here
            removeButton.setEnabled(false);
        }
    }
    
    /**Removes the given pathway from the combo box list and the pathway manager*/
    public void removePathway(LWPathway oldPathway)
    {
        //switches to the no pathway selected
        pathwayList.setSelectedIndex(0);
        
        pathwayList.removeItem(oldPathway);
        pathwayManager.removePathway(oldPathway);
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {       
        //moves to the first node of the pathway
        if (e.getSource() == firstButton){
          this.getCurrentPathway().getFirst();
          
        }
            
        //moves to the last node of the pathway
        else if (e.getSource() == lastButton){
          this.getCurrentPathway().getLast();
          
        }
        //moves to the next node of the pathway
        else if (e.getSource() == forwardButton){
          this.getCurrentPathway().getNext();
          
        }
        //moves to the previous node of the pathway
        else if (e.getSource() == backButton){
          this.getCurrentPathway().getPrevious();
          
        }
        
        //temporarily here
        else if (e.getSource() == removeButton)
          removePathway(this.getCurrentPathway());
          
        //notifies the change to the panel
        updateControlPanel();
        VUE.getActiveViewer().repaint();
    }
    
    /**Reacts to item events dispatched by the combo box*/
    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getStateChange() == ItemEvent.SELECTED) 
        {
            //if the pathway was selected, then sets the current pathway to the selected pathway and updates accordingly
            
            if (pathwayList.getSelectedItem() instanceof LWPathway)
            {
                LWPathway pathway = (LWPathway)pathwayList.getSelectedItem();
                this.setCurrentPathway(pathway);            
            }
            
            //if "no" pathway was selected, then set the current pathway to nothing and updates accordingly
            else if (pathwayList.getSelectedItem().equals(noPathway))
              this.setCurrentPathway(null);
           
            
            //if "add" pathway was selected, then adds a pathway, sets it to the current pathway, and updates accordingly
            else if (pathwayList.getSelectedItem().equals(addPathway))
            {    
                PathwayDialog dialog = new PathwayDialog(this, getLocationOnScreen());
                dialog.show();
            }
        } 
    }
     
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Pathway Control");
        
        return (Action)displayAction;
    }
    
    /**A private class which defines how the combo box should be rendered*/
    private class pathwayRenderer extends BasicComboBoxRenderer 
    {
        public pathwayRenderer ()
        {
            super();
        }
        
        //a method which defines how to render cells
        public Component getListCellRendererComponent(JList list,
               Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            //if the cell contains a pathway, then displays its label
            if (value instanceof LWPathway)
            {    
                LWPathway pathway = (LWPathway)value;
                if(pathway.getLabel().length() > 15)
                    setText(pathway.getLabel().substring(0, 13) + "...");
                else
                    setText(pathway.getLabel());
            }
            
            //if it is a string, then displays the string itself
            else
                setText((String)value);
            
            //setting the color to the default setting
            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            return this;
        }   
    }    
    
    /**A dialog displayed when the user chooses to add a new pathway to the current map */
    private class PathwayDialog extends JDialog 
        implements ActionListener, KeyListener
    {
        JButton okButton, cancelButton;
        JTextField textField;
        
        public PathwayDialog(JDialog dialog, Point location)
        {
            super(dialog, "New Pathway Name", true);
            setSize(250, 100);
            setLocation(location);
            setUpUI();
        }
        
        public void setUpUI()
        {
            okButton = new JButton("Ok");
            cancelButton = new JButton("Cancel");
            
            okButton.addActionListener(this);
            okButton.addKeyListener(this);
            cancelButton.addActionListener(this);
            cancelButton.addKeyListener(this);
            
            textField = new JTextField("default", 18);
            textField.addKeyListener(this);
            textField.setPreferredSize(new Dimension(40, 20));
            
            JPanel buttons = new JPanel();
            buttons.setLayout(new FlowLayout());
            buttons.add(okButton);
            buttons.add(cancelButton);
            
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new FlowLayout());
            textPanel.add(textField);
            
            Container dialogContentPane = getContentPane();
            dialogContentPane.setLayout(new BorderLayout());
            
            dialogContentPane.add(textPanel, BorderLayout.CENTER);
            dialogContentPane.add(buttons, BorderLayout.SOUTH);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent e) 
        {
            if (e.getSource() == okButton)
            {
                //calls addPathway method defined in PathwayControl
                addPathway(new LWPathway(textField.getText()));
                dispose();
            }
            
            else if (e.getSource() == cancelButton)
            {
                //selects empty pathway if the cancel button was pressed
                pathwayList.setSelectedItem(noPathway);
                dispose();
            }
        }
        
        //key events for the dialog box
        public void keyPressed(KeyEvent e) {}
        public void keyReleased(KeyEvent e) {}
        
        public void keyTyped(KeyEvent e) 
        {
            //when enter is pressed
            if(e.getKeyChar()== KeyEvent.VK_ENTER)
            {
                //if the ok button or the text field has the focus, add a designated new pathway
                if (okButton.isFocusOwner() || textField.isFocusOwner())
                {    
                    addPathway(new LWPathway(textField.getText()));
                    dispose();                  
                }
                
                //else if the cancel button has the focus, just aborts it
                else if (cancelButton.isFocusOwner())
                {
                    //selects empty pathway if the cancel button was pressed
                    pathwayList.setSelectedItem(noPathway);
                    dispose(); //does catch event?
                }
            }
        }
        
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
            System.out.println("setting pathway manager in actionPerformed-DisplayAction for Map: "+VUE.getActiveViewer().getMap().getLabel());
            setPathwayManager(VUE.getActiveViewer().getMap().getPathwayManager());
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
}