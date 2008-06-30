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
 * AddRemoveDataSourceDialog.java
 * The UI to Add/Remove Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
 * @version $Revision: 1.6 $ / $Date: 2008-06-30 20:52:54 $ / $Author: mike $
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class GetLibraryInfoDialog extends JDialog implements java.awt.event.ActionListener {
    
	private edu.tufts.vue.dsm.DataSource dataSource = null;
	private org.osid.repository.Repository repository = null;	
	
	java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
	java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();

    JPanel getLibraryInfoPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	JButton okButton = new JButton("OK");
	JLabel libraryIcon;
	
    public GetLibraryInfoDialog(edu.tufts.vue.dsm.DataSource dataSource)
	{
        super(VUE.getDialogParentAsFrame(),"INFO",true);
		try {
			this.dataSource = dataSource;
			repository = dataSource.getRepository();
			
			getLibraryInfoPanel.setBackground(VueResources.getColor("White"));
			setBackground(VueResources.getColor("White"));
			
			okButton.addActionListener(this);			
			buttonPanel.add(okButton);
			getRootPane().setDefaultButton(okButton);
			
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			getLibraryInfoPanel.setLayout(gbLayout);
			makePanel();

			javax.swing.JScrollPane jsp = new javax.swing.JScrollPane(getLibraryInfoPanel);
			jsp.setPreferredSize(new java.awt.Dimension(600,500));
			jsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			jsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
						
			getContentPane().add(jsp,BorderLayout.WEST);
			pack();
			setLocation(300,300);
			setSize(new Dimension(600,500));
		} catch (Throwable t) {
			t.printStackTrace();
		}
    }
	
	private void makePanel()
	{
		try {			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			
			libraryIcon = new JLabel(VueResources.getImageIcon("NoImage"));
			libraryIcon.setPreferredSize(new Dimension(80,80));
			getLibraryInfoPanel.add(libraryIcon,gbConstraints);
			
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Repository Id"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(repository.getId().getIdString()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Name"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(repository.getDisplayName()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Description"),gbConstraints);
			
			gbConstraints.gridx = 1;
			String description = repository.getDescription();
			int descriptionLength = description.length();
			int numCharacters = 60;
			int rows = descriptionLength / numCharacters + 1;
			for (int x=0; x < rows-1; x++) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(description.substring(x * numCharacters,x * numCharacters + numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			if (descriptionLength > 1) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(description.substring((rows-1) * numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Type"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(repository.getType())),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			System.out.println("Data Source1 " + dataSource);
			System.out.println("creatpr " + dataSource.getCreator());
			getLibraryInfoPanel.add(new javax.swing.JLabel("Creator"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getCreator()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Publisher"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getPublisher()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Release Date"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.dateToString(dataSource.getReleaseDate())),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Provider Id"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getProviderId().getIdString()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Osid Service"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getOsidVersion()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Osid Load Key"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getOsidLoadKey()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Provider Display Name"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel(dataSource.getProviderDisplayName()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Provider Description"),gbConstraints);
			
			gbConstraints.gridx = 1;
			description = dataSource.getProviderDescription();
			descriptionLength = description.length();
			rows = descriptionLength / numCharacters + 1;
			for (int x=0; x < rows-1; x++) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(description.substring(x * numCharacters,x * numCharacters + numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			if (descriptionLength > 1) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(description.substring((rows-1) * numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Online?"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel( (dataSource.isOnline()) ? "Yes" : "No" ),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Supports Update?"),gbConstraints);
			
			gbConstraints.gridx = 1;
			getLibraryInfoPanel.add(new javax.swing.JLabel( (dataSource.supportsUpdate()) ? "The Library Supports Updating" : "The Library Is Read Only" ),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Asset Types"),gbConstraints);
			
			gbConstraints.gridx = 1;
			org.osid.shared.TypeIterator typeIterator = repository.getAssetTypes();
			while (typeIterator.hasNextType()) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType())),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			getLibraryInfoPanel.add(new javax.swing.JLabel("Search Types"),gbConstraints);
			
			gbConstraints.gridx = 1;
			typeIterator = repository.getSearchTypes();
			while (typeIterator.hasNextType()) {
				getLibraryInfoPanel.add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType())),gbConstraints);
				gbConstraints.gridy++;
			}
/*			
			java.awt.Image image = null;
			if ( (image = dataSource.getImageForRepository()) != null ) {		
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
				getLibraryInfoPanel.add(new javax.swing.JLabel(new javax.swing.ImageIcon(image)),gbConstraints);
			}
*/
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void actionPerformed(java.awt.event.ActionEvent ae)
	{
		if (ae.getActionCommand().equals("OK")) {
			setVisible(false);
		}
	}
    
	public String toString() 
	{
        return "GetLibraryInfoDialog";
    }
}



