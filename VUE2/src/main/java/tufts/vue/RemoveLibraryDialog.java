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
/*
 * AddRemoveDataSourceDialog.java
 * The UI to Add/Remove Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
 * @version $Revision: 1.7 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 * @author  akumar03
 */
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RemoveLibraryDialog extends JDialog implements java.awt.event.ActionListener {
    
    JPanel removeLibraryPanel = new JPanel();
	
	JPanel buttonPanel = new JPanel();
	JButton okButton = new JButton(VueResources.getString("button.ok.label"));
	
	java.awt.GridBagConstraints gbConstraints;
    
    public RemoveLibraryDialog()
	{
        super(VUE.getDialogParentAsFrame(),VueResources.getString("dialog.removelib.title"),true);
		try {
			removeLibraryPanel.setBackground(VueResources.getColor("White"));
			setBackground(VueResources.getColor("White"));
			
			java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
			gbConstraints = new java.awt.GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			removeLibraryPanel.setLayout(gbLayout);
			
			makePanel();
			
			okButton.addActionListener(this);			
			buttonPanel.add(okButton);

			//populate();

			getContentPane().add(removeLibraryPanel,BorderLayout.CENTER);
			pack();
			setLocation(300,300);
			setSize(new Dimension(480,300));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		setVisible(true);
    }
	
	private void makePanel()
	{
		try {			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			removeLibraryPanel.add(new JLabel(VueResources.getString("dialog.removelib.underconstruct")),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 1;
			removeLibraryPanel.add(buttonPanel,gbConstraints);				
			getRootPane().setDefaultButton(okButton);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    public void update(int check) 
	{
		try {
			getContentPane().remove(removeLibraryPanel);
			getContentPane().add(removeLibraryPanel,BorderLayout.CENTER);
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
		if (ae.getActionCommand().equals(VueResources.getString("button.ok.label"))) {
			setVisible(false);
		}
	}
    
	public String toString() 
	{
        return "RemoveLibraryDialog";
    }
}



