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
    private LWSelection selection;

    JLabel idField = new JLabel();
    JLabel locationField = new JLabel();
    JLabel sizeField = new JLabel();
    JTextField labelField = new JTextField(15);
    JTextField fontField = new JTextField();
    JTextField strokeField = new JTextField();
    JTextField fillColorField = new JTextField();
    JTextField textColorField = new JTextField();
    JTextField strokeColorField = new JTextField();
    JTextField categoryField = new JTextField();
    JTextField resourceField = new JTextField();
    JTextField notesField = new JTextField();
    
    //JTextArea notesField = new JTextArea(1, 20);

    JPanel fieldPane = new JPanel();

        //String[] labels = { "ID", "<html><font color=red>Label</font></html>", "Category", "Resource", "Notes" };
        Object[] labelTextPairs = {
            "-ID",      idField,
            "-Location",locationField,
            "-Size",    sizeField,
            "Label",    labelField,
            "Font",     fontField,
            "Stroke",   strokeField,
            "Fill Color",fillColorField,
            "Text Color",textColorField,
            "Stroke Color",strokeColorField,
            "Category", categoryField,
            "Resource", resourceField,
            "Notes",    notesField,
        };

    public LWCInspector()
    {
        setBorder(new TitledBorder("Inspector"));
        
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

        //removeListeners(idField, MouseListener.class);
        //removeListeners(idField, MouseMotionListener.class);
        // failed experiment to see if removing mouse focus
        // from a text field would let the events pass thu
        // to parent window, as they do with JLabels

        setLayout(new BorderLayout());
        add(fieldPane, BorderLayout.CENTER);

        VUE.ModelSelection.addListener(this);
    }

    /*
    private void removeListeners(Component c, Class listenerType)
    {
        java.util.EventListener[] listeners = c.getListeners(listenerType);
        for (int i = 0; i < listeners.length; i++) {
            java.util.EventListener l = listeners[i];
            System.out.println("Removing "
                               + listenerType.getName()
                               + " " + l);
            if (l instanceof MouseListener)
                c.removeMouseListener((MouseListener)l);
            else if (l instanceof MouseMotionListener)
                c.removeMouseMotionListener((MouseMotionListener)l);
        }

    }
    */

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

            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            if (txt.startsWith("-")) {
                txt = txt.substring(1);
                readOnly = true;
            } 
            txt += ": ";

            JLabel label = new JLabel(txt);
            //JLabel label = new JLabel(labels[i]);
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
    }
    

    public void LWCChanged(LWCEvent e)
    {
        //System.out.println(this + " " + e);
        if (this.lwc != e.getSource())
            return;
        if (e.getWhat().equals("deleting")) {
            this.lwc = null;
            //loadItem(null);
            setAllEnabled(false);
        }
        else if (e.getSource() != this)
            loadItem(this.lwc);

            /* this possible now because children of our displayed LWC
               will pass their events up to us also.
            throw new IllegalStateException("unexpected update event: " + e
                                            + "\n\tshowing: " + lwc
                                            + "\n\t    got: " + e.getComponent());
            */
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
        this.selection = selection;
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
        this.selection = selection;
        if (selection.isEmpty()) {
            setAllEnabled(false);
            return;
        }
        setAllEnabled(true);

        String id = "<selection> ";
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

        if (c instanceof LWNode) { // todo: instanceof Node interface
            if (c.getResource() != null)
                loadText(resourceField, c.getResource().toString());
            else
                loadText(resourceField, "");
            resourceField.setEditable(true);
            //resourceLabel.setVisible(true);
            //resourceField.setVisible(true);
        } else {
            loadText(resourceField, "");
            resourceField.setEditable(false);
            //resourceLabel.setVisible(false);
            //resourceField.setVisible(false);
        }

        String id = c.getID();
        if (c.getParent() == null)
            id += " [PARENT IS NULL]";
        else {
            id += " [parent: " + c.getParent().getLabel() + "]";
            id += " z:" + c.getParent().getLayer(c);
        }
        
        idField.setText(id);
        labelField.setBackground(c.getFillColor());
        loadText(labelField, c.getLabel());
        loadText(categoryField, c.getCategory());
        loadText(notesField, c.getNotes());
        //loadText(widthField, new Float(c.getWidth()));
        //loadText(heightField, new Float(c.getHeight()).toString());
        
        locationField.setText("x: " + c.getX() + "   y: " + c.getY());
        String sizeText = c.getWidth() + " x " + c.getHeight();
        if (c.getScale() != 1f)
            sizeText += "  z" + c.getScale();
        sizeField.setText(sizeText);
        //Font f = c.getFont();
        //if (c.getScale() != 1)
        //  fontString += " (" + (f.getSize()*c.getScale()) + ")";
        fontField.setText(c.getXMLfont());
        //fontField.setText(f.getName() + "-" + fontSize);
        //sizeField.setText(c.getWidth() + "x" + c.getHeight());
        
        fillColorField.setText(c.getXMLfillColor());
        textColorField.setText(c.getXMLtextColor());
        strokeColorField.setText(c.getXMLstrokeColor());
        strokeField.setText(""+c.getStrokeWidth());
    }

    private void setFillColors(String text)
    {
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setXMLfillColor(text);
        }
    }
    private void setStrokeWidths(String text)
        throws NumberFormatException
    {
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float w = Float.parseFloat(text);
            c.setStrokeWidth(w);
        }
    }
    private void setFonts(String text)
        throws NumberFormatException
    {
        Iterator i = selection.iterator();
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
            if (src == labelField)          c.setLabel(text);
            else if (src == categoryField)  c.setCategory(text);
            else if (src == notesField)     c.setNotes(text);
            else if (src == resourceField)  c.setResource(text);
            else if (src == fontField)      setFonts(text);
            else if (src == fillColorField) setFillColors(text);
            else if (src == textColorField)     c.setXMLtextColor(text);
            else if (src == strokeColorField)   c.setXMLstrokeColor(text);
            else if (src == strokeField)        setStrokeWidths(text);
            //            else if (src == strokeField) {
            //                float w = Float.parseFloat(text);
            //                c.setStrokeWidth(w);
            //            }
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
