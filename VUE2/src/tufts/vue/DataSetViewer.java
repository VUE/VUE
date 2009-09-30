package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tufts.Util;
import tufts.vue.DataSource;
import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;


public class DataSetViewer extends ContentViewer {
	public static final long			serialVersionUID = 1;
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataSetViewer.class);

	protected DSBrowser					DSB = null;
	protected static DataSourceList		dataSourceList = null;
	private static Object				activeDataSource = null;
	protected JPopupMenu				contextMenu = null;

	public DataSetViewer(DSBrowser dsb) {
		DSB = dsb;

		dataSourceList = new DataSourceList(this);
		loadDataSets();
		addListeners();

		setLayout(new BorderLayout());
		add(dataSourceList);

		if (editInfoDockWindow == null) {
			initUI();
		}

//		editInfoDockWindow.setLocation(DSB.dockWindow.getX() + DSB.dockWindow.getWidth(), DSB.dockWindow.getY());
	}


	public void finalize() {
		DSB = null;
		browserDS = null;
		dataSourceList = null;
	}


	protected void loadDataSets() {
		File file = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));

		if (!file.exists()) {
			if (DEBUG.DR) System.out.println("Loading Datasets (does not exist: " + file + ")");
		} else {
			try {
				SaveDataSourceViewer		dataSourceViewer = unMarshallMap(file);
				Vector<BrowseDataSource>	dataSources = dataSourceViewer.getSaveDataSources();

				while (!dataSources.isEmpty()) {
					BrowseDataSource	ds = (BrowseDataSource)dataSources.remove(0);

					// Only show XML data sources (this includes CSV files).
					if (ds.getTypeName().equals(XmlDataSource.TYPE_NAME)) {
						dataSourceList.addOrdered(ds);
					}
				}

				// select the first new data set, if any
				DefaultListModel	model = dataSourceList.getModelContents();

//				if (activeDataSource == null && model.size() > 0)
//					setActiveDataSource((DataSource)model.getElementAt(0));
			} catch (Exception ex) {
				Log.error("Loading Datasets: ", ex);
			}
		}
	}


	private void addListeners() {
		dataSourceList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (DEBUG.KEYS || DEBUG.EVENTS) Log.debug("valueChanged: " + e);

				Object obj = ((JList)e.getSource()).getSelectedValue();

				if (obj !=null) {
					if (obj instanceof tufts.vue.DataSource) {
						DataSource ds = (DataSource)obj;
						setActiveDataSource(ds);
						refreshEditInfo(ds);
					} else {
						obj = dataSourceList.getModelContents().getElementAt(((JList)e.getSource()).getSelectedIndex() - 1);
						if (obj instanceof tufts.vue.DataSource) {
							DataSource ds = (DataSource)obj;
							setActiveDataSource(ds);
							refreshEditInfo(ds);
						}
					}
				}

				DSB.refreshMenuActions();
			}
		});

		dataSourceList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				int			index = dataSourceList.locationToIndex(event.getPoint());
				DataSource	ds = (DataSource)dataSourceList.getModel().getElementAt(index);

				if (event.getClickCount() == 2) {
						displayEditOrInfo(ds);
				} else {
					if (event.getButton() == MouseEvent.BUTTON3) {
//						lastMouseClick = e.getPoint();

						setActiveDataSource(ds);
						displayContextMenu(event);
					}
				}
			}
		});
	}


	public void setActiveDataSource(final DataSource ds)
	{
		if (DEBUG.Enabled) Log.debug("setActiveDataSource: " + ds);

		if (activeDataSource != ds) {
			activeDataSource = ds;
			dataSourceList.setSelectedValue(ds, true);
		}

		browserDS = (tufts.vue.BrowseDataSource) ds;
		displayInBrowsePane(produceViewer(browserDS), true);
	}


	void refreshBrowser()
	{
		if (browserDS == null || browserDS.isLoading())
			return;

		browserDS.unloadViewer();
		dataSourceList.repaint(); // so change in loaded status will be visible

		displayInBrowsePane(produceViewer(browserDS), false);
	}

	protected void displayInBrowsePane(JComponent viewer, boolean priority)
	{
		if (DEBUG.Enabled) Log.debug("displayInBrowsePane: " + browserDS + "; " + GUI.name(viewer));

		String title = VueResources.getString("button.browse.label")+": " + browserDS.getDisplayName();
		if (browserDS.getCount() > 0)
			title += " (" + browserDS.getCount() + ")";
		Widget.setTitle(DSB.browsePane, title);

		DSB.browsePane.removeAll();
		DSB.browsePane.add(viewer);
		DSB.browsePane.revalidate();
		DSB.browsePane.repaint();

		Widget.setExpanded(DSB.browsePane, true);

		DSB.refreshMenuActions();
	}


	protected void displayContextMenu(MouseEvent event) {
		getPopup(event).show(event.getComponent(), event.getX(), event.getY());
	}


	protected JPopupMenu getPopup(MouseEvent e) 
	{
		if (contextMenu == null)
		{
			contextMenu = new JPopupMenu();

			contextMenu.add(DSB.editLibraryAction);
			contextMenu.add(DSB.reloadLibraryAction);
			contextMenu.addSeparator();
			contextMenu.add(DSB.removeLibraryAction);
		}

		return contextMenu;
	}


	protected void repaintList() {
		dataSourceList.repaint(); // so change in loaded status will be visible
	}


	public static DataSourceList getDataSetList()
	{
		return dataSourceList;
	}
	public static void saveDataSetViewer() {
		if (dataSourceList == null) {
			System.err.println("DataSetViewer: No dataSourceList to save.");
		} else {
			int size = dataSourceList.getModel().getSize();
			Vector dataSources = new Vector();

			if (DEBUG.DR) Log.debug("saveDataSetViewer: found " + size + " dataSets: scanning for local's to save...");

			for (int i = 0; i<size; i++) {
				Object item = dataSourceList.getModel().getElementAt(i);

				if (DEBUG.DR) System.err.print("\tsaveDataSetViewer: item " + i + " is " + tufts.Util.tag(item) + "[" + item + "]...");

				if (item instanceof DataSource) {
					dataSources.add((DataSource)item);

					if (DEBUG.DR) System.err.println("saving");
				} else {
					if (DEBUG.DR) System.err.println("skipping");
				}
			}

			// For backwards compatability, "default" data source like My Computer and Saved Content are also saved to this XML file.
			// These data sources are displayed in DataSourceViewer, not DataSetViewer, so get them from there.
			if (DataSourceViewer.dataSourceList !=null)
			{
					size = DataSourceViewer.dataSourceList.getModel().getSize();

			for (int i = 0; i<size; i++) {
				Object item = DataSourceViewer.dataSourceList.getModel().getElementAt(i);

				if (DEBUG.DR) System.err.print("\tsaveDataSetViewer: item " + i + " is " + tufts.Util.tag(item) + "[" + item + "]...");

				if (item instanceof DataSource) {
					dataSources.add((DataSource)item);

					if (DEBUG.DR) System.err.println("saving");
				} else {
					if (DEBUG.DR) System.err.println("skipping");
				}
			}
			}
			try {
				if (DEBUG.DR) Log.debug("saveDataSetViewer: creating new SaveDataSourceViewer");

				File file = new File(VueUtil.getDefaultUserFolder().getAbsolutePath() + File.separatorChar + VueResources.getString("save.datasources"));
				SaveDataSourceViewer sViewer= new SaveDataSourceViewer(dataSources);

				if (DEBUG.DR) Log.debug("saveDataSourceViewer: marshallMap: saving " + sViewer + " to " + file);

				marshallMap(file,sViewer);

				if (DEBUG.DR) Log.debug("saveDataSourceViewer: saved");
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	private static DockWindow editInfoDockWindow; // hack for now: need this set before DSV is created
	private static WidgetStack editInfoStack; // static hack: is needed before this class is constructed
	private Object loadedDataSource;

	public void displayEditOrInfo(DataSource ds) {
		if (DEBUG.DR) Log.debug("DISPLAY " + Util.tags(ds));
		if (!editInfoDockWindow.isVisible())
			positionEditInfoWindow();
		refreshEditInfo(ds, true);
		editInfoDockWindow.setVisible(true);
		editInfoDockWindow.raise();
	}

	static void initUI() {
		editInfoDockWindow = buildConfigWindow();
	}

	private void positionEditInfoWindow()
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		if ((DSB.dockWindow.getX() + DSB.dockWindow.getWidth() + editInfoDockWindow.getWidth()) < screenSize.getWidth())
			editInfoDockWindow.setLocation(DSB.dockWindow.getX() + DSB.dockWindow.getWidth(), DSB.dockWindow.getY());
		else
			editInfoDockWindow.setLocation(DSB.dockWindow.getX() - editInfoDockWindow.getWidth(), DSB.dockWindow.getY());
	}

	private void refreshEditInfo(tufts.vue.DataSource ds) {
		refreshEditInfo(ds, false);
	}

	private void refreshEditInfo(tufts.vue.DataSource ds, boolean force) {
		if (ds == loadedDataSource)
			return;

		if (DEBUG.DR && DEBUG.META) Log.debug("refresh " + Util.tags(ds));

		if (force || editInfoDockWindow.isVisible()) {

			if (DEBUG.DR) Log.debug("REFRESH " + Util.tags(ds));

			editInfoStack.removeAll();

			final String name;
			if (DEBUG.Enabled)
				name = VueResources.getString("optiondialog.configuration.message")+": " + ds.getClass().getName();
			else
				name = VueResources.getString("optiondialog.configuration.message")+": " + ds.getTypeName();

			editInfoStack.addPane(name, new EditLibraryPanel(this, ds), 1f);

			doLoad(ds, ds.getDisplayName());
		}
	}

	private void doLoad(Object dataSource, String name) {
		editInfoStack.setTitleItem(name);
		//editInfoDockWindow.invalidate();
		//editInfoDockWindow.repaint();
		loadedDataSource = dataSource;
	}

	private static DockWindow buildConfigWindow() {
		try {
			return _buildWindow();
		} catch (Throwable t) {
			Log.error("buildConfigWindow", t);
		}
		return null;
	}

	private static DockWindow _buildWindow() {

		final DockWindow dw = GUI.createDockWindow(VueResources.getString("dockWindow.dataset.title"));

		editInfoStack = new WidgetStack();
		//editInfoStack.addPane("startup", new javax.swing.JLabel("config init"));
		editInfoStack.setMinimumSize(new Dimension(300,300));
		dw.setContent(editInfoStack);

		if (DEBUG.Enabled) {
			//editInfoStack.setMinimumSize(new Dimension(400,600));
			dw.setSize(500,800);
		} else {
			dw.setWidth(300);
			dw.setHeight(500);
		}

		// We don't have DSB yet to set location.
		return dw;
	}
}
