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
 * EditDataSourcePanel.java
 *
 * Created on June 9, 2004, 7:35 PM
 */

package tufts.vue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  akumar03
 */
public class EditDataSourcePanel extends JPanel{
    
    /** Creates a new instance of EditDataSourcePanel */
    AddEditDataSourceDialog dialog;
    JPanel editDataSourcePanel;
    public EditDataSourcePanel(AddEditDataSourceDialog dialog) {
        this.dialog = dialog;
        editDataSourcePanel = new JPanel();
        setLayout(new BorderLayout());
        setDataSource(DataSourceViewer.getActiveDataSource());
        DataSourceViewer.saveDataSourceViewer();
    }
    
    public void setDataSource(DataSource dataSource) {
        removeAll();
        if(dataSource instanceof LocalFileDataSource) {
            editDataSourcePanel = new FileDataSourcePanel((LocalFileDataSource)dataSource);
        } else if(dataSource instanceof FavoritesDataSource) {
            editDataSourcePanel = new FavoritesDataSourcePanel((FavoritesDataSource)dataSource);
        } else if(dataSource instanceof RemoteFileDataSource) {
            editDataSourcePanel = new RemoteFileDataSourcePanel((RemoteFileDataSource)dataSource);
        } else if(dataSource instanceof FedoraDataSource) {
            editDataSourcePanel = new FedoraDataSourcePanel((FedoraDataSource)dataSource);
        } else if(dataSource instanceof GoogleDataSource) {
            editDataSourcePanel = new GoogleDataSourcePanel((GoogleDataSource)dataSource);
        }
        add(editDataSourcePanel,BorderLayout.NORTH);
        validate();
    }
    class FileDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField pathField ;
        LocalFileDataSource dataSource;
        String cDsNameField; //cached field to be used for reset.
        String cPathField; // cached path to be used on reset.
        public FileDataSourcePanel(LocalFileDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            cPathField = dataSource.getAddress();
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            this.setLayout(gridbag);
            JLabel dsNameLabel = new JLabel("Display Name: ");
            JLabel pathLabel = new JLabel("Path:");
            dsNameField = new JTextField();
            pathField = new JTextField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        FileDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        FileDataSourcePanel.this.dataSource.setAddress(pathField.getText());
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
            gridbag.setConstraints(pathLabel,c);
            this.add(pathLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(pathField,c);
            this.add(pathField);
            
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
            pathField.setText(cPathField);
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && pathField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class FavoritesDataSourcePanel extends JPanel {
        JTextField dsNameField;
        FavoritesDataSource dataSource;
        String cDsNameField; //cached field to be used for reset.
        public FavoritesDataSourcePanel(FavoritesDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            
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
                        FavoritesDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
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
            resetPanel();
        }
        
        private void resetPanel() {
            dsNameField.setText(cDsNameField);
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class RemoteFileDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField ;
        JTextField userField;
        JPasswordField passwordField;
        String cDsNameField;
        String cAddressField;
        String cUserField;
        String cPasswordField;
        RemoteFileDataSource dataSource;
        public RemoteFileDataSourcePanel(RemoteFileDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            cAddressField = dataSource.getAddress();
            cUserField = dataSource.getUserName();
            cPasswordField = dataSource.getPassword();
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
                        RemoteFileDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        RemoteFileDataSourcePanel.this.dataSource.setAddress(addressField.getText());
                        RemoteFileDataSourcePanel.this.dataSource.setUserName(userField.getText());
                        RemoteFileDataSourcePanel.this.dataSource.setPassword(new String(passwordField.getPassword()));
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
            resetPanel();
            
        }
        
        private void resetPanel() {
            dsNameField.setText(cDsNameField);
            addressField.setText(cAddressField);
            userField.setText(cUserField);
            passwordField.setText(cPasswordField);
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name and address should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class FedoraDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField ;
        JTextField userField;
        JPasswordField passwordField;
        String cDsNameField;
        String cAddressField;
        String cUserField;
        String cPasswordField;
        FedoraDataSource dataSource;
        
        public FedoraDataSourcePanel(FedoraDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            cAddressField = dataSource.getAddress();
            cUserField = dataSource.getUserName();
            cPasswordField = dataSource.getPassword();
            
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
                        FedoraDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        FedoraDataSourcePanel.this.dataSource.setAddress(addressField.getText());
                        FedoraDataSourcePanel.this.dataSource.setUserName(userField.getText());
                        FedoraDataSourcePanel.this.dataSource.setPassword(new String(passwordField.getPassword()));
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
            resetPanel();
            
            
        }
        
        private void resetPanel() {
            dsNameField.setText(cDsNameField);
            addressField.setText(cAddressField);
            userField.setText(cUserField);
            passwordField.setText(cPasswordField);
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name and address should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    class GoogleDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField;
        JTextField siteField;
        JTextField clientField;
        GoogleDataSource dataSource;
        String cDsNameField; //cached field to be used for reset.
        String cAddressField; // cached path to be used on reset.
        String cSiteField;
        String cClientField;
        public GoogleDataSourcePanel(GoogleDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            cAddressField = dataSource.getUrl();
            cSiteField  = dataSource.getSite();
            cClientField = dataSource.getClient();
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
                        GoogleDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        GoogleDataSourcePanel.this.dataSource.setUrl(addressField.getText());
                        GoogleDataSourcePanel.this.dataSource.setClient(clientField.getText());
                        GoogleDataSourcePanel.this.dataSource.setSite(siteField.getText());
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
            resetPanel();
        }
        
        private void resetPanel() {
            dsNameField.setText(cDsNameField);
            addressField.setText(cAddressField);
            siteField.setText(cSiteField);
            clientField.setText(cClientField);
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
    
    class OsidDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField addressField;
        GoogleDataSource dataSource;
        String cDsNameField; //cached field to be used for reset.
        String cAddressField; // cached path to be used on reset.
        public OsidDataSourcePanel(GoogleDataSource dataSource) {
            this.dataSource = dataSource;
            cDsNameField = dataSource.getDisplayName();
            cAddressField = dataSource.getAddress();
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
                        OsidDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        OsidDataSourcePanel.this.dataSource.setUrl(addressField.getText());
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
            resetPanel();
        }
        
        private void resetPanel() {
            dsNameField.setText(cDsNameField);
            addressField.setText(cAddressField);
            
        }
        
        private boolean validateFields(){
            
            if(dsNameField.getText().length() > 0 && addressField.getText().length() >0) {
                return true;
            } else {
                VueUtil.alert(EditDataSourcePanel.this, "Name should be atleast one character long", "DataSource Creation Error");
                return false;
            }
        }
    }
    
}
