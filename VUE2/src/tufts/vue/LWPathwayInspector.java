/*
 * LWPathwayInspector.java
 *
 * Created on June 23, 2003, 3:54 PM
 */

package tufts.vue;

import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.text.Document;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.Vector;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayInspector extends InspectorWindow 
{
    
    /**Pane holds three tabs: general info, nodes in path, pathway notes*/
    private static JTabbedPane pane = null;
    
    /**'info' holds the pathway's general info*/
    private JTable info = null, pathwayTable = null;
    
    /**current pathway as indicated by the manager*/
    private LWPathway pathway = null; //new LWPathway(1);
    
    /**third tab which holds the notes for the current pathway*/
    public JTextArea text = null;
    
    /** buttons to navigate between pathway nodes*/
    private JButton moveUp, moveDown, remove, submit;
    
    /**standard border for all tabs*/
    private LineBorder border = new LineBorder(Color.black);
    
    /**handles opening and closing inspector*/
    private AbstractButton aButton = null;
    
    public InfoTableModel model = null;
    private Notes notes = null;
    private PathwayTab path = null;
    
    public LWPathwayInspector(JFrame owner, LWPathway pathway){
        this(owner);
        this.pathway = pathway;
        path.setPathway(pathway);
    }
 
    public LWPathwayInspector(JFrame owner) {
        super(owner, "");
        
        /**three components to be added to the tabbed pane*/
        InfoTable table = new InfoTable();
        notes = new Notes();
        path = new PathwayTab();
        
        if(pathway != null)
            this.setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
        else
            this.setTitle("PATHWAY INSPECTOR");
        
        /**instantiating and setting up tabbed pane*/
        pane = new JTabbedPane();
        pane.addTab("General Info", null, new JScrollPane(table), "Info Panel");
        pane.addTab("Node Info", null, path, "Path Panel");
        pane.addTab("Notes", null, new JScrollPane(notes), "Notes Panel");
        
        /**adding pane and setting location of this stand alone window*/
        this.getContentPane().add(pane);
        this.setSize(350, 300);
        
        /**unselects checkbox in VUE window menu*/
        super.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {setButton(false);}});
    }
    
    class DisplayAction extends AbstractAction
    {
        public DisplayAction(String label)
        {
            super(label);
        }
        public void actionPerformed(ActionEvent e)
        {
            aButton = (AbstractButton) e.getSource();
            setVisible(aButton.isSelected());
        }
    }
    
    public void setButton(boolean state){
        aButton.setSelected(state);
    }
    
    public void setPathway(LWPathway pathway){
        this.pathway = pathway;
        
        /*
        if(pathway != null && path != null){ 
            path.setPathway(pathway);
            System.out.println("set pathway to: " + pathway);
            setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
            pane.removeTabAt(2);
            pane.removeTabAt(1);
            path = null;
            path = new PathwayTab();
            path.setPathway(pathway);
            notes = null;
            notes = getNotes();
            pane.addTab("Node Info", null, path, "Path Panel");
            pane.addTab("Notes", null, new JScrollPane(notes), "Notes Panel");
        
            model.fireTableDataChanged();
            repaint();
        } 
        */
        
        path.setPathway(pathway);
        notes.setNotes(pathway);
        
        model.fireTableDataChanged();
    }
    
    public LWPathway getPathway(){
        return this.pathway;
    }
    
    public void notifyPathwayTab()
    {
        path.updateTable();
    }
    
    Action displayAction = null;
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Pathway Inspector");
        return displayAction;
    }
   
    private class Notes extends JPanel
    {
        private JTextArea area = null;
        private LWPathway notesPathway = null;
        
        public Notes ()
        {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(315, 200));
            setBorder(border);    
            
            area = new JTextArea();
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.addKeyListener(new KeyAdapter()
                {
                    public void keyTyped(KeyEvent e)
                    {
                        notesPathway.setComment(area.getText());
                    }
                });
                
            JLabel north = new JLabel("Pathway Notes", JLabel.CENTER);
        
            add(north, BorderLayout.NORTH);
            add(area, BorderLayout.CENTER);    
        }
        
        public void setNotes(LWPathway givenPathway)
        {
            notesPathway = givenPathway;
            
            //if the pathway is null disable the notes panel
            if (notesPathway == null)
            {
                area.setEnabled(false);
                area.setText(null);
            }
            
            else
            {
                area.setEnabled(true);
                area.setText(notesPathway.getComment());
            }     
        }
    }
    
    /**class to create a new general info table*/
    class InfoTable extends JTable{
           public InfoTable(){
                
               /**sets up model to handle changes in pathway data*/
               model = new InfoTableModel();
               this.setModel(model);
               model.addTableModelListener(this);
               
               this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setCellSelectionEnabled(true);
               this.setBorder(border);
               /**creates a color chooser when pathway color is selected*/
               if(pathway != null){
                   this.addMouseListener(new MouseAdapter(){

                       public void mouseClicked(MouseEvent e){
                            int row = getSelectedRow();
                            int col = getSelectedColumn();
                            if(row==3 && col==1){
                                JColorChooser choose = new JColorChooser();
                                Color newColor = choose.showDialog((Component)null, 
                                    "Choose Pathway Color", 
                                    (Color)model.getValueAt(row, col));
                                if(newColor != null)
                                    model.setValueAt(newColor, row, col);
                            }
                       }
                    });
               }
               
               TableColumn col = this.getColumn(this.getColumnName(1));
               InfoRenderer rend = new InfoRenderer();
               col.setCellRenderer(rend);
           }
    }
    
    /**renderers each of the cells according to location*/
    class InfoRenderer implements TableCellRenderer {       
        public Component getTableCellRendererComponent(
            JTable table, Object val,
            boolean isSelected, boolean focus,
            int row, int col){  
            if(row <= 2){
                JTextField field = new JTextField((String)val);
                field.setBorder(null);
                return field;
            }    
            else if(row == 3){
                JPanel panel = new JPanel();
                panel.setBackground((Color)val);
                return panel;
            }
            return null;
        }
    }
    
    /**model for info table to handle data from pathway instance*/
    class InfoTableModel extends AbstractTableModel 
    {
        int columns = 2;
        final String[] rowNames = {"Title", "Length", "Weight", "Color"};
       
        public int getRowCount() { return rowNames.length; }
        public int getColumnCount() {return columns;}

        public Object getValueAt(int row, int col) {
            
            if(col == 0) return rowNames[row];
            else if(col == 1 && pathway != null){
                if(row == 0){
                    return pathway.getLabel();
                }
                else if(row == 1){
                    return Integer.toString(pathway.length());
                }
                else if(row == 2){
                    return Integer.toString(pathway.getWeight());
                }
                else if(row == 3){
                    return pathway.getBorderColor();
                }
            }
            return null;
        }

        public boolean isCellEditable(int row, int col){
            if(pathway == null) return false;
            if(col==0 || row==3 || row==1) return false;
            return true;
        }

        public void setValueAt(Object value, int row, int col) {
            if(col == 1){
                if(row == 0){
                    pathway.setLabel((String)value);
                    setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
                } 
                //can't set the length
                else if(row == 2){
                    pathway.setWeight(Integer.parseInt((String)value));
                }
                else if(row == 3){
                    pathway.setBorderColor((Color)value);
                }
            }
        }

        public String getColumnName(int i){
            if(i == 0) return "Property";
            else if (i == 1) return "Value";
            else return null;
        }
    }
    
    /**main method used for testing purposes*/
    /*public static void main(String args[])
    {
        JFrame frame = new JFrame("ToolBar test");
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});
        
        frame.getContentPane().setLayout(new BorderLayout());
        LWPathwayInspector inspect = new LWPathwayInspector(frame);
        frame.show();
        
    }*/
}
