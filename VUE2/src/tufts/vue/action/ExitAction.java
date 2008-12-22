/*
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

/*
 * exitAction.java
 *
 * Created on April 30, 2003, 3:04 PM
 */

package tufts.vue.action;

/**
 *
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;
import javax.swing.JTree.*;
import javax.swing.tree.*;
import java.io.*;
import tufts.vue.*;
import tufts.vue.Actions.LWCAction;

import java.util.Vector;


public class ExitAction extends AbstractAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ExitAction.class);
    
    private static String  FAVORITES_MAPPING;
    private static String  DATASOURCES_MAPPING;
    
    /** Creates a new instance of exitAction */
    public ExitAction(String s) 
    {
    	super(s);    
    }
    
   
    public void actionPerformed(ActionEvent e) {
        exitVue();
    }
    
    public static void exitVue()
    {
        try {
            if (!VUE.isOkayToExit())
                return;
        } catch (Throwable t) {
            // any exception at this point and we've a serious problem: immediately force quit
            java.awt.Toolkit.getDefaultToolkit().beep();
            System.err.println("VUE exit exception; aborting.");
            tufts.Util.printStackTrace(t);
            System.exit(-1);
        }

        Log.info("Exiting...");
        
        try {
            if (VUE.getRootWindow() != null)
                VUE.getRootWindow().setVisible(false);
            saveResources();
//             if (false) {
//                 System.out.println("Saving user preferences...");
//                 saveDataSourceInfo();
//                 System.out.println("Saved user preferences.");
//             }
        } catch (Throwable t) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            Log.error("exception exiting view: " + t);
            t.printStackTrace();
        } finally {
            Log.info("Exiting VUE.");
            System.exit(0);
        }
    }
    
    private static void saveDataSourceInfo() {
        tufts.vue.DataSourceViewer.saveDataSourceViewer();
        DataSource ds;
        if (tufts.vue.DataSourceViewer.dataSourceList == null) {
            System.err.println("ExitAction: saveDataSourceInfo: no dataSourceList");
            return;
        }
        ListModel model = tufts.vue.DataSourceViewer.dataSourceList.getModel();
        int i;
        for (i =0 ; i< model.getSize(); i++){
            if (!(model.getElementAt(i) instanceof String)) {
                ds = (DataSource)model.getElementAt(i) ;
                if (ds instanceof FavoritesDataSource){
                    FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();
                    if (fw.favoritesTree != null)  {
                        tufts.vue.VueDandDTree ft = ((FavoritesWindow)ds.getResourceViewer()).getFavoritesTree();
                        ft.setRootVisible(true);
                        tufts.vue.SaveVueJTree sfavtree = new tufts.vue.SaveVueJTree(ft);
                        File favf  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+ds.getDisplayName()+VueResources.getString("save.favorites"));
                        ((FavoritesWindow)ds.getResourceViewer()).marshallFavorites(favf,sfavtree);
                    }
                }
            }
        }
        
    }
    
    public static void saveResources() {
        DataSource ds;
        if (tufts.vue.DataSourceViewer.dataSourceList == null) {
            System.err.println("dataSourceList is null; no resources to save.");
            return;
        }
        ListModel model = tufts.vue.DataSourceViewer.dataSourceList.getModel();
        int i;
        for (i =0 ; i< model.getSize(); i++){
            if (model.getElementAt(i) instanceof FavoritesDataSource) {
                ds = (DataSource)model.getElementAt(i) ;
                FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();
                if (fw != null)
                	fw.save();
               
            }
        }
    }
}