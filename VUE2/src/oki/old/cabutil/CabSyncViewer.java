//
//  CabSyncViewer.java
//  S.Fraize July 2002

// TODO: when autosync is enabled, trigger a sync.  (Could trigger a refresh,
// but that only detects state *changes*, so if it's already aware of
// stuff that's out of sync, the next refresh won't actually trigger
// a sync).

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import org.okip.service.filing.api.*;

public class CabSyncViewer extends JFrame
{
    static Logger log = new Logger(CabSyncViewer.class);

    JTree jtree;
    DragTable localTable;
    DragTable remoteTable;
    JLabel statusBar;
    SyncList syncList;
    JLabel leftLabel = new JLabel("lv", SwingConstants.CENTER);
    JLabel rightLabel = new JLabel("rv", SwingConstants.CENTER);

    SyncListTCModel leftTCModel;
    SyncListTCModel rightTCModel;

    boolean showGhosts = false;

    private static final Cursor dragCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private static final Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    /*
     * DragTable is designed to have EXACTLY TWO INSTANCES.
     * This is for a hacked-up drag&drop implementation
     * to get around JTable mouse grabs, and until we
     * could do a nice D&D with the new stuff in Java 1.4.
     */
    static boolean dragging = false;
    static DragTable firstTable;
    class DragTable extends JTable
    {
        DragTable otherTable = null;

        boolean containsMouse = false;
        
        public DragTable(TableModel dm, TableColumnModel cm)
        {
            super(dm, cm);
            //defaultCursor = getCursor();
            if (firstTable == null) {
                firstTable = this;
            } else {
                otherTable = firstTable;
                firstTable.otherTable = this;
            }
        }

        //todo: we also need to be able to drop into the table container!
        // (in case there's, say, 3 items in the table, and then mostly
        // white space below which currently won't be a drop target!)
        protected void processMouseEvent(MouseEvent e)
        {
            if (log.d) log.debug("processMouseEvent: " + e.paramString());
            int id = e.getID();
            
            if (id == MouseEvent.MOUSE_ENTERED) {
                containsMouse = true;
            } else if (id == MouseEvent.MOUSE_EXITED) {
                containsMouse = false;
            } else if (id == MouseEvent.MOUSE_PRESSED) {
                int r = rowAtPoint(e.getPoint());
                // if we've pressed the mouse within the selection 
                // zone, start dragging...
                if (r >= getSelectionModel().getMinSelectionIndex() &&
                    r <= getSelectionModel().getMaxSelectionIndex())
                    startDragging();
                else
                    super.processMouseEvent(e);
            } else if (dragging && id == MouseEvent.MOUSE_RELEASED) {
                if (otherTable.containsMouse) {
                    if (log.v) log.verbose("Drag SUCCEDED");
                    exportSelected(this);
                }
                else {
                    if (log.v) log.verbose("Drag ABORTED");
                }
                stopDragging();
                super.processMouseEvent(e);
            } else
                super.processMouseEvent(e);
            
        }

        protected void processMouseMotionEvent(MouseEvent e)
        {
            int id = e.getID();
            if (dragging && id == MouseEvent.MOUSE_DRAGGED) {
                if (log.d) log.debug("drag");
            } else
                super.processMouseMotionEvent(e);
        }

        void startDragging()
        {
            dragging = true;
            if (log.d) log.debug("DRAG START");
            getParent().setCursor(dragCursor);
        }
        void stopDragging()
        {
            dragging = false;
            if (log.d) log.debug("DRAG STOP");
            getParent().setCursor(defaultCursor);
        }
        
    }
    
    class SyncListTreeNode extends DefaultMutableTreeNode
    {
        // a tree node capable of creating a data-model based
        // on its recalled object.
        
        // a TreeNode, initialzed with some object that can generate a
        // list of items that can be fed to a given factory to
        // produduce an AbstractTableModel of some kind.  Could use
        // this generic call with CabViewer if we used a
        // TableModelFactory and an additional name factory to be used
        // in toString() (to ge ta displayable name from the user
        // object); OR, if it was passed in as an argument.
        
	SyncListTableModel dataModel;
	SyncListTableModel remoteDataModel;
	String displayName;
	
	SyncListTreeNode(SyncList sl)
	{
	    super(sl);
            if (log.d) log.debug(this, "new with " + sl);
	}
        
        public void setDisplayName(String name)
        {
            this.displayName = name;
        }

        private SyncList getSyncList()
        {
            return (SyncList) getUserObject();
        }
        private SyncList getRemoteSyncList()
        {
            return getSyncList().getRemoteList();
        }

	SyncListTableModel getDataModel()
	{
	    if (this.dataModel == null) {
                if (log.d) log.debug(this, "getDataModel: creating SyncListTableModel for " + getSyncList());
                this.dataModel = new SyncListTableModel(getSyncList());//todo: propigate log bits
                buildTree();
            }
	    return this.dataModel;
	}
        
	SyncListTableModel getRemoteDataModel()
	{
	    if (this.remoteDataModel == null) {
                if (log.d) log.debug(this, "getRemoteDataModel: creating SyncListTableModel for " + getRemoteSyncList());
                this.remoteDataModel = new SyncListTableModel(getRemoteSyncList());//todo: propigate log bits
            }
	    return this.remoteDataModel;
	}

