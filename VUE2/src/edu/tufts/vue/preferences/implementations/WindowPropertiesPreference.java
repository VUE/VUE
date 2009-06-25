/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import tufts.vue.VueResources;
import tufts.vue.VueUtil;
import edu.tufts.vue.preferences.ui.PreferencesDialog;

/**
 * @author Mike Korcynski
 *
 */
public class WindowPropertiesPreference extends StringPreference implements ItemListener, ActionListener {
	private static final boolean DEBUG = true;
	Hashtable table = new Hashtable();
	private static final String ENABLED_KEY = "ENABLED_KEY";
	private static final String VISIBLE_KEY = "VISIBLE_KEY";
	private static final String WIDTH_KEY = "WIDTH_KEY";
	private static final String HEIGHT_KEY = "HEIGHT_KEY";
	private static final String X_POS_KEY = "X_POS_KEY";
	private static final String Y_POS_KEY = "Y_POS_KEY";
	private static final String ROLLEDUP_KEY = "ROLLEDUP_KEY";
	private static final String defval = "true,false,-1,-1,-1,-1,false";
	private Preferences p2 = Preferences.userNodeForPackage(getPrefRoot());
	private static String defaultEnabledVal = "true";
	private String value;
	private static JCheckBox checkValue = new JCheckBox();
	private JButton resetButton = new JButton(VueResources.getString("button.reset.label"));
	private JPanel resetPanel = new JPanel();
	private JPanel panel = null; 

	/**
	 * The first parameter in the string used to store the window position was originally intended 
	 * to say whether the preference as a whole was enabled or disabled, this is now not used but 
	 * its kept there for legacy purpose.  There's now a new separate preference enableWinPos which 
	 * stores whether window positions should be stored if this is true, then the strings are used
	 * to mantain the positions. 
	 * 
	 * Also, this tool is very useful for exploring your Java preferences:
	 * 						http://freshmeat.net/projects/javaprefs/
	 * -MK
	 */
	protected WindowPropertiesPreference(String category, String key, String name, String desc, boolean showInUI) {
		super(category, key, name, desc, defval, showInUI);
		value = (String)getValue();
		StringTokenizer tokens = new StringTokenizer(value,",");						

		try {
			table.put(ENABLED_KEY, tokens.nextToken());	
			table.put(VISIBLE_KEY,tokens.nextToken());		
			table.put(WIDTH_KEY, tokens.nextToken());		
			table.put(HEIGHT_KEY,tokens.nextToken());
			table.put(X_POS_KEY,tokens.nextToken());
			table.put(Y_POS_KEY,tokens.nextToken());
			table.put(ROLLEDUP_KEY,tokens.nextToken());		
		} catch(NoSuchElementException nsee) {
			//nsee.printStackTrace();
			//this shouldn't happen but if it does stuff the table with the defaults
			table.put(ENABLED_KEY, "true");	
			table.put(VISIBLE_KEY,"false");		
			table.put(WIDTH_KEY, "-1");		
			table.put(HEIGHT_KEY,"-1");
			table.put(X_POS_KEY,"-1");
			table.put(Y_POS_KEY,"-1");
			table.put(ROLLEDUP_KEY,"false");
		}
		finally {
			getCheckBox().setSelected(isEnabled());
		}
		return;
	}

	public boolean isAllValuesDefaults() {
		if (	this.isEnabled() && 
				!this.isWindowVisible() && 
				this.getWindowSize().getHeight() == -1 && 
				this.getWindowSize().getWidth() == -1 &&
				this.getWindowLocationOnScreen().getX() == -1.0 &&
				this.getWindowLocationOnScreen().getY() == -1.0) {
			return true;
		} else
			return false;
	}

	public void itemStateChanged(ItemEvent e) {
		JCheckBox box = (JCheckBox)e.getSource();

		//System.out.println("ITEM STATE CHANGED");
		defaultEnabledVal = (new Boolean(box.isSelected()).toString());
		
		if (p2.get("enabledWinPos", "null").equals("null"))
			p2.put("enabledWinPos", defaultEnabledVal);
		else
			p2.put("enabledWinPos",defaultEnabledVal);
	
		defaultEnabledVal = p2.get("enabledWinPos",defaultEnabledVal);
		table.put(ENABLED_KEY, box.isSelected());
		configureResetPanel();
		panel.validate();
		panel.repaint();
	}

	public static WindowPropertiesPreference create(String category, String key, String name, String desc, boolean showInUI) {
		return new WindowPropertiesPreference(category,key,name,desc,showInUI);
	}

	//show in UI defaults to true
	public static WindowPropertiesPreference create(String category, String key, String name, String desc) {
		return new WindowPropertiesPreference(category,key,name,desc,true);
	}

	public void setEnabled(boolean enable) {
		//System.out.println("FLIPPING ENABLED?" + enable);
		defaultEnabledVal = Boolean.valueOf(enable).toString();

		if (p2.get("enabledWinPos", "null").equals("null"))
			p2.put("enabledWinPos", defaultEnabledVal);
		else
			p2.put("enabledWinPos",defaultEnabledVal);

		table.put(ENABLED_KEY, defaultEnabledVal);
	}

	public boolean isEnabled() {
		String set = p2.get("enabledWinPos", defaultEnabledVal);

		if (set.equals("reset")) {
			set = "true";
			p2.put("enabledWinPos", "true");
		}

		return new Boolean(set);//Boolean.valueOf((String)table.get(ENABLED_KEY));
	}

