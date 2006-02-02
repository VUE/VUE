package tufts.vue;

public class IconRenderer
extends javax.swing.table.DefaultTableCellRenderer
{

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table
                                                          , Object value
                                                          , boolean isSelected
                                                          , boolean hasFocus
                                                          , int row
                                                          , int column)
    {
        super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if (value instanceof javax.swing.ImageIcon)
        {
            setOpaque(true);
            setIcon((javax.swing.ImageIcon)value);
            setText("");
        }
        else
        {
            setIcon(null);
        }
        return this;
    }
 }
