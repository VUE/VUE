/*
 * PathwayTableModel.java
 *
 * Created on December 3, 2003, 1:15 PM
 */

package tufts.vue;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * @author  Scott Fraize
 * @author  Jay Briedis
 * @version February 2004
 */
public class PathwayTableModel extends DefaultTableModel {
    
    public PathwayTableModel() { }

    private LWPathwayList getPathways() {
        return VUE.getActiveMap() == null ? null : VUE.getActiveMap().getPathways();
    }
    Iterator getPathwayIterator() {
        return VUE.getActiveMap() == null ? VueUtil.EmptyIterator : VUE.getActiveMap().getPathways().iterator();
    }
    
    // get rid of this
    void setCurrentPathway(LWPathway path){
        if (getPathways() != null){           
            getPathways().setActivePathway(path);
            fireTableDataChanged();
            //SMF tab.updateControlPanel();
        }
    }
    
    /** for PathwayTable */
    LWPathway getCurrentPathway(){
        if (getPathways() != null)
            return getPathways().getActivePathway();
        else
            return null;
    }

    /** for PathwayPanel */
    int getCurrentPathwayIndex(){
        return getList().indexOf(VUE.getActivePathway());
    }

    /** for PathwayTable */
    LWPathway getPathwayForElementAt(int pRow)
    {
        Iterator i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (row == pRow)
                return p;
            row++;
            if (p.isOpen()) {
                Iterator ci = p.getElementIterator();
                while (ci.hasNext()) {
                    LWComponent c = (LWComponent) ci.next();
                    if (row == pRow)
                        return p;
                    row++;
                }
            }
        }
        return null;
    }

    // get the model list
    private java.util.List getList() {
        java.util.List list = new ArrayList();
        Iterator i = getPathwayIterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            list.add(p);
            if (p.isOpen())
                list.addAll(p.getElementList());
        }
        return list;
    }

    /** for PathwayTable */
    LWComponent getElement(int row){
        if (getPathways() == null)
            return null;
        return (LWComponent) getList().get(row);
        /*
        Iterator i = pathways.iterator();
        int idx = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (cnt == row)
                return p;
            idx++;
        }
        */
    }

    public synchronized int getRowCount(){
        return getList().size();
    }

    public int getColumnCount() {
        return 6;
    }
    
    public String getColumnName(int col){
        switch(col){
        case 0: return "A";
        case 1: return "B";
        case 2: return "C";
        case 3: return "D";
        case 4: return "E";
        case 5: return "F";
        }
        return "";
    }

    public Class getColumnClass(int col){
        if(col == 1)
            return Color.class;
        else if(col == 0 || col == 2 || col == 4 || col == 5)
            return ImageIcon.class;
        else if(col == 3)
            return Object.class;
        else
            return null;
    }
    
    public boolean isCellEditable(int row, int col){
        if (getPathways() != null){
            LWPathway p = getPathwayForElementAt(row);
            if (p.isLocked())
                return false;
            else
                return col == 1 || col == 3;
        }
        return false;
    }
    
    public boolean containsPathwayNamed(String label) {
        Iterator i = getPathwayIterator();
        while (i.hasNext()){
            LWPathway p = (LWPathway) i.next();
            if (p.getLabel().equals(label))
                return true;
        }
        return false;
    }
    
    public synchronized Object getValueAt(int row, int col)
    {
        LWComponent c = getElement(row);
        if (c instanceof LWPathway) {
            LWPathway p = (LWPathway) c;
            try{
                switch (col){
                case 0: return new Boolean(p.isVisible());
                case 1: return p.getStrokeColor();
                case 2: return new Boolean(p.isOpen());
                case 3: return p.getLabel();
                case 4: return new Boolean(p.hasNotes());
                case 5: return new Boolean(p.isLocked());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway cell:" + e);
            } 
        } else {
            try {
                if (col == 3) return c.getLabel();
                //if (col == 4) return Boolean.FALSE;
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway element cell:" + e);
            }  
        }
        return null;
    }

    private void setActivePathway(LWPathway p)
    {
        getPathways().setActivePathway(p);
    }

    
    public void setValueAt(Object aValue, int row, int col){
        LWComponent c = getElement(row);
        if (c instanceof LWPathway){
            LWPathway p = (LWPathway) c;

            if (col == 0) {
                p.setVisible(!p.isVisible());
            } else if (col == 1){
                p.setStrokeColor((Color)aValue);
            } else if (col == 2){
                p.setOpen(!p.isOpen());
                setActivePathway(p);
            } else if (col == 3){
                p.setLabel((String)aValue);
                setActivePathway(p);
            } else if (col == 5){
                p.setLocked(!p.isLocked());
            }
            fireTableDataChanged();
        } else if (c != null) {
            if (col == 3)
                c.setLabel((String)aValue);
        }
    }   

}