	public boolean isWindowVisible() {
		return Boolean.valueOf((String)table.get(VISIBLE_KEY));
	}

	public Dimension getWindowSize() {
		return new Dimension(Integer.valueOf((String)table.get(WIDTH_KEY)),Integer.valueOf((String)table.get(HEIGHT_KEY)));
	}

	public void actionPerformed(ActionEvent e) {
		try {
			String[] keys = p2.keys();
			for (int i = 0 ; i < keys.length; i++)
			{
				if (keys[i].startsWith("windows."))
					p2.remove(keys[i]);
			}

			String s = p2.get("enabledWinPos", "true");

			if (s.equals("true"))
				p2.put("enabledWinPos", "reset");

			VueUtil.alert(PreferencesDialog.getDialog(), VueResources.getString("dialog.preference.message"), VueResources.getString("dialog.preference.title"), JOptionPane.PLAIN_MESSAGE);
		} catch (BackingStoreException e2) {
			e2.printStackTrace();
		}		
	}
	
	public JComponent getPreferenceUI() {
		if (panel != null) {
			return panel;
		}

		panel = new JPanel();
		panel.setBackground(Color.WHITE);
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);

		JLabel titleLabel = new JLabel(getTitle());
		Font f = titleLabel.getFont().deriveFont(Font.BOLD);
		titleLabel.setFont(f);

		JLabel descLabel = new JLabel(getDescription());
		GridBagConstraints gbConstraints = new GridBagConstraints();
	  
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.gridwidth = 1;
		gbConstraints.fill = GridBagConstraints.HORIZONTAL;
		gbConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		gbConstraints.weightx = 1;
		gbConstraints.weighty = 0;
		gbConstraints.insets = new Insets(15, 10, 0, 10);

		panel.add(titleLabel, gbConstraints);

		gbConstraints.gridy = 1;
		panel.add(descLabel, gbConstraints);
		
		checkValue.addItemListener(this);
		checkValue.setBackground(Color.WHITE);
		checkValue.setSelected(isEnabled());
		checkValue.setText(getMessage());

		gbConstraints.gridy = 2;
		gbConstraints.insets = new Insets(15, 30, 0, 10);
		panel.add(checkValue, gbConstraints);

		FlowLayout flow = new FlowLayout(FlowLayout.LEADING);
		flow.setHgap(10);
		flow.setVgap(0);
		resetPanel.setLayout(flow);
		resetPanel.setBackground(Color.WHITE);
		resetPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.gray));

		resetButton.addActionListener(this);

		resetPanel.add(resetButton);
		resetPanel.add(new JLabel(VueResources.getString("jlabel.resetdefaultwindow")));

		gbConstraints.gridy = 3;
		gbConstraints.insets = new Insets(15, 20, 0, 10);
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.weighty = 1;
		panel.add(resetPanel, gbConstraints);
		configureResetPanel();

		if (DEBUG) {
			panel.setBackground(Color.CYAN);
			resetPanel.setBackground(Color.MAGENTA);
			titleLabel.setBackground(Color.YELLOW);
			titleLabel.setOpaque(true);
			descLabel.setBackground(Color.YELLOW);
			descLabel.setOpaque(true);
			checkValue.setBackground(Color.YELLOW);
			checkValue.setOpaque(true);
			resetButton.setBackground(Color.YELLOW);
			resetButton.setOpaque(true);
		}

		return panel;
	}

	private void configureResetPanel() {
		if ( p2.get("enabledWinPos", "true").equals("true") || p2.get("enabledWinPos", "true").equals("reset")) {
			resetPanel.setVisible(true);
		} else {
			resetPanel.setVisible(false);
		}
	}

	private static JCheckBox getCheckBox() {
		return checkValue;
	}

	public Point getWindowLocationOnScreen() {
		return new Point(Integer.valueOf((String)table.get(X_POS_KEY)),Integer.valueOf((String)table.get(Y_POS_KEY)));
	}

	public boolean isRolledUp() {
		return Boolean.valueOf((String)table.get(ROLLEDUP_KEY));
	}

	public void updateWindowProperties(boolean visible, int width, int height, int x, int y, boolean rolled) {
		//table.put(ENABLED_KEY, "true");
		table.put(VISIBLE_KEY, Boolean.valueOf(visible).toString());
		table.put(WIDTH_KEY, Integer.valueOf(width).toString());
		table.put(HEIGHT_KEY, Integer.valueOf(height).toString());
		table.put(X_POS_KEY, Integer.valueOf(x).toString());
		table.put(Y_POS_KEY, Integer.valueOf(y).toString());
		table.put(ROLLEDUP_KEY, Boolean.valueOf(rolled).toString());

		StringBuffer sb = new StringBuffer();
		String enabled = p2.get("enabledWinPos", defaultEnabledVal);

		sb.append(enabled);

		if (!enabled.equals("reset")) {	
			sb.append(",");
			sb.append(Boolean.valueOf(visible).toString());
			sb.append(",");
			sb.append(Integer.valueOf(width).toString());
			sb.append(",");
			sb.append(Integer.valueOf(height).toString());
			sb.append(",");
			sb.append(Integer.valueOf(x).toString());
			sb.append(",");
			sb.append(Integer.valueOf(y).toString());		
			sb.append(",");
			sb.append(Boolean.valueOf(rolled).toString());
			//	System.out.println(sb.toString());
		}
		setValue(sb.toString());
	}
}
