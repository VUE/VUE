package tufts.vue;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class ExpandSelectionControl extends JPanel implements ExpandSelectionListener {
	public static final long		serialVersionUID = 1;
	protected static final boolean	DEBUG = false;
	protected static Icon			iconFirstOff = VueResources.getImageIcon("expandselection.first.off"),
									iconFirstOn = VueResources.getImageIcon("expandselection.first.on"),
									iconFirstOver = VueResources.getImageIcon("expandselection.first.over"),
									iconRestOff = VueResources.getImageIcon("expandselection.rest.off"),
									iconRestOn = VueResources.getImageIcon("expandselection.rest.on"),
									iconRestOver = VueResources.getImageIcon("expandselection.rest.over");
	protected int					currentDepth = 0;
	protected JLabel				labels[] = {new JLabel(iconFirstOff),
										new JLabel(iconRestOff),
										new JLabel(iconRestOff),
										new JLabel(iconRestOff),
										new JLabel(iconRestOff),
										new JLabel(iconRestOff)};


	public ExpandSelectionControl() {
		GridBagConstraints	gbc = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
								GridBagConstraints.EAST, GridBagConstraints.NONE,
								new Insets(0, 0, 0, 0), 0, 0);

		setLayout(new GridBagLayout());

		for (int index = 0; index < 6; index++) {
			labels[index].setFont(tufts.vue.gui.GUI.LabelFace);
			labels[index].putClientProperty("JComponent.sizeVariant", "small");
			labels[index].setMinimumSize(labels[index].getPreferredSize());
			labels[index].setToolTipText(VueResources.getString("interactionTools.depth.toolTip"));
			labels[index].addMouseListener(new ExpandSelectionMouseListener());
			add(labels[index], gbc);
			gbc.gridx++;

			if (DEBUG) {
				labels[index].setBackground(Color.YELLOW);
				labels[index].setOpaque(true);
			}
		}

		if (DEBUG) {
			setBackground(Color.MAGENTA);
			setOpaque(true);
		}
	}


	public void redraw() {
		labels[0].setIcon(currentDepth > 0 ? iconFirstOn : iconFirstOff);

		for (int index = 1; index < 6; index++) {
			labels[index].setIcon(index <= currentDepth ? iconRestOn : iconRestOff);
		}
	}


	/* ExpandSelectionListener method */
	public void depthChanged(ExpandSelectionEvent event) {
		currentDepth = event.getDepth();
		redraw();
	}


	public void addExpandSelectionListener(ExpandSelectionListener tel) {
		listenerList.add(ExpandSelectionListener.class, tel);
	}


	public void removeExpandSelectionListener(ExpandSelectionListener tel) {
		listenerList.remove(ExpandSelectionListener.class, tel);
	}


	public ExpandSelectionListener[] getExpandSelectionListeners() {
		return (ExpandSelectionListener[])listenerList.getListeners(
				ExpandSelectionListener.class);
	}


	protected void fireDepthChanged(int depth) {
		ExpandSelectionListener[] listeners = getExpandSelectionListeners();

		ExpandSelectionEvent	event = new ExpandSelectionEvent(this, depth);

		for (ExpandSelectionListener listener : listeners) {
			listener.depthChanged(event);
		}
	}


	protected class ExpandSelectionMouseListener extends MouseAdapter {


		ExpandSelectionMouseListener() {}


		public void mousePressed(MouseEvent event) {
			JLabel	source = (JLabel)event.getSource();
			int		index;

			for (index = 0; index < 6; index++) {
				if (labels[index] == source) {
					break;
				}
			}

			currentDepth = index;

			redraw();

			fireDepthChanged(currentDepth);
		}

		public void mouseEntered(MouseEvent event) {
			JLabel	source = (JLabel)event.getSource();
			Icon	icon = iconRestOver;

			if ((labels[0] != source || currentDepth > 0)) {
				labels[0].setIcon(iconFirstOver);
			}

			if (labels[0] == source) {
				icon = iconRestOff;
			}

			for (int index = 1; index < 6; index++) {
				labels[index].setIcon(icon);

				if (labels[index] == source) {
					icon = iconRestOff;
				}
			}
		}


		public void mouseExited(MouseEvent event) {
			redraw();
		}
	}
}
