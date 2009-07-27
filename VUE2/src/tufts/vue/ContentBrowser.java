package tufts.vue;

import java.awt.LayoutManager;

import javax.swing.JPanel;

public abstract class ContentBrowser extends JPanel {
	public static final long	serialVersionUID = 1;

	public ContentBrowser() {
		super();
	}

	public ContentBrowser(LayoutManager layout) {
		super(layout);
	}

	public ContentBrowser(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}

	public ContentBrowser(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}
}
