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

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.compare;

import java.util.*;

public class VoteAggregate extends WeightAggregate {
    public static final int POSITIVE_VOTE = 1;
    public static final int NEGATIVE_VOTE = 0;
    public static final double DEFAULT_THRESHOLD =  .5;
    private double nodeThreshold = DEFAULT_THRESHOLD;
    private double linkThreshold = DEFAULT_THRESHOLD;
    
    protected int[][] linkCounts = new int[SIZE][SIZE];
    
    /** Creates a new instance of VotingAggregate */
    public VoteAggregate(List<ConnectivityMatrix> matrices ){
        super(matrices);
        for(int i=0;i<SIZE;i++)
            for(int j=0;j<SIZE;j++)
                linkCounts[i][j] = c[i][j];
        computeVotes();
    }
    
    /**
     *
     * @param matrices
     * @param nodeMerge
     */
    public VoteAggregate(List<ConnectivityMatrix> matrices,boolean nodeMerge) {
        super(matrices);
    }
    /*
     * computes votes
     */
    private void computeVotes() {
        int count = getCount();
        double threshold = (double)count*linkThreshold;
        for(int i=0; i<size;i++){
            for(int j=0;j<size;j++) {
                if(c[i][j] >= threshold) {
                    c[i][j] = POSITIVE_VOTE;
                } else {
                    c[i][j] = NEGATIVE_VOTE;
                }
            }
        }
    }
    
    /*
     * returns true if a node/label ia above the threshold
     * @param label the label/merge property of the nodes
     * @return  true if the node occurs above certain threshold
     */
    
    public boolean isNodeVoteAboveThreshold(String label) {
        int nodeCount = getNodeCount(label);
        int count = getCount();
        double threshold = (double)count*nodeThreshold;
        if(nodeCount>=threshold) {
            return true;
        }else
            return false;
    }
   /*
    * returns the nunber of links between two nodes
    * @param label1 label/merge property of node 1
    * @param label2 label/merge property of node 2
    * @return number of links between node 1 and node 2
    */
            
    public int getLinkCount(String label1,String label2) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            return linkCounts[index1][index2];
        } else {
            return 0;
        }
    }
    
    /*
     * This method is similar to isNodeVoteAboveThreshold except it works for links
     * @param label1 label/merge property of node 1
     * @param label2 label/merge property of node 2
     * @return true when number of links is above threshold
     */
    
    public boolean isLinkVoteAboveThreshold(String label1,String label2) {
        int linkCount = getLinkCount(label1,label2);
        int count = getCount();
        double threshold = (double)count*linkThreshold;
        if( (linkCount>=threshold) && (linkCount > 0) ) {
            return true;
        } else {
            return false;
        }
    }
    
    public void setLinkThreshold(double percentage) {
        linkThreshold = percentage;
    }
    
    public void setNodeThreshold(double percentage) {
        nodeThreshold = percentage;
    }
}