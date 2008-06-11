/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
 * The class creates a connectivity Matrix using a VUE Map.  Further information
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
    public static final int TRUNCATE_LENGTH = 8;
    protected List labels = new  ArrayList(); // these labels need not be node labels
    protected int c[][] = new int[SIZE][SIZE];
    protected int size;
    private LWMap map;
    private static final boolean DEBUG = false;
    
    public ConnectivityMatrix() {
    }
    
    public ConnectivityMatrix(LWMap map) {
        size = 0;
        this.map = map;
        addLabels();
        generateMatrix();
    }
    /*
     *The method adds labels to the connectivity matrix
     *
     */
    
    private void addLabels(){
        Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext()){
            Object o = i.next();
            if(o instanceof LWNode || o instanceof LWImage) {
                //LWNode node = (LWNode) o;
                if(!labels.contains(getMergeProperty((LWComponent)o)))
                {    
                  labels.add(getMergeProperty((LWComponent)o));
                }
                size++;
            }
        }
    }
    /*
     * creates a matrix from the map.
     */
    private void generateMatrix() {
        Iterator links = map.getLinkIterator();
        while(links.hasNext()) {
            LWLink link = (LWLink)links.next();
            LWComponent n1 = link.getHead();
            LWComponent n2 = link.getTail();
            int arrowState = link.getArrowState();
            if( (n1  instanceof LWNode || n1 instanceof LWImage) && ( n2 instanceof LWNode || n2 instanceof LWImage) ) {
                try {
                    if(arrowState == LWLink.ARROW_BOTH || arrowState == LWLink.ARROW_NONE) {
                        c[labels.indexOf(getMergeProperty(n2))][labels.indexOf(getMergeProperty(n1))] = 1;
                        c[labels.indexOf(getMergeProperty(n1))][labels.indexOf(getMergeProperty(n2))] =1;
                    } else if(arrowState == LWLink.ARROW_HEAD) { // EP1 and EP2 were deprecated.
                        c[labels.indexOf(getMergeProperty(n2))][labels.indexOf(getMergeProperty(n1))] = 1;
                    } else    if(arrowState == LWLink.ARROW_TAIL) { // EP1 and EP2 were deprecated.
                        c[labels.indexOf(getMergeProperty(n1))][labels.indexOf(getMergeProperty(n2))] =1;
                    }
                } catch(ArrayIndexOutOfBoundsException ae) {
                    System.out.println("Connectivity Matrix Exception - skipping link: " + link);
                    System.out.println("Exception was: " + ae);
                }
            }
            
        }
    }
    public List getLabels() {
        return labels;
    }
    
    public void setLabels(List labels){
        this.labels = labels;
    }
    
    public int getConnection(int i, int j) {
        return c[i][j];
    }
    
    public int getConnection(String label1,String label2) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            return c[index1][index2];
        } else {
            return 0;
        }
    }
    
    public void setConnection(String label1, String label2, int value) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            c[index1][index2] = value;
        }
    }
    public int getSize(){
        return size;
    }
    public int[][] getMatrix() {
        return c;
    }
    public void setMatrix(int[][] c) {
        this.c = c;
    }
    
    public LWMap getMap() {
        return this.map;
    }
    public void store(OutputStream out) {
        try {
            out.write(this.toString().getBytes());
        }catch(IOException ex) {
            System.out.println("ConnectivityMatrix.store:"+ex);
        }
    }
    /**
     * Compares a connectivity matrix to input connectivity matrix
     * returns truf if both are same
     * @param c2 ConnectivityMatrix to be compared to
     * @return boolean
     *
     */
    
    public boolean compare(ConnectivityMatrix c2) {
        if(c2.getSize() != size) {
            return false;
        }
        for(int i=0;i<size;i++) {
            for(int j=0;j<size;j++) {
                if(c[i][j] != c2.getMatrix()[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public String toString() {
        String output = new String();
        // removed the first label from the output. to add it uncomment the commented lines in this function
        //       output = "\t";   //leave the first cell empty;
        Iterator iterator = labels.iterator();
        while(iterator.hasNext()){
            String label = (String)iterator.next();
            
            int endIndex = Math.min(TRUNCATE_LENGTH,label.length());
            if(endIndex != 0)
            {
              output += label.substring(0,endIndex)+"\t";
            }
            else
            {
              output += "\t";
            }
        }
        output += System.getProperty("line.separator");
        for(int i=0;i<size;i++){
//            output += labels.get(i)+"\t";
            for(int j=0;j<size;j++) {
                output  += c[i][j]+"\t";
            }
            output +=System.getProperty("line.separator");
        }
        return output;
    }
    
    private String getMergeProperty(LWComponent node) {
        
        if(DEBUG)
        {
          System.out.println("Connectivity Matrix - getMergeProperty: " + Util.getMergeProperty(node));
        }
        
        return  Util.getMergeProperty(node);
    }
}
