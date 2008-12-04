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
package edu.tufts.vue.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.FocusTraversalPolicy;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTextField;

import tufts.vue.VueResources;

import tufts.vue.gui.VueButton;
import tufts.vue.gui.VueFileChooser;

public class ConfigurationUI extends javax.swing.JPanel {
    private static String xmlFilename = null;
    private static final String FILE_NOT_FOUND_MESSAGE = "Cannot find or open ";
    
    private static final String DEFAULT_TAG = "default";
    private static final String DESCRIPTION_TAG = "description";
    private static final String FIELD_TAG = "field";
    private static final String KEY_TAG = "key";
    private static final String MANDATORY_TAG = "mandatory";
    private static final String MAX_CHARS_TAG = "maxChars";
    private static final String TITLE_TAG = "title";
    private static final String UI_TAG = "ui";
    
    public static final int SINGLE_LINE_CLEAR_TEXT_CONTROL = 0;
    public static final int SINGLE_LINE_MASKED_TEXT_CONTROL = 1;
    public static final int MULTI_LINE_TEXT_CONTROL = 2;
    public static final int BOOLEAN_CONTROL = 3;
    public static final int INTEGER_CONTROL = 4;
    public static final int FLOAT_CONTROL = 5;
    public static final int DATE_TIME_CONTROL = 6;
    public static final int DURATION_CONTROL = 7;
    public static final int FILECHOOSER_CONTROL = 8;
    public static final int SINGLE_LINE_NONEDITABLE_TEXT_CONTROL = 9;
    
    private java.util.Vector defaultValueVector = new java.util.Vector();
    private java.util.Vector descriptionVector = new java.util.Vector();
    private java.util.Vector keyVector = new java.util.Vector();
    private java.util.Vector mandatoryVector = new java.util.Vector();
    private java.util.Vector maxCharsVector = new java.util.Vector();
    private java.util.Vector titleVector = new java.util.Vector();
    private java.util.Vector uiVector = new java.util.Vector();
    private java.util.Vector fieldVector = new java.util.Vector();
    
    private java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
    private java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
    private JButton chooser;
    private ActionListener chooserActionListener;
    private JTextField textField8;
    
    private String errorMessage = null;
    
    public ConfigurationUI(java.io.InputStream stream) {
    	this.setFocusCycleRoot(true);
    	
    	
        getXML(stream);
        if (errorMessage != null) {
            System.out.println("Error: " + this.errorMessage);
        } else {
            populatePanel();
            
        }
    }
    
    private void getXML(java.io.InputStream stream) {
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = null;
            javax.xml.parsers.DocumentBuilder db = null;
            org.w3c.dom.Document document = null;
            
            dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            document = db.parse(stream);
            
            org.w3c.dom.NodeList fields = document.getElementsByTagName(FIELD_TAG);
            int numFields = fields.getLength();
            for (int i=0; i < numFields; i++) {
                String defaultValue = null;
                String description = null;
                String key = null;
                String mandatory = null;
                String maxChars = null;
                String title = null;
                String ui = null;
                String noedit = null;
                
                
                org.w3c.dom.Element field = (org.w3c.dom.Element)fields.item(i);
                org.w3c.dom.NodeList nodeList = field.getElementsByTagName(DEFAULT_TAG);
                org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    defaultValue = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(DESCRIPTION_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    description = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(KEY_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    key = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(MANDATORY_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    mandatory = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(MAX_CHARS_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    maxChars = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(TITLE_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    title = e.getFirstChild().getNodeValue();
                }
                nodeList = field.getElementsByTagName(UI_TAG);
                e = (org.w3c.dom.Element)nodeList.item(0);
                if (e.hasChildNodes()) {
                    ui = e.getFirstChild().getNodeValue();
                }
            
                
                
                                /*
                                 Validate data:
                                 
                                 key must be non-null
                                 title must be non-null
                                 description can be null
                                 default value can be null; if not it must match ui control type
                                 mandatory must be a boolean, default to true
                                 maxChars must be non-negative; 0 means no specific length
                                 ui must be a numeral 0-7, inclusive
                                 */
                
                if ( (key == null) || (key.trim().length() == 0) ) {
                    this.errorMessage = "Key must be non-null";
                    return;
                }
                if ( (title == null) || (title.trim().length() == 0) ) {
                    this.errorMessage = "Title must be non-null";
                    return;
                }
                Boolean isMandatory = new Boolean(true);
                try {
                    isMandatory = new Boolean(mandatory);
                } catch (Exception ex) {
                    this.errorMessage = "Mandatory must be true or false";
                    return;
                }
                
                //Integer numChars = new Integer(15);
                Integer numChars = 0; // default 0 (no limit)
                if (maxChars != null) {
                    try {
                        numChars = Integer.valueOf(maxChars);
                        if (numChars < 0) {
                            this.errorMessage = "Number of characters must be a positive integer";
                            return;
                        }
                    } catch (Exception ex) {
                        this.errorMessage = "Number of characters must be a positive integer";
                        return;
                    }
                }

//                 if (maxChars == null) {
//                     // attept to provide some reasonable defaults
//                     if ("username".equals(key) ||
//                         "password".equals(key))
//                         numChars = 16;
//                     else if ("port".equals(key))
//                         numChars = 8;
//                 }
                
                Integer uiCode = new Integer(0);
                try {
                    uiCode = new Integer(ui);
                    int n = uiCode.intValue();
                    if ( (n < 0) || (n > 9) ) {
                        this.errorMessage = "Invalid UI control code";
                        return;
                    }
                } catch (Exception ex) {
                    this.errorMessage = "Invalid UI control code";
                    return;
                }
                
                descriptionVector.addElement(description);
                keyVector.addElement(key);
                mandatoryVector.addElement(isMandatory);
                maxCharsVector.addElement(numChars);
                titleVector.addElement(title);
                uiVector.addElement(uiCode);
                
                if (defaultValue == null) {
                    defaultValueVector.addElement(null);
                } else {
                    switch (uiCode.intValue()) {
                        case SINGLE_LINE_CLEAR_TEXT_CONTROL:
                            // no extra validation for default value since it is a string
                            break;
                        case SINGLE_LINE_MASKED_TEXT_CONTROL:
                            // no extra validation for default value since it is a string
                            break;
                        case MULTI_LINE_TEXT_CONTROL:
                            // no extra validation for default value since it is a string
                            break;
                        case   SINGLE_LINE_NONEDITABLE_TEXT_CONTROL:
                            
                            break;
                        case BOOLEAN_CONTROL:
                            try {
                                Boolean b = new Boolean(defaultValue);
                            } catch (Exception ex) {
                                this.errorMessage = "Default value for " + key + " must be true or false";
                                return;
                            }
                            break;
                        case INTEGER_CONTROL:
                            try {
                                Integer ix = new Integer(defaultValue);
                            } catch (Exception ex) {
                                this.errorMessage = "Default value for " + key + " must be an integer";
                                return;
                            }
                            break;
                        case FLOAT_CONTROL:
                            try {
                                Float f = new Float(defaultValue);
                            } catch (Exception ex) {
                                this.errorMessage = "Default value for " + key + " must be a float";
                                return;
                            }
                            break;
                        case DATE_TIME_CONTROL:
                            try {
                                java.util.Date d = new java.util.Date(defaultValue);
                            } catch (Exception ex) {
                                this.errorMessage = "Default value for " + key + " must be a date";
                                return;
                            }
                            break;
                        case DURATION_CONTROL:
                            // TODO: add some reasonable validation here
                            break;
                    }
                    defaultValueVector.addElement(defaultValue);
                }
            }
        } catch (Exception ex) {
            this.errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
    }

    Vector<Component> order = null;
    private void populatePanel() {
        try {
            // setup panel layout
            gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gbConstraints.insets = new java.awt.Insets(2,2,2,2);
            //gbConstraints.weighty = 1;
            gbConstraints.weighty = 0;
            gbConstraints.ipadx = 0;
            gbConstraints.ipady = 0;
            setLayout(gbLayout);
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 0;
            order = new Vector<Component>(this.uiVector.size());
            
            for (int i = 0, size = this.uiVector.size(); i < size; i++) {
                int uiCode = ((Integer)uiVector.elementAt(i)).intValue();
                String defaultValue = (String)defaultValueVector.elementAt(i);
                
                // if default value from Provider is null, check that there is not a value already set
                
                final String title = (String)titleVector.elementAt(i);
                final int numChars = ((Integer)maxCharsVector.elementAt(i)).intValue();
                // FYI: JTextField.setColumns doesn't actually limit the size
                // of the text field: it just sets the preferred size, which
                // may be ignored depending on the GridBag configuration.

                final boolean limitWidth = numChars > 0;
                
                final boolean isMandatory = ((Boolean)mandatoryVector.elementAt(i)).booleanValue();
                //String prefix = (isMandatory) ? "*" : "";
                final javax.swing.JLabel prompt = new javax.swing.JLabel(title + ": ");

                if (tufts.vue.DEBUG.DR)
                    System.out.println("CONFIGURATION - Title:"+title+" prompt:"+prompt+" numChars:"+numChars+" uiCode:"+uiCode);
                switch (uiCode) {
                    // create appropriate field
                    // add to panel
                    // update vectors for when we want to get the values out
                    case SINGLE_LINE_CLEAR_TEXT_CONTROL:
                        javax.swing.JTextField textField0 = new javax.swing.JTextField();
                        if (numChars > 0) {
                            textField0.setColumns(numChars);
                        }
                        if (defaultValue != null) {
                            textField0.setText(defaultValue);
                        }
                        populateField(prompt,textField0, limitWidth);
                        break;
                    case SINGLE_LINE_MASKED_TEXT_CONTROL:
                        javax.swing.JPasswordField textField1 = new javax.swing.JPasswordField();
                        
                        if (numChars > 0) {
                            textField1.setColumns(numChars);
                        }
                        if (defaultValue != null) {
                            textField1.setText(defaultValue);
                        }
                        populateField(prompt,textField1, limitWidth);
                        break;
                    case SINGLE_LINE_NONEDITABLE_TEXT_CONTROL:
                        javax.swing.JTextField textField9 = new javax.swing.JTextField();
                        
                        if (numChars > 0) {
                            textField9.setColumns(numChars);
                        }
                        if (defaultValue != null) {
                            textField9.setText(defaultValue);
                        }
                        //textField9.setEditable(false);
                        textField9.setEnabled(false);
                        populateField(prompt,textField9, limitWidth);
                        break;
                    case MULTI_LINE_TEXT_CONTROL:
                        javax.swing.JTextArea textField2 = new javax.swing.JTextArea(20,5);
                        if (defaultValue != null) {
                            textField2.setText(defaultValue);
                        }
                        
                        //TODO: special layout??
                        populateField(prompt,textField2, limitWidth);
                        break;
                    case BOOLEAN_CONTROL:
                        String[] items = new String[2];
                        if (defaultValue != null) {
                            boolean b = (new Boolean(defaultValue)).booleanValue(); // validated earlier
                            if (b) {
                                items[0] = "true";
                                items[1] = "false";
                            } else {
                                items[1] = "true";
                                items[0] = "false";
                            }
                        } else {
                            items[0] = "true";
                            items[1] = "false";
                        }
                        javax.swing.JComboBox box = new javax.swing.JComboBox(items);
                        populateField(prompt,box, limitWidth);
                        break;
                    case INTEGER_CONTROL:
                        javax.swing.JFormattedTextField textField4 = null;
                        if (defaultValue != null) {
                            textField4 = new javax.swing.JFormattedTextField(new Integer(defaultValue));
                        } else {
                            textField4 = new javax.swing.JFormattedTextField(new Integer(0));
                        }
                        populateField(prompt,textField4, limitWidth);
                        break;
                    case FLOAT_CONTROL:
                        javax.swing.JFormattedTextField textField5 = null;
                        if (defaultValue != null) {
                            textField5 = new javax.swing.JFormattedTextField(new Float(defaultValue));
                        } else {
                            textField5 = new javax.swing.JFormattedTextField(new Float(0.0));
                        }
                        populateField(prompt,textField5, limitWidth);
                        break;
                    case DATE_TIME_CONTROL:
                        javax.swing.JFormattedTextField textField6 = null;
                        if (defaultValue != null) {
                            textField6 = new javax.swing.JFormattedTextField(new java.util.Date(defaultValue));
                        } else {
                            textField6 = new javax.swing.JFormattedTextField(new java.util.Date());
                        }
                        populateField(prompt,textField6, limitWidth);
                        break;
                    case DURATION_CONTROL:
                        // TODO: maybe use a JFormattedTextField
                        javax.swing.JTextField textField7 = new javax.swing.JTextField();
                        if (numChars > 0) {
                            textField7.setColumns(numChars);
                        }
                        if (defaultValue != null) {
                            textField7.setText(defaultValue);
                        }
                        populateField(prompt,textField7, limitWidth);
                        break;
                    case FILECHOOSER_CONTROL:
                        textField8 = new javax.swing.JTextField();
                        if (numChars > 0) {
                            textField8.setColumns(numChars);
                        }
                        if (defaultValue != null) {
                            textField8.setText(defaultValue);
                        }
                        
                        chooser = new JButton(VueResources.getString("resourceInstallation.chooserOpen"));
                        
                        
                        chooserActionListener = new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                VueFileChooser fileChooser = VueFileChooser.getVueFileChooser();
                                fileChooser.setCurrentDirectory(new java.io.File("."));
                                fileChooser.setDialogTitle(VueResources.getString("resourceInstallation.chooserTitle"));
                                fileChooser.setFileSelectionMode(VueFileChooser.FILES_AND_DIRECTORIES);
                                if (fileChooser.showOpenDialog(chooser) == VueFileChooser.APPROVE_OPTION) {
                                    textField8.setText(fileChooser.getSelectedFile().toString());
                                }
                            }
                        };
                        
                        chooser.addActionListener(chooserActionListener);
                        populateField(prompt,textField8,chooser);
                        break;
                }
                
                String description = (String)descriptionVector.elementAt(i);
                if (description != null) {
                    prompt.setToolTipText(description);
                }                
            }
            VectorFocusTraversalPolicy policy = new VectorFocusTraversalPolicy(order);
            this.setFocusTraversalPolicy(policy);
            
            
            
        } catch (Exception ex) {
            this.errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        
    }
    
    private void populateField(javax.swing.JLabel prompt, javax.swing.JComponent component, boolean limitWidth) {

        //final java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        // as we're re-using the same GBC object, must set every used field each time
        
        //gbConstraints.weighty = 0;
        
        gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gbConstraints.fill = java.awt.GridBagConstraints.NONE;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridx = 0;
        gbConstraints.weightx = 0;
        
        add(prompt, gbConstraints);
        
        gbConstraints.gridx = 1;
        
        if (limitWidth) {

            // There appears to be no way to prevent a JTextField from collapsing to
            // virtually zero width if the container width allows for any less than it's
            // preferred size.  Adding it w/out settng gridwidth REMAINDER works great
            // as long as there's extra space -- it only takes the length of it's
            // preferred size.  But if any LESS space than that is available, it
            // completely collapses.  We can "fix" this by setting it's minimum size, in
            // which case it won't collapse, but then if maxChars wasn't config
            // specificied as a small minimum, but instead meant as an upper limit
            // maximum, then we could have a problem if the component minimum size is
            // taken literally (it would go off the right and be clipped, or even worse,
            // force it's parent container/window to expand to be that wide).
            // Fortunately, the mimimum size only appears to be attended to by GridBag
            // if it's actually less than preferred size, so this seems to work.
            
            //gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            //gbConstraints.anchor = java.awt.GridBagConstraints.CENTER;
            //gbConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
            //gbConstraints.weightx = 1;
            //gbConstraints.gridwidth = 1;
            
            component.setMinimumSize(component.getPreferredSize());
            add(component, gbConstraints);
            
//             gbConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
//             gbConstraints.fill = java.awt.GridBagConstraints.BOTH;
//             gbConstraints.weightx = 1;
//             gbConstraints.gridx = 2;
//             javax.swing.JComponent fill = new javax.swing.JPanel();
//             fill.setBackground(java.awt.Color.darkGray);
//             fill.setOpaque(true);
//             add(fill, gbConstraints);
//             //add(javax.swing.Box.createHorizontalGlue(), gbConstraints);
            
        } else {
            gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
            gbConstraints.weightx = 1;
            add(component, gbConstraints);
        }
        
        if (tufts.vue.DEBUG.BOXES)
            component.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.blue));        
        
        order.add(component);
        
        gbConstraints.gridy++;
        
        fieldVector.addElement(component);
    }
    
    private void populateField(javax.swing.JLabel prompt,
            javax.swing.JComponent component,
            javax.swing.JComponent component2) {

        //final java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        // as we're re-using the same GBC object, must set every used field each time
        
        gbConstraints.gridx = 0;
        gbConstraints.fill = java.awt.GridBagConstraints.NONE;
        gbConstraints.gridwidth = 1;
        gbConstraints.weightx = 0;
        add(prompt,gbConstraints);
        
        gbConstraints.gridx = 1;
        gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gbConstraints.weightx = 1;
        
        add(component,gbConstraints);
        if (tufts.vue.DEBUG.BOXES)
            component.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.orange));
        
