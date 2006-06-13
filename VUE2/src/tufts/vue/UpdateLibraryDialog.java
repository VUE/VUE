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
 * @version $Revision: 1.2 $ / $Date: 2006-06-13 18:08:33 $ / $Author: jeff $
 * @author  akumar03
  */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;
import tufts.vue.gui.*;

public class UpdateLibraryDialog extends JDialog implements ListSelectionListener, ActionListener {
    
    JPanel addLibraryPanel = new JPanel();
    JList addLibraryList;
	JTextArea descriptionTextArea;
	DefaultListModel listModel = new DefaultListModel();
	JScrollPane listJsp;
	JScrollPane descriptionJsp;
	
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
	
	private static String TITLE = "Update a Resource";
	private static String AVAILABLE = "Resources available:";
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    
    public UpdateLibraryDialog(DataSourceList dataSourceList)
	{
        super(VUE.getDialogParentAsFrame(),TITLE,true);
		this.dataSourceList = dataSourceList;
		
		try {
			addLibraryList = new JList(listModel);
			addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
			addLibraryList.setPreferredSize(new Dimension(300,180));
			addLibraryList.addListSelectionListener(this);
			addLibraryList.setCellRenderer(new ProviderListCellRenderer());
			
			descriptionTextArea = new JTextArea();
			descriptionTextArea.setLineWrap(true);
			descriptionTextArea.setWrapStyleWord(true);
			descriptionTextArea.setPreferredSize(new Dimension(300,180));
			
			populate();

			listJsp = new JScrollPane(addLibraryList);
			listJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			listJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			
			descriptionTextArea.setText("description");
			descriptionJsp = new JScrollPane(descriptionTextArea);
			descriptionJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			descriptionJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 

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
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 2;
			addLibraryPanel.add(descriptionJsp,gbConstraints);
			
			buttonPanel.add(cancelButton);
			cancelButton.addActionListener(this);
			buttonPanel.add(addButton);
			addButton.addActionListener(this);
			getRootPane().setDefaultButton(addButton);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 3;
			addLibraryPanel.add(buttonPanel,gbConstraints);

			getContentPane().add(addLibraryPanel,BorderLayout.CENTER);
			pack();
			setLocation(300,300);
			//setSize(new Dimension(300,400));
			
			//addLibraryList.getSelectionMdoel().setSelectionInterval(0,1);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		setVisible(true);
    }

	public void refresh()
	{
		populate();
	}
	
    private void populate()
	{
		listModel.removeAllElements();
		try
		{
			if (dataSourceManager == null) {
				dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
				factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
			}
			
			listModel.removeAllElements();

			org.osid.provider.ProviderIterator providerIterator = factory.getProvidersNeedingUpdate();
			while (providerIterator.hasNextProvider()) {
				org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				// place all providers on list, whether installed or not, whether duplicates or not
				listModel.addElement(nextProvider);
				descriptionTextArea.setText(nextProvider.getDescription());
				checkedVector.addElement(nextProvider);
			}
			// copy to an array
			int size = listModel.size();
			checked = new org.osid.provider.Provider[size];
			for (int i=0; i < size; i++) {
				checked[i] = (org.osid.provider.Provider)checkedVector.elementAt(i);
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
			javax.swing.JOptionPane.showMessageDialog(null,
													  t.getMessage(),
													  "Error",
													  javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void valueChanged(ListSelectionEvent lse) {				
		int index = ((JList)lse.getSource()).getSelectedIndex();
		if (index != -1) {
			try {
				org.osid.provider.Provider p = (org.osid.provider.Provider)(((JList)lse.getSource()).getSelectedValue());
				descriptionTextArea.setText(p.getDescription());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("Update")) {
			try {
				Object o = addLibraryList.getSelectedValue();
				String xml = null;
				org.osid.provider.Provider provider = (org.osid.provider.Provider)o;
				
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
							if (javax.swing.JOptionPane.showOptionDialog(this,
																		 area,
																		 "License Acknowledgement",
																		 javax.swing.JOptionPane.DEFAULT_OPTION,
																		 javax.swing.JOptionPane.QUESTION_MESSAGE,
																		 null,
																		 new Object[] {
																			 "Accept", "Decline"
																		 },
																		 "Decline") != 0) {
								proceed = false;
							}
						}
					}
					
					if (proceed) {
						factory.updateProvider(provider.getId());
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
			
				if (xml != null) {
					edu.tufts.vue.ui.ConfigurationUI cui = 
					new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
					cui.setPreferredSize(new Dimension(400,200));
					
					if (javax.swing.JOptionPane.showOptionDialog(this,
																 cui,
																 "Configuration",
																 javax.swing.JOptionPane.DEFAULT_OPTION,
																 javax.swing.JOptionPane.QUESTION_MESSAGE,
																 null,
																 new Object[] {
																	 "Cancel", "Update"
																 },
																 "Update") != 0) {
						this.newDataSource.setConfiguration(cui.getProperties());
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else {
			setVisible(false);
		}
	}
}