        private void buildTree()
        {
            Iterator i = getSyncList().iterator();
            while (i.hasNext()) {
                SyncEntry se = (SyncEntry) i.next();
                CabinetEntry ce = se.getCabinetEntry();
                //if (log.d) log.debug("getDataModel scanning " + se);
                //if (se instanceof SyncList) {
                if (ce instanceof Cabinet) {
                    Cabinet c = (Cabinet) ce;
                    if (log.d) log.debug("will want new SyncListTreeNode for " + se);
                    //this.add(new SyncListTreeNode((SyncList)se);
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
                // won't work once we're recursing...
		return getUserObject().toString();
	    } catch (Exception e) {
		return getUserObject().getClass().toString();
	    }
	}
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer
    {
	public BooleanRenderer() {
	    super();
	    setHorizontalAlignment(JLabel.CENTER);
            //setRequestFocusEnabled(false);
	}

        public Component getTableCellRendererComponent(JTable table, Object value,
						       boolean isSelected, boolean hasFocus, int row, int column) {
	    if (isSelected) {
	        setForeground(table.getSelectionForeground());
	        super.setBackground(table.getSelectionBackground());
	    }
	    else {
	        setForeground(table.getForeground());
	        setBackground(table.getBackground());
	    }
            if (value != null) {
                if (value instanceof Boolean)
                    setSelected(((Boolean)value).booleanValue());
                else
                    log.errout("NOT BOOLEAN: " + value.getClass().getName() + ": " + value);
            }
            return this;
        }
    }

    /*
     * The main renderer
     */
    final static int COL_NAME = 0;
    final static int COL_SIZE = 1;
    final static int COL_MODTIME = 2;
    final static int COL_STATUS = 3;
    final static int COL_DOSYNC = 4;
    final static int COL_OWNER = 5;

    static DateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");//fix: this ignores locale

    class SyncEntryRenderer extends DefaultTableCellRenderer
    {
        int col;

        public SyncEntryRenderer(int index)
        {
            col = index;
            if (col == COL_MODTIME || col == COL_SIZE)
                setHorizontalAlignment(SwingConstants.RIGHT);
        }
        
        private boolean looping = false;
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (value == null)
                return super.getTableCellRendererComponent(table, "<null>", isSelected, hasFocus, row, column);
            
            SyncEntry se = (SyncEntry) value;
            // if a file goes missing, sometimes value doesn't get set below before
            // going on to call super.getTableCellRendererComponent (why?) so
            // we make sure it doesn't call an innapropraite toString() on a
            // SyncEntry by immediately setting value to something innocous
            // here.
            value = "-";
            boolean isGhost = se.isGhost();
	    boolean isBS = se.isByteStore();
	    ByteStore bs = isBS ? se.getByteStore() : null;
            //CabinetEntry ce = se.getCabinetEntry();
            
            if (isGhost)
                setForeground(ghostColor);
            else
                setForeground(defaultColor);
            
            try {
                if (col == COL_NAME) {
                    String label = se.getName();
                    if (se.isCabinet())
                        label += "/";
                    if (!isGhost) {
                        //if (syncList.isImportAll() || se.isSyncRequested())
                        // add above condition if you only want to see the
                        // status color of items explicitly marked for syncing.
                            setForeground(getStatusColor(se));
                    }
                    value = label;
                } else if (col == COL_MODTIME) {
                    //if (isGhost) value = ""; else
                    if (isBS) {
                        long time;
                        try {
                            time = bs.getLastModifiedTime();
                            value = dateFormatter.format(new Date(time));
                        } catch (FilingException e) {
                            // if a file is deleted out from under us,
                            // we start getting an exception here -- so
                            // we attempt a single refresh if we detect this.
                            // todo: this is still a bit dangerous -- we
                            // should really set a bit and find a control
                            // point with a larger granularity.  Or perhaps
                            // we could initiate the refresh only if we
                            // verified that the bytestore no longer exists.
                            if (!looping && !syncList.isSyncRunning()) {
                                looping = true;
                                if (log.d) log.debug(this, "loop-protected syncList refresh");
                                try {
                                    syncList.refresh();
                                    // todo: only call on the LOCAL list,
                                    // and let it master the process?
                                } catch (Exception ex) {
                                    looping = false;
                                    throw ex;
                                }
                                looping = false;
                            } else
                                //throw e;
                                value="-";
                        }
                    }
                    else
                        value = "";
                } else if (col == COL_SIZE) {
                    //if (isGhost) value = ""; else
                    if (isBS)
                        value = new Long(bs.length());
                    else
                        value = "";
                } else if (col == COL_STATUS)
                    value = " " + se.getStatusDescription();
                // default: leave value alone
            } catch (Exception e) {
                log.errout(e, "rendering");
                value = "<"+e.getMessage()+">";
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    };

    class SyncListTCModel extends DefaultTableColumnModel
    {
        TableColumn syncColumn;
        boolean syncColumnShowing = false;
        
        SyncListTCModel(boolean local)
        {
            createColumns(local);
        }

        private void createColumns(boolean local)
        {
            addTableColumn(COL_NAME, "Entry Name", 800);
            addTableColumn(COL_SIZE, "Size", 350);
            addTableColumn(COL_MODTIME, "Modified", 700);
            
            if (local) {
                addTableColumn(COL_STATUS, "Status", 400);
                syncColumn = addTableColumn(COL_DOSYNC, "Sync", 150);
                syncColumnShowing = true;
                syncColumn.setCellRenderer(new BooleanRenderer());
            }

            //addTableColumn(COL_OWNER, "Owner", 500);
        }

        public boolean isSyncShowing()
        {
            return syncColumnShowing;
        }

        TableColumn addTableColumn(int index, String name, int width)
        {
            TableColumn tc = new TableColumn(index, width);
            // could also initialize the TableColumn here with renderers instead of
            // relying on getColumnClass to map to them.  That actually feels a bit cleaner.
            tc.setHeaderValue(name);
            tc.setCellRenderer(new SyncEntryRenderer(index));
            addColumn(tc);
            return tc;
        }
        
        public void showSyncColumn(boolean showit)
        {
            if (showit) {
                if (!syncColumnShowing) {
                    addColumn(syncColumn);
                    syncColumnShowing = true;
                }
            } else if (syncColumnShowing) {
                removeColumn(syncColumn);
                syncColumnShowing = false;
            }
        }
    }
    
    class SyncListTableModel extends AbstractTableModel
                                     implements ChangeListener
    {
        SyncList syncList;
	ArrayList entries;
	
        SyncListTableModel(SyncList sl)
	{
            if (log.d) log.debug(this, "new with " + sl);
	    this.entries = new ArrayList();
	    this.refreshModel(sl);
	}

        public void stateChanged(ChangeEvent e)
        {
            if (log.d) log.debug(this, "stateChanged: " + e);
            if (e instanceof SyncListChangeEvent) {
                SyncListChangeEvent se = (SyncListChangeEvent) e;
                if (se.changes > 0)
                    refreshModel();
                else // se.updates>0
                    fireTableDataChanged();
                if (se.tosync > 0 && !syncList.isSyncRunning() && syncList.isAutoSync()) {
                    if (log.v) log.verbose(se + ": initiating auto-synchronization");
                    doAutoSync();
                }
            }
            else if (e.getSource() instanceof SyncEntry) {
                //currently, we get the syncentry that changed
                // if one did durning a synchronization
                fireTableDataChanged();
            }
            else
                refreshModel();
        }
        
        SyncListTCModel columnModel;
        
        public SyncListTCModel getTableColumnModel()
        {
            if (columnModel == null)
                columnModel = new SyncListTCModel(true);
            return columnModel;
        }

        public boolean isCellEditable(int rIndex, int cIndex) {
            if (cIndex == COL_DOSYNC) {
                SyncEntry se = getEntryAt(rIndex);
                if (se.isGhost())
                    return syncList.isImportAll() == false;
                return true;
            }
            return false;
        }        
        public void setValueAt(Object aValue, int rIndex, int cIndex)
        {
            //if (log.d) log.debug("setValueAt r="+rIndex + " c="+cIndex + " " + aValue);
            SyncEntry se = getEntryAt(rIndex);
            if (se == null)
                log.errout("setValueAt, no SyncEntry at row " + rIndex);
            else
                syncList.setSyncRequested(se, ((Boolean)aValue).booleanValue());
        }
        
        /* This is mainly being ignored because we use our own
         * renderer now, tho it's still needed for the Boolean
         * editor, as we don't define our own editors.
         */
	public Class getColumnClass(int col) {
	    if (col == COL_DOSYNC) return Boolean.class;        // required for editing
	    //if (col == COL_NAME) return SyncEntry.class;
	    //if (col == COL_SIZE) return Integer.class;	        // this will cause align-right
	    //if (col == COL_MODTIME) return Date.class;
 	    return super.getColumnClass(col);
	}

        // since we're using a TableColumnModel, nobody should be calling getColumnCount.
        public int 	getColumnCount() { return 0; }
        //        public int 	getColumnCount() { throw new RuntimeException("getColumnCount:where was TableColumnModel?"); }
        public int 	getRowCount() { return entries.size(); }
        public Object 	getValueAt(int row, int col)
	{
	    SyncEntry se = getEntryAt(row);

            if (se == null) {
                //if (log.d) log.debug("getValueAt: no entry at row " + row);
                return null;
            }
            
            try {
                return getColumnData(se, col);
            } catch (FilingException e) {
                log.errout(e);
                return e.getMessage();
            }
	}

        private SyncEntry getEntryAt(int row)
        {
            try {
                return (SyncEntry) entries.get(row);
            } catch (java.lang.IndexOutOfBoundsException e) {
                // got this once I think in a race-condition
                return null;
            }
        }

	
        // This almost always proudces just a SyncEntry
        // now that we handle evertyhing in the renderer
        // (because it needs all that information).  When
        // we implement sorting, we may need to move some
        // of the raw data production back down here.
	private Object getColumnData(SyncEntry se, int col)
            throws FilingException
	{
	    Object data = null;

	    try {
                if (col == COL_DOSYNC) {
                    if (syncList.isImportAll())
                        data = Boolean.TRUE;
                    else
                        data = new Boolean(se.isSyncRequested());
                } else
                    data = se;
	    } catch (Exception e) {
		log.errout(e);
		data = "<" + e.getMessage() + ">";
	    }
	    return data == null ? "-" : data;
	}

	public void refreshModel()
        {
            refreshModel(this.syncList);
        }
        
        // note that ultimately this will be a SORTED
        // list, so we do need to keep our own array lst.
        public void refreshModel(SyncList sl)
        {
            /*
             * if there are any additions or deletions to
             * the SyncList, we need to reflect those in our
             * arraylist.  As this shouldn't happen often
             * (when sombody is modifying a cabinet out from
             * under you) we just rebuild the whole list.
             */
            if (log.d) log.debug(this, "refreshModel: " + sl);
	    this.entries.clear();
            Iterator i = sl.iterator();
            while (i.hasNext()) {
                SyncEntry se = (SyncEntry) i.next();
                if (showGhosts || !se.isGhost())
                    entries.add(se);
	    }
            setSyncList(sl);
            // todo.perf: only fire if something actually changed...
            if (log.d) log.debug(this, "refreshModel: fireTableDataChanged " + sl);
            fireTableDataChanged();
	}
        
        private void setSyncList(SyncList sl)
        {
            if (syncList != sl) {
                if (syncList != null)
                    syncList.removeChangeListener(this);
                syncList = sl;
                syncList.addChangeListener(this);
            }
        }

    };
        
    //------------------------------------------------------------------
    // CabSyncViewer
    //------------------------------------------------------------------

    CSAction aRefresh = new CSAction("Refresh");
    CSAction aSync = new CSAction("Synchronize");
    CSAction aCancel = new CSAction("Cancel");
    CSAction aAutoSync = new CSAction("AutoSync");
    
    AbstractButton tbAutoSync = new JCheckBox(aAutoSync);
    AbstractButton tbRefresh = new JButton(aRefresh);

    // file menu actions
    CSAction aOpen = new CSAction("Open...");
    CSAction aQuit = new CSAction("Quit");

    // edit menu actions
    CSAction aCopy = new CSAction("Copy");
    CSAction aPaste = new CSAction("Paste");
    CSAction aClear = new CSAction("Clear");
    CSAction aSelectAll = new CSAction("Select All");
    CSAction aSyncSelected = new CSAction("Sync Selected");
    CSAction aMarkSync = new CSAction("Mark for Sync");
    CSAction aUnmarkSync = new CSAction("Unmark for Sync");

    JCheckBox leftImportAll = new JCheckBox("Sync All");
    JCheckBox rightImportAll = new JCheckBox("Import All");
    CSAction aUploadOnly = new CSAction("Upload ->");
    CSAction aDownloadOnly = new CSAction("<- Download");

    protected JMenu editMenu = new JMenu("Edit");
    protected JMenuItem miCopy;
    protected JMenuItem miPaste;
    protected JMenuItem miClear;
    protected JMenuItem miSelectAll;

    CSAction defaultActionHandler = new CSAction("default");

    
    public CabSyncViewer(SyncList syncList)
    {
        super("OKI Cabinet Synchronizer");
        if (log.d) log.debug("new with " + syncList);

        Cabinet cabinet = syncList.getLocalCabinet();
        this.syncList = syncList;
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(new Dimension(1024,640));
        this.setLocation(100,100);
        this.addMenus();
        this.setVisible(true);// show something right away
        
        SyncListTreeNode rootNode;
	rootNode = new SyncListTreeNode(syncList);

        String pathName;
        try {
            pathName = cabinet.getPath();
        } catch (FilingException e) {
            log.errout(e);
            pathName = "<" + e.getMessage() + ">";
        }
        rootNode.setDisplayName(pathName);
        
        /*
         * create the JTree
         */
	
        //TODO: don't show the JTree unless the cabinets
        // you're syncing have subdirectories.
        this.jtree = new JTree(rootNode, true);
        addJTreeListeners();
        
        /*
         * create the local JTable -- and we need a table-column model for that
         */
        Color gridColor = new Color(224,224,224);
        // todo: cleanup getting the data model
        // ALSO: set up updaters for remote list -- we're not seeing changes..
        SyncListTableModel dataModel = (SyncListTableModel) rootNode.getDataModel();
        leftTCModel = new SyncListTCModel(true);
        //leftTCModel.setColumnMargin(10);// this makes for ugly breaks in line-selection indicator tho...
        this.localTable = new DragTable(dataModel, leftTCModel);
        this.localTable.setGridColor(gridColor);
        //addTableRenderers(localTable);

        /*
         * create the remote JTable
         */

        dataModel = rootNode.getRemoteDataModel();
        rightTCModel = new SyncListTCModel(false);
        this.remoteTable = new DragTable(dataModel, rightTCModel);
        this.remoteTable.setGridColor(gridColor);
        //addTableRenderers(remoteTable);
        
        /*
         * set up the two tables with scrollers & in a splitPane
         */
        JSplitPane tableSplit = new JSplitPane();
        final JScrollPane leftScroller = new JScrollPane(localTable);
        final JScrollPane rightScroller = new JScrollPane(remoteTable);
        final Container leftView = new Container();
        final Container rightView = new Container();

        //        setLeftView(syncList, null);
        
        // todo: okay, make a class that handles all the left/right common
        // stuff -- really a sync list viewer.
        // TODO: if you ever go back to single-pane view (local list only)
        // you'd need an import all AND an export all to enable
        // full sync functionality!
        JPanel leftBar = new JPanel();
        leftBar.setLayout(new BoxLayout(leftBar, BoxLayout.X_AXIS));
        leftImportAll.addActionListener(defaultActionHandler);
        leftImportAll.setRequestFocusEnabled(false);
        leftBar.add(Box.createHorizontalGlue());
        leftBar.add(leftLabel);
        leftBar.add(Box.createHorizontalGlue());
        leftBar.add(leftImportAll);
        leftBar.add(Box.createHorizontalGlue());
        JButton leb = new JButton(aUploadOnly);
        leb.setForeground(darkGreen);
        leb.setRequestFocusEnabled(false);
        leftBar.add(leb);
        leftBar.add(Box.createHorizontalGlue());
        
        JPanel rightBar = new JPanel();
        rightBar.setLayout(new BoxLayout(rightBar, BoxLayout.X_AXIS));
        rightImportAll.addActionListener(defaultActionHandler);
        rightImportAll.setRequestFocusEnabled(false);
        rightBar.add(Box.createHorizontalGlue());
        JButton b = new JButton(aDownloadOnly);
        b.setForeground(darkGreen); 
        b.setRequestFocusEnabled(false);
        rightBar.add(b);
        rightBar.add(Box.createHorizontalGlue());
        //rightBar.add(rightImportAll);
        rightBar.add(Box.createHorizontalGlue());
        rightBar.add(rightLabel);
        rightBar.add(Box.createHorizontalGlue());

        leftView.setLayout(new BorderLayout());
        leftView.add(leftBar, BorderLayout.NORTH);
        leftView.add(leftScroller, BorderLayout.CENTER);

        //        setRightView(syncList, null);
        rightView.setLayout(new BorderLayout());
        rightView.add(rightBar, BorderLayout.NORTH);
        rightView.add(rightScroller, BorderLayout.CENTER);

        tableSplit.setLeftComponent(leftView);
        tableSplit.setRightComponent(rightView);
        tableSplit.setOneTouchExpandable(true);
        tableSplit.setResizeWeight(0.7);

        MouseInputAdapter mouseListener = new MouseInputAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (log.d) log.debug("mouseClicked (clicks= "+ e.getClickCount() + ") on " + e.getComponent());
            }
            
            public void mousePressed(MouseEvent e)
            {
                if (log.d) log.debug("mousePressed " + e);
                Component c = e.getComponent();
                // clicking anywhere in the container NOT
                // on the table is a short-cut for de-select
                // all.  E.g., clicking on the title (the
                // name of the cabinet)
                if (c != remoteTable)
                    remoteTable.clearSelection();
                if (c != localTable)
                    localTable.clearSelection();
                    
                if (e.getComponent() == leftView) {
                    localTable.requestFocus();
                } else if (e.getComponent() == rightView) {
                    remoteTable.requestFocus();
                }
            }

                // Part of our drag&drop hack...
                public void mouseEntered(MouseEvent e) {
                    if (e.getComponent() == leftView)
                        localTable.containsMouse = true;
                    else if (e.getComponent() == rightView)
                        remoteTable.containsMouse = true;
                }
                    
                public void mouseExited(MouseEvent e) {
                    if (e.getComponent() == leftView)
                        localTable.containsMouse = false;
                    else if (e.getComponent() == rightView)
                        remoteTable.containsMouse = false;
                }
                
        };
            
        leftView.addMouseListener(mouseListener);
        rightView.addMouseListener(mouseListener);
        localTable.addMouseListener(mouseListener);
        remoteTable.addMouseListener(mouseListener);
        
        setView(syncList);


        /*
         * create the tree/table(s) splitpane & subcomponents with scrollers
         */

if (false){        
        JSplitPane splitPane = new JSplitPane();
        JScrollPane treeScroller = new JScrollPane(jtree);
        splitPane.setResizeWeight(0.25); // 25% space to the left component
splitPane.setResizeWeight(0.0);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        //splitPane.setLeftComponent(treeScroller);
splitPane.setLeftComponent(new Label(""));
        splitPane.setRightComponent(tableSplit);
splitPane.setDividerLocation(0);// start collapsed for now
}

        /*
         * layout all the application components
         */

        statusBar = new JLabel("OKI Cabinet Synchronization demo.");
        statusBar.setBorder(new EmptyBorder(1, 4, 2, 0));//too crowded by default

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(getToolBar(), BorderLayout.NORTH);
        //        pane.add(splitPane, BorderLayout.CENTER);
        pane.add(tableSplit, BorderLayout.CENTER);
        pane.add(statusBar, BorderLayout.SOUTH);

        setSyncAllEnabled(syncList.isImportAll());

        this.constructing = false;
    }
    
