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

package edu.tufts.vue.preferences.ui.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.tree.TreePath;

import tufts.vue.gui.GUI;

/**
 * @author Mike Korcynski
 *
 */
public class VueTreeUI extends MetalTreeUI{
	
	private GridBagConstraints gbc = new GridBagConstraints();
	private JPanel p;
	public VueTreeUI()
	{
		p = new JPanel();
	}
	protected void paintRow(Graphics g,
            Rectangle clipBounds,
            Insets insets,
            Rectangle bounds,
            TreePath path,
            int row,
            boolean isExpanded,
            boolean hasBeenExpanded,
            boolean isLeaf)
	{
//		 Don't paint the renderer if editing this row.
		if(editingComponent != null && editingRow == row)
		    return;

		int leadIndex;

		if(tree.hasFocus()) {
		    leadIndex = this.getSelectionModel().getLeadSelectionRow();
		}
		else
		    leadIndex = -1;

		Component component;

		component = currentCellRenderer.getTreeCellRendererComponent
		              (tree, path.getLastPathComponent(),
			       tree.isRowSelected(row), isExpanded, isLeaf, row,
			       (leadIndex == row));									
		
		p.setLayout(new GridBagLayout());
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, bounds.x, 0, 0);
		
		p.add(component,gbc);
		   Color bg;
	        if (tree.isRowSelected(row) && isLeaf) {
	            bg = GUI.getTextHighlightColor();
	        } else {	            
	                bg = tree.getBackground();
	        }
	     p.setBackground(bg);
	     Component[] c = p.getComponents();
	     for (int i = 0 ; i < c.length; i++)	    	
	     {
	    	 c[i].setBackground(bg);
	     }
		//For Debugging.
		//component.setBackground(row %2 == 0 ? Color.red: Color.blue);	     
	    p.setSize(Short.MAX_VALUE,bounds.height); 
		rendererPane.paintComponent(g,p, tree, 0, bounds.y,
					    p.getWidth(), p.getHeight(), true);	
		
	}
	
     public Icon getExpandedIcon() {
         return null;
     }
     public Icon getCollapsedIcon() {
         return null;
     }


}
