/*
 * DatasetAction.java
 *
 * Created on August 6, 2008, 12:05 PM
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


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import tufts.vue.*;

public class DatasetAction  extends VueAction {
    public static final String LABEL = "Import Dataset";
    /** Creates a new instance of DatasetAction */
    public DatasetAction() {
        super(LABEL);
    }
    private static  final Object LOCK = new Object();
    private static boolean openUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
            VUE.activateWaitCursor();
            File file = tufts.vue.action.ActionUtil.openFile("Open Map", "text");
            DatasetLoader dsl = new DatasetLoader();
            Dataset ds = dsl.load(file);
            LWMap loadedMap = ds.createMap();
            VUE.displayMap(loadedMap);
            VUE.clearWaitCursor();
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            openUnderway = false;
        }
    }
}
