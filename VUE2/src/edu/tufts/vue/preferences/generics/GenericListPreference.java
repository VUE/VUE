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

package edu.tufts.vue.preferences.generics;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import tufts.vue.VUE;
import tufts.vue.VueResources;

/**
 * @author Brian Goodmon
 */

public abstract class GenericListPreference extends BasePref implements ListSelectionListener
{
	protected JPanel		panel = null;
	protected JLabel		titleLabel = null;
	protected JTextArea		messageArea = null;
	protected JList			list = null;
	protected JScrollPane	scrollPane = new JScrollPane(list);
	protected Object		previousValue = null;
	protected Runnable 		loadList = null;

	public GenericListPreference() {
	}

	public Object getPreviousValue() {
		return (previousValue != null ? previousValue : getDefaultValue());
	}

	public abstract String getTitle();
	public abstract String getDescription();
	public abstract String getDefaultValue();

	public JComponent getPreferenceUI() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBackground(Color.WHITE);
			panel.setLayout(new GridBagLayout());

			Font				defaultFont = panel.getFont();
			GridBagConstraints	gbConstraints = new GridBagConstraints();

			titleLabel = new JLabel(getTitle());
			titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			gbConstraints.gridwidth = 1;
			gbConstraints.gridheight = 1;
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbConstraints.anchor = GridBagConstraints.NORTHWEST;
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 0.0;
			gbConstraints.insets = new Insets(15, 10, 2, 2);
			panel.add(titleLabel, gbConstraints);

			messageArea = new JTextArea(getDescription());
			messageArea.setFont(defaultFont);
			messageArea.setColumns(30);
			messageArea.setLineWrap(true);
			messageArea.setWrapStyleWord(true);

			gbConstraints.gridy = 1;
			panel.add(messageArea, gbConstraints);

			String[]		listInit = {VueResources.getString("preferences.language.loading")};

			list = new JList(listInit);
			list.setFont(defaultFont);
			list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.addListSelectionListener(this);

			scrollPane = new JScrollPane(list);

			gbConstraints.gridy = 2;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weighty = 1.0;
			gbConstraints.insets = new Insets(15, 30, 15, 30);
			panel.add(scrollPane, gbConstraints);

			new Thread(loadList).start();
		}

		return panel;
	}

 	/** interface VuePreference */
	public String getValue() {
		Preferences	p = Preferences.userNodeForPackage(getPrefRoot());

		return p.get(getPrefName(), getDefaultValue());
	}

	/** interface VuePreference */
 	public void setValue(Object obj) {
 		Preferences	p = Preferences.userNodeForPackage(getPrefRoot());

 		previousValue = getValue();
 		p.put(getPrefName(), (String)obj);
 		_fireVuePrefEvent();
	}
}
