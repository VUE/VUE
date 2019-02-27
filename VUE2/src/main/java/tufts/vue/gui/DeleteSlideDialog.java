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

package tufts.vue.gui;

import java.awt.Frame;

import javax.swing.JLabel;
import javax.swing.JPanel;

import tufts.vue.VueResources;
import edu.tufts.vue.preferences.implementations.ShowAgainDialog;

public class DeleteSlideDialog extends ShowAgainDialog{

	private final JPanel panel = new JPanel();    	

	 public DeleteSlideDialog(Frame parentFrame)
	 {		 	 
		super(parentFrame,"deleteSlide",VueResources.getString("deleteslidedialog.deleteslides"),VueResources.getString("deleteslidedialog.delete"),VueResources.getString("deleteslidedialog.cancel"));
	    panel.add(new JLabel(VueResources.getString("jlabel.deletingthisslide")));
	    setContentPanel(panel);
	 }
}
