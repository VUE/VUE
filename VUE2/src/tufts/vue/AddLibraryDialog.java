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
/*
 * AddEditDataSourceDialog.java
 * The UI to Add/Edit Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
 * @version $Revision: 1.11 $ / $Date: 2006-05-25 21:20:10 $ / $Author: jeff $
 * @author  akumar03
  */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class AddLibraryDialog extends JDialog implements ListSelectionListener, ActionListener {
    
    JPanel addLibraryPanel = new JPanel();
    JList addLibraryList;
	DefaultListModel listModel = new DefaultListModel();
	JScrollPane listJsp;
	JScrollPane descriptionJsp;
	JLabel libraryIcon;
	JTextArea libraryDescription;
	edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
	edu.tufts.vue.dsm.OsidFactory factory;
	org.osid.provider.Provider checked[];
	java.util.Vector checkedVector = new java.util.Vector();
	JButton addButton = new JButton("Add");
	JButton cancelButton = new JButton("Done");
	JPanel buttonPanel = new JPanel();
	DataSourceList dataSourceList;
	DataSource oldDataSource = null;
	edu.tufts.vue.dsm.DataSource newDataSource = null;
	
	private static String MY_COMPUTER = "My Computer";
	private static String MY_COMPUTER_DESCRIPTION = "Add a browse control for your filesystem.  You can configure where to start the tree.";
	private static String MY_SAVED_CONTENT = "My Saved Content";
	private static String MY_SAVED_CONTENT_DESCRIPTION = "Add a browse control for your saved content.  You can configure a name for this source.";
	private static String FTP = "FTP";
	private static String FTP_DESCRIPTION = "Add a browse control for an FTP site.  You must configure this.";
	private static String TITLE = "ADD A LIBRARY";
	private static String AVAILABLE = "Available";
    
    public AddLibraryDialog(DataSourceList dataSourceList)
	{
        super(VUE.getDialogParentAsFrame(),TITLE,true);
		this.dataSourceList = dataSourceList;
		
		try {
			addLibraryList = new JList(listModel);
			addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
			addLibraryList.setPreferredSize(new Dimension(160,180));
			addLibraryList.addListSelectionListener(this);
			
			populate();
			listJsp = new JScrollPane(addLibraryList);
			//if (this.settings.isMac()) 
			{ 
				listJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				listJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
			}
			
			if (checked.length > 0) {
				// TODO: Replace this with an image, but we need this in the Provider not just the Data Source
				libraryIcon = new JLabel(VueResources.getImageIcon("NoImage"));
				libraryDescription = new JTextArea(checked[0].getDescription());
				addLibraryList.setSelectedIndex(0);
			} else {
				libraryIcon = new JLabel(VueResources.getImageIcon("NoImage"));
				libraryDescription = new JTextArea();
			}

			libraryIcon.setPreferredSize(new Dimension(60,60));
			libraryDescription.setLineWrap(true);
			libraryDescription.setWrapStyleWord(true);
			
			descriptionJsp = new JScrollPane(libraryDescription);
			//if (this.settings.isMac()) 
			{ 
				descriptionJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				descriptionJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
			}

			libraryDescription.setPreferredSize(new Dimension(220,140));			
			
			addLibraryPanel.setBackground(VueResources.getColor("White"));
			setBackground(VueResources.getColor("White"));

			java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
			java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			addLibraryPanel.setLayout(gbLayout);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			addLibraryPanel.add(new JLabel(AVAILABLE),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 1;
			addLibraryPanel.add(listJsp,gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.gridy = 1;
			addLibraryPanel.add(libraryIcon,gbConstraints);
			
			gbConstraints.gridx = 2;
			gbConstraints.gridy = 1;
			addLibraryPanel.add(descriptionJsp,gbConstraints);
			
			buttonPanel.add(cancelButton);
			cancelButton.addActionListener(this);
			buttonPanel.add(addButton);
			addButton.addActionListener(this);
			getRootPane().setDefaultButton(addButton);
			
			gbConstraints.gridx = 2;
			gbConstraints.gridy = 2;
			addLibraryPanel.add(buttonPanel,gbConstraints);

			getContentPane().add(addLibraryPanel,BorderLayout.CENTER);
			pack();
			setLocation(300,300);
			setSize(new Dimension(550,350));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		setVisible(true);
    }

    private void populate()
	{
		try
		{
			if (dataSourceManager == null) {
				dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
				factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
			}
			
			listModel.removeAllElements();
			org.osid.provider.ProviderIterator providerIterator = factory.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				// place all providers on list, whether installed or not, whether duplicates or not
				listModel.addElement(nextProvider.getDisplayName());
				checkedVector.addElement(nextProvider);
			}
			// copy to an array
			int size = listModel.size();
			checked = new org.osid.provider.Provider[size];
			for (int i=0; i < size; i++) {
				checked[i] = (org.osid.provider.Provider)checkedVector.elementAt(i);
			}
			
			// add all data sources we include with VUE
			listModel.addElement(MY_COMPUTER);
			listModel.addElement(MY_SAVED_CONTENT);
			listModel.addElement(FTP);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
    public String toString() 
	{
        return "AddLibraryDialog";
    }

	public void valueChanged(ListSelectionEvent lse) {				
		int index = ((JList)lse.getSource()).getSelectedIndex();
		if (index != -1) {
			try {
				// TODO: update icon
				String s = (String)(((JList)lse.getSource()).getSelectedValue());
				if (s.equals(MY_COMPUTER)) {
					libraryDescription.setText(MY_COMPUTER_DESCRIPTION);
				} else if (s.equals(MY_SAVED_CONTENT)) {
					libraryDescription.setText(MY_SAVED_CONTENT_DESCRIPTION);
				} else if (s.equals(FTP)) {
					libraryDescription.setText(FTP_DESCRIPTION);
				} else {
					libraryDescription.setText(checked[index].getDescription());
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("Add")) {
			try {
				this.oldDataSource = null;
				int index = addLibraryList.getSelectedIndex();
				String s = (String)addLibraryList.getSelectedValue();
				String xml = null;
				if (s.equals(MY_COMPUTER)) {
					LocalFileDataSource ds = new LocalFileDataSource(MY_COMPUTER,"");
					xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Starting path</title><description>The path to start from</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>512</maxChars><ui>0</ui></field></configuration>";
					String name = ds.getDisplayName();
					String address = ds.getAddress();
					xml = xml.replaceFirst("DEFAULT_NAME",name);
					xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
					this.oldDataSource = ds;
				} else if (s.equals(MY_SAVED_CONTENT)) {
					FavoritesDataSource ds = new FavoritesDataSource(MY_SAVED_CONTENT);
					xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field></configuration>";
					String name = ds.getDisplayName();
					xml = xml.replaceFirst("DEFAULT_NAME",name);
					this.oldDataSource = ds;
				} else if (s.equals(FTP)) {
					RemoteFileDataSource ds = new RemoteFileDataSource();
					xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Dane for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>FTP Address</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>256</maxChars><ui>0</ui></field><field><key>username</key><title>Username</title><description>FTP site username</description><default>DEFAULT_USERNAME</default><mandatory>true</mandatory><maxChars>64</maxChars><ui>0</ui></field><field><key>password</key><title>Password</title><description>FTP site password for username</description><default>DEFAULT_PASSWORD</default><mandatory>true</mandatory><maxChars></maxChars><ui>1</ui></field></configuration>";
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
					this.oldDataSource = ds;
				} else {					
					org.osid.provider.Provider provider = (org.osid.provider.Provider)this.checkedVector.elementAt(index);
					
					boolean proceed = true;
					edu.tufts.vue.dsm.DataSource ds = null;
					// show dialog containing license, if any
					try {
						if (provider.requestsLicenseAcknowledgement()) {
							String license = provider.getLicense();
							if (license != null) {
								javax.swing.JTextArea area = new javax.swing.JTextArea();
								area.setLineWrap(true);
								area.setText(license);
								area.setEditable(false);
								area.setSize(new Dimension(500,300));
								if (javax.swing.JOptionPane.showOptionDialog(VUE.getDialogParent(),
																			 area,
																			 "License Acknowledgement",
																			 javax.swing.JOptionPane.DEFAULT_OPTION,
																			 javax.swing.JOptionPane.QUESTION_MESSAGE,
																			 null,
																			 new Object[] {
																				 "Accept", "Decline"
																			 },
																			 "Decline") != 0) {
									
									System.out.println("Accept or Decline: Decline");
									proceed = false;
								} else {
									System.out.println("Accept or Decline: Accept");
								}
							}
						}
						
						if (proceed && provider.isInstalled()) { //To Do: reverse this
							factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
							factory.installProvider(provider.getId());
						}
						
						if (proceed) {
							// add to data sources list
							ds = new edu.tufts.vue.dsm.impl.VueDataSource(provider.getId(),true);
							//dataSourceManager.add(ds);
							ds.setIncludedInSearch(true);
							dataSourceList.addOrdered(ds);
							DataSourceViewer.refreshDataSourcePanel(ds);
							
							// show configuration, if needed
							if (ds.hasConfiguration()) {
								xml = ds.getConfigurationUIHints();
								this.newDataSource = ds;
							} else {
								System.out.println("No configuration to show");
							}
							
						}
					} catch (Throwable t) {
						javax.swing.JOptionPane.showMessageDialog(null,
																  t.getMessage(),
																  "OSID Installation Error",
																  javax.swing.JOptionPane.ERROR_MESSAGE);
						t.printStackTrace();
						return;
					}
				}
			
				if (xml != null) {
					edu.tufts.vue.ui.ConfigurationUI cui = 
						new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
					cui.setPreferredSize(new Dimension(400,200));

					if (javax.swing.JOptionPane.showOptionDialog(VUE.getDialogParent(),
																 cui,
																 "Configuration",
																 javax.swing.JOptionPane.DEFAULT_OPTION,
																 javax.swing.JOptionPane.QUESTION_MESSAGE,
																 null,
																 new Object[] {
																	 "Cancel", "Update"
																 },
																 "Update") != 0) {
						if (s.equals(MY_COMPUTER)) {
							java.util.Properties p = cui.getProperties();
							LocalFileDataSource ds = (LocalFileDataSource)this.oldDataSource;
							ds.setDisplayName(p.getProperty("name"));
							ds.setAddress(p.getProperty("address"));
						} else if (s.equals(MY_SAVED_CONTENT)) {
							java.util.Properties p = cui.getProperties();
							FavoritesDataSource ds = (FavoritesDataSource)this.oldDataSource;
							ds.setDisplayName(p.getProperty("name"));
						} else if (s.equals(FTP)) {
							java.util.Properties p = cui.getProperties();
							RemoteFileDataSource ds = (RemoteFileDataSource)this.oldDataSource;
							ds.setDisplayName(p.getProperty("name"));
							ds.setUserName(p.getProperty("username"));
							ds.setPassword(p.getProperty("password"));
							try {
								ds.setAddress(p.getProperty("address")); // this must be set last
							} catch (Exception ex) {
								// ignore any error for now
							}
						} else {
							this.newDataSource.setConfiguration(cui.getProperties());
						}
					}
				}
				if (this.oldDataSource != null) {
					dataSourceList.addOrdered(this.oldDataSource);
				} else {
					dataSourceList.addOrdered(this.newDataSource);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else {
			setVisible(false);
		}
	}
}



