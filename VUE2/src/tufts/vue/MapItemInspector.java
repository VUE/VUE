package tufts.vue;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JPanel;

/*
 * Temporary hack implementation placeholder
 * Need to do something nice here.
 */

class MapItemInspector extends javax.swing.JPanel
    implements MapSelectionListener
{
    MapItem mapItem;

    JTextField idField = new JTextField(20);
    JTextField labelField = new JTextField(20);
    JTextField categoryField = new JTextField(20);
    JTextField resourceField = new JTextField(20);
    JTextArea notesField = new JTextArea(1, 20);

    JLabel resourceLabel = new JLabel("Resource");
    
    JPanel labelPane = new JPanel();
    JPanel fieldPane = new JPanel();

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 9);
    
    public MapItemInspector()
    {
        labelPane.setLayout(new GridLayout(0, 1));
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
    public void eventRaised(MapSelectionEvent e)
    {
        setItem(e.getMapItem());
    }

    public void setItem(MapItem mapItem)
    {
        //System.err.println("inspector: " + mapItem);
        this.mapItem = mapItem;
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
