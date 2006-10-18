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

package edu.tufts.vue.preferences.ui;

import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.interfaces.VuePreference;
import edu.tufts.vue.preferences.tree.PrefCategoryTreeNode;
import edu.tufts.vue.preferences.tree.PrefTreeNode;
import edu.tufts.vue.preferences.tree.VuePrefRenderer;
import edu.tufts.vue.preferences.tree.VueTreeUI;
import tufts.Util;
import tufts.vue.gui.GUI;
import java.util.prefs.*;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private GridBagConstraints gbConstraints = new GridBagConstraints();

	private JTree prefTree;

	private JSplitPane splitPane = null;

	// private JTable editTable;

		public PreferencesDialog(JFrame owner, String title, Class userObj,
			boolean showUserPrefs, Object systemObj, boolean showSystemPrefs) {
		super(owner, title);
		getContentPane().setLayout(new GridBagLayout());
		setSize(640, 480);
		Util.centerOnScreen(this);
		setModal(true);
		createTree();
		// editTable = new JTable();
		createSplitPane();
		createButtonPanel();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	private void createTree() {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
				"VUE Preferences");

		Iterator i = PreferencesManager.getCategories().iterator();
		// Build Category List.
		while (i.hasNext()) {
			String category = (String) i.next();
			//System.out.println(category);
			PrefCategoryTreeNode categoryNode = new PrefCategoryTreeNode(
					category);
			rootNode.add(categoryNode);
		}

		addPrefsToCats(rootNode);

		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		prefTree = new JTree(model);
		
		prefTree.setBorder(BorderFactory.createEmptyBorder());
		prefTree.setCellRenderer(new VuePrefRenderer());
		prefTree.setRootVisible(false);
		prefTree.setShowsRootHandles(false);
		prefTree.setUI(new VueTreeUI());
	         
			

			
		prefTree.putClientProperty("JTree.lineStyle", "None");
		 expandAll( prefTree, new TreePath( prefTree.getModel().getRoot() ) );       
		prefTree.addTreeSelectionListener(new PrefTreeSelectionListener());
	}

	  /**                                                                                                        
     * Expand a tree node and all its child nodes recursively.                                                 
     *                                                                                                         
     * @param tree the tree                                                            
     * @param path the path to expand                                                              
     */                                                                                                        
    public static void expandAll( JTree tree, TreePath path )                                                    
    {                                                                                                          
        Object node = path.getLastPathComponent();                                                         
        TreeModel model = tree.getModel();                                                                 
        if( model.isLeaf( node ) )                                                                             
            return;   
        
        tree.expandPath( path );                                                                             
        int num = model.getChildCount( node );                                                               
        for( int i = 0; i < num; i++ )                                                                       
            expandAll( tree, path.pathByAddingChild( model.getChild( node, i ) ) );                          
    }                   
    
  
	private void addPrefsToCats(DefaultMutableTreeNode node) {
		String pckgname = "edu.tufts.vue.preferences.implementations";
		List classes =PreferencesManager.getPreferences();
	
		Object[] classesA = classes.toArray();
		Preferences p = Preferences
				.userNodeForPackage(edu.tufts.vue.preferences.PreferencesManager.class);

		for (int i = 0; i < classesA.length; i++) {
			Enumeration nodes = node.breadthFirstEnumeration();
			try {
				VuePreference vp = (VuePreference) ((Class)classesA[i]).newInstance();

				while (nodes.hasMoreElements()) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodes
							.nextElement();
					if ((n instanceof PrefCategoryTreeNode)
							&& (((PrefCategoryTreeNode) n).toString().equals(vp
									.getPreferenceCategory()))) {
						try {
							n.add(new PrefTreeNode(vp));
						} catch (BackingStoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void createSplitPane() {
		splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);		
		splitPane.setOneTouchExpandable(false);
		JScrollPane pane = new JScrollPane(prefTree)
		{
			public Dimension getPreferredSize()
			{
				return new Dimension(180,prefTree.getHeight());
			}
		};
		prefTree.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		splitPane.setLeftComponent(pane);
		splitPane.setRightComponent(new JScrollPane());

		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.gridwidth = 1;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.weightx = 1;
		gbConstraints.weighty = 1;
		gbConstraints.insets = new Insets(10, 10, 0, 10);

		getContentPane().add(splitPane, gbConstraints);
	}

	private void createButtonPanel() {
		JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PreferencesDialog.this.setVisible(false);
				PreferencesDialog.this.dispose();
			}
		});
		buttonPanel.add(closeButton, BorderLayout.EAST);

		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		gbConstraints.gridwidth = 1;
		gbConstraints.fill = GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		gbConstraints.weighty = 0;
		gbConstraints.insets = new Insets(5, 0, 5, 10);
		getContentPane().add(buttonPanel, gbConstraints);
	}

	

	class PrefTreeSelectionListener implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			try {
				PrefTreeNode node = (PrefTreeNode) e.getPath()
						.getLastPathComponent();
				// Preferences pref = node.getPrefObject();
				// editTable.setModel(new PrefTableModel(node));
				splitPane.setRightComponent(node.getPrefObject().getPreferenceUI());
			} catch (ClassCastException ce) {
				//System.out.println("Node not PrefTreeNode!");
				// editTable.setModel(new DefaultTableModel());
			}
		}
	}
		
	public static void main(String[] args) {
		Preferences prefs = Preferences
				.userNodeForPackage(edu.tufts.vue.preferences.PreferencesManager.class);
		prefs.putBoolean("mapDisplay.AutoZoom", true);

		PreferencesDialog dialog = new PreferencesDialog(null,
				"Vue Preferences",
				edu.tufts.vue.preferences.PreferencesManager.class, true, null,
				false);
		dialog.show();

	}
}