        gbConstraints.gridx = 2;
        gbConstraints.fill = java.awt.GridBagConstraints.NONE;
        gbConstraints.gridwidth = 1;
        gbConstraints.weightx = 0;
        
        add(component2,gbConstraints);
        gbConstraints.gridy++;
        
        fieldVector.addElement(component);
    }
    
    public java.util.Properties getProperties() {
        java.util.Properties properties = new java.util.Properties();
        for (int i = 0, size = this.uiVector.size(); i < size; i++) {
            int uiCode = ((Integer)uiVector.elementAt(i)).intValue();
            String key = (String)keyVector.elementAt(i);
            
            switch( uiCode) {
                case SINGLE_LINE_CLEAR_TEXT_CONTROL:
                    javax.swing.JTextField field0 = (javax.swing.JTextField)fieldVector.elementAt(i);
                    String field0Value = field0.getText();
                    properties.setProperty(key,field0Value);
                    break;
                case SINGLE_LINE_MASKED_TEXT_CONTROL:
                    javax.swing.JPasswordField field1 = (javax.swing.JPasswordField)fieldVector.elementAt(i);
                    char[] password = field1.getPassword();
                    StringBuffer sb = new StringBuffer();
                    for (int j=0; j < password.length; j++) {
                        sb.append(password[j]);
                    }
                    //String enc = edu.tufts.vue.util.Encryption.encrypt(sb.toString());
                    //String dec = edu.tufts.vue.util.Encryption.decrypt(enc);
                    properties.setProperty(key,sb.toString());
                    //System.out.println("PASS: "+sb.toString()+ " encrypted to: "+ enc+" decrypted to: "+ dec);
                    break;
                case MULTI_LINE_TEXT_CONTROL:
                    javax.swing.JTextArea field2 = (javax.swing.JTextArea)fieldVector.elementAt(i);
                    String field2Value = field2.getText();
                    properties.setProperty(key,field2Value);
                    break;
                case BOOLEAN_CONTROL:
                    javax.swing.JComboBox box = (javax.swing.JComboBox)fieldVector.elementAt(i);
                    properties.setProperty(key,(String)box.getSelectedItem());
                    break;
                case INTEGER_CONTROL:
                    javax.swing.JFormattedTextField field4 = (javax.swing.JFormattedTextField)fieldVector.elementAt(i);
                    Number ivalue = (Number)field4.getValue();
                    properties.setProperty(key,ivalue.toString());
                    break;
                case FLOAT_CONTROL:
                    javax.swing.JFormattedTextField field5 = (javax.swing.JFormattedTextField)fieldVector.elementAt(i);
                    Number fvalue = (Number)field5.getValue();
                    properties.setProperty(key,fvalue.toString());
                    break;
                case DATE_TIME_CONTROL:
                    javax.swing.JFormattedTextField field6 = (javax.swing.JFormattedTextField)fieldVector.elementAt(i);
                    java.util.Date date = (java.util.Date)field6.getValue();
                    properties.setProperty(key,date.toString());
                    break;
                case DURATION_CONTROL:
                    javax.swing.JTextField field7 = (javax.swing.JTextField)fieldVector.elementAt(i);
                    properties.setProperty(key,field7.getText());
                    break;
                case FILECHOOSER_CONTROL:
                    properties.setProperty(key,textField8.getText());
                    break;
                case SINGLE_LINE_NONEDITABLE_TEXT_CONTROL:
                    javax.swing.JTextField field9 = (javax.swing.JTextField)fieldVector.elementAt(i);
                    String field9Value = field9.getText();
                    properties.setProperty(key,field9Value);
                    break;
                    
            }
        }
        return properties;
    }
    
    public void setProperties(java.util.Properties properties) {
        for (int i = 0, size = this.keyVector.size(); i < size; i++) {
            String key = (String)keyVector.elementAt(i);
            String value = properties.getProperty(key);
            if (value != null) {
                int uiCode = ((Integer)uiVector.elementAt(i)).intValue();
                
                switch( uiCode) {
                    case SINGLE_LINE_CLEAR_TEXT_CONTROL:
                        javax.swing.JTextField field0 = (javax.swing.JTextField)fieldVector.elementAt(i);
                        field0.setText(value);
                        break;
                    case SINGLE_LINE_MASKED_TEXT_CONTROL:
                        javax.swing.JPasswordField field1 = (javax.swing.JPasswordField)fieldVector.elementAt(i);
                        //String dec = edu.tufts.vue.util.Encryption.decrypt(value);
                        //field1.setText(dec);
                        field1.setText(value);
                        break;
                    case MULTI_LINE_TEXT_CONTROL:
                        javax.swing.JTextArea field2 = (javax.swing.JTextArea)fieldVector.elementAt(i);
                        field2.setText(value);
                        break;
                    case SINGLE_LINE_NONEDITABLE_TEXT_CONTROL:
                        javax.swing.JTextField field9 = (javax.swing.JTextField)fieldVector.elementAt(i);
                        field9.setText(value);
                        //field9.setEditable(false);
                        field9.setEnabled(false);
                        System.out.println(this + " setting Field"+field9.getText());
                        break;
                        //TO DO:  add more support for other property types
                }
            }
        }
    }
    
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("usage: ConfigurationUI xmlFilename");
            System.exit(1);
        }
        try {
            String filename = args[0];
            java.io.InputStream stream = new java.io.FileInputStream(new java.io.File(filename));
            javax.swing.JFrame frame = new javax.swing.JFrame();
            
            ConfigurationUI cui = new ConfigurationUI(stream);
            cui.setPreferredSize(new java.awt.Dimension(420,300));
            if (javax.swing.JOptionPane.showOptionDialog(frame,
                    cui,
                    "Update Resource Configuration",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[] {
                "Cancel", "Update"}, "Update") == 1) {
                System.out.println("Properties: " + cui.getProperties());
            }
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static class VectorFocusTraversalPolicy
    extends FocusTraversalPolicy
    {
    	Vector<Component> order;

    	public VectorFocusTraversalPolicy(Vector<Component> order) {
    		this.order = new Vector<Component>(order.size());
    		this.order.addAll(order);
    	}
    	public Component getComponentAfter(Container focusCycleRoot,
                             Component aComponent)
    	{
    		int idx = (order.indexOf(aComponent) + 1) % order.size();
    		return order.get(idx);
    	}

    	public Component getComponentBefore(Container focusCycleRoot,
                              Component aComponent)
    	{
    		int idx = order.indexOf(aComponent) - 1;
    		if (idx < 0) {
    			idx = order.size() - 1;
    		}
    		return order.get(idx);
    	}

    	public Component getDefaultComponent(Container focusCycleRoot) {
    		return order.get(0);
    	}

    	public Component getLastComponent(Container focusCycleRoot) {
    		return order.lastElement();
    	}

    	public Component getFirstComponent(Container focusCycleRoot) {
    		return order.get(0);
    	}	
    }
}
