package tufts.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
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
	protected static JCheckBox		zoomLockCheckBox = null;
	protected static JButton		zoomSelButton = null,
									zoomMapButton = null;
	protected static JSlider		fadeSlider = null;
	protected static JLabel			fadeLabel = null,
									zoomSelLabel = null,
									zoomMapLabel = null;
	protected JPanel				zoomPanel = null,
									fadePanel = null,
									linePanel = null,
									emptyPanel1 = null,
									emptyPanel2 = null;
	protected WidgetStack			widgetStack = null;

	public PrototypePanel(DockWindow dw) {
		GridBagConstraints	constraints = new GridBagConstraints();
		Insets				halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

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
		zoomPanel.setBorder(BorderFactory.createEmptyBorder(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER));

		zoomSelLabel = new JLabel(VueResources.getString("interactionTools.zoomSel.label"), SwingConstants.RIGHT);
		zoomSelLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomPanel.add(zoomSelLabel, constraints);

		zoomSelButton = new JButton();
		zoomSelButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomSelButton.setText(VueResources.getString("interactionTools.zoomSel"));
		zoomSelButton.setToolTipText(VueResources.getString("interactionTools.zoomSel.toolTip"));
		zoomSelButton.addActionListener(this);
		constraints.gridx = 1;
		zoomPanel.add(zoomSelButton, constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		zoomMapLabel = new JLabel(VueResources.getString("interactionTools.zoomMap.label"), SwingConstants.RIGHT);
		zoomMapLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomPanel.add(zoomMapLabel, constraints);

		zoomMapButton = new JButton();
		zoomMapButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomMapButton.setText(VueResources.getString("interactionTools.zoomMap"));
		zoomMapButton.setToolTipText(VueResources.getString("interactionTools.zoomMap.toolTip"));
		zoomMapButton.addActionListener(this);
		constraints.gridx = 1;
		zoomPanel.add(zoomMapButton, constraints);

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

		linePanel.setPreferredSize(new Dimension(2 * GUTTER, (2 * zoomMapButton.getHeight()) + GUTTER));
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.fill = GridBagConstraints.VERTICAL;
		constraints.weighty = 1.0;
		zoomPanel.add(linePanel, constraints);

		zoomLockCheckBox = new JCheckBox(VueResources.getString("interactionTools.auto"));
		zoomLockCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomLockCheckBox.setToolTipText(VueResources.getString("interactionTools.auto.toolTip"));
		zoomLockCheckBox.addChangeListener(this);
		constraints.gridx = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0.0;
		constraints.weighty = 0.0;
		zoomPanel.add(zoomLockCheckBox, constraints);

		emptyPanel1 = new JPanel();
		constraints.gridx = 4;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		zoomPanel.add(emptyPanel1, constraints);

		fadePanel = new JPanel();
		fadePanel.setLayout(new GridBagLayout());
		fadePanel.setBorder(BorderFactory.createEmptyBorder(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER));

		fadeLabel = new JLabel(VueResources.getString("interactionTools.opacity"), SwingConstants.RIGHT);
		fadeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		constraints.gridx = 0;
		constraints.gridheight = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0.0;
		constraints.weighty = 0.0;
		fadePanel.add(fadeLabel, constraints);

		fadeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);

		JLabel		label100 = new JLabel(VueResources.getString("interactionTools.oneHundredPercent"));
		JLabel		label0 = new JLabel(VueResources.getString("interactionTools.zeroPercent"));
		Hashtable<Integer, JLabel>
					labelTable = new Hashtable<Integer, JLabel>();

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

		constraints.gridx = 1;
		fadePanel.add(fadeSlider, constraints);

		emptyPanel2 = new JPanel();
		constraints.gridx = 2;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		fadePanel.add(emptyPanel2, constraints);

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
			zoomSelLabel.setBackground(Color.BLUE);
			zoomSelLabel.setOpaque(true);
			zoomMapLabel.setBackground(Color.BLUE);
			zoomMapLabel.setOpaque(true);
			fadeSlider.setBackground(Color.BLUE);
			fadeSlider.setOpaque(true);
			zoomPanel.setBackground(Color.ORANGE);
			zoomPanel.setOpaque(true);
			fadePanel.setBackground(Color.ORANGE);
			fadePanel.setOpaque(true);
			emptyPanel1.setBackground(Color.YELLOW);
			emptyPanel1.setOpaque(true);
			emptyPanel2.setBackground(Color.YELLOW);
			emptyPanel2.setOpaque(true);
			linePanel.setBackground(Color.CYAN);
			linePanel.setOpaque(true);
		}

		setVisible(true);
	}

	public void finalize() {
		zoomLockCheckBox = null;
		zoomSelButton = null;
		zoomMapButton = null;
		fadeSlider = null;
		fadeLabel = null;
		zoomSelLabel = null;
		zoomMapLabel = null;
		zoomPanel = null;
		fadePanel = null;
		emptyPanel1 = null;
		emptyPanel2 = null;
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
}
