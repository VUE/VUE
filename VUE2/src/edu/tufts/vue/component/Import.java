/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * Import.java
 *
 * Created on Feb 20, 2009
 *
 * Copyright 2003-2009 Tufts University  Licensed under the
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
package edu.tufts.vue.component;

import tufts.vue.ds.*;
import edu.tufts.vue.dataset.Dataset;
import edu.tufts.vue.layout.*;
import tufts.vue.action.SaveAction;
import tufts.vue.action.ActionUtil;
import tufts.vue.*;
import java.io.File;

public class Import {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) throws Exception {
        String inputFile = args[0];
        String outputFile = args[1];
        Dataset ds = new Dataset();
        ds.setFileName(inputFile);
        ds.loadDataset();
        Layout layout = new ListRandomLayout();
        LWMap map = layout.createMap(ds, "ClubZora");
        map.setFile(new File(outputFile));
        ActionUtil.marshallMap(new File(outputFile), map);


    }
}
