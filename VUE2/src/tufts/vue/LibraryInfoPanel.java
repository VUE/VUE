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
 * @version $Revision: 1.6 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class LibraryInfoPanel extends JPanel
{
    public LibraryInfoPanel(edu.tufts.vue.dsm.DataSource dataSource)
	{
		try {
			org.osid.repository.Repository repository = dataSource.getRepository();

			java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
			java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();			
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			setLayout(gbLayout);

			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Repository Id"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(repository.getId().getIdString()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Name"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(repository.getDisplayName()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Description"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			String description = repository.getDescription();
			int descriptionLength = description.length();
			int numCharacters = 60;
			int rows = descriptionLength / numCharacters + 1;
			for (int x=0; x < rows-1; x++) {
				add(new javax.swing.JLabel(description.substring(x * numCharacters,x * numCharacters + numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			if (descriptionLength > 1) {
				add(new javax.swing.JLabel(description.substring((rows-1) * numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Type"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(repository.getType())),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Creator"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getCreator()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Publisher"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getPublisher()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Release Date"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.dateToString(dataSource.getReleaseDate())),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Provider Id"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getProviderId().getIdString()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Osid Service"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getOsidName() +
														   " " +
														   dataSource.getOsidVersion()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Osid Load Key"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getOsidLoadKey()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Provider Display Name"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel(dataSource.getProviderDisplayName()),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Provider Description"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			description = dataSource.getProviderDescription();
			descriptionLength = description.length();
			rows = descriptionLength / numCharacters + 1;
			for (int x=0; x < rows-1; x++) {
				add(new javax.swing.JLabel(description.substring(x * numCharacters,x * numCharacters + numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			if (descriptionLength > 1) {
				add(new javax.swing.JLabel(description.substring((rows-1) * numCharacters)),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Online?"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel( (dataSource.isOnline()) ? "Yes" : "No" ),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Supports Update?"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			add(new javax.swing.JLabel( (dataSource.supportsUpdate()) ? "The Library Supports Updating" : "The Library Is Read Only" ),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Asset Types"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			org.osid.shared.TypeIterator typeIterator = repository.getAssetTypes();
			while (typeIterator.hasNextType()) {
				add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType())),gbConstraints);
				gbConstraints.gridy++;
			}
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy++;
			gbConstraints.fill = java.awt.GridBagConstraints.NONE;
			gbConstraints.weightx = 0;
			add(new javax.swing.JLabel("Search Types"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbConstraints.weightx = 1;
			typeIterator = repository.getSearchTypes();
			while (typeIterator.hasNextType()) {
				add(new javax.swing.JLabel(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType())),gbConstraints);
				gbConstraints.gridy++;
			}
			
			java.awt.Image image = null;
			if ( (image = dataSource.getIcon16x16()) != null ) {		
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
				add(new javax.swing.JLabel(new javax.swing.ImageIcon(image)),gbConstraints);
			}
		} catch (Throwable t) {
			//t.printStackTrace();
			System.out.println(t.getMessage());
		}
    }
}



