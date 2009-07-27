package tufts.vue;

import java.awt.BorderLayout;

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

		associationsPane.setActions();	// Must happen AFTER Widget is added to WidgetStack.

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
}
