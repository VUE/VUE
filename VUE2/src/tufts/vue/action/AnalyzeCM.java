/*
 * AnalyzeCM.java
 *
 * Created on November 6, 2006, 11:53 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */


package tufts.vue.action;

import tufts.vue.*;
import edu.tufts.vue.compare.*;

import java.io.*;
import java.util.*;


import javax.swing.*;
import java.awt.event.*;


public class AnalyzeCM extends VueAction {
    
    /** Creates a new instance of AnalyzeCM */
    public AnalyzeCM(String label) {
        super(label);
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            ArrayList<ConnectivityMatrix> list = new ArrayList();
            LWMap referenceMap = null;
            Iterator<LWMap> i =   VUE.getLeftTabbedPane().getAllMaps();
            while(i.hasNext()) {
                LWMap map = i.next();
                if(referenceMap == null)
                    referenceMap = map;
                list.add(new ConnectivityMatrix(map));
//                System.out.println("Map:"+map.getLabel());
            }
            VoteAggregate voteAggregate = new VoteAggregate(list);
//            System.out.println(voteAggregate);
            LWMap aggregate = new LWMap("Vote Aggregate");
            Iterator children = referenceMap.getNodeIterator();
            while(children.hasNext()) {
                LWNode node = (LWNode)((LWComponent)children.next()).duplicate();
                aggregate.addNode(node);
            }
            Iterator children1 = aggregate.getNodeIterator();
            while(children1.hasNext()) {
                LWNode node1 = (LWNode)children1.next();
//                System.out.println("Processing node: "+node1.getLabel());
                Iterator children2 = aggregate.getNodeIterator();
                while(children2.hasNext()) {
                    LWNode node2 = (LWNode)children2.next();
                    if(node2 != node1) {
                        int c = voteAggregate.getConnection(node1.getLabel(),node2.getLabel());
                        if(c >0) {
                            aggregate.addLink(new LWLink(node1,node2));
//                            System.out.println("Adding Link between: "+node1.getLabel()+ " and "+ node2.getLabel());
                        }
                    }
                }
            }
            VUE.displayMap(aggregate);
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    
}
