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
 * AddDataSourcePanel.java
 *
 * Created on June 2, 2004, 10:12 PM
 */


package tufts.vue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This  panel is the UI to add/edit datasources.
 * @author  akumar03
 */
public class AddDataSourcePanel extends JPanel {
    
    /** Creates a new instance of AddDataSourcePanel */
    String[] dataSourceTypes = {"Local Folder","Favorites List", "FTP Server","Fedora","Local Google","OSID-DR"};
    Box addDataSourceBox;
    JPanel addPanel;
    JPanel typesPanel;
    AddEditDataSourceDialog dialog;
    
    public AddDataSourcePanel(AddEditDataSourceDialog dialog) {
        this.dialog = dialog;
        addDataSourceBox = Box.createVerticalBox();
        typesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel = new JPanel(new BorderLayout());
        addPanel.setBorder(BorderFactory.createEmptyBorder(0, 5,0, 5));
        addPanel.add(new FileDataSourcePanel());
        JComboBox typeField = new JComboBox(dataSourceTypes);    //  This will be a menu, later.
        typeField.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    addPanel.removeAll();
                    if(e.getItem().toString().equals(dataSourceTypes[0])){
                        addPanel.add(new FileDataSourcePanel(),BorderLayout.CENTER);
                    }else if(e.getItem().toString().equals(dataSourceTypes[1])){
                        addPanel.add(new FavoritesDataSourcePanel(),BorderLayout.CENTER);
                    }else if(e.getItem().toString().equals(dataSourceTypes[2])){
                        addPanel.add(new RemoteFileDataSourcePanel(),BorderLayout.CENTER);
                    }else if(e.getItem().toString().equals(dataSourceTypes[3])){
                        addPanel.add(new FedoraDataSourcePanel(),BorderLayout.CENTER);
                    }else if(e.getItem().toString().equals(dataSourceTypes[4])) {
                        addPanel.add(new GoogleDataSourcePanel(),BorderLayout.CENTER);
                    }else if(e.getItem().toString().equals(dataSourceTypes[5])) {
                        addPanel.add(new OsidDataSourcePanel(),BorderLayout.CENTER);
                    }
                    validate();
                }
            }
        });
        typesPanel.add(new JLabel("Type:"));
        typesPanel.add(typeField);
        addDataSourceBox.add(typesPanel);
        addDataSourceBox.add(addPanel);
        setLayout(new BorderLayout());
        add(addDataSourceBox,BorderLayout.NORTH);
    }
    
    class FileDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField pathField ;
        JButton fileSelectButton;
        public FileDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel pathLabel = new JLabel("Path:");
            fileSelectButton  = new JButton("...");
            dsNameField = new JTextField();
            pathField = new JTextField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new LocalFileDataSource(dsNameField.getText(), pathField.getText());
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
                    }
                }
            });
            
            fileSelectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                     JFileChooser chooser = new JFileChooser();
                     chooser.setDialogTitle("Select Folder");
                     chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                     chooser.setFileFilter(new VueFileFilter());
                     int option = chooser.showDialog(tufts.vue.VUE.getInstance(), "Select");
                     if(option ==  JFileChooser.APPROVE_OPTION) {
                         pathField.setText(chooser.getSelectedFile().getAbsolutePath());
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
            //c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(dsNameLabel,c);
            this.add(dsNameLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(dsNameField,c);
            this.add(dsNameField);
            
            c.gridwidth =1;
             
            c.weightx = 0.0;
            gridbag.setConstraints(pathLabel,c);
            this.add(pathLabel);
            
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(pathField,c);
            this.add(pathField);
       
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(fileSelectButton,c);
            this.add(fileSelectButton);
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
            pathField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && pathField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class FavoritesDataSourcePanel extends JPanel {
        JTextField dsNameField;
        public FavoritesDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            dsNameField = new JTextField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new FavoritesDataSource(dsNameField.getText());
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
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
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class RemoteFileDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField ;
        JTextField userField;
        JPasswordField passwordField;
        public RemoteFileDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel addressLabel = new JLabel("Address:");
            JLabel userLabel = new JLabel("User Name:");
            JLabel passwordLabel = new JLabel("Password:");
            dsNameField = new JTextField();
            addressField = new JTextField();
            userField = new JTextField();
            passwordField = new JPasswordField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new RemoteFileDataSource(dsNameField.getText(), addressField.getText(),userField.getText(),new String(passwordField.getPassword()));
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
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
            gridbag.setConstraints(addressLabel,c);
            this.add(addressLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(addressField,c);
            this.add(addressField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(userLabel,c);
            this.add(userLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(userField,c);
            this.add(userField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(passwordLabel,c);
            this.add(passwordLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(passwordField,c);
            this.add(passwordField);
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
            addressField.setText("");
            userField.setText("");
            passwordField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name and address should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class FedoraDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField ;
        JTextField userField;
        JPasswordField passwordField;
        public FedoraDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel addressLabel = new JLabel("Address:");
            JLabel userLabel = new JLabel("User Name:");
            JLabel passwordLabel = new JLabel("Password:");
            dsNameField = new JTextField();
            addressField = new JTextField();
            userField = new JTextField();
            passwordField = new JPasswordField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new FedoraDataSource(dsNameField.getText(), addressField.getText(),userField.getText(),new String(passwordField.getPassword()));
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
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
            gridbag.setConstraints(addressLabel,c);
            this.add(addressLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(addressField,c);
            this.add(addressField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(userLabel,c);
            this.add(userLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(userField,c);
            this.add(userField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(passwordLabel,c);
            this.add(passwordLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(passwordField,c);
            this.add(passwordField);
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
            addressField.setText("");
            userField.setText("");
            passwordField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name and address should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class GoogleDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField;
        JTextField siteField;
        JTextField clientField;
        public GoogleDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel addressLabel = new JLabel("Address:");
            JLabel siteLabel = new JLabel("Site:");
            JLabel clientLabel = new JLabel("Client:");
            dsNameField = new JTextField();
            addressField = new JTextField();
            siteField = new JTextField();
            clientField = new JTextField();
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new GoogleDataSource(dsNameField.getText(), addressField.getText(),siteField.getText(),clientField.getText());
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
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
            gridbag.setConstraints(addressLabel,c);
            this.add(addressLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(addressField,c);
            this.add(addressField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(siteLabel,c);
            this.add(siteLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(siteField,c);
            this.add(siteField);
            
             c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            gridbag.setConstraints(clientLabel,c);
            this.add(clientLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(clientField,c);
            this.add(clientField);
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
            addressField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
     class OsidDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField;
        public OsidDataSourcePanel() {
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel addressLabel = new JLabel("Address:");
             dsNameField = new JTextField();
            addressField = new JTextField();
             JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        DataSource ds = new OsidDataSource(dsNameField.getText(), addressField.getText());
                        DataSourceViewer.addDataSource(ds);
                        dialog.hide();
                        dialog.dispose();
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
            gridbag.setConstraints(addressLabel,c);
            this.add(addressLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(addressField,c);
            this.add(addressField);
            
 
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(bottomPanel,c);
            this.add(bottomPanel);
            
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText("");
            addressField.setText("");
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(AddDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    
}
