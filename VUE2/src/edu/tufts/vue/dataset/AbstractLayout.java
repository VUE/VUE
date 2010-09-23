/*
 * AbstractLayout.java
 *
 * Created on July 15, 2008, 5:40 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.dataset;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;

public abstract class  AbstractLayout  extends VueAction {
    public  int MAP_SIZE = 500;
    public  int MAX_SIZE =5000;
    /** Creates a new instance of AbstractLayout */
    protected   AbstractLayout(String label) {
        super(label);
    }
    private static final Object LOCK = new Object();
    private static boolean openUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
            File file = tufts.vue.action.ActionUtil.openFile("Open Map", "text");
            displayMap(file);
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        } finally {
            openUnderway = false;
        }
    }
    
    
    public  void displayMap(File file) {
        if (file != null) {
            VUE.activateWaitCursor();
            try {
                LWMap loadedMap = loadMap(file.getAbsolutePath());
                VUE.displayMap(loadedMap);
            }catch(Throwable t) {
                t.printStackTrace();
            } finally {
                VUE.clearWaitCursor();
            }
        }
    }
    
    protected LWMap loadMap(String fileName)  throws Exception{
        System.out.println("Loading file: "+fileName);
        DatasetLoader dsLoader = new DatasetLoader();
        Dataset ds = dsLoader.load(fileName);
        
        System.out.println("Class of ds is: "+ds.getClass());
        return createMap(ds,getMapName(fileName));
        // return loadMap(fileName,getMapName(fileName));
    }
    /* creates name for map from file name
     */
    
    private  String getMapName(String fileName) {
        String mapName = fileName.substring(fileName.lastIndexOf(File.separator)+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        return mapName;
    }
    public  LWMap createMap(Dataset ds,String fileName) throws Exception  {
        return new LWMap("Dummy DS");
    }
    public  LWMap createMap(ListDataset ds,String fileName) throws Exception  {
        return new LWMap("Dummy LDS");
    }
    
    public LWMap createMap(RelationalDataset ds, String fileName) throws Exception {
        return new LWMap("Dummy RDS");
    }
    
    
    
}