    private String abbrevName(String s)
    {
        final int max = 30;
        String base = null;
        int i;
        if ((i=s.indexOf(':')) > 0) {
            base = s.substring(0, i);
            s = s.substring(i);
        }
        if (s.length() > max)
            s = "..." + s.substring(s.length()-max);
        if (base == null)
            return s;
        else
            return base + s;
    }
    public void setLeftView(SyncList sl, AbstractTableModel tm)
    {
        leftLabel.setText(abbrevName(sl.getLocalCabinetName()));
        //+ "<->" + sl.getRemoteCabinetName());
        if (tm != null)
            localTable.setModel(tm);
    }
    public void setRightView(SyncList sl, AbstractTableModel tm)
    {
        rightLabel.setText(abbrevName(sl.getLocalCabinetName()));
        if (tm != null)
            remoteTable.setModel(tm);
    }

    public void setView(SyncListTreeNode syncNode)
    {
        setView(syncNode.getSyncList());
    }
    
    //this method will make more sense when we re-introduce the tree
    public void setView(SyncList sl)
    {
        setLeftView(sl, null);
        setRightView(sl.getRemoteList(), null);
        //todo: put back when tree up and running
        //setLeftView(sl, new SyncListTableModel(sl));
        //setRightView(sl.getRemoteList(), new SyncListTableModel(sl.getRemoteList()));
    }
    protected void addJTreeListeners()
    {
	jtree.addTreeSelectionListener
	    (new TreeSelectionListener() {
		    public void valueChanged(TreeSelectionEvent e)
		    {
			SyncListTreeNode syncNode = (SyncListTreeNode) jtree.getLastSelectedPathComponent();
			if (syncNode == null) return;
			if (log.d) log.debug("tree node selected: " + syncNode);
                        setView(syncNode);
		    }
		});

        // could put these on the the class instead of inline
        jtree.addTreeWillExpandListener
           (new TreeWillExpandListener() {
                   public void treeWillExpand(TreeExpansionEvent e)
                   {
                        TreePath path = e.getPath();
                        if (log.d) log.debug("tree expanding at path: " + path);
                        SyncListTreeNode syncNode = (SyncListTreeNode) path.getLastPathComponent();
			if (syncNode == null) return;
			if (log.d) log.debug("tree expanding at node: " + syncNode);
                        jtree.setSelectionPath(path);
                        setView(syncNode);
                   }
                   public void treeWillCollapse(TreeExpansionEvent e) {}
               });

    }

