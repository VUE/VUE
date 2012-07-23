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
import tufts.vue.*;

/**
 * @author akumar03
 * @author Scott Fraize re-write 2012
 */
public class WeightAggregate extends ConnectivityMatrix
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(WeightAggregate.class);
    
    private final int count;

    public static WeightAggregate create(List<ConnectivityMatrix> matrices) {
        final IndexedCountingSet preComputed = new IndexedCountingSet();
        for (ConnectivityMatrix matrix : matrices)
            preComputed.addAll(matrix.keys);
        // We pre-compute the index so the matrix aggregate will know how big
        // of a matrix will be required to hold it all.
        return new WeightAggregate(preComputed, matrices);
    }
    
    protected WeightAggregate(IndexedCountingSet preComputed, List<ConnectivityMatrix> matrices) {
        super(preComputed);
        this.count = matrices.size();
        for (ConnectivityMatrix input : matrices)
            mergeInConnectionValues(input);
    }

    /**
     * This will search the input matrix for all non-zero connection values and add them to the
     * connection value for the same two keys in this aggregate.  This will only work if the keys
     * from the input matrix have already been merged to this aggregate, and the matrix for this
     * aggregate has been sized to handle all of the input keys.
     */
    private void mergeInConnectionValues(final ConnectivityMatrix input) {
        Log.info("mergeIn " + input);
        final int inSize = input.size();
        for (int index1 = 0; index1 < inSize; index1++) {
            for (int index2 = 0; index2 < inSize; index2++) {
                final int connection = input.cx[index1][index2];
                if (connection != 0) {
                    final Object key1 = input.keys.get(index1);
                    final Object key2 = input.keys.get(index2);
                    this.cx[this.keys.indexOf(key1)]
                           [this.keys.indexOf(key2)] += connection;
                    // Log.debug("merged " + connection + " for " + key1 + "," + key2);
                }
            }
        }
    }
    
    public ConnectivityMatrix getAggregate(){
        return this;
    }
    
    /** @return the number of nodes with the given merge-key (e.g. label) in the aggregate */        
    public int getNodeCount(Object key) {
        try {
            return super.keys.count(key);
        } catch (NullPointerException e) {
            Log.warn("getNodeCount: key not present: " + tufts.Util.tags(key));
            return 0;
        }
    }

    
    /** this does not return exactly the % of maps a node is found on: a node many times
     * on a single map will increase it's weight, yes?  todo: do I have that right?
     * See mergeInConnectionValues... */
    public double getPercentFound(LWComponent c) {

        final int nodeCount = getNodeCount(Util.getMergeProperty(c));

        if (nodeCount <= 0) {
            Log.warn("not found w/key: " + c);
            return 0.0;
        }
        return (100.0 * nodeCount) / (double) this.count;
    }
    
    
    /** @return the number of matricies summed into this aggregate */
    public int getCount() {
        return this.count;
    }
}
