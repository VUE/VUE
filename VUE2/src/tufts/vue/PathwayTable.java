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
            
    
    private final Color regular = Color.lightGray;
    private final Color selected = Color.white;
    
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
        
        this.setDefaultRenderer(Color.class, new ColorRenderer(tab));
        this.setDefaultRenderer(ImageIcon.class, new ImageRenderer(tab));
        this.setDefaultRenderer(Object.class, new LabelRenderer(tab));
        
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
                            tableModel.setCurrentPathway(path);
                            
                            tab.removeElement.setEnabled(false);
                            
                            if(col == 0 || col == 2){
                                setValueAt(path, row, col);
                            }
                            else if(col == 1){
                                Color selColor = JColorChooser.showDialog(tab, 
                                                                          "Pathway Color Selection",
                                                                          Color.BLUE);
                                if(selColor != null)
                                    setValueAt(selColor, row, col);
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
            this.setBorder(normalBorder);
            Object path = tab.getPathwayTableModel().getElement(row);
            if(path instanceof LWPathway){
                this.setBackground((Color)value);
                //if(VUE.getPathwayInspector().getCurrentPathway().equals((LWPathway)path))
                if(((LWPathway)path).getOpen())
                    this.setBorder(selectedBorder);
            }else{
                this.setBackground(Color.white);
                this.setBorder(selectedBorder);
            }
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
            Object obj = tab.getPathwayTableModel().getElement(row);
            this.setForeground(Color.black);
            this.setFont(normalFont);
            this.setBorder(normalBorder);
            
            /*
            if((path instanceof LWPathway && ((LWPathway)path).getOpen())
                || path instanceof LWComponent){
                    this.setBorder(selectedBorder);
            }*/
            
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