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
/**
 *
 * @author  Jay Briedis
 */
public class PathwayTable extends JTable{
        
    
    private final ImageIcon close = new ImageIcon("ZoomIn16.gif");
    private final ImageIcon open = new ImageIcon("ZoomOut16.gif");
    private final ImageIcon notes = new ImageIcon("Zoom16.gif");
    
    JCheckBox box = null;
    JTextField field = null;
    
    private final Color notSelected = Color.white;
    private final Color selected = Color.orange;
    
    private final MatteBorder border = new MatteBorder(3,0,3,0,notSelected);
    private final MatteBorder selectedBorder = new MatteBorder(3,0,3,0, selected);
    private final MatteBorder leftBorder = new MatteBorder(3,3,3,0,notSelected);
    private final MatteBorder leftSelectedBorder = new MatteBorder(3,3,3,0,selected);    
    private final MatteBorder rightBorder = new MatteBorder(3,0,3,3,notSelected);
    private final MatteBorder rightSelectedBorder = new MatteBorder(3,0,3,3,selected);  
    
    //sets whether or not table column headers are shown
    boolean showHeaders = true;
    
    public PathwayTable(PathwayTab ta){
        final PathwayTab tab = ta;
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        this.setIntercellSpacing(new Dimension(0,0));
        
        if(showHeaders){
        this.getTableHeader().setVisible(false);
        this.getTableHeader().setPreferredSize(new Dimension(this.getTableHeader().getPreferredSize().width, 1));
        this.getTableHeader().setIgnoreRepaint(true);
         }
        
        box = new JCheckBox();
        field = new JTextField();
        
        this.setDefaultRenderer(Boolean.class, new CheckRenderer(tab));
        this.setDefaultRenderer(Color.class, new ColorRenderer(tab));
        this.setDefaultRenderer(ImageIcon.class, new ImageRenderer(tab));
        this.setDefaultRenderer(String.class, new LabelRenderer(tab));
        //this.setDefaultEditor(Boolean.class, new CheckEditor(box, tab));
        //this.setDefaultEditor(String.class, new LabelEditor(field, tab));
        
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            
            public void valueChanged(ListSelectionEvent le){
                    
                ListSelectionModel lsm = (ListSelectionModel)le.getSource();
                if (!lsm.isSelectionEmpty()){
                    int row = lsm.getMinSelectionIndex();
                    int col = getSelectedColumn();
                    
                    PathwayTableModel tableModel = tab.getPathwayTableModel();
                    if(tableModel != null){
                        Object elem = tableModel.getElement(row);

                        if(elem instanceof LWPathway){
                            LWPathway path = (LWPathway)elem;
                            boolean same = tableModel.getCurrentPathway().equals(path);
                            tableModel.setCurrentPathway(path);
                            tab.removeElement.setEnabled(false);

                            if(col == 2){
                                if(same)
                                    tableModel.setCurrentOpen();
                                else
                                    tableModel.setCurrentOpen(true);
                                tab.updateControlPanel();
                            }
                            tableModel.fireTableDataChanged();
                        }else
                            tab.removeElement.setEnabled(true);
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
    
    private class ColorRenderer extends DefaultTableCellRenderer{
        private PathwayTab tab = null;
        
        public ColorRenderer(PathwayTab tab){
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
            
            
            Object path = tab.getPathwayTableModel().getElement(row);
            if(path instanceof LWPathway){
                this.setBackground((Color)value);
            }else{
                this.setBackground(Color.white);
                
            }
            //if(tab.getPathwayTable().getSelectedRow() == row)
                //this.setBorder(border);
            //else
                //this.setBorder(selectedBorder);
            return this;
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
            Object path = tab.getPathwayTableModel().getElement(row);
            
            //if(tab.getPathwayTable().getSelectedRow() == row)
                //this.setBorder(selectedBorder);
            //else
                //this.setBorder(border);
            
            return this;
        }  
    }
    
    private class ImageRenderer extends DefaultTableCellRenderer{
        
        private PathwayTab tab = null;
        
        public ImageRenderer(PathwayTab tab){
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
            Object path = tab.getPathwayTableModel().getElement(row);
            if(path instanceof LWPathway){
                //System.out.println("in image renderer color: "+value.toString());
                if(col == 2){
                    if(((Boolean)value).booleanValue())
                        this.setIcon(open);
                    else
                        this.setIcon(close);
                }
                else if(col == 4){
                    if(((Boolean)value).booleanValue())
                        this.setIcon(notes);
                    else
                        this.setIcon(null);
                }  
            }else{
                if(col == 2){
                    if(((Boolean)value).booleanValue())
                        this.setIcon(notes);
                    else
                        this.setIcon(null);
                }else{
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
    
    private class CheckRenderer extends JCheckBox implements TableCellRenderer{
        
        PathwayTab tab = null;
        
        public CheckRenderer(PathwayTab tab){
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
            //System.out.println("in check renderer value: "+value);
            Object path = tab.getPathwayTableModel().getElement(row);
            
            //if(tab.getPathwayTable().getSelectedRow() == row)
             //   this.setBorder(leftSelectedBorder);
            //else
            //    this.setBorder(leftBorder);
            
            if(path instanceof LWPathway){
                this.setSelected(((Boolean)value).booleanValue());
                return this;
            }
            return new JLabel();
        }     
    }
}
    /*
    private class CheckEditor extends DefaultCellEditor{
        private PathwayTab tab = null;
        
        public CheckEditor(JCheckBox box, PathwayTab tab){
            super(box);
            this.tab = tab;
            System.out.println("setup checkbox contructor...");
        }    
    }
    
    private class LabelEditor extends DefaultCellEditor{
        //private PathwayTab tab = null;
        
        public LabelEditor(JTextField field, PathwayTab tab){
            super(field);
            //this.tab = tab;
        }
    }*/
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