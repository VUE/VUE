 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.gui.GUI;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
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
 * @version $Revision: 1.60 $ / $Date: 2007-02-21 00:24:48 $ / $Author: sfraize $
 */

public class PathwayTable extends JTable
{
    private final ImageIcon closeIcon;
    private final ImageIcon openIcon;
    private final ImageIcon notesIcon;
    private final ImageIcon lockIcon;
    private final ImageIcon eyeOpen;
    private final ImageIcon eyeClosed;
    
    // default of "SansSerif" on mac appears be same as default system font: "Lucida Grande"

    private final Font PathwayFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font EntryFont = new Font("SansSerif", Font.PLAIN, 10);
    private final Font SelectedEntryFont = new Font("SansSerif", Font.BOLD, 10);
    
    private final Color BGColor = new Color(241, 243, 246);;
    private final Color SelectedBGColor = Color.white;
    private final Color CurrentNodeColor = Color.red;
    
    private final LineBorder DefaultBorder = null;//new LineBorder(regular, 2);
    
    private int lastSelectedRow = -1;
    private LWPathway.Entry lastSelectedEntry;

    private static final boolean showHeaders = true; // sets whether or not table column headers are shown
    private final int[] colWidths = {20,20,13,100,20,20,20};

    private static Color selectedColor;

