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

package tufts.vue;

import java.util.Iterator;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;


class LWCInspector extends javax.swing.JPanel
    implements VueConstants,
               LWSelection.Listener,
               LWComponent.Listener,
               ActionListener
{
    private JLabel idField = new JLabel();
    private JLabel locationField = new JLabel();
    private JLabel sizeField = new JLabel();
    private JTextField labelField = new JTextField(15);
    private JTextField widthField = new JTextField();
    private JTextField heightField = new JTextField();
    private JTextField xField = new JTextField();
    private JTextField yField = new JTextField();
    private JTextField zoomField = new JTextField();
    private JTextField fontField = new JTextField();
    private JTextField strokeField = new JTextField();
    private JTextField fillColorField = new JTextField();
    private JTextField textColorField = new JTextField();
    private JTextField strokeColorField = new JTextField();
    private JTextField categoryField = new JTextField();
    private JTextField resourceField = new JTextField();
    private JTextField notesField = new JTextField();
    private JPanel extraPanel = new JPanel();
    
    //private JTextArea notesField = new JTextArea(1, 20);

    private JPanel fieldPane = new JPanel();
    private JPanel resourceMetadataPanel = new JPanel();
    private JPanel metadataPane = new JPanel();

    //String[] labels = { "ID", "<html><font color=red>Label</font></html>", "Category", "Resource", "Notes" };
    private Object[] labelTextPairs = {
        "-ID",      idField,
        "-Location",locationField,
        "-Size",    sizeField,
        "Label",    labelField,
        "Width",    widthField,
        "Height",    heightField,
        "X",         xField,
        "Y",         yField,
        "Zoom",    zoomField,
        "Font",     fontField,
        "Stroke",   strokeField,
        "Fill Color",fillColorField,
        "Text Color",textColorField,
        "Stroke Color",strokeColorField,
        "Resource", resourceField,
        //"Category", categoryField,
        //"-Notes",    notesField,
        //"Extra",    extraPanel,
    };
    
    public LWCInspector()
    {
        //extraPanel.setLayout(new BorderLayout());
        //extraPanel.setSize(200,100);
        //extraPanel.add(new JLabel("foo"));
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        fieldPane.setLayout(gridBag);

        /*
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        //gridbag.setConstraints(actionLabel, c);
        //textControlsPane.add(actionLabel);
        */

        if (!(notesField instanceof JTextField))
            notesField.setBorder(LineBorder.createGrayLineBorder());
        
        addLabelTextRows(labelTextPairs, gridBag, fieldPane);
        // settting metadata
        //setUpMetadataPane();

        setLayout(new BorderLayout());
        add(fieldPane, BorderLayout.CENTER);
        //add(metadataPane,BorderLayout.SOUTH);

        VUE.ModelSelection.addListener(this);
    }

    private void setUpMetadataPane() {
        BoxLayout layout = new BoxLayout(metadataPane,BoxLayout.Y_AXIS);
        metadataPane.setLayout(layout);
        metadataPane.add(resourceMetadataPanel);
    }
    
    private void addLabelTextRows(Object[] labelTextPairs,
                                  GridBagLayout gridbag,
                                  Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;

        for (int i = 0; i < num; i += 2) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            c.anchor = GridBagConstraints.EAST;
            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            if (txt.startsWith("-")) {
                txt = txt.substring(1);
                readOnly = true;
            } 
            txt += ": ";

            JLabel label = new JLabel(txt);
            label.setFont(VueConstants.SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
          
            c.weightx = 1.0;

            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setFont(VueConstants.SmallFont);
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
        /*
          JComponent field = new JLabel("Metadata");
          c.gridwidth = GridBagConstraints.RELATIVE;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(field, c);
            container.add(field);
        */
    }
    

    public void LWCChanged(LWCEvent e)
    {
        if (!isShowing())
            return;

        //System.out.println(this + " " + e);
        if (this.lwc != e.getSource())
            return;
        if (e.getWhat() == LWKey.Deleting) {
            this.lwc = null;
            //loadItem(null);
            setAllEnabled(false);
        }
        else if (e.getSource() != this)
            loadItem(this.lwc);
    }
    
    public void selectionChanged(LWSelection selection)
    {
        setSelection(selection);
    }

    private void loadText(JTextComponent c, String text)
    {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }

    private LWComponent lwc; // temporary
    public void setSelection(LWSelection selection)
    {
        //System.err.println("Inspector setSelection: " + sl);

        if (selection.size() == 1)
            loadItem(selection.first());
        else //if (!selection.isEmpty())
            loadSelection(selection);
    }

    private void setAllEnabled(boolean tv)
    {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(tv);
        }
    }

    private void disable(JTextComponent tc)
    {
        tc.setText("");
        tc.setEnabled(false);
    }
    private void disable(JLabel l)
    {
        l.setText("");
        l.setEnabled(false);
    }

    private void loadSelection(LWSelection selection)
    {
        if (selection.isEmpty()) {
            loadItem(VUE.getActiveMap());
            return;
        }
        setAllEnabled(true);

        String id = "";
        if (selection.allOfSameType())
            id += selection.first().getClass().getName();
        else
            id += "LWComponent";
        id += "[" + selection.size() + "]";
        
        LWComponent c = selection.first();
        // grab first in selection for moment
        idField.setText(id);
        labelField.setBackground(c.getFillColor());
        loadText(labelField, "");
        loadText(categoryField, "");
        
        disable(notesField);
        disable(resourceField);
        disable(locationField);
        disable(sizeField);
        
        fontField.setText("");
        fillColorField.setText(c.getXMLfillColor());
        textColorField.setText(c.getXMLtextColor());
        strokeColorField.setText(c.getXMLstrokeColor());
        strokeField.setText(""+c.getStrokeWidth());
    }

    
    private void loadItem(LWComponent lwc)
    {
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            if (this.lwc != null) {
                this.lwc.addLWCListener(this);
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

        String id = c.getID();
        if (c.getParent() == null)
            id += " [PARENT IS NULL]";
        else {
            id += ", #" + c.getParent().getLayer(c);
            id += " in <" + c.getParent().getLabel() + ">";
            id += " links:" + c.getLinkRefs().size();
            if (c instanceof LWContainer && c.hasChildren())
                id += " children:" + ((LWContainer)c).getChildList().size();
        }
        
        idField.setText(id);
        labelField.setBackground(c.getFillColor());
        loadText(labelField, c.getLabel());
        //loadText(categoryField, c.getCategory());
        loadText(notesField, c.getNotes());
        //loadText(widthField, new Float(c.getWidth()));
        //loadText(heightField, new Float(c.getHeight()).toString());
        
        locationField.setText("x: " + c.getX() + "   y: " + c.getY());
        String sizeText = c.getWidth() + " x " + c.getHeight();
        if (c.getScale() != 1f)
            sizeText += "  z" + c.getScale();
        if (!c.isAutoSized())
            sizeText += " userSize";
        sizeField.setText(sizeText);
        widthField.setText(""+c.getAbsoluteWidth());
        heightField.setText(""+c.getAbsoluteHeight());
        xField.setText(""+c.getX());
        yField.setText(""+c.getY());
        zoomField.setText(""+c.getScale());
        //Font f = c.getFont();
        //if (c.getScale() != 1)
        //  fontString += " (" + (f.getSize()*c.getScale()) + ")";
        fontField.setText(c.getXMLfont());
        // todo: font.getSize2D()?
        //fontField.setText(f.getName() + "-" + fontSize);
        //sizeField.setText(c.getWidth() + "x" + c.getHeight());
        
        fillColorField.setText(c.getXMLfillColor());
        textColorField.setText(c.getXMLtextColor());
        strokeColorField.setText(c.getXMLstrokeColor());
        strokeField.setText(""+c.getStrokeWidth());
        
        //loading the metadata if it exists
        /*
        if(c.getResource() != null && c.getResource().getProperties() != null) {
            metadataPane.remove(resourceMetadataPanel);
            if(c.getResource().getType() == Resource.ASSET_FEDORA)
                resourceMetadataPanel = new PropertiesEditor(c.getResource().getProperties(), false);
            else
                resourceMetadataPanel = new PropertiesEditor(c.getResource().getProperties(), true);
            metadataPane.add(resourceMetadataPanel);
        }
        */
        /*
        if (false&&c.labelBox != null) {
            //extraPanel.add(p);
            if (lastp != null) {
            //System.out.println("REMOVING " + lastp);
                VUE.toolPanel.remove(lastp);
            }
            JPanel np = new JPanel();
            //System.out.println("ADDING " + p);
            VUE.toolPanel.add(np, BorderLayout.CENTER);
            System.out.println("ADDING MTP TO VUE for " + c);
            np.add(c.labelBox);
            lastp = np;
        }
        */
    }

    LWSelection dummySelection = (LWSelection) new LWSelection().clone(); // clone so won't set selection bits
    private LWSelection getSelection() {
        if (VUE.getSelection().isEmpty()) {
            dummySelection.setTo(this.lwc);
            return dummySelection;
        } else
            return VUE.getSelection();
    }

    private void setFillColors(String text) {
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setXMLfillColor(text);
        }
    }
    private void setTextColors(String text) {
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setXMLtextColor(text);
        }
    }
    private void setStrokeColors(String text) {
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setXMLstrokeColor(text);
        }
    }
    private void setStrokeWidths(String text)
        throws NumberFormatException
    {
        float w = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setStrokeWidth(w);
        }
    }
    private void setWidths(String text)
        throws NumberFormatException
    {
        float w = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setAbsoluteSize(w, c.getAbsoluteHeight());
        }
    }
    private void setHeights(String text)
        throws NumberFormatException
    {
        float h = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setAbsoluteSize(c.getAbsoluteWidth(), h);
        }
    }
    private void setXs(String text)
        throws NumberFormatException
    {
        float v = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(v, c.getY());
        }
    }
    private void setYs(String text)
        throws NumberFormatException
    {
        float v = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(c.getX(), v);
        }
    }
    private void setScales(String text)
        throws NumberFormatException
    {
        float s = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(s);
        }
    }
    private void setFonts(String text)
        throws NumberFormatException
    {
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setXMLfont(text);
        }
    }


    public void actionPerformed(ActionEvent e)
    {
        //if (this.lwcomponent == null)
        if (this.lwc == null)
            return;
        String text = e.getActionCommand();
        Object src = (JTextComponent) e.getSource();
        LWComponent c = this.lwc;
        //System.out.println("Inspector " + e);
        try {
            boolean set = true;
            if (src == labelField)          c.setLabel(text);
            //else if (src == categoryField)  c.setCategory(text);
            //else if (src == notesField)     c.setNotes(text);
            else if (src == widthField)         setWidths(text);
            else if (src == heightField)        setHeights(text);
            else if (src == zoomField)          setScales(text);
            else if (src == resourceField)      c.setResource(text);
            else if (src == fontField)          setFonts(text);
            else if (src == fillColorField)     setFillColors(text);
            else if (src == textColorField)     setTextColors(text);
            else if (src == strokeColorField)   setStrokeColors(text);
            else if (src == strokeField)        setStrokeWidths(text);
            else if (src == xField)             setXs(text);
            else if (src == yField)             setYs(text);
            //            else if (src == strokeField) {
            //                float w = Float.parseFloat(text);
            //                c.setStrokeWidth(w);
            //            }
            else
                set = false;
            if (set)
                VUE.getUndoManager().mark();
            else
                return;
        } catch (Exception ex) {
            System.err.println(ex);
            System.err.println("LWCInspector: error setting property value ["+text+"] on " + src);
        }
        
        transferFocus(); // this isn't going to next field
        
        //todo: getNextFocusableComponent().requestFocus();
        // Try below with next-focus action for enter?
        //   component.getInputMap().put(aKeyStroke, aCommand);
        //   component.getActionMap().put(aCommmand, anAction);
        // Or: just have something else request focus.
        // Could even track to the next component ourself,
        // tho it seems rediculous to manage ourselves given
        // there's a BOODLE of awt code for this -- so
        // why isn't this dead easy???

    }

    public String toString()
    {
        return "LWCInspector@" + Integer.toHexString(hashCode());
    }


}
