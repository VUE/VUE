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
 * AddPanel.java
 *
 * Created on August 13, 2004, 11:08 AM
 */

package  tufts.googleapi;

/**
 *
 * @author  akumar03
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import tufts.vue.*;


public class AddPanel extends JPanel {
    JTextField dsNameField;
    JTextField keyField;
    JDialog dialog; // keep track of the parent dialog, required for closing
    public AddPanel(JDialog dialog) {
        this.dialog = dialog;
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel keyLabel = new JLabel("Key: ");
        dsNameField = new JTextField();
        keyField = new JTextField();
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                if(validateFields()) {
                    try {
                        tufts.vue.DataSource ds = new tufts.googleapi.DataSource(dsNameField.getText(),keyField.getText());
                        DataSourceViewer.addDataSource(ds);
                    } catch(Exception ex) {
                        JOptionPane.showMessageDialog(tufts.vue.VUE.getDialogParent(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "Artifact", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    } finally {
                        AddPanel.this.dialog.setVisible(false);
                        AddPanel.this.dialog.dispose();
                    }
                }
            }
        });
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                resetPanel();
            }
        });
        bottomPanel.add(submitButton);
        bottomPanel.add(resetButton);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        gridbag.setConstraints(dsNameLabel,c);
        this.add(dsNameLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        gridbag.setConstraints(dsNameField,c);
        this.add(dsNameField);
        
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        gridbag.setConstraints(keyLabel,c);
        this.add(keyLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        gridbag.setConstraints(keyField,c);
        this.add(keyField);
        
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(bottomPanel,c);
        this.add(bottomPanel);
    }
    
    private void resetPanel() {
        dsNameField.setText("");
        keyField.setText("");
    }
    
    private boolean validateFields(){
        
        if(dsNameField.getText().length() > 0 && keyField.getText().length() > 0) {
            return true;
        }else {
       
            VueUtil.alert(this, "Name should be atleast one character long", "DataSource Creation Error");
            return false;
        }
    }
}