    public PathwayTable(PathwayTableModel model) {
        super(model);

        selectedColor = GUI.getTextHighlightColor();

        this.closeIcon = VueResources.getImageIcon("pathwayClose");
        this.openIcon = VueResources.getImageIcon("pathwayOpen");
        this.notesIcon = VueResources.getImageIcon("notes");
        this.lockIcon = VueResources.getImageIcon("lock");
        this.eyeOpen = VueResources.getImageIcon("pathwayOn");
        this.eyeClosed = VueResources.getImageIcon("pathwayOff");
    
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        this.setShowVerticalLines(false);
        this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        this.setIntercellSpacing(new Dimension(0,1));
        this.setBackground(BGColor);
        //this.setSelectionBackground(SelectedBGColor);
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
        this.setDefaultRenderer(Boolean.class, new BooleanRenderer());
        this.setDefaultEditor(Color.class, new ColorEditor());
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        for (int i = 0; i < colWidths.length; i++){
            TableColumn col = getColumn(PathwayTableModel.ColumnNames[i]);
            if (i == PathwayTableModel.COL_OPEN)
                col.setMinWidth(colWidths[i]);
            if (i != PathwayTableModel.COL_LABEL)
                col.setMaxWidth(colWidths[i]);
        }

        this.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent le) {
                    // this usually happens via mouse click, but also possible via arrow key's moving selected item
                    // Note that dragging the mouse over an image icon sends us continuous value change events,
                    // so ignore events where the model says the value is adjusting, so we only change on the
                    // final event.  This does have an odd side effect tho: if you click down over one image
                    // icon, then release over another, only the released over icon get's the change request.

                    if (DEBUG.PATHWAY) {
                        System.out.println("PathwayTable: valueChanged: " + le);
                        if (DEBUG.META) new Throwable("PATHWAYVALUECHANGED").printStackTrace();
                    }

                    ListSelectionModel lsm = (ListSelectionModel) le.getSource();
                    if (lsm.isSelectionEmpty() || le.getValueIsAdjusting())
                        return;
                
                    PathwayTableModel tableModel = getTableModel();
                    int row = lsm.getMinSelectionIndex();
                    
                    lastSelectedRow = row;
                    int col = getSelectedColumn();
                    if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: selected row "+row+", col "+col);
                    
                    //PathwayTable.this.tableSelectionUnderway = true;
                    
                    final LWPathway.Entry entry = tableModel.getEntry(row);
                    if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: object at row: " + entry);

                    lastSelectedEntry = entry;
                    tableModel.setCurrentPathway(entry.pathway);
                    
                    if (entry.isPathway()) {
                        if (col == PathwayTableModel.COL_VISIBLE ||
                            col == PathwayTableModel.COL_OPEN ||
                            col == PathwayTableModel.COL_LOCKED)
                        {
                            // setValue forces a value toggle in these cases
                            setValueAt(entry.pathway, row, col);
                        }
                        //pathway.setCurrentIndex(-1);
                    } else {
                        entry.pathway.setCurrentEntry(entry);
                    }

                    //PathwayTable.this.tableSelectionUnderway = false;
                    VUE.getUndoManager().mark();
                }
                });


        addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (DEBUG.PATHWAY || DEBUG.KEYS) System.out.println(this + " " + e);
                    final LWPathway pathway = VUE.getActivePathway();
                    if (pathway == null)
                        return;
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_UP) {
                        if (pathway.atFirst())
                            pathway.setIndex(-1);
                        else
                            pathway.setPrevious();
                        e.consume();
                    } else if (key == KeyEvent.VK_DOWN) {
                        pathway.setNext();
                        e.consume();
                    }
                }
            });
        // end of PathwayTable constructor
    }


    /*
    LWPathway.Entry getLastSelectedEntry() {
        return lastSelectedEntry;
    }

    int getLastSelectedRow() {
        return lastSelectedRow;
    }
    */

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
            //button.setBorder(new LineBorder(BGColor, 3));
        }

        public void actionPerformed(ActionEvent e) {
            if (VUE.getActivePathway().isLocked())
                return;
            Color c = VueUtil.runColorChooser("Pathway Color Selection", currentColor, VUE.getDialogParent());
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
            setBorder(new LineBorder(BGColor, 3)); // fyi: empty border no good: won't paint over
            setToolTipText("Select Color");
        }
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            if (entry == null) {
                return null;
            } else if (entry.isPathway()) {
                setBackground((Color) color);
                return this;
            } else
                return null;
        }  
    }
    private class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        public BooleanRenderer() {
            setFocusable(false);
            setToolTipText("Set as the Revealer");
        }
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            if (entry == null)
                return null;
            
            if (entry.isPathway()) {
                if (entry.pathway == VUE.getActivePathway())
                    setBackground(selectedColor);
                else
                    setBackground(BGColor);
                setSelected(getTableModel().getPathwayList().getRevealer() == entry.pathway);
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
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            if (entry == null)
                return null;
            
            setBorder(DefaultBorder);

            String debug = "";

            if (DEBUG.PATHWAY) debug = "(row"+row+")";
            
            if (entry.isPathway()){
                final LWPathway p = entry.pathway;
                if (p == VUE.getActivePathway())
                    setBackground(selectedColor);
                else
                    setBackground(BGColor);
                setFont(PathwayFont);
                setForeground(Color.black);
                setText(debug+"  " + entry.getLabel());
                
            } else {
                setBackground(BGColor);

                final LWPathway activePathway = VUE.getActivePathway();
                
                if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) {
                    // This is the current item on the current path
                    setFont(SelectedEntryFont);
                    setForeground(CurrentNodeColor);
                    setText(debug+"    "+entry.getLabel());
                    //setText(debug+"  * "+getText());
                } else {
                    setFont(EntryFont);
                    setText(debug+"    "+entry.getLabel());
                    if (entry.node.isFiltered() || entry.node.isHidden())
                        setForeground(Color.lightGray);
                    else
                        setForeground(Color.black);
                }
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
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            if (entry == null)
                return null;
            
            this.setBorder(DefaultBorder);
            this.setBackground(BGColor);
            //this.setHorizontalAlignment(SwingConstants.CENTER); // too far to right for open/close icon(why??)
            
            if (entry.isPathway()) {
                boolean bool = false;
                if (obj instanceof Boolean)
                    bool = ((Boolean)obj).booleanValue();
                
                if (col == PathwayTableModel.COL_VISIBLE) {
                    setIcon(bool ? eyeOpen : eyeClosed);
                    setBorder(iconBorder);
                    setToolTipText("Show/hide pathway");
                }
                else if (col == PathwayTableModel.COL_OPEN) {
                    setIcon(bool ? openIcon : closeIcon);
                    if (bool)
                        setToolTipText("Collapse member list");
                    else
                        setToolTipText("Expand member list");
                }
                else if (col == PathwayTableModel.COL_NOTES) {
                    if (entry.node == VUE.getActivePathway())
                        setBackground(selectedColor);
                    else
                        setBackground(BGColor);
                }
                else if (col == PathwayTableModel.COL_LOCKED) {
                    setIcon(bool ? lockIcon : null);
                    if (entry.node == VUE.getActivePathway())
                        setBackground(selectedColor);
                    else
                        setBackground(BGColor);
                    setToolTipText("Is locked");
                }
            }

            // This applies to both regular entries as well as pathway entries:
            if (col == PathwayTableModel.COL_NOTES) {
                if (entry.hasNotes()) {
                    setIcon(notesIcon);
                    setToolTipText(entry.getNotes());
                } else {
                    setToolTipText(null);
                    setIcon(null);
                }
            } else if (!entry.isPathway())
                return null;

            return this;
            
        }  
    }

    public String toString()
    {
        return "PathwayTable[" + VUE.getActivePathway() + "]";
    }
    
}
    
