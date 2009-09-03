package tufts.vue;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;

import edu.tufts.vue.ontology.ui.OntologyBrowser;

import tufts.vue.gui.DockWindow;

public class ContentPanel extends JPanel {
	public static final long	serialVersionUID = 1;
	JTabbedPane					tabbedPane = new JTabbedPane();
	DRBrowser					resources = null;
	DSBrowser					datasets = null;
	OntologyBrowser				ontologies = null;

	
	public ContentPanel(DockWindow dockWindow) {
		if (!VUE.isApplet()) {
			resources = new DRBrowser(true, dockWindow);
			addBrowser(VueResources.getString("dockWindow.contentPanel.resources.title"), resources);
		}

		datasets = new DSBrowser(dockWindow);
		addBrowser(VueResources.getString("dockWindow.contentPanel.datasets.title"), datasets);

		ontologies = OntologyBrowser.getBrowser();
		ontologies.initializeBrowser(false, null);
		addBrowser(VueResources.getString("dockWindow.contentPanel.ontologies.title"), ontologies);

		dockWindow.setContent(tabbedPane);

                if (DEBUG.Enabled) tabbedPane.setSelectedIndex(1);
	}


	public void finalize() {
		resources = null;
		datasets = null;
		ontologies = null;
		tabbedPane = null;
	}

	protected void addBrowser(String title, JPanel browser) {
		JScrollPane scrollPane = new JScrollPane(null, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(null);
		scrollPane.setName(title + ".dockScroll");
		scrollPane.setWheelScrollingEnabled(true);

		browser.putClientProperty("VUE.sizeTrack", scrollPane.getViewport());
        scrollPane.setViewportView(browser);

		tabbedPane.addTab(title, scrollPane);

		if (DEBUG.BOXES) scrollPane.setBorder(new LineBorder(Color.green, 4));
	}

	public void loadDataSourceViewer() {
		if (resources != null) {
			resources.loadDataSourceViewer();
		}
	}

	public DRBrowser getDRBrowser() {
		return resources;
	}

	public DSBrowser getDSBrowser() {
		return datasets;
	}

	public void showResourcesTab() {
		tabbedPane.setSelectedIndex(0);
	}

	public void showDatasetsTab() {
		if (VUE.isApplet())
			tabbedPane.setSelectedIndex(0);
		else
			tabbedPane.setSelectedIndex(1);
	}

	public void showOntologiesTab() {
		if (VUE.isApplet())
			tabbedPane.setSelectedIndex(1);
		else
			tabbedPane.setSelectedIndex(2);
	}
}
