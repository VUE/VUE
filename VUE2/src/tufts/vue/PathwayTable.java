package tufts.vue;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * PathwayTable.java
 *
 * A JTable that displays all of the pathways that exists in a given map,
 * and provides user interaction with the list of pathways.  Relies
 * on PathwayTableModel to produce a view of all the pathways that allows
 * for "opening" and "closing" the pathway -- displaying or hiding the
 * pathway elements in the JTable.
 *
 * @see PathwayTableModel
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Jay Briedis
 * @author  Scott Fraize
 * @version February 2004
 */

public class PathwayTable extends JTable
{
    private final ImageIcon close;
    private final ImageIcon open;
    private final ImageIcon notes;
    private final ImageIcon lock;
    private final ImageIcon eyeOpen;
    private final ImageIcon eyeClosed;
    
    private final Font currentFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 10);
    private final Color bgColor = new Color(241, 243, 246);;
    //private final Color bgColor = Color.red;
    private final Color selectedbgColor = Color.white;
    private final Color currentNodeColor = Color.red;
    
    private final LineBorder normalBorder = null;//new LineBorder(regular, 2);
    
    private int lastSelectedRow = -1;
    private LWComponent lastSelectedComponent;
    private boolean inTableSelection;

    private final boolean showHeaders = true; // sets whether or not table column headers are shown
    private final int[] colWidths = {20,20,13,100,20,20};

    private final Color selectedColor = VueTheme.getTheme().getTextHighlightColor();
    
    public PathwayTable(PathwayTableModel model) {
        super(model);

        this.close = VueResources.getImageIcon("pathwayClose");
        this.open = VueResources.getImageIcon("pathwayOpen");
        this.notes = VueResources.getImageIcon("notes");
        this.lock = VueResources.getImageIcon("lock");
        this.eyeOpen = VueResources.getImageIcon("pathwayOn");
        this.eyeClosed = VueResources.getImageIcon("pathwayOff");
    
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        this.setShowVerticalLines(false);
        this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        this.setIntercellSpacing(new Dimension(0,1));
        this.setBackground(bgColor);
        //this.setSelectionBackground(selectedbgColor);
        this.setDragEnabled(false);
        
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        
        if (showHeaders) {
            this.getTableHeader().setVisible(false);
            this.getTableHeader().setPreferredSize(new Dimension(this.getTableHeader().getPreferredSize().width, 1));
            this.getTableHeader().setIgnoreRepaint(true);
         }
        
        this.setDefaultRenderer(Color.class, new ColorRenderer());
        this.setDefaultRenderer(ImageIcon.class, new ImageRenderer());
        this.setDefaultRenderer(Object.class, new LabelRenderer());
        this.setDefaultEditor(Color.class, new ColorEditor());
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final String[] colNames = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < colWidths.length; i++){
            TableColumn col = getColumn(colNames[i]);
            if (i == 2) col.setMinWidth(colWidths[i]);
            if (i != 3) col.setMaxWidth(colWidths[i]);
        }

        this.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent le) {
                    // this usually happens via mouse click, but also possible via arrow key's moving selected item
                    ListSelectionModel lsm = (ListSelectionModel)le.getSource();
                    if (lsm.isSelectionEmpty())
                        return;
                
                    PathwayTableModel tableModel = getTableModel();
                    int row = lsm.getMinSelectionIndex();
                    
                    lastSelectedRow = row;
                    int col = getSelectedColumn();
                    if (DEBUG.PATHWAY) System.out.println("PathwayTable: selected row "+row+", col "+col);    
                    
                    PathwayTable.this.inTableSelection = true;
                    
                    LWComponent c = tableModel.getElement(row);
                    lastSelectedComponent = c;
                    LWPathway pathway = null;
                    if (c instanceof LWPathway)
                        pathway = (LWPathway) c;
                    else
                        pathway = tableModel.getPathwayForElementAt(row);
                    tableModel.setCurrentPathway(pathway);
                    
                    if (c instanceof LWPathway) {
                        if (col == 0 || col == 2 || col == 5)
                            setValueAt(pathway, row, col); // toggle pathway bits: visible, open or locked
                        //pathway.setCurrentIndex(-1);
                    } else {
                        pathway.setCurrentIndex(tableModel.getPathwayIndexForElementAt(row));
                    }

                    PathwayTable.this.inTableSelection = false;

                    VUE.getUndoManager().mark();
                }
                });


        addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e);
                }
            });
        

        /*
        model.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e + " (FYI)");
                }
            });
        */
        
        // end of PathwayTable constructor
    }

    boolean inTableSelection() {
        return inTableSelection;
    }

    int getLastSelectedRow() {
        return lastSelectedRow;
    }

    LWComponent getLastSelectedComponent() {
        return lastSelectedComponent;
    }

    private PathwayTableModel getTableModel() {
        return (PathwayTableModel) getModel();
    }

    private class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener
    {
        Color currentColor;
        JButton button;

        public ColorEditor() {
            button = new ColorRenderer();
            button.addActionListener(this);
            button.setBorder(null);
            //button.setBorder(new LineBorder(bgColor, 3));
        }

        public void actionPerformed(ActionEvent e) {
            if (VUE.getActivePathway().isLocked())
                return;
            Color c = VueUtil.runColorChooser("Pathway Color Selection", currentColor);
            fireEditingStopped();
            if (c != null) {
                // why the row checking here?
                int row = getSelectedRow();
                if (row == -1)
                    row = lastSelectedRow;
                if (row != -1)
                    getTableModel().setValueAt(currentColor = c, row, 1);
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
    
    private class ColorRenderer extends JButton implements TableCellRenderer {
        public ColorRenderer() {
            setOpaque(true);
            setBorder(new LineBorder(bgColor, 3)); // fyi: empty border no good: won't paint over
        }
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
            if (getTableModel().getElement(row) instanceof LWPathway) {
                setBackground((Color) color);
                return this;
            } else
                return null;
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
            LWComponent c = getTableModel().getElement(row);

            if (c.isFiltered())
                setForeground(Color.lightGray);
            else
                setForeground(Color.black);

            this.setBorder(normalBorder);
            
            if (c instanceof LWPathway){
                LWPathway p = (LWPathway) c;
                if (p == VUE.getActivePathway())
                    this.setBackground(selectedColor);
                else
                    this.setBackground(bgColor);
                this.setFont(currentFont);
                this.setText("  " + p.getDisplayLabel());
                
            } else if (c != null) {
                setFont(normalFont);
                setBackground(bgColor);
                setText(c.getDisplayLabel());

                LWPathway activePath = VUE.getActivePathway();
                LWPathway elementPath = getTableModel().getPathwayForElementAt(row);
                int elementIndexInPath = getTableModel().getPathwayIndexForElementAt(row);
                
                if (elementPath == activePath && elementPath.getCurrentIndex() == elementIndexInPath) {
                    // This is the current item on the current path
                    this.setForeground(currentNodeColor);
                    this.setText("  * "+this.getText());
                } else
                    this.setText("    "+this.getText());
            }
            return this;
        }  
    }
 
    private Border iconBorder = new EmptyBorder(0,3,0,0);
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
            //this.setHorizontalAlignment(SwingConstants.CENTER); // too far to right for open/close icon(why??)
            
            if (c instanceof LWPathway) {
                LWPathway p = (LWPathway) c;
                boolean bool = false;
                if (obj instanceof Boolean)
                    bool = ((Boolean)obj).booleanValue();
                
                if (col == 0) {
                    setIcon(bool ? eyeOpen : eyeClosed);
                    setBorder(iconBorder);
                }
                else if (col == 2) {
                    setIcon(bool ? open : close);
                }
                else if (col == 4) {
                    setIcon(bool ? notes : null);
                    if (p == VUE.getActivePathway())
                        setBackground(selectedColor);
                    else
                        setBackground(bgColor);
                }
                else if (col == 5) {
                    setIcon(bool ? lock : null);
                    if (p == VUE.getActivePathway())
                        setBackground(selectedColor);
                    else
                        setBackground(bgColor);
                }
            } else {
                if (col == 4 && getTableModel().getPathwayForElementAt(row).getElementNotes(c) != null)
                    setIcon(notes);
                else
                    return null;
            }
            return this;
            
        }  
    }

    public String toString()
    {
        return "PathwayTable[" + VUE.getActivePathway() + "]";
    }
    
}
    
