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
/**
 *
 * @author Jay Briedis
 */
/**A private class which defines how the combo box should be rendered*/
public class PathwayRenderer extends BasicComboBoxRenderer{
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
