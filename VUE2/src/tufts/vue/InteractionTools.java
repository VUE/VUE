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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;


public class InteractionTools extends JPanel implements ActionListener, ItemListener, ChangeListener,
		ExpandSelectionListener {
	public static final long		serialVersionUID = 1;
	private static final org.apache.log4j.Logger
									Log = org.apache.log4j.Logger.getLogger(InteractionTools.class);
	protected static final boolean	DEBUG = false;
	protected static final int		HALF_GUTTER = 4,
									GUTTER = 2 * HALF_GUTTER,
									DEPTH_SLIDER_MIN = 0,
									DEPTH_SLIDER_MAX = 5;
	protected static final String	ONE_HUNDRED_PERCENT = new String("100%"),
									EIGHTY_PERCENT = new String("80%"),
									SIXTY_PERCENT = new String("60%"),
									FOURTY_PERCENT = new String("40%"),
									TWENTY_PERCENT = new String("20%"),
									ZERO_PERCENT = new String("0%"),
									OFF = new String(VueResources.getString("interactionTools.off")),
									ONE = new String("1"),
									TWO = new String("2"),
									THREE = new String("3"),
									FOUR = new String("4"),
									FIVE = new String("5");
	protected ArrayList<ExpandSelectionListener>
									expandSelectionListeners = new ArrayList<ExpandSelectionListener>();
	boolean							ignoreSliderEvents = false;
	protected JSlider				fadeSlider = null,
									depthSlider = null;
	protected JButton				zoomSelButton = null,
									zoomMapButton = null;
	protected JCheckBox				zoomLockCheckBox = null;
	protected JLabel				fadeLabel = null,
									depthLabel = null,
									zoomSelLabel = null,
									zoomMapLabel = null;
	protected JPanel				fadeDepthPanel = null,
									fadeDepthInnerPanel = null,
									zoomPanel = null,
									zoomInnerPanel = null,
									linePanel = null;
	protected WidgetStack			widgetStack = null;


	public InteractionTools(DockWindow dw) {
		Insets						halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		fadeDepthInnerPanel = new JPanel();
		fadeDepthInnerPanel.setLayout(new GridBagLayout());

		fadeLabel = new JLabel(VueResources.getString("interactionTools.opacity"), SwingConstants.RIGHT);
		fadeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(fadeDepthInnerPanel, fadeLabel, 0, 0, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		fadeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);

		JLabel						label100 = new JLabel(ONE_HUNDRED_PERCENT),
									label80 = new JLabel(EIGHTY_PERCENT),
									label60 = new JLabel(SIXTY_PERCENT),
									label40 = new JLabel(FOURTY_PERCENT),
									label20 = new JLabel(TWENTY_PERCENT),
									label0 = new JLabel(ZERO_PERCENT);
		Hashtable<Integer, JLabel>	fadeTable = new Hashtable<Integer, JLabel>();

		label100.setFont(tufts.vue.gui.GUI.LabelFace);
		label80.setFont(tufts.vue.gui.GUI.LabelFace);
		label60.setFont(tufts.vue.gui.GUI.LabelFace);
		label40.setFont(tufts.vue.gui.GUI.LabelFace);
		label20.setFont(tufts.vue.gui.GUI.LabelFace);
		label0.setFont(tufts.vue.gui.GUI.LabelFace);
		fadeTable.put(new Integer(100), label100);
		fadeTable.put(new Integer(80), label80);
		fadeTable.put(new Integer(60), label60);
		fadeTable.put(new Integer(40), label40);
		fadeTable.put(new Integer(20), label20);
		fadeTable.put(new Integer(0), label0);

		fadeSlider.setLabelTable(fadeTable);
		fadeSlider.setPaintLabels(true);
		fadeSlider.setSnapToTicks(false);
		fadeSlider.setMinimumSize(fadeSlider.getPreferredSize());
		fadeSlider.setToolTipText(VueResources.getString("interactionTools.opacity.toolTip"));
		fadeSlider.addChangeListener(this);
		addToGridBag(fadeDepthInnerPanel, fadeSlider, 1, 0, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		depthLabel = new JLabel(VueResources.getString("interactionTools.depth"), SwingConstants.RIGHT);
		depthLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(fadeDepthInnerPanel, depthLabel, 0, 1, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		depthSlider = new JSlider(JSlider.HORIZONTAL, DEPTH_SLIDER_MIN, DEPTH_SLIDER_MAX, DEPTH_SLIDER_MIN);

		JLabel						labelOff = new JLabel(OFF),
									label1 = new JLabel(ONE),
									label2 = new JLabel(TWO),
									label3 = new JLabel(THREE),
									label4 = new JLabel(FOUR),
									label5 = new JLabel(FIVE);
		Hashtable<Integer, JLabel>	depthTable = new Hashtable<Integer, JLabel>();
		DepthSelectionListener		depthListener = new DepthSelectionListener();

		labelOff.setFont(tufts.vue.gui.GUI.LabelFace);
		label1.setFont(tufts.vue.gui.GUI.LabelFace);
		label2.setFont(tufts.vue.gui.GUI.LabelFace);
		label3.setFont(tufts.vue.gui.GUI.LabelFace);
		label4.setFont(tufts.vue.gui.GUI.LabelFace);
		label5.setFont(tufts.vue.gui.GUI.LabelFace);
		depthTable.put(new Integer(0), labelOff);
		depthTable.put(new Integer(1), label1);
		depthTable.put(new Integer(2), label2);
		depthTable.put(new Integer(3), label3);
		depthTable.put(new Integer(4), label4);
		depthTable.put(new Integer(5), label5);

		depthSlider.setLabelTable(depthTable);
		depthSlider.setPaintLabels(true);
		depthSlider.setSnapToTicks(true);
		depthSlider.setMinimumSize(depthSlider.getPreferredSize());
		depthSlider.setToolTipText(VueResources.getString("interactionTools.depth.toolTip"));
		depthSlider.addChangeListener(depthListener);
		VUE.getSelection().addListener(depthListener);
		addToGridBag(fadeDepthInnerPanel, depthSlider, 1, 1, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		fadeDepthPanel = new JPanel();
		fadeDepthPanel.setLayout(new GridBagLayout());
		addToGridBag(fadeDepthPanel, fadeDepthInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 1.0, 0.0, halfGutterInsets);

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
		addToGridBag(zoomInnerPanel, zoomSelButton, 1, 0, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		zoomMapLabel = new JLabel(VueResources.getString("interactionTools.zoomMap.label"), SwingConstants.RIGHT);
		zoomMapLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(zoomInnerPanel, zoomMapLabel, 0, 1, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		zoomMapButton = new JButton();
		zoomMapButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomMapButton.setText(VueResources.getString("interactionTools.zoomMap"));
		zoomMapButton.setToolTipText(VueResources.getString("interactionTools.zoomMap.toolTip"));
		zoomMapButton.addActionListener(this);
		addToGridBag(zoomInnerPanel, zoomMapButton, 1, 1, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

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

		Dimension	linePanelSize = new Dimension(2 * GUTTER, (2 * zoomMapButton.getPreferredSize().height) + GUTTER);

		linePanel.setPreferredSize(linePanelSize);
		linePanel.setMinimumSize(linePanelSize);
		addToGridBag(zoomInnerPanel, linePanel, 2, 0, 1, 2, halfGutterInsets);

		zoomLockCheckBox = new JCheckBox(VueResources.getString("interactionTools.auto"));
		zoomLockCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomLockCheckBox.setToolTipText(VueResources.getString("interactionTools.auto.toolTip"));
		zoomLockCheckBox.addItemListener(this);
		addToGridBag(zoomInnerPanel, zoomLockCheckBox, 3, 0, 1, 2, halfGutterInsets);

		zoomPanel = new JPanel();
		zoomPanel.setLayout(new GridBagLayout());
		addToGridBag(zoomPanel, zoomInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 1.0, 0.0, halfGutterInsets);

		widgetStack = new WidgetStack(VueResources.getString("dockWindow.interactionTools.title"));
		widgetStack.addPane(VueResources.getString("interactionTools.fadeAndDepthWidget"), fadeDepthPanel);
		widgetStack.addPane(VueResources.getString("interactionTools.zoomWidget"), zoomPanel);
		dw.setContent(widgetStack);

		validate();

		if (DEBUG) {
			fadeSlider.setBackground(Color.CYAN);
			fadeSlider.setOpaque(true);
			depthSlider.setBackground(Color.CYAN);
			depthSlider.setOpaque(true);
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
			fadeDepthPanel.setBackground(Color.YELLOW);
			fadeDepthPanel.setOpaque(true);
			fadeDepthInnerPanel.setBackground(Color.MAGENTA);
			fadeDepthInnerPanel.setOpaque(true);
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
		fadeDepthPanel = null;
		fadeDepthInnerPanel = null;
		zoomPanel = null;
		zoomInnerPanel = null;
		linePanel = null;
		widgetStack = null;
	}


	protected void zoomIfLocked() {
		if (zoomLockCheckBox.isSelected()) {
			zoom();
		}
	}


	protected void zoom() {
		LWSelection	selection = VUE.getSelection();

		if (selection.size() == 0) {
			ZoomTool.setZoomFit();
		} else {
			ZoomTool.setZoomFitRegion(VUE.getActiveViewer(), selection.getBounds(), 32, false);
		}
	}


	public double getAlpha() {
		return (VUE.getSelection().size() == 0 ? 1.0 : ((double)fadeSlider.getValue()) / 100.0);
	}


	public boolean canExpand() {
		return depthSlider.getValue() < DEPTH_SLIDER_MAX;
	}


	public void doExpand() {
		depthSlider.setValue(depthSlider.getValue() + 1);
	}


	public boolean canShrink() {
		return depthSlider.getValue() > DEPTH_SLIDER_MIN;
	}


	public void doShrink() {
		depthSlider.setValue(depthSlider.getValue() - 1);
	}


	/* ActionListener method -- button has been clicked */
	public void actionPerformed(ActionEvent event) {
		Object	source = event.getSource();

		if (source == zoomSelButton) {
			ZoomTool.setZoomFitRegion(VUE.getActiveViewer(), VUE.getSelection().getBounds(), 32, false);
		} else if (source == zoomMapButton) {
			ZoomTool.setZoomFit();
		}
	}


	/* ItemListener method -- checkbox has been clicked */
	public void itemStateChanged(ItemEvent event) {
		Object	source = event.getSource();

		if (source == zoomLockCheckBox) {
			zoomIfLocked();
		}
	}


	/* ChangeListener method -- slider has been moved */
	public void stateChanged(ChangeEvent event) {
		Object	source = event.getSource();

		if (source == fadeSlider) {
			VUE.getActiveViewer().repaint();
		}
	}


	/* ExpandSelectionListener method */
	public void depthChanged(int newDepth) {
		ignoreSliderEvents = true;
		depthSlider.setValue(newDepth);
		ignoreSliderEvents = false;
	}


	public void addExpandSelectionListener(ExpandSelectionListener listener) {
		if (!expandSelectionListeners.contains(listener)) {
			expandSelectionListeners.add(listener);
		}
	}


	public void removeExpandSelectionListener(ExpandSelectionListener listener) {
		if (expandSelectionListeners.contains(listener)) {
			expandSelectionListeners.remove(listener);
		}
	}


	public void notifyExpandSelectionListeners() {
		if (!ignoreSliderEvents) {
			for (ExpandSelectionListener listener : expandSelectionListeners) {
				listener.depthChanged(depthSlider.getValue());
			}
		}

	}


	/* Static methods */

	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			Insets insets) {
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets, 0, 0) ;

		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}


	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor,
			Insets insets) {
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				0.0, 0.0, anchor, GridBagConstraints.NONE, insets, 0, 0) ;

		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}


	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor, int fill, double weightX, double weightY,
			Insets insets) {
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				weightX, weightY, anchor, fill, insets, 0, 0) ;

		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}


	protected static void addToGridBag(Container container, Component component,
			int gridX, int gridY, int gridWidth, int gridHeight,
			int anchor, int fill, double weightX, double weightY,
			Insets insets, int padX, int padY) {
		GridBagConstraints	constraints = new GridBagConstraints(gridX, gridY, gridWidth, gridHeight,
				weightX, weightY, anchor, fill, insets, padX, padY) ;

		((GridBagLayout)container.getLayout()).setConstraints(component, constraints);
		container.add(component);
	}

	
	protected class DepthSelectionListener implements ChangeListener, LWSelection.Listener {
		HashSet<LWComponent>	userSelection = new HashSet<LWComponent>(),	// LWComponents selected by the user
								deepSelection = new HashSet<LWComponent>();	// LWComponents selected by this class
		int						previousDepth = 0;
		boolean					ignoreSelectionEvents = false;


		DepthSelectionListener() {}


		// ChangeListener method for depthSlider
		public void stateChanged(ChangeEvent event) {
			JSlider	source = (JSlider)event.getSource();

			if (!source.getValueIsAdjusting()) {
				notifyExpandSelectionListeners();
				GUI.invokeAfterAWT(sliderMoved);
			}
		}


		// LWSelection.Listener method
		public void selectionChanged(LWSelection selection) {
			if (!ignoreSelectionEvents) {
				if (depthSlider.getValue() > 0) {
					// Changes to selection can't be made until after listener notification completes, so invoke this later.
					GUI.invokeAfterAWT(selectionChanged);
				}
				else {
					zoomSelButton.setEnabled(selection.size() > 0);
					zoomIfLocked();
				}				
			}
		}


		Runnable sliderMoved = new Runnable() {
			public void run() {
				try {
					LWSelection	guiSelection = VUE.getSelection();
					int			depth = depthSlider.getValue();

					// ignoreSelectionEvents = true means that the selection events that will be coming up
					// are caused by deepening the selection and should be ignored -- ie, they shouldn't
					// cause the selection to be deepened further.
					ignoreSelectionEvents = true;

					if (previousDepth == 0) {
						// userSelection will be empty;  set it to the GUI's current selection.
						userSelection.addAll(guiSelection);
					} else {
						// deepSelection will be recomputed below (if previousDepth is 0, it's already empty).
						deepSelection.clear();

						if (depth < previousDepth) {
							// deepSelection will be smaller;  reset the GUI's selection to userSelection.
							guiSelection.setTo(userSelection);
						}
					}

					if (depth == 0) {
						// Done with userSelection for now;  empty it.
						userSelection.clear();
					} else {
						// Find deepSelection and add it to the GUI's selection.
						findChildrenToDepth(userSelection, depth + 1);
						guiSelection.add(deepSelection.iterator());
					}

					previousDepth = depth;

					zoomIfLocked();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					Log.error("exception in InteractionPanel.sliderMoved()", ex);
				}
				finally {
					ignoreSelectionEvents = false;
				}
			}
		};


		Runnable selectionChanged = new Runnable() {
			public void run() {
				try {
					LWSelection	guiSelection = VUE.getSelection();
					int			depth = depthSlider.getValue();

					ignoreSelectionEvents = true;

					if (depth > 0) {
						// Compute userSelection as the GUI's current selection minus deepSelection.
						userSelection.clear();
						userSelection.addAll(guiSelection);

						Iterator<LWComponent> deepNodes = deepSelection.iterator();

						while (deepNodes.hasNext()) {
							userSelection.remove(deepNodes.next());
						}

						// Find deepSelection.
						deepSelection.clear();
						findChildrenToDepth(userSelection, depth + 1);

						// Set the GUI's selection to userSelection (it may have gotten smaller) and add deepSelection.
						guiSelection.setTo(userSelection);
						guiSelection.add(deepSelection);
					}

					zoomSelButton.setEnabled(guiSelection.size() > 0);
					zoomIfLocked();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					Log.error("exception in InteractionPanel.selectionChanged()", ex);
				}
				finally {
					ignoreSelectionEvents = false;
				}
			}
		};


		protected void findChildrenToDepth(Collection<LWComponent> collection, int depth) {
			// Add each node to deepSelection.
			Iterator<LWComponent>	nodes = collection.iterator();

			while (nodes.hasNext()) {
				LWComponent		node = nodes.next();

				if (node.getClass() == LWNode.class) {
					if (!userSelection.contains(node)) {
						deepSelection.add(node);
					}

					if (depth > 1) {
						// Add each node's links to deepSelection.
						Iterator<LWComponent>	links = (Iterator<LWComponent>)(node.getConnected().iterator());

						while (links.hasNext()) {
							LWComponent		link = links.next();

							if (!userSelection.contains(link)) {
								deepSelection.add(link);
							}
						}

						// Add each node's child nodes to deepSelection.
						findChildrenToDepth(node.getLinked(), depth - 1);
					}
				}
			}
		}
	}
}
