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

    JTextField idField = new JTextField();
    JTextField labelField = new JTextField(15);
    JTextField categoryField = new JTextField();
    JTextField resourceField = new JTextField();
    JTextField notesField = new JTextField();
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

        String[] labels = { "ID", "Label", "Category", "Resource", "Notes" };
        JTextComponent[] textFields = {idField, labelField, categoryField, resourceField, notesField};
        //idField.setBorder(LineBorder.createBlackLineBorder());
        idField.setBorder(new EmptyBorder(1,1,1,1));
        idField.setEditable(false);
        if (VueUtil.isMacPlatform())
            idField.setBackground(SystemColor.window);
        if (!(notesField instanceof JTextField))
            notesField.setBorder(LineBorder.createGrayLineBorder());
        
        addLabelTextRows(labels, textFields, gridBag, fieldPane);

        setLayout(new BorderLayout());
        add(fieldPane, BorderLayout.CENTER);

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

    }

    private void addLabelTextRows(String[] labels,
                                  JTextComponent[] textFields,
                                  GridBagLayout gridbag,
                                  Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default

            JLabel label = new JLabel(labels[i] + ": ");
            label.setFont(SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;

            JTextComponent field = textFields[i];
            field.setFont(SmallFont);
            if (field instanceof JTextField)
                ((JTextField)field).addActionListener(this);
            gridbag.setConstraints(field, c);
            container.add(field);
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
                    resourceField.setText(node.getResource().toString());
                else
                    resourceField.setText("");
                resourceField.setEditable(true);
                //resourceLabel.setVisible(true);
                //resourceField.setVisible(true);
            } else {
                resourceField.setText("");
                resourceField.setEditable(false);
                //resourceLabel.setVisible(false);
                //resourceField.setVisible(false);
            }
            idField.setText(mapItem.getID());
            labelField.setText(mapItem.getLabel());
            categoryField.setText(mapItem.getCategory());
            notesField.setText(mapItem.getNotes());
        }
        
    }
}
