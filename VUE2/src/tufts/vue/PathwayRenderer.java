/*
 * PathwayRenderer.java
 *
 * Created on December 3, 2003, 1:18 PM
 */

package tufts.vue;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
/**
 *
 * @author Jay Briedis
 */ 

public class PathwayRenderer extends DefaultTableCellRenderer{
        
    private final Font selectedFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 12);

    public PathwayRenderer(){
        
    }

    public Component getTableCellRendererComponent(JTable table,
                                           Object value,
                                           boolean isSelected,
                                           boolean hasFocus,
                                           int row,
                                           int col)
    {
        System.out.println("value class: "+value.getClass());
        System.out.println("this class:  "+this.getClass());
        if(col == 0){
            JCheckBox box = new JCheckBox();
            return box;
        }
        else if(col == 1){
            JLabel lab = new JLabel();
            lab.setBackground((Color)value);
            return lab;
        }
        else if(col == 2){
            JLabel la = new JLabel();
            la.setBackground(Color.blue);
            return la;
        }
        else if(col == 3){
            JTextField field = new JTextField((String)value);
            return field;
        }
        else if(col == 4){
            JLabel label = new JLabel();
            label.setBackground(Color.black);
            return label;
        }
        return new JLabel("aaa");
    }
}

/**A private class which defines how the combo box should be rendered*/
/*public class PathwayRenderer extends BasicComboBoxRenderer{
    public PathwayRenderer ()
    {
        super();
    }

    //a method which defines how to render cells
    public Component getListCellRendererComponent(JList list,
           Object value, int index, boolean isSelected, boolean cellHasFocus) 
    {
        //if the cell contains a pathway, then displays its label
        if (value instanceof LWPathway)
        {    
            LWPathway pathway = (LWPathway)value;
            if(pathway.getLabel().length() > 15)
                setText(pathway.getLabel().substring(0, 13) + "...");
            else
                setText(pathway.getLabel());
        }

        //if it is a string, then displays the string itself
        else
            setText((String)value);

        //setting the color to the default setting
        if (isSelected)
        {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }

        else
        {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }   
}
*/