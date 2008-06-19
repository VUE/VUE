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

package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;

import tufts.vue.gui.*;

// TODO FIX: the text input fields should save value on focus loss

/**
 * @version $Revision: 1.1 $ / $Date: 2008-06-19 03:46:58 $ / $Author: sfraize $
 */

class LWCInfoPanel extends javax.swing.JPanel
    implements VueConstants,
               //LWSelection.Listener,
               LWComponent.Listener,
               ActionListener,
               FocusListener
{
    //private JTextField labelField = new JTextField(15);
    //private JTextComponent labelField = new JTextPane();
    private JTextComponent labelField = new VueTextPane();
    
    //private JLabel resourceField = new JLabel();
    //private JTextComponent resourceField = new tufts.vue.gui.VueTextField();
    //private JTextArea resourceField = new JTextArea();
    //private JTextComponent resourceField = new JTextPane();
    private JTextComponent resourceField = new VueTextPane();
    
    private JLabel sizeField = new JLabel();
    
    private JPanel fieldPane = new JPanel();
    private JPanel resourceMetadataPanel = new JPanel();
    private JPanel metadataPane = new JPanel();
    private PropertiesEditor propertiesEditor = null;
    
    private Object[] labelTextPairs = {
        "Label",    labelField,
        "Resource", resourceField,
        "-Size",    sizeField,
    };
    
    private LWComponent lwc;
    

    public LWCInfoPanel()
    {
        super(new BorderLayout());
        
        setOpaque(false);
        
        if (!GUI.isMacAqua()) {
            Border textPaneBorder = BorderFactory.createEtchedBorder();
            //Border textPaneBorder = BorderFactory.createLineBorder(Color.lightGray);
            labelField.setBorder(textPaneBorder);
            resourceField.setBorder(textPaneBorder);
        }
        labelField.setEditable(false);

        resourceField.setEditable(false);
        resourceField.setOpaque(false);
        resourceField.setBorder(null);
        
        //setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        fieldPane.setLayout(gridBag);
        fieldPane.setOpaque(false);
        addLabelTextRows(labelTextPairs, gridBag, fieldPane);
        // settting metadata
        setUpMetadataPane();
        metadataPane.setOpaque(false);
        add(fieldPane, BorderLayout.NORTH);
        add(metadataPane,BorderLayout.CENTER);
        //VUE.ModelSelection.addListener(this);
        VUE.addActiveListener(LWComponent.class, this);
    }

    private void setUpMetadataPane() {
        metadataPane.setLayout(new BorderLayout());
        metadataPane.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        // todo: bug in properties editor:  will never display edit elements unless create editable initially
        propertiesEditor = new PropertiesEditor(false);
        metadataPane.add(propertiesEditor,BorderLayout.CENTER);
        validate();
    }
    
    private void addLabelTextRows(Object[] labelTextPairs, GridBagLayout gridbag, Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;
        boolean lastWasLabelAbove = false;

        Border lastBorder = null;
        
        for (int i = 0; i < num; i += 2) {
            
            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            boolean labelAbove = false;
            if (txt.charAt(0) == '-') {
                txt = txt.substring(1);
                readOnly = true;
            } else if (txt.charAt(0) == '+') {
                labelAbove = true;
                txt = txt.substring(1);
            }
            txt += ": ";
            
            //-------------------------------------------------------
            // Add the label field
            //-------------------------------------------------------

            int topPad = lastWasLabelAbove ? 3 : 1;
            
            if (labelAbove) {
                c.insets = new Insets(topPad, 0, 0, 0);
                c.gridwidth = GridBagConstraints.REMAINDER; // last in row
                c.anchor = GridBagConstraints.WEST;
            } else {
                c.insets = new Insets(topPad, 0, 1, 0);
                c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
                c.anchor = GridBagConstraints.EAST;
                // this makes labels stay at top left of multi-line fields, tho it throws off
                // baseline alignment for normal cases.  What we REALLY want is a baseline
                // alignment against the first line of text in the field.
                // c.anchor = GridBagConstraints.NORTHEAST;
            }
            c.fill = GridBagConstraints.NONE; // the label never grows
            c.weightx = 0.0;                  // reset
            
            JLabel label = new JLabel(txt);
            gridbag.setConstraints(label, c);
            container.add(label);
            label.setFont(FONT_NARROW);

            //-------------------------------------------------------
            // Add the text value field
            //-------------------------------------------------------
            
            c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
            c.fill = GridBagConstraints.HORIZONTAL;
            if (labelAbove)
                c.insets = new Insets(0, 0, 0, 0);
            else
                c.insets = new Insets(0, 0, 1, 0);
            c.weightx = 1.0;
            
            JComponent field = (JComponent) labelTextPairs[i+1];
            //field.setFont(VueConstants.SmallFont);
            if (field instanceof JTextField) {
                //((JTextField)field).setHorizontalAlignment(JTextField.LEFT);
                ((JTextField)field).addActionListener(this);
                   ((JTextField)field).addFocusListener(this);
            }
            //field.setFont(FONT_NARROW);
            gridbag.setConstraints(field, c);
            container.add(field);

            if (lastBorder != null && field instanceof JTextPane) {
                field.setBorder(lastBorder);
            }
            
            
            if (readOnly) {
                Border b = field.getBorder();
                //System.out.println("LWCInfoPanel: got border " + b);
                //lastBorder = b;
                if (b != null) {
                    final Insets borderInsets = b.getBorderInsets(field);
                    System.out.println("LWCInfoPanel: got border insets " + borderInsets + " for " + field);
                    field.putClientProperty(VueTextField.ActiveBorderKey, b);
                    Border emptyBorder = new EmptyBorder(borderInsets);
                    field.putClientProperty(VueTextField.InactiveBorderKey, emptyBorder);
                    field.setBorder(emptyBorder);
                }
                //field.setBorder(new EmptyBorder(1,1,1,1));
                if (field instanceof JTextComponent) {
                    JTextComponent tc = (JTextComponent) field;
                    tc.setEditable(false);
                    tc.setFocusable(false);
                }
                if (VueUtil.isMacPlatform()) {
                    //field.setBackground(SystemColor.control);
                    field.setOpaque(false);
                }
            }

            lastWasLabelAbove = labelAbove;
        }
        /**
         * JLabel field  = new JLabel("Metadata");
         * c.gridwidth = GridBagConstraints.REMAINDER;     //end row
         * c.fill = GridBagConstraints.HORIZONTAL;
         * c.anchor = GridBagConstraints.WEST;
         * gridbag.setConstraints(field, c);
         * container.add(field);
         */
    }
    
    
    public void LWCChanged(LWCEvent e) {
        if (this.lwc != e.getSource())
            return;
        
        if (e.key == LWKey.Deleting) {
            this.lwc = null;
            setAllEnabled(false);
        } else if (e.getSource() != this)
            loadItem(this.lwc);
    }
    
//     public void selectionChanged(LWSelection selection) {
//         if (selection.isEmpty() || selection.size() > 1)
//             setAllEnabled(false);
//         else
//             loadItem(selection.first());
//     }

    public void activeChanged(ActiveEvent e, LWComponent c) {
        if (c == null)
            setAllEnabled(false);
        else
            loadItem(c);
    }
    
    
    private void loadText(JTextComponent c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    private void loadText(JLabel c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    
    private void setAllEnabled(boolean tv) {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(tv);
        }
        resourceMetadataPanel.setEnabled(tv);
        metadataPane.setEnabled(tv);
        propertiesEditor.setEnabled(tv);
    }
    
    private void loadItem(LWComponent lwc) {
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            if (this.lwc != null) {
                this.lwc.addLWCListener(this, LWKey.Label, LWKey.Resource, LWKey.Deleting);
                setAllEnabled(true);
            } else
                setAllEnabled(false);
        }
        
        //System.out.println(this + " loadItem " + lwc);
        LWComponent c = this.lwc;
        if (c == null)
            return;
        
        setAllEnabled(true);
        //System.out.println(this + " loading " + c);

        final Resource r = c.getResource();
        
        if (r != null)
            loadText(resourceField, r.getSpec());
        else
            loadText(resourceField, "");
        
        loadText(labelField, c.getLabel());

        String ss = VueUtil.abbrevBytes(r.getByteSize());
        sizeField.setText(ss);
        
        //loading the metadata if it exists
        if (c.getResource() != null) {
            PropertyMap properties = c.getResource().getProperties();
            if (properties != null) {
                if (c.getResource().getClientType() == Resource.ASSET_FEDORA)
                    propertiesEditor.setProperties(properties, false);
                else
                    propertiesEditor.setProperties(properties, true);
            }
            
        } else {
            propertiesEditor.clear();
        }
        
        
    }
    
    public void actionPerformed(ActionEvent e) {
        if (this.lwc == null)
            return;
        String text = e.getActionCommand();
        Object src = e.getSource();
        LWComponent c = this.lwc;
        updateLWComponent(text,src,c);
    }
    public void focusGained(FocusEvent e) {
    }
    
    public void focusLost(FocusEvent e) {
        if (this.lwc == null)
            return;
        String text = ((JTextField)e.getSource()).getText();
        Object src = e.getSource();
        LWComponent c = this.lwc;
        updateLWComponent(text,src,c);
        
    }
    
    public void updateLWComponent(String text,Object src,LWComponent c) {
         try {
            boolean set = true;
            if (src == labelField)
                c.setLabel(text);
            else if (src == resourceField)
                c.setResource(text);
            else
                set = false;
            if (set)
                VUE.getUndoManager().mark();
            else
                return;
        } catch (Exception ex) {
            System.err.println(ex);
            System.err.println("LWCInfoPanel: error setting property value ["+text+"] on " + src);
        }
    }
    public String toString() {
        return "LWCInfoPanel@" + Integer.toHexString(hashCode());
    }

    public static void main(String args[]) {

        VUE.init(args);

        LWCInfoPanel p = new LWCInfoPanel();
        LWComponent node = new LWNode("Test Node");
        node.setNotes("I am a note.");
        node.setResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        Resource r = node.getResource();
        for (int i = 1; i < 6; i++)
            r.setProperty("field_" + i, "value_" + i);
        if (args.length > 1) {
            //ToolWindow w = VUE.createToolWindow("LWCInfoPanel", p);
            DockWindow w = new DockWindow("LWCInfoPanel", p);
            w.setVisible(true);
        } else
            tufts.Util.displayComponent(p);
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

        // Must have at least ONE active frame for our focus manager to work
        new Frame("An Active Frame").setVisible(true);
        
        /*
        p.setAllEnabled(true);
        p.labelField.setEditable(true);
        p.labelField.setEnabled(true);
        p.resourceField.setEditable(true);
        p.resourceField.setEnabled(true);
        */
    }
    
    
    
}
