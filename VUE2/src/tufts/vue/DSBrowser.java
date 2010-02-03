/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;

import edu.tufts.vue.dsm.DataSourceManager;

import tufts.vue.DataSourceViewer.MiscActionMouseListener;
import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
import tufts.vue.ui.AssociationsPane;


public class DSBrowser extends ContentBrowser {
	public static final long	serialVersionUID = 1;
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DSBrowser.class);

	public static DockWindow			dockWindow = null;
	protected Widget					librariesPane = new Widget(VueResources.getString("dockWindow.contentPanel.datasets.title"));
	protected AssociationsPane			associationsPane = new AssociationsPane();
	protected Widget					browsePane = new Widget(VueResources.getString("button.browse.label"));
	protected DataSetViewer				dataSetViewer = new DataSetViewer(this);


	public DSBrowser(DockWindow dw) {
		super("DSBrowser");

		dockWindow = dw;

		setName(VueResources.getString("dockWindow.contentPanel.datasets.title"));
		dataSetViewer.setName("Data Set Viewer");

		librariesPane.add(dataSetViewer);
		librariesPane.revalidate();	// Necessary for dataSetViewer names to be rendered;  not sure why.

		addPane(librariesPane, 0f);
		addPane(associationsPane, 0f);
		addPane(browsePane, 1f);

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
	}

	public DataSetViewer getDataSetViewer()
	{
		return dataSetViewer;
	}

	public void finalize() {
		librariesPane = null;
		associationsPane = null;
		browsePane = null;
		dataSetViewer = null;
	}


	public DataSource addDataset() {
		XmlDataSource	ds = new XmlDataSource("", null);

		try {
			String			xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>RSS Url</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>1000</maxChars><ui>8</ui></field></configuration>";
			String			name = ds.getDisplayName();
			String			address = ds.getAddress();

			xml = xml.replaceFirst("DEFAULT_NAME", (name != null ? name : ""));
			xml = xml.replaceFirst("DEFAULT_ADDRESS", (address != null ? address : ""));

			edu.tufts.vue.ui.ConfigurationUI cui =
						new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));

			cui.setPreferredSize(new Dimension(350, (int)cui.getPreferredSize().getHeight()));

			if (VueUtil.option(this,
					cui,
					VueResources.getString("datasourcehandler.adddataset"),
					javax.swing.JOptionPane.DEFAULT_OPTION,
					javax.swing.JOptionPane.PLAIN_MESSAGE,
					new Object[] {
						VueResources.getString("optiondialog.configuration.continue"), VueResources.getString("optiondialog.configuration.cancel")
					},
					VueResources.getString("optiondialog.configuration.continue")) == 1) {
				// Cancelled
				ds = null;
			} else {
				java.util.Properties	p = cui.getProperties();
				BrowseDataSource		bds = (BrowseDataSource)ds;

				bds.setDisplayName(p.getProperty("name"));
				bds.setAddress(p.getProperty("address"));
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return ds;
	}


	class MiscActionMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			addLibraryAction.actionPerformed(null);
		}
	}


	protected void refreshMenuActions() {
		boolean		datasetSelected = (dataSetViewer.dataSourceList.getSelectedValue() != null);

		reloadLibraryAction.setEnabled(datasetSelected);
		removeLibraryAction.setEnabled(datasetSelected);
		editLibraryAction.setEnabled(datasetSelected);

		Widget.setMenuActions(librariesPane,
				new Action[] {
			addLibraryAction,
			null,
			editLibraryAction,
			reloadLibraryAction,
			removeLibraryAction
		});
	}


	public AbstractAction addLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.adddataset")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			DataSource ds = addDataset();

			if (ds != null) {
				dataSetViewer.dataSourceList.addOrdered(ds);
				dataSetViewer.setActiveDataSource(ds);

				GUI.invokeAfterAWT(new Runnable() {
					public void run() {
						try {
							DataSetViewer.saveDataSetViewer();
						} catch (Throwable t) {
							tufts.Util.printStackTrace(t);
						}
					}
				});
			}
		}
	};


	public AbstractAction editLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.aboutthisdataset")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			DataSource ds = (DataSource)DataSetViewer.dataSourceList.getSelectedValue();

			dataSetViewer.displayEditOrInfo(ds);
		}
	};


	public AbstractAction reloadLibraryAction = new AbstractAction(VueResources.getString("datasourcehandler.reloaddataset")) {
		public static final long	serialVersionUID = 1;
		public void actionPerformed(ActionEvent event) {
			dataSetViewer.refreshBrowser();
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
						VueResources.getString("dataset.dialog.title"),
						javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
					DataSetViewer.dataSourceList.getModelContents().removeElement(ds);

					GUI.invokeAfterAWT(new Runnable() {
						public void run() {
							try {
								DataSetViewer.saveDataSetViewer();
							} catch (Throwable t) {
								tufts.Util.printStackTrace(t);
							}
						}
					});

					browsePane.remove(ds.getResourceViewer());
					browsePane.revalidate();
					browsePane.repaint();
				}
			}
		}
	};
}
