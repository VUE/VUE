package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;

import edu.tufts.vue.dsm.DataSourceManager;

import tufts.vue.DataSourceViewer.MiscActionMouseListener;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
import tufts.vue.ui.AssociationsPane;


public class DSBrowser extends ContentBrowser {
	public static final long	serialVersionUID = 1;
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DSBrowser.class);

	protected Widget					librariesPane = new Widget(VueResources.getString("dockWindow.contentPanel.datasets.title"));
	protected AssociationsPane			associationsPane = new AssociationsPane();
	protected Widget					browsePane = new Widget(VueResources.getString("button.browse.label"));
	protected WidgetStack				widgetStack = new WidgetStack(VueResources.getString("dockWindow.contentPanel.title"));
	protected DataSetViewer				dataSetViewer = new DataSetViewer(this);
	protected DockWindow				dockWindow = null;


	public DSBrowser() {
		super(new BorderLayout());
		setName(VueResources.getString("dockWindow.contentPanel.title"));

		librariesPane.add(dataSetViewer);
		librariesPane.revalidate();	// Necessary for DSV names to be rendered;  not sure why.

		widgetStack.addPane(librariesPane, 0f);
		widgetStack.addPane(associationsPane, 0f);
		widgetStack.addPane(browsePane, 1f);
		Widget.setWantsScroller(widgetStack, true);
		Widget.setWantsScrollerAlways(widgetStack, true);

		// The following must happen AFTER each Widget is added to the WidgetStack.
		associationsPane.setActions();	
		Widget.setHelpAction(librariesPane, VueResources.getString("dockWindow.Datasources.libraryPane.helpText"));
		// dockWindow.addButton is not a localized string in this context;  it's just a property that is later resolved.
		Widget.setMiscAction(librariesPane, new MiscActionMouseListener(), "dockWindow.addButton");
		Widget.setRefreshAction(browsePane, new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				dataSetViewer.refreshBrowser();
			}
		});

		refreshMenuActions();

		add(widgetStack);

		// Hack alert -- in order for the WidgetStack's scrollbar to appear when needed, it seems to be
		// necessary that the WidgetStack be contained in a DockWindow.  So the creator of a DSBrowser
		// should display not the DSBrowser, but the DSBrowser's getDockWindow().getContentPanel().
		dockWindow = GUI.createDockWindow(widgetStack);
	}


	public void finalize() {
		librariesPane = null;
		associationsPane = null;
		browsePane = null;
		widgetStack = null;
		dockWindow = null;
	}


	DockWindow getDockWindow() {
		return dockWindow;
	}


	class MiscActionMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			addLibraryAction.actionPerformed(null);
		}
	}


	protected void refreshMenuActions() {
		boolean		datasetSelected = (dataSetViewer.dataSourceList.getSelectedValue() != null);

		removeLibraryAction.setEnabled(datasetSelected);
		editLibraryAction.setEnabled(datasetSelected);

		Widget.setMenuActions(librariesPane,
				new Action[] {
			addLibraryAction,
			null,
			editLibraryAction,
			removeLibraryAction
		});
	}


	public AbstractAction addLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.adddatasets")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			AddLibraryDialog addLibraryDialog = new AddLibraryDialog(dataSetViewer.dataSourceList);

			DataSource ds = addLibraryDialog.getOldDataSource();

			if (ds != null) {
				dataSetViewer.setActiveDataSource(ds);
			}

			GUI.invokeAfterAWT(new Runnable() { public void run() {
//				queryEditor.refresh();

				try {
					DataSourceManager dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();

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
	};


	public AbstractAction editLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.aboutthisdataset")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			DataSource ds = (DataSource)DataSetViewer.dataSourceList.getSelectedValue();

			dataSetViewer.displayEditOrInfo(ds);
		}
	};


	public AbstractAction removeLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.deletedataset")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			Object obj = DataSetViewer.dataSourceList.getSelectedValue();

			if (obj != null && obj instanceof DataSource) {
				DataSource ds = (DataSource) obj;
				String displayName = ds.getDisplayName();

				if (VueUtil.confirm(VUE.getDialogParent(),
						String.format(Locale.getDefault(), VueResources.getString("datasource.dialog.message"), displayName),
						VueResources.getString("datasource.dialog.title"),
						javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
					DataSetViewer.dataSourceList.getModelContents().removeElement(ds);
					DataSetViewer.saveDataSetViewer();
				}

				browsePane.remove(ds.getResourceViewer());
				browsePane.revalidate();
				browsePane.repaint();
			}
		}
	};
}
