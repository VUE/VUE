package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tufts.Util;
import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;


public class DataSetViewer extends ContentViewer {
	public static final long			serialVersionUID = 1;
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataSetViewer.class);

	protected DSBrowser					DSB = null;
	protected DataSourceList			dataSourceList = null;

	public DataSetViewer(DSBrowser dsb) {
		DSB = dsb;

		dataSourceList = new DataSourceList(this);
// should call super's loadBrowseableDataSources() but for now:
		loadDataSets();
		addListeners();

		setLayout(new BorderLayout());
		add(dataSourceList);
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
				SaveDataSourceViewer		dataSourceContainer = unMarshallMap(file);
				Vector<BrowseDataSource>	saveDataSources = dataSourceContainer.getSaveDataSources();
				String						XMLFeedTypeName = XmlDataSource.TYPE_NAME;

				while (!saveDataSources.isEmpty()) {
					BrowseDataSource	ds = (BrowseDataSource)saveDataSources.remove(0);

					// Only show XML data sources (this includes CSV files).
					if (ds.getTypeName().equals(XMLFeedTypeName)) {
						dataSourceList.addOrdered(ds);
					}
				}
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
//						refreshEditInfo(ds);
					} else {
						obj = dataSourceList.getModelContents().getElementAt(((JList)e.getSource()).getSelectedIndex() - 1);
						if (obj instanceof tufts.vue.DataSource) {
							DataSource ds = (DataSource)obj;
							setActiveDataSource(ds);
//							refreshEditInfo(ds);
						}
					}
				}
//				refreshMenuActions();
			}}
		);

/*		dataSourceList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (activeDataSource instanceof DataSource) {
						displayEditOrInfo((DataSource)activeDataSource);
					} else {
						displayEditOrInfo((edu.tufts.vue.dsm.DataSource)activeDataSource);
					}
				} else {
					Point pt = e.getPoint();
					if ( (activeDataSource instanceof edu.tufts.vue.dsm.DataSource) && (pt.x <= 40) ) {
						int index = dataSourceList.locationToIndex(pt);

						edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)
						dataSourceList.getModel().getElementAt(index);
						boolean included = !ds.isIncludedInSearch();
						if (DEBUG.DR) Log.debug("DataSource " + ds + " [" + ds.getProviderDisplayName() + "] inclusion: " + included);
						ds.setIncludedInSearch(included);
						dataSourceList.repaint();
						//queryEditor.refresh();

						GUI.invokeAfterAWT(new Runnable() { public void run() {
							queryEditor.refresh();
							try {
								synchronized (dataSourceManager) {
									if (DEBUG.DR) Log.debug("DataSourceManager saving...");
									dataSourceManager.save();
									if (DEBUG.DR) Log.debug("DataSourceManager saved.");
								}
							} catch (Throwable t) {
								tufts.Util.printStackTrace(t);
							}
						}});
					}
					if(e.getButton() == e.BUTTON3)
					{
						lastMouseClick = e.getPoint();
						displayContextMenu(e);
					}
					//popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});*/
	}


	public void setActiveDataSource(final tufts.vue.DataSource ds)
	{
		if (DEBUG.Enabled) Log.debug("setActiveDataSource: " + ds);

//		activeDataSource = ds;

		dataSourceList.setSelectedValue(ds, true);

		browserDS = (tufts.vue.BrowseDataSource) ds;

		displayInBrowsePane(produceViewer(browserDS), true);
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
	}


	protected void repaintList() {
		dataSourceList.repaint(); // so change in loaded status will be visible
	}
}
