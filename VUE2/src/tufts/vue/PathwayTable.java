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
 * @author  Jay Briedis
 * @author  Scott Fraize
 * @version February 2004
 */
public class PathwayTable extends JTable
{
    private final ImageIcon close = VueResources.getImageIcon("pathwayClose");
    private final ImageIcon open = VueResources.getImageIcon("pathwayOpen");
    private final ImageIcon notes = VueResources.getImageIcon("notes");
    private final ImageIcon lock = VueResources.getImageIcon("lock");
    private final ImageIcon eyeOpen = VueResources.getImageIcon("pathwayOn");
    private final ImageIcon eyeClosed = VueResources.getImageIcon("pathwayOff");
    
    private static final Font currentFont = new Font("SansSerif", Font.BOLD, 12);
    private static final Font normalFont = new Font("SansSerif", Font.PLAIN, 10);
    private static final Color bgColor = new Color(241, 243, 246);;
    private static final Color selectedbgColor = Color.white;
    private static final Color currentNodeColor = Color.red;
    
    private static final LineBorder normalBorder = null;//new LineBorder(regular, 2);
    private static final LineBorder selectedBorder = null;//new LineBorder(selected, 2);
    

    private JCheckBox box = null;
    private JTextField field = null;
    private int lastSelectedRow = -1;
    private boolean showHeaders = true;    //sets whether or not table column headers are shown
    
