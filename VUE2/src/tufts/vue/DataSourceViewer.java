/*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * DataSourceViewer.java
 *
 * Created on October 15, 2003, 1:03 PM
 */

package tufts.vue;
/**
 *
 * @author  akumar03
 */

import tufts.vue.gui.VueButton;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;
import java.io.*;
import java.util.*;
import java.net.URL;


// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;



//

public class DataSourceViewer  extends JPanel implements KeyListener{
    /** Creates a new instance of DataSourceViewer */
    
    
    
    static DRBrowser drBrowser;
    static DataSource activeDataSource;
    static JPanel resourcesPanel,dataSourcePanel;
    String breakTag = "";
    
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT = VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    
    
    
    JPopupMenu popup;       // add edit popup
    AddEditDataSourceDialog addEditDialog = null;   //  The add/edit dialog box.
    AbstractAction addAction;//
    AbstractAction editAction;
    AbstractAction deleteAction;
    AbstractAction saveAction;
    AbstractAction refreshAction;
    
    
    
    
    
    public static Vector  allDataSources = new Vector();
    
    
    public static DataSourceList dataSourceList;
    
    public DataSourceViewer(DRBrowser drBrowser){
        
        
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Data Source"));
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        
        dataSourceList = new DataSourceList(this);
        dataSourceList.addKeyListener(this);
        
        
        loadDataSources();
        
        // if (loadingFromFile)dataSourceChanged = false;
        setPopup();
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                
                Object o = ((JList)e.getSource()).getSelectedValue();
                if (o !=null){
                    
                    if (!(o instanceof String)){
                        DataSourceViewer.this.setActiveDataSource((DataSource)o);
                    }
                    else{
                        
                        int index = ((JList)e.getSource()).getSelectedIndex();
                        DataSourceViewer.this.setActiveDataSource((DataSource)(dataSourceList.getContents().getElementAt(index-1)));
                        
                    }
                }
                
            }}
        );
        dataSourceList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == e.BUTTON3) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        
        // GRID: addConditionButton
        JButton addButton=new VueButton("add");
        addButton.setBackground(this.getBackground());
        addButton.setToolTipText("Add new or edit data source");
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                
            }
        });
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new VueButton("delete");
        deleteButton.setBackground(this.getBackground());
        deleteButton.setToolTipText("Delete data source");
        
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteDataSource(activeDataSource);
                
                refreshDataSourceList();
                if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
                else{
                    DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                    DataSourceViewer.this.resourcesPanel  = new JPanel();
                    DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                    DataSourceViewer.this.drBrowser.repaint();
                    DataSourceViewer.this.drBrowser.validate();
                }
            }
        });
        
        
        // GRID: addConditionButton
        
        
        JButton refreshButton=new VueButton("refresh");
        
        refreshButton.setBackground(this.getBackground());
        refreshButton.setToolTipText("Refresh data sources");
        
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    activeDataSource.setResourceViewer();
                } catch(Exception ex){
                    if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
                }
                refreshDataSourceList();
                
            }
        });
        
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("This panel lists data sources currently availabe to VUE. Use the data source panel buttons to edit, delete  or create new data sources");
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        
        
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);
        topPanel.add(questionLabel);
        
        
        
        dataSourcePanel = new JPanel();
        dataSourcePanel.setLayout(new BorderLayout());
        dataSourcePanel.add(topPanel,BorderLayout.NORTH);
        
        
        JScrollPane dataJSP = new JScrollPane(dataSourceList);
        dataSourcePanel.add(dataJSP,BorderLayout.CENTER);
        add(dataSourcePanel,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        
        
        
    }
    
    public static void addDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else type = 6;
        
        Vector dataSourceVector = (Vector)allDataSources.get(type);
        dataSourceVector.add(ds);
        refreshDataSourceList();
        saveDataSourceViewer();
    }
    
    public void deleteDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else type = 6;
        if(VueUtil.confirm(this,"Are you sure you want to delete DataSource :"+ds.getDisplayName(),"Delete DataSource Confirmation") == JOptionPane.OK_OPTION) {
            Vector dataSourceVector = (Vector)allDataSources.get(type);
            dataSourceVector.removeElement(ds);
        }
        saveDataSourceViewer();
        
    }
    
    public static void refreshDataSourceList(){
        int i =0; Vector dsVector;
        String breakTag = "";
        int NOOFTYPES = 6;
        if (!(dataSourceList.getContents().isEmpty()))dataSourceList.getContents().clear();
        for (i = 0; i < NOOFTYPES; i++){
            dsVector = (Vector)allDataSources.get(i);
            if (!dsVector.isEmpty()){
                int j = 0;
                for(j = 0; j < dsVector.size(); j++){
                    dataSourceList.getContents().addElement(dsVector.get(j));
                }
                boolean breakNeeded = false; int typeCount = i+1;
                while ((!breakNeeded) && (typeCount < NOOFTYPES)){
                    if (!((Vector)allDataSources.get(i)).isEmpty())breakNeeded = true;
                    typeCount++;
                }
                if (breakNeeded) dataSourceList.getContents().addElement(breakTag);
            }
        }
        dataSourceList.setSelectedValue(getActiveDataSource(),true);
        dataSourceList.validate();
    }
    
    
    public static DataSource getActiveDataSource() {
        return activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){
        
        this.activeDataSource = ds;
        
        refreshDataSourcePanel(ds);
        
        
        dataSourceList.setSelectedValue(ds,true);
        
    }
    public static void refreshDataSourcePanel(DataSource ds){
        
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        
        resourcesPanel.setBorder(new TitledBorder(ds.getDisplayName()));
        
        JPanel dsviewer = (JPanel)ds.getResourceViewer();
        resourcesPanel.add(dsviewer,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
        
        
    }
    
    public void  setPopup() {
        popup = new JPopupMenu();
        
        
        addAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        editAction = new AbstractAction("Edit") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(1);
                
                
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        deleteAction =  new AbstractAction("Delete") {
            public void actionPerformed(ActionEvent e) {
                deleteDataSource(activeDataSource);
                refreshDataSourceList();
                if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
                else{
                    DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                    DataSourceViewer.this.resourcesPanel  = new JPanel();
                    DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                    DataSourceViewer.this.drBrowser.repaint();
                    DataSourceViewer.this.drBrowser.validate();
                }
                
                
            }
        };
        
        saveAction =  new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                // saveDataSourceViewer();
            }
        };
        refreshAction =  new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refreshDataSourceList();
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
        popup.addSeparator();
        popup.add(deleteAction);
        // popup.addSeparator();
        // popup.add(saveAction);
        popup.addSeparator();
        popup.add(refreshAction);
        
    }
    private boolean checkValidUser(String userName,String password,int type) {
        if(type == 3) {
            try {
                TuftsDLAuthZ tuftsDL =  new TuftsDLAuthZ();
                osid.shared.Agent user = tuftsDL.authorizeUser(userName,password);
                if(user == null)
                    return false;
                if(tuftsDL.isAuthorized(user, TuftsDLAuthZ.AUTH_VIEW))
                    return true;
                else
                    return false;
            } catch(Exception ex) {
                VueUtil.alert(null,"DataSourceViewer.checkValidUser - Exception :" +ex, "Validation Error");
                ex.printStackTrace();
                return false;
            }
        } else
            return true;
    }
    
    public void showAddEditWindow(int mode) {
        if ((addEditDialog == null)  || true) { // always true, need to work for cases where case where the dialog already exists
            if (DEBUG.DR) System.out.println("Creating new addEditDialog...");
            addEditDialog = new AddEditDataSourceDialog();
            if (DEBUG.DR) System.out.println("Created new addEditDialog: " + addEditDialog + "; showing...");
            addEditDialog.show(mode);
            if (DEBUG.DR) System.out.println("Showed new addEditDialog: " + addEditDialog);
        }
    }
    
    
    
    
    
    
    
    public void loadDataSources(){
        
        Vector dataSource0 = new Vector();
        Vector dataSource1 = new Vector();
        Vector dataSource2 = new Vector();
        Vector dataSource3 = new Vector();
        Vector dataSource4 = new Vector();
        Vector dataSource5 = new Vector();
        
        allDataSources.add(dataSource0);
        allDataSources.add(dataSource1);
        allDataSources.add(dataSource2);
        allDataSources.add(dataSource3);
        allDataSources.add(dataSource4);
        allDataSources.add(dataSource5);
        
        boolean init = true;
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        
        int type;
        try{
            SaveDataSourceViewer rViewer = unMarshallMap(f);
            Vector rsources = rViewer.getSaveDataSources();
            while (!(rsources.isEmpty())){
                DataSource ds = (DataSource)rsources.remove(0);
                ds.setResourceViewer();
                System.out.println(ds.getDisplayName()+ds.getClass());
                try {
                    addDataSource(ds);
                    setActiveDataSource(ds);
                } catch(Exception ex) {System.out.println("this is a problem in restoring the datasources");}
            }
            saveDataSourceViewer();
            refreshDataSourceList();
        }catch (Exception ex) {
            if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
            //VueUtil.alert(null,"Previously saved datasources file does not exist or cannot be read. Adding Default Datasources","Loading Datasources");
            loadDefaultDataSources();
        }
        refreshDataSourceList();
    }
    
    /**
     *If load datasources fails this method is called
     */
    
    private void loadDefaultDataSources() {
        try {
            DataSource ds1 = new LocalFileDataSource("My Computer","");
            addDataSource(ds1);
            DataSource ds2 = new FavoritesDataSource("My Favorites");
            addDataSource(ds2);
            DataSource ds3 = new FedoraDataSource("Tufts Digital Library","snowflake.lib.tufts.edu", "test","test");
            addDataSource(ds3);
            DataSource ds4 = new GoogleDataSource("Tufts Web","http://googlesearch.tufts.edu","tufts01","tufts01");
            addDataSource(ds4);
            saveDataSourceViewer();
            setActiveDataSource(ds1);
        } catch(Exception ex) {
            if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
        }
        
    }
    /*
     * static method that returns all the datasource where Maps can be published.
     * Only FEDORA @ Tufts is available at present
     */
    public static Vector getPublishableDataSources(int i) {
        Vector mDataSources = new Vector();
        /**
         * try {
         * mDataSources.add(new FedoraDataSource("Tufts Digital Library","vue-dl.tccs.tufts.edu","test","test"));
         *
         * } catch (Exception ex) {
         * System.out.println("Datasources can't be loaded");
         * }
         **/
        
        Enumeration e = dataSourceList.getContents().elements();
        while(e.hasMoreElements() ) {
            Object mDataSource = e.nextElement();
            if(mDataSource instanceof Publishable)
                mDataSources.add(mDataSource);
        }
        
        return mDataSources;
        
    }
    
    public static void saveDataSourceViewer() {
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        Vector sDataSources = new Vector();
        int size = dataSourceList.getModel().getSize();
        for (int i = 0; i< size; i++) {
            Object item = dataSourceList.getModel().getElementAt(i);
            if (DEBUG.DR) System.out.println("saveDataSourceViewer: item " + i + " is " + item.getClass().getName() + "[" + item + "]");
            if (item instanceof DataSource) {
                sDataSources.add((DataSource)item);
            } else {
                if (DEBUG.DR) System.out.println("\tskipped item of " + item.getClass());
            }
            
        }
        if (DEBUG.DR) System.out.println("saveDataSourceViewer: creating new SaveDataSourceViewer");
        SaveDataSourceViewer sViewer= new SaveDataSourceViewer(sDataSources);
        if (DEBUG.DR) System.out.println("saveDataSourceViewer: marshallMap: saving " + sViewer + " to " + f);
        marshallMap(f,sViewer);
    }
    
    
    public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
        Marshaller marshaller = null;
        Mapping mapping = new Mapping();
        
        try {
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: loading mapping " + XML_MAPPING_DEFAULT);
            mapping.loadMapping(XML_MAPPING_DEFAULT);
            marshaller.setMapping(mapping);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: marshalling " + dataSourceViewer + " to " + file + "...");
            marshaller.marshal(dataSourceViewer);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: done marshalling.");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            System.err.println("DRBrowser.marshallMap " + e);
        }
    }
    
    
    public  SaveDataSourceViewer unMarshallMap(File file) throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException{
        Unmarshaller unmarshaller = null;
        SaveDataSourceViewer sviewer = null;
        
        
        
        Mapping mapping = new Mapping();
        
        
        unmarshaller = new Unmarshaller();
        mapping.loadMapping(XML_MAPPING_DEFAULT);
        unmarshaller.setMapping(mapping);
        
        FileReader reader = new FileReader(file);
        
        sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
        
        
        reader.close();
        
        
        
        return sviewer;
    }
    
    
    
    
    
    
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
    
    
}
