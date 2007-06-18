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
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.TableUI;
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
 * @version $Revision: 1.72 $ / $Date: 2007-06-18 20:28:45 $ / $Author: mike $
 */

public class PathwayTable extends JTable
    implements DropTargetListener,
               DragSourceListener,
               DragGestureListener,
               MouseListener
{
	
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
    private final int[] colWidths = {25,20,240,30,30,20};

    private static Color selectedColor;

    public void setUI(TableUI ui)
    {
    	super.setUI(ui);
    }
    public PathwayTable(PathwayTableModel model) {
        super(model);
        initComponents();
        selectedColor = GUI.getTextHighlightColor();

        this.notesIcon = VueResources.getImageIcon("notes");
        this.lockIcon = VueResources.getImageIcon("lock");
        this.lockOpenIcon = VueResources.getImageIcon("lockOpen");
        this.eyeOpen = VueResources.getImageIcon("pathwayOn");
        this.eyeClosed = VueResources.getImageIcon("pathwayOff");
        this.mapViewIcon = VueResources.getImageIcon("mapView");
        this.slideViewIcon = VueResources.getImageIcon("slideView");
        
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setRowHeight(20);
        this.setRowSelectionAllowed(true);
        this.setShowVerticalLines(true);        
        this.setShowHorizontalLines(true);
        this.setGridColor(Color.lightGray);
        this.setIntercellSpacing(new Dimension(0,1));
        this.setBackground(BGColor);
        //this.setSelectionBackground(SelectedBGColor);
     //   this.setDragEnabled(true);

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

        for (int i = 0; i < colWidths.length; i++){
            TableColumn col = getColumn(PathwayTableModel.ColumnNames[i]);
            if (i == PathwayTableModel.COL_OPEN)
                col.setMaxWidth(20);
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

        addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (DEBUG.PATHWAY || DEBUG.KEYS) System.out.println(this + " " + e);
                    final LWPathway pathway = VUE.getActivePathway();
                    if (pathway == null)
                        return;
                    switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_LEFT:
                        if (pathway.atFirst())
                            pathway.setIndex(-1);
                        else
                            pathway.setPrevious();
                        e.consume();
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_RIGHT:
                        pathway.setNext();
                        e.consume();
                        break;
                    }
                }
            });
        // end of PathwayTable constructor
    }

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

    private class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			             MouseListener
    {
        Color currentColor;
        JPanel button;

        public ColorEditor() {
            button = new ColorRenderer();
            //button.addActionListener(this);
            button.addMouseListener(this);
            button.setBorder(null);
            //button.setBorder(new LineBorder(BGColor, 3));
        }

//        public void actionPerformed(ActionEvent e) {
    
  //      }

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
            Color c = VueUtil.runColorChooser("Pathway Color Selection", currentColor, VUE.getDialogParent());
            fireEditingStopped();
            if (c != null) {
                // why the row checking here?
                int row = getSelectedRow();
                if (row == -1)
                    row = lastSelectedRow;
                if (row != -1)
                    getTableModel().setValueAt(currentColor = c, row, 5);
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
            
         //   setBorder(new LineBorder(BGColor, 1)); // fyi: empty border no good: won't paint over
        	Gradient = new GradientPaint(0,           0, TopGradient1,
                    0, 20, BottomGradient1);
        	Gradient2 = new GradientPaint(0,           0, TopGradient2,
                    0, 20, BottomGradient2);
        	
            setToolTipText("Select Color");
        }
        
    	//final RoundRectangle2D BlobShape = new RoundRectangle2D.Float();
    	Color paintColor = null;
    	
        protected void paintComponent(Graphics g)
        {
        	paintGradient((Graphics2D)g);
        	//g.setClip(2, 2, getWidth()-4, getHeight()-4);
        	Graphics2D g2 = (Graphics2D)g;
        	g2.setColor(paintColor);
        	//BlobShape.setRoundRect(2, 2, getWidth()-8, getHeight()-4, 7, 7);
        	//g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);                          
            g2.fillRoundRect(2, 2, getWidth()-8, getHeight()-4,7,7);
            g2.setColor(Color.gray);
            g2.drawRoundRect(2, 2, getWidth()-8, getHeight()-4,7,7);
        	//g2.fill(BlobShape);
        //	super.paintComponent(g);
        }
        
        private void paintGradient(Graphics2D g)
        {       
        	final LWPathway.Entry entry = getTableModel().getEntry(curRow);
        	
        	paintColor = this.getForeground();
        	
            if (entry.pathway != null && entry.pathway == VUE.getActivePathway())
                g.setPaint(Gradient);
            else
                g.setPaint(Gradient2);
            
            g.fillRect(0, 0, getWidth(),20);
        }
        
        public java.awt.Component getTableCellRendererComponent(
                                    JTable table, Object color, 
                                    boolean isSelected, boolean hasFocus, 
                                    int row, int col)
        {
            final LWPathway.Entry entry = getTableModel().getEntry(row);
            curRow=row;
            if (entry == null) {
            	{
            		setBackground((Color) color);
                    setForeground((Color) color);
                return this;
            	}
            } else if (entry.isPathway()) {
                setBackground((Color) color);
                setForeground((Color) color);
               
                return this;
            } else
            {
            	JLabel p = new DefaultTableCellRenderer();
            	p.setOpaque(true);
            	final LWPathway activePathway = VUE.getActivePathway();
            	if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
        		{
            		
            		p.setBackground(selectedColor);
            		p.setForeground(selectedColor);
        		}
            	else
            	{
            		p.setBackground(BGColor);
            		p.setForeground(BGColor);
            	}
                
            	return p;
            }
             //   return null;
        }  
    }
  /*  private class BooleanRenderer extends JCheckBox implements TableCellRenderer {
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
    */
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
         
           // setBorder(DefaultBorder);

            String debug = "";

            if (DEBUG.PATHWAY) debug = "(row"+row+")";
            GradientLabel gl = new GradientLabel(entry.pathway);
            
            setMinimumSize(new Dimension(10, 20));
            setPreferredSize(new Dimension(500, 20));      
            setOpaque(true);
            
            if (entry.isPathway())
            {
            	  if (col == PathwayTableModel.COL_OPEN) 
            	  {
                  	boolean bool = false;
                    if (value instanceof Boolean)
                        bool = ((Boolean)value).booleanValue();
                  
                  	setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
                  	setForeground(Color.white);
                  	setText(bool ? " "+DownArrowChar : " "+RightArrowChar);                  
                  }
            	  else
            	  {
            		  final LWPathway p = entry.pathway;
            		  /*if (p == VUE.getActivePathway())
                    	setBackground(Color.red);
                	else*/
            		  //Background is always going to be gradient now.
                
            		 setBackground(BGColor);
            		 setFont(PathwayFont);
            		 setForeground(Color.white);
            		 setText(debug+"   " + entry.getLabel());            		
            	  }
            }
            else {
            	//entry is not a pathway if you're in the wrong column go null;
            	
            	//only return the label for the proper column...
            	
            	final LWPathway activePathway = VUE.getActivePathway();
            	
            	if (col != PathwayTableModel.COL_LABEL)
            	{
            		if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
            		{
            			setBackground(selectedColor);
            			setForeground(selectedColor);
            			
            		}
            		else
            		{
            			setBackground(BGColor);
            			setForeground(BGColor);
            		}
            		return this;
            	}
            	
                setFont(SelectedEntryFont);
            	setForeground(Color.black);
                if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) {
                    // This is the current item on the current path
                                        
                	setBackground(selectedColor); 
                    
                    	setText(debug+"   "+entry.getLabel());
                    //setText(debug+"  * "+getText());
                } else {
                   // setFont(EntryFont);
                    
                    
                	setText(debug+"   "+entry.getLabel());
                        if (entry.node != null && (entry.node.isFiltered() || entry.node.isHidden()))
                            setForeground(Color.lightGray);
                        
                        
                }
            }
            
            if (entry.isPathway())
            {
            	this.setOpaque(false);
            	gl.setLayout(new BorderLayout());
            	gl.add(this,BorderLayout.CENTER);            	
            	return gl;
            }
            else                  	
            	return this;
        }  
    }
 
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
       
        public GradientLabel(LWPathway pathway)
        {
        	setOpaque(false);
        	path=pathway;
        	Gradient = new GradientPaint(0,           0, TopGradient1,
                    0, 20, BottomGradient1);
        	Gradient2 = new GradientPaint(0,           0, TopGradient2,
                    0, 20, BottomGradient2);
        }
        
        
    	 public void paintComponent(Graphics g) {
             paintGradient((Graphics2D)g);
             super.paintComponent(g);
         }

         private void paintGradient(Graphics2D g)
         {       
             if (path != null && path == VUE.getActivePathway())
                 g.setPaint(Gradient);
             else
                 g.setPaint(Gradient2);
             
             g.fillRect(0, 0, getWidth(),20);
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
            
            if (entry.isPathway()) {
                boolean bool = false;
                if (obj instanceof Boolean)
                    bool = ((Boolean)obj).booleanValue();
                
                if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW) {
                    setIcon(bool ? eyeOpen : eyeClosed);
                    setBorder(iconBorder);
                    setToolTipText("Show/hide pathway");
                }              
                else if (col == PathwayTableModel.COL_NOTES) {
                    if (entry.node == VUE.getActivePathway()  && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor);
                    else
                        setBackground(BGColor);
                                        
                }
                else if (col == PathwayTableModel.COL_LOCKED) {
                    setIcon(bool ? lockIcon : lockOpenIcon);
                    if (entry.node == VUE.getActivePathway() && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor);
                    else
                        setBackground(BGColor);
                    setToolTipText("Is locked");
                }
                else
                {
                    if (entry.node == VUE.getActivePathway()  && entry.pathway.getCurrentEntry() == entry)
                        setBackground(selectedColor);
                    else
                        setBackground(BGColor);
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
            }  else if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW && !entry.isPathway()) 
            {
            	boolean bool = false;
                if (obj instanceof Boolean)
                    bool = ((Boolean)obj).booleanValue();
                setBorder(iconBorder);
                if (entry.hasVariableDisplayMode())
                    setIcon(bool ? mapViewIcon : slideViewIcon);
                else
                    setIcon(null);
                setToolTipText("Toggle map/slide node");
                final LWPathway activePathway = VUE.getActivePathway();
            	if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
        		{
        			setBackground(selectedColor);
        			setForeground(selectedColor);
            		setOpaque(true);        			        			
        		}
        		else
        		{
        			setBackground(BGColor);
        			setOpaque(true);        			
        			setForeground(BGColor);        			
        		}
            } 
            else if (!entry.isPathway())
            {
            	final LWPathway activePathway = VUE.getActivePathway();
            	//System.out.println("return null");
            	if (entry.pathway == activePathway && entry.pathway.getCurrentEntry() == entry) 
        		{
        			setBackground(selectedColor);
        			setForeground(selectedColor);
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
        		return this;             
            }
            
           if (entry.isPathway())
           {
        	   GradientLabel gl = new GradientLabel(entry.pathway);
        	   this.setOpaque(false);
        	   gl.add(this);
        	   return gl;
           }
           else        	   
            return this;            
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
            		 JOptionPane.showMessageDialog(this,
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
            			model.moveRow(dropIndex, dropIndex, model.getPathwayIndexForElementAt(rowAtPoint(arg0.getLocation())),model.getPathwayForElementAt(dropRow));            		
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
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void mousePressed(MouseEvent e) {
		  int row = getSelectedRow();
		  PathwayTableModel tableModel = getTableModel();
	      lastSelectedRow = row;
	      int col = getSelectedColumn();
	      
	      if (DEBUG.PATHWAY) System.out.println("PathwayTable: valueChanged: selected row "+row+", col "+col);
	                    
	                    
	      final LWPathway.Entry entry = tableModel.getEntry(row);	
	
	    if (entry.isPathway()) 
	    {
            if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW ||
                col == PathwayTableModel.COL_OPEN ||
                col == PathwayTableModel.COL_LOCKED) {
                // setValue forces a value toggle in these cases
                setValueAt(entry.pathway, row, col);
        //        selectedEntry = false;
            }
            //pathway.setCurrentIndex(-1);
        } 
	    else if (col == PathwayTableModel.COL_VISIBLEnMAPVIEW && entry.hasVariableDisplayMode()) 
        {
            setValueAt(entry.pathway, row,col);
          //  selectedEntry = false;
        }
    }
	
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}	    
}
    
