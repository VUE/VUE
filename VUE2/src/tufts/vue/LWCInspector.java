package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;


class LWCInspector extends javax.swing.JPanel
    implements VueConstants,
               MapSelectionListener,
               LWCListener,
               ActionListener
{
    java.util.List selectionList;

    JLabel idField = new JLabel();
    JLabel locationField = new JLabel();
    JLabel sizeField = new JLabel();
    JTextField fontField = new JTextField();
    JTextField labelField = new JTextField(15);
    JTextField colorField = new JTextField();
    JTextField categoryField = new JTextField();
    JTextField resourceField = new JTextField();
    JTextField notesField = new JTextField();
    
    //JTextArea notesField = new JTextArea(1, 20);

    JPanel fieldPane = new JPanel();

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

        //String[] labels = { "ID", "<html><font color=red>Label</font></html>", "Category", "Resource", "Notes" };
        Object[] labelTextPairs = {
            "-ID",      idField,
            "-Location",locationField,
            "-Size",    sizeField,
            "Font",     fontField,
            "Label",    labelField,
            "Fill Color",colorField,
            "Category", categoryField,
            "Resource", resourceField,
            "Notes",    notesField,
        };

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
        if (e.getSource() == this)
            return;
        if (this.lwc != e.getComponent())
            return;
            /* this possible now because children of our displayed LWC
               will pass their events up to us also.
            throw new IllegalStateException("unexpected update event: " + e
                                            + "\n\tshowing: " + lwc
                                            + "\n\t    got: " + e.getComponent());
            */
        loadItem(this.lwc);
    }
    
    public void eventRaised(MapSelectionEvent e)
    {
        setSelection(e.getList());
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
    public void setSelection(java.util.List sl)
    {
        this.selectionList = sl;
        //System.err.println("Inspector setSelection: " + sl);

        if (selectionList.size() > 1)
            return;

        LWComponent lwc = (LWComponent) sl.get(0);
        loadItem(lwc);
    }

    private void loadItem(LWComponent lwc)
    {
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            this.lwc.addLWCListener(this);
        }

        if (lwc != null) {
            if (lwc instanceof LWNode) { // todo: instanceof Node interface
                if (lwc.getResource() != null)
                    loadText(resourceField, lwc.getResource().toString());
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

            idField.setText(lwc.getID());
            labelField.setBackground(lwc.getFillColor());
            loadText(labelField, lwc.getLabel());
            loadText(categoryField, lwc.getCategory());
            loadText(notesField, lwc.getNotes());
            //loadText(widthField, new Float(lwc.getWidth()));
            //loadText(heightField, new Float(lwc.getHeight()).toString());

            locationField.setText("x: " + lwc.getX() + "   y: " + lwc.getY());
            sizeField.setText(lwc.getWidth() + "x" + lwc.getHeight());
            //Font f = lwc.getFont();
            //if (lwc.getScale() != 1)
            //  fontString += " (" + (f.getSize()*lwc.getScale()) + ")";
            fontField.setText(lwc.getXMLfont());
            //fontField.setText(f.getName() + "-" + fontSize);
            //sizeField.setText(lwc.getWidth() + "x" + lwc.getHeight());

            colorField.setText(lwc.getXMLfillColor());
        }
        
    }

    public void actionPerformed(ActionEvent e)
    {
        //if (this.component == null)
        if (this.lwc == null)
            return;
        String text = e.getActionCommand();
        Object src = (JTextComponent) e.getSource();
        //System.out.println("Inspector " + e);
        if (src == labelField)
            lwc.setLabel(text);
        else if (src == categoryField)
            lwc.setCategory(text);
        else if (src == notesField)
            lwc.setNotes(text);
        else if (src == resourceField)
            lwc.setResource(text);
        else if (src == fontField)
            lwc.setXMLfont(text);
        else if (src == colorField)
            lwc.setXMLfillColor(text);
        else
            return;
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


}
