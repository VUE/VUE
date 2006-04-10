 /*
  * -----------------------------------------------------------------------------
  *
  * <p><b>License and Copyright: </b>The contents of this file are subject to the
  * Mozilla Public License Version 1.1 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of the License
  * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
  *
  * <p>Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
  * the specific language governing rights and limitations under the License.</p>
  *
  * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
  * Tufts University. All rights reserved.</p>
  *
  * -----------------------------------------------------------------------------
  */


package tufts.vue;

import java.util.*;

/**
 * A general HashMap for storing property values: e.g., meta-data.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-04-10 18:41:22 $ / $Author: sfraize $
 */

public class PropertyMap extends java.util.HashMap {

    private SortedMapModel mTableModel;
    private boolean mHoldingChanges = false;
    private int mChanges;

    public PropertyMap() {}

    public synchronized Object put(Object k, Object v) {
        Object prior = super.put(k, v);

        // todo: this a bit overkill: could have a higher level
        // trigger for this, instead of triggering any table listeners
        // every time.  Our hold/release deals with specific batch
        // loads, which is good enough for now.
        
        if (mHoldingChanges)
            mChanges++;
        else if (mTableModel != null)
            mTableModel.reload();

        return prior;

    }
    public synchronized Object get(Object k) {
        return super.get(k);
    }

    public String getProperty(Object key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }
    
    public void setProperty(String key, String value) {
        put(key, value);
    }

    /** No listeners will be updated until releaseChanges is called.  Multiple
     * overlapping holds are okay. */
    public synchronized void holdChanges() {
        mHoldingChanges = true;
    }

    /**
     * If any changes made since holdChanges, listeners will be notified.
     * It is crucial this is called at some point after holdChanges,
     * or listeners may never get updated. Tho ideally only one call to
     * this per holdChanges is made, Extra calls to this are okay 
     * -- it will at worst degrade update performance with rapid updates.
     */
    public synchronized void releaseChanges() {
        mHoldingChanges = false;
        if (mChanges > 0 && mTableModel != null) {
            if (DEBUG.RESOURCE) System.out.println("RsrcProps: releasing changes " + mChanges);
            mTableModel.reload();
        }
        mChanges = 0;
    }

    public synchronized java.util.Properties asProperties() {
        java.util.Properties props = new java.util.Properties();
        // todo: this not totally safe: values may not all be strings
        props.putAll(this);
        return props;
    }

    public synchronized javax.swing.table.TableModel getTableModel() {
        if (mTableModel == null)
            mTableModel = new SortedMapModel();
        return mTableModel;

    }

    private static class Entry implements Comparable {
        final String key;
        final Object value;
        Entry(Map.Entry e) {
            key = tufts.Util.upperCaseWords((String) e.getKey());
            value = e.getValue();
        }

        public int compareTo(Object o) {
            return key.compareTo(((Entry)o).key);
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    private class SortedMapModel extends javax.swing.table.AbstractTableModel {

        private Entry[] mEntries;
        
        SortedMapModel() { reload(); }

        // make sure there is a sync on the HashMap before this is called
        private void reload() {
            mEntries = new Entry[size()];
            Iterator i = entrySet().iterator();
            int ei = 0;
            while (i.hasNext())
                mEntries[ei++] = new Entry((Map.Entry)i.next());
            Arrays.sort(mEntries);

            /*
            mEntries = (Map.Entry[]) PropertyMap.this.entrySet().toArray(new Map.Entry[size()]);
            Arrays.sort(mEntries, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String k1 = (String) ((Map.Entry)o1).getKey();
                        String k2 = (String) ((Map.Entry)o2).getKey();
                        return k1.compareTo(k2);
                    }});
            */
            if (DEBUG.RESOURCE) System.out.println("RsrcProps: model loaded " + Arrays.asList(mEntries));
            fireTableDataChanged();
        }
        
        public int getRowCount() {
            return mEntries.length;
        }
        
        public Object getValueAt(int row, int col) {
            if (col == 0)
                return mEntries[row].key;
            else
                return mEntries[row].value;
        }

        public int getColumnCount() { return 2; }

        public String getColumnName(int col) {
            //return col == 0 ? "Field" : "Value";
            return null;
        }

        public Class getColumnClass(int colIndex) {
            return colIndex == 0 ? String.class : Object.class;
        }

        
        
    }
}