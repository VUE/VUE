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
 *
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class AddEditDataSourceDialog extends JDialog {
    public static final int ADD_MODE = 0;
    public static final int EDIT_MODE = 1;
    
    JTabbedPane tabbedPane;
    JPanel addPanel;
    JPanel editPanel;
    
    /** Creates a new instance of AddEditDataSourceDialog */
    public AddEditDataSourceDialog() {
        super(tufts.vue.VUE.getInstance(),"Add/Edit Data Source",true);
        if (DEBUG.DR) System.out.println(this + " new JTabbedPane");
        tabbedPane = new JTabbedPane();
        if (DEBUG.DR) System.out.println(this + " setPreferredSize");
        tabbedPane.setPreferredSize(new Dimension(300,400));
        if (DEBUG.DR) System.out.println(this + " new AddDataSourcePanel");
        addPanel = new AddDataSourcePanel(this);
        if (DEBUG.DR) System.out.println(this + " new EditDataSourcePanel");
        editPanel = new EditDataSourcePanel(this);
        if (DEBUG.DR) System.out.println(this + " setup");
        editPanel.setName("Edit Panel");
        tabbedPane.add("Add", addPanel);
        tabbedPane.add("Edit",editPanel);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane,BorderLayout.CENTER);
        if (DEBUG.DR) System.out.println(this + " pack");
        pack();
        setLocation(300,300);
        if (DEBUG.DR) System.out.println(this + " setSize");
        setSize(new Dimension(325, 245));
        if (DEBUG.DR) System.out.println(this + " INITIALIZED");
    }
    
    public void show(int mode) {
        if (DEBUG.DR) System.out.println(this + " show, mode=" + mode);
        if(mode == EDIT_MODE) {
            tabbedPane.setSelectedComponent(editPanel);
        }else {
            tabbedPane.setSelectedComponent(addPanel);
        }
        if (DEBUG.DR) System.out.println(this + " calling super.show");
        show();
    }

    public String toString() {
        return "AddEditDataSourceDialog";
    }
}



