/*
 * PathwayControl.java
 *
 * Created on June 19, 2003, 11:33 AM
 */

package tufts.vue;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.border.LineBorder;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Component;

/**
 *
 * @author  Daisuke Fujiwara
 */
/**A class that displays the control of pathways*/
public class PathwayControl extends JPanel implements ActionListener, ItemListener
{
    /**Necessary widgets to control pathways*/
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JLabel nodeLabel;
    private JComboBox pathwayList;
    
    //pathway currently being kept track of
    private LWPathway currentPathway;
    
    private final String noPathway = "";
    private final String addPathway = "add a new pathway";
    private final String emptyLabel = "empty";
    
    /** Creates a new instance of PathwayControl */
    public PathwayControl() 
    {   
        currentPathway = null;
        pathwayList = new JComboBox();
        
        setLayout(new BorderLayout());
        setBackground(Color.white);
        setBorder(new LineBorder(Color.black));
        
        firstButton = new JButton("<<");
        lastButton = new JButton(">>");
        forwardButton = new JButton(">");
        backButton = new JButton("<");
        nodeLabel = new JLabel(emptyLabel);
        
        /*
        firstButton.setEnabled(false);
        lastButton.setEnabled(false);
        forwardButton.setEnabled(false);
        backButton.setEnabled(false);
        */
        
        firstButton.addActionListener(this);
        lastButton.addActionListener(this);
        forwardButton.addActionListener(this);
        backButton.addActionListener(this);
        
        pathwayList.setRenderer(new pathwayRenderer());
        //pathwayList.setMaximumRowCount();
        //pathwayList.setEditable(true);
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
        
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout());
        descriptionPanel.setBackground(Color.white);
        descriptionPanel.add(new JLabel("Label:"));
        descriptionPanel.add(nodeLabel);
        
        add(pathwayList, BorderLayout.SOUTH);
        add(buttonPanel, BorderLayout.NORTH);
        add(descriptionPanel, BorderLayout.CENTER);
    }
    
    /**Another constructor which takes a given pathway as an argument*/
    public PathwayControl(LWPathway pathway)
    {
        this();
        pathwayList.addItem(pathway);
        setCurrentPathway(pathway);
    }
    
    /**Sstes the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        currentPathway = pathway;
        
        //setting the current node of the pathway to the first node
        currentPathway.setCurrent(currentPathway.getFirst());
        
        updateControlPanel();
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
        PathwayControl control = new PathwayControl();
        
        //setting up pathway control in a tool window
        ToolWindow window = new ToolWindow("Pathway Control", null);
        window.addTool(control);
        window.setSize(400, 300);
        window.setVisible(true);
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        //if there is a pathway currently selected
        if (currentPathway != null)
        {
            Node currentNode = currentPathway.getCurrent();
        
            //if there is a node in the pathway
            if(currentNode != null)
            {
                //sets the label to the current node's label
                nodeLabel.setText(currentNode.getLabel());
          
                //if it is the first node in the pathway, then disables first and back buttons
                if (currentPathway.isFirst(currentNode))
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
                if (currentPathway.isLast(currentNode))
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
            
            //needs to be tested
            else
            {
                firstButton.setEnabled(false);
                lastButton.setEnabled(false);
                forwardButton.setEnabled(false);
                backButton.setEnabled(false);
                nodeLabel.setText(emptyLabel);
            }
        }
        
        //if no pathway is selected currently, disables all buttons and resets the label
        else
        {
            firstButton.setEnabled(false);
            lastButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
            nodeLabel.setText(emptyLabel);
        }
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {       
        //moves to the first node of the pathway
        if (e.getSource() == firstButton)
          {
            currentPathway.setCurrent(currentPathway.getFirst());
            updateControlPanel();
          }
            
        //moves to the last node of the pathway
        else if (e.getSource() == lastButton)
          {
            currentPathway.setCurrent(currentPathway.getLast());
            updateControlPanel();
          }
            
        //moves to the next node of the pathway
        else if (e.getSource() == forwardButton)
          {
            currentPathway.setCurrent(currentPathway.getNext(currentPathway.getCurrent()));
            updateControlPanel();
          }
            
        //moves to the previous node of the pathway
        else if (e.getSource() == backButton)
          {
            currentPathway.setCurrent(currentPathway.getPrevious(currentPathway.getCurrent()));
            updateControlPanel();
          }
            
        //default case
        else
          System.out.println("an action from no buttons");
    }
    
    /**Reacts to item events dispatched by the combo box*/
    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getStateChange() == ItemEvent.SELECTED) 
        {
            //if the pathway was selected, then sets the current pathway to the selected pathway and updates accordingly
            if (pathwayList.getSelectedItem() instanceof LWPathway)
            {
              currentPathway = (LWPathway)pathwayList.getSelectedItem();
              updateControlPanel();
            }
            
            //if "no" pathway was selected, then set the current pathway to nothing and updates accordingly
            else if (pathwayList.getSelectedItem().equals(noPathway))
            {
              System.out.println("selecting empty string");
              currentPathway = null;
              updateControlPanel();
            }
            
            //if "add" pathway was selected, then adds a pathway, sets it to the current pathway, and updates accordingly
            else if (pathwayList.getSelectedItem().equals(addPathway))
            {
                System.out.println("adding a new pathway");
                pathwayList.addItem(new LWPathway(0));
                pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);
            }
            
            else
                System.out.println("item event from no where");
        } 
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
            {
                setText((String)value);
            }
            
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
}