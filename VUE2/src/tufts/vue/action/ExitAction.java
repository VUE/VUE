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
import javax.swing.tree.TreePath;
import java.io.*;
import tufts.vue.*;
import java.util.Vector;


public class ExitAction extends AbstractAction {
    
    private static java.util.prefs.Preferences prefs = tufts.vue.VUE.prefs;
    private static String  FAVORITES_MAPPING;
    private static String  DATASOURCES_MAPPING;
    
    /** Creates a new instance of exitAction */
    public ExitAction() {
    }
    
    public ExitAction(String label) {
        super(label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        exitVue();
    }
    
    public static void exitVue()
    {
        if (tufts.vue.VUE.favoritesWindow != null) {
            //Saving favorites
            
            tufts.vue.VueDandDTree ft =  tufts.vue.VUE.favoritesWindow.getFavoritesTree();
            tufts.vue.SaveVueJTree sfavtree = new tufts.vue.SaveVueJTree(ft);
                        
            try {
          
                FAVORITES_MAPPING = prefs.get("mapping.favorites","") ;
            }catch(Exception e) { System.out.println("Favorites"+e);}
        
                         
            File favf  = new File(FAVORITES_MAPPING);
            FavoritesWindow.marshallMap(favf,sfavtree);
            System.out.println("Favorites Saved");
        }

        if (tufts.vue.VUE.dataSourceViewer != null) {
            //Saving Datasources
            try {
                DATASOURCES_MAPPING = prefs.get("mapping.datasources","") ;
            }catch(Exception e) { System.out.println("datasources"+e);}
        
            File dsf  = new File(DATASOURCES_MAPPING);
            Vector sdataSources = tufts.vue.VUE.dataSourceViewer.getDataSources();
            tufts.vue.SaveDataSourceViewer sViewer= new tufts.vue.SaveDataSourceViewer(sdataSources);
             
            tufts.vue.DataSourceViewer.marshallMap(dsf,sViewer);
                         
            System.out.println("Datasources Saved");
        }
        
        System.exit(0);
    }
    
}
