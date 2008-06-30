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
/*
 * UpdateEditDataSourceDialog.java
 * The UI to Update/Edit Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
* @version $Revision: 1.25 $ / $Date: 2008-06-30 20:52:56 $ / $Author: mike $
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
    JButton addButton = new JButton("Update");
    JButton cancelButton = new JButton("Done");
    JPanel buttonPanel = new JPanel();
    DataSourceList dataSourceList;
    edu.tufts.vue.dsm.DataSource newDataSource = null;
	edu.tufts.vue.dsm.DataSource dataSourceThatWasSelectedForUpdate = null;
    
    private static String TITLE = VueResources.getString("updateLibrary.dialogTitle");
    private static String AVAILABLE = "Resources available:";
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private ProviderListCellRenderer providerListRenderer;
    private Timer timer;
    
    public UpdateLibraryDialog(DataSourceList dataSourceList,
							   edu.tufts.vue.dsm.DataSource dataSourceThatWasSelectedForUpdate) {
        super(VUE.getDialogParentAsFrame(),TITLE,true);
        this.dataSourceList = dataSourceList;
		this.dataSourceThatWasSelectedForUpdate = dataSourceThatWasSelectedForUpdate;
        
        try {
            factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert(this,"Error instantiating Provider support","Error");
        }
        
        try {
        	VueLabel helpButton = new VueLabel(VueResources.getImageIcon("addLibrary.helpIcon"));
            helpButton.setToolTipText("Help Text");
            
            String helpText = VueResources.getString("updateLibrary.helpText");
            
            if (helpText != null)
                helpButton.setToolTipText(helpText);
            
            getContentPane().setLayout(new GridBagLayout());
            addLibraryList = new JList(listModel);
            addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
            addLibraryList.addListSelectionListener(this);
            addLibraryList.setFixedCellHeight(25);
            
            providerListRenderer = new ProviderListCellRenderer();
            addLibraryList.setCellRenderer(providerListRenderer);
            
            descriptionTextArea = new JTextArea();
            descriptionTextArea.setEditable(false);
            descriptionTextArea.setMargin(new Insets(4,4,4,4));
            descriptionTextArea.setLineWrap(true);
            descriptionTextArea.setWrapStyleWord(true);
            
            populate();
            
            listJsp = new JScrollPane(addLibraryList);
            listJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            listJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            listJsp.setPreferredSize(new Dimension(300,180));
            
            descriptionTextArea.setText("description");
            descriptionJsp = new JScrollPane(descriptionTextArea);
            descriptionJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            descriptionJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            descriptionJsp.setPreferredSize(new Dimension(300,180));
            
            addLibraryPanel.setBackground(VueResources.getColor("White"));
            setBackground(VueResources.getColor("White"));
            
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
            
            
            gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gbConstraints.insets = new java.awt.Insets(2,2,2,2);
            addLibraryPanel.setLayout(gbLayout);
            
            JLabel avail = new JLabel(AVAILABLE);
            JPanel availabilityPanel = new JPanel();            
            availabilityPanel.setLayout(new BorderLayout());
            availabilityPanel.add(avail,BorderLayout.CENTER);
            availabilityPanel.add(helpButton,BorderLayout.EAST);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 0;
            gbConstraints.gridwidth = 1;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weightx=1;
            gbConstraints.weighty=1;
            gbConstraints.insets = new Insets(4,15,4,15);
            addLibraryPanel.add(availabilityPanel,gbConstraints);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 1;
            gbConstraints.weighty=1;
            addLibraryPanel.add(listJsp,gbConstraints);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 2;
            addLibraryPanel.add(descriptionJsp,gbConstraints);
            
            buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));            
            
            buttonPanel.add(Box.createHorizontalGlue());
            cancelButton.setPreferredSize(addButton.getPreferredSize());
            buttonPanel.add(cancelButton);            
            buttonPanel.add(Box.createHorizontalStrut(6));
            cancelButton.addActionListener(this);

            buttonPanel.add(addButton);
            addButton.addActionListener(this);

            /*buttonPanel.add(cancelButton);
            
            cancelButton.setMinimumSize(new Dimension(80,25));
            cancelButton.setPreferredSize(new Dimension(80,25));
            cancelButton.addActionListener(this);            
            
            buttonPanel.add(addButton);
            
            addButton.setMinimumSize(new Dimension(80,25));
            addButton.setPreferredSize(new Dimension(80,25));
            addButton.addActionListener(this);
            */
            getRootPane().setDefaultButton(addButton);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 3;
            gbConstraints.weighty=0;            
            gbConstraints.anchor=GridBagConstraints.EAST;
            addLibraryPanel.add(buttonPanel,gbConstraints);
            
            gbConstraints.gridx=0;
            gbConstraints.gridy=0;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weighty=1;
            getContentPane().add(addLibraryPanel,gbConstraints);
            
            pack();
            setResizable(false);
            setLocation(300,300);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        setVisible(true);
    }
    
    public void refresh() {
        populate();
    }
    
    private void populate() {
        listModel.removeAllElements();
		this.newDataSource = null;
        try {
            GUI.activateWaitCursor();
            if (dataSourceManager == null) {
                dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            }
            
            listModel.removeAllElements();
            descriptionTextArea.setText("");
			// find the ids of all providers in VUE
			java.util.Vector installedProviderVector = new java.util.Vector();
			edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
			for (int i=0; i < dataSources.length; i++) {
				installedProviderVector.addElement(dataSources[i].getProviderId().getIdString());
			}
			
            org.osid.provider.ProviderIterator providerIterator = factory.getProvidersNeedingUpdate();
            while (providerIterator.hasNextProvider()) {
                org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
                // place only providers that are already added to VUE
				String idString = nextProvider.getId().getIdString();
				if (installedProviderVector.contains(idString)) {
					// futher check that we do not already have the update 
					nextProvider = nextProvider.getNextVersion();
					if (!(installedProviderVector.contains(nextProvider.getId().getIdString()))) {
						listModel.addElement(nextProvider);
						checkedVector.addElement(nextProvider);
					}
				}
            }
            // copy to an array
            int size = listModel.size();
            checked = new org.osid.provider.Provider[size];
            for (int i=0; i < size; i++) {
                checked[i] = (org.osid.provider.Provider)checkedVector.elementAt(i);
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert(this,t.getMessage(),"Error");
        } finally {
            GUI.clearWaitCursor();
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
    
    public void add() {
        
        try {
			boolean proceed = true;
            Object o = addLibraryList.getSelectedValue();
            String xml = null;
            String s = null;
            
			org.osid.provider.Provider provider = (org.osid.provider.Provider)o;
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
							return;
						}
					}
				}
				
				//System.out.println("checking if installed");
				//System.out.println("Version " + provider.getVersion());
				if (proceed && (!provider.isInstalled())) {
					//System.out.println("installing...");
					factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
					try {
						GUI.activateWaitCursor();
						factory.installProvider(provider.getId());
					} catch (Throwable t1) {
						//System.out.println("install failed " + provider.getId().getIdString());
						VueUtil.alert(this,"Installation Failed","Error");
						return;
					} finally {
						GUI.clearWaitCursor();
					}
				} else {
					//System.out.println("No need to install");
				}
				
				if (proceed) {
					// add to data sources list
					try {
						//System.out.println("creating data source");
						ds = new edu.tufts.vue.dsm.impl.VueDataSource(factory.getIdManagerInstance().createId(),
																	  provider.getId(),
																	  true);
					} catch (Throwable t) {
						VueUtil.alert(this,"Loading Manager Failed","Error");
						return;
					}
					//System.out.println("created data source");
					
					// show configuration, if needed
					if (ds.hasConfiguration()) {
						xml = ds.getConfigurationUIHints();
					} else {
						//System.out.println("No configuration to show");
					}
					this.newDataSource = ds;
				}
			} catch (Throwable t) {
				//System.out.println("configuration setup failed");
				VueUtil.alert(this,t.getMessage(),"OSID Installation Error");
				t.printStackTrace();
				return;
			}
		
			if (xml != null) {
				edu.tufts.vue.ui.ConfigurationUI cui =
				new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
				cui.setPreferredSize(new Dimension(350,(int)cui.getPreferredSize().getHeight()));
				
				if (javax.swing.JOptionPane.showOptionDialog(this,
															 cui,
															 "Configuration",
															 javax.swing.JOptionPane.DEFAULT_OPTION,
															 javax.swing.JOptionPane.QUESTION_MESSAGE,
															 null,
															 new Object[] {
																 "Continue", "Cancel"
															 },
															 "Continue") == 1) {
					proceed = false;
				} else {
					try {
						GUI.activateWaitCursor();
						this.newDataSource.setConfiguration(cui.getProperties());
						GUI.invokeAfterAWT(new Runnable() { public void run() {
							try {
								synchronized (dataSourceManager) {
									dataSourceManager.save();
								}
							} catch (Throwable t) {
								System.out.println(t.getMessage());
							}
						}});
						
					} catch (Throwable t2) {
						
					} finally {
						GUI.clearWaitCursor();
					}
				}
			}
			if (proceed) {
				dataSourceList.addOrdered(this.newDataSource);
				dataSourceManager.add(this.newDataSource);
				dataSourceList.getContents().removeElement(dataSourceThatWasSelectedForUpdate);
				dataSourceManager.remove(dataSourceThatWasSelectedForUpdate.getId());
				providerListRenderer.setChecked(addLibraryList.getSelectedIndex());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			providerListRenderer.endWaitingMode();
			addLibraryList.repaint();
			GUI.clearWaitCursor();
			timer.stop();
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		DataSourceViewer.saveDataSourceViewer();
	}
	
	public edu.tufts.vue.dsm.DataSource getNewDataSource()
	{
		return this.newDataSource;
	}
    
    public void actionPerformed(ActionEvent ae) {
        
        
        if (ae.getActionCommand().equals("Update")) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            GUI.activateWaitCursor();
            providerListRenderer.invokeWaitingMode(addLibraryList.getSelectedIndex());
            repaint();
            int ONE_TNTH_SECOND = 100;
            
            timer = new Timer(ONE_TNTH_SECOND, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    repaint();
                }});
			timer.start();
			
			UpdateDSThread t = new UpdateDSThread();
			t.start();
			
        } else {
            providerListRenderer.clearAllChecked();
            setVisible(false);
        }
    }
    private class UpdateDSThread extends Thread {
        public UpdateDSThread() {
            super();
        }
        public void run() {
            add();
        }
    }
}



