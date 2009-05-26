package tufts.vue;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tufts.vue.gui.DockWindow;

public class PrototypePanel extends JPanel implements ChangeListener {
	public static final long	serialVersionUID = 1;
    public final static int		HALF_GUTTER = 4;
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
		
		zoomButton = new JButton("Selection Fit Window");
		zoomButton.setAction(tufts.vue.Actions.ZoomToSelection);
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
		setVisible(true);
	}

	public void finalize() {
		zoomLockCheckBox = null;
		zoomButton = null;
	}

	public static void zoomIfLocked() {
System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!in zoomIfLocked()");
		if (zoomLockCheckBox.isSelected()) {
			zoomButton.doClick();
		}
	}

	public void stateChanged(ChangeEvent event) {
		zoomIfLocked();
	}
}
