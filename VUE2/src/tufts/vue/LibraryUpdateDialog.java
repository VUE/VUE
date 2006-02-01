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
 * @version $Revision: 1.1 $ / $Date: 2006-02-01 02:35:48 $ / $Author: jeff $
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class LibraryUpdateDialog extends JDialog implements java.awt.event.ActionListener {
    
    JPanel updateLibraryPanel = new JPanel();
	JCheckBox checkBox = new JCheckBox();
	JLabel noUpdatesLabel = new JLabel("VUE has not detected any updates");
	JLabel updateLabelBeforeName = new JLabel("VUE has detected an update for");
	JLabel updateLabelAfterName = new JLabel("Would you like to update this library?");
	
	JLabel libraryIcon;
	edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
	edu.tufts.vue.dsm.Registry registry;
	org.osid.registry.Provider checked[];

	JPanel buttonPanel = new JPanel();
	JButton okButton = new JButton("OK");
	JButton cancelButton = new JButton("Do Not Update");
	JButton updateButton = new JButton("Update");
	
	int currentDataSource = 0;
	
	public static final int CHECK = 0;
	public static final int DO_NOT_CHECK = 1;
	
	java.awt.GridBagConstraints gbConstraints;
    
    public LibraryUpdateDialog()
	{
        super(VUE.getDialogParentAsFrame(),"LIBRARY UPDATE",true);
		try {
			updateLibraryPanel.setBackground(VueResources.getColor("White"));
			setBackground(VueResources.getColor("White"));
			
			java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
			gbConstraints = new java.awt.GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			updateLibraryPanel.setLayout(gbLayout);
			
			cancelButton.addActionListener(this);
			updateButton.addActionListener(this);
			okButton.addActionListener(this);
			
			buttonPanel.add(cancelButton);
			buttonPanel.add(updateButton);
			updateButton.setBackground(VueResources.getColor("Orange")); //TODO:  Why is this BLUE??
			okButton.setBackground(VueResources.getColor("Orange")); //TODO:  Why is this BLUE??

			populate();
			if (checked.length > 0) {
				makePanel(checked[0].getDisplayName());
			} else {
				gbConstraints.gridx = 0;
				gbConstraints.gridy = 0;
				updateLibraryPanel.add(noUpdatesLabel);
				gbConstraints.gridx = 0;
				gbConstraints.gridy = 1;
				updateLibraryPanel.add(okButton,gbConstraints);
				getRootPane().setDefaultButton(okButton);
			}

			getContentPane().add(updateLibraryPanel,BorderLayout.CENTER);
			pack();
			setLocation(300,300);
			setSize(new Dimension(480,300));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		setVisible(true);
    }
	
	private void makePanel(String displayName)
	{
		try {
			// TODO: Replace this with an image, but we need this in the Provider not just the Data Source
			libraryIcon = new JLabel(VueResources.getImageIcon("NoImage"));
			libraryIcon.setPreferredSize(new Dimension(80,80));
			
			JPanel messagePanel = new JPanel();
			messagePanel.setLayout(new GridLayout(3,1));
			messagePanel.add(updateLabelBeforeName);
			messagePanel.add(new JLabel(displayName));
			messagePanel.add(updateLabelAfterName);
			
			JPanel checkBoxTextPanel = new JPanel();
			checkBoxTextPanel.setLayout(new GridLayout(2,1));
			checkBoxTextPanel.add(new JLabel("Do not check for update automatically or"));
			checkBoxTextPanel.add(new JLabel("use the preferences menu to customize settings"));

			JPanel checkBoxPanel = new JPanel();
			checkBoxPanel.add(checkBox);
			checkBoxPanel.add(checkBoxTextPanel);
			
			JPanel iconPanel = new JPanel();
			iconPanel.add(libraryIcon);
			iconPanel.add(messagePanel);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			updateLibraryPanel.add(iconPanel,gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 1;
			updateLibraryPanel.add(checkBoxPanel,gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 2;
			updateLibraryPanel.add(buttonPanel,gbConstraints);				
			getRootPane().setDefaultButton(updateButton);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    private void populate()
	{
		try
		{
			currentDataSource = 0;
			if (dataSourceManager == null) {
				dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
				registry = edu.tufts.vue.dsm.impl.VueRegistry.getInstance();
			}
			edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
			checked = registry.checkRegistryForUpdated(dataSources);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
    public void update(int check) 
	{
		try {
			getContentPane().remove(updateLibraryPanel);
			if (check == CHECK) populate();
			if (checked.length > 0) {
				makePanel(checked[currentDataSource].getDisplayName());
			} else {
				gbConstraints.gridx = 0;
				gbConstraints.gridy = 0;
				updateLibraryPanel.add(noUpdatesLabel);
				gbConstraints.gridx = 0;
				gbConstraints.gridy = 1;
				updateLibraryPanel.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			getContentPane().add(updateLibraryPanel,BorderLayout.CENTER);
			getContentPane().repaint();
			getContentPane().validate();
			pack();
			setVisible(true);
			super.setVisible(true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
    }

	public void actionPerformed(java.awt.event.ActionEvent ae)
	{
		if (ae.getActionCommand().equals("Do Not Update")) {
			currentDataSource++;
			if (checked.length < currentDataSource) update(DO_NOT_CHECK);
		} else if (ae.getActionCommand().equals("Update")) {
			// perform some update operation
			System.out.println("updated");
			currentDataSource++;
			if (checked.length < currentDataSource) update(DO_NOT_CHECK);
		}
		setVisible(false);
	}
    
	public String toString() 
	{
        return "LibraryUpdateDialog";
    }
}



