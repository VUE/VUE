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
package edu.tufts.vue.preferences.implementations;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import tufts.vue.VueResources;
import tufts.vue.VueUtil;

import edu.tufts.vue.preferences.generics.BasePref;
import edu.tufts.vue.preferences.interfaces.VuePreference;
import edu.tufts.vue.preferences.ui.PreferencesDialog;

public class StringPreference extends BasePref implements ActionListener
{

	private JPanel		panel = null;
	private JLabel		titleLabel = null;
	private JTextArea		messageArea = null;
	private JTextField field = new JTextField(30);
	private JPanel innerPanel = null;
	private JButton resetButton =null;// new JButton(VueResources.getString("button.reset.label"));
	private JButton saveButton = null;//new JButton(VueResources.getString("button.save.label"));

	private String category;
	private String name;
	private String key;
	private String description;
	private Object defaultValue;
	private Object previousValue;
	private Preferences p = Preferences.userNodeForPackage(getPrefRoot());
	
	public static StringPreference create(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		return new StringPreference(category,key,name,desc,defaultValue,showInUI);
	}
	
	//show in UI defaults to true
	public static StringPreference create(String category, String key, String name, String desc, Object defaultValue)
	{
		return new StringPreference(category,key,name,desc,defaultValue,true);
	}
	
	protected StringPreference(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = defaultValue;
		if (showInUI)
			edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}
		
	public Object getDefaultValue()
	{
		return defaultValue;
	}
	public String getDescription() { 
		return description;
	}

	public String getMessage()
	{
		return name;
	}
	public String getTitle() {
		return name;
	}

	public String getCategoryKey() { 
		return category;
	}

	public String getPrefName() {
		return category + "." + key;
	}

	public JComponent getPreferenceUI() {
		
		panel = new JPanel();
		innerPanel = new JPanel();
		resetButton = new JButton(VueResources.getString("button.reset.label"));
		saveButton = new JButton(VueResources.getString("button.save.label"));
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

		gbConstraints.gridy = 2;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.weighty = 1.0;
		gbConstraints.insets = new Insets(15, 30, 15, 30);
		
		innerPanel.setBackground(Color.white);
		field.setText((String)this.getValue());
		innerPanel.add(field);
		resetButton.addActionListener(this);

		saveButton.addActionListener(this);

		innerPanel.add(resetButton);
		innerPanel.add(saveButton);
		saveButton.setEnabled(true);
		resetButton.setEnabled(true);		
		panel.add(innerPanel,gbConstraints);

		return panel;
		
		
	}

	public Object getPreviousValue()
	{
		if (previousValue == null)
			return getDefaultValue();
		else
			return previousValue;
	}

	public Object getValue(){
		String s = (String)(p.get(getPrefName(), (String)getDefaultValue()));
		return s;
	
	}

	public void setValue(Object s)
	{
		previousValue = getValue(); 		
		p.put(getPrefName(), (String)s);
		_fireVuePrefEvent();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(resetButton))
		{
			this.setValue(getDefaultValue());
		}
		else if (e.getSource().equals(saveButton))
		{
			this.setValue(field.getText());
		}
	}
}
