/*
 * PathwayTab.java
 *
 * Created on June 19, 2003, 12:50 PM
 */

package tufts.vue;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.*;
import javax.swing.ListSelectionModel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.util.Vector;
import java.awt.Color;
import java.awt.Dimension;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author  Daisuke Fujiwara
 */
/**A class which displays nodes in a pathway */
public class PathwayTab extends JPanel implements ActionListener, ListSelectionListener
{    
    //necessary widgets
    private JTable pathwayTable;
    private JButton moveUp, moveDown, remove;
    private JTextArea text;
    
    /** Creates a new instance of PathwayTab */
    public PathwayTab() 
    {   
        pathwayTable = new JTable(new PathwayTableModel());
        pathwayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = pathwayTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        setLayout(new BorderLayout());
        setBorder(new LineBorder(Color.black));
       
        JScrollPane scrollPane = new JScrollPane(pathwayTable);
        //pathwayTable.setPreferredScrollableViewportSize(getPreferredSize());
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        buttons.setPreferredSize(new Dimension (80, 60));
        
        moveUp = new JButton("Up");
        moveDown = new JButton("Down");
        remove = new JButton("Remove");
        
        text = new JTextArea("empty comment");
        text.setEditable(true);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setPreferredSize(new Dimension(100, 50));
        
        moveUp.addActionListener(this);
        moveDown.addActionListener(this);
        remove.addActionListener(this);
        
        moveUp.setEnabled(false);
        moveDown.setEnabled(false);
        remove.setEnabled(false);
        
        buttons.add(moveUp);
        buttons.add(moveDown);
        buttons.add(remove);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(text);
        
        add(scrollPane, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);
        add(textPanel, BorderLayout.SOUTH);
    }
    
    /**Another constructor which takes in a pathway as an argument*/
    public PathwayTab(LWPathway pathway)
    {
        this();
        ((PathwayTableModel)pathwayTable.getModel()).setPathway(pathway);
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        //gets the selected row number
        int selected = pathwayTable.getSelectedRow();
        
        //moves up the selected row 
        if (e.getSource() == moveUp)
        {
            selected--;
            pathwayTable.setRowSelectionInterval(selected, selected);
        }
        
        //moves down the selected row
        else if (e.getSource() == moveDown)
        {
            selected++; 
            pathwayTable.setRowSelectionInterval(selected, selected);
        }
        
        //removes the selected row
        else
        {    
             if (selected != -1)
             {
                ((PathwayTableModel)pathwayTable.getModel()).deleteRow(selected);
             }  
        }        
    }
    
    /**Reacts to list selections dispatched by the table*/
    public void valueChanged(ListSelectionEvent le)
    {
        ListSelectionModel lsm = (ListSelectionModel)le.getSource();

        //if there is a row selected
        if (!lsm.isSelectionEmpty())
        {
            int selectedRow = lsm.getMinSelectionIndex();
            remove.setEnabled(true);
            
            //if the selected row is the last row, then disables the move down button
            if(selectedRow == pathwayTable.getRowCount() - 1)
                moveDown.setEnabled(false);
            else
                moveDown.setEnabled(true);
            
            //if the selected row is the first row, then disables the move up button
            if(selectedRow == 0)
                moveUp.setEnabled(false);
            else
                moveUp.setEnabled(true);
        }
        
        //if no row is selected, then disables all buttons
        else
        {
            moveDown.setEnabled(false);
            moveUp.setEnabled(false);
            remove.setEnabled(false);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        //setting up the tool window
        ToolWindow window = new ToolWindow("Pathway Control", null);
        window.setSize(400, 300);
        window.addTool(new PathwayTab(new LWPathway(0)));
        window.setVisible(true);
    }
    
    /**A model used by the table which displays nodes of the pathway*/
    private class PathwayTableModel extends AbstractTableModel
    {
        //pathway whose nodes are displayed in the table
        private LWPathway pathway;
        
        //column names for the table
        private final String[] columnNames = {"Nodes/Links"};
         
        public PathwayTableModel()
        {
            //default pathway
            pathway = new LWPathway();
        }
        
        //sets the pathway to the given pathway
        public synchronized void setPathway(LWPathway pathway)
        {
            this.pathway = pathway;
        }
        
        //returns the number of row (nodes of the pathway)
        public synchronized int getRowCount()
        {
            return pathway.length();
        }
        
        public int getColumnCount()
        {
            return columnNames.length;
        }
        
        public String getColumnName(int column)
        {
            return columnNames[column];
        }
        
        //a method which defines how what each cell should display
        public synchronized Object getValueAt(int row, int column)
        {
            try
            {
                switch(column)
                {                    
                    case 0:
                        Node rowNode = pathway.getNode(row);
                        return rowNode.getLabel();
                    //case 1:
                       // return new String("Comments about the node");
                }
            }
            
            catch (Exception e)
            {
                System.err.println("exception in the table model:" + e);
            }
            
            return "";
        }
        
        //adds a row to the table (insertion)
        public synchronized void addRow(Node node)
        {
            pathway.addNode(node);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
        }
        
        //deletes the given row from the table
        public synchronized void deleteRow(int row)
        {
            pathway.removeNode(pathway.getNode(row));
            fireTableRowsDeleted(row, row);  
        }
    }
}
