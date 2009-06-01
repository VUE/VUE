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

public class PrototypePanel extends JPanel implements ActionListener, ChangeListener {
	public static final long	serialVersionUID = 1;
    public final static int		HALF_GUTTER = 4;
    public final static String	ZOOM_IN = "Fit Selection In Window",
    							ZOOM_OUT = "Fit Map In Window";
	protected static JCheckBox	zoomLockCheckBox = null;
	protected static JButton	zoomButton = null;
    protected static JSlider	opacitySlider = null;

	public PrototypePanel(DockWindow dw) {
		GridBagConstraints	constraints = new GridBagConstraints();
		JPanel				contents = new JPanel();
		Insets				insets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		contents.setLayout(new GridBagLayout());

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.insets = insets;
		
		zoomButton = new JButton(ZOOM_IN);
//		zoomButton.setAction(tufts.vue.Actions.ZoomToSelection);
		zoomButton.addActionListener(this);
		contents.add(zoomButton, constraints);

		zoomLockCheckBox = new JCheckBox("Lock On");
		zoomLockCheckBox.addChangeListener(this);
		constraints.gridx = 1;
		contents.add(zoomLockCheckBox, constraints);

		opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);

/*      JLabel		label0 = new JLabel("0");
        JLabel		label25 = new JLabel("25");
        JLabel		label50 = new JLabel("50");
        JLabel		label75 = new JLabel("75");
	    JLabel		label100 = new JLabel("100");
        Hashtable	labelTable = new Hashtable();

        label0.setFont(tufts.vue.gui.GUI.LabelFace);
        label25.setFont(tufts.vue.gui.GUI.LabelFace);
        label50.setFont(tufts.vue.gui.GUI.LabelFace);
        label75.setFont(tufts.vue.gui.GUI.LabelFace);
        label100.setFont(tufts.vue.gui.GUI.LabelFace);

        label0.setForeground(Color.DARK_GRAY);
        label25.setForeground(Color.DARK_GRAY);
        label50.setForeground(Color.DARK_GRAY);
        label75.setForeground(Color.DARK_GRAY);
        label100.setForeground(Color.DARK_GRAY);

        labelTable.put(new Integer( 0 ), label0);
        labelTable.put(new Integer( 25 ), label25);
        labelTable.put(new Integer( 50 ), label50);
        labelTable.put(new Integer( 75 ), label75);
        labelTable.put(new Integer( 100 ), label100);
        
        opacitySlider.setLabelTable(labelTable); */
        opacitySlider.setLabelTable(opacitySlider.createStandardLabels(25));
        opacitySlider.setSnapToTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setPaintTicks(false);
        opacitySlider.setPreferredSize(new Dimension(130,35));
        opacitySlider.addChangeListener(this);

        constraints.gridx = 0;
		constraints.gridy = 1;
		contents.add(opacitySlider, constraints);

	    constraints.fill = GridBagConstraints.NONE;
		constraints.gridx = 0;
		setLayout(new GridBagLayout());
		add(contents, constraints);

		dw.setContent(this);

		validate();
		setButtonTitle();
		setVisible(true);
	}

	public void finalize() {
		zoomLockCheckBox = null;
		zoomButton = null;
	}

	public static void setButtonTitle() {
		LWSelection	selection = VUE.getSelection();

		if (selection.size() == 0) {
			zoomButton.setText(ZOOM_OUT);
		} else {
			zoomButton.setText(ZOOM_IN);
		}
	}

	public static void zoomIfLocked() {
		setButtonTitle();

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

/*	public static double getNodeAlpha() {
		return 1.0 - (((double)opacitySlider.getValue()) / 100.0);
	}*/

	public static double getAlpha() {
		return ((double)opacitySlider.getValue()) / 100.0;
	}

	/* ActionListener method -- button has been clicked */
	public void actionPerformed(ActionEvent event) {
		zoom();
	}

	/* ChangeListener method -- checkbox has been clicked */
	public void stateChanged(ChangeEvent event) {
		Object	source = event.getSource();

		if (source == zoomLockCheckBox) {
			zoomIfLocked();
		} else if (source == opacitySlider) {
			if (!opacitySlider.getValueIsAdjusting()) {
				VUE.getActiveViewer().paintImmediately();
			}
		}
	}
}
