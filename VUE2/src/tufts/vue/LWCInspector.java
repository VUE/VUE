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
               MapItemListener,
               ActionListener
{
    java.util.List selectionList;

    JLabel idField = new JLabel();
    JLabel fontField = new JLabel();
    JLabel locationField = new JLabel();
    JLabel sizeField = new JLabel();
    JTextField labelField = new JTextField(15);
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
            "-Font",    fontField,
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
    

    // todo: will need to change this to display
    // a LWComponent...
    
    public void mapItemChanged(MapItemEvent e)
    {
        MapItem mi = e.getSource();
        if (this.mapItem != mi)
            throw new IllegalStateException("unexpected event " + e);
        loadItem(mi, this.lwc);
    }
    public void LWCChanged(LWCEvent e)
    {
        if (e.getSource() == this)
            return;
        if (this.lwc != e.getComponent())
            throw new IllegalStateException("unexpected update event");
        loadItem(this.mapItem, this.lwc);
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

    private MapItem mapItem; // temporary
    private LWComponent lwc; // temporary
    public void setSelection(java.util.List sl)
    {
        this.selectionList = sl;
        //System.err.println("Inspector setSelection: " + sl);

        if (selectionList.size() > 1)
            return;

        LWComponent lwc = (LWComponent) sl.get(0);
        MapItem mapItem = lwc.getMapItem();
        loadItem(mapItem, lwc);
    }

    private void loadItem(MapItem mapItem, LWComponent lwc)
    {
        // handling both a MapItem and a LWC here
        // is a temporary hack until we change
        // elimate seperate concept map objects and
        // implement as an interface.
        if (this.mapItem != mapItem) {
            if (this.mapItem != null)
                this.mapItem.removeChangeListener(this);
            this.mapItem = mapItem;
            this.mapItem.addChangeListener(this);
        }
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            this.lwc.addLWCListener(this);
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
            //loadText(widthField, new Float(lwc.getWidth()));
            //loadText(heightField, new Float(lwc.getHeight()).toString());

            locationField.setText("x: " + lwc.getX() + "   y: " + lwc.getY());
            sizeField.setText(lwc.getWidth() + "x" + lwc.getHeight());
            Font f = lwc.getFont();
            fontField.setText(f.getName() + "-" + f.getSize());
            //sizeField.setText(mapItem.getWidth() + "x" + mapItem.getHeight());
        }
        
    }

        public void actionPerformed(ActionEvent e)
    {
        //if (this.component == null)
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


}
