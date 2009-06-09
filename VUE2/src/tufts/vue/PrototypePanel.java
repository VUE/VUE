package tufts.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;

public class PrototypePanel extends JPanel implements ChangeListener {
	public static final long		serialVersionUID = 1;
	protected static final boolean	DEBUG = false;
	protected static final int		HALF_GUTTER = 4;
	protected static JCheckBox		zoomLockCheckBox = null;
	protected static JButton		zoomSelButton = null,
									zoomMapButton = null;
	protected static JSlider		fadeSlider = null;
	protected static JLabel			fadeLabel = null;
	protected JPanel				zoomPanel = null,
									fadePanel = null;
    protected WidgetStack			widgetStack = null;

	public PrototypePanel(DockWindow dw) {
		GridBagConstraints	constraints = new GridBagConstraints();
		Insets				halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		setLayout(new GridBagLayout());

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0.0;
		constraints.weighty = 0.0;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.insets = halfGutterInsets;
		
		zoomPanel = new JPanel();
		zoomPanel.setLayout(new GridBagLayout());

		zoomSelButton = new JButton();
		zoomSelButton.setAction(tufts.vue.Actions.ZoomToSelection);
		zoomPanel.add(zoomSelButton, constraints);

		zoomMapButton = new JButton();
		zoomMapButton.setAction(tufts.vue.Actions.ZoomFit);
		constraints.gridy = 1;
		zoomPanel.add(zoomMapButton, constraints);

		zoomLockCheckBox = new JCheckBox(VueResources.getString("interactionTools.auto"));
		zoomLockCheckBox.addChangeListener(this);
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		zoomPanel.add(zoomLockCheckBox, constraints);

		fadePanel = new JPanel();
		fadePanel.setLayout(new GridBagLayout());

		fadeLabel = new JLabel(VueResources.getString("interactionTools.opacity"));

		constraints.gridx = 0;
		constraints.gridheight = 1;
		fadePanel.add(fadeLabel, constraints);

		fadeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);

		JLabel		label100 = new JLabel(VueResources.getString("interactionTools.oneHundredPercent"));
		JLabel		label0 = new JLabel(VueResources.getString("interactionTools.zeroPercent"));
		Hashtable	labelTable = new Hashtable();

		labelTable.put(new Integer( 0 ), label100);
		labelTable.put(new Integer( 100 ), label0);

		fadeSlider.setLabelTable(labelTable);
		fadeSlider.setPaintLabels(true);
		fadeSlider.setPaintTicks(false);
		fadeSlider.setPreferredSize(new Dimension(130,35));
		fadeSlider.addChangeListener(this);

		constraints.gridx = 1;
		fadePanel.add(fadeSlider, constraints);

		widgetStack = new WidgetStack(VueResources.getString("dockWindow.interactionTools.title"));
		widgetStack.addPane(VueResources.getString("interactionTools.fadeAndDepthWidget"), fadePanel);
		widgetStack.addPane(VueResources.getString("interactionTools.zoomWidget"), zoomPanel);
		dw.setContent(widgetStack);

		validate();

		if (DEBUG) {
			zoomLockCheckBox.setBackground(Color.BLUE);
			zoomLockCheckBox.setOpaque(true);
			zoomSelButton.setBackground(Color.BLUE);
			zoomSelButton.setOpaque(true);
			zoomMapButton.setBackground(Color.BLUE);
			zoomMapButton.setOpaque(true);
			fadeLabel.setBackground(Color.BLUE);
			fadeLabel.setOpaque(true);
			fadeSlider.setBackground(Color.BLUE);
			fadeSlider.setOpaque(true);
			zoomPanel.setBackground(Color.ORANGE);
			zoomPanel.setOpaque(true);
			fadePanel.setBackground(Color.ORANGE);
			fadePanel.setOpaque(true);
		}

		setVisible(true);
	}

	public void finalize() {
		zoomLockCheckBox = null;
		zoomSelButton = null;
		zoomMapButton = null;
		fadeSlider = null;
		fadeLabel = null;
		zoomPanel = null;
		fadePanel = null;
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

	/* ChangeListener method -- checkbox has been clicked */
	public void stateChanged(ChangeEvent event) {
		Object	source = event.getSource();

		if (source == zoomLockCheckBox) {
			zoomIfLocked();
		} else if (source == fadeSlider) {
			VUE.getActiveViewer().repaint();
		}
	}
}
