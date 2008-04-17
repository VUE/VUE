/*
 * TestConnectivityMatrix.java
 *
 * Created on October 6, 2006, 2:31 PM
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


import junit.framework.TestCase;

import java.io.*;
import java.net.*;
import tufts.vue.*;

public class TestConnectivityMatrix extends TestCase {
    
 public void testConnectivityMatrixCreation() {
     tufts.vue.gui.GUI.init();
     LWMap map = edu.tufts.vue.compare.Util.getMap();
     ConnectivityMatrix matrix = new ConnectivityMatrix(map);
     
 }
 
 public void testConnectivityMatrixSave() {
     tufts.vue.gui.GUI.init();
     LWMap map = edu.tufts.vue.compare.Util.getMap();
     ConnectivityMatrix matrix = new ConnectivityMatrix(map);
     URL url = edu.tufts.vue.TestResources.getURL("ConnectivityMatrixTest");
     String fileName = url.getFile();
     try
     {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        pw.write(matrix.toString());
        pw.close();
     }
     catch(Exception ex)
     {
        ex.printStackTrace();
     }
     
     String matrix2 = "";
     
     try
     {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = "";
        while(line != null)
        {
           line = br.readLine();
           if (matrix2!="" && line != null)
             matrix2 += "\n" + line;
           else
           if(line!= null)
             matrix2 += line;
        }
     }
     catch(Exception ex)
     {
        ex.printStackTrace();
     }
     
     String saved = matrix.toString().trim();
     String readFromFile = matrix2.toString().trim();
     
     if(!saved.equals(readFromFile))
     {
         System.out.println("Matrix Save Test: matrices not equal");
     }
     
 }
 
}

