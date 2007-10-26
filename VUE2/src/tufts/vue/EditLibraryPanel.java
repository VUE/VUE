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
	JButton updateButton = new JButton("Save");
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
			cui.setProperties(dataSource.getConfiguration());

			// layout container
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			gbConstraints.ipadx=110;
			gbConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			setLayout(gridbag);
			//cui.setBorder(BorderFactory.createLineBorder(Color.black));
		//	this.setBorder(BorderFactory.createLineBorder(Color.red));
			add(cui,gbConstraints);
			updateButton.addActionListener(this);
			gbConstraints.ipadx=15;
			gbConstraints.gridy = 1;
			gbConstraints.anchor=GridBagConstraints.NORTHEAST;
			gbConstraints.fill=GridBagConstraints.NONE;
			add(updateButton,gbConstraints);
			gbConstraints.ipadx=0;
			gbConstraints.gridx=1;
			gbConstraints.gridy=0;
			gbConstraints.weightx=1;
			gbConstraints.weighty=1;
			gbConstraints.gridheight=2;
			gbConstraints.fill=GridBagConstraints.HORIZONTAL;
			add(new JPanel(),gbConstraints);
			
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
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Starting path</title><description>The path to start from</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>512</maxChars><ui>8</ui></field></configuration>";
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
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars>0<ui>0</ui></field><field><key>address</key><title>Address</title><description>FTP Address</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>256</maxChars><ui>0</ui></field><field><key>username</key><title>Username</title><description>FTP site username</description><default>DEFAULT_USERNAME</default><mandatory>true</mandatory><maxChars>64</maxChars><ui>9</ui></field><field><key>password</key><title>Password</title><description>FTP site password for username</description><default>DEFAULT_PASSWORD</default><mandatory>true</mandatory><maxChars></maxChars><ui>1</ui></field></configuration>";
				RemoteFileDataSource ds = (RemoteFileDataSource)dataSource;
				String name = ds.getDisplayName();
				if (name == null) name = "";
				String address = ds.getAddress();
				if (address == null) address = "";
				String username = ds.getUserName();
				if (username == null) username = RemoteFileDataSource.ANONYMOUS;
				String password = ds.getPassword();
				if (password == null) password = "";
				xml = xml.replaceFirst("DEFAULT_NAME",name);
				xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
				xml = xml.replaceFirst("DEFAULT_USERNAME",username);
				xml = xml.replaceFirst("DEFAULT_PASSWORD",password);
			} else if (dataSource instanceof edu.tufts.vue.rss.RSSDataSource) {
                            edu.tufts.vue.rss.RSSDataSource ds =  (edu.tufts.vue.rss.RSSDataSource) dataSource;
                            xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>RSS Url</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>1000</maxChars><ui>0</ui></field></configuration>";
                            String name = ds.getDisplayName();
                            if (name == null) name = "";
                            String address = ds.getAddress();
                            if (address == null) address = "";
                            xml = xml.replaceFirst("DEFAULT_NAME",name);
                            xml = xml.replaceFirst("DEFAULT_ADDRESS",address);	
                        }
			
			cui = new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
			
			// layout container
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.gridx = 0;
			gbConstraints.ipadx=110;
			gbConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gbConstraints.gridy = 0;
			setLayout(gridbag);
			//cui.setBorder(BorderFactory.createLineBorder(Color.green));
			//this.setBorder(BorderFactory.createLineBorder(Color.red));
			add(cui,gbConstraints);
			updateButton.addActionListener(this);
			gbConstraints.gridy = 1;
			gbConstraints.ipadx=15;
			gbConstraints.anchor=GridBagConstraints.NORTHEAST;
			gbConstraints.fill=GridBagConstraints.NONE;
			add(updateButton,gbConstraints);
			gbConstraints.gridx=1;
			gbConstraints.ipadx=0;
			gbConstraints.gridy=0;
			gbConstraints.weightx=1;
			gbConstraints.weighty=1;
			gbConstraints.gridheight=2;
			gbConstraints.fill=GridBagConstraints.REMAINDER;
			add(new JPanel(),gbConstraints);
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
					edu.tufts.vue.dsm.DataSourceManager dsm = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
					dsm.save();					
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
					ds.setAddress(p.getProperty("address")); // this must be set last
                                        ds.setPassword(p.getProperty("password"));
					this.dsv.setActiveDataSource(this.oldDataSource); // refresh
				} else if (this.oldDataSource instanceof edu.tufts.vue.rss.RSSDataSource) {
                                        java.util.Properties p = cui.getProperties();
                                        edu.tufts.vue.rss.RSSDataSource ds = (edu.tufts.vue.rss.RSSDataSource)this.oldDataSource;
                                        ds.setDisplayName(p.getProperty("name"));
                                        ds.setAddress(p.getProperty("address"));
                                        this.dsv.setActiveDataSource(this.oldDataSource);
                                }
			}
                        DataSourceViewer.saveDataSourceViewer();
		} catch (Throwable t) {
			t.printStackTrace();
			VueUtil.alert("Configuration error: "+ t.getMessage(),"Error");
		}
	}
}