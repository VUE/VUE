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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.table.*;
public class MapFilterModel  extends AbstractTableModel{
    
    /** Creates a new instance of MapMetadataModel */
    
    private Vector keyVector;
    boolean editable;
    public MapFilterModel(boolean editable) {
        this();
        this.editable = editable;
    }
    public MapFilterModel() {
        keyVector = new Vector();
    }
    public synchronized void add(Key key) {
        keyVector.add(key);
    }
    public void remove(Key key) {
        keyVector.remove(key);
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
