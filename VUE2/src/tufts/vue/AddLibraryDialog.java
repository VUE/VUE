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
 * @version $Revision: 1.1 $ / $Date: 2006-01-30 21:48:37 $ / $Author: jeff $
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class AddLibraryDialog extends JDialog {
    
    JPanel addLibraryPanel;
    JList addLibraryList;
	DefaultListModel listModel = new DefaultListModel();
	JScrollPane jsp;
	JLabel libraryIcon;
	JTextArea libraryDescription;
	edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
	edu.tufts.vue.dsm.Registry registry;
    
    /** Creates a new instance of AddEditDataSourceDialog */
    public AddLibraryDialog() {
        super(VUE.getDialogParentAsFrame(),"ADD A LIBRARY",true);
		addLibraryList = new JList(listModel);
        addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		populate();
		jsp = new JScrollPane(addLibraryList);
		addLibraryPanel.setPreferredSize(new Dimension(300,400));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jsp,BorderLayout.CENTER);
        pack();
        setLocation(300,300);
    }

    private void populate()
	{
		try
		{
			if (dataSourceManager == null) {
				dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
				registry = edu.tufts.vue.dsm.impl.VueRegistry.getInstance();
			}
			edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
			org.osid.registry.Provider checked[] = registry.checkRegistryForNew(dataSources);
			listModel.removeAllElements();
			if (checked.length == 0) {
				listModel.addElement("No new Libraries");
			}
			for (int i=0; i < checked.length; i++) {
				listModel.addElement(checked[i].getDisplayName());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
    public void show(int mode) {
		populate();
        super.setVisible(true);
    }

    public String toString() {
        return "AddLibraryDialog";
    }
}



