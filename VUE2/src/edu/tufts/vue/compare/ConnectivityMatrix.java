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

public class ConnectivityMatrix {
    private String[] labels;
    private int c[][];
    private int size;
    /** Creates a new instance of ConnectivityMatrix */
    public ConnectivityMatrix() {
    }
    public String[] getLabels() {
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
    public String toString() { 
        String output = new String();
        output = "\t";   //leave the first cell empty;
        for(int i=0;i<size;i++) {
            output += labels[i]+"\t";
        }
        output +="\n";
        for(int i=0;i<size;i++){
            output += labels[i]+"\t";
            for(int j=0;j<size;j++) {
                output  += c[i][j]+"\t";
            }
            output +="\n";
        }
        return output;
    }
}
