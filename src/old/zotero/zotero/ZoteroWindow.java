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

package edu.tufts.vue.zotero;

import tufts.vue.FavoritesWindow;
import tufts.vue.Resource;
import tufts.vue.ResourceNode;
import tufts.vue.VueDragTree;
import tufts.vue.VueResources;
import tufts.vue.action.ActionUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.xml.sax.InputSource;

import com.google.common.collect.Multimap;


public class ZoteroWindow extends JPanel
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FavoritesWindow.class);
    
    public static final String NEW_FAVORITES = VueResources.getString("dialog.favoriteswindow.newfFavoritesfolder");
    public static final String ADD_FAVORITES = VueResources.getString("dialog.favoriteswindow.addfavoritesfolder");
    public static final String REMOVE_FAVORITES = VueResources.getString("dialog.favoriteswindow.removefavoritesfolder");
    public static final String OPEN_RESOURCE = VueResources.getString("dialog.favoriteswindow.openresource");
    public static final String REMOVE_RESOURCE = VueResources.getString("dialog.favoriteswindow.removeresource");
    public static final String CONFIRM_DEL_RESOURCE =VueResources.getString("dialog.favoriteswindow.deleteresource");
    public static final String TITLE_DEL_RESOURCE = VueResources.getString("dialog.favoriteswindow.delresourceconf");
    private final ResourceNode rootNode = new ResourceNode(Resource.instance("My Library"));
    private String saveFile;
    public static final int DEFAULT_SELECTION_ROW = 0;

    public  VueDragTree favoritesTree ;

    JTextField keywords;
    boolean fileOpen = false;
    
    /** Creates a new instance of HierarchyTreeWindow */
    public ZoteroWindow(String displayName, String saveFile ) {
        setLayout(new BorderLayout());
        this.saveFile = saveFile;
        load();
        if (!fileOpen)
            Log.info("Error opening zotero datasource");
    
            // now that entire Resource DockWindow is in a scroll pane,
            // this is just messy -- SMF 2008-04-15
            add(favoritesTree, BorderLayout.CENTER);
    } 
    
    public VueDragTree getFavoritesTree(){
        return (this.favoritesTree);
    }
    
    public void setFavoritesTree(VueDragTree favoritesTree){
        this.favoritesTree = favoritesTree;
    }
    
    public void save() {
    	//read-only 
    	return;
    }
    private static int ATTACHMENT_ID=14;
    private static int NOTE_ID=2;
    
    private ZoteroCollection getItemsForCollection(Connection conn, String name, int id)
    {
    	ZoteroCollection collection = null;
   	    java.util.List<ZoteroItem> itemList = new ArrayList<ZoteroItem>();
    	Statement stat;
		
    	try 
		{
			stat = conn.createStatement();
 		    conn.setAutoCommit(true);
 		 
           //select itemId from collectionItems where collectionID=1 order by collectionID, orderIndex;
           ResultSet rs = stat.executeQuery("select ci.itemId, i.itemTypeID from collectionItems ci, items i where collectionID="+id+" and i.itemId=ci.itemId and i.itemID not in (select itemID from deletedItems) order by collectionID, orderIndex;");

           //get list of items in the collection by id in order that they are displayed in zotero
           while (rs.next()) 
           {
         	  int itemId = rs.getInt("itemID");
         	  int itemTypeID = rs.getInt("itemTypeID");
         	  if (itemTypeID != ATTACHMENT_ID && itemTypeID != NOTE_ID)
         	  {
         		stat = conn.createStatement();
         	  
         	    //now get the details for each item in the collection
         	    ResultSet rs2 = stat.executeQuery("select data.itemId, f.fieldName, v.value from itemData data, fields f, itemDataValues v where f.fieldID=data.fieldID and data.valueID=v.valueID and data.itemID="+itemId+";\"");
         	           	           	  
         	    ZoteroItem zItem = new ZoteroItem();
         	    while (rs2.next())
         	    {	  
         	  	  System.out.println(rs2.getString("fieldName") + " " + rs2.getString("value"));
         		  zItem.addAttribute(rs2.getString("fieldName"),rs2.getString("value"));         		
         	    }
         	    System.out.println("ADD ITEM");
        	    itemList.add(zItem);
         	  } //end if.
         	 
           }                     
           
           System.out.println("Size : " + itemList.size());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
         
    	collection = new ZoteroCollection(name,itemList);
    	
    	return collection;
    }
    public void load() {
            fileOpen = true;
            java.util.List<Resource> libraries = new ArrayList<Resource>();
            Connection conn = null;
            ResultSet rs = null;
            try 
            {
				Class.forName("org.sqlite.JDBC");		
				
				conn = DriverManager.getConnection("jdbc:sqlite://Users/mkorcy01//Library/Application Support/Firefox/Profiles/311aivcq.default/zotero/zotero.sqlite");
				Statement stat = conn.createStatement();
				conn.setAutoCommit(true);
				rs = stat.executeQuery("select * from collections;");
				java.util.List<ZoteroCollection> list = new ArrayList<ZoteroCollection>();
            
				while (rs.next()) 
				{
					System.out.println("name = " + rs.getString("collectionName"));
//					libraries.add(Resource.instance(rs.getString("collectionName")));
					ResourceNode collectionNode = new ResourceNode(Resource.instance(rs.getString("collectionName")));
					
				
					ZoteroCollection zColl = getItemsForCollection(conn,rs.getString("collectionName"),rs.getInt("collectionID"));

					java.util.List<ZoteroItem> itemList = zColl.getItemList();

					Iterator<ZoteroItem> itemListIterator = itemList.iterator();
					
					while (itemListIterator.hasNext())
					{
						ZoteroItem zItem = itemListIterator.next();
						Multimap<String,String> itemMap = zItem.getAttributeMap();
						Collection c = itemMap.get("title");
						Iterator<String> i = c.iterator();
						String title = null;
						if (i.hasNext())
							title = i.next();
						else
							title = "no title";
						System.out.println("Add item node, " + title );
						Resource res = Resource.instance(title);
						res.addProperty("things", "value");
						collectionNode.add(new ResourceNode(res));
					}
					rootNode.add(collectionNode);
				}                   
        	} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{				
				try {
					if (rs !=null)
						rs.close();
					if (conn !=null)
						conn.close();	
				} catch (SQLException e) {
					e.printStackTrace();
				}		
			}
			
            
            favoritesTree = new ZoteroDandDTree(rootNode);           
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);
            this.setFavoritesTree(favoritesTree);
    }
}