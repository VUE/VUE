//
//  CabViewer.java
//  S.Fraize July 2002
//

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import org.okip.service.filing.api.*;

public class CabViewer extends JFrame
    implements  ActionListener
{
    JTree jtree;
    JTable jtable;
    CabTreeNode rootNode;

    AbstractTableModel displayedModel;

    public CabViewer()
    {
        super("OKI Cabinet Viewer");
        debug("constructing CabViewer");
        
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(new Dimension(800,640));
        this.setLocation(100,100);
        this.addMenus();
        this.setVisible(true);// show something right away
        
        rootNode = new CabTreeNode("CabViewer(invisible root node)");
        rootNode.setDisplayName("CabViewer");

        /*
         * create the JTree
         */
        this.jtree = new JTree(rootNode, true);
        this.jtree.setRootVisible(false);
        this.jtree.setExpandsSelectedPaths(true);//this have effect?
        addJTreeListeners();
        
        /*
         * create the JTable -- and we need a table-column model for that
         */
        CabinetTableModel cabinetDataModel = getDefaultCabinetTableModel();
        TableColumnModel columnModel = cabinetDataModel.getTableColumnModel();
        columnModel.setColumnMargin(10);// this makes for ugly breaks in line-selection indicator tho...
        this.jtable = new JTable(cabinetDataModel, columnModel);
        this.jtable.setGridColor(Color.lightGray);
        addTableRenderers();
        
        /*
         * create the JSplitPane & subcomponents with scrollers
         */
        JSplitPane splitPane = new JSplitPane();
        JScrollPane leftScroller = new JScrollPane(this.jtree);
        JScrollPane rightScroller = new JScrollPane(this.jtable);

        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(leftScroller);
        splitPane.setRightComponent(rightScroller);

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        //pane.add(getToolBar(), BorderLayout.NORTH);
        JLabel statusBar = new JLabel("Status messages");
        pane.add(splitPane, BorderLayout.CENTER);
        //pane.add(statusBar, BorderLayout.SOUTH);
        
        this.constructing = false;
    }

    public void setDisplayedModel(AbstractTableModel tm)
    {
        this.displayedModel = tm;
        jtable.setModel(this.displayedModel);
    }

    /*
     * Refresh the currently visible table.
     * todo: also refresh the tree in case
     * a directory was added.
     */
    public void refresh()
    {
        ((CabinetTableModel)this.displayedModel).refresh();
    }

    public void addRootCabinet(Cabinet cabinet)
    {
        CabTreeNode ctn = new CabTreeNode(cabinet);
        try {
            ctn.setDisplayName(cabinet.getPath());
        } catch (FilingException e) {
            errout(e);
        }
        rootNode.add(ctn);
        ((DefaultTreeModel)jtree.getModel()).reload(rootNode);
    }

    public void expandRootEntry(int n)
    {
        jtree.expandPath(jtree.getPathForRow(n));
    }

    CabinetTableModel defaultTableModel;
    CabinetTableModel getDefaultCabinetTableModel()
    {
        if (defaultTableModel == null)
            defaultTableModel = new CabinetTableModel(null);
        return defaultTableModel;
    }

    private JToolBar getToolBar()
    {
        // cache this!
        JToolBar tb = new JToolBar();
        //JButton tbQuit = new JButton("Quit");
        JButton tbClose = new JButton("Close");
        JButton tbRefresh = new JButton("Refresh");

        //tbQuit.addActionListener(this);
        tbClose.addActionListener(this);// will pick up "Close" action from menu
        tbRefresh.addActionListener(this);
        //tbQuit.setAction();

        //tb.add(tbQuit);
        tb.add(tbClose);
        tb.add(tbRefresh);

        
        tb.setBorderPainted(false);
        tb.setFloatable(false);
        return tb;
    }


    protected void addJTreeListeners()
    {
	jtree.addTreeSelectionListener
	    (new TreeSelectionListener() {
		    public void valueChanged(TreeSelectionEvent e)
		    {
			CabTreeNode cabNode = (CabTreeNode) jtree.getLastSelectedPathComponent();
			if (cabNode == null) return;
			debug("tree node selected: " + cabNode);
                        setDisplayedModel(cabNode.getDataModel());
		    }
		});

        // could put these on the the class instead of inline
        jtree.addTreeWillExpandListener
           (new TreeWillExpandListener() {
                   public void treeWillExpand(TreeExpansionEvent e)
                   {
                        TreePath path = e.getPath();
                        debug("tree expanding at path: " + path);
                        CabTreeNode cabNode = (CabTreeNode) path.getLastPathComponent();
			if (cabNode == null) return;
			debug("tree expanding at node: " + cabNode);
                        jtree.setSelectionPath(path);
                        setDisplayedModel(cabNode.getDataModel());
                   }
                   public void treeWillCollapse(TreeExpansionEvent e) {}
               });

    }
                   

    protected void addTableRenderers()
    {
        // Set some renderers (defined in-line) for the CabinetEntry & Date classes.
        // Could also set these up by column instead of class in CabinetTableModel.createTableColumnModel();
        // It would probably be cleaner to do it there.

        jtable.setDefaultRenderer(CabinetEntry.class, new DefaultTableCellRenderer()
            { // in-line class definition

                // An example custom renderer -- if the CabinetEntry is unwriteable, make it red,
                // if a directory (Cabinet vs. ByteStore) make it blue and add a "/", if
                // emacs backup file, gray it out.
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column)
                 {
                     if (value instanceof CabinetEntry) {
                         CabinetEntry ce = (CabinetEntry) value;
                         try {
                             String label = ce.getName();
                             if (ce.isCabinet())
                                 label += "/";
                             
                             if (!ce.canRead())
                                 setForeground(Color.lightGray);
                             else if (label.endsWith("~"))
                                 setForeground(Color.gray);
                             //else if (!ce.canWrite())
                             //  setForeground(Color.red);
                             else if (ce.isCabinet())
                                 setForeground(Color.blue);
                             else
                                 setForeground(Color.black);
                             value = label;
                         } catch (FilingException e) {
                             errout(e);
                             value = "<"+e.getMessage()+">";
                         }
                     }
                     return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                 }
            });
        
        jtable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer()
            { // in-line class definition
                DateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm:ss zzz yyyy");
                {
                    //setToolTipText("Last modification time");
                    setHorizontalAlignment(SwingConstants.RIGHT);
                }
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column)
                {
                    if (value instanceof Date) {
                        Date d = (Date) value;
                        value = dateFormatter.format(d);
                    } // else, it may be an empty string -- no date info available
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            });
    }
        
    
    private boolean constructing = true;
    private Font font = new Font("serif", Font.ITALIC+Font.BOLD, 48);
    public void paint(Graphics g)
    {
        super.paint(g);
        if (constructing) {
            g.setColor(Color.lightGray);
            g.setFont(font);
            g.drawString("OKI FILING DEMO", getWidth()/4, getHeight()/2);
        }
    }
    protected void _processEvent(AWTEvent e)
    {
        debug(e.toString());
	super.processEvent(e);
    }
    
	
    
    // Declarations for menus
    static JMenuBar mainMenuBar = null;
    static JMenu fileMenu = null;
    
    private static JMenuBar getMainMenuBar()
    {
        if (mainMenuBar == null)
            mainMenuBar = new JMenuBar();
        return mainMenuBar;
    }
    private static JMenu getFileMenu()
    {
        if (fileMenu == null)
            fileMenu = new JMenu("File");
        return fileMenu;
    }
        
    
    protected JMenuItem miRefresh;
    protected JMenuItem miNew;
    protected JMenuItem miOpen;
    protected JMenuItem miClose;
    protected JMenuItem miSave;
    protected JMenuItem miSaveAs;
	
    static final JMenu editMenu = new JMenu("Edit");
    protected JMenuItem miUndo;
    protected JMenuItem miCut;
    protected JMenuItem miCopy;
    protected JMenuItem miPaste;
    protected JMenuItem miClear;
    protected JMenuItem miSelectAll;
	
    public void addFileMenuItems() {
        getFileMenu();

        miRefresh = new JMenuItem ("Refresh");
        miRefresh.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        miRefresh.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.Event.META_MASK));
        fileMenu.add(miRefresh).setEnabled(true);
        miRefresh.addActionListener(this);
        
        miNew = new JMenuItem ("New");
        miNew.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.Event.META_MASK));
        fileMenu.add(miNew).setEnabled(false);
        miNew.addActionListener(this);

        miOpen = new JMenuItem ("Open...");
        miOpen.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.META_MASK));
        fileMenu.add(miOpen).setEnabled(false);
        miOpen.addActionListener(this);
		
        miClose = new JMenuItem ("Close");
        miClose.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.Event.META_MASK));
        fileMenu.add(miClose).setEnabled(true);
        miClose.addActionListener(this);
		
        miSave = new JMenuItem ("Save");
        miSave.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.META_MASK));
        fileMenu.add(miSave).setEnabled(false);
        miSave.addActionListener(this);
		
        miSaveAs = new JMenuItem ("Save As...");
        fileMenu.add(miSaveAs).setEnabled(false);
        miSaveAs.addActionListener(this);
		
        getMainMenuBar().add(fileMenu);
    }
	
	
    public void addEditMenuItems() {
        miUndo = new JMenuItem("Undo");
        miUndo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.Event.META_MASK));
        editMenu.add(miUndo).setEnabled(false);
        miUndo.addActionListener(this);
        editMenu.addSeparator();
 
        miCut = new JMenuItem("Cut");
        miCut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.Event.META_MASK));
        editMenu.add(miCut).setEnabled(false);
        miCut.addActionListener(this);

        miCopy = new JMenuItem("Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.Event.META_MASK));
        editMenu.add(miCopy).setEnabled(false);
        miCopy.addActionListener(this);

        miPaste = new JMenuItem("Paste");
        miPaste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.Event.META_MASK));
        editMenu.add(miPaste).setEnabled(false);
        miPaste.addActionListener(this);

        miClear = new JMenuItem("Clear");
        editMenu.add(miClear).setEnabled(true);
        miClear.addActionListener(this);
        editMenu.addSeparator();

        miSelectAll = new JMenuItem("Select All");
        miSelectAll.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.Event.META_MASK));
        editMenu.add(miSelectAll).setEnabled(true);
        miSelectAll.addActionListener(this);

        getMainMenuBar().add(editMenu);
    }
	
    public void addMenus() {
        addFileMenuItems();
        addEditMenuItems();
        //mainMenuBar.add(new JLabel("     Hello World    ", JLabel.RIGHT));
        //mainMenuBar.add(new JButton("Spin"));
        setJMenuBar (mainMenuBar);
    }

    // ActionListener interface (for menus)
    public void actionPerformed(ActionEvent newEvent) {
        if (newEvent.getActionCommand().equals(miNew.getActionCommand())) doNew();
        else if (newEvent.getActionCommand().equals(miRefresh.getActionCommand())) doRefresh();
        else if (newEvent.getActionCommand().equals(miOpen.getActionCommand())) doOpen();
        else if (newEvent.getActionCommand().equals(miClose.getActionCommand())) doClose();
        else if (newEvent.getActionCommand().equals(miSave.getActionCommand())) doSave();
        else if (newEvent.getActionCommand().equals(miSaveAs.getActionCommand())) doSaveAs();
        else if (newEvent.getActionCommand().equals(miUndo.getActionCommand())) doUndo();
        else if (newEvent.getActionCommand().equals(miCut.getActionCommand())) doCut();
        else if (newEvent.getActionCommand().equals(miCopy.getActionCommand())) doCopy();
        else if (newEvent.getActionCommand().equals(miPaste.getActionCommand())) doPaste();
        else if (newEvent.getActionCommand().equals(miClear.getActionCommand())) doClear();
        else if (newEvent.getActionCommand().equals(miSelectAll.getActionCommand())) doSelectAll();
        else
            debug("actionPerformed: " + newEvent.toString());
    }

    public void doRefresh()
    {
        this.refresh();
    }

    
    public void doNew() {}
    public void doOpen() { debug("doOpen"); }
    public void doClose() { debug("doClose"); System.exit(0); }
    public void doSave() {}
    public void doSaveAs() {}
    public void doUndo() {}
    public void doCut() { debug("cut row " + jtable.getSelectedRow()); }
    public void doCopy() {}
    public void doPaste() {}
    public void doClear() { jtable.clearSelection(); }
    public void doSelectAll() { jtable.selectAll(); }


    class CabTreeNode extends DefaultMutableTreeNode
    {
	AbstractTableModel dataModel;
	String displayName;
	
	CabTreeNode(Cabinet cabinet)
	{
	    super(cabinet);
	}
        
	private CabTreeNode(String s)
	{
	    super(s);
            setDisplayName(s);
            // for constructing the invisible root node
	}
        
        public void setDisplayName(String name)
        {
            this.displayName = name;
        }

	private Cabinet getCabinet()
	{
            if (getUserObject() instanceof Cabinet)
                return (Cabinet) getUserObject();
            return null;
	}
	
	AbstractTableModel getDataModel()
	{
	    if (this.dataModel == null) {
                debug("CabTreeNode: creating new CabinetTableModel for " + getCabinet());
		this.dataModel = new CabinetTableModel(getCabinet());
                expandTree();
	    }
	    return this.dataModel;
	}

        private void expandTree()
        {
            /*
             * BUILD OUT THE TREE
             * (Create new cab CabTreeNodes for any child cabinets found)
             * We assume that if they want the data model, it's an appropriate
             * time to expand deeper in to the tree structure.
             */
            Iterator i;
            try {
                i = getCabinet().entries();
            } catch (FilingException e) {
                errout(e);
                return;
            }
            while (i.hasNext()) {
                CabinetEntry ce = (CabinetEntry) i.next();
                try {
                    if (ce.isCabinet())
                        this.add(new CabTreeNode((Cabinet)ce));
                } catch (FilingException e) {
                    errout(e);
                }
            }
        }
            
	/*
	 * toString is very functional here -- it's
	 * used to produce the text that the tree widget displays.
	 */
	public String toString()
	{
	    if (displayName != null)
		return displayName;
	    try {
		CabinetEntry ce = (CabinetEntry) userObject;
		return ce.getName();
	    } catch (Exception e) {
		errout(e);
		return userObject.getClass().toString();
	    }
	}
    }

    class CabinetTableModel extends AbstractTableModel
    {
        final int COL_NAME = 0;
        final int COL_SIZE = 1;
        final int COL_MODTIME = 2;
        final int COL_PERMISSIONS = 3;
        final int COL_OWNER = 4;

        Cabinet cabinet;
	ArrayList entries;
        //        int columnCount = 0;
	
        CabinetTableModel(Cabinet c)
	{
	    this.entries = new ArrayList();
            if (c != null)
                this.loadModelFromCabinet(c);
	}
        
        /*
         * At the moment, getTableColumnModel and it's helper addTablecolumn
         * could really be methods on CabViewer (or static inner classes
         * if that were allowed), tho we're keeping them here and dynamic
         * in case we end up using different TableColumnModels for specific
         * cabinet types (or, perhaps, just via configuration preferences --
         * e.g., remembering a custom view on a particular cabinet).
         */
          
        TableColumnModel columnModel;
        public TableColumnModel getTableColumnModel()
        {
            if (columnModel != null)
                return columnModel;

            columnModel = new DefaultTableColumnModel();

            addTableColumn(COL_NAME, "File Name", 1000);
            addTableColumn(COL_SIZE, "Size", 200);
            addTableColumn(COL_MODTIME, "Modified", 700);
            addTableColumn(COL_PERMISSIONS, "Access", 150);
            //addTableColumn(COL_OWNER, "Owner", 500);

            return columnModel;
        }
        
        private void addTableColumn(int index, String name, int width)
        {
            TableColumn tc = new TableColumn(index, width);
            tc.setHeaderValue(name);
            //            columnCount++;
            columnModel.addColumn(tc);
        }
        
	public Class getColumnClass(int col) {
	    if (col == COL_NAME) return CabinetEntry.class;
	    if (col == COL_SIZE) return Integer.class;	        // this will cause align-right
	    if (col == COL_MODTIME) return Date.class;
 	    return super.getColumnClass(col);
	}

        public int 	getColumnCount() { return 0;/*columnCount;*/ }
        public int 	getRowCount() { return entries.size(); }
        public Object 	getValueAt(int row, int col)
	{
	    CabinetEntry ce = (CabinetEntry) entries.get(row);
            try {
                return getColumnData(ce, col);
            } catch (FilingException e) {
                return e;
            }
	}
	
	private Object getColumnData(CabinetEntry ce, int col)
            throws FilingException
	{
	    boolean isBS = ce.isByteStore();
	    boolean isCAB = ce.isCabinet();
	    ByteStore bs = isBS ? (ByteStore) ce : null;
	    Object data = null;

	    try {
		if (col ==  COL_NAME) {
                    data = ce;
                } else if (col == COL_SIZE) {
		    if (isBS)
                        data = new Long(bs.length());
                    else
                        data = "";
                } else if (col == COL_MODTIME) {
                    if (isBS)
                        data = new Date(bs.getLastModifiedTime());
                    else
                        data = "";
                } else if (col == COL_PERMISSIONS) {
		    String s = " ";
                    s += isCAB ? 'd' : '-';
		    s += ce.canRead() ? 'r' : '-';
		    s += ce.canWrite() ? 'w' : '-';
		    data = s;
                } else if (col == COL_OWNER) {
		    if (isBS) data = bs.getOwner();
		}
	    } catch (Exception e) {
		errout(e.toString());
		data = "<" + e.getMessage() + ">";
	    }
	    return data == null ? "-" : data;
	}

	public void loadModelFromCabinet(Cabinet c)
        {
            this.cabinet = c;
	    this.entries.clear();
	    Iterator i;
            try {
                i = this.cabinet.entries();
            } catch (FilingException e) {
                errout(e);
                return;
            }
	    while (i.hasNext()) {
		CabinetEntry ce = (CabinetEntry) i.next();
		entries.add(ce);
		debug("CabinetTableModel added " + ce);
	    }
            fireTableDataChanged();
	}

        public void refresh()
        {
            if (cabinet instanceof Refreshable) {
                try {
                    ((Refreshable)cabinet).refresh();
                } catch (Exception e) {
                    errout(e);
                }
            }
            loadModelFromCabinet(cabinet);
        }

    };
    //------------------------------------------------------------------
    // end of inner classes
    //------------------------------------------------------------------

    private static void out(String s) { System.out.print(s); }
    private static void outln(String s) { System.out.println("CabViewer: " + s); }
    private static void errout(String s) { System.err.println("CabViewer: " + s); }
    private static void errout(Exception e)
    {
        if (e instanceof org.okip.service.shared.api.Exception)
            ((org.okip.service.shared.api.Exception)e).printChainedTrace();
        else {
            errout(e.toString());
            e.printStackTrace();
        }
    }
    static void debug(String s) { if (A_DEBUG) System.out.println("CabViewer: " + s); }
    static boolean A_DEBUG = false;
    static boolean A_VERBOSE = false;
    public static void main(String args[])
    {
        CabUtil.parseCommandLine(args);

        ArrayList dirs = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-debug")) {
                A_DEBUG = !A_DEBUG;
                if (A_DEBUG)
                    A_VERBOSE = true;
            } else if (a.equals("-verbose"))
                A_VERBOSE = !A_VERBOSE;
            else if (!a.startsWith("-"))
                dirs.add(a);
        }
        
        try {
            CabViewer cv = new CabViewer();
            if (dirs.size() == 0)
                dirs.add(".");
            int found = 0;
            Iterator i = dirs.iterator();
            while (i.hasNext()) {
                Cabinet cabinet = CabUtil.getCabinetFromDirectory((String)i.next());
                if (cabinet != null) {
                    cv.addRootCabinet(cabinet);
                    found++;
                }
            }
            if (found <= 0)
                System.exit(1);
            cv.expandRootEntry(0);    // open the 1st cabinet
            cv.setVisible(true);
        } catch (Exception e) {
            errout(e);
            System.exit(2);
        }
    }
}
