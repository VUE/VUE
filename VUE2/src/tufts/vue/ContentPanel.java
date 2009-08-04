package tufts.vue;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.tufts.vue.ontology.ui.OntologyBrowser;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;

public class ContentPanel extends JPanel {
	public static final long	serialVersionUID = 1;
	JTabbedPane					tabbedPane = new JTabbedPane();
	JPanel						resources = VUE.getDRBrowserDock().getContentPanel();  // VUE.getDRBrowserDock() returns the DockWindow containing the DRBrowser.
	JPanel						datasets = new DSBrowser().getDockWindow().getContentPanel();
	JPanel						ontologies = OntologyBrowser.getBrowser().getDockWindow().getContentPanel();

	public ContentPanel(DockWindow dockWindow) {
		if (!VUE.isApplet())
			tabbedPane.addTab(VueResources.getString("dockWindow.contentPanel.resources.title"), resources);
		tabbedPane.addTab(VueResources.getString("dockWindow.contentPanel.datasets.title"), datasets);
		tabbedPane.addTab(VueResources.getString("dockWindow.contentPanel.ontologies.title"), ontologies);

		dockWindow.setContent(tabbedPane);
	}

	public void finalize() {
		resources = null;
		datasets = null;
		ontologies = null;
		tabbedPane = null;
	}

	public void showResourcesTab() {
		tabbedPane.setSelectedComponent(resources);
	}

	public void showDatasetsTab() {
		tabbedPane.setSelectedComponent(datasets);
	}

	public void showOntologiesTab() {
		tabbedPane.setSelectedComponent(ontologies);
	}
}
