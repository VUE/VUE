package tufts.vue;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tufts.vue.gui.DockWindow;

public class PrototypePanel extends JPanel implements ActionListener, ChangeListener {
	public static final long	serialVersionUID = 1;
    public final static int		HALF_GUTTER = 4;
    public final static String	ZOOM_IN = "Fit Selection In Window",
    							ZOOM_OUT = "Fit Map In Window";
	protected static JCheckBox	zoomLockCheckBox = null;
	protected static JButton	zoomButton = null;

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

	/* ActionListener method -- button has been clicked */
	public void actionPerformed(ActionEvent event) {
		zoom();
	}

	/* ChangeListener method -- checkbox has been clicked */
	public void stateChanged(ChangeEvent event) {
		zoomIfLocked();
	}
}