    static final Color darkGreen = new Color(0, 128, 0);
    static final Color ghostColor = Color.gray;
    static final Color defaultColor = Color.black;
// changing font is not working...
//    static final Font defaultFont = new Font("serif", Font.PLAIN, 14);
//    static final Font ghostFont = new Font("serif", Font.ITALIC, 10);
    //static final Color statusColors[] = { Color.black, Color.black, darkGreen };
    private static Color getStatusColor(SyncEntry se)
    {
        switch (se.getStatus()) {
        case SyncList.STATUS_NEW_REMOTE: return ghostColor;
        case SyncList.STATUS_CHANGED_REMOTE: return Color.red;
        case SyncList.STATUS_NEW_LOCAL:
        case SyncList.STATUS_CHANGED_LOCAL:
            return darkGreen;
        case SyncList.STATUS_CONFLICT: return Color.orange;
        }
        return defaultColor;
    }

    public void setMessage(String s)
    {
        statusBar.setText(s);
    }
    public void progressMessage(String s)
    {
        statusBar.setText(s + "...");
    }
    public void clearMessage()
    {
        statusBar.setText("");
    }
        
    
    private boolean constructing = true;
    private Font startupMessageFont = new Font("serif", Font.ITALIC+Font.BOLD, 48);
    public void paint(Graphics g)
    {
        super.paint(g);
        if (constructing) {
            g.setColor(Color.lightGray);
            g.setFont(startupMessageFont);
            g.drawString("OKI FILING DEMO", getWidth()/4, getHeight()/2);
        }
    }
    
    protected void processEvent(AWTEvent e)
    {
        if (log.d) log.debug(e.toString());
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
        
    class CSAction extends AbstractAction {
        //todo: use this or lose it
        public CSAction(String name)
        {
            super(name);
        }

        public String getActionCommand()
        {
            return (String) getValue(Action.NAME);
        }
        private boolean actionMatch(ActionEvent ae, CSAction a)
        {
            if (a != null)
                return ae.getActionCommand().equals(a.getActionCommand());
            return false;
        }
        
        public void actionPerformed(ActionEvent ae) {
            if (log.d) log.debug(ae.getActionCommand() + " ActionEvent=" + ae.getSource());
            // handle the menu items
            if (actionMatch(ae, aCopy)) doCopy();
            else if (actionMatch(ae, aPaste)) doPaste();
            else if (actionMatch(ae, aClear)) doClear();
            else if (actionMatch(ae, aSelectAll)) doSelectAll();
            else if (actionMatch(ae, aAutoSync)) syncList.setAutoSync(tbAutoSync.isSelected());
            else if (actionMatch(ae, aSyncSelected)) doSyncSelected();
            else if (actionMatch(ae, aMarkSync)) doMarkSelectionForSync(Boolean.TRUE);
            else if (actionMatch(ae, aUnmarkSync)) doMarkSelectionForSync(Boolean.FALSE);
            // handle the tool-bar buttons
            else if (actionMatch(ae, aRefresh)) {
                if (ae.getSource() == tbRefresh)
                    syncList.refresh(ae.getModifiers() != 0);//modifiers is always 0 no matter what keys we hold!
                else
                    tbRefresh.doClick();
            }
            else if (actionMatch(ae, aUploadOnly)) doAsyncSync(SyncList.SYNC_UPLOADS_ONLY);
            else if (actionMatch(ae, aDownloadOnly)) doAsyncSync(SyncList.SYNC_DOWNLOADS_ONLY);
            else if (actionMatch(ae, aSync)) doAsyncSync(SyncList.SYNC_ALL);
            else if (actionMatch(ae, aCancel)) doCancel();
            else if (actionMatch(ae, aQuit)) doQuit();
            else if (ae.getSource() == leftImportAll) {
                setSyncAllEnabled(leftImportAll.isSelected());
            } else if (ae.getSource() == rightImportAll) {
                syncList.getRemoteList().setImportAll(leftImportAll.isSelected());
            }
            else {
                //if (log.v) log.verbose("unhandled action: " + ae.getActionCommand());
                if (log.v) log.verbose("unhandled action: " + ae);
            }
        }
    }

    private synchronized void setSyncAllEnabled(boolean importAll)
    {
        syncList.setImportAll(importAll);
        leftTCModel.showSyncColumn(!importAll);
        aMarkSync.setEnabled(!importAll);
        aUnmarkSync.setEnabled(!importAll);
        //aSyncSelected.setEnabled(!importAll);
        leftImportAll.setSelected(importAll);
    }

    public void setPollInterval(int seconds)
    {
        syncList.setPollInterval(seconds);
        if (syncList.getPollInterval() <= 0)
            aAutoSync.setEnabled(false);
        else
            aAutoSync.setEnabled(true);
    }
    
    Container toolBar;

    //    AbstractButton tbPoll = new JRadioButton("AutoRefresh");
    private Component getToolBar()
    {
        if (toolBar != null)
            return toolBar;

        toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.add(Box.createHorizontalGlue());

        //JButton tbSync = new JButton(new ImageIcon("images/sync.gif", "Synchronize"));

        addToolButton(tbRefresh);
//         JButton b = new JButton(aUploadOnly);
//         b.setForeground(darkGreen);
//         addToolButton(b);
        addToolButton(new JButton(aSync));
//         b = new JButton(aDownloadOnly);
//         b.setForeground(darkGreen);
//         addToolButton(b);
        addToolButton(new JButton(aCancel));
        aCancel.setEnabled(false);

        //toolBar.addSeparator();
        //tbAutoSync.setForeground(Color.blue);
        
        toolBar.add(Box.createHorizontalGlue());
        addToolButton(new JButton(aSyncSelected));
        
        toolBar.add(Box.createHorizontalGlue());
        addToolButton(tbAutoSync);
        toolBar.add(Box.createHorizontalGlue());

        //tbSync.setBackground(Color.white);
        //toolBar.add(new JLabel(new ImageIcon("images/sync.gif")));
        //toolBar.add(new JLabel("Synchronize", new ImageIcon("images/sync.gif"), JLabel.CENTER));

        return toolBar;
    }

    private void addToolButton(AbstractButton b)
    {
        //toolBar.addSeparator();
        if (b instanceof JComponent) {
            // tab-selected items (those with default input focus)
            // in mac LAF look gross when 'selected' -- this
            // doesn't let them get the default selection.
            ((JComponent)b).setRequestFocusEnabled(false);
        }
        toolBar.add(b);
    }
    
    public void addMenus() {
        addFileMenuItems();
        addEditMenuItems();
        //mainMenuBar.add(Box.createHorizontalGlue());
        //mainMenuBar.add(new JLabel(syncList.getLocalCabinet().toString(), JLabel.RIGHT));

//         JButton btnSync = null;//new JButton("Synchronize");
//         if (btnSync != null) {
//             mainMenuBar.add(btnSync);
//             //btnSync.addActionListener(this);
//             btnSync.setDefaultCapable(false);//drawn border gets messed up in apple LAF
//         }
        
        setJMenuBar(mainMenuBar);
    }


	
    public void addFileMenuItems()
    {
        getFileMenu();
        
        //todo: change from meta to ctrl if it's a PC? META accelerators don't
        // seem to be working on my Java 1.4 Win2k box.

        fileMenu.add(new JMenuItem(aOpen)).
            setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.META_MASK));
		
        fileMenu.add(new JMenuItem(aRefresh)).
            setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.META_MASK));
		
        fileMenu.add(new JMenuItem(aSync)).
            setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.META_MASK));
        
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(aQuit)).
            setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.META_MASK));
        
        getMainMenuBar().add(fileMenu);
    }
	
	
    public void addEditMenuItems()
    {
        miCopy = new JMenuItem(aCopy);
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.META_MASK));
        editMenu.add(miCopy);

        miPaste = new JMenuItem(aPaste);
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.META_MASK));
        editMenu.add(miPaste).setEnabled(false);

        miClear = new JMenuItem(aClear);
        editMenu.add(miClear);
        miClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.META_MASK));
        //why neither of these working?
        //miClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        //miClear.setMnemonic(KeyEvent.VK_ESCAPE);
        editMenu.addSeparator();

        miSelectAll = new JMenuItem(aSelectAll);
        miSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.META_MASK));
        editMenu.add(miSelectAll);
        editMenu.addSeparator();

        editMenu.add(new JMenuItem(aSyncSelected));
        editMenu.add(new JMenuItem(aMarkSync))
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.META_MASK));
        editMenu.add(new JMenuItem(aUnmarkSync))
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.META_MASK));

        getMainMenuBar().add(editMenu);
    }
    
    private int doSyncType = SyncList.SYNC_ALL;
    private Throwable syncException = null;
    private Runnable syncThreadCode = new Runnable() {
            public void run() {
                if (log.d) log.debug("sync thread started");
                try {
                    doSync(doSyncType);
                } finally {
                    enableSyncButtons(true);
                }
                if (syncException != null) {
                    Throwable e = syncException;
                    syncException = null;
                    if (e instanceof LockException) {
                        // todo: embed lock data in exception and unpack it here for display,
                        // offering user option of breaking the lock.
                        LockException le = (LockException) e;
                        JOptionPane.showMessageDialog(null, e.getMessage() + "\nlock" + le.lock + "\nCurrent time: " + new Date());
                    } else if (e instanceof SyncException)
                        JOptionPane.showMessageDialog(null, "Synchronization error: " + e.getMessage());
                    else {
                        log.outln("displaying exception dialog: " + e);
                        // Bug: java 1.3.1 on Mac is Segmentation Faulting if the
                        // line-length in a string handed to showMessageDialog is too long...
                        JOptionPane.showMessageDialog(null, e.toString());
                    }
                }
                if (log.d) log.debug("sync thread complete");
            }};
    
    Thread syncThread;
    public synchronized void doAsyncSync(int syncType)
    {
        enableSyncButtons(false);
        try {
            doSyncType = syncType;
            syncThread = new Thread(syncThreadCode, "CabSyncViewer sync");
            syncThread.setPriority(Thread.MIN_PRIORITY);//give gui max priority
            syncThread.start();
        } catch (Exception e) {
            enableSyncButtons(true);
            log.errout(e);
        }
    }

    private void enableSyncButtons(boolean tv)
    {
        aRefresh.setEnabled(tv);
        aSync.setEnabled(tv);
        aUploadOnly.setEnabled(tv);
        aDownloadOnly.setEnabled(tv);
        aCancel.setEnabled(!tv);
        SwingUtilities.getRootPane(this).setCursor(tv ? defaultCursor : waitCursor);
    }
    

    public synchronized void doAutoSync()
    {
        if (syncList.isSyncRunning())
            return;
        aSync.setEnabled(false);
        try {
            doSync(SyncList.SYNC_ALL);
        } finally {
            aSync.setEnabled(true);
        }
    }

    public synchronized void doSync(int syncType)
    {
        if (log.d) log.debug("synchronizing");
//         progressMessage("Synchronizing "
//                         + syncList.getLocalCabinet() + " with "
//                         + syncList.getRemoteCabinet());
        
        try {
            syncList.doSynchronize(syncType);
        } catch (Exception e) {
            syncException = e;
            log.errout(e, "doSync");
        }
        // TODO: Don't set this message if we didn't do anything
        //setMessage("Synchronization complete at " + new Date());
        clearMessage();
    }
    
    public void doCancel()
    {
        syncList.requestSyncCancel();
        if (log.d) log.debug("doCancel");
        try {
            syncThread.join();
        } catch (InterruptedException e) {
            log.errout(e);
        }
    }
    
    public void doSyncSelected()
    {
        JTable table = getFocusedTable();
        syncSelected(table, false);
    }

    protected void exportSelected(JTable table)
    {
        syncSelected(table, true);
    }
    
    protected void syncSelected(JTable table, boolean exportsOnly)
    {
        boolean remoteSource = (table == remoteTable);
        boolean localSource = (table == localTable);
        int rows[] = table.getSelectedRows();
        if (log.d) log.debug("exportSelected " + rows.length + " rows");

        //TODO: disable buttons.
        TableModel tm = table.getModel();
        for (int i = 0; i < rows.length; i++) {
            SyncEntry se = (SyncEntry) tm.getValueAt(rows[i], COL_NAME);
            
            if (exportsOnly) {
                /*
                 * ONLY do "exports" -- copy out data from source
                 * So only do items that appear new in this table.
                 */
                if (! (se.getStatus() == SyncList.STATUS_NEW_LOCAL ||
                       se.getStatus() == SyncList.STATUS_CHANGED_LOCAL))
                    continue;
            }
                
            if (log.d) log.debug("export " + se);
            
            // if this is REMOTE list, need to get local match.
            // right now we're set up only to do remote list --
            // todo: CHANGE THAT.
            // todo: this is confusing: addRemote only for remote
            // sources?
            SyncEntry localEntry;
            if (remoteSource)
                localEntry = syncList.addRemote(se);
            else
                localEntry = se;
            
            int changes = 0;
                try {
                    if (syncList.doSynchronizeEntry(localEntry))
                        changes++;
                } catch (FilingException e) {
                    setMessage(e.toString());
                    log.errout(e);
                } finally {
                    // be sure to keep the properties up to date!
                    syncList.releaseLocks();
                    if (changes > 0)
                        syncList.saveProperties();
                }
        }
    }
    
    public void doMarkSelectionForSync(Boolean b)
    {
        JTable table = getFocusedTable();
        if (table != localTable || !leftTCModel.isSyncShowing())
            return;
        TableModel tm = table.getModel();
        int rows[] = getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            tm.setValueAt(b, rows[i], COL_DOSYNC);
        }
        // when using accelerators, the table doesn't
        // seem to update, so we tried firing this manually to be sure.
        // UNFORTUNATELY, this extra call also causes the
        // selection to clear!
        //        if (tm instanceof AbstractTableModel)
        //            ((AbstractTableModel)tm).fireTableDataChanged();
        // okay, repaint actually does the trick:
        table.repaint();
    }
    
    public void doCopy()
    {
        if (log.d) log.debug("copy row " + localTable.getSelectedRow());
        // determine active viewer, get all selected rows, get
        // SyncEntry's from the data model, and do our stuff.
    }
    public void doPaste() {}
    public void doQuit()
    {
        if (log.v) log.verbose("exiting");
        System.exit(0);
    }
    public void doClear()
    {
        if (log.d) log.debug("doClear");
        getFocusedTable().clearSelection();
    }
    public void doSelectAll()
    {
        if (log.d) log.debug("doSelectAll");
        getFocusedTable().selectAll();
    }

    protected JTable getFocusedTable()
    {
        Component fo = getFocusOwner();
        if (fo != null) {
            if (log.d) log.debug("focusOwner: " + fo.getClass().getName()
                      +", "+fo.getLocation()
                      +", "+fo.getSize());
            if (fo == remoteTable)
                return remoteTable;
        }
        return localTable;
    }
        

    protected int[] getSelectedRows()
    {
        int rows[] = getFocusedTable().getSelectedRows();

        if (log.d) {
            log.debug("Selected rows:");
            for (int i = 0; i < rows.length; i++)
                log.debug("r"+rows[i]);
        }
        return rows;
    }

    static boolean A_DEBUG = false;
    static boolean A_VERBOSE = false;
    static boolean A_TAKE_ACTION = true;
    static boolean A_SHOW_GHOSTS = false;
    
    public static final String USAGE = "Usage: CabSyncViewer <local-cabinet> <remote-cabinet> [-poll seconds] [-show_ghosts]";

    public static void main(String args[])
    {
        int pollInterval = 30;   // default seconds
        
        CabUtil.parseCommandLine(args);
        log.parseArgs(args);
        
        ArrayList dirs = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-poll")) {
                if (args.length > (i+1)) {
                    i++;
                    pollInterval = Integer.parseInt(args[i]);
                }
            }
            else if (a.equals("-help")) {
                System.out.println(USAGE);
                System.exit(0);
            }
            else if (a.equals("-show_ghosts"))
                A_SHOW_GHOSTS = true;
            //            else if (a.equals("-always_sync_all"))
                //todo
            else if (!a.startsWith("-"))
                dirs.add(a);
        }

        String dirLocal = "./c/l";
        String dirRemote = "./c/r";

        if (dirs.size() > 1) {
            dirLocal = (String) dirs.get(0);
            dirRemote = (String) dirs.get(1);
        } else {
            log.errout(USAGE);
            System.exit(1);
        }

        Cabinet cabinet0 = CabUtil.getCabinetFromDirectory(dirLocal);
        Cabinet cabinet1 = CabUtil.getCabinetFromDirectory(dirRemote);
        //CabUtil.printCabinet(cabinet0);
        //CabUtil.printCabinet(cabinet1);

        if (log.v) log.verbose("local=" + cabinet0 + ", remote=" + cabinet1);
        if (cabinet0 == null || cabinet1 == null) {
            log.errout("Couldn't open cabinet(s).");
            System.exit(1);
        }
        
        SyncList syncList = new SyncList(cabinet0, cabinet1, log);
        CabSyncViewer cv = new CabSyncViewer(syncList);
        cv.showGhosts = A_SHOW_GHOSTS;
        syncList.setVisibleGhosts(A_SHOW_GHOSTS);
        cv.setPollInterval(pollInterval);
        cv.setVisible(true);
    }
}

