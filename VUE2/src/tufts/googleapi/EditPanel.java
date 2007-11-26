/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
/**
 *
 * @author  akumar03
 */
package  tufts.googleapi;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class EditPanel extends JPanel {
    JDialog dialog;
    JTextField dsNameField;
    JTextField keyField;
    DataSource dataSource;
    String cDsNameField; //cached field to be used for reset.
    String cKeyField;
    public EditPanel(DataSource dataSource,JDialog dialog) {
        this.dialog = dialog;
        this.dataSource = (tufts.googleapi.DataSource) dataSource;
        dsNameField = new JTextField();
        keyField = new JTextField();
        cDsNameField = dataSource.getDisplayName();
        cKeyField = dataSource.getKey();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel keyLabel = new JLabel("Key: ");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                if(validateFields()) {
                    try {
                        EditPanel.this.dataSource.setDisplayName(dsNameField.getText());
                        EditPanel.this.dataSource.setKey(keyField.getText());
                    } catch(Exception ex) {
                        JOptionPane.showMessageDialog(tufts.vue.VUE.getDialogParent(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "Google Edit Alert", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        EditPanel.this.dialog.setVisible(false);
                        EditPanel.this.dialog.dispose();
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
        resetPanel();
        
    }
    
    private void resetPanel() {
        dsNameField.setText(cDsNameField);
        keyField.setText(cKeyField);  
    }
    
    private boolean validateFields(){
        
        if(dsNameField.getText().length() > 0 && keyField.getText().length() > 0) {
            return true;
        } else {
            tufts.vue.VueUtil.alert(EditPanel.this, "Name should be atleast one character long", "DataSource Creation Error");
            return false;
        }
    }
}