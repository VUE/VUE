/*
 * PathwayTable.java
 *
 * Created on December 3, 2003, 1:11 PM
 */

package tufts.vue;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import javax.swing.event.*;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
/**
 *
 * @author  Jay Briedis
 */
public class PathwayTable extends JTable{
        
    
    private final ImageIcon close = VueResources.getImageIcon("pathwayClose");
    private final ImageIcon open = VueResources.getImageIcon("pathwayOpen");
    private final ImageIcon notes = VueResources.getImageIcon("notes");
    private final ImageIcon lock = VueResources.getImageIcon("lock");
    private final ImageIcon eyeOpen = VueResources.getImageIcon("pathwayOn");
    private final ImageIcon eyeClosed = VueResources.getImageIcon("pathwayOff");
    
    JCheckBox box = null;
    JTextField field = null;
    
    private final Font currentFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 10);
            
    
    private final Color bgColor = new Color(241, 243, 246);;
    private final Color selectedbgColor = Color.white;
    
    private final Color currentNodeColor = Color.red;
    
    private final LineBorder normalBorder = null;//new LineBorder(regular, 2);
    private final LineBorder selectedBorder = null;//new LineBorder(selected, 2);
    
    //sets whether or not table column headers are shown
    boolean showHeaders = true;
    
    public PathwayTable(PathwayTab ta){
        final PathwayTab tab = ta;
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        
        this.setShowVerticalLines(false);
        this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        
        this.setBackground(bgColor);
        //this.setSelectionBackground(selectedbgColor);
        
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        
        if(showHeaders){
        this.getTableHeader().setVisible(false);
        this.getTableHeader().setPreferredSize(new Dimension(this.getTableHeader().getPreferredSize().width, 1));
        this.getTableHeader().setIgnoreRepaint(true);
         }
        
        box = new JCheckBox();
        field = new JTextField();
        
        this.setDefaultRenderer(Color.class, new ColorRenderer(tab));
        this.setDefaultRenderer(ImageIcon.class, new ImageRenderer(tab));
        this.setDefaultRenderer(Object.class, new LabelRenderer(tab));
        
        this.setDefaultEditor(Color.class, new ColorEditor(tab));
        //this.setDefaultEditor(ImageIcon.class, new ImageEditor(tab));
        
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        this.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            
            public void valueChanged(ListSelectionEvent le){
                ListSelectionModel lsm = (ListSelectionModel)le.getSource();
                if (!lsm.isSelectionEmpty()){
                    int row = lsm.getMinSelectionIndex();
                    int col = getSelectedColumn();
                    System.out.println("clicked row, col: "+row+", "+col);    
                
                    PathwayTableModel tableModel = tab.getPathwayTableModel();
                    if(tableModel != null){
                        Object elem = tableModel.getElement(row);

                        if(elem instanceof LWPathway){
                            LWPathway path = (LWPathway)elem;
                            tableModel.setCurrentPathway(path);
                            
                            tab.removeElement.setEnabled(false);
                            
                            if(col == 0 || col == 2){
                                setValueAt(path, row, col);
                            }
                            tableModel.fireTableDataChanged();
                            tab.updateControlPanel();
                            
                        }else{
                            tab.removeElement.setEnabled(true);
                            LWPathway path = tab.getPathwayTableModel().getManager().getPathwayforElementAt(row);
                            tab.getPathwayTableModel().getManager().setCurrentPathway(path);
                            tab.updateControlPanel();
                        }
                    }
                    else 
                        tab.removeElement.setEnabled(false);
                    
                    
                    String newText = "Notes: ";
                    String notesText = "";
                    LWPathway path = null;
                    LWComponent comp = null;
                        
                    Object obj = tableModel.getElement(row);

                    if(obj instanceof LWPathway){
                        newText = newText + ((LWPathway)obj).getLabel();
                        notesText = ((LWPathway)obj).getNotes();
                        path = (LWPathway)obj;
                    }
                    else{
                        path = tableModel.getCurrentPathway();
                        comp = (LWComponent)tableModel.getElement(row);
                        newText = newText + path.getLabel() 
                            + " / " 
                            + comp.getLabel();
                        notesText = comp.getNotes();
                    }
                    
                    
                    tab.updateLabels(newText, notesText, path, comp);
                    tab.repaint();
                }
            }
        });
    }
    
    public void setCurrentElement(int row){
        //tab.setCurrentElement(row);
    }
    
    private class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener {
        PathwayTab tab = null;
        Color currentColor;
        JButton button;
        JColorChooser colorChooser;
        JDialog dialog;
        protected static final String EDIT = "edit";

        public ColorEditor(PathwayTab tab) {
            this.tab = tab;
            button = new ColorRenderer(tab);
            button.setActionCommand(EDIT);
            button.addActionListener(this);
            //button.setBorderPainted(false);
            button.setBorder(BorderFactory.createMatteBorder(3,3,3,3,
                                                  Color.white));
            colorChooser = new JColorChooser();
            dialog = JColorChooser.createDialog(button,
                                            "Pathway Color Selection",
                                            true,  
                                            colorChooser,
                                            this,  
                                            null);
        }

        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                
                colorChooser.setColor(currentColor);
                dialog.setVisible(true);
                fireEditingStopped();

            } else { 
                currentColor = colorChooser.getColor();
                if(currentColor != null){
                    int row = tab.getPathwayTable().getSelectedRow();
                    tab.getPathwayTable().setValueAt(currentColor, row, 1);
                    tab.getPathwayTableModel().fireTableDataChanged();
                }               
            }
        }

        public Object getCellEditorValue() {
            return currentColor;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentColor = (Color)value;
            button.setBackground(currentColor);
            return button;
        }
    }
    
    private class ColorRenderer extends JButton implements TableCellRenderer{
        private PathwayTab tab = null;
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;
        
        public ColorRenderer(PathwayTab tab){
            this.tab = tab;
            setOpaque(true);
        }
        
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
            Color newColor = (Color)color;
            //this.setBorder(normalBorder);
            selectedBorder = BorderFactory.createMatteBorder(3,3,3,3,
                                                  bgColor);
                    
            setBorder(selectedBorder);
            
            
            Object path = tab.getPathwayTableModel().getElement(row);
            if(path instanceof LWPathway){
                this.setBackground(newColor);
                //if(VUE.getPathwayInspector().getCurrentPathway().equals((LWPathway)path))
                //if(((LWPathway)path).getOpen())
                //    this.setBorder(selectedBorder);
            }else{
                this.setBackground(Color.white);
                Border compBorder = BorderFactory.createMatteBorder(3,3,3,3,
                                                  bgColor);
                    
                setBorder(compBorder);
                //this.setBorder(selectedBorder);
            }
            
            JPanel con = new JPanel(new BorderLayout(0,0));
            con.add(this, BorderLayout.CENTER);
            return con;
        }  
    }
    
    private class LabelRenderer extends DefaultTableCellRenderer{
        private PathwayTab tab = null;
        
        public LabelRenderer(PathwayTab tab){
            this.tab = tab;
            
        }
        
        public java.awt.Component getTableCellRendererComponent(
                                    javax.swing.JTable jTable, 
                                    Object value, 
                                    boolean isSelected, 
                                    boolean hasFocus, 
                                    int row, 
                                    int col)
        {
            Object obj = tab.getPathwayTableModel().getElement(row);
            this.setForeground(Color.black);
            this.setFont(normalFont);
            this.setBorder(normalBorder);
            
            if(obj instanceof LWPathway){
                LWPathway path = (LWPathway)obj;
                //use p for border, font?
                this.setFont(currentFont);
                this.setText(path.getLabel());   
                
            }else if (obj instanceof LWComponent){
                LWPathway p = tab.getPathwayTableModel().getManager().getPathwayforElementAt(row);
                LWComponent c = (LWComponent)obj;
                this.setText(c.getLabel());
                
                //use p & c for border, font?
                
                if(tab.getPathwayTableModel().getCurrentPathway().equals(p)
                        && p.getCurrent().equals(c)){
                    this.setForeground(currentNodeColor);
                    this.setText("* "+this.getText());
                }else
                    this.setText("  "+this.getText());
            }
            return this;
        }  
    }
    
/*    private class LabelEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            MouseListener {
        PathwayTab tab = null;
        JLabel label = null;
        int row, col;
        
        
        public LabelEditor(PathwayTab tab) {
            this.tab = tab;
            label = new ImageRenderer(tab);
            label.addMouseListener(this);
            label.setBorder(normalBorder);
        }

        public Object getCellEditorValue() {
            return new Boolean(true);
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            
            return ((ImageRenderer)label).getTableCellRendererComponent(
                                    table, value, isSelected, true, 
                                    row, column);
        }
        
        public void mouseClicked(MouseEvent e) {
            row = tab.getPathwayTable().getSelectedRow();
            col = tab.getPathwayTable().getSelectedColumn();
            System.out.println("mouse clicked row, col: "+row+", "+col);
            if(col == 0 || col == 2){
                tab.getPathwayTable().setValueAt(new Boolean(true), row, col);
                tab.getPathwayTableModel().fireTableDataChanged();  
            }            
        }
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}        
    }
  */  
    private class ImageRenderer extends DefaultTableCellRenderer{
        
        private PathwayTab tab = null;
        
        public ImageRenderer(PathwayTab tab){
            this.tab = tab;
        }
        
        public java.awt.Component getTableCellRendererComponent(
                                    javax.swing.JTable jTable, 
                                    Object obj, 
                                    boolean isSelected, 
                                    boolean hasFocus, 
                                    int row, 
                                    int col)
        {
            Object path = tab.getPathwayTableModel().getElement(row);
            this.setBorder(normalBorder);
            if(path instanceof LWPathway){
                //if(VUE.getPathwayInspector().getCurrentPathway().equals((LWPathway)path))
                if(((LWPathway)path).getOpen())
                    this.setBorder(selectedBorder);
                
                if(col == 0) {
                    if(((Boolean)obj).booleanValue())
                        this.setIcon(eyeOpen);
                    else
                        this.setIcon(eyeClosed);
                }
                else if(col == 2){
                    if(((Boolean)obj).booleanValue())
                        this.setIcon(open);
                    else
                        this.setIcon(close);
                }
                else if(col == 4){
                    if(((Boolean)obj).booleanValue())
                        this.setIcon(notes);
                    else
                        this.setIcon(null);
                }
                
            }else{
                this.setBorder(selectedBorder);
                if(col == 0){
                    this.setIcon(null);
                }
                if(col == 2){
                    this.setIcon(null);
                }
                if(col == 4){
                    if( ((LWComponent)path).getNotes() != null && ((LWComponent)path).getNotes() != "")
                        this.setIcon(notes);
                    else
                        this.setIcon(null);
                }
            }
            if(tab.getPathwayTable().getSelectedRow() == row){
                //if(col == 2)
                    //this.setBorder(selectedBorder);
                //else
                    //this.setBorder(rightSelectedBorder);
            }else{
                //if(col == 2)
                //    this.setBorder(border);
                //else
                //    this.setBorder(rightBorder);
            }
            
            return this;
        }  
    }
}
    

/*
 LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selectedRow);
                        //text.setText(element.getNotes());

                        //if the selected row is the last row, then disables the move down button
                        //if(selectedRow == getRowCount() - 1)
                        //    tab.moveDown.setEnabled(false);
                        //else
                        //    tab.moveDown.setEnabled(true);

                        //if the selected row is the first row, then disables the move up button
                        //if(selectedRow == 0)
                        //    tab.moveUp.setEnabled(false);
                        //else
                        //    tab.moveUp.setEnabled(true);
                        //tab.moveDown.setEnabled(false);
                        //tab.moveUp.setEnabled(false);
*/