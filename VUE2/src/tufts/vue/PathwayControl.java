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
public class PathwayControl extends JPanel implements ActionListener, ItemListener
{
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JLabel nodeLabel;
    private JComboBox pathwayList;
    
    private LWPathway currentPathway;
    
    /** Creates a new instance of PathwayControl */
    public PathwayControl() 
    {   
        currentPathway = null;
        pathwayList = new JComboBox();
        
        firstButton = new JButton("<<");
        lastButton = new JButton(">>");
        forwardButton = new JButton(">");
        backButton = new JButton("<");
        nodeLabel = new JLabel("empty");
        
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
        pathwayList.setEditable(false);
        pathwayList.addActionListener(this);
        pathwayList.addItemListener(this);
        pathwayList.addItem("");
        pathwayList.addItem("Add new Pathway");
        
        setLayout(new BorderLayout());
        setBackground(Color.white);
        setBorder(new LineBorder(Color.black));
             
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
    
    public PathwayControl(LWPathway pathway)
    {
        this();
        pathwayList.addItem(pathway);
        setCurrentPathway(pathway);
    }
    
    public void setCurrentPathway(LWPathway pathway)
    {
        currentPathway = pathway;
        currentPathway.setCurrent(currentPathway.getFirst());
        updateControlPanel();
    }
    
    public LWPathway getCurrentPathway()
    {
        return currentPathway;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PathwayControl control = new PathwayControl();
        
        ToolWindow window = new ToolWindow("Pathway Control", null);
        window.addTool(control);
        window.setSize(400, 300);
        window.setVisible(true);
    }
    
    public void updateControlPanel()
    {
        if (currentPathway != null)
        {
            Node currentNode = currentPathway.getCurrent();
        
            if(currentNode != null)
            {
                nodeLabel.setText(currentNode.getLabel());
          
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
            
            else
                nodeLabel.setText("empty");
        }
        
        else
        {
            nodeLabel.setText("empty");
            
            firstButton.setEnabled(false);
            lastButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == pathwayList)
        {   
            if (pathwayList.getSelectedItem().equals("Add new Pathway"))
            {
                System.out.println("adding a new pathway");
                pathwayList.addItem(new LWPathway(0));
                pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);
            }
            
            else 
                System.out.println("causing a problem in action listener first part");
        }
        
        else
        {   
            if (e.getSource() == firstButton)
            {
                currentPathway.setCurrent(currentPathway.getFirst());
                updateControlPanel();
            }
            
            else if (e.getSource() == lastButton)
            {
                currentPathway.setCurrent(currentPathway.getLast());
                updateControlPanel();
            }
            
            else if (e.getSource() == forwardButton)
            {
                currentPathway.setCurrent(currentPathway.getNext(currentPathway.getCurrent()));
                updateControlPanel();
            }
            
            else if (e.getSource() == backButton)
            {
                currentPathway.setCurrent(currentPathway.getPrevious(currentPathway.getCurrent()));
                updateControlPanel();
            }
            
            else
                System.out.println("causing problem in action listener second part");
        }
    }
    
    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getStateChange() == ItemEvent.SELECTED) 
        {
            if (pathwayList.getSelectedItem() instanceof LWPathway)
            {
              currentPathway = (LWPathway)pathwayList.getSelectedItem();
              updateControlPanel();
            }
            
            else if (pathwayList.getSelectedItem().equals(""))
            {
              System.out.println("selecting empty string");
              currentPathway = null;
              updateControlPanel();
            }
            
            else
                System.out.println("causing problem");
        } 
    }
    
    private class pathwayRenderer extends BasicComboBoxRenderer 
    {
        public pathwayRenderer ()
        {
            super();
        }
        
        public Component getListCellRendererComponent(JList list,
               Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            if (value instanceof LWPathway)
            {    
                LWPathway pathway = (LWPathway)value;
                setText(pathway.getLabel());
            }
            
            else
            {
                setText((String)value);
            }
            
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