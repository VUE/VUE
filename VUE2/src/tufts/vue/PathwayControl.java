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
    private LWPathway currentPathway;
    private LWPathwayManager pathwayManager;
    
    private final String noPathway = "";
    private final String addPathway = "add a new pathway";
    private final String emptyLabel = "empty";
    
    private DisplayAction displayAction = null;
    
    /** Creates a new instance of PathwayControl */
    public PathwayControl(JFrame parent) 
    {   
        super(parent, "Pathway Control");
        setSize(450, 120);
        
        currentPathway = null;
        pathwayManager = null;
        pathwayList = new JComboBox();
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.white);
        //getContentPane().setBorder(new LineBorder(Color.black));
        
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
        removeButton = new JButton("remove");
        removeButton.addActionListener(this);
        
        pathwayList.setRenderer(new pathwayRenderer());
        //pathwayList.setMaximumRowCount();
        pathwayList.addItemListener(this);
        pathwayList.addItem(noPathway);
        pathwayList.addItem(addPathway);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.white);
        buttonPanel.add(firstButton);
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(lastButton);
        buttonPanel.add(removeButton);
         
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout());
        descriptionPanel.setBackground(Color.white);
        descriptionPanel.add(new JLabel("Label:"));
        descriptionPanel.add(nodeLabel);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(pathwayList);
        bottomPanel.add(buttonPanel);
        
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(descriptionPanel, BorderLayout.CENTER);
        
        super.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e) 
                {
                    displayAction.setButton(false);
                }
            }
        );
    }
    
    /* not used at this point
    public PathwayControl(LWPathway pathway)
    {
        this();
        pathwayList.addItem(pathway);
        setCurrentPathway(pathway);
    }
    */
    
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
        {
            pathwayList.addItem((LWPathway)i.next());           
        }
        
        //sets the current pathway to the current pathway 
        LWPathway pathway;
        if ((pathway = this.pathwayManager.getCurrentPathway()) != null)
          setCurrentPathway(pathway);
        
    }
    
    /**Returns the currently associated pathway manager*/
    public LWPathwayManager getPathwayManger()
    {
        return pathwayManager;
    }
    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        currentPathway = pathway;
        
        if ((currentPathway.getCurrent() == null) && (currentPathway.getFirst() != null) )
            currentPathway.setCurrent(currentPathway.getFirst());
        
        System.out.println("selecting to the pathway: " + pathway.getLabel());
        pathwayList.setSelectedItem(pathway);
        
        updateControlPanel();
        
        //update the inspector here
        VUE.getPathwayInspector().setPathway(currentPathway);
    }
    
    /**Returns the currently selected pathway*/
    public LWPathway getCurrentPathway()
    {
        return currentPathway;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //PathwayControl control = new PathwayControl(LWPathwayManager.getInstance());
        PathwayControl control = new PathwayControl(null);
        control.show();
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        //if there is a pathway currently selected
        if (currentPathway != null)
        {
            LWComponent currentElement = currentPathway.getCurrent();
            
            //temporarily here 
            removeButton.setEnabled(true);
            
            //if there is a node in the pathway
            if(currentElement != null)
            {
                //sets the label to the current node's label
                nodeLabel.setText(currentElement.getLabel());
          
                //if it is the first node in the pathway, then disables first and back buttons
                if (currentPathway.isFirst(currentElement))
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
                if (currentPathway.isLast(currentElement))
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
    
    /**Adds the given pathway to the combo box list and the pathway manager*/
    public void addPathway(LWPathway newPathway)
    {
        pathwayList.addItem(newPathway);
        pathwayManager.addPathway(newPathway);
    }
    
    /**Removes the given pathway from the combo box list and the pathway manager*/
    public void removePathway(LWPathway oldPathway)
    {
        //sets to the no pathway selected
        pathwayList.setSelectedIndex(0);
        
        pathwayList.removeItem(oldPathway);
        pathwayManager.removePathway(oldPathway);
        
        //updates the inspector
        VUE.getPathwayInspector().setPathway(null);
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {       
        //moves to the first node of the pathway
        if (e.getSource() == firstButton)
          currentPathway.setCurrent(currentPathway.getFirst());
            
        //moves to the last node of the pathway
        else if (e.getSource() == lastButton)
          currentPathway.setCurrent(currentPathway.getLast());
            
        //moves to the next node of the pathway
        else if (e.getSource() == forwardButton)
          currentPathway.setCurrent(currentPathway.getNext(currentPathway.getCurrent()));
            
        //moves to the previous node of the pathway
        else if (e.getSource() == backButton)
          currentPathway.setCurrent(currentPathway.getPrevious(currentPathway.getCurrent()));
            
        //temporarily here
        else if (e.getSource() == removeButton)
          removePathway(currentPathway);
          
        //notifies the change to the panel
        updateControlPanel();
    }
    
    /**Reacts to item events dispatched by the combo box*/
    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getStateChange() == ItemEvent.SELECTED) 
        {
            //if the pathway was selected, then sets the current pathway to the selected pathway and updates accordingly
            if (pathwayList.getSelectedItem() instanceof LWPathway)
            {
                System.out.println("pathway selected");
                currentPathway = (LWPathway)pathwayList.getSelectedItem();
                
                if (pathwayManager != null)
                {
                    System.out.println("setting the current pathway in the manager to : " + currentPathway.getLabel());
                    pathwayManager.setCurrentPathway(currentPathway);
                }
                
                //update the inspector here
                VUE.getPathwayInspector().setPathway(currentPathway);
                
                updateControlPanel();
            }
            
            //if "no" pathway was selected, then set the current pathway to nothing and updates accordingly
            else if (pathwayList.getSelectedItem().equals(noPathway))
            {
                System.out.println("selecting empty string");
                currentPathway = null;
                
                //if there is a pathwayManager currently set 
                if (pathwayManager != null)
                    pathwayManager.setCurrentPathway(currentPathway);
                
                //update the inspector here
                VUE.getPathwayInspector().setPathway(currentPathway);
                
                updateControlPanel();
            }
            
            //if "add" pathway was selected, then adds a pathway, sets it to the current pathway, and updates accordingly
            else if (pathwayList.getSelectedItem().equals(addPathway))
            {
                System.out.println("adding a new pathway");
                
                PathwayDialog dialog = new PathwayDialog(this, getLocationOnScreen());
                dialog.show();
                
                pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);
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
    
    private class PathwayDialog extends JDialog implements ActionListener
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
            cancelButton.addActionListener(this);
            
            textField = new JTextField("default", 18);
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
                System.out.println("ok button");
                addPathway(new LWPathway(textField.getText()));
                dispose();
            }
            
            else if (e.getSource() == cancelButton)
            {
                System.out.println("cancel button");
                dispose();
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
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
}