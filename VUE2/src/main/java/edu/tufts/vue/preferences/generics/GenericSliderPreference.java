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

package edu.tufts.vue.preferences.generics;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * @author Mike Korcynski
 *
 */
public abstract class GenericSliderPreference extends BasePref implements ChangeListener {
	private static final boolean DEBUG = false;
	private String message;
	private JSlider slider = new JSlider();
	private Object previousValue = null;

	public GenericSliderPreference() {
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		slider.setBackground(Color.WHITE);
	}

	public JSlider getSlider() {
		return slider;
	}

	public abstract String getTitle();
	public abstract String getDescription();

	public JComponent getPreferenceUI() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);

		JLabel titleLabel = new JLabel(getTitle());
		Font f = titleLabel.getFont().deriveFont(Font.BOLD);
		titleLabel.setFont(f);

		JTextArea descTextArea = new JTextArea(getDescription());
		final Font defaultFont = panel.getFont();
		descTextArea.setFont(defaultFont);
		descTextArea.setColumns(30);
		descTextArea.setLineWrap(true);
		descTextArea.setWrapStyleWord(true);

		GridBagConstraints gbConstraints = new GridBagConstraints();
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.gridwidth = 1;
		gbConstraints.gridheight = 1;
		gbConstraints.fill=GridBagConstraints.HORIZONTAL;
		gbConstraints.anchor=GridBagConstraints.FIRST_LINE_START;
		gbConstraints.weightx=1;
		gbConstraints.weighty=0;
		gbConstraints.insets = new Insets(15,10,2,10);
		panel.add(titleLabel, gbConstraints);

		gbConstraints.gridy = 1;
		panel.add(descTextArea, gbConstraints);

		JPanel booleanPanel = new JPanel();
		String msg = getMessage();
		JLabel msgLabel = new JLabel(msg);

		booleanPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		booleanPanel.setBackground(Color.WHITE);
		booleanPanel.add(slider);
		booleanPanel.add(msgLabel);

		gbConstraints.gridy=2;
		gbConstraints.fill=GridBagConstraints.BOTH;
		gbConstraints.weighty=1;
		gbConstraints.insets = new Insets(15, 30, 15, 10);
		panel.add(booleanPanel, gbConstraints);

		if (DEBUG) {
			panel.setBackground(Color.GREEN);
			titleLabel.setOpaque(true);
			titleLabel.setBackground(Color.CYAN);
			descTextArea.setOpaque(true);
			descTextArea.setBackground(Color.MAGENTA);
			slider.setOpaque(true);
			slider.setBackground(Color.YELLOW);
			msgLabel.setOpaque(true);
			msgLabel.setBackground(Color.ORANGE);
			booleanPanel.setOpaque(true);
			booleanPanel.setBackground(Color.BLUE);

			if (DEBUG && msg == null) {
				msgLabel.setText("test message");
			}
		}

		return panel;
	}

	public String getMessage(){
		return message;
	}

	public Object getPreviousValue() {
		return (previousValue == null ? getDefaultValue() : previousValue);
	}

	public int getSliderValueMappedToPref() {
		return 0;
	}

	public Object getValue() {
		 Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		 return p.getInt(getPrefName(), ((Integer)getDefaultValue()).intValue());
	}		

	public void setMessage(String s) {
		this.message = s;
	}

	public void setValue(Object i) {	
		previousValue = getValue();
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		p.putInt(getPrefName(), getSliderValueMappedToPref());					
		_fireVuePrefEvent();
	}
}
