/*
 * LWPathwayInspector.java
 *
 * Created on June 23, 2003, 3:54 PM
 */

package tufts.vue;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.BorderLayout;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayInspector extends InspectorWindow{
    
    private static JTabbedPane pane = null;
    private JTable info = null;
    private LWPathwayManager manager = null;
    private LWPathway pathway = null;
    public JTextArea area = null;
    
    public LWPathwayInspector(JFrame owner, String title) {
        super(owner, "");
        
        pathway = new LWPathway(1);
        this.setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
        InfoTable table = new InfoTable();
        JPanel panelTwo = new JPanel();
        JPanel notes = getNotes();
        pane = new JTabbedPane();
        
        pane.addTab("General Info", null, new JScrollPane(table), "Info Panel");
        pane.addTab("Node Info", null, panelTwo, "Path Panel");
        pane.addTab("Notes", null, new JScrollPane(notes), "Notes Panel");
        this.getContentPane().add(pane);
        this.pack();
        this.setSize(350, 200);
        
        //System.out.println("table bounds: " + table.getBounds());
        //System.out.println("pane pref size: " + pane.getPreferredSize());
        this.show();
        
    }
    
    public static void main(String args[])
    {
        JFrame frame = new JFrame("ToolBar test");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});
        //frame.setSize(50, 50);
        frame.getContentPane().setLayout(new BorderLayout());
        LWPathwayInspector inspect = new LWPathwayInspector(frame, "Pathway Inspector");
        JButton button = new JButton("Button");
        //button.addActionListener(inspect);
        frame.getContentPane().add(button);
        frame.show();
        
    }
    
    private JPanel getNotes(){
        area = new JTextArea();
        area.setText(pathway.getComment());
        System.out.println("comment: "+ pathway.getComment());
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.addKeyListener(new KeyAdapter(){
           public void keyTyped(KeyEvent e){
               pathway.setComment(area.getText());
           }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel north = new JLabel("Pathway Notes", JLabel.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(area, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(315, 200));
        return panel;
    }
    
    class InfoTable extends JTable implements TableModelListener{
           InfoTableModel model = null;
           public InfoTable(){
               //this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setCellSelectionEnabled(true);
               model = new InfoTableModel();
               this.setModel(model);
               model.addTableModelListener(this);
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
           
           public void out(int x, int y){
               System.out.println("the component at event e: "+ this.getComponentAt(x, y));
               
           }
           
           public void TableChanged(TableModelEvent e){
                System.out.println("table changed: " + e);
                int row = e.getFirstRow();
                int col = e.getColumn();
                String colName = model.getColumnName(col);
                Object data = model.getValueAt(row, col);
                System.out.println("new data: " + data);
           }
    }
    
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
                        } //can't set the length
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
