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
 * @author  ptadministrator
 */
public class PathwayTab extends JPanel implements ActionListener, ListSelectionListener
{    
    private JTable pathwayTable;
    private PathwayTableModel model;
    private JButton moveUp, moveDown, remove;
    private JTextArea text;
    
    /** Creates a new instance of PathwayTab */
    public PathwayTab() 
    {   
        model = new PathwayTableModel();
        
        pathwayTable = new JTable(model);
        pathwayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = pathwayTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        setLayout(new BorderLayout());
        setBorder(new LineBorder(Color.black));
       
        JScrollPane scrollPane = new JScrollPane(pathwayTable);
        pathwayTable.setPreferredScrollableViewportSize(getPreferredSize());
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        
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
    
    public PathwayTab(LWPathway pathway)
    {
        this();
        model.setPathway(pathway);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        int selected = pathwayTable.getSelectedRow();
        
        if (e.getSource() == moveUp)
        {
            selected--;
            pathwayTable.setRowSelectionInterval(selected, selected);
            //remove.setEnabled(true);
        }
        
        else if (e.getSource() == moveDown)
        {
            selected++; 
            pathwayTable.setRowSelectionInterval(selected, selected);
            //remove.setEnabled(true);
        }
        
        //remove
        else
        {    
             if (selected != -1)
             {
                model.deleteRow(selected);
                //remove.setEnabled(false);                
             }  
        }        
    }
    
    public void valueChanged(ListSelectionEvent le)
    {
        ListSelectionModel lsm = (ListSelectionModel)le.getSource();

        if (!lsm.isSelectionEmpty())
        {
            int selectedRow = lsm.getMinSelectionIndex();
            remove.setEnabled(true);
            
            if(selectedRow == pathwayTable.getRowCount() - 1)
                moveDown.setEnabled(false);
            else
                moveDown.setEnabled(true);
            
            if(selectedRow == 0)
                moveUp.setEnabled(false);
            else
                moveUp.setEnabled(true);
        }
        
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
        ToolWindow window = new ToolWindow("Pathway Control", null);
        window.setSize(400, 300);
        window.addTool(new PathwayTab(new LWPathway(0)));
        window.setVisible(true);
    }
    
    private class PathwayTableModel extends AbstractTableModel
    {
        private LWPathway pathway;
        private final String[] columnNames = {"Nodes/Links"};
        
        public PathwayTableModel()
        {
            pathway = new LWPathway();
        }
        
        public synchronized void setPathway(LWPathway pathway)
        {
            this.pathway = pathway;
        }
        
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
        
        public synchronized void addRow(Node node)
        {
            pathway.addNode(node);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
        }
        
        public synchronized void deleteRow(int row)
        {
            pathway.removeNode(pathway.getNode(row));
            fireTableRowsDeleted(row, row);  
        }
    }
}
