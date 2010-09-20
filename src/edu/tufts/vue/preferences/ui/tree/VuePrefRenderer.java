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
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import tufts.vue.gui.GUI;

/**
 * @author Mike Korcynski
 *
 */
public class VuePrefRenderer extends DefaultTreeCellRenderer {
	
	
    private JLabel mLabel = new DefaultTreeCellRenderer();
    
	public VuePrefRenderer() {
	    setLeafIcon(null);		    
	    setClosedIcon(null);
	    setOpenIcon(null);           
        mLabel.setMinimumSize(new Dimension(10, 20));
        mLabel.setPreferredSize(new Dimension(100, 20));                    
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) 
	{
		
		mLabel.setOpaque(true);
			         	           
        if (!leaf)
        {
        	mLabel.setForeground(Color.gray);
        }
        else
        {
        	mLabel.setForeground(Color.black);
        }
        
        mLabel.setBackground(Color.white);
                
        String displayName = null;
        if (value instanceof edu.tufts.vue.preferences.ui.tree.PrefCategoryTreeNode)
        {
        	displayName = ((PrefCategoryTreeNode)value).nodeName;
        }
        else if (value instanceof edu.tufts.vue.preferences.ui.tree.PrefTreeNode)
        {
        	displayName = ((PrefTreeNode)value).pref;
        }
            
        mLabel.setText(displayName);
        	        
		return mLabel;
	}
}