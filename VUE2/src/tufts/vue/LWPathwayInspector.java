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
    implements ActionListener, ListSelectionListener, DocumentListener{
    
    /**Pane holds three tabs: general info, nodes in path, pathway notes*/
    private static JTabbedPane pane = null;
    
    /**'info' holds the pathway's general info*/
    private JTable info = null, pathwayTable = null;
    
    /**receives updates from manager as to which pathway is current*/
    private LWPathwayManager manager = null;
    
    /**current pathway as indicated by the manager*/
    private LWPathway pathway = null; //new LWPathway(1);
    
    /**third tab which holds the notes for the current pathway*/
    public JTextArea area = null, text = null;
    
    /** buttons to navigate between pathway nodes*/
    private JButton moveUp, moveDown, remove, submit;
    
    /**standard border for all tabs*/
    private LineBorder border = new LineBorder(Color.black);
    
    /**handles opening and closing inspector*/
    private AbstractButton aButton = null;
    
    public LWPathwayInspector(JFrame owner) {
        super(owner, "");
        manager = LWPathwayManager.getInstance();
        pathway = manager.getCurrentPathway();
        this.setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
        
        /**three components to be added to the tabbed pane*/
        InfoTable table = new InfoTable();
        JPanel path = getPath();
        JPanel notes = getNotes();
        
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
    
    Action displayAction = null;
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Pathway Inspector");
        return displayAction;
    }
     
    /**gets the current notes saved in the pathway's class and creates a test area for them*/
    private JPanel getNotes(){
        
        area = new JTextArea();
        area.setText(pathway.getComment());
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.addKeyListener(new KeyAdapter(){
           public void keyTyped(KeyEvent e){
               pathway.setComment(area.getText());
           }
        });
        
        JLabel north = new JLabel("Pathway Notes", JLabel.CENTER);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(area, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(315, 200));
        panel.setBorder(border);        
        return panel;
    }
    
    /**class to create a new general info table*/
    class InfoTable extends JTable{
           InfoTableModel model = null;
           public InfoTable(){
                
               /**sets up model to handle changes in pathway data*/
               model = new InfoTableModel();
               this.setModel(model);
               model.addTableModelListener(this);
               
               this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setCellSelectionEnabled(true);
               this.setBorder(border);
               /**creates a color chooser when pathway color is selected*/
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
            else if(col == 1){
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
    
    private JPanel getPath(){
        pathwayTable = new JTable(new PathwayTableModel());
        pathwayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = pathwayTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        JPanel pathPanel = new JPanel();
        
        pathPanel.setLayout(new BorderLayout());
        pathPanel.setBorder(border);
       
        JScrollPane scrollPane = new JScrollPane(pathwayTable);
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        buttons.setPreferredSize(new Dimension (80, 60));
        
        moveUp = new JButton("Up");
        moveDown = new JButton("Down");
        remove = new JButton("Remove");
        submit = new JButton("Submit");
        
        text = new JTextArea();
        text.setEditable(true);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setPreferredSize(new Dimension(100, 50));
        
        Document document = text.getDocument();
        document.addDocumentListener(this);
        
        moveUp.addActionListener(this);
        moveDown.addActionListener(this);
        remove.addActionListener(this);
        submit.addActionListener(this);
        
        moveUp.setEnabled(false);
        moveDown.setEnabled(false);
        remove.setEnabled(false);
        submit.setEnabled(false);
        
        buttons.add(moveUp);
        buttons.add(moveDown);
        buttons.add(remove);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(text);
        textPanel.add(submit);
        
        pathPanel.add(scrollPane, BorderLayout.CENTER);
        pathPanel.add(buttons, BorderLayout.EAST);
        pathPanel.add(textPanel, BorderLayout.SOUTH);
        return pathPanel;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        //gets the selected row number
        int selected = pathwayTable.getSelectedRow();
        
        //moves up the selected row 
        if (e.getSource() == moveUp)
        {
            selected--;
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
        }
        
        //moves down the selected row
        else if (e.getSource() == moveDown)
        {
            selected++; 
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
        }
        
        //removes the selected row
        else if (e.getSource() == remove)
        {    
             if (selected != -1)
             {
                ((PathwayTableModel)pathwayTable.getModel()).deleteRow(selected);
                submit.setEnabled(false);
             }  
        }        
        
        //submit
        else
        {            
            Node node = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getNode(selected);
            node.setNotes(text.getText());
            submit.setEnabled(false);
        }
    }
    
    /**Reacts to list selections dispatched by the table*/
    public void valueChanged(ListSelectionEvent le)
    {
        ListSelectionModel lsm = (ListSelectionModel)le.getSource();

        //if there is a row selected
        if (!lsm.isSelectionEmpty())
        {
            int selectedRow = lsm.getMinSelectionIndex();
            remove.setEnabled(true);
            
            Node node = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getNode(selectedRow);
            text.setText(node.getNotes());
            
            //if the selected row is the last row, then disables the move down button
            if(selectedRow == pathwayTable.getRowCount() - 1)
                moveDown.setEnabled(false);
            else
                moveDown.setEnabled(true);
            
            //if the selected row is the first row, then disables the move up button
            if(selectedRow == 0)
                moveUp.setEnabled(false);
            else
                moveUp.setEnabled(true);
        }
        
        //if no row is selected, then disables all buttons
        else
        {
            moveDown.setEnabled(false);
            moveUp.setEnabled(false);
            remove.setEnabled(false);
            submit.setEnabled(false);
            text.setText("comment");
        }
    }
    
    public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        if(pathwayTable.getSelectedRow() != -1)
          submit.setEnabled(true);
    }
    
    public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        if(pathwayTable.getSelectedRow() != -1)
          submit.setEnabled(true);
    }
    
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
       if(pathwayTable.getSelectedRow() != -1)
         submit.setEnabled(true);
    }
    
    /**A model used by the table which displays nodes of the pathway*/
    private class PathwayTableModel extends AbstractTableModel
    {
        //pathway whose nodes are displayed in the table
        //private LWPathway pathway;
        
        //column names for the table
        private final String[] columnNames = {"Nodes/Links"};
         
        public PathwayTableModel()
        {
            //default pathway
            //pathway = new LWPathway();
        }
        
        //sets the pathway to the given pathway
        public synchronized void setPathway(LWPathway way)
        {
            pathway = way;
        }
        
        //returns the current pathway
        public synchronized LWPathway getPathway()
        {
            return pathway;
        }
        
        //returns the number of row (nodes of the pathway)
        public synchronized int getRowCount()
        {
            return pathway.length();
        }
        
        public int getColumnCount()
        {
            return columnNames.length;
        }
        
        public String getColumnName(int column)
        {
            return columnNames[column];
        }
        
        //a method which defines how what each cell should display
        public synchronized Object getValueAt(int row, int column)
        {
            try
            {
                switch(column)
                {                    
                    case 0:
                        Node rowNode = pathway.getNode(row);
                        return rowNode.getLabel();
                    //case 1:
                       // return new String("Comments about the node");
                }
            }
            
            catch (Exception e)
            {
                System.err.println("exception in the table model:" + e);
            }
            
            return "";
        }
        
        //adds a row to the table (insertion)
        public synchronized void addRow(Node node)
        {
            pathway.addNode(node);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
        }
        
        //deletes the given row from the table
        public synchronized void deleteRow(int row)
        {
            pathway.removeNode(pathway.getNode(row));
            fireTableRowsDeleted(row, row);  
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
