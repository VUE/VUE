/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
package tufts.vue;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;
import tufts.vue.gui.*;
import tufts.vue.gui.FocusManager;

public class SakaiCollectionDialog extends SizeRestrictedDialog implements javax.swing.event.TreeSelectionListener
{
	private JTree tree = null;
    private javax.swing.tree.DefaultTreeModel treeModel = null;
    private JScrollPane jsp = null;
	private static String TITLE = "Sakai Collections";
	
	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("sakaiproject.org","asset","siteCollection");
	private org.osid.shared.Type _sakaiRepositoryType = new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
	private edu.tufts.vue.dsm.DataSourceManager _dsm = null;	   
	
	public SakaiCollectionDialog(edu.tufts.vue.dsm.DataSource dataSources[])
	{
		super(VUE.getDialogParentAsFrame(),TITLE,true);
		try {
			//System.out.println("making tree");
			javax.swing.tree.DefaultMutableTreeNode root =
			new javax.swing.tree.DefaultMutableTreeNode("Sites");
			this.treeModel = new javax.swing.tree.DefaultTreeModel(root);
			//System.out.println("num ds " + dataSources.length);
			for (int i=0; i < dataSources.length; i++) {
				org.osid.repository.Repository repository = dataSources[i].getRepository();
				//System.out.println("repository is " + repository.getDisplayName());
				org.osid.repository.AssetIterator assetIterator = repository.getAssetsByType(_collectionAssetType);
				while (assetIterator.hasNextAsset()) {
					org.osid.repository.Asset asset = assetIterator.nextAsset();
					//System.out.println("asset is " + asset.getDisplayName());
					SakaiSiteUserObject userObject = new SakaiSiteUserObject();
					userObject.setId(asset.getId().getIdString());
					userObject.setDisplayName(asset.getDisplayName());
					//System.out.println("another obj " + userObject);
					javax.swing.tree.DefaultMutableTreeNode nextTreeNode = new javax.swing.tree.DefaultMutableTreeNode(userObject);				
					this.treeModel.insertNodeInto(nextTreeNode,root,0);
				}
			}
			this.tree = new JTree(this.treeModel);
			this.jsp = new JScrollPane(this.tree);
			add(this.jsp);
			setSize(200,200);
			setVisible(true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
    private void populate(org.osid.repository.Asset asset,
						  javax.swing.tree.DefaultMutableTreeNode parent)
    {
        try {
			// recursively descend until only agents remain
			if (asset.getAssetType().isEqual(_collectionAssetType)) {
				org.osid.repository.AssetIterator assetIterator = asset.getAssetsByType(_collectionAssetType);
				while (assetIterator.hasNextAsset()) {
					org.osid.repository.Asset a = assetIterator.nextAsset();
					//System.out.println("asset is " + asset.getDisplayName());
					SakaiSiteUserObject userObject = new SakaiSiteUserObject();
					userObject.setId(a.getId().getIdString());
					userObject.setDisplayName(a.getDisplayName());
					//System.out.println("another obj " + userObject);
					javax.swing.tree.DefaultMutableTreeNode nextTreeNode = new javax.swing.tree.DefaultMutableTreeNode(userObject);				
					this.treeModel.insertNodeInto(nextTreeNode,parent,0);
				}
			}
        } catch (Throwable t) {
            t.printStackTrace();
		}
    }
	
	public void valueChanged(javax.swing.event.TreeSelectionEvent tse)
    {
        javax.swing.tree.TreePath treePath = tse.getPath();
        javax.swing.tree.DefaultMutableTreeNode node = 
            (javax.swing.tree.DefaultMutableTreeNode)treePath.getLastPathComponent();
        SakaiSiteUserObject userObject = (SakaiSiteUserObject)(node.getUserObject());
        String idOfSelection = userObject.getId();
		System.out.println("Selected " + idOfSelection);
    }
	
    private void expandAll()
    {
        try {
			int currentRowCount = 0;
			int previousRowCount = -1;
			while (previousRowCount != currentRowCount) {
				previousRowCount = this.tree.getRowCount();
				for (int r = 0; r < previousRowCount; r++) this.tree.expandRow(r);
				currentRowCount = this.tree.getRowCount();
			}        
		}
        catch (Throwable t) {
            t.printStackTrace();
		}
    }
}
