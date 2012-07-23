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

import tufts.vue.DEBUG;
import java.util.*;

/**
 * @author akumar03
 * @author Scott Fraize re-write 2012
 */
public class VoteAggregate extends WeightAggregate
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VoteAggregate.class);

    public static final int POSITIVE_VOTE = 1;
    public static final int NEGATIVE_VOTE = 0;
    
    /** The percentage of maps that must contain this node (by merge-key) before the vote is yes */
    private final double nodePercentMaps;
    /** The percentage of maps that must contain this link (by endpoint merge-keys) before the vote is yes */
    private final double linkPercentMaps;
    
    private final int aggregateLinkThreshold;
    private final int aggregateNodeThreshold;

    protected final int[][] linkCounts;
    
    public static VoteAggregate create(List<ConnectivityMatrix> matrices, double nodeThresh, double linkThresh) {
        final IndexedCountingSet preComputed = new IndexedCountingSet();
        for (ConnectivityMatrix matrix : matrices)
            preComputed.addAll(matrix.keys);
        // We pre-compute the index so the matrix aggregate will know how big
        // of a matrix will be required to hold it all.
        return new VoteAggregate(preComputed, matrices, nodeThresh, linkThresh);
    }
    
    private VoteAggregate(IndexedCountingSet preComputed, List<ConnectivityMatrix> matrices, double nodePercent, double linkPercent) {
        super(preComputed, matrices);
        // WeightAggregate superclass will have merged in all the matrices to produce counts: we save them here
        this.linkCounts = super.cx.clone();
        this.nodePercentMaps = nodePercent;
        this.linkPercentMaps = linkPercent;
        final double nodeThresh = nodePercent * (double) matrices.size();
        final double linkThresh = linkPercent * (double) matrices.size();
        this.aggregateNodeThreshold = (int) nodeThresh;
        this.aggregateLinkThreshold = (int) linkThresh;
        if (DEBUG.Enabled) {
            Log.debug(this + " nodePercent " + nodePercent + "*count(" + getCount() + ") = vote threshold: " + nodeThresh + "=" + aggregateNodeThreshold);
            Log.debug(this + " linkPercent " + linkPercent + "*count(" + getCount() + ") = vote threshold: " + linkThresh + "=" + aggregateLinkThreshold);
        }
        computeLinkVotes();
    }
    
    private void computeLinkVotes()
    {
        final int size = super.size();

        // This will cause super.getConnection to return 0 or 1,
        // depending on if the vote passed.

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (super.cx[i][j] > 0 && super.cx[i][j] >= aggregateLinkThreshold)
                    super.cx[i][j] = POSITIVE_VOTE;
                else
                    super.cx[i][j] = NEGATIVE_VOTE;
            }
        }
    }
    
    public boolean isNodeVotedIn(Object mergeKey) {
        return getNodeCount(mergeKey) >= aggregateNodeThreshold;
    }
    
    public boolean isLinkVotedIn(Object headKey, Object tailKey) {
        return super.getConnection(headKey, tailKey) == POSITIVE_VOTE;
    }
    
    /** if the alpha [head,tail] relationship was voted yes over threshold, zero out the omega
     * [tail,head] so we can't vote for this head/tail relationship again */
    public boolean testAndConsumeOppositeLinkVote(Object headKey, Object tailKey) {
        final int ihead = super.keys.findIndex(headKey);
        final int itail = super.keys.findIndex(tailKey);
        if (ihead < 0 || itail < 0)
            return false;
        
        if (super.cx[ihead][itail] > 0) {
            // reverse the index order and consume the opposite direction link vote
            super.cx[itail][ihead] = 0;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param headKey label/merge property of head endpoint node
     * @param tailKey label/merge property of tail endpoint node
     * @return number of links between head and tail (based merge-keys)
     */
    public int getLinkCount(Object headKey, Object tailKey) {
        final int headIndex = keys.findIndex(headKey);
        final int tailIndex = keys.findIndex(tailKey);
        if (headIndex >= 0 && tailIndex >= 0)
            return linkCounts[headIndex][tailIndex];
        else
            return 0;
    }

    
    // /*
    //  * This method is similar to isNodeVoteAboveThreshold except it works for links
    //  * @param label1 label/merge property of node 1
    //  * @param label2 label/merge property of node 2
    //  * @return true when number of links is above threshold
    //  */
    // public boolean isLinkVoteAboveThreshold(Object headKey, Object tailKey) {
    //     return super.getConnection(headKey, tailKey) > 0;
    //     // This is exactly what was already computed in computeVotes!
    //     // final int linkCount = getLinkCount(headKey, tailKey);
    //     // return linkCount > 0 && linkCount >= aggregateLinkThreshold;
    // }
    // Public void setLinkThreshold(double percentage) {
    //     linkThreshold = percentage;
    //     if (DEBUG.Enabled) Log.debug("linkThresh set to " + percentage, new Throwable("HERE"));
    // }
    // public void setNodeThreshold(double percentage) {
    //     nodeThreshold = percentage;
    //     if (DEBUG.Enabled) Log.debug("nodeThresh set to " + percentage, new Throwable("HERE"));
    // }
    // public VoteAggregate(List<ConnectivityMatrix> matrices,boolean nodeMerge) { super(matrices); }    
}
