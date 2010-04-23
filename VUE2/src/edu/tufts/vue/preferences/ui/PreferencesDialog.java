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
import edu.tufts.vue.preferences.ui.tree.PrefCategoryTreeNode;
import edu.tufts.vue.preferences.ui.tree.PrefTreeNode;
import edu.tufts.vue.preferences.ui.tree.VuePrefRenderer;
import edu.tufts.vue.preferences.ui.tree.VueTreeUI;
import tufts.Util;
import tufts.vue.VueResources;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;

/**
 * @author Mike Korcynski
 *
 */
public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private GridBagConstraints gbConstraints = new GridBagConstraints();

	private JTree prefTree;

	private JSplitPane splitPane = null;
	private DefaultMutableTreeNode rootNode = null;
	private static PreferencesDialog dialog;
	// private JTable editTable;

		public PreferencesDialog(JFrame owner, String title, Class userObj,
			boolean showUserPrefs, Object systemObj, boolean showSystemPrefs) {
		super(owner, title);
		dialog = this;
		getContentPane().setLayout(new GridBagLayout());
		setSize(640, 480);
		Util.centerOnScreen(this);
		setModal(true);
		createTree();
		// editTable = new JTable();
		createSplitPane();
		createButtonPanel();
		String[] array = new String[2];
		array[0] = new String(VueResources.getString("preferencedailog.vuepreference"));
		array[1] = new String(VueResources.getString("preferences.category.mapdisplay"));
		//array[2] = new String(VueResources.getString("preferencedailog.images"));
//		array[2] = new String("Metadata:");
	//	array[3] = new String("Windows:");

                try {
                    TreePath path = findByName(prefTree,array);
                    PrefCategoryTreeNode node = (PrefCategoryTreeNode) path.getLastPathComponent();
//                    splitPane.setRightComponent(node.getPrefObject().getPreferenceUI());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		prefTree.setSelectionRow(1);
	}
		
		  // Finds the path in tree as specified by the array of names. The names array is a
	    // sequence of names where names[0] is the root and names[i] is a child of names[i-1].
	    // Comparison is done using String.equals(). Returns null if not found.
	    public TreePath findByName(JTree tree, String[] names) {
	        TreeNode root = (TreeNode)tree.getModel().getRoot();
	        return find2(tree, new TreePath(root), names, 0, true);
	    }
	    private TreePath find2(JTree tree, TreePath parent, Object[] nodes, int depth, boolean byName) {
	        TreeNode node = (TreeNode)parent.getLastPathComponent();
	        Object o = node;
	    
	        // If by name, convert node to a string
	        if (byName) {
	            o = o.toString();
	        }
	    
	        // If equal, go down the branch
	        if (o.equals(nodes[depth])) {
	            // If at end, return match
	            if (depth == nodes.length-1) {
	                return parent;
	            }
	    
	            // Traverse children
	            if (node.getChildCount() >= 0) {
	                for (Enumeration e=node.children(); e.hasMoreElements(); ) {
	                    TreeNode n = (TreeNode)e.nextElement();
	                    TreePath path = parent.pathByAddingChild(n);
	                    TreePath result = find2(tree, path, nodes, depth+1, byName);
	                    // Found a match
	                    if (result != null) {
	                        return result;
	                    }
	                }
	            }
	        }
	    
	        // No match at this branch
	        return null;
	    }		

	    public static PreferencesDialog getDialog()
	    {
	    	return dialog;
	    }
	private void createTree() {
		rootNode = new DefaultMutableTreeNode(
				VueResources.getString("preferencedailog.vuepreference"));

		Iterator i = PreferencesManager.getCategories().iterator();
		// Build Category List.
		while (i.hasNext()) {
			String prefKey = (String)i.next();
			String category = VueResources.getString("preferences.category." + prefKey);
			System.out.println(category);
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
    
    private VuePreference getPreferenceObject(Class a)
    {
		Method m = null;
		VuePreference vp = null;
		
    	try {
			m = a.getMethod("getInstance");
			//m = a.getMethod("getInstance", null);
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		 
		
		try {
                    vp = (VuePreference)m.invoke(null);
                    //vp = (VuePreference)m.invoke(null, null);
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return vp;			
    }
    
	private void addPrefsToCats(DefaultMutableTreeNode node) {
		String pckgname = "edu.tufts.vue.preferences.implementations";
		List classes =PreferencesManager.getPreferences();
		VuePreference vp = null;
		Object[] classesA = classes.toArray();
	
		Preferences p = Preferences
				.userNodeForPackage(edu.tufts.vue.preferences.PreferencesManager.class);

		for (int i = 0; i < classesA.length; i++) 
		{
			Enumeration nodes = node.breadthFirstEnumeration();
			
				if (classesA[i] instanceof VuePreference)
					vp = (VuePreference)classesA[i];
				else 	
					vp = getPreferenceObject((Class)classesA[i]);
				while (nodes.hasMoreElements()) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodes
							.nextElement();
					if ((n instanceof PrefCategoryTreeNode)
							&& (((PrefCategoryTreeNode) n).toString().equals(PreferencesManager.mapCategoryKeyToName(vp
									.getCategoryKey())))) {
						try {
							n.add(new PrefTreeNode(vp));							
						} catch (BackingStoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}		
		}
	}

	private void createSplitPane() {
		splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);		
		splitPane.setOneTouchExpandable(false);
		splitPane.setEnabled(false);
		JScrollPane pane = new JScrollPane(prefTree)
		{
			public Dimension getMaximumSize()
			{
				return new Dimension(180,prefTree.getHeight());
			}
			public Dimension getPreferredSize()
			{
				return new Dimension(180,prefTree.getHeight());
			}
			public Dimension getMinimumSize()
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
		JButton closeButton = new JButton(VueResources.getString("button.close.label"));
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
				VueResources.getString("preferencedailog.vuepreference"),
				edu.tufts.vue.preferences.PreferencesManager.class, true, null,
				false);
		dialog.show();

	}
}