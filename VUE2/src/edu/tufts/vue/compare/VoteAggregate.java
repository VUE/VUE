/*
 * VotingAggregate.java
 *
 * Created on October 13, 2006, 5:41 PM
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
     
    public VoteAggregate(List<ConnectivityMatrix> matrices,boolean nodeMerge) {
        super(matrices);
    } 
    
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
    
    public boolean isNodeVoteAboveThreshold(String label) {
        int nodeCount = getNodeCount(label);
        int count = getCount();
        double threshold = (double)count*nodeThreshold;
        if(nodeCount>=threshold) {
            return true;
        }else 
            return false;
    }
    
    public int getLinkCount(String label1,String label2) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            return linkCounts[index1][index2];
        } else {
            return 0;
        }
    }
    
    public boolean isLinkVoteAboveThreshold(String label1,String label2)
    {
        
        //System.out.println("-----------\n Is link vote above threshold");
        
        int linkCount = getLinkCount(label1,label2);
        //System.out.println("ilvat: linkCount " + linkCount);
        int count = getCount();
        //System.out.println("ilvat: count: " + count);
        double threshold = (double)count*linkThreshold;
        //System.out.println("ilvat: threshold -- " + threshold + " for: " + label1 + " | " + label2);
        //System.out.println("ilvat: active viewer: " + tufts.vue.VUE.getActiveViewer() + " - active map: " + tufts.vue.VUE.getActiveMap());
        
        
        if( (linkCount>=threshold) && (linkCount > 0) ) {
            //System.out.println("ilvat: returning true \n -------------");
            return true;
        }
        else
        {    
            //System.out.println("ilvat: returning false \n -------------");
            return false;
        }
    }
    
    public void setLinkThreshold(double percentage)
    {
        linkThreshold = percentage;
    }
    
    public void setNodeThreshold(double percentage)
    {
        nodeThreshold = percentage;
    }
}
