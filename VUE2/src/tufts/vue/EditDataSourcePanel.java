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
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: constructing");
        this.dialog = dialog;
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: new JPanel");
        editDataSourcePanel = new JPanel();
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: layout");
        setLayout(new BorderLayout());
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: setBorder");
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: setDataSource");
        setDataSource(DataSourceViewer.getActiveDataSource());
        if (DEBUG.DR) System.out.println("EditDataSourcePanel: save data sources?????");
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
        } else if(dataSource instanceof OsidDataSource) {
            editDataSourcePanel = new OsidDataSourcePanel((OsidDataSource)dataSource);
        }
        add(editDataSourcePanel,BorderLayout.NORTH);
        validate();
    }
    class FileDataSourcePanel extends JPanel {
        JTextField dsNameField;
        JTextField pathField ;
        JButton fileSelectButton;
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
            fileSelectButton  = new JButton("Browse...");
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
            dsNameField = new JTextField();
            pathField = new JTextField();
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    if(validateFields()) {
                        try {
                            FileDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                            FileDataSourcePanel.this.dataSource.setAddress(pathField.getText());
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
                        try {
                            FavoritesDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
                        try {
                            RemoteFileDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                            RemoteFileDataSourcePanel.this.dataSource.setAddress(addressField.getText());
                            RemoteFileDataSourcePanel.this.dataSource.setUserName(userField.getText());
                            RemoteFileDataSourcePanel.this.dataSource.setPassword(new String(passwordField.getPassword()));
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
                        try {
                            FedoraDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                            FedoraDataSourcePanel.this.dataSource.setAddress(addressField.getText());
                            FedoraDataSourcePanel.this.dataSource.setUserName(userField.getText());
                            FedoraDataSourcePanel.this.dataSource.setPassword(new String(passwordField.getPassword()));
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
                        try {
                            GoogleDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                            GoogleDataSourcePanel.this.dataSource.setUrl(addressField.getText());
                            GoogleDataSourcePanel.this.dataSource.setClient(clientField.getText());
                            GoogleDataSourcePanel.this.dataSource.setSite(siteField.getText());
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
        OsidDataSource dataSource;
        String cDsNameField; //cached field to be used for reset.
        String cAddressField; // cached path to be used on reset.
        public OsidDataSourcePanel(OsidDataSource dataSource) {
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
                        try {
                            OsidDataSourcePanel.this.dataSource.setDisplayName(dsNameField.getText());
                            OsidDataSourcePanel.this.dataSource.setAddress(addressField.getText());
                        } catch(Exception ex) {
                            if(DEBUG.DR) System.out.println(ex);
                            JOptionPane.showMessageDialog(tufts.vue.VUE.getInstance(),"Can't add datasource: "+dsNameField.getText()+" "+ ex.getMessage(), "OSID DR Alert", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            dialog.hide();
                            dialog.dispose();
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
