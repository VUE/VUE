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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class which displays nodes in a pathway */
public class PathwayTab extends JPanel implements ActionListener, ListSelectionListener, DocumentListener
{    
    //necessary widgets
    private JTable pathwayTable;
    private JButton moveUp, moveDown, remove, submit, add;
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
        
        moveUp = new JButton("^");
        moveDown = new JButton("v");
        remove = new JButton("Delete");
        submit = new JButton("Submit");
        add = new JButton("Add");
        
        text = new JTextArea();
        text.setEditable(true);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        
        JScrollPane textScrollPane = new JScrollPane(text);
        textScrollPane.setPreferredSize(new Dimension(200, 70));
        textScrollPane.setBorder(new TitledBorder("Node Comment"));
        
        Document document = text.getDocument();
        document.addDocumentListener(this);
        
        moveUp.addActionListener(this);
        moveDown.addActionListener(this);
        remove.addActionListener(this);
        submit.addActionListener(this);
        add.addActionListener(this);
        
        moveUp.setEnabled(false);
        moveDown.setEnabled(false);
        remove.setEnabled(false);
        submit.setEnabled(false);
        add.setEnabled(false);
        
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener(
            new LWSelection.Listener()
            {
                public void selectionChanged(LWSelection selection)
                {
                    if (!selection.isEmpty() && getPathway() != null)
                      add.setEnabled(true);
                    
                    else
                      add.setEnabled(false);
                }
            }     
        );
        
        buttons.add(moveUp);
        buttons.add(moveDown);
        buttons.add(add);
        buttons.add(remove);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(textScrollPane);
        textPanel.add(submit);
        
        add(scrollPane, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);
        add(textPanel, BorderLayout.SOUTH);
    }
    
    //sets the table's pathway to the given pathway
    public void setPathway(LWPathway pathway)
    {
        ((PathwayTableModel)pathwayTable.getModel()).setPathway(pathway);
        
        if (pathway == null)
          add.setEnabled(false);
        
        else if (!VUE.ModelSelection.isEmpty())
          add.setEnabled(true);
    }
    
    /**Gets the current pathway associated with the pathway table*/
    public LWPathway getPathway()
    {
        return ((PathwayTableModel)pathwayTable.getModel()).getPathway();
    }
    
    /**Notifies the table of data change*/
    public void updateTable()
    {
        ((PathwayTableModel)pathwayTable.getModel()).fireTableDataChanged();
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        //gets the selected row number
        int selected = pathwayTable.getSelectedRow();
        
        //moves up the selected row 
        if (e.getSource() == moveUp)
        {
            //selected--;
            //pathwayTable.setRowSelectionInterval(selected, selected);
            
            ((PathwayTableModel)pathwayTable.getModel()).switchRow(selected, --selected);
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
            
            //update the pathway control panel
            VUE.getPathwayControl().updateControlPanel();
        }
        
        //moves down the selected row
        else if (e.getSource() == moveDown)
        {
            //selected++; 
            //pathwayTable.setRowSelectionInterval(selected, selected);
            
            ((PathwayTableModel)pathwayTable.getModel()).switchRow(selected, ++selected);
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
            
            //update the pathway control panel
            VUE.getPathwayControl().updateControlPanel();
        }
        
        //removes the selected row
        else if (e.getSource() == remove)
        {    
             if (selected != -1)
             {
                ((PathwayTableModel)pathwayTable.getModel()).deleteRow(selected);
                VUE.getPathwayControl().updateControlPanel();
                
                submit.setEnabled(false);
             }  
        }        
        
        //add selected elements to the pathway where the table is selected
        else if (e.getSource() == add)
        {
            LWComponent array[] = VUE.ModelSelection.getArray();
              
            //adds everything in the current selection 
            for (int i = 0; i < array.length; i++)
            {
                ((PathwayTableModel)pathwayTable.getModel()).addRow(array[i], ++selected);
            }
            
            pathwayTable.setRowSelectionInterval(selected, selected);
            
            //update the pathway control panel
            VUE.getPathwayControl().updateControlPanel();
        }

        //submit
        else
        {            
            LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selected);
            element.setNotes(text.getText());
            submit.setEnabled(false);
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
            
            LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selectedRow);
            text.setText(element.getNotes());
      
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
            submit.setEnabled(false);
            text.setText("comment");
        }
    }
   
    /**document listener's methods*/
    public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        if(pathwayTable.getSelectedRow() != -1)
          submit.setEnabled(true);
    }
    
    public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        if(pathwayTable.getSelectedRow() != -1)
          submit.setEnabled(true);
    }
    
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
       if(pathwayTable.getSelectedRow() != -1)
         submit.setEnabled(true);
    }
    
    /**end of the document listener methods*/
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        /**
        //setting up the tool window
        ToolWindow window = new ToolWindow("Pathway Control", null);
        window.setSize(400, 300);
        window.addTool(new PathwayTab(new LWPathway(0)));
        window.setVisible(true);
         **/
        /*
        InspectorWindow window = new InspectorWindow(null, "test");
        window.getContentPane().add(new PathwayTab(new LWPathway(0)));
        window.show();*/
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
           pathway = new LWPathway("default");
        }
        
        //sets the pathway to the given pathway
        public synchronized void setPathway(LWPathway pathway)
        {
            this.pathway = pathway;
            fireTableDataChanged();
        }
        
        //returns the current pathway
        public synchronized LWPathway getPathway()
        {
            return pathway;
        }
        
        //returns the number of row (nodes of the pathway)
        public synchronized int getRowCount()
        {
            if(pathway != null) 
                return pathway.length();
            
            else
                return 0;
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
                        LWComponent rowElement = pathway.getElement(row);
                        return rowElement.getLabel();
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
        
        //adds a row at the designated location
        public synchronized void addRow(LWComponent element, int row)
        {
            pathway.addElement(element, row);
            fireTableRowsInserted(row, row);
        }
        
        //deletes the given row from the table
        public synchronized void deleteRow(int row)
        {
            pathway.removeElement(row);
            fireTableRowsDeleted(row, row);  
        }
        
        //switches a row to a new location
        public void switchRow(int oldRow, int newRow)
        {
            pathway.moveElement(oldRow, newRow);
            fireTableDataChanged();
        }
    }
}
