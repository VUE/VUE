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
public class FavoritesWindow extends JPanel implements ActionListener, ItemListener 
{
    private DisplayAction displayAction = null;
    private VueDandDTree favoritesTree ;
    private JScrollPane browsePane;
    final static String XML_MAPPING = VueResources.getURL("mapping.lw").getFile();
    private static String  FAVORITES_MAPPING;
    private static int newFavorites = 0;
    /** Creates a new instance of HierarchyTreeWindow */
    public FavoritesWindow(String displayName ) 
    {
        setLayout(new BorderLayout());
       
       JTabbedPane favoritesPane = new JTabbedPane();
       
    
   
         
           File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.favorites"));
             
              if (f.exists()){ 
                  
            SaveVueJTree restorefavtree = unMarshallMap(f);
          //  System.out.println("Afte Unmarshalling "+restorefavtree.getClass().getName()+ " root"+ restorefavtree.getSaveTreeRoot().getResourceName());
            VueDandDTree favoritesTree =restorefavtree.restoreTree();
             favoritesTree.setRootVisible(true);
             favoritesTree.expandRow(0);
             favoritesTree.setRootVisible(false);
           
              this.setFavoritesTree(favoritesTree); 
          
           }
           
           else{
               
               FavoritesNode favRoot = new FavoritesNode("My Favorites");
               VueDandDTree favoritesTree = new VueDandDTree(favRoot);
                favoritesTree.setRootVisible(true);
             favoritesTree.expandRow(0);
             favoritesTree.setRootVisible(false);
           
             
              this.setFavoritesTree(favoritesTree); 
               
           }
            
           
          //System.out.println("Favorites--"+ FAVORITES_MAPPING);
        JPanel searchResultsPane = new JPanel();
        FavSearchPanel favSearchPanel = new FavSearchPanel(favoritesTree,favoritesPane,searchResultsPane);
            
         favoritesPane.addTab("Search",favSearchPanel);
         
          
         
        favoritesPane.addTab("Search Results",searchResultsPane);
       
       
       
        browsePane = new JScrollPane(favoritesTree);
         favoritesPane.add("Browse", browsePane); 
         
        
          
          
          createPopupMenu();
        favoritesPane.setSelectedIndex(2);
          
         add(favoritesPane,BorderLayout.CENTER);
         
          
                         
                         
                        
         
        
    }
    
   public VueDandDTree getFavoritesTree(){
       return (this.favoritesTree);

   }
   
   public void setFavoritesTree(VueDandDTree favoritesTree){
       this.favoritesTree = favoritesTree;
       
   }
       
       
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("My Favorites");
        
