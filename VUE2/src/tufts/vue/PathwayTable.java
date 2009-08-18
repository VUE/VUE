 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.DeleteSlideDialog;
import tufts.vue.gui.GUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import osid.dr.Asset;

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
 * @version $Revision: 1.112 $ / $Date: 2009-08-18 14:29:39 $ / $Author: sfraize $
 */

public class PathwayTable extends JTable
    implements DropTargetListener,
               DragSourceListener,
               DragGestureListener,
               MouseListener,
               ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PathwayTable.class);
    
	private DropTarget dropTarget = null;
	private DragSource dragSource = null;
	private int dropIndex = -1;
	private int dropRow = -1;
    private final ImageIcon notesIcon;
    private final ImageIcon lockIcon;
    private final ImageIcon lockOpenIcon;
    private final ImageIcon mapViewIcon;
    private final ImageIcon slideViewIcon;
    private final ImageIcon eyeOpen;
    private final ImageIcon eyeClosed;
    
    final static char RightArrowChar = 0x25B8; // unicode
    final static char DownArrowChar = 0x25BE; // unicode
    
    // default of "SansSerif" on mac appears be same as default system font: "Lucida Grande"

    private final Font PathwayFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font EntryFont = new Font("SansSerif", Font.PLAIN, 10);
    private final Font SelectedEntryFont = new Font("SansSerif", Font.BOLD, 10);
    
    private final Color BGColor = Color.white;//new Color(241, 243, 246);;
    //private final Color SelectedBGColor = Color.white;
    //private final Color CurrentNodeColor = Color.red;
    
    private final LineBorder DefaultBorder = null;//new LineBorder(regular, 2);
    
    private int lastSelectedRow = -1;
   // private LWPathway.Entry lastSelectedEntry;

    private static final boolean showHeaders = true; // sets whether or not table column headers are shown
    private final int[] colWidths = {30,20,200,30,30,20};

    private static Color SelectedColor;

    private static PathwayTable Singleton;

    public void setUI(TableUI ui)
    {
    	super.setUI(ui);
    }
    public PathwayTable(PathwayTableModel model) {
        super(model);
        initComponents();
        SelectedColor = GUI.getTextHighlightColor();

        this.notesIcon = VueResources.getImageIcon("notes");
        this.lockIcon = VueResources.getImageIcon("lock");
        this.lockOpenIcon = VueResources.getImageIcon("lockOpen");
        this.eyeOpen = VueResources.getImageIcon("pathwayOn");
        this.eyeClosed = VueResources.getImageIcon("pathwayOff");
        this.mapViewIcon = VueResources.getImageIcon("mapView");
        this.slideViewIcon = VueResources.getImageIcon("slideView");
       // this.setDoubleBuffered(true);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        this.setShowVerticalLines(false);      
        this.setDoubleBuffered(true);
       this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        this.setIntercellSpacing(new Dimension(0,1));
        this.setBackground(BGColor);
        
       // this.setIgnoreRepaint(true);
        //this.setSelectionBackground(SelectedBGColor);
     //   this.setDragEnabled(true);
        
    //    ToolTipManager.sharedInstance().registerComponent(this);
        
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
      
        JTextField field = new PathwayNameField();
        LabelCellEditor labelEditor = new LabelCellEditor(field);
        ((PathwayNameField)field).setCellEditor(labelEditor);
        this.setDefaultEditor(Object.class, labelEditor);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        CellEditor e = this.getCellEditor(2,0);
        for (int i = 0; i < colWidths.length; i++){
            TableColumn col = getColumn(PathwayTableModel.ColumnNames[i]);
            if (i == PathwayTableModel.COL_OPEN)
                col.setMaxWidth(26);
            else if (i == PathwayTableModel.COL_LABEL)
                {//col.setMaxWidth(colWidths[i]);
                col.setMinWidth(160);
                col.setWidth(160);                
                }
            else if ( i == PathwayTableModel.COL_COLOR)
            {
            	col.setMaxWidth(25);
            }
            else
            {
            	col.setMaxWidth(colWidths[i]);
            }
        }

        VUE.addActiveListener(LWPathway.Entry.class, this);
        addMouseListener(this);
/*         getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                 public void valueChanged(ListSelectionEvent le) {
                     handleValueChanged(le);
                 }
             });
  */      

        addKeyListener(PathwayKeyHandler);
//         addKeyListener(new KeyAdapter() {
//                 public void keyPressed(KeyEvent e) {
//                     if (DEBUG.PATHWAY || DEBUG.KEYS) System.out.println(this + " " + e);
//                     final LWPathway pathway = VUE.getActivePathway();
//                     if (pathway == null)
//                         return;
//                     switch (e.getKeyCode()) {
//                     case KeyEvent.VK_UP:
//                     case KeyEvent.VK_LEFT:
//                         if (pathway.atFirst())
//                             pathway.setIndex(-1);
//                         else
//                             pathway.setPrevious();
//                         e.consume();
//                         break;
//                     case KeyEvent.VK_DOWN:
//                     case KeyEvent.VK_RIGHT:
//                         pathway.setNext();
//                         e.consume();
//                         break;
//                     }
//                 }
//             });

        Singleton = this;
    	renamePresentation.addActionListener(this);
    	playbackPresentation.addActionListener(this);
    	masterSlidePresentation.addActionListener(this);
    	deletePresentation.addActionListener(this);
    	editEntry.addActionListener(this);
    	previewEntry.addActionListener(this);
    	addNoteEntry.addActionListener(this);
    	deleteEntry.addActionListener(this);
    	duplicateEntry.addActionListener(this);
    	
        // end of PathwayTable constructor
    }

    public static final KeyListener PathwayKeyHandler =
        new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (DEBUG.PATHWAY || DEBUG.KEYS) System.out.println(this + " " + e);
                final LWPathway pathway = VUE.getActivePathway();
                if (pathway == null)
                    return;

                boolean handled = true;

                switch (e.getKeyCode()) {
                case KeyEvent.VK_PAGE_UP:
                    // page-up will not go to master-slide: presentation slides only
                    pathway.setPrevious();
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_LEFT:
                    if (pathway.atFirst()) {
                        // load master slide
                        pathway.setIndex(-1);
                    } else
                        pathway.setPrevious();
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_PAGE_DOWN:
                    pathway.setNext();
                    break;
                case KeyEvent.VK_HOME:
                    pathway.setFirst();
                    break;
                case KeyEvent.VK_END:
                    pathway.setLast();
                    break;
                case KeyEvent.VK_ENTER:
                    if (e.getSource() == PathwayTable.Singleton) {
                        if (VUE.getActiveViewer() != null)
                            VUE.getActiveViewer().popFocal();
                        break;
                    } else
                        handled = false;
                default:
                    handled = false;
                }

                if (handled)
                    e.consume();
            }
        };
    

    

    public void valueChanged(ListSelectionEvent e) {
        //System.out.println("JTABLE     VALUE     CHANGED " + e);
        super.valueChanged(e);
        handleValueChanged(e);
    }
    
//     public void columnSelectionChanged(ListSelectionEvent e) {
//         System.out.println("JTABLE COL SELECTION CHANGED " + e);
//         super.columnSelectionChanged(e);
//     }


    private void handleValueChanged(ListSelectionEvent le) {
        // this usually happens via mouse click, but also possible via arrow key's moving selected item
        // Note that dragging the mouse over an image icon sends us continuous value change events,
        // so ignore events where the model says the value is adjusting, so we only change on the
        // final event.  This does have an odd side effect tho: if you click down over one image
        // icon, then release over another, only the released over icon get's the change request.

        ListSelectionModel lsm = (ListSelectionModel) le.getSource();
        if (lsm.isSelectionEmpty() || le.getValueIsAdjusting())
            return;
                
        if (DEBUG.PATHWAY) {
            System.out.println("PathwayTable: VALUECHANGED:  " + le);
            if (DEBUG.META) new Throwable("PATHWAYVALUECHANGED").printStackTrace();
        }

        PathwayTableModel tableModel = getTableModel();
        int row = lsm.getMinSelectionIndex();
                    
        lastSelectedRow = row;
        int col = getSelectedColumn();
        if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: selected row "+row+", col "+col);
                    
                    
        final LWPathway.Entry entry = tableModel.getEntry(row);
        if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: object at row: " + entry);

        boolean selectedEntry = true;
                    
        // TODO: this isn't adequate for handling clicks on particular columns for, e.g., toggling values
        // there, as if the row is already selected, there's no guarantee we get a new selection event
        // when clicking (apparently if you click in the map-view column in another row, which doesn't
        // change the selectedion, and then back on the map view icon for the selected row, we
        // start getting selection events again, tho obviously we can't rely on that as a user action).
        // We can always catch the actual mouse events, tho JTable editors may also handle this for us (?)
        // -- SMF 2007-05-10

  /*      if (entry.isPathway()) {
            if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW ||
                col == PathwayTableModel.COL_OPEN ||
                col == PathwayTableModel.COL_LOCKED) {
                // setValue forces a value toggle in these cases
                setValueAt(entry.pathway, row, col);
                selectedEntry = false;
            }
            //pathway.setCurrentIndex(-1);
        } else if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW && entry.hasVariableDisplayMode()) {
            setValueAt(entry.pathway, row,col);
            selectedEntry = false;
        }
    */                
        if (selectedEntry) {
            entry.pathway.setCurrentEntry(entry);
            // The above will generate the below event, but only if it's the current pathway.
            // If this entry isn't on the current pathway, this will ensure the entry's
            // pathway becomes the new current pathway (and if the event won't actually fire
            // again if there's been no change).
            VUE.setActive(LWPathway.Entry.class, this, entry);
        } else {
            // in case the setValueAt's resulted in any permanent changes:
            VUE.getUndoManager().mark();
        }
    }
    

    public void activeChanged(ActiveEvent e, LWPathway.Entry entry) {
        if (entry != null) {
            int row = getTableModel().getRow(entry);
            if (row >= 0)
                changeSelection(row, -1, false, false);
        }
    }

    private static Color lastAlpha;
    private static Color lastMixed;
    private static Color selectedColor(LWPathway p) {

        //if (true) return SelectedColor;
        
        final Color alphaColor = p.getColor();
        // todo: LWPathway should cache this as a white / non-alpha color
        if (alphaColor != lastAlpha) {
            lastAlpha = alphaColor;
            lastMixed = Util.alphaMix(alphaColor, Color.white);
        }
        return lastMixed;
    }
    private static Color selectedColor(LWPathway.Entry e) {
        return selectedColor(e.pathway);
    }
    

    /*
    LWPathway.Entry getLastSelectedEntry() {
        return lastSelectedEntry;
    }

    int getLastSelectedRow() {
        return lastSelectedRow;
    }
    */

    private void initComponents() {
        dropTarget = new DropTarget(this, this);
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
    }
    
    private PathwayTableModel getTableModel() {
        return (PathwayTableModel) getModel();
    }

    private class LabelEditor extends AbstractCellEditor    	    	
    {    
    	public LabelEditor()
    	{
    		
    	}
    	//public LabelEditor(JTextField f){super(f);}	

		public Object getCellEditorValue() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    
    private class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			             MouseListener
    {
        Color currentColor;
        ColorRenderer button;
        int curRow =0;
        
        public ColorEditor() {
            button = new ColorRenderer();
            button.addMouseListener(this);
            button.setBorder(null);
        }

        public Object getCellEditorValue() {
            return currentColor;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
        	curRow=row;
            currentColor = (Color)value;
         //   button.setBackground(currentColor);
           // button.setForeground(currentColor);            
            Component c = button.getTableCellRendererComponent(table, value, isSelected,true, row, column);
            c.addMouseListener(this);
            return c;
        }

      
		public void mouseClicked(MouseEvent arg0) {
		
		}

		
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mousePressed(MouseEvent e) {
//			 TODO Auto-generated method stub
	        if (VUE.getActivePathway().isLocked())
                return;
	    //    System.out.println(e.getY());
	        if (e.getY() > 20)
	        	return;
	        
	        
            Color c = VueUtil.runColorChooser("Pathway Color Selection", currentColor, VUE.getDialogParent());
            fireEditingStopped();
            if (c != null) {
                    getTableModel().setValueAt(currentColor = c, curRow, 4);
            }		
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
    }
    
    private class ColorRenderer extends JPanel implements TableCellRenderer {
        private final Color
        TopGradient1 = new Color(179,166,121),
        BottomGradient1 = new Color(142,129,82);
        private final Color TopGradient2 = new Color(195,193,186);
        private final Color BottomGradient2 = new Color(162,161,156);
        private GradientPaint Gradient = null;
        private GradientPaint Gradient2 = null;
        private int curRow =0;
    	public ColorRenderer() {
            setOpaque(true);
            
        	//Gradient = new GradientPaint(0,           0, TopGradient1,
            //        0, 20, BottomGradient1);
        	//Gradient2 = new GradientPaint(0,           0, TopGradient2,
            //        0, 20, BottomGradient2);
        	
            setToolTipText("Select Color");
        }
        
    	//final RoundRectangle2D BlobShape = new RoundRectangle2D.Float();
    	Color paintColor = null;
    	
   /*     protected void paintComponent(Graphics g)
        {
        	Graphics2D g2 = (Graphics2D)g;
        	paintGradient(g2);        	
        	g2.setColor(paintColor);
            g2.fillRoundRect(2, 2, getWidth()-8, 20-4,7,7);
            g2.setColor(Color.gray);
            g2.drawRoundRect(2, 2, getWidth()-8, 20-4,7,7);
        }
        
        public void setPaintColor(Color c)
        {
        	paintColor = c;        	
        }
        private void paintGradient(Graphics2D g)
        {
        	Paint p = g.getPaint();

        	final LWPathway.Entry entry = getTableModel().getEntry(curRow);
        	final LWPathway activePathway = VUE.getActivePathway();
//        	final LWPathway.Entry entryReal = getTableModel().getEntry(PathwayTable.this.getSelectedRow());
        	//entry.
           // if (entry.pathway != null && entryReal.equals(entry.pathway))
        	
        	if (entry != null && entry.pathway != null && entry.node == activePathway) 
                g.setPaint(Gradient);
            else
                g.setPaint(Gradient2);
            
            g.fillRect(0, 0, getWidth(),20);
            g.setPaint(p);
            g.setColor(Color.white);             
            g.fillRect(0,20,getWidth(),40);
        }
        boolean useGoldGradient = true;
        */
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
        	  
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            
           // paintColor =  (Color)getTableModel().getValueAt(row, 4);
            curRow=row;
            paintColor = (Color)color;
            GradientLabel gl = new GradientLabel(entry.pathway,paintColor);
            
            if (entry == null) {
            	{
            	//	setBackground((Color) color);
                 //   setForeground((Color) color);
                return gl;
            	}
            } 
            else if (entry.isPathway()) {
                //setBackground((Color) color);
                //setForeground((Color) color);
               
                return gl;
            } 
            else
            {
            	JLabel p = new DefaultTableCellRenderer();
            	p.setOpaque(true);
            	final LWPathway activePathway = VUE.getActivePathway();
            	if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
        		{
            	//	useGoldGradient = true;
            		p.setBackground(selectedColor(entry));
            		p.setForeground(selectedColor(entry));
        		}
            	else
            	{
            	//	useGoldGradient=false;
            		p.setBackground(BGColor);
            		p.setForeground(BGColor);
            	}
                
            	return p;
            }
             //   return null;
        }  
    }
    
    private static Font ArrowIconFont = new Font("Lucida Sans Unicode", Font.PLAIN, 20);

    private class LabelRenderer extends DefaultTableCellRenderer{
        
    	
        public java.awt.Component getTableCellRendererComponent(
                                    javax.swing.JTable jTable, 
                                    Object value, 
                                    boolean isSelected, 
                                    boolean hasFocus, 
                                    int row, 
                                    int col)
        {
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            String debug = "";
            
            if (Util.isUnixPlatform())
            	ArrowIconFont = new Font("DejaVu Sans", Font.PLAIN, 20);
            
            if (entry == null)
                return this;
         
            if (DEBUG.PATHWAY) debug = "(row"+row+")";
         
          
            JLabel label = new JLabel();
            
            setMinimumSize(new Dimension(10, 20));
            setPreferredSize(new Dimension(500, 20));      
          
            if (entry.isPathway())
            {
            	String emptyString = null;
            	            	
            	  GradientLabel gl = new GradientLabel(entry.pathway);
            	  
            	  if (col == PathwayTableModel.COL_OPEN) 
            	  {
                      boolean bool = false;
                      if (value instanceof Boolean)
                          bool = ((Boolean)value).booleanValue();
                      
                      setFont(ArrowIconFont);
                      setForeground(Color.white);                      
                      setText(bool ? " "+DownArrowChar : " "+RightArrowChar);                  
                  }
            	  else
            	  {
            		 // final LWPathway p = entry.pathway;
            		  if (entry.pathway.getEntries().isEmpty())
            		  {
            			  if (entry.pathway.getEntries().isEmpty())
                      		emptyString = VueResources.getString("pathways.empty.label");
            			  if (getRowHeight(row) != 40)
            				  setRowHeight(row, 40);            			
            			  gl = new GradientLabel(entry.pathway,emptyString);
            			
            		  }
            		  /*if (p == VUE.getActivePathway())
                    	setBackground(Color.red);
                	else*/
            		  //Background is always going to be gradient now.
                
            		 setBackground(BGColor);
            		 setFont(PathwayFont);
                         if (VUE.getActiveEntry() == entry)
                             setForeground(Color.black);
                         else
                             setForeground(Color.white);
            		 setText(debug+"   " + entry.getLabel());
            		// this.setAlignmentY(Component.TOP_ALIGNMENT);
            	  }
            	this.setOpaque(false);
                gl.setLayout(new BorderLayout());
                gl.add(this,BorderLayout.NORTH);

                // todo: not very efficient to be now using the GradientLabel this way --
                // we're constructing one of these for each pathway for every single paint,
                // and they create a ton of sub-objects themselves...
                return gl; 
            }
            else {
            	//entry is not a pathway if you're in the wrong column go null;            	
            	//only return the label for the proper column...
            	
            	final LWPathway activePathway = VUE.getActivePathway();
            	
            	if (col != PathwayTableModel.COL_LABEL)
            	{
            		if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
            		{
            			setBackground(selectedColor(entry));
            			setForeground(selectedColor(entry));            			            			
            		}
            		else
            		{
            			setBackground(BGColor);
            			setForeground(BGColor);
            		}
            		this.setOpaque(false);
                	label.setForeground(this.getForeground());
                	label.setBackground(this.getBackground());
                	label.setOpaque(true);
                	label.setLayout(new BorderLayout());
                	label.add(this,BorderLayout.CENTER);
                	return label;
            	}
            	
                setFont(SelectedEntryFont);
            	setForeground(Color.black);
                if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) {
                    // This is the current item on the current path
                                        
                	setBackground(selectedColor(entry)); 
                    
                    	setText(debug+"   "+entry.getLabel());
                    //setText(debug+"  * "+getText());
                } else {
                   // setFont(EntryFont);
                    
                    
                	setText(debug+"   "+entry.getLabel());
                        if (entry.node != null && (entry.node.isFiltered() || entry.node.isHidden()))
                            setForeground(Color.lightGray);
                        
                        
                }
                
            	this.setOpaque(false);
            	label.setForeground(this.getForeground());
            	label.setBackground(this.getBackground());
            	label.setOpaque(true);
            	label.setLayout(new BorderLayout());
            	label.add(this,BorderLayout.CENTER);
            	return label;
            }                        	
        }

	}
 
    private static final RoundRectangle2D BlobShape = new RoundRectangle2D.Float();
    
    private class GradientLabel extends JPanel
    {
        //Gradient painting necessities
        private final Color
            TopGradient1 = new Color(179,166,121),
            BottomGradient1 = new Color(142,129,82);

        private final Color TopGradient2 = new Color(195,193,186);
        private final Color BottomGradient2 = new Color(162,161,156);
        private LWPathway path;
        private GradientPaint Gradient = null;
       
        private GradientPaint Gradient2 = null;
        private String emptyString = null;
        private Color paintColor = null;
       
        public GradientLabel(LWPathway pathway, String emptyString, Color paintColor)
        {
            setOpaque(false);
            path=pathway;
            Gradient = new GradientPaint(0,           0, TopGradient1,
                                         0, 20, BottomGradient1);
            Gradient2 = new GradientPaint(0,           0, TopGradient2,
                                          0, 20, BottomGradient2);
            setLayout(new BorderLayout());
            this.paintColor = paintColor;
            setPreferredSize(new Dimension(getWidth(),40));
           
            this.emptyString = emptyString;
        }
       
        public GradientLabel(LWPathway pathway, String emptyString)
        {
            this(pathway,emptyString,null);
        }
        public GradientLabel(LWPathway pathway)
        {
            this(pathway,null,null);
        }
        
        public GradientLabel(LWPathway pathway, Color paintColor)
        {
            this(pathway,null,paintColor);
        }
        
        
        public void paintComponent(Graphics _g) {
            final Graphics2D g = (Graphics2D) _g;
            paintGradient(g);
            if (paintColor != null)
                paintColorBlob(g, paintColor);
            //  super.paintComponent(g2);
        }

        private void paintColorBlob(final Graphics2D g, final Color color)
        {
            BlobShape.setRoundRect(2, 1, getWidth()-8, 20-4, 7, 7);

            // Draw the pathway color swatch.
            
            // We want to show the translucent nature of the pathway
            // color, so we fill with white first, so when we draw the
            // translucent pathway color, it will be mixed with pure
            // white (default map color)

            g.setColor(Color.white);
            g.fill(BlobShape);
            g.setColor(color);
            g.fill(BlobShape);

            // Now draw a border on the color swatch:

            g.setColor(Color.gray);
            g.draw(BlobShape);

//             g.setColor(color);
//             g.fillRoundRect(2, 2, getWidth()-8, 20-4,7,7);
//             g.setColor(Color.gray);
//             g.drawRoundRect(2, 2, getWidth()-8, 20-4,7,7);
        }              


         private void paintGradient(Graphics2D g)
         {       
        	 Paint p = g.getPaint();
             if (path != null && path == VUE.getActivePathway())
                 g.setPaint(Gradient);
             else
                 g.setPaint(Gradient2);
             
             g.fillRect(0, 0, getWidth(),20);
             
             g.setPaint(p);
             g.setColor(Color.white);                    
             g.fillRect(0,20,getWidth(),40);
             g.setColor(Color.lightGray);
             g.setFont(EntryFont);
             if (emptyString != null)
            	 g.drawString(emptyString, 0, 33);
         }
    }
    private final Border iconBorder = new EmptyBorder(3,0,5,5);
    private final Border NodeIconBorder = new EmptyBorder(1,0,0,0);
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
                return this;
                                    
            this.setBorder(DefaultBorder);
            
            
            if (entry.isPathway()) 
            {
                boolean bool = false;
                if (obj instanceof Boolean)
                    bool = ((Boolean)obj).booleanValue();
                
                if (col == PathwayTableModel.COL_VISIBLE) {
                    setIcon(bool ? eyeOpen : eyeClosed);
                    setBorder(iconBorder);
                    setToolTipText("Show/hide pathway");
                }              
                else if (col == PathwayTableModel.COL_NOTES) {
                    if (entry.node == VUE.getActivePathway()  && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor(entry));
                    else
                        setBackground(BGColor);
                    
                    if (entry.hasNotes()) {
                        setIcon(notesIcon);
                        setToolTipText(entry.getNotes());
                    } else {
                        setToolTipText(null);
                        setIcon(null);
                    }                                        
                }
                else if (col == PathwayTableModel.COL_LOCKEDnMAPVIEW) {                	
            		
                    setBorder(iconBorder);
                    setIcon(bool ? lockIcon : lockOpenIcon);
                    this.setAlignmentY(JLabel.CENTER_ALIGNMENT);
                    if (entry.node == VUE.getActivePathway() && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor(entry));
                    else
                        setBackground(BGColor);
                    setToolTipText("Is locked");
                }
                else
                {
                    if (entry.node == VUE.getActivePathway()  && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor(entry));
                    else
                        setBackground(BGColor);
                }
               
               GradientLabel gl = new GradientLabel(entry.pathway);
         	   this.setOpaque(false);
         	   gl.add(this,BorderLayout.NORTH);
         	   return gl;
            }
            else
            {
            	final LWPathway activePathway = VUE.getActivePathway();
            	//System.out.println("return null");
            	if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
        		{
        			setBackground(selectedColor(entry));
        			setForeground(selectedColor(entry));
            		setOpaque(true);            	
        			setIcon(null);
        			
        		}
        		else
        		{
        			setBackground(BGColor);
        			setOpaque(true);        		
        			setForeground(BGColor);
        			setIcon(null);
        		}
            	
                if (col == PathwayTableModel.COL_NOTES) {
                    if (entry.hasNotes()) {
                        setIcon(notesIcon);
                        setToolTipText(entry.getNotes());
                    } else {
                        setToolTipText(null);
                        setIcon(null);
                    }
                }
                else if (col == PathwayTableModel.COL_LOCKEDnMAPVIEW) 
                {
                    if (entry.hasVariableDisplayMode()) {
                        boolean isMapView = false;
                        if (obj instanceof Boolean)
                            isMapView = ((Boolean)obj).booleanValue();
                    
                        if (isMapView) {
                            setBorder(NodeIconBorder);
                            setIcon(mapViewIcon);
                        } else {
                            setIcon(entry.pathway.mSlideIcon);
                            //setIcon(slideViewIcon);
                        }
                    } else {
                        setIcon(null);
                    }
                    
                    setToolTipText("Toggle map/slide node");
                    
                } 

                return this;             
            }                                             	                
        }
        
    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
    public String toString()
    {
        return "PathwayTable[" + VUE.getActivePathway() + "]";
    }
    
	public void dragEnter(DropTargetDragEvent arg0) {
	/*	PathwayTableModel model = (PathwayTableModel)this.getModel();
		System.out.println(arg0.getLocation());
		int index = model.getPathwayIndexForElementAt(rowAtPoint(arg0.getLocation()));
	    if (index < 0)  
	    {
	    	arg0.rejectDrag();	    	
	    	System.out.println("DRAG REJECTED" +index);
	    }
	    else
	    {
	    	arg0.acceptDrag(DnDConstants.ACTION_MOVE);
	    	System.out.println("DRAG ACCEPTED" + index);
	    }
		*/
		arg0.acceptDrag(DnDConstants.ACTION_MOVE);
		
	}
	
	public void dragOver(DropTargetDragEvent arg0) {
		arg0.acceptDrag(DnDConstants.ACTION_MOVE);
		/*PathwayTableModel model = (PathwayTableModel)this.getModel();
		System.out.println(arg0.getLocation());
		int index = model.getPathwayIndexForElementAt(rowAtPoint(arg0.getLocation()));
	    if (index < 0)  
	    {
	    	arg0.rejectDrag();	    		    	
	    	
	    }
	    else
	    {
	    	
	    arg0.acceptDrag(DnDConstants.ACTION_MOVE);	
	    }
	    */
			
	}
	public void drop(DropTargetDropEvent arg0) {
	
		 Transferable transferable = arg0.getTransferable();				
	      LWPathway.Entry entry =null;
	      
			try {
				entry = (LWPathway.Entry)transferable.getTransferData(DataFlavor.plainTextFlavor);
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();	
				arg0.rejectDrop();
				arg0.dropComplete(false);
			} catch (IOException e) {
				e.printStackTrace();
				arg0.rejectDrop();
				arg0.dropComplete(false);
			}

			if( entry != null ) {
				
                // See where in the list we dropped the element.
            	PathwayTableModel model = (PathwayTableModel)this.getModel();

            	//System.out.println("START" + model.getPathwayForElementAt(dropRow));
            	//System.out.println("END" + model.getPathwayForElementAt(rowAtPoint(arg0.getLocation())));
            	if (!model.getPathwayForElementAt(dropRow).equals(model.getPathwayForElementAt(rowAtPoint(arg0.getLocation()))))
            	{            		            		 
            		 VueUtil.alert(this,
            				 VueResources.getString("presentationDialog.dropError.text"),
            				 VueResources.getString("presentationDialog.dropError.title"),
            				    JOptionPane.ERROR_MESSAGE);
            		             
            		 arg0.rejectDrop();
            		 arg0.dropComplete(false);
            	}
            	else
            	{
            		if (dropIndex < 0 || model.getPathwayIndexForElementAt(rowAtPoint(arg0.getLocation())) < 0)
            		{	
            			arg0.dropComplete(false);
            			arg0.rejectDrop();
            		}
            		else
            		{

                            model.moveRow(dropIndex, dropIndex,
                                          model.getPathwayIndexForElementAt(rowAtPoint(arg0.getLocation())),
                                          model.getPathwayForElementAt(dropRow));
                            VUE.getUndoManager().mark();                            

                            arg0.dropComplete(true);
            			arg0.acceptDrop(DnDConstants.ACTION_MOVE);
            		}
            	}	            					
            }   // end if: we got the object
            // Else there was a problem getting the object
            else 
            {
				arg0.dropComplete(false);
				arg0.rejectDrop();
            }
               // end else: can't get the object
           
			return;
	}
	public void dropActionChanged(DropTargetDragEvent arg0) {
	}
	
    public void dragGestureRecognized(DragGestureEvent event) {
    		PathwayTableModel model = (PathwayTableModel)this.getModel();
    		//System.out.println("SELROW : " + this.getSelectedRow());
    		dropIndex = model.getPathwayIndexForElementAt(this.getSelectedRow());
    		dropRow = this.getSelectedRow();
                            
			try {
				//TODO: FIGURE OUT WHAT TO TRANSFER				
				LWPathway.Entry entry = model.getEntry(this.getSelectedRow());
				
				if(entry.isPathway())
					return;
				else
					dragSource.startDrag(event, DragSource.DefaultMoveDrop, entry, this);
				
			} catch (InvalidDnDOperationException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
        
    }

	public void dragExit(DropTargetEvent arg0) {
		// not implemented			
//		System.out.println("dragexittarget");
	}
	public void dragDropEnd(DragSourceDropEvent arg0) {
		// not implemented		
		//System.out.println("dragendsource");
	}
	public void dragEnter(DragSourceDragEvent arg0) {
		// not implemented		
		//System.out.println("dragentersource");
	}
	public void dragExit(DragSourceEvent arg0) {
	//	System.out.println("dragexitsource");
		//not implemented		
	}
	public void dragOver(DragSourceDragEvent arg0) {
		//Not implemented
		//System.out.println("dragoversource");
		
	}
	public void dropActionChanged(DragSourceDragEvent arg0) {
		// not implemented'
	//	System.out.println("dropactionchanged");
	}
	
    private void displayContextMenu(MouseEvent e) {
        getPopup(e).show(e.getComponent(), e.getX(), e.getY());
    }

	private JPopupMenu m = null;
	
	//presentation menu items
	private static final String context="contextMenus.pathways.";
	private static final JMenuItem renamePresentation = new JMenuItem(VueResources.getString(context+"pathway.rename"));
	private static final JMenuItem playbackPresentation = new JMenuItem(VueResources.getString(context+"pathway.start"));
	private static final JMenuItem addNoteEntry = new JMenuItem(VueResources.getString(context+"pathway.notes"));
	private static final JMenuItem masterSlidePresentation = new JMenuItem(VueResources.getString(context+"pathway.masterslide"));
	private static final JMenuItem deletePresentation = new JMenuItem(VueResources.getString(context+"pathway.delete"));	

	//entry menu items
	private static final JMenuItem deleteEntry = new JMenuItem(VueResources.getString(context+"entry.delete"));
	private static final JMenuItem previewEntry = new JMenuItem(VueResources.getString(context+"entry.preview"));
	private static final JMenuItem editEntry = new JMenuItem(VueResources.getString(context+"entry.edit"));
	private static final JMenuItem duplicateEntry = new JMenuItem(VueResources.getString(context+"entry.duplicate"));
	
	public void actionPerformed(ActionEvent e)
	{
            if (DEBUG.EVENTS) Log.debug("actionPerformed: " + e);
            
		if (e.getSource().equals(renamePresentation))
		{
			//System.out.println("selectedX : " + selectedX);
			//the pathway name column is always column 2
			boolean a = this.editCellAt(selectedY,2);			
		}
		else if (e.getSource().equals(playbackPresentation))
		{
          	final PresentationTool presTool = PresentationTool.getTool();
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                VUE.toggleFullScreen(true);
            }});
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                //VueToolbarController.getController().setSelectedTool(presTool);
                VUE.setActive(VueTool.class, this, presTool);
            }});
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                presTool.startPresentation();
            }});
		}
		else if (e.getSource().equals(masterSlidePresentation))
		{	
			Actions.EditMasterSlide.act(selectedEntry.pathway.getMasterSlide());
		}
		else if (e.getSource().equals(deletePresentation))
		{
			final Object[] defaultOrderButtons = {VueResources.getString("optiondialog.deletepathway.cancel"),VueResources.getString("optiondialog.deletepathway.delete")};
			
			final Object[]	macOrderButtons = {VueResources.getString("optiondialog.deletepathway.delete"), VueResources.getString("optiondialog.deletepathway.cancel")};
			
		   int response = VueUtil.option
           (VUE.getDialogParent(),
        	 VueResources.getString("optiondialog.deletepathway.message") ,         
        	 VueResources.getString("optiondialog.deletepathway.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            (Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons),             
            VueResources.getString("optiondialog.deletepathway.cancel")
            );
		   if (Util.isMacPlatform() ? response == 0 : response == 1)
		   {
			//delete the pathway
			   VUE.getPathwayPanel().deletePathway(selectedEntry.pathway);
		   }
			
		}
		else if (e.getSource().equals(addNoteEntry))
		{
			Actions.NotesAction.actionPerformed(e);
		}
		else if (e.getSource().equals(editEntry) || e.getSource().equals(previewEntry))	
		{			
			Actions.EditSlide.act(selectedEntry.getSlide());
		}
		else if (e.getSource().equals(duplicateEntry))	
		{			
			//Actions.DuplicateSlide.act(selectedEntry.getFocal());
		}
		else if (e.getSource().equals(deleteEntry))
		{
		    GUI.invokeAfterAWT(new Runnable() { public void run() {
		    	DeleteSlideDialog dsd = PathwayPanel.getDeleteSlideDialog();
		        java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
                p.x -= dsd.getWidth() / 2;
                p.y -= dsd.getHeight() / 2;
                dsd.setLocation(p);
                if (dsd.showAgain())
                {
                	dsd.setVisible(true);
                }
                
                if (dsd.getOkCanel())
                	VUE.getActivePathway().remove(VUE.getActivePathway().getCurrentIndex());
		    	
            }});
			   
		}
		repaint();
	}
	LWPathway.Entry selectedEntry = null;
	int selectedX = 0;
	int selectedY = 0;
	private JPopupMenu getPopup(MouseEvent e) 
	{
		m = new JPopupMenu(VueResources.getString("pathwaytable.menu.pathway"));
		int row = this.rowAtPoint(e.getPoint());
		
        PathwayTableModel tableModel = getTableModel();
		int col = this.getColumnModel().getColumnIndexAtX(e.getX());
	    final LWPathway.Entry entry = tableModel.getEntry(row);
	    
	    
	    VUE.setActive(LWPathway.Entry.class, this, entry);
	    
        if (row >= 0)
            changeSelection(row, -1, false, false);
	    selectedEntry = VUE.getActiveEntry();
	    selectedX = col;
	    selectedY = row;
	    if (entry.isPathway())
	    {
	    	m.add(renamePresentation);
	    	m.add(playbackPresentation);
	    	m.add(addNoteEntry);
	    	m.addSeparator();
	    	m.add(masterSlidePresentation);
	    	m.add(deletePresentation);	    	

	    }
	    else
	    {
	    	m.add(addNoteEntry);
	    	m.add(previewEntry);
	    	m.addSeparator();
	    	m.add(editEntry);
	    	m.add(masterSlidePresentation);
	    	//m.add(duplicateEntry);
	    	m.add(deleteEntry);
	    	if (entry.isPortal())
	    		editEntry.setEnabled(false);
	    	else
	    		editEntry.setEnabled(true);
	    }

		return m;
	}
    
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    private boolean mouseWasConsumed;
    
    public void mousePressed(MouseEvent e) {

        mouseWasConsumed = false;

        if (DEBUG.MOUSE) Log.debug(e);
		
        if (GUI.isMenuPopup(e)) {
            displayContextMenu(e);
            mouseWasConsumed = true;
            return;
        }
	
    	if ((e.getModifiers() & MouseEvent.BUTTON3_MASK )!=0)
            return;
        
        int row = getSelectedRow();
        PathwayTableModel tableModel = getTableModel();
        lastSelectedRow = row;
        int col = getSelectedColumn();
        //this.changeSelection(row,col,false,false);
        if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: selected row "+row+", col "+col);
	                    
	                    
        final LWPathway.Entry entry = tableModel.getEntry(row);
        
        // the below statement appears to be by definition false:
        // a pathway entry asks it's pathway if it has no members,
        // which should never be true, unless this is somehow
        // to detect something that was just deleted? -- SMF 2008-05-28
        if (entry.pathway.getEntries().isEmpty())
            return;
        
        if (entry.isPathway()) {
            if (col == PathwayTableModel.COL_VISIBLE ||
                col == PathwayTableModel.COL_OPEN ||
                col == PathwayTableModel.COL_LOCKEDnMAPVIEW)
                {
                    // setValue forces a value toggle in these cases
                    setValueAt(entry.pathway, row, col);
                    mouseWasConsumed = true;
                }
        } else if (col == PathwayTableModel.COL_LOCKEDnMAPVIEW && entry.hasVariableDisplayMode()) {
            setValueAt(entry.pathway, row,col);
            PathwayPanel.updateMapViewDependentActions();
            mouseWasConsumed = true;
        }
    }

    public void mouseClicked(MouseEvent e) {
        
        if (DEBUG.MOUSE) Log.debug(e);

        if (!mouseWasConsumed && isDoubleClickEvent(e)) {
            Actions.LaunchPresentation.fire(this, e);
        }
    }

    private final boolean isDoubleClickEvent(MouseEvent e) {
        return (e.getClickCount() > 1 && e.getClickCount() % 2 == 0) // % 2 detects cascading double clicks (reported as a 4 click, 6 click, etc)
            && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0;
    }

    
    
    
    
	

    class LabelCellEditor extends DefaultCellEditor
    {
    	public LabelCellEditor(JTextField edit)
    	{
    		super(edit);        
    	}

    }
    
    class PathwayNameField extends JTextField implements MouseListener
    {
    	LabelCellEditor labelEditor;
    	public PathwayNameField()
    	{
    		super();
    		addMouseListener(this);
    		        		        
    	}
    
    	public void setCellEditor(LabelCellEditor editor)
    	{
    		labelEditor = editor;
    	}
    	
    	public Rectangle getBounds()
    	{
    		Rectangle r = super.getBounds();
    		r.height=20;
    		return r;
    	}
    	public boolean isOpaque()
    	{
    		return true;
    	}
    
    	//public void repaint(){}
    	//public void paintImmediately(){}        	
   // 	public void paint(Graphics g){}
    	public void paintComponent(Graphics g)
    	{        		
    		 g.setColor(Color.white);
    		 super.paintComponent(g);
             Graphics2D g2 = (Graphics2D)g;
             Rectangle r= g2.getClipRect();
             g2.setColor(Color.gray);
             g2.drawLine(r.x, 20, r.width, 20);
             g2.setColor(Color.lightGray);
             g2.setFont(EntryFont);                 
             g2.drawString("This pathway is empty", 0, 33);	
    	}
    	public void paintBorder(Graphics g)
    	{
    	
    		
    	}
   // 	public void paintChildren(Graphics g){}
    //	public void update(){}

    	public void mouseClicked(MouseEvent e)
    	{
    		
    	}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mousePressed(MouseEvent e) {
		if (e.getY() >20 && labelEditor != null)
			labelEditor.stopCellEditing();
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
    	
    
    }
   

}
    
