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
import javax.swing.JColorChooser;
import javax.swing.ListSelectionModel;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.BorderLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayInspector extends InspectorWindow{
    
    /**Pane holds three tabs: general info, nodes in path, pathway notes*/
    private static JTabbedPane pane = null;
    
    /**'info' holds the pathway's general info*/
    private JTable info = null;
    
    /**receives updates from manager as to which pathway is current*/
    private LWPathwayManager manager = null;
    
    /**current pathway as indicated by the manager*/
    private LWPathway pathway = new LWPathway(1);
    
    /**third tab which holds the notes for the current pathway*/
    public JTextArea area = null;
    
    public LWPathwayInspector(JFrame owner, String title) {
        super(owner, "");
        this.setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
        
        /**three components to be added to the tabbed pane*/
        InfoTable table = new InfoTable();
        JPanel panelTwo = new JPanel();
        JPanel notes = getNotes();
        
        /**instantiating and setting up tabbed pane*/
        pane = new JTabbedPane();
        pane.addTab("General Info", null, new JScrollPane(table), "Info Panel");
        pane.addTab("Node Info", null, panelTwo, "Path Panel");
        pane.addTab("Notes", null, new JScrollPane(notes), "Notes Panel");
        
        /**adding pane and setting location of this stand alone window*/
        this.getContentPane().add(pane);
        this.setSize(350, 200);
        this.show();     
    }
    
    /**main method used for testing purposes*/
    public static void main(String args[])
    {
        JFrame frame = new JFrame("ToolBar test");
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});
        
        frame.getContentPane().setLayout(new BorderLayout());
        LWPathwayInspector inspect = new LWPathwayInspector(frame, "Pathway Inspector");
        frame.show();
        
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
               
               /**creates a color chooser when pathway color is selected*/
               this.addMouseListener(new MouseAdapter(){
                   
                   public void mouseClicked(MouseEvent e){
                        int row = getSelectedRow();
                        int col = getSelectedColumn();
                        System.out.println("selected row, col: "+row+col);
                        if(row==3 && col==1){
                            
                            JColorChooser choose = new JColorChooser();
                            Color newColor = choose.showDialog((Component)null, 
                                "Choose Pathway Color", 
                                (Color)model.getValueAt(row, col));
                            System.out.println(newColor);
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
    class InfoRenderer extends JTextField implements TableCellRenderer {
        public InfoRenderer(){
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setBorder(null);
          
        }
        
        public Component getTableCellRendererComponent(
            JTable table, Object val,
            boolean isSelected, boolean focus,
            int row, int col){  
            this.setBackground(Color.white);
            this.setText("");
            if(row <= 2)
                this.setText((String)val);
            else if(row == 3)
                this.setBackground((Color)val);
            return this;
        }
    }
    
    /**model for info table to handle data from pathway instance*/
    class InfoTableModel extends AbstractTableModel {
                int columns = 2;
                final String[] rowNames = {"Title", "Length", "Weight", "Color"};
                public InfoTableModel(){
                }
                
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
}
