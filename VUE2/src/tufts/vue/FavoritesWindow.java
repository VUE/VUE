package tufts.vue;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPopupMenu;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;
import osid.dr.*;
/**
 *
 * @author  Ranjani Saigal
 */
public class FavoritesWindow extends JPanel implements ActionListener, ItemListener,KeyListener {
    private DisplayAction displayAction = null;
    public  VueDandDTree favoritesTree ;
    private JPanel searchResultsPane ;
    private JScrollPane browsePane;
    private JTabbedPane favoritesPane;
    final static String XML_MAPPING = VueResources.getURL("mapping.lw").getFile();
    private static String  FAVORITES_MAPPING;
    private static int FAVORITES = DataSource.FAVORITES;
    private static int newFavorites = 0;
    JTextField keywords;
    boolean fileOpen = false;
   
    /** Creates a new instance of HierarchyTreeWindow */
    public FavoritesWindow(String displayName ) {
        setLayout(new BorderLayout());
        
        
        favoritesPane = new JTabbedPane();
        
        
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+ displayName+VueResources.getString("save.favorites"));
       
        //System.out.println("I tried to open" + f);
     
            try {
            SaveVueJTree restorefavtree = unMarshallMap(f);
            fileOpen = true;
              favoritesTree =restorefavtree.restoreTree();
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);
            
