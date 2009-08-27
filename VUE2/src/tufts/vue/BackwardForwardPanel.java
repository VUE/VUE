/*
 * Copyright 2003-2009 Tufts University  Licensed under the
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;

import tufts.vue.Actions;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.VueButtonIcon;

public class BackwardForwardPanel extends JPanel
{
	public static final long	serialVersionUID = 1;


	public BackwardForwardPanel() {
		super(new GridLayout(1, 2, 0, 0));


		VueButton	backwardButton = createButton(Actions.ViewBackward, VueResources.getIcon("Back.raw")),
					forwardButton = createButton(Actions.ViewForward, VueResources.getIcon("Forward.raw"));

		add(backwardButton);
		add(forwardButton);

		if (DEBUG.BOXES) {
			this.setBackground(Color.MAGENTA);
		}
	}


	protected VueButton createButton(Action action, Icon icon) {
		VueButton	newButton = new VueButton(action);

		VueButtonIcon.installGenerated(newButton, icon, null);

		Icon		installedIcon = newButton.getIcon();
		Dimension	buttonSize = new Dimension(installedIcon.getIconWidth(), installedIcon.getIconHeight());

		newButton.setMinimumSize(buttonSize);
		newButton.setMaximumSize(buttonSize);
		newButton.setPreferredSize(buttonSize);

		newButton.setAsToolbarButton(true);

		return newButton;
	}
}
