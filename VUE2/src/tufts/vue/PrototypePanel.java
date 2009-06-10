package tufts.vue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.WidgetStack;

public class PrototypePanel extends JPanel implements ActionListener, ChangeListener {
	public static final long		serialVersionUID = 1;
	protected static final boolean	DEBUG = false;
	protected static final int		HALF_GUTTER = 4,
									GUTTER = 2 * HALF_GUTTER;
	protected static JSlider		fadeSlider = null;
	protected static JButton		zoomSelButton = null,
									zoomMapButton = null;
	protected static JCheckBox		zoomLockCheckBox = null;
	protected static JLabel			fadeLabel = null,
									depthLabel = null,
									zoomSelLabel = null,
									zoomMapLabel = null;
	protected JPanel				fadePanel = null,
									fadeInnerPanel = null,
									zoomPanel = null,
									zoomInnerPanel = null,
									linePanel = null;
	protected WidgetStack			widgetStack = null;

	public PrototypePanel(DockWindow dw) {
		Insets						halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		fadeInnerPanel = new JPanel();
		fadeInnerPanel.setLayout(new GridBagLayout());

		fadeLabel = new JLabel(VueResources.getString("interactionTools.opacity"), SwingConstants.RIGHT);
		fadeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(fadeInnerPanel, fadeLabel, 0, 0, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		fadeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);

		JLabel						label100 = new JLabel(VueResources.getString("interactionTools.oneHundredPercent"));
		JLabel						label0 = new JLabel(VueResources.getString("interactionTools.zeroPercent"));
		Hashtable<Integer, JLabel>	labelTable = new Hashtable<Integer, JLabel>();

		label100.setFont(tufts.vue.gui.GUI.LabelFace);
		label0.setFont(tufts.vue.gui.GUI.LabelFace);
		labelTable.put(new Integer( 0 ), label100);
		labelTable.put(new Integer( 100 ), label0);

		fadeSlider.setLabelTable(labelTable);
		fadeSlider.setPaintLabels(true);
		fadeSlider.setPaintTicks(false);
		fadeSlider.setPreferredSize(new Dimension(130,35));
		fadeSlider.addChangeListener(this);
		fadeSlider.setToolTipText(VueResources.getString("interactionTools.opacity.toolTip"));
		addToGridBag(fadeInnerPanel, fadeSlider, 1, 0, 1, 1, halfGutterInsets);

		depthLabel = new JLabel(VueResources.getString("interactionTools.depth"), SwingConstants.RIGHT);
		depthLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(fadeInnerPanel, depthLabel, 0, 1, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		fadePanel = new JPanel();
		fadePanel.setLayout(new GridBagLayout());
		addToGridBag(fadePanel, fadeInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 1.0, 0.0, halfGutterInsets);

		zoomInnerPanel = new JPanel();
		zoomInnerPanel.setLayout(new GridBagLayout());

		zoomSelLabel = new JLabel(VueResources.getString("interactionTools.zoomSel.label"), SwingConstants.RIGHT);
		zoomSelLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(zoomInnerPanel, zoomSelLabel, 0, 0, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		zoomSelButton = new JButton();
		zoomSelButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomSelButton.setText(VueResources.getString("interactionTools.zoomSel"));
		zoomSelButton.setToolTipText(VueResources.getString("interactionTools.zoomSel.toolTip"));
		zoomSelButton.addActionListener(this);
		addToGridBag(zoomInnerPanel, zoomSelButton, 1, 0, 1, 1, halfGutterInsets);

		zoomMapLabel = new JLabel(VueResources.getString("interactionTools.zoomMap.label"), SwingConstants.RIGHT);
		zoomMapLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(zoomInnerPanel, zoomMapLabel, 0, 1, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		zoomMapButton = new JButton();
		zoomMapButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomMapButton.setText(VueResources.getString("interactionTools.zoomMap"));
		zoomMapButton.setToolTipText(VueResources.getString("interactionTools.zoomMap.toolTip"));
		zoomMapButton.addActionListener(this);
		addToGridBag(zoomInnerPanel, zoomMapButton, 1, 1, 1, 1, halfGutterInsets);

		linePanel = new JPanel() {
			public static final long		serialVersionUID = 1;
			protected void paintComponent(java.awt.Graphics g) {
				if (isOpaque()) {
					g.setColor(getBackground());
					g.fillRect(0, 0, getWidth(), getHeight());
				}

				g.setColor(java.awt.Color.DARK_GRAY);
				g.drawLine(0, 0, 0, getHeight());
				g.drawLine(0, getHeight() / 2, getWidth() - 1, getHeight() / 2);
			}
		};

		linePanel.setPreferredSize(new Dimension(2 * GUTTER, (2 * zoomMapButton.getPreferredSize().height) + GUTTER));
		linePanel.setMinimumSize(linePanel.getPreferredSize());
		addToGridBag(zoomInnerPanel, linePanel, 2, 0, 1, 2, halfGutterInsets);

		zoomLockCheckBox = new JCheckBox(VueResources.getString("interactionTools.auto"));
		zoomLockCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomLockCheckBox.setToolTipText(VueResources.getString("interactionTools.auto.toolTip"));
		zoomLockCheckBox.addChangeListener(this);
		addToGridBag(zoomInnerPanel, zoomLockCheckBox, 3, 0, 1, 2, halfGutterInsets);

		zoomPanel = new JPanel();
		zoomPanel.setLayout(new GridBagLayout());
		addToGridBag(zoomPanel, zoomInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 1.0, 0.0, halfGutterInsets);

		widgetStack = new WidgetStack(VueResources.getString("dockWindow.interactionTools.title"));
		widgetStack.addPane(VueResources.getString("interactionTools.fadeAndDepthWidget"), fadePanel);
		widgetStack.addPane(VueResources.getString("interactionTools.zoomWidget"), zoomPanel);
		dw.setContent(widgetStack);

		validate();

		if (DEBUG) {
			fadeSlider.setBackground(Color.CYAN);
			fadeSlider.setOpaque(true);
			zoomSelButton.setBackground(Color.CYAN);
			zoomSelButton.setOpaque(true);
			zoomMapButton.setBackground(Color.CYAN);
			zoomMapButton.setOpaque(true);
			zoomLockCheckBox.setBackground(Color.CYAN);
			zoomLockCheckBox.setOpaque(true);
			fadeLabel.setBackground(Color.CYAN);
			fadeLabel.setOpaque(true);
			depthLabel.setBackground(Color.CYAN);
			depthLabel.setOpaque(true);
			zoomSelLabel.setBackground(Color.CYAN);
			zoomSelLabel.setOpaque(true);
			zoomMapLabel.setBackground(Color.CYAN);
			zoomMapLabel.setOpaque(true);
			fadePanel.setBackground(Color.YELLOW);
			fadePanel.setOpaque(true);
			fadeInnerPanel.setBackground(Color.MAGENTA);
			fadeInnerPanel.setOpaque(true);
			zoomPanel.setBackground(Color.YELLOW);
			zoomPanel.setOpaque(true);
			zoomInnerPanel.setBackground(Color.MAGENTA);
			zoomInnerPanel.setOpaque(true);
			linePanel.setBackground(Color.CYAN);
			linePanel.setOpaque(true);
		}

		setVisible(true);
	}

	public void finalize() {
		fadeSlider = null;
		zoomSelButton = null;
		zoomMapButton = null;
		zoomLockCheckBox = null;
		fadeLabel = null;
		depthLabel = null;
		zoomSelLabel = null;
		zoomMapLabel = null;
		fadePanel = null;
		fadeInnerPanel = null;
		zoomPanel = null;
		zoomInnerPanel = null;
		linePanel = null;
		widgetStack = null;
}

	public static void zoomIfLocked() {
		if (zoomLockCheckBox.isSelected()) {
			zoom();
		}
	}

	public static void zoom() {
		LWSelection	selection = VUE.getSelection();

		if (selection.size() == 0) {
			ZoomTool.setZoomFit();
		} else {
			ZoomTool.setZoomFitRegion(VUE.getActiveViewer(), selection.getBounds(), 16, false);
		}
	}

	public static double getAlpha() {
		return (VUE.getSelection().size() == 0 ? 1.0 : 1.0 - (((double)fadeSlider.getValue()) / 100.0));
	}

	/* ActionListener method -- button has been clicked */
	public void actionPerformed(ActionEvent event) {
		Object	source = event.getSource();

		if (source == zoomSelButton) {
			ZoomTool.setZoomFitRegion(VUE.getActiveViewer(), VUE.getSelection().getBounds(), 16, false);
		} else if (source == zoomMapButton) {
			ZoomTool.setZoomFit();
		}
	}

	/* ChangeListener method -- checkbox has been clicked */
	public void stateChanged(ChangeEvent event) {
		Object	source = event.getSource();

		if (source == zoomLockCheckBox) {
			zoomIfLocked();
		} else if (source == fadeSlider) {
			VUE.getActiveViewer().repaint();
		}
	}

	/* Static methods */

	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			Insets insets)
		{
			GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
					0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets, 0, 0) ;


			((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
			container.add(component);
		}

	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor,
			Insets insets)
	{
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				0.0, 0.0, anchor, GridBagConstraints.NONE, insets, 0, 0) ;


		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}

	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor, int fill, double weightX, double weightY,
			Insets insets)
	{
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				weightX, weightY, anchor, fill, insets, 0, 0) ;


		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}

	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor, int fill, double weightX, double weightY,
			Insets insets, int padX, int padY)
		{
			GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
					weightX, weightY, anchor, fill, insets, padX, padY) ;


			((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
			container.add(component);
		}
}
