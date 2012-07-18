/*
* Copyright 2003-2010 Tufts University  Licensed under the
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


package edu.tufts.vue.compare;

import java.util.*;
import java.io.*;
import tufts.vue.DEBUG;
import tufts.vue.LWComponent;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWImage;


/**
 * @author akumar03
 * @author Scott Fraize re-write 2012
 *
 * The class creates a connectivity Matrix for a VUE map.
 *
 * Further information on connectivity matrix can be found:
 * @see http://w3.antd.nist.gov/wctg/netanal/netanal_netmodels.html
 *
 * The matrix can be used to assess the connectivity among a given set of nodes.  A value of
 * connetion is 1 if there is a connection between nodes:
 * connection(a,b) = 1 implies there is a link from a to b
 * connection(b,a) = 1 implies there is a link from b to a
 * connection(b,a) may not be equal to connection(a,b)
 * connection(a,b) = connection(b,a) implies the link between a and b is not directed.
 */
public class ConnectivityMatrix
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ConnectivityMatrix.class);
    
    public static final int INDEX_ERROR = -1;

    protected final LWMap map;
    protected final IndexedCountingSet keys;
    protected final int cx[][];
    
    protected int scanCount = 0;
    protected int hitCount = 0;

    /**
     * An object set that provides a fixed index value for each unique object, a count for repeat
     * objects, and fast constant lookup times for all queries.  Note that there is no error
     * checking with indexOf or count (in typical usage, we want exceptions if we don't know what
     * we're asking for).  Use findIndex to see -1 return value for objects not in the set.
     * Indicies and iteration order occur in the preserved insertion order.
     */
    protected static class IndexedCountingSet<T> implements Iterable<T> {
        private static final int INDEX = 0, COUNT = 1;
        // redundant data-structures for x-way lookups:
        private final Map<T,int[]> map = new HashMap<T,int[]>();
        private final List<T> values = new ArrayList<T>();
        private int unique = 0;
        private int maxLen = 0;
        
        void add(T s) {
            if (s == null) {
                if (DEBUG.Enabled) Log.warn("null value ignored in IndexedCountingSet");
                return;
            }
            int[] entry = map.get(s);
            if (entry != null) {
                entry[COUNT]++;
            } else {
                entry = new int[2];
                entry[INDEX] = unique++;
                entry[COUNT] = 1;
                map.put(s, entry);
                values.add(s); // values.size() == unique
                String ts = s.toString();
                if (ts.length() > maxLen)
                    maxLen = ts.length();
            }
        }
      //int count(T s)          { try{return map.get(s)[COUNT];}catch(Throwable t){return 0;}}
        int count(T s)          { return map.get(s)[COUNT]; }
        int indexOf(T s)        { return map.get(s)[INDEX]; }
          T get(int atIndex)    { return values.get(atIndex); }
        int size()              { return unique; }
        boolean contains(T s)   { return map.containsKey(s); }

        /** @return index, or -1 if not found */
        int findIndex(T s) {
            final int[] entry = map.get(s);
            return entry == null ? INDEX_ERROR : entry[INDEX];
        }

        void addAll(IndexedCountingSet<T> mergeIn) {
            for (T val : mergeIn.values)
                add(val);
        }

        int maxLength() { return maxLen; }
        
        public Iterator<T> iterator()  { return values.iterator(); }
    }
    
    protected ConnectivityMatrix(IndexedCountingSet preComputedSet) {
        this.map = null;
        this.keys = preComputedSet;
        this.cx = new int[keys.size()][keys.size()];
        if (DEBUG.Enabled) Log.debug(this + " created from pre-computed.");
    }
    
    public ConnectivityMatrix(LWMap map) {
        this.map = map;
        this.keys = new IndexedCountingSet();
        
        final Collection<LWComponent> allInMap = map.getAllDescendents(ChildKind.PROPER);
        
        indexMergeKeys(allInMap);
        // after adding all keys, we know exactly how big to make the matrix
        this.cx = new int[keys.size()][keys.size()];
        generateMatrix(allInMap);
        if (DEBUG.Enabled) Log.debug(this + " created.");
    }
    
    public IndexedCountingSet getKeys() { return keys; }
    public boolean containsKey(String key) { return keys.contains(key); }
    public int size() { return keys.size(); }
    public LWMap getMap() { return map; }
    public int[][] getMatrix() { return cx; }
    

    public static final boolean isValidTarget(LWComponent c) {
        if (c != null) {
            if (c.hasFlag(LWComponent.Flag.ICON)) {
                return false;
            } else {
                final Class clazz = c.getClass();
                return clazz == tufts.vue.LWNode.class || clazz == tufts.vue.LWImage.class;
            }
        } else {
            return false;
        }
    }
    
    /**The method adds labels to the connectivity matrix */
    private void indexMergeKeys(final Collection<LWComponent> allInMap) {
        if (DEBUG.MERGE) Log.debug("indexing [" + Util.getMergeProperty() + "] merge keys for map " + map);
        for (LWComponent c : allInMap) {
            if (isValidTarget(c)) {
                final Object key = getMergeKey(c);
                if (key != null) {
                    keys.add(key);
                    hitCount++;
                }
                scanCount++;
            }
        }
        if (DEBUG.MERGE) Log.debug(String.format("merge keys: %d valid targets scanned, %d keys found, %d unique.", scanCount, hitCount, size()));
    }

    /** set connection values for merge-key components that have a VUE map-link between them */
    private void generateMatrix(final Collection<LWComponent> allInMap) {
        Log.info("generateMatrix for " + tufts.Util.tags(allInMap));
        for (LWComponent c : allInMap) {
            if (c instanceof LWLink) {
                final LWLink link = (LWLink) c;
                final LWComponent head = link.getHead(); // note: will be null if pruned (or use getPersistHead)
                final LWComponent tail = link.getTail(); // note: will be null if pruned (or use getPersistHead)
                if (isValidTarget(head) && isValidTarget(tail)) {
                    try {
                        final int arrowState = link.getArrowState();
                        final Object headKey = getMergeKey(head);
                        final Object tailKey = getMergeKey(tail);
                        if (headKey == null || tailKey == null) {
                            // Just because both are valid targets does *not* mean a valid
                            // merge-key will be found for them.
                            continue;
                        }
                        final int headIndex = keys.findIndex(headKey);
                        final int tailIndex = keys.findIndex(tailKey);
                        
                        if (arrowState == LWLink.ARROW_BOTH || arrowState == LWLink.ARROW_NONE) {
                            cx[headIndex][tailIndex] = 1;
                            cx[tailIndex][headIndex] = 1;
                        } else if (arrowState == LWLink.ARROW_HEAD) {
                            cx[tailIndex][headIndex] = 1;
                        } else if (arrowState == LWLink.ARROW_TAIL) {
                            cx[headIndex][tailIndex] = 1;
                        }
                    } catch (Throwable t) {
                        // Should never happen, but theoretically could get NPE or ArrayOutOfBounds
                        Log.debug("exception: skipping link: " + link + "; " + t);
                    }
                }
            }
        }
        //Log.info("generateMatrix complete.");
    }
    
    
    public int getConnection(int i, int j) {
        return cx[i][j];
    }
    
    /** @return connection value found for these two keys, if any, otherwise 0 */
    public int getConnection(Object key1, Object key2) {
        final int row = keys.findIndex(key1);
        final int col = keys.findIndex(key2);
        if (row >= 0 && col >=0) 
            return this.cx[row][col];
        else 
            return 0;
    }
    
    public void setConnection(Object key1, Object key2, int value) {
        final int index1 = keys.findIndex(key1);
        final int index2 = keys.findIndex(key2);
        if (index1 >= 0 && index2 >=0)
            this.cx[index1][index2] = value;
    }
    
    /**
     * Compares a connectivity matrix to input connectivity matrix
     * returns truf if both are same
     * @param c2 ConnectivityMatrix to be compared to
     * @return boolean
     *
     */
    public boolean compare(ConnectivityMatrix c2) {
        final int size = size();
        if(c2.size() != size) 
            return false;
        for(int i=0;i<size;i++) {
            for(int j=0;j<size;j++) {
                if (this.cx[i][j] != c2.cx[i][j])
                    return false;
            }
        }
        return true;
    }

    // public void store(OutputStream out) {
    //     try {
    //         out.write(this.asString().getBytes());
    //     }catch(IOException ex) {
    //         System.out.println("ConnectivityMatrix.store:"+ex);
    //     }
    // }
    
    public String toString() {
        return getClass().getSimpleName() + "[" + size() + "^2=" + (size()*size()) + " for map " + (map==null?"<aggregate>":map) + "]";
    }
        
    private static final String NewLine = System.getProperty("line.separator");
    private static final char TAB = '\t';
    private static final char SPACE = ' ';
    private static final boolean LABELS_TOP = true;
    private static final boolean LABELS_TOP_CHAR = true; // 1st-char of label only
    private static final boolean LABELS_TOP_BIG = (LABELS_TOP && !LABELS_TOP_CHAR);
    private static final boolean LABELS_LEFT = true;
    
    public String asString()
    {
        final int size = keys.size(); 

        int capacity = size * size * 2 + size; // grid + newlines

        final int maxLeftLen = Math.min(keys.maxLength(), 42); // if using tabs, better multiple of 8 less 1

        // note: only really need to have have this generate to a Writer (buffered)
        // instead of having to guess capacity to get performance.  This is
        // normally only used to write out to a file.

        if (LABELS_TOP)
            capacity += size * (LABELS_TOP_CHAR ? 2 : 8);
        if (LABELS_LEFT)
            capacity += size * (maxLeftLen+1);

        if (DEBUG.MERGE) {
            Log.debug("toString:   cx.size=" + cx.length + " (max alloc matrix)");
            Log.debug("toString: nodesSeen=" + scanCount); 
            Log.debug("toString: keysFound=" + hitCount);
            Log.debug("toString: keys.size=" + keys.size() + " (unique property values)");
            Log.debug("toString:  capacity=" + capacity);
        }

        final StringBuilder b = new StringBuilder(capacity);
        
        
        if (LABELS_TOP) {
            if (LABELS_LEFT) for (int i = 0; i <= maxLeftLen; i++) b.append(' ');
            for (Object key : this.keys) {
                if (key != null) {
                    if (LABELS_TOP_CHAR) {
                        final String txt = key.toString().trim();
                        b.append(txt.length() > 0 ? txt.charAt(0) : '?');
                        b.append(' ');
                    } else {
                        final String txt = key.toString().replace('\n', ' ').trim();
                        if (txt.length() > 7)
                            b.append(txt, 0, 7);
                        else
                            b.append(txt);
                        b.append(TAB);
                    }
                }
            }
            b.append(NewLine);
        }
        
        // Note: this used to use the input-size count, which would end up generating an unlabeled
        // column with all zeros in them for ever node that had the a same merge-property (e.g,
        // nodes with same label).
        for (int row = 0; row < size; row++) {
            if (LABELS_LEFT) {
                // b.append(String.format("%*.*s", maxLeftLen, maxLeftLen, labels.get(i).replace('\n', ' ').trim()));
                final String txt = this.keys.get(row).toString().replace('\n', ' ').trim();
                if (txt.length() > maxLeftLen) {
                    b.append(txt, 0, maxLeftLen);
                } else {
                    for (int i = txt.length(); i < maxLeftLen; i++)
                        b.append(SPACE);
                    b.append(txt);
                }
                // I'd be nice to have an invisible char separator at end of label here in case
                // anyone wants to script process this data (e.g., even a tab).  Tho I suppose they
                // could get the label-field length from the 1st index of the top-char label.
                // (A further problem would be that labels themselves can have special chars in them)
                b.append(SPACE);
            } 
            for (int col = 0; col < size; col++) {
                if (col != 0)
                    b.append(LABELS_TOP_BIG ? TAB : SPACE);
                final int val = cx[row][col];
                if (val == 0)
                    b.append((row==col) ? '0' : '.');
                else
                    b.append(val);
            }
            b.append(NewLine);
        }

        if (DEBUG.MERGE) Log.debug("toString:    length=" + b.length());
        return b.toString();
    }
    
    public static final Object getMergeKey(LWComponent node) {
        // Log.debug("getMergeKey: " + Util.getMergeKey(node));
        return Util.getMergeProperty(node);
    }
}
