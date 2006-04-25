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

package tufts.vue;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class EditLibraryPanel extends JPanel implements ActionListener, FocusListener
{
	JButton updateButton = new JButton("Update");
	JTextField fields[] = null;
	edu.tufts.vue.dsm.DataSource dataSource = null;
	String originalValue = null;
	
	public EditLibraryPanel(edu.tufts.vue.dsm.DataSource dataSource)
	{
		try {
			this.dataSource = dataSource;
			
			// Get keys and values.  These will be the same length.  Keys will not be null or 0-length.
			// Values may be null (no-default) or non-null (default value).
			String keys[] = dataSource.getConfigurationKeys();
			String values[] = dataSource.getConfigurationValues();
			java.util.Map maps[] = dataSource.getConfigurationMaps();
			
			this.fields = new JTextField[keys.length];
			for (int i=0; i < keys.length; i++) {
				// special case for password fields
				if (maps[i].containsKey("password")) {
					if ( ((Boolean)maps[i].get("password")).booleanValue() ) {
						this.fields[i] = new JPasswordField();
					} else {
						this.fields[i] = new JTextField();
					}
				} else {
					this.fields[i] = new JTextField();
				}
				
				// special case for columns property
				if (maps[i].containsKey("columns")) {
					this.fields[i].setColumns( ((Integer)maps[i].get("columns")).intValue() );
				} else {
					this.fields[i].setColumns(20);
				}
				
				// rest of the initialization
				String value = values[i];
				if (value != null) {
					this.fields[i].setText(value);
				}
				
				// setup listeners to enable the update button
				this.updateButton.setEnabled(false);
				this.fields[i].addFocusListener(this);
				this.fields[i].addActionListener(this);
			}
			
			// layout container
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			setLayout(gridbag);
			
			// populate container
			for (int i=0; i < keys.length; i++) {
				add(new JLabel(keys[i] + ": "),gbConstraints);
				gbConstraints.gridx++;
				add(this.fields[i],gbConstraints);
				gbConstraints.gridy++;
				gbConstraints.gridx = 0;
			}
			
			updateButton.addActionListener(this);
			gbConstraints.gridx = 1;
			add(updateButton,gbConstraints);
		} catch (Throwable t) {
			
		}
	}
	
	public void focusGained(FocusEvent fe)
	{
		JTextField tf = (JTextField)fe.getSource();
		this.originalValue = tf.getText();
	}
	
	public void focusLost(FocusEvent fe)
	{
		checkForNewData( (JTextField)fe.getSource() );
	}

	private void checkForNewData(JTextField tf)
	{
		String currentValue = tf.getText();
		if (!currentValue.equals(this.originalValue)) {
			updateButton.setEnabled(true);
		}
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() instanceof JButton) {
			System.out.println("Setting values");
			this.dataSource.setConfigurationValues(getValues());
			updateButton.setEnabled(false);
		} else {
			checkForNewData( (JTextField)ae.getSource() );
		}
	}
	
	// get whatever is in the value fields, coerce a blank to a null
	public String[] getValues()
	{
		String values[] = new String[0];
		
		if (this.fields != null) {
			values = new String[this.fields.length];
			for (int i=0; i < this.fields.length; i++) {
				String value = this.fields[i].getText().trim();
				if (value.length() == 0) {
					values[i] = null;
				} else {
					values[i] = value.trim();
				}
			}
		}
		return values;
	}
}