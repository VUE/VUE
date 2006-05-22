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

public class EditLibraryPanel extends JPanel implements ActionListener
{
	JButton updateButton = new JButton("Update");
	JTextField fields[] = null;
	edu.tufts.vue.dsm.DataSource dataSource = null;
	DataSource oldDataSource = null;
	String originalValue = null;
	edu.tufts.vue.ui.ConfigurationUI cui = null;
	DataSourceViewer dsv = null;
	
	public EditLibraryPanel(DataSourceViewer dsv,
							edu.tufts.vue.dsm.DataSource dataSource)
	{
		try {
			this.dsv = dsv;
			this.dataSource = dataSource;
			
			String xml = dataSource.getConfigurationUIHints();
			cui = new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));

			// layout container
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			setLayout(gridbag);
			
			add(cui,gbConstraints);
			updateButton.addActionListener(this);
			gbConstraints.gridy = 1;
			add(updateButton,gbConstraints);
		} catch (Throwable t) {
			
		}
	}

	public EditLibraryPanel(DataSourceViewer dsv,
							DataSource dataSource)
	{
		try {
			this.dsv = dsv;
			this.oldDataSource = dataSource;
			
			// use canned configurations -- substitue current values for defaults
			String xml = null;
			if (dataSource instanceof LocalFileDataSource) {
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Starting path</title><description>The path to start from</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>512</maxChars><ui>0</ui></field></configuration>";
				LocalFileDataSource ds = (LocalFileDataSource)dataSource;
				String name = ds.getDisplayName();
				String address = ds.getAddress();
				xml = xml.replaceFirst("DEFAULT_NAME",name);
				xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
			} else if (dataSource instanceof FavoritesDataSource) {
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field></configuration>";
				FavoritesDataSource ds = (FavoritesDataSource)dataSource;
				String name = ds.getDisplayName();
				xml = xml.replaceFirst("DEFAULT_NAME",name);
			} else if (dataSource instanceof RemoteFileDataSource) {
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Dane for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>FTP Address</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>256</maxChars><ui>0</ui></field><field><key>username</key><title>Username</title><description>FTP site username</description><default>DEFAULT_USERNAME</default><mandatory>true</mandatory><maxChars>64</maxChars><ui>0</ui></field><field><key>password</key><title>Password</title><description>FTP site password for username</description><default>DEFAULT_PASSWORD</default><mandatory>true</mandatory><maxChars></maxChars><ui>1</ui></field></configuration>";
				RemoteFileDataSource ds = (RemoteFileDataSource)dataSource;
				String name = ds.getDisplayName();
				if (name == null) name = "";
				String address = ds.getAddress();
				if (address == null) address = "";
				String username = ds.getUserName();
				if (username == null) username = "";
				String password = ds.getPassword();
				if (password == null) password = "";
				xml = xml.replaceFirst("DEFAULT_NAME",name);
				xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
				xml = xml.replaceFirst("DEFAULT_USERNAME",username);
				xml = xml.replaceFirst("DEFAULT_PASSWORD",password);
			}
			
			cui = new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
			
			// layout container
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			setLayout(gridbag);
			
			add(cui,gbConstraints);
			updateButton.addActionListener(this);
			gbConstraints.gridy = 1;
			add(updateButton,gbConstraints);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		try {
			if (ae.getSource() instanceof JButton) {
				if (this.dataSource != null) {
					this.dataSource.setConfiguration(cui.getProperties());
					this.dsv.setActiveDataSource(this.dataSource); // refresh
				} else if (this.oldDataSource instanceof LocalFileDataSource) {
					java.util.Properties p = cui.getProperties();
					LocalFileDataSource ds = (LocalFileDataSource)this.oldDataSource;
					ds.setDisplayName(p.getProperty("name"));
					ds.setAddress(p.getProperty("address"));
					this.dsv.setActiveDataSource(this.oldDataSource); // refresh
				} else if (this.oldDataSource instanceof FavoritesDataSource) {
					java.util.Properties p = cui.getProperties();
					FavoritesDataSource ds = (FavoritesDataSource)this.oldDataSource;
					ds.setDisplayName(p.getProperty("name"));
					this.dsv.setActiveDataSource(this.oldDataSource); // refresh
				} else if (this.oldDataSource instanceof RemoteFileDataSource) {
					java.util.Properties p = cui.getProperties();
					RemoteFileDataSource ds = (RemoteFileDataSource)this.oldDataSource;
					ds.setDisplayName(p.getProperty("name"));
					ds.setUserName(p.getProperty("username"));
					ds.setPassword(p.getProperty("password"));
					ds.setAddress(p.getProperty("address")); // this must be set last
					this.dsv.setActiveDataSource(this.oldDataSource); // refresh
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			JOptionPane.showMessageDialog(VUE.getDialogParent(),"Configuration error: "+ t.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
		}
	}
}