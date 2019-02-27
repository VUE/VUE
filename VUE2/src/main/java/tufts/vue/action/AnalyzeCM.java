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


package tufts.vue.action;

import tufts.vue.*;
import edu.tufts.vue.compare.*;

import java.io.*;
import java.util.*;

import javax.swing.*;
import java.awt.event.*;


public class AnalyzeCM extends VueAction {
    
    //private MergeMapsChooser mmc = null;
    
    private tufts.vue.gui.DockWindow w = null;
    
    /** Creates a new instance of AnalyzeCM */
    public AnalyzeCM(String label) {
        super(label);
    }
    
    public void actionPerformed(ActionEvent e) {
// Functionality moved to MergeMapsChooser Dialog and LWMergeMap 1/26/2007
/*        try {
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
            LWMap aggregate = new LWMergeMap("Vote Aggregate");
            Iterator children = referenceMap.getNodeIterator();
            while(children.hasNext()) {
                LWComponent comp = (LWComponent)children.next();
              //  System.out.print("Label: "+comp.getLabel()+" vote:"+voteAggregate.isNodeVoteAboveThreshold(comp.getLabel()));
                if(voteAggregate.isNodeVoteAboveThreshold(comp.getLabel())) {
                    LWNode node = (LWNode)comp.duplicate();
                   aggregate.addNode(node);
               }
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
        }*/
        
        
        final int MMC_VERSION = 1;
        
        if(MMC_VERSION == 1)
        {
          if(w==null)
          {
            w = tufts.vue.gui.GUI.createDockWindow(VueResources.getString("dialog.mergemap.title"));
            w.setLocation(200,200);
            edu.tufts.vue.compare.ui.MergeMapsControlPanel mmcp = new edu.tufts.vue.compare.ui.MergeMapsControlPanel(w);
          }
          else
            w.setVisible(true);
        }
        
        // if(MMC_VERSION == 0)
        // {
        //   tufts.vue.gui.DockWindow w = MergeMapsChooser.getDockWindow();
        //   if(w==null)
        //   {
        //    mmc = new MergeMapsChooser();
        //    w = tufts.vue.gui.GUI.createDockWindow(VueResources.getString("dialog.mergemap.title"),mmc);
        //    MergeMapsChooser.setDockWindow(w); 
        //    //$
        //          //MergeMapsChooser.loadDefaultStyle();
        //          //mmc.refreshSettings();
        //    //$
        //   }
        //   if(!w.isVisible())
        //   {
        //     if(!(VUE.getActiveMap() instanceof LWMergeMap))
        //     MergeMapsChooser.loadDefaultStyle();
        //     mmc.refreshSettings();
        //     w.setLocation(200,200);
        //     w.pack();
        //     w.setVisible(true);
        //   } 
        // }
                
    }
    
    
}