            this.setFavoritesTree(favoritesTree);
            }catch (Exception ex){
                System.out.println("I tried to open" + f);
                fileOpen = false;
               
            }
            
           if (!fileOpen){
            //  System.out.println("Afte Unmarshalling "+restorefavtree.getClass().getName()+ " root"+ restorefavtree.getSaveTreeRoot().getResourceName());
          
    
            MapResource favResource = new MapResource(displayName);
            favResource.setType(DataSource.FAVORITES);
            
            
            FavoritesNode favRoot= new FavoritesNode(favResource);
           
            favoritesTree = new VueDandDTree(favRoot);
            
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);
            this.setFavoritesTree(favoritesTree);
            
         }
        
        
        searchResultsPane = new JPanel();
        JPanel favSearchPanel = createfavSearchPanel(favoritesTree,favoritesPane,searchResultsPane,displayName);
        
        favoritesPane.addTab("Search",favSearchPanel);
        favoritesPane.addTab("Search Results",searchResultsPane);
        browsePane = new JScrollPane(favoritesTree);
        favoritesPane.add("Browse", browsePane);
        createPopupMenu();
        favoritesPane.setSelectedIndex(2);
        
        add(favoritesPane,BorderLayout.CENTER);
        
    }
    
    
    public JPanel createfavSearchPanel(VueDandDTree favoritesTree,JTabbedPane fp,JPanel sp, String displayName){
        
        JPanel FavSearchPanel = new JPanel();
        setLayout(new BorderLayout());
        
        JPanel queryPanel =  new JPanel();
        
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        queryPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        c.fill = GridBagConstraints.HORIZONTAL;
        
        //adding the top label
        c.weightx = 1.0;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=3;
        c.ipady = 10;
        c.insets = defaultInsets;
        c.anchor = GridBagConstraints.NORTH;
        JLabel topLabel = new JLabel("");
        gridbag.setConstraints(topLabel, c);
        queryPanel.add(topLabel);
        
        
        //adding the search box and the button
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=2;
        c.ipady=0;
        keywords = new JTextField();
        keywords.setPreferredSize(new Dimension(200,20));
        keywords.addKeyListener(this);
        
        gridbag.setConstraints(keywords, c);
        queryPanel.add(keywords);
        
        c.gridx=2;
        c.gridy=1;
        c.gridwidth=1;
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80,20));
        searchButton.addActionListener(this);
        
        gridbag.setConstraints(searchButton,c);
        queryPanel.add(searchButton);
        
        
        
        
        JPanel qp = new JPanel(new BorderLayout());
        
        qp.add(queryPanel,BorderLayout.NORTH);
        FavSearchPanel.setMinimumSize(new Dimension(300, 50));
        FavSearchPanel.add(queryPanel,BorderLayout.CENTER);
        
        
        return FavSearchPanel;
        
        
    }
    
    public VueDandDTree getFavoritesTree(){
        return (this.favoritesTree);
        
    }
    
    public void setFavoritesTree(VueDandDTree favoritesTree){
        this.favoritesTree = favoritesTree;
        
    }
    
    
    public Action getDisplayAction(String displayName) {
        if (displayAction == null)
            displayAction = new DisplayAction(displayName);
        
        return (Action)displayAction;
    }
    
    
    
    private class DisplayAction extends AbstractAction {
        private AbstractButton aButton;
        
        public DisplayAction(String label) {
            super(label);
        }
        
        
        public void actionPerformed(ActionEvent e) {
            aButton = (AbstractButton) e.getSource();
            
            setVisible(aButton.isSelected());
        }
        
        
        public void setButton(boolean state) {
            aButton.setSelected(state);
        }
    }
    
    
    
    
    public void createPopupMenu() {
        JMenuItem menuItem;
        
        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        
        menuItem = new JMenuItem("Add Favorites Folder");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Remove Favorites Folder");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        JPopupMenu popup2 = new JPopupMenu();
        
        menuItem = new JMenuItem("Open Resource");
        menuItem.addActionListener(this);
        popup2.add(menuItem);
        menuItem = new JMenuItem("Remove Resource");
        menuItem.addActionListener(this);
        popup2.add(menuItem);
        
         JPopupMenu popup3 = new JPopupMenu();
        
        menuItem = new JMenuItem("Add Favorites Folder");
        menuItem.addActionListener(this);
        popup3.add(menuItem);
       
        MouseListener popupListener = new PopupListener(popup,popup2,popup3);
        favoritesTree.addMouseListener(popupListener);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        ResourceNode resNode;
        FavoritesNode favresNode;
        ResourceNode temp;
        
        if (e.getSource() instanceof JMenuItem){
            
            
            JMenuItem source = (JMenuItem)(e.getSource());
            TreePath tp = favoritesTree.getSelectionPath();
            
            DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
            
            if (tp == null){ resNode = (ResourceNode)model.getRoot();
            }
            else{
                resNode = (ResourceNode)tp.getLastPathComponent();
                
            }
            
            
            
            
            if (e.getActionCommand().toString().equals("Remove Resource") | e.getActionCommand().toString().equals("Remove Favorites Folder")) {
                
                if (resNode.isRoot()) {
                    System.out.println("cannot delete root");
                }
                else {
                    
                    model.removeNodeFromParent(resNode);
                    
                }
            }
            
            
            
            else if (e.getActionCommand().toString().equals("Add Favorites Folder")){//add a new bookmak folder
                
              
              
                
                         tp = new TreePath(resNode.getPath());
                        
                       
                        favresNode = (FavoritesNode)tp.getLastPathComponent();
                         
                       
                  
                    
                    if ((favresNode.isLeaf())){
                        
                    
                        
                        MapResource favResource = new MapResource("New Favorites Folder");
                        favResource.setType(FAVORITES);
                        FavoritesNode favNode = new FavoritesNode(favResource);
                        System.out.println("Am in fav" + favNode);
                        
                        if (model.getRoot() != resNode){
                            
                         
                            
                            model.insertNodeInto(favNode, favresNode, (favresNode.getChildCount()));
                            
                            favoritesTree.expandPath(new TreePath(favresNode.getPath()));
                            favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
                            
                            favoritesTree.setRootVisible(true);
                            this.setFavoritesTree(favoritesTree);
                            favoritesTree.setRootVisible(false);
                        }
                        else {
                            
                        
                            model.insertNodeInto(favNode, favresNode, (favresNode.getChildCount()));
            
                            favoritesTree.setRootVisible(true);
                            
                            this.setFavoritesTree(favoritesTree);
                            
                            favoritesTree.expandRow(0);
                            favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
                            favoritesTree.setRootVisible(false);
                            
                            
                        }
                        
                        
                    }
                    
                    else{
                        
                      
                        MapResource favResource = new MapResource("New Favorites Folder");
                        favResource.setType(FAVORITES);
                        FavoritesNode favNode = new FavoritesNode(favResource);
                        
                        
                        model.insertNodeInto(favNode,favresNode, (favresNode.getChildCount()));
                     
                        
                        
                        favoritesTree.setRootVisible(true);
                        
                        favoritesTree.expandPath(new TreePath(favresNode.getPath()));
                         favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
                    
                        favoritesTree.setRootVisible(false);
                        this.setFavoritesTree(favoritesTree);
                        
                        
                    }
                    
                
                
               
                
            }//Finish add a bookmark folder
            else
                
            {
                
                if (resNode instanceof FileNode) { ((FileNode)resNode).displayContent();}
                else {
                resNode.getResource().displayContent();
                }
            }
            
            
            
        }
        
        else {
            
            performSearch();
            
        }
        
        
    }
    
    
    
    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        
        
    }
    
    
    public static void marshallMap(File file,SaveVueJTree favoritesTree) {
        Marshaller marshaller = null;
        
        
        Mapping mapping = new Mapping();
        
        try {
            FileWriter writer = new FileWriter(file);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING);
            marshaller.setMapping(mapping);
            
            
            marshaller.marshal(favoritesTree);
            
            writer.flush();
            writer.close();
            
        }
        catch (Exception e) {System.err.println("FavoritesWindow.marshallMap " + e);}
        
        
    }
    
    
    
    public  SaveVueJTree unMarshallMap(File file) {
        Unmarshaller unmarshaller = null;
        SaveVueJTree sTree = null;
        
        //if (this.unmarshaller == null) {
        Mapping mapping = new Mapping();
        
        try {
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);
            unmarshaller.setMapping(mapping);
            
            FileReader reader = new FileReader(file);
            
            sTree= (SaveVueJTree) unmarshaller.unmarshal(new InputSource(reader));
            
            reader.close();
        }
        catch (Exception e) {
            System.err.println("ActionUtil.unmarshallMap: " + e);
            e.printStackTrace();
            sTree = null;
        }
        //}
        
        return sTree;
    }
    
    
    
    
    
    
    
    public void performSearch(){
        int index = 0;
        
        JScrollPane jsp = new JScrollPane();
        
        String searchString = keywords.getText();
        VueDragTree serResultTree = new VueDragTree("Search", "No Hits");
        
        if (!searchString.equals("")){
            
            boolean foundit = false;
            // VueDragTree serResultTree = new VueDragTree("Search", "Search Results");
            DefaultTreeModel serTreeModel = (DefaultTreeModel)serResultTree.getModel();
            ResourceNode serRoot = (ResourceNode)serTreeModel.getRoot();
            
            TreeModel favTreeModel = this.favoritesTree.getModel();
            ResourceNode favRoot = (ResourceNode)favTreeModel.getRoot();
            int childCount = favRoot.getChildCount();
            
            
            if (childCount > 0){
                
                for (int outi = 0; outi < childCount ; ++outi){
                    
                    if (favRoot.getChildAt(outi) instanceof FileNode){
                        FileNode childNode = (FileNode)favRoot.getChildAt(outi);
                        childNode.explore();
                    }
                    
                    
                    ResourceNode childNode = (ResourceNode)favRoot.getChildAt(outi);
                    
                    foundit = compareNode(searchString,childNode,serRoot,false);
                  //  System.out.println("And here "+ childNode.toString()+foundit);
                    
                }
                
            }
            
            
            
            
            
      
            serResultTree.expandRow(0);
           
            serResultTree.setRootVisible(false);
            serResultTree.setShowsRootHandles(true);
           
            
            JScrollPane fPane = new JScrollPane(serResultTree);
            
            this.searchResultsPane.setLayout(new BorderLayout());
            this.searchResultsPane.add(fPane,BorderLayout.NORTH,index);
            index = index + 1;
            
            this.favoritesPane.setSelectedIndex(1);
            
            
        }
        
        
    }
    
    
    
    
    
    
    public boolean compareNode(String searchString, ResourceNode Node, ResourceNode serRoot, boolean foundsomething) {
        
        
        
        
        if (searchString.compareToIgnoreCase(Node.toString()) == 0){
            
            addSearchNode(serRoot,Node);
            
            foundsomething = true;
            
            
        }
        
        else {
            
            int startIndex = 0;
            int lenSearchString = searchString.length();
            String thisString = Node.toString();
            int lenthisString = thisString.length();
            int endIndex = startIndex + lenSearchString;
            boolean found = false;
            
            while ((endIndex <= lenthisString) && !found){
                String   testString  = thisString.substring(startIndex,endIndex);
                
                if (searchString.compareToIgnoreCase(testString)== 0){
                    
                    
                    
                    addSearchNode(serRoot,Node);
                    
                    
                    
                    
                    found = true;
                    
                    foundsomething = true;
                    
                }
                
                startIndex = startIndex + 1;
                endIndex = endIndex + 1;
                
                
                
            }
            
            
        }
        
        
        if (!(Node.isLeaf())){
            
            int childCount = Node.getChildCount();
            if (childCount  > 0){
                
                
                for (int i = 0; i < childCount; ++i){
                    
                    ResourceNode childNode = (ResourceNode)Node.getChildAt(i);
                    
                    if(compareNode(searchString,childNode,serRoot,foundsomething)) {
                        foundsomething = true;
                        
                    }
                    
                    
                }
                
            }
        }
        
        
        return foundsomething;
        
        
        
    }
    
    
    public void addSearchNode(ResourceNode topNode, ResourceNode Node) {
        
        
        ResourceNode newNode = (ResourceNode)Node.clone();
        
        if (!(Node.isLeaf())){
            
            
            for(int inni = 0; inni < Node.getChildCount(); ++ inni){
                
                ResourceNode child = (ResourceNode)Node.getChildAt(inni);
                addSearchNode(newNode,child);
                
                
                
            }
        }
        
      // System.out.println("What is the child count" + newNode.getChildCount());
        
        topNode.add(newNode);
        
        
        
        
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar()== KeyEvent.VK_ENTER) {
            
            performSearch();
        }
    }
    
 class PopupListener extends MouseAdapter {
        JPopupMenu popup,popup2,popup3;
         VueDandDTree vtree = FavoritesWindow.this.getFavoritesTree();
        
        PopupListener(JPopupMenu popupMenu, JPopupMenu popupMenu2, JPopupMenu popupMenu3 ) {
            popup = popupMenu;
            popup2 = popupMenu2;
             popup3 = popupMenu3;
            
        }

        public void mousePressed(MouseEvent e) {
            
           
            maybeShowPopup(e);
        }
                 public void mouseClicked(MouseEvent e) {
            
              if (vtree.getSelectionPath() != null){
               DefaultMutableTreeNode seltreeNode = (DefaultMutableTreeNode)vtree.getSelectionPath().getLastPathComponent();
               
                  DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)vtree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                           if (treeNode != seltreeNode) { vtree.clearSelection();}
                                              
                              TreeModel vmodel = vtree.getModel();
                           if(e.getClickCount() == 2) {
                              
                             if (((DefaultMutableTreeNode)vmodel.getRoot()).getChildCount() == 1) vtree.clearSelection();
                                 if (!(seltreeNode instanceof FavoritesNode)) vtree.clearSelection();
                           
                           }
                      
                  }
                 
            maybeShowPopup(e);
        }


      

        public void mouseReleased(MouseEvent e) {
           
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            
            if (e.isPopupTrigger()) {
                   
                    popup3.show(e.getComponent(),
                           e.getX(), e.getY());
                    
                
                if (vtree.getSelectionPath() != null){
               if (vtree.getSelectionPath().getLastPathComponent() instanceof FavoritesNode){
                    popup.show(e.getComponent(),
                           e.getX(), e.getY());
                         }
                   else{
                       popup2.show(e.getComponent(),
                           e.getX(), e.getY());
                       
                   }
                }
                
                 
                
                
               
            }
             }
        }
   }      
     
    





