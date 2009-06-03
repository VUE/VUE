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
	public static final long		serialVersionUID = 1;
	protected static final boolean	DEBUG = false;
	protected static final int		HALF_GUTTER = 4;
	protected static final String	ZOOM_IN = "Fit Selection",
    								ZOOM_OUT = "Fit Map";
	protected static JCheckBox		zoomLockCheckBox = null;
	protected static JButton		zoomButton = null;
    protected static JSlider		fadeSlider = null;
    protected static JLabel			fadeLabel = null;

	public PrototypePanel(DockWindow dw) {
		GridBagConstraints	constraints = new GridBagConstraints();
		JPanel				zoomPanel = new JPanel(),
							linePanel = null,
							fadePanel = new JPanel();
		Insets				halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		setLayout(new GridBagLayout());
		zoomPanel.setLayout(new GridBagLayout());
		fadePanel.setLayout(new GridBagLayout());

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0.0;
		constraints.weighty = 0.0;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.insets = halfGutterInsets;
		
		zoomButton = new JButton(ZOOM_IN);
//		zoomButton.setAction(tufts.vue.Actions.ZoomToSelection);
		zoomButton.addActionListener(this);
		zoomPanel.add(zoomButton, constraints);

		zoomLockCheckBox = new JCheckBox("Lock On");
		zoomLockCheckBox.addChangeListener(this);
		constraints.gridx = 1;
		zoomPanel.add(zoomLockCheckBox, constraints);

		fadeLabel = new JLabel("Opacity");

        constraints.gridx = 0;
		fadePanel.add(fadeLabel, constraints);

		fadeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);

	    JLabel		label100 = new JLabel("100%");
/*      JLabel		label75 = new JLabel("75");
        JLabel		label50 = new JLabel("50");
        JLabel		label25 = new JLabel("25");	*/
        JLabel		label0 = new JLabel("0%");
        Hashtable	labelTable = new Hashtable();

/*      label100.setFont(tufts.vue.gui.GUI.LabelFace);
        label75.setFont(tufts.vue.gui.GUI.LabelFace);
        label50.setFont(tufts.vue.gui.GUI.LabelFace);
        label25.setFont(tufts.vue.gui.GUI.LabelFace);
        label0.setFont(tufts.vue.gui.GUI.LabelFace);

        label100.setForeground(Color.DARK_GRAY);
        label75.setForeground(Color.DARK_GRAY);
        label50.setForeground(Color.DARK_GRAY);
        label25.setForeground(Color.DARK_GRAY);
        label0.setForeground(Color.DARK_GRAY);	*/

        labelTable.put(new Integer( 0 ), label100);
/*      labelTable.put(new Integer( 25 ), label75);
        labelTable.put(new Integer( 50 ), label50);
        labelTable.put(new Integer( 75 ), label25);	*/
        labelTable.put(new Integer( 100 ), label0);
        
        fadeSlider.setLabelTable(labelTable);
		//fadeSlider.setLabelTable(fadeSlider.createStandardLabels(25));
		fadeSlider.setPaintLabels(true);
		//fadeSlider.setMajorTickSpacing(25);
        //fadeSlider.setSnapToTicks(true);
		fadeSlider.setPaintTicks(false);
		fadeSlider.setPreferredSize(new Dimension(130,35));
		fadeSlider.addChangeListener(this);

        constraints.gridx = 1;
		fadePanel.add(fadeSlider, constraints);

	    constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.insets = new Insets(HALF_GUTTER, HALF_GUTTER, 0, HALF_GUTTER);
		add(zoomPanel, constraints);

        linePanel = new JPanel() {
        	public static final long		serialVersionUID = 1;
            protected void paintComponent(java.awt.Graphics g) {
                if (isOpaque()) {
                	g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                g.setColor(java.awt.Color.DARK_GRAY);
                g.drawLine(HALF_GUTTER, getHeight() / 2, getWidth() - HALF_GUTTER - 1, getHeight() / 2);
            }
        };

		constraints.gridy = 1;
		constraints.insets = new Insets(0, HALF_GUTTER, 0, HALF_GUTTER);
		add(linePanel, constraints);

		constraints.gridy = 2;
		constraints.insets = new Insets(0, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);
		add(fadePanel, constraints);

		dw.setContent(this);

		validate();

		// Set zoom button's minimum size to fit the longer of its two labels...
		zoomButton.setText(ZOOM_IN);
		zoomButton.setPreferredSize(zoomButton.getSize());

		// and then set zoom button's label to be appropriate to the selection.
		setButtonTitle();

		if (DEBUG) {
			zoomLockCheckBox.setBackground(Color.BLUE);
			zoomLockCheckBox.setOpaque(true);
			zoomButton.setBackground(Color.BLUE);
			zoomButton.setOpaque(true);
		    fadeLabel.setBackground(Color.BLUE);
		    fadeLabel.setOpaque(true);
		    fadeSlider.setBackground(Color.BLUE);
		    fadeSlider.setOpaque(true);
		    zoomPanel.setBackground(Color.ORANGE);
		    zoomPanel.setOpaque(true);
		    linePanel.setBackground(Color.ORANGE);
		    linePanel.setOpaque(true);
		    fadePanel.setBackground(Color.ORANGE);
		    fadePanel.setOpaque(true);
		}

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

	public static double getAlpha() {
		return (VUE.getSelection().size() == 0 ? 1.0 : 1.0 - (((double)fadeSlider.getValue()) / 100.0));
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
		} else if (source == fadeSlider) {
			VUE.getActiveViewer().repaint();
		}
	}
}
