package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;

/*
 * rename ItemInspector
 */

class MapItemInspector extends javax.swing.JPanel
    implements VueConstants,
               MapSelectionListener,
               MapItemChangeListener,
               ActionListener
{
    MapItem mapItem;

    JLabel idField = new JLabel();
    JTextField labelField = new JTextField(15);
    JTextField categoryField = new JTextField();
    JTextField resourceField = new JTextField();
    JTextField notesField = new JTextField();
    JLabel locationField = new JLabel();
    JLabel sizeField = new JLabel();
    
    //JTextArea notesField = new JTextArea(1, 20);

    JPanel fieldPane = new JPanel();

    public MapItemInspector()
    {
        setBorder(new TitledBorder("Item Inspector"));
        
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
            //"-Size",    sizeField,
            "Label",    labelField,
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

    public void actionPerformed(ActionEvent e)
    {
        if (this.mapItem == null)
            return;
        String text = e.getActionCommand();
        Object src = (JTextComponent) e.getSource();
        //System.out.println("Inspector " + e);
        if (src == labelField)
            mapItem.setLabel(text);
        else if (src == categoryField)
            mapItem.setCategory(text);
        else if (src == notesField)
            mapItem.setNotes(text);
        else if (src == resourceField)
            mapItem.setResource(text);
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
            label.setFont(SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;

            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setFont(SmallFont);
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
    

    public void mapItemSelected(MapItem mapItem)
    {
        setItem(mapItem);
    }

    public void mapItemChanged(MapItemChangeEvent e)
    {
        MapItem mi = e.getSource();
        if (this.mapItem != mi)
            throw new IllegalStateException("unexpected event " + e);
        setItem(mi);
    }
    
    public void eventRaised(MapSelectionEvent e)
    {
        setItem(e.getMapItem());
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

    public void setItem(MapItem mapItem)
    {
        //System.err.println("inspector: " + mapItem);
        if (this.mapItem != mapItem) {
            if (this.mapItem != null)
                this.mapItem.removeChangeListener(this);
            this.mapItem = mapItem;
            this.mapItem.addChangeListener(this);
        }

        if (mapItem != null) {
            if (mapItem instanceof Node) {
                Node node = (Node) mapItem;
                if (node.getResource() != null)
                    loadText(resourceField, node.getResource().toString());
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

            //loadText(idField, mapItem.getID());
            idField.setText(mapItem.getID());
            loadText(labelField, mapItem.getLabel());
            loadText(categoryField, mapItem.getCategory());
            loadText(notesField, mapItem.getNotes());

            locationField.setText("x=" + mapItem.getX() + " y=" + mapItem.getY());
            //sizeField.setText(mapItem.getWidth() + "x" + mapItem.getHeight());
        }
        
    }
}
