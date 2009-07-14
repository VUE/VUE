package tufts.vue;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.tufts.vue.ontology.ui.OntologyBrowser;

import tufts.vue.gui.DockWindow;
//import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;

public class ContentBrowser extends JPanel {
	public static final long	serialVersionUID = 1;
	JTabbedPane					tabbedPane = new JTabbedPane();
	JPanel						resources = VUE.getContentDock().getContentPanel();
	WidgetStack					datasets = new WidgetStack(VueResources.getString("dockWindow.contentBrowser.datasets.title"));
	JPanel						ontologies = OntologyBrowser.getBrowser().getDockWindow().getContentPanel();

	public ContentBrowser(DockWindow dw) {
		tabbedPane.addTab(VueResources.getString("dockWindow.contentBrowser.resources.title"), resources);
		tabbedPane.addTab(VueResources.getString("dockWindow.contentBrowser.datasets.title"), datasets);
		tabbedPane.addTab(VueResources.getString("dockWindow.contentBrowser.ontologies.title"), ontologies);

		dw.setContent(tabbedPane);

		validate();
		setVisible(true);
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