    public PathwayTable(PathwayPanel ta, PathwayTableModel model) {
        super(model);

        model.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e + " (FYI)");
                }
            });

        final PathwayPanel pathPanel = ta;

        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        this.setShowVerticalLines(false);
        this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        this.setBackground(bgColor);
        //this.setSelectionBackground(selectedbgColor);
        //this.setDragEnabled(true);
        
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        
        if (showHeaders) {
            this.getTableHeader().setVisible(false);
            this.getTableHeader().setPreferredSize(new Dimension(this.getTableHeader().getPreferredSize().width, 1));
            this.getTableHeader().setIgnoreRepaint(true);
         }
        
        box = new JCheckBox();
        field = new JTextField();
        
        this.setDefaultRenderer(Color.class, new ColorRenderer());
        this.setDefaultRenderer(ImageIcon.class, new ImageRenderer());
        this.setDefaultRenderer(Object.class, new LabelRenderer());
        this.setDefaultEditor(Color.class, new ColorEditor());
        
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            
            public void valueChanged(ListSelectionEvent le){
                ListSelectionModel lsm = (ListSelectionModel)le.getSource();
                if (!lsm.isSelectionEmpty()){
                    int row = lsm.getMinSelectionIndex();
                    lastSelectedRow = row;
                    int col = getSelectedColumn();
                    if (DEBUG.PATHWAY) System.out.println("PathwayTable: clicked row "+row+", col "+col);    
                    
                    //PathwayTableModel tableModel = pathPanel.getTableModel();
                    // could this have even been null??
                    PathwayTableModel tableModel = getTableModel();
                    if (tableModel != null){
                        LWComponent c = tableModel.getElement(row);
                        LWPathway p = null;
                        if (c instanceof LWPathway)
                            p = (LWPathway) c;
                        else
                            p = tableModel.getPathwayForElementAt(row);
                        tableModel.setCurrentPathway(p);

                        if (c instanceof LWPathway) {
                            //pathPanel.setAddElementEnabled();
                            //pathPanel.removeElement.setEnabled(false);
                            
                            if (col == 0 || col == 2)
                                setValueAt(p, row, col); // what's this do??
                            
                            tableModel.fireChanged(this);
                            //tableModel.fireTableDataChanged(new TableModelEvent(this));
                            //pathPanel.updateControlPanel();
                            
                        } else {
                            p.setCurrentElement(c);
                            //pathPanel.removeElement.setEnabled(true);
                            //tableModel.fireChanged(this);
                            //tableModel.fireTableDataChanged(new TableModelEvent(this)); // new
                            //pathPanel.updateControlPanel();
                        }
                    }
                    else {
                        // is this reachable code?
                        pathPanel.removeElement.setEnabled(false);
                        new Throwable("tableModel is null").printStackTrace();
                    }
                    
                    
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
                        
                        //notesText = comp.getNotes();
                        notesText = path.getElementNotes(comp);
                    }
                    
                    
                    pathPanel.updateLabels(newText, notesText, path, comp);
                    pathPanel.repaint();
                }
            }
        });
    }
    
    private PathwayTableModel getTableModel()
    {
        return (PathwayTableModel) getModel();
    }
    
    public void setCurrentElement(int row){
        new Throwable("setCurrentElement").printStackTrace();
        //pathPanel.setCurrentElement(row);
    }
    
    private class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener {
        Color currentColor;
        JButton button;
        JColorChooser colorChooser;
        JDialog dialog;
        protected static final String EDIT = "edit";

        public ColorEditor() {
            button = new ColorRenderer();
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
            if (!VUE.getActivePathway().isLocked())
            {
                if (EDIT.equals(e.getActionCommand())) {
                    colorChooser.setColor(currentColor);
                    dialog.setVisible(true);
                    fireEditingStopped();
                } else { 
                    currentColor = colorChooser.getColor();
                    if(currentColor != null){
                        int row = getSelectedRow();
                        if(row == -1)
                            row = lastSelectedRow;
                        if(row != -1){
                            getTableModel().setValueAt(currentColor, row, 1);
                            getTableModel().fireChanged(this);
                            //getTableModel().fireTableDataChanged(new TableModelEvent(this));
                            VUE.getActiveViewer().repaint();//todo: handle via event
                        }
                    }               
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
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;
        
        public ColorRenderer(){
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
            this.setBackground(bgColor);
            
            Object path = getTableModel().getElement(row);
            if(path instanceof LWPathway){
                this.setBackground(newColor);
                //if(VUE.getPathwayInspector().getCurrentPathway().equals((LWPathway)path))
                //if(((LWPathway)path).isOpen())
                //    this.setBorder(selectedBorder);
            }else{
                this.setBackground(bgColor);
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
    
    private class LabelRenderer extends DefaultTableCellRenderer {
        
        public java.awt.Component getTableCellRendererComponent(
                                    javax.swing.JTable jTable, 
                                    Object value, 
                                    boolean isSelected, 
                                    boolean hasFocus, 
                                    int row, 
                                    int col)
        {
            this.setForeground(Color.black);
            this.setBorder(normalBorder);
            
            LWComponent c = getTableModel().getElement(row);
            
            if (c instanceof LWPathway){
                LWPathway p = (LWPathway) c;
                if (p == VUE.getActivePathway())
                    this.setBackground(Color.yellow);
                else
                    this.setBackground(bgColor);
                this.setFont(currentFont);
                this.setText(p.getLabel());
                
            } else if (c != null) {
                this.setFont(normalFont);
                this.setBackground(bgColor);
                this.setText(c.getLabel());

                LWPathway p = getTableModel().getPathwayForElementAt(row);
                LWPathway curPath = getTableModel().getCurrentPathway();
                //System.out.println("LabelRenderer current path: " + curPath);
                if (curPath != null && curPath == p && p.getCurrent() == c) {
                    // This is the current item on the current path
                    this.setForeground(currentNodeColor);
                    this.setText("* "+this.getText());
                } else
                    this.setText("  "+this.getText());
            }
            return this;
        }  
    }
 
    private class ImageRenderer extends DefaultTableCellRenderer {
        
        public java.awt.Component getTableCellRendererComponent(
                                    javax.swing.JTable jTable, 
                                    Object obj, 
                                    boolean isSelected, 
                                    boolean hasFocus, 
                                    int row, 
                                    int col)
        {
            LWComponent c = getTableModel().getElement(row);
            this.setBorder(normalBorder);
            this.setBackground(bgColor);
            
            if (c instanceof LWPathway) {
                LWPathway p = (LWPathway) c;
                
                if (p.isOpen())
                    setBorder(selectedBorder);
                
                if (col == 0) {
                    if (((Boolean)obj).booleanValue())
                        setIcon(eyeOpen);
                    else
                        setIcon(eyeClosed);
                }
                else if (col == 2) {
                    if(((Boolean)obj).booleanValue())
                        setIcon(open);
                    else
                        setIcon(close);
                }
                else if (col == 4) {
                    if (p == VUE.getActivePathway())
                        setBackground(Color.yellow);
                    else
                        setBackground(bgColor);
                    if (((Boolean)obj).booleanValue())
                        setIcon(notes);
                    else
                        setIcon(null);
                }
                else if (col == 5) {
                    if (p == VUE.getActivePathway())
                        setBackground(Color.yellow);
                    else
                        setBackground(bgColor);
                      
                    if (((Boolean)obj).booleanValue())
                        setIcon(lock);
                    else
                        setIcon(null);
                }
            } else {
                setBorder(selectedBorder);
                switch (col) {
                case 0:
                case 2:
                case 5:
                    setIcon(null);
                    break;
                case 4:
                    if (getTableModel().getPathwayForElementAt(row).getElementNotes(c) != null)
                        setIcon(notes);
                    else
                        setIcon(null);
                    break;
                }
            }
            return this;
            
        }  
    }

    public String toString()
    {
        return "PathwayTable[" + VUE.getActivePathway() + "]";
    }
    
}
    
