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

import java.util.prefs.BackingStoreException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * @author Mike Korcynski
 *
 */
public class PrefTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	String pref;

	String nodeName;

	VuePreference vuePref;

	public PrefTreeNode(VuePreference pref) throws BackingStoreException {
		this.pref = pref.getTitle();
		this.vuePref = pref;

	}

	public VuePreference getPrefObject() {
		return vuePref;
	}

	public boolean isLeaf() {
		return true;
	}

	public int getChildCount() {
		return 0;
	}

	public TreeNode getChildAt(int childIndex) {
		return new DefaultMutableTreeNode(pref);

	}

	public String toString() {

		return pref;
	}
}
