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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;

public class DataSourceViewer  extends JPanel{
    /** Creates a new instance of DataSourceViewer */
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
    java.util.Vector dataSources;
    DataSource activeDataSource;
    DRBrowser drBrowser;
    JPopupMenu popup;
    JList dataSourceList;
    JPanel resourcesPanel;

    public DataSourceViewer(DRBrowser drBrowser) {
        
       // super("DataSource",true,true,true);// incase we dicide to use JInternalFrame
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        
        dataSources = new java.util.Vector();
        setPopup();
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        loadDataSources();
        dataSourceList = new JList(dataSources);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,Object value, int index, boolean iss,boolean chf)   {
                super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);
                setIcon(new PolygonIcon(((DataSource)value).getDisplayColor()));
                return this;
            }
        };
        dataSourceList.setCellRenderer(renderer);       
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
               DataSourceViewer.this.setActiveDataSource(((DataSource)((JList)e.getSource()).getSelectedValue()));
            }
        });
        /**
       dataSourceList.addMouseListener(new MouseAdapter() {
           public void mouseClicked(MouseEvent e) {
               if(e.getButton() == e.BUTTON3) {
                  popup.show(e.getComponent(), e.getX(), e.getY());
               }
           }
       });
        **/
        JScrollPane jSP = new JScrollPane(dataSourceList);
        add(jSP,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        //setSize(jSP.getPreferredSize());
        
    }
    
    public DataSource getActiveDataSource() {
        return this.activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){ 
        activeDataSource = ds;
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        resourcesPanel.setBorder(new TitledBorder(activeDataSource.getDisplayName()));
        resourcesPanel.add(activeDataSource.getResourceViewer(),BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
    }
    public void  setPopup() {
        popup = new JPopupMenu();
        AbstractAction addAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        AbstractAction editAction = new AbstractAction("Edit") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(1);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
    }
    
    public void showAddEditWindow(int mode) {
     JDialog dialog = new JDialog();
     JTabbedPane tabbedPane = new JTabbedPane();
     JPanel addPanel = new JPanel();
     JPanel editPanel = new JPanel();
     tabbedPane.add("Add", addPanel);
     tabbedPane.add("Edit",editPanel);
     tabbedPane.setSelectedComponent(addPanel);
     if(mode == EDIT_MODE) 
         tabbedPane.setSelectedComponent(editPanel);
     dialog.getContentPane().add(tabbedPane);
     dialog.pack();
     dialog.setLocation(300,300);
     dialog.show();
    }
            
        
            
    private void loadDataSources() {
         // this should be created automatically from a config file. That will be done in future.
        DataSource dsMyComputer = new DataSource("ds1", "My Computer", "My Computer");
        DataSource dsTuftsDL = new DataSource("ds2", "Tufts Digital Library","fedora");
        DataSource dsMyFavorites = new DataSource("ds3", "My Favorites","favorites");
        DataSource ds4 = new DataSource("ds4", "Local FileSystem","favorites");
        dsTuftsDL.setDisplayColor(Color.RED);
        dsTuftsDL.setResourceViewer(new DRViewer("fedora.conf", "Fedora:1","DR@Tufts","Tufts Fedora Repository"));
        dsMyFavorites.setResourceViewer(new FavoritesWindow("My Favorites"));
        dsMyFavorites.setDisplayColor(Color.BLUE);
        Vector fileVector  = new Vector();
        fileVector.add(new File("C:\\"));
        VueDragTree fileTree = new VueDragTree(fileVector.iterator(),"File System");
        JScrollPane jSP = new JScrollPane(fileTree);   
        dsMyComputer.setResourceViewer(jSP);
        //ds4.setResourceViewer(jSP);
        dataSources.add(dsMyComputer);
        dataSources.add(dsTuftsDL);
        dataSources.add(dsMyFavorites);
        //dataSources.add(ds4);
        setActiveDataSource(dsTuftsDL);
        //drBrowser.add(dsMyComputer.getResourceViewer(),BorderLayout.CENTER);
        //drBrowser.add(dsMyComputer.getResourceViewer(),BorderLayout.SOUTH);
    }
}
