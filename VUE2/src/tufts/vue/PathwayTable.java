/*
 * PathwayTable.java
 *
 * Created on December 3, 2003, 1:11 PM
 */

package tufts.vue;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

/**
 *
 * @author  Jay Briedis
 */
public class PathwayTable extends JTable{
        
    PathwayTab tab = null;
    private final Font selectedFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 12);

    public PathwayTable(PathwayTab tab){
        //super(new PathwayTableModel(tab));
        this.tab = tab;
        this.setShowGrid(false);
        this.setShowHorizontalLines(true);
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = getSelectionModel();
        lsm.addListSelectionListener(tab);     
    }
    
    public void setCurrentElement(int row){
        //tab.setCurrentElement(row);
    }
    
    private class PathwayTableCellRenderer extends DefaultTableCellRenderer{
        
        public void PathwayTableCellRenderer(){}
        
        public Component getTableCellRendererComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               boolean hasFocus,
                                               int row,
                                               int column)
            {
                int num = row + 1;
                setBackground(Color.white);
                setText(num + ". " + (String)value);
                setForeground(Color.black);
                setFont(normalFont);
                if(isSelected){
                    setForeground(Color.blue);
                    setFont(selectedFont);
                    setText(" > " + getText());
                }else setText("    " + getText());

                return this;
            }
    }
}


    
        //setToolTipText("Double click to select the current elment");
        /*addMouseListener(
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent me)
                {
                    if (me.getClickCount() == 2)
                       setCurrentElement(getSelectedRow());
                }  
            }
        );*/