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
 * @version $Revision: 1.1 $ / $Date: 2006-02-02 21:52:10 $ / $Author: jeff $
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

public class EditLibraryDialog extends JDialog implements java.awt.event.ActionListener {
    
    JPanel editLibraryPanel = new JPanel();
	
	JPanel buttonPanel = new JPanel();
	JButton okButton = new JButton("OK");
	
	java.awt.GridBagConstraints gbConstraints;
    
    public EditLibraryDialog()
	{
        super(VUE.getDialogParentAsFrame(),"EDIT LIBRARY",true);
		try {
			editLibraryPanel.setBackground(VueResources.getColor("White"));
			setBackground(VueResources.getColor("White"));
			
			java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
			gbConstraints = new java.awt.GridBagConstraints();
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			editLibraryPanel.setLayout(gbLayout);
			
			makePanel();
			
			okButton.addActionListener(this);			
			buttonPanel.add(okButton);
			okButton.setBackground(VueResources.getColor("Orange")); //TODO:  Why is this BLUE??

			//populate();

			getContentPane().add(editLibraryPanel,BorderLayout.CENTER);
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
			editLibraryPanel.add(new JLabel("Under construction"),gbConstraints);
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 1;
			editLibraryPanel.add(buttonPanel,gbConstraints);				
			getRootPane().setDefaultButton(okButton);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    public void update(int check) 
	{
		try {
			getContentPane().remove(editLibraryPanel);
			getContentPane().add(editLibraryPanel,BorderLayout.CENTER);
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
		if (ae.getActionCommand().equals("OK")) {
			setVisible(false);
		}
	}
    
	public String toString() 
	{
        return "EditLibraryDialog";
    }
}