        return (Action)displayAction;
    }
    
 
    
    private class DisplayAction extends AbstractAction
    {
        private AbstractButton aButton;
        
        public DisplayAction(String label)
        {
            super(label);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            aButton = (AbstractButton) e.getSource();
            setVisible(aButton.isSelected());
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
 
    //----------------------------Closing Favorites window
    
   //---------------addPoppup stuff
    
    
          
          
         public void createPopupMenu() {
        JMenuItem menuItem;

        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        menuItem = new JMenuItem("Open Resource");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Add Bookmark Folder");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Remove Resource");
        menuItem.addActionListener(this);
        popup.add(menuItem);
         menuItem = new JMenuItem("Save Favorites");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
       
        //Add listener to the text area so the popup menu can come up.
        MouseListener popupListener = new PopupListener(popup);
        favoritesTree.addMouseListener(popupListener);
    }
    
          
           public void actionPerformed(ActionEvent e) {
         
               DefaultMutableTreeNode dn;
             
        JMenuItem source = (JMenuItem)(e.getSource());
         
        TreePath tp = favoritesTree.getSelectionPath();
          
        DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
        
              if (tp == null){
                  
                  
                  
                   dn = (DefaultMutableTreeNode)model.getRoot();
              
              }
              else{
                dn = (DefaultMutableTreeNode)tp.getLastPathComponent();
          
              }
        
            if (e.getActionCommand().toString().equals("Add Bookmark Folder")){
            
                 
                      
                
              if (dn.isLeaf()){        
                 
                 FavoritesNode newNode= new FavoritesNode("New Favorites Folder");
                 
                 
                 newFavorites = newFavorites +1 ;
                  if (model.getRoot() != dn){
                 FavoritesNode node = (FavoritesNode)dn.getParent();
                    model.insertNodeInto(newNode, node, 0); 
                   
                    
                     favoritesTree.setRootVisible(true);
                     favoritesTree.expandRow(dn.getLevel());
                     
                      favoritesTree.setRootVisible(false);
                    
                 }
                 else {
                     
                      model.insertNodeInto(newNode, dn, 0); 
                      favoritesTree.setRootVisible(true);
                       favoritesTree.expandRow(dn.getLevel());
                        favoritesTree.setRootVisible(false);
                 }
              
                        
                        this.setFavoritesTree(favoritesTree);
                    
           
                                                 }
                else
                     {
                  
                                 
                    FavoritesNode newNode= new FavoritesNode("New Favorites Folder") ;
                   
                    model.insertNodeInto(newNode, dn, 0);  
                    
                    favoritesTree.setRootVisible(true);
                     favoritesTree.expandRow(dn.getLevel());
                     favoritesTree.setRootVisible(false);
                      this.setFavoritesTree(favoritesTree);
            
             
                         }
            
                       
                }
        
             else if (e.getActionCommand().toString().equals("Remove Resource"))
             {
                 
                    // System.out.println("I am in remove resource in favorites");
                         if (dn.isRoot())
                         {
                             System.out.println("cannot delete root");
                         }
                         else
                         {
           
                           
                            model.removeNodeFromParent(dn);
                          
                                            }
                }
             else if (e.getActionCommand().toString().equals("Restore Favorites")){
                 
                         // File f  = new File("C:\\temp\\savetree.xml");
                         System.out.println("before mapping - fav");
                          File f  = new File(FAVORITES_MAPPING);
                          System.out.println("after mapping - fav");
                          SaveVueJTree restorefavtree = unMarshallMap(f);
                          System.out.println("Afte Unmarshalling "+restorefavtree.getClass().getName()+ " root"+ restorefavtree.getSaveTreeRoot().getResourceName());
                          VueDandDTree vueTree =restorefavtree.restoreTree();
                         
                         JFrame jf = new JFrame();
                   
                       
                         JScrollPane newPane = new JScrollPane(vueTree);
                         jf.getContentPane().add(newPane);
                         jf.pack();
                         jf.setVisible(true);
                 }
        
            
            //-----------------Save/Restore Tree
        
        
        
             else {
                 // System.out.println("I am in open resource in favorites");
                
                    if (tp.getLastPathComponent() instanceof FavoritesNode){
            
                     
                         }
                     else
                    {
                 
                            if ((dn.getUserObject()) instanceof  Asset){
                                
                       
                            }
                            else
                            {
                            Resource resource =new Resource(dn.getUserObject().toString());
                            resource.displayContent();
                            }
                     }     
               
                           
                        }
                 
             }
    
           

    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        
        
    }
   class PopupListener extends MouseAdapter {
        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

         public void mouseClicked(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
               
                
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }
    }   
 
  public static void marshallMap(File file,SaveVueJTree favoritesTree)
    {
        Marshaller marshaller = null;
        
        
        Mapping mapping = new Mapping();
            
        try 
        {  
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
    

   
    public  SaveVueJTree unMarshallMap(File file)
    {
        Unmarshaller unmarshaller = null;
        SaveVueJTree sTree = null;
        
        //if (this.unmarshaller == null) {   
        Mapping mapping = new Mapping();
            
        try 
        {
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);    
            unmarshaller.setMapping(mapping);  
            
            FileReader reader = new FileReader(file);
            
            sTree= (SaveVueJTree) unmarshaller.unmarshal(new InputSource(reader));
            
            reader.close();
        } 
        catch (Exception e) 
        {
            System.err.println("ActionUtil.unmarshallMap: " + e);
            e.printStackTrace();
            sTree = null;
        }
        //}
        
        return sTree;
    }
    
 class FavSearchPanel extends JPanel{
     
     JTabbedPane fp;
     JPanel sp;
     JTextField keywords;
     FavSearchPanel(VueDandDTree favortiesTree, JTabbedPane fp, JPanel sp){
     
         setLayout(new BorderLayout());
        this.fp = fp;
        this.sp = sp;
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
        JLabel topLabel = new JLabel("Search Favorites");
        gridbag.setConstraints(topLabel, c);
        queryPanel.add(topLabel);
        
        
    //adding the search box and the button
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=2;
        c.ipady=0;
        keywords = new JTextField();
        keywords.setPreferredSize(new Dimension(100,20));
        gridbag.setConstraints(keywords, c);
        queryPanel.add(keywords);
        
        c.gridx=2;
        c.gridy=1;
        c.gridwidth=1;
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80,20));
       
        gridbag.setConstraints(searchButton,c);
        queryPanel.add(searchButton);
        
     
        
       
        JPanel qp = new JPanel(new BorderLayout());
        qp.add(queryPanel,BorderLayout.NORTH);
        this.add(qp,BorderLayout.CENTER);
     
        
        
         searchButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                
            int index = 0;
           
                JScrollPane jsp = new JScrollPane();
                
                String searchString = keywords.getText();
                        
               if (!searchString.equals("")){
                            
                        boolean foundit = false;
                        VueDragTree serResultTree = new VueDragTree("Search", "Search Results");
                        DefaultTreeModel serTreeModel = (DefaultTreeModel)serResultTree.getModel();
                        DefaultMutableTreeNode serRoot = (DefaultMutableTreeNode)serTreeModel.getRoot();
                        
                        TreeModel favTreeModel = favoritesTree.getModel();
                        FavoritesNode favRoot = (FavoritesNode)favTreeModel.getRoot();
                        int childCount = favRoot.getChildCount();
                        
                        
                        if (childCount > 0){
                         
                        for (int outi = 0; outi < childCount ; ++outi){
                        
                       if (favRoot.getChildAt(outi) instanceof FileNode){
                         FileNode childNode = (FileNode)favRoot.getChildAt(outi);
                         childNode.explore();
                       }
                       
                        
                           DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)favRoot.getChildAt(outi);
                       
                              foundit = compareNode(searchString,childNode,serRoot,false);
                               System.out.println("And here "+ childNode.toString()+foundit);
           
                               }
                
                        } 
                        
                      
                        
                              
                       
                        serResultTree.expandRow(0);
                        JScrollPane fPane = new JScrollPane(serResultTree);
                        FavSearchPanel.this.sp.setLayout(new BorderLayout());
                        FavSearchPanel.this.sp.add(fPane,BorderLayout.NORTH,index);
                        index = index + 1;
                        
                        FavSearchPanel.this.fp.setSelectedIndex(1);
                     
                        
                }
               }

            });
}
     
public boolean compareNode(String searchString, DefaultMutableTreeNode Node, DefaultMutableTreeNode serRoot, boolean foundsomething)
{
 
  
    
                             
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
                                                         
                                               DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)Node.getChildAt(i);
                         
                                                 if(compareNode(searchString,childNode,serRoot,foundsomething))
                                                            {
                                                                         foundsomething = true;   
                              
                                                                                  }
                             
                             
                                                                    }
                          
                                                                }    
                                                       }
                            
   
             return foundsomething;
                          
    
 
 }
 
public void addSearchNode(DefaultMutableTreeNode topNode, DefaultMutableTreeNode Node)
{
    
    
    DefaultMutableTreeNode newNode = (DefaultMutableTreeNode)Node.clone();
    
             if (!(Node.isLeaf())){
                 
                 
          for(int inni = 0; inni < Node.getChildCount(); ++ inni){
                                  
          DefaultMutableTreeNode child = (DefaultMutableTreeNode)Node.getChildAt(inni);
          addSearchNode(newNode,child);
        
        
               }
             }
      
        topNode.add(newNode); 
            
    
        

}

     
     
     
}
}

