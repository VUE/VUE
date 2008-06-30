 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Vector;
import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.table.*;

import tufts.vue.LWPathway.Entry;

/**
 * PathwayTableModel.java
 *
 * Provides a view of the currently active LWPathwayList (from the currently active map).
 * This view is a list of all the pathways and their contents in a single list, with
 * the option of a pathway being "closed", in which case it's contents doesn't appear
 * in the list.
 *
 * @see PathwayTable
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Scott Fraize
 * @author  Jay Briedis
 * @version February 2004
 */
public class PathwayTableModel extends DefaultTableModel
    implements ActiveListener, LWComponent.Listener
{
    private LWMap mMap;

    final static String[] ColumnNames = {"A", "B", "C", "D", "E", "F"};

    public static final int COL_VISIBLE = 1;
    public static final int COL_OPEN = 0;
    //public static final int COL_MAPVIEW = 6;
    public static final int COL_LABEL = 2; // Applies to pathway's and pathway members
    public static final int COL_NOTES = 3; // Applies to pathway's and pathway members
    
    public static final int COL_LOCKEDnMAPVIEW = 5;
    public static final int COL_COLOR = 4;
    
    
    
   // public static final int COL_REVEALER = 1;
    
    public PathwayTableModel()
    {
        VUE.addActiveListener(LWMap.class, this);
        //VUE.addActiveListener(LWPathway.class, this);
        VUE.addActiveListener(LWPathway.Entry.class, this);
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
        boolean fireChange = false;
        if (e.key instanceof LWComponent.Key) {
            if (e.getKey().type == LWComponent.KeyType.DATA)
                fireChange = true;
        } else {
            // if we can't know the key type, just fire no matter what
            fireChange = true;
        }
        if (fireChange) {
            //System.out.println("TABLE DATA CHANGED: " + e);
            fireTableDataChanged();
        }

        /*
        if (e.getSource() instanceof LWPathway) {
            // The events mainly of interest to us are either a structural event, or a LWPathway label/note event,
            // although if anything in the pathway changes, fire a change event just in case.
            if (DEBUG.PATHWAY) System.out.println(this + " pathway event " + e);
            fireTableDataChanged();
        } else if (e.key == LWKey.Label || e.getName().startsWith("pathway.")) {
            if (DEBUG.PATHWAY) System.out.println(this + " pathway child event " + e);
            // This means one of the LWComponents in the pathway has changed.
            // We only care about label changes as that's all that's displayed
            // in the PathwayTable, or if pathway.notes has changed so we can update
            // the note icon.
            // We only really need the PathwayTable to repaint if a label
            // has changed, but this will do it.

            // TODO: a global selection (ActiveChangeSupport / ActiveSelection) of a
            // single pathway will serve the purposes of the currently selected
            // pathway -- we should be able to get rid of this pathway.list.active event...
            
            if (false && e.getName().equals("pathway.list.active"))
                ;//setCurrentPathway((LWPathway) e.getComponent());
            else
                fireTableDataChanged();
        }
        */
    }

    public void activeChanged(ActiveEvent e)
    {
        if (e.type == LWMap.class) {
            setMap((LWMap) e.active);
        } else if (e.type == LWPathway.Entry.class) {
            final LWPathway.Entry entry = (LWPathway.Entry) e.active;
            final LWPathway pathway = (entry == null ? null : entry.pathway);
            setCurrentPathway(pathway);
            if (getPathwayList() != null) {           
                getPathwayList().setActivePathway(pathway);
                fireTableDataChanged();
            }
        }
//         else if (e.type == LWPathway.class) {
//             if (getPathwayList() != null) {           
//                 getPathwayList().setActivePathway((LWPathway) e.active);
//                 fireTableDataChanged();
//             }
//         }
            
    }

    LWPathwayList getPathwayList() {
        return mMap == null ? null : mMap.getPathwayList();
    }
    Iterator<LWPathway> getPathwayIterator() {
        return mMap == null ? VueUtil.EmptyIterator : mMap.getPathwayList().iterator();
    }
    public void moveRow(int start,int end, int to, LWPathway pathway)
    {
    	//System.out.println("MOVE : " + start + " to: " + to);
    	//final LWPathway activePathway = VUE.getActivePathway();    	
        pathway.moveEntry(start, to);
        fireTableDataChanged();
    	 return;
    
    	
    }
    void fireChanged(Object invoker) {
        fireTableChanged(new DataEvent(invoker));
    }

    private void setCurrentPathway(LWPathway path) {
        if (getPathwayList() != null){           
            getPathwayList().setActivePathway(path);
            fireTableDataChanged();
            //VUE.setActive(LWPathway.class, this, path);
        }
    }

    /* for PathwayPanel 
    int getCurrentPathwayIndex(){
        return getList().indexOf(VUE.getActivePathway());
    }
*/
    /*
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
     
    
    /*
     * for PathwayTable
     * Returns index of element within given pathway.  We need
     * this because an element can appear in the pathway more
     * than once, and this is how we differentiate them (by index).
     * If the element at @param pRow is a pathway, return -1.*/
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
    /*
    private List getList() {
        List list = new ArrayList();
        Iterator<LWPathway> i = getPathwayIterator();
        while (i.hasNext()) {
            LWPathway p = i.next();
            list.add(p);
            if (p.isOpen()) {
                for (LWPathway.Entry e : p.getEntries())
                    list.add(e.node);
            }
        }
        return list;
    }
    */

    /**
     * @return the entry currently displayed in the given row.
     */
    LWPathway.Entry getEntry(int pRow) {
        if (pRow < 0)
            return null;
        Iterator<LWPathway> i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = i.next();
            if (row++ == pRow)
                return p.asEntry();
            if (p.isOpen()) {
                for (LWPathway.Entry e : p.getEntries()) {
                    if (row++ == pRow)
                        return e;
                }
            }
        }
        return null;
    }


    /**
     * @return the entry display at the given row.  If the given
     * entry is not currently displayed (the pathway is closed),
     * return -1.
     */
    int getRow(LWPathway.Entry findEntry) {
        Iterator<LWPathway> i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = i.next();
            if (findEntry.isPathway() && findEntry.pathway == p)
                return row;
            row++;
            if (p.isOpen()) {
                for (LWPathway.Entry e : p.getEntries()) {
                    if (e == findEntry)
                        return row;
                    row++;
                }
            }
        }
        return -1;
    }
    
    /*
    Object getElement(int pRow) {
        if (pRow < 0)
            return null;
        Iterator<LWPathway> i = getPathwayIterator();
        int row = 0;
        while (i.hasNext()) {
            LWPathway p = i.next();
            if (row++ == pRow)
                return p;
            if (p.isOpen()) {
                for (LWPathway.Entry e : p.getEntries()) {
                    if (row++ == pRow)
                        return e;
                }
            }
        }
        return null;
    }
    */

    public synchronized int getRowCount()
    {
        int rows = 0;
        Iterator<LWPathway> i = getPathwayIterator();
        while (i.hasNext()) {
            LWPathway p = i.next();
            rows++;
            if (p.isOpen())
                rows += p.length();
        }
        return rows;
        
        ///final int rowCount = getList().size();
        //if (DEBUG.PATHWAY) System.out.println("getRowCount=" + rowCount);
        //return rowCount;
    }

    public int getColumnCount() {
        return ColumnNames.length;
    }
    
    public String getColumnName(int col){
        return ColumnNames[col];
    }

    public Class getColumnClass(int col){
        if (col == COL_COLOR)
            return Color.class;
        else if (col == COL_VISIBLE || col == COL_NOTES || col == COL_LOCKEDnMAPVIEW)// || col == COL_MAPVIEW)
            return ImageIcon.class;
        else if (col == COL_LABEL || col == COL_OPEN)
            return Object.class;
       // else if (col == COL_REVEALER)
       //     return Boolean.class;
        //return javax.swing.JLabel.class;
        else
            return null;
    }
    
    public boolean isCellEditable(int row, int col){
        if (getPathwayList() != null) {
            final LWPathway.Entry entry = getEntry(row);

            if (entry == null)
                return false;
            
            if (entry.pathway.isLocked())
                return false;

            //if (col == 3) // label always editable
            //return true;
            // 2 problems: need to add end-of-action undo
            // marker, and as single-line edit will blow
            // away any newlines in the label.

            if (entry.isPathway())
                return col == COL_COLOR || col == COL_LABEL;// || col == COL_REVEALER;
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
        final LWPathway.Entry entry = getEntry(row);
        if (entry == null)
            return null;
        
        if (entry.isPathway()) {
            LWPathway p = entry.pathway;
            try {
                switch (col) {
                case COL_VISIBLE: return p.isVisible() ? Boolean.TRUE : Boolean.FALSE;
                case COL_COLOR: return p.getStrokeColor();
                case COL_OPEN: return p.isOpen() ? Boolean.TRUE : Boolean.FALSE;
                case COL_LABEL: return p.getDisplayLabel();
                case COL_NOTES: return p.hasNotes() ? Boolean.TRUE : Boolean.FALSE;
                case COL_LOCKEDnMAPVIEW: return p.isLocked() ? Boolean.TRUE : Boolean.FALSE;
              //  case COL_REVEALER: return new Boolean(getPathwayList().getRevealer() == p);
                //case COL_REVEALER: return new Boolean(p.isRevealer());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway cell:" + e);
            } 
        } else {
            try {
                if (col == COL_LABEL)                               	
                    return entry.getLabel();  
                else if (col == COL_LOCKEDnMAPVIEW)
                    return entry.isMapView() ? Boolean.TRUE : Boolean.FALSE;
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("exception in the table model, setting pathway element cell:" + e);
            }  
        }
        return null;
    }

    public void setValueAt(Object aValue, int row, int col){
        if (DEBUG.PATHWAY) System.out.println(this + " setValutAt " + row + "," + col + " " + aValue);
        LWPathway.Entry entry = getEntry(row);
        if (entry == null)
            return;
        
        if (entry.isPathway()) {
            LWPathway p = entry.pathway;
            boolean bool = false;
            if (aValue instanceof Boolean)
                bool = ((Boolean)aValue).booleanValue();

                 if (col == COL_VISIBLE) { p.setVisible(!p.isVisible()); }        // not proper (must use aValue to be proper)
            else if (col == COL_COLOR)   { p.setStrokeColor((Color)aValue); }     // proper
            else if (col == COL_OPEN)    { p.setOpen(!p.isOpen()); }              // not proper
            else if (col == COL_LABEL)   { entry.setLabel((String)aValue); }      // proper
            else if (col == COL_LOCKEDnMAPVIEW)  { p.setLocked(!p.isLocked()); }          // not proper
            //else if (col == COL_MAPVIEW)
        	//{entry.setMapView(((Boolean)aValue).booleanValue());        	
        	//}
           /* else if (col == COL_REVEALER) {
                if (bool)
                    getPathwayList().setRevealer(p);
                else
                    getPathwayList().setRevealer(null);
            }*/
        } else {
            if (col == COL_LABEL) entry.setLabel((String)aValue);
            else if (col == COL_LOCKEDnMAPVIEW)
            	{
                    if (!entry.isOffMapSlide()) {
                        entry.setMapView(!entry.isMapView());
                        //VUE.getSlideViewer().reload();
                    }
            
            	}
            	
        }
        VUE.getUndoManager().mark();
        // all the above sets will trigger LWCEvents, listeneted to by the
        // LWPathways they're in, which are listeneted to by their map's
        // LWPathwayList, which is listented to by us, the PathwayTableModel.
        // When we get the callback to LWCChanged, we call fireTableDataChanged.
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
