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
        if (!VUE.isOkayToExit())
            return;

        try {
            VUE.getInstance().hide();
            System.out.println("Saving user preferences...");
            saveDataSourceInfo();
            System.out.println("Saved user preferences.");
        } catch (Throwable t) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            System.err.println("Error exiting view: " + t);
            t.printStackTrace();
        } finally {
            System.out.println("Exiting VUE.");
            System.exit(0);
        }
    }

    private static void saveDataSourceInfo()
    {
        DataSource ds;
        ListModel model = tufts.vue.DataSourceViewer.dataSourceList.getModel();
        int i;
         
        for (i =0 ; i< model.getSize(); i++){
        
            ds = (DataSource)model.getElementAt(i);
         
            if (ds.getType() == DataSource.FAVORITES){
            
                FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();              
                if (fw.favoritesTree != null)  {
                    //Saving favorites
            
                    tufts.vue.VueDandDTree ft = ((FavoritesWindow)ds.getResourceViewer()).getFavoritesTree();
                    ft.setRootVisible(true);
                    System.out.println("This is tree" + (ft.getModel()).getRoot());
                    tufts.vue.SaveVueJTree sfavtree = new tufts.vue.SaveVueJTree(ft);
                    File favf  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+ds.getDisplayName()+VueResources.getString("save.favorites"));
                    ((FavoritesWindow)ds.getResourceViewer()).marshallMap(favf,sfavtree);
                    System.out.println("Favorites Saved"+ds.getDisplayName());
                }
            }
        
        }
        tufts.vue.DataSourceViewer.saveDataSourceViewer();
    }
    
}
