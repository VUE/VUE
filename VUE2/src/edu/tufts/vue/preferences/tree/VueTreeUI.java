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

package edu.tufts.vue.preferences.tree;

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
		gbc.insets = new Insets(3, bounds.x, 3, 0);
		
		p.add(component,gbc);
		   Color bg;
	        if (tree.isRowSelected(row) && isLeaf) {
	            bg = GUI.getTextHighlightColor();
	        } else {	            
	                bg = tree.getBackground();
	        }
	     p.setBackground(bg);
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
