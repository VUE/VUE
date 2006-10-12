/*
 * ConnectivityMatrix.java
 *
 * Created on September 13, 2006, 11:13 AM
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
 * The class creates a connectivity Matrix using a VUe Map.  Further information
 * on connectivity matrix can be found at
 * http://w3.antd.nist.gov/wctg/netanal/netanal_netmodels.html 
 * The matrix can be used to assess the connetiving among give set of nodes.
 * A value of connetion is 1 if there is a connection between nodes
 * connection(a,b) = 1 implies there is a link from a to b
 * connection(b,a) = 1 implies there is a link from b to a
 * connection(b,a) may not be equal to connection(a,b)
 * connection(a,b) = connection(b,a) implies the link between a and b is not 
 * directed.
 */

package edu.tufts.vue.compare;

import java.util.*;
import java.io.*;
import tufts.vue.*;


public class ConnectivityMatrix {
    public static final int SIZE = 1000;
    private List labels = new  ArrayList();
    private int c[][] = new int[SIZE][SIZE];
    private int size;
    /** Creates a new instance of ConnectivityMatrix */
    public ConnectivityMatrix() {
    }
    
    public ConnectivityMatrix(LWMap map) {
        size = 0;
        Iterator i = map.getNodeIterator();
        while(i.hasNext()){
            LWNode node = (LWNode)i.next();
            labels.add(node.getLabel());
            size++;
        }
        Iterator links = map.getLinkIterator();
        while(links.hasNext()) {
            LWLink link = (LWLink)links.next();
            LWComponent n1 = link.getComponent1();
            LWComponent n2 = link.getComponent2();
            int arrowState = link.getArrowState();
            if(n1  instanceof LWNode && n2 instanceof LWNode) {
                if(arrowState == LWLink.ARROW_BOTH || arrowState == LWLink.ARROW_NONE) {
                    c[labels.indexOf(n2.getLabel())][labels.indexOf(n1.getLabel())] = 1;
                    c[labels.indexOf(n1.getLabel())][labels.indexOf(n2.getLabel())] =1;
                } else if(arrowState == LWLink.ARROW_EP1) {
                    c[labels.indexOf(n2.getLabel())][labels.indexOf(n1.getLabel())] = 1;
                } else    if(arrowState == LWLink.ARROW_EP2) {
                    c[labels.indexOf(n1.getLabel())][labels.indexOf(n2.getLabel())] =1;
                }
                
            }
        }
        
    }
    public List getLabels() {
        return labels;
    }
    public int getConnection(int i, int j) {
        return c[i][j];
    }
    public int getSize(){
        return size;
    }
    public int[][] getMatrix() {
        return c;
    }
    
    public void store(OutputStream out) {
        out.write(this.toString().getBytes());
    }
    
    
    public String toString() { 
        String output = new String();
        output = "\t";   //leave the first cell empty;
        Iterator iterator = labels.iterator();
        while(iterator.hasNext()){
            output += (String)iterator.next()+"\t";
        }
        output +="\n";
        for(int i=0;i<size;i++){
            output += labels.get(i)+"\t";
            for(int j=0;j<size;j++) {
                output  += c[i][j]+"\t";
            }
            output +="\n";
        }
        return output;
    }
}
