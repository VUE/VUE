package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;


class LWCInfoPanel extends javax.swing.JPanel
implements VueConstants,
LWSelection.Listener,
LWComponent.Listener,
ActionListener {
    private JTextField labelField = new JTextField(15);
    private JTextField resourceField = new JTextField();
    
    private JPanel fieldPane = new JPanel();
    private JPanel resourceMetadataPanel = new JPanel();
    private JPanel metadataPane = new JPanel();
    private PropertiesEditor propertiesEditor = null;
    
    private Object[] labelTextPairs = {
        "Label",    labelField,
        "Resource", resourceField,
    };
    
    private LWComponent lwc;
    
    public LWCInfoPanel() {
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        fieldPane.setLayout(gridBag);
        addLabelTextRows(labelTextPairs, gridBag, fieldPane);
        // settting metadata
        setUpMetadataPane();
        setLayout(new BorderLayout());
        add(fieldPane, BorderLayout.CENTER);
        add(metadataPane,BorderLayout.SOUTH);
        VUE.ModelSelection.addListener(this);
    }
    
    private void setUpMetadataPane() {
        BoxLayout layout = new BoxLayout(metadataPane,BoxLayout.Y_AXIS);
        metadataPane.setLayout(new BorderLayout());
        metadataPane.add(resourceMetadataPanel,BorderLayout.WEST);
        
    }
    
    private void addLabelTextRows(Object[] labelTextPairs,
    GridBagLayout gridbag,
    Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;
        
        for (int i = 0; i < num; i += 2) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            c.anchor = GridBagConstraints.WEST;
            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            if (txt.startsWith("-")) {
                txt = txt.substring(1);
                readOnly = true;
            }
            txt += ": ";
            
            JLabel label = new JLabel(txt);
            //JLabel label = new JLabel(labels[i]);
            //label.setFont(VueConstants.SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);
            
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            
            c.weightx = 1.0;
            
            JComponent field = (JComponent) labelTextPairs[i+1];
            //field.setFont(VueConstants.SmallFont);
            if (field instanceof JTextField)
                ((JTextField)field).addActionListener(this);
            gridbag.setConstraints(field, c);
            container.add(field);
            
            
            
            if (readOnly) {
                field.setBorder(new EmptyBorder(1,1,1,1));
                if (field instanceof JTextField) {
                    JTextField tf = (JTextField) field;
                    tf.setEditable(false);
                    tf.setFocusable(false);
                }
                if (VueUtil.isMacPlatform())
                    field.setBackground(SystemColor.control);
            }
        }
        JLabel field  = new JLabel("Metadata");
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(field, c);
        container.add(field);
    }
    
    
    public void LWCChanged(LWCEvent e) {
        if (this.lwc != e.getSource())
            return;
        
        if (e.getWhat() == LWKey.Deleting) {
            this.lwc = null;
            setAllEnabled(false);
        } else if (e.getSource() != this)
            loadItem(this.lwc);
    }
    
    public void selectionChanged(LWSelection selection) {
        if (selection.isEmpty() || selection.size() > 1)
            setAllEnabled(false);
        else
            loadItem(selection.first());
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
    
    private void setAllEnabled(boolean tv) {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(tv);
        }
    }
    
    private void loadItem(LWComponent lwc) {
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            if (this.lwc != null) {
                this.lwc.addLWCListener(this, new Object[] { LWKey.Label, LWKey.Resource, LWKey.Deleting });
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
        
        if (c.getResource() != null)
            loadText(resourceField, c.getResource().toString());
        else
            loadText(resourceField, "");
        
        loadText(labelField, c.getLabel());
        
        //loading the metadata if it exists
        if(propertiesEditor == null) {
            if(c.getResource() != null && c.getResource().getProperties() != null) {
                if(c.getResource().getType() == Resource.ASSET_FEDORA)
                    propertiesEditor = new PropertiesEditor(c.getResource().getProperties(), false);
                else
                    propertiesEditor = new PropertiesEditor(c.getResource().getProperties(), true);
                resourceMetadataPanel = propertiesEditor;
                metadataPane.add(resourceMetadataPanel,BorderLayout.WEST);
            }
        } else {
            if(c.getResource() != null && c.getResource().getProperties() != null) {
                if(c.getResource().getType() == Resource.ASSET_FEDORA)
                    propertiesEditor.setProperties(c.getResource().getProperties(), false);
                else
                    propertiesEditor.setProperties(c.getResource().getProperties(), true);
            } else {
                propertiesEditor.clear();
            }
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        if (this.lwc == null)
            return;
        String text = e.getActionCommand();
        Object src = e.getSource();
        LWComponent c = this.lwc;
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
    
    
}
