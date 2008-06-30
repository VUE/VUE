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

/*
 * MapMetadataModel.java
 *
 * Created on February 14, 2004, 2:40 PM
 */

package tufts.vue.filter;


/**
 *
 * @author  akumar03
 */

import java.util.*;
import javax.swing.table.*;
public class MapFilterModel  extends AbstractTableModel{
    
    /** Creates a new instance of MapMetadataModel */
    public interface Listener extends java.util.EventListener {
        public void mapFilterModelChanged(MapFilterModelEvent e);
    }
    private Vector keyVector;
    boolean editable;
    private List listeners = new ArrayList();
    public MapFilterModel(boolean editable) {
        this();
        this.editable = editable;
    }
    public MapFilterModel() {
        keyVector = new Vector();
    }
    public synchronized void add(Key key) {
        keyVector.add(key);
        notifyListeners(new MapFilterModelEvent(this,key,MapFilterModelEvent.KEY_ADDED));
    }
    public synchronized void remove(Key key) {
        int i = keyVector.indexOf(key);
        keyVector.remove(keyVector.indexOf(key));
        Iterator iter = tufts.vue.VUE.getActiveMap().getAllDescendents().iterator();
        while(iter.hasNext()) 
            ((tufts.vue.LWComponent)iter.next()).getNodeFilter().removeStatements(key);
        tufts.vue.VUE.getActiveMap().getLWCFilter().removeStatements(key);
        notifyListeners(new MapFilterModelEvent(this,key,MapFilterModelEvent.KEY_DELETED));
    }
    
    public synchronized void remove(int i) {
        remove(get(i));
    }
        
    /** adds the listeners if it doesn't exitst **/
    public synchronized void addListener(Listener l){
        if(listeners.indexOf(l) < 0)
            listeners.add(l);
      
    }
    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }
    private synchronized void notifyListeners(MapFilterModelEvent e)  {
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            Listener l = (Listener) i.next();
            try {
                l.mapFilterModelChanged(e);
            } catch (Exception ex) {
                System.err.println(this + " notifyListeners: exception during selection change notification:"
                + "\n\tselection: " + this
                + "\n\tfailing listener: " + l);
                ex.printStackTrace();
                //java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }
    
    /** for Actions.java */
    public ArrayList getListeners() {
        return (ArrayList)listeners;
    }
    
    public void setListeners(ArrayList listeners) {
      this.listeners = listeners;
    }
    public void addAll(MapFilterModel keys) {
        keyVector.addAll(keys.getKeyVector());
    }
    public void removeAll(MapFilterModel keys) {
        keyVector.removeAll(keys.getKeyVector());
    }
    public void removeAllElements() {
        keyVector.removeAllElements();
    }
    public int size() {
        return keyVector.size();
    }
    
    public Key get(int i) {
        return (Key)keyVector.get(i);
    }
    public void setKeyVector(Vector keyVector) {
        this.keyVector = keyVector;
    }
    
    public void addKey(Key key) {
        this.add(key);
    }
    
    public boolean isEditable() {
        return editable;
    }
    
    public String getColumnName(int col) {
        if (col==0) {
            return "Field";
        } else {
            return "Type";
        }
    }
    
    public int getRowCount() {
        return this.size();
    }
    
    public int getColumnCount() {
        return 2;
    }
    
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
    
    public Object getValueAt(int row, int col) {
        Key  key=(Key) this.get(row);
        if (col==0)
            return key.getKey().toString();
        else
            return key.getType().getDisplayName();
        
    }
    public void setValueAt(Object value, int row, int col) {
        
        Key key = (Key)this.get(row);
        if(col == 0)
            key.setKey((String)value);
        // row = -1 adds new condions else replace the existing one.
        
        fireTableCellUpdated(row, col);
    }
    
    
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        //  return editable;
        if(col == 0)
            return true;
        else
            return false;
    }
    
    public Vector getKeyVector() {
        return keyVector;
    }
    public boolean add(Object o) {
        throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    }
    
    
}
