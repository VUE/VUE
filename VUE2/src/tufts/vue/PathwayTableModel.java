/*
 * PathwayTableModel.java
 *
 * Created on December 3, 2003, 1:15 PM
 */

package tufts.vue;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Jay Briedis
 */
public class PathwayTableModel extends AbstractTableModel{
    //pathway whose nodes are displayed in the table
    //private LWPathway pathway;
    private PathwayTab tab = null;
    private LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
    //column names for the table
    //private final String[] columnNames = {"Nodes/Links"};

    public PathwayTableModel(PathwayTab tab)
    {
       this.tab = tab;
     
    }

    public void addPathway(LWPathway pathway){
        manager.addPathway(pathway);
        this.fireTableDataChanged();
        //tab.updateControlPanel();
    }
    
    public void removePathway(LWPathway pathway){
        manager.removePathway(pathway);
        this.fireTableDataChanged();
        //tab.updateControlPanel();
    }
    
    //sets the pathway to the given pathway
    /*public synchronized void setPathway(LWPathway pathway)
    {
        //this.pathway = pathway;
        fireTableDataChanged();
    }
*/
    //returns the current pathway
    //public synchronized LWPathway getPathway()
    //{
    //    return pathway;
    //}

    //returns the number of row (nodes of the pathway)
    public synchronized int getRowCount()
    {
        return manager.length();
    }

    public int getColumnCount()
    {
        return 5;
    }

    public synchronized Object getValueAt(int row, int column)
    {
        LWPathway rowPath = manager.getPathway(row);
        int hasNotes = 1;
        if(rowPath.getNotes().equals(null) || rowPath.getNotes().equals(""))
            hasNotes = 0;
        try
        {
            switch(column)
            {                    
                case 1:
                    return rowPath.getBorderColor();
                case 3:
                    return rowPath.getLabel();
                case 5:
                    return new Integer(hasNotes);
            }
        }

        catch (Exception e)
        {
            System.err.println("exception in the table model:" + e);
        }

        return "";
    }
    /*
    //adds a row at the designated location
    public synchronized void addRow(LWComponent element, int row)
    {
        //pathway.addElement(element, row);
        fireTableRowsInserted(row, row);

        //update the pathway control panel
        tab.updateControlPanel();
    }

    //deletes the given row from the table
    public synchronized void deleteRow(int row)
    {
        //pathway.removeElement(row);
        fireTableRowsDeleted(row, row); 

        //update the pathway control panel
        tab.updateControlPanel();
        this.removePathway(pathway);
    }
*/
    //switches a row to a new location
    public void switchRow(int oldRow, int newRow)
    {
        //pathway.moveElement(oldRow, newRow);
        fireTableDataChanged();

        //update the pathway control panel
        tab.updateControlPanel();
    }
}
