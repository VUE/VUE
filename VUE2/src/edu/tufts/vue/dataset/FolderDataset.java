/*
 * ListDataset.java
 *
 * Created on July 23, 2008, 6:06 PM
 *
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
package edu.tufts.vue.dataset;
import java.util.*;
import java.io.*;
public class FolderDataset  extends Dataset {
    
    /** Creates a new instance of ListDataset */
    public FolderDataset() {
    }
    public  void loadDataset() throws Exception{
        // setthing the heading
        ArrayList<String> heading = new ArrayList<String>();
        heading.add("label");
        heading.add("resource");
         setHeading(heading);
       
        rowList = new ArrayList<ArrayList<String>>();
        File file = new File(fileName);
        if(!file.isDirectory()) throw new Exception("FolderDataset.loadDataset: The file " + fileName +" is not a folder");
        File[] children = file.listFiles();
        for(int i =0;i<children.length;i++) {
            ArrayList<String> row = new ArrayList<String>();
            row.add(children[i].getName());
            row.add(children[i].getAbsolutePath());
            rowList.add(row);
        }
    }
}
