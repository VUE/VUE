package tufts.vue;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.border.*;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JPanel;

/*
 * Temporary hack implementation placeholder
 * Need to do something nice here.
 */

class MapItemInspector extends javax.swing.JPanel
    implements MapSelectionListener, MapItemChangeListener
{
    MapItem mapItem;

    JTextField idField = new JTextField(15);
    JTextField labelField = new JTextField(15);
    JTextField categoryField = new JTextField(15);
    JTextField resourceField = new JTextField(15);
    JTextArea notesField = new JTextArea(1, 20);

    JLabel resourceLabel = new JLabel("Resource");
    
    JPanel labelPane = new JPanel();
    JPanel fieldPane = new JPanel();

    private static final Font defaultFont = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 9);
    
    public MapItemInspector()
    {
        // todo: report a preferred size
        setFont(defaultFont); // todo: make this effective
        labelPane.setLayout(new GridLayout(0, 1));
        labelPane.setFont(defaultFont);
        labelPane.add(new JLabel("ID"));
        labelPane.add(new JLabel("Label"));
        labelPane.add(new JLabel("Category"));
        labelPane.add(resourceLabel).setVisible(false);
        labelPane.add(new JLabel("Notes"));
        
        //fieldPane.setLayout(new GridLayout(0, 1));
        fieldPane.setLayout(new BoxLayout(fieldPane, BoxLayout.Y_AXIS));
        fieldPane.add(idField);
        idField.setEditable(false);
        fieldPane.add(labelField);
        fieldPane.add(categoryField);
        resourceField.setFont(smallFont);
        fieldPane.add(resourceField).setVisible(false);
        notesField.setFont(smallFont);
        notesField.setBorder(new BevelBorder(BevelBorder.LOWERED));
        fieldPane.add(notesField);

        setBorder(new TitledBorder("Item Inspector"));
        setLayout(new BorderLayout());
        add(labelPane, BorderLayout.CENTER);
        add(fieldPane, BorderLayout.EAST);

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
                resourceLabel.setVisible(true);
                resourceField.setVisible(true);
            } else {
                resourceLabel.setVisible(false);
                resourceField.setVisible(false);
            }
            idField.setText(mapItem.getID());
            labelField.setText(mapItem.getLabel());
            categoryField.setText(mapItem.getCategory());
            notesField.setText(mapItem.getNotes());
        }
        
    }
}
