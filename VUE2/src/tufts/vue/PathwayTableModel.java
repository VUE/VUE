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
public class PathwayTableModel extends DefaultTableModel
    implements VUE.ActiveMapListener, LWComponent.Listener
{
    private LWMap mMap;
    
    public PathwayTableModel()
    {
        VUE.addActiveMapListener(this);
        setMap(VUE.getActiveMap());
    }

    private void setMap(LWMap map)
    {
        if (mMap == map)
            return;
        if (mMap != null)
            mMap.getPathwayList().removeListener(this);
        mMap = map;
        if (mMap != null)
            mMap.getPathwayList().addListener(this);
        fireTableDataChanged();
    }

    /** LWComponent.Listener */
    public void LWCChanged(LWCEvent e)
    {
        if (e.getSource() instanceof LWPathway) {
            // The events mainly of interest to us are either a structural event, or a LWPathway label/note event,
            // although if anything in the pathway changes, fire a change event just in case.
            if (DEBUG.PATHWAY) System.out.println(this + " pathway event " + e);
            fireTableDataChanged();
        } else if (e.getWhat() == LWCEvent.Label || e.getWhat().startsWith("pathway.")) {
            if (DEBUG.PATHWAY) System.out.println(this + " pathway child event " + e);
            // This means one of the LWComponents in the pathway has changed.
            // We only care about label changes as that's all that's displayed
            // in the PathwayTable, or if pathway.notes has changed so we can update
            // the note icon.
            // We only really need the PathwayTable to repaint if a label
            // has changed, but this will do it.
            fireTableDataChanged();
        }
    }

    /** VUE.ActiveMapListener */
    public void activeMapChanged(LWMap map)
    {
        if (DEBUG.PATHWAY) System.out.println(this + " activeMapChanged to " + map);
        setMap(map);
    }

    private LWPathwayList getPathwayList() {
        return mMap == null ? null : mMap.getPathwayList();
        //return VUE.getActiveMap() == null ? null : VUE.getActiveMap().getPathwayList();
    }
    Iterator getPathwayIterator() {
        //return VUE.getActiveMap() == null ? VueUtil.EmptyIterator : VUE.getActiveMap().getPathwayList().iterator();
        return mMap == null ? VueUtil.EmptyIterator : mMap.getPathwayList().iterator();
    }
    
    void fireChanged(Object invoker) {
        fireTableChanged(new DataEvent(invoker));
    }

    // get rid of this
    void setCurrentPathway(LWPathway path){
        if (getPathwayList() != null){           
            getPathwayList().setActivePathway(path);
            fireTableDataChanged();
            //SMF tab.updateControlPanel();
        }
    }

    /** for PathwayPanel */
    int getCurrentPathwayIndex(){
        return getList().indexOf(VUE.getActivePathway());
    }

    /**
     * for PathwayTable
     * Given @param pRow in the displayed table model,
     * return the pathway that contains it.  If element
     * at that row is a pathway, return that pathway.
     */
    LWPathway getPathwayForElementAt(int pRow)
    {
        Iterator i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (row++ == pRow)
                return p;
            if (p.isOpen()) {
                for (int index = 0; index < p.length(); index++) {
                    if (row++ == pRow)
                        return p;
                }
            }
        }
        throw new IllegalArgumentException("Couldn't find any element at row " + pRow);
    }
    /**
     * for PathwayTable
     * Returns index of element within given pathway.  We need
     * this because an element can appear in the pathway more
     * than once, and this is how we differentiate them (by index).
     * If the element at @param pRow is a pathway, return -1.
     */
    int getPathwayIndexForElementAt(int pRow)
    {
        Iterator i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (row++ == pRow)
                return -1;
            if (p.isOpen()) {
                for (int index = 0; index < p.length(); index++)
                    if (row++ == pRow)
                        return index;
            }
        }
        throw new IllegalArgumentException("Couldn't find any element at row " + pRow);
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

    /** for PathwayTable
     * Returns the element at @param pRow, which will
     * be an LWComponent -- either a LWPathway or an LWComponent
     * memeber of a pathway.
     */
    LWComponent getElement(int pRow){
        Iterator i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (row++ == pRow)
                return p;
            if (p.isOpen()) {
                Iterator ci = p.getElementIterator();
                while (ci.hasNext()) {
                    LWComponent c = (LWComponent) ci.next();
                    if (row++ == pRow)
                        return c;
                }
            }
        }
        throw new IllegalArgumentException("Couldn't find any element at row " + pRow);
        
        /* The simple but slow version of getElement:
        return (LWComponent) getList().get(pRow);
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
        if (getPathwayList() != null) {
            LWPathway p = getPathwayForElementAt(row);
            if (p.isLocked())
                return false;
            if (col == 3) // label always editable
                return true;
            if (getElement(row) instanceof LWPathway)
                return col == 1;  // if pathway, color also editable
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
                case 3: return p.getDisplayLabel();
                case 4: return new Boolean(p.hasNotes());
                case 5: return new Boolean(p.isLocked());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway cell:" + e);
            } 
        } else {
            try {
                if (col == 3) return c.getDisplayLabel();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway element cell:" + e);
            }  
        }
        return null;
    }

    public void setValueAt(Object aValue, int row, int col){
        if (DEBUG.PATHWAY) System.out.println(this + " setValutAt " + row + "," + col + " " + aValue);
        LWComponent c = getElement(row);
        if (c instanceof LWPathway){
            LWPathway p = (LWPathway) c;

            if (col == 0) {
                p.setVisible(!p.isVisible()); // not proper
            } else if (col == 1){
                p.setStrokeColor((Color)aValue); // proper
            } else if (col == 2){
                p.setOpen(!p.isOpen()); // not proper
            } else if (col == 3){
                p.setLabel((String)aValue); // proper
            } else if (col == 5) {
                //p.setLocked(((Boolean)aValue).getBooleanValue()); // proper
                p.setLocked(!p.isLocked()); // not proper
            }
        } else if (c != null) {
            if (col == 3)
                c.setLabel((String)aValue);
        }
    }


    private class DataEvent extends javax.swing.event.TableModelEvent {
        private Object invoker;
        DataEvent(Object invoker) {
            super(PathwayTableModel.this);
            this.invoker = invoker;
        }
        public String toString()
        {
            return "TableModelEvent["
                + "src=" + getSource()
                + " rows=" + getFirstRow() + "-" + getLastRow()
                + " col=" + getColumn()
                + " type=" + getType()
                + " invoker=" + invoker.getClass().getName()
                + "]";
        }
    }

    
    /** for PathwayTable */
    /*
    LWPathway getCurrentPathway(){
        if (getPathwayList() != null)
            return getPathwayList().getActivePathway();
        else
            return null;
    }
    */
    

}
