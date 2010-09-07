/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
	boolean							ignoreSliderEvents = false;
	protected JSlider				fadeSlider = null,
									depthSlider = null;
	protected JButton				zoomSelButton = null,
									zoomMapButton = null;
	protected JCheckBox				zoomLockCheckBox = null,
									incomingLinksCheckBox = null,
									outgoingLinksCheckBox = null;
	protected JLabel				linksLabel = null,
									zoomSelLabel = null,
									zoomMapLabel = null;
	protected JPanel				fadePanel = null,
									fadeInnerPanel = null,
									depthPanel = null,
									depthInnerPanel = null,
									linkDirectionPanel = null,
									zoomPanel = null,
									zoomInnerPanel = null,
									linePanel = null,
									depthSpacerPanel = null,
									zoomSpacerPanel = null;
	protected WidgetStack			widgetStack = null;


	public InteractionTools(DockWindow dw) {
		Insets						halfGutterInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

		fadeInnerPanel = new JPanel();
		fadeInnerPanel.setLayout(new GridBagLayout());

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
		fadeSlider.setToolTipText(VueResources.getString("interactionTools.fade.toolTip"));
		fadeSlider.addChangeListener(this);
		addToGridBag(fadeInnerPanel, fadeSlider, 0, 0, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		fadePanel = new JPanel();
		fadePanel.setLayout(new GridBagLayout());
		addToGridBag(fadePanel, fadeInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		depthInnerPanel = new JPanel();
		depthInnerPanel.setLayout(new GridBagLayout());

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
		addToGridBag(depthInnerPanel, depthSlider, 0, 0, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		linkDirectionPanel = new JPanel();
		linkDirectionPanel.setLayout(new GridBagLayout());

		depthSpacerPanel = new JPanel();
		addToGridBag(linkDirectionPanel, depthSpacerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, new Insets(0, 0, 0, 0));

		linksLabel = new JLabel(VueResources.getString("interactionTools.linkDirection.label"), SwingConstants.RIGHT);
		linksLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(linkDirectionPanel, linksLabel, 1, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, new Insets(0, 0, 0, HALF_GUTTER));

		incomingLinksCheckBox = new JCheckBox(VueResources.getString("interactionTools.incomingLinks"));
		incomingLinksCheckBox.setSelected(true);
		incomingLinksCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		incomingLinksCheckBox.setToolTipText(VueResources.getString("interactionTools.incomingLinks.toolTip"));
		incomingLinksCheckBox.addItemListener(depthListener);
		addToGridBag(linkDirectionPanel, incomingLinksCheckBox, 2, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, new Insets(0, HALF_GUTTER, 0, HALF_GUTTER));

		outgoingLinksCheckBox = new JCheckBox(VueResources.getString("interactionTools.outgoingLinks"));
		outgoingLinksCheckBox.setSelected(true);
		outgoingLinksCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		outgoingLinksCheckBox.setToolTipText(VueResources.getString("interactionTools.outgoingLinks.toolTip"));
		outgoingLinksCheckBox.addItemListener(depthListener);
		addToGridBag(linkDirectionPanel, outgoingLinksCheckBox, 3, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, new Insets(0, HALF_GUTTER, 0, 0));

		addToGridBag(depthInnerPanel, linkDirectionPanel, 0, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, halfGutterInsets);

		depthPanel = new JPanel();
		depthPanel.setLayout(new GridBagLayout());
		addToGridBag(depthPanel, depthInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		zoomInnerPanel = new JPanel();
		zoomInnerPanel.setLayout(new GridBagLayout());

		zoomSpacerPanel = new JPanel();
		addToGridBag(zoomInnerPanel, zoomSpacerPanel, 0, 0, 1, 2, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 0.0, 0.0, new Insets(0, 0, 0, 0));

		zoomSelLabel = new JLabel(VueResources.getString("interactionTools.zoomSel.label"), SwingConstants.RIGHT);
		zoomSelLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(zoomInnerPanel, zoomSelLabel, 1, 0, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		zoomSelButton = new JButton();
		zoomSelButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomSelButton.setText(VueResources.getString("interactionTools.zoomSel"));
		zoomSelButton.setToolTipText(VueResources.getString("interactionTools.zoomSel.toolTip"));
		zoomSelButton.addActionListener(this);
		addToGridBag(zoomInnerPanel, zoomSelButton, 2, 0, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

		zoomMapLabel = new JLabel(VueResources.getString("interactionTools.zoomMap.label"), SwingConstants.RIGHT);
		zoomMapLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		addToGridBag(zoomInnerPanel, zoomMapLabel, 1, 1, 1, 1, GridBagConstraints.LINE_END, halfGutterInsets);

		zoomMapButton = new JButton();
		zoomMapButton.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomMapButton.setText(VueResources.getString("interactionTools.zoomMap"));
		zoomMapButton.setToolTipText(VueResources.getString("interactionTools.zoomMap.toolTip"));
		zoomMapButton.addActionListener(this);
		addToGridBag(zoomInnerPanel, zoomMapButton, 2, 1, 1, 1, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, 1.0, 0.0, halfGutterInsets);

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
		addToGridBag(zoomInnerPanel, linePanel, 3, 0, 1, 2, halfGutterInsets);

		zoomLockCheckBox = new JCheckBox(VueResources.getString("interactionTools.auto"));
		zoomLockCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		zoomLockCheckBox.setToolTipText(VueResources.getString("interactionTools.auto.toolTip"));
		zoomLockCheckBox.addItemListener(this);
		addToGridBag(zoomInnerPanel, zoomLockCheckBox, 4, 0, 1, 2, halfGutterInsets);

		zoomPanel = new JPanel();
		zoomPanel.setLayout(new GridBagLayout());
		addToGridBag(zoomPanel, zoomInnerPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, 1.0, 0.0, halfGutterInsets);

		widgetStack = new WidgetStack(VueResources.getString("dockWindow.interactionTools.title"));
		widgetStack.addPane(VueResources.getString("interactionTools.fadeWidget"), fadePanel);
		widgetStack.addPane(VueResources.getString("interactionTools.depthWidget"), depthPanel);
		widgetStack.addPane(VueResources.getString("interactionTools.zoomWidget"), zoomPanel);
		dw.setContent(widgetStack);

		// Line up the labels on the depth and zoom widgets.

		Dimension	linksPrefSize = linksLabel.getPreferredSize();
		int			depthExcessWidth = linksPrefSize.width - Math.max(zoomSelLabel.getPreferredSize().width, zoomMapLabel.getPreferredSize().width),
					depthPrefWidth = 0,
					zoomPrefWidth = 0;

		if (depthExcessWidth > 0) {				// depth widget labels are wider
			zoomPrefWidth = depthExcessWidth;
		} else if (depthExcessWidth < 0){		// zoom widget labels are wider
			depthPrefWidth = -depthExcessWidth;
		}										// otherwise, they are the same width

		Dimension	depthPrefSize = new Dimension(depthPrefWidth, linksPrefSize.height),
					zoomPrefSize = new Dimension(zoomPrefWidth, linePanelSize.height);

		// Not only MUST minimum AND preferred sizes be set, they MUST be set IN THAT ORDER, and the panels MUST be opaque.
		depthSpacerPanel.setMinimumSize(depthPrefSize);
		depthSpacerPanel.setPreferredSize(depthPrefSize);
		depthSpacerPanel.setOpaque(true);
		zoomSpacerPanel.setMinimumSize(zoomPrefSize);
		zoomSpacerPanel.setPreferredSize(zoomPrefSize);
		zoomSpacerPanel.setOpaque(true);

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
			incomingLinksCheckBox.setOpaque(true);
			incomingLinksCheckBox.setBackground(Color.CYAN);
			outgoingLinksCheckBox.setOpaque(true);
			outgoingLinksCheckBox.setBackground(Color.CYAN);
			linkDirectionPanel.setOpaque(true);
			linkDirectionPanel.setBackground(Color.YELLOW);
			linksLabel.setBackground(Color.CYAN);
			linksLabel.setOpaque(true);
			zoomSelLabel.setBackground(Color.CYAN);
			zoomSelLabel.setOpaque(true);
			zoomMapLabel.setBackground(Color.CYAN);
			zoomMapLabel.setOpaque(true);
			fadePanel.setBackground(Color.YELLOW);
			fadePanel.setOpaque(true);
			fadeInnerPanel.setBackground(Color.MAGENTA);
			fadeInnerPanel.setOpaque(true);
			depthPanel.setBackground(Color.YELLOW);
			depthPanel.setOpaque(true);
			depthInnerPanel.setBackground(Color.MAGENTA);
			depthInnerPanel.setOpaque(true);
			zoomPanel.setBackground(Color.YELLOW);
			zoomPanel.setOpaque(true);
			zoomInnerPanel.setBackground(Color.MAGENTA);
			zoomInnerPanel.setOpaque(true);
			linePanel.setBackground(Color.CYAN);
			linePanel.setOpaque(true);
			depthSpacerPanel.setOpaque(true);
			depthSpacerPanel.setBackground(Color.GREEN);
			zoomSpacerPanel.setOpaque(true);
			zoomSpacerPanel.setBackground(Color.GREEN);
		}

		setVisible(true);
	}


	public void finalize() {
		fadeSlider = null;
		zoomSelButton = null;
		zoomMapButton = null;
		zoomLockCheckBox = null;
		incomingLinksCheckBox = null;
		outgoingLinksCheckBox = null;
		linksLabel = null;
		zoomSelLabel = null;
		zoomMapLabel = null;
		fadePanel = null;
		fadeInnerPanel = null;
		depthPanel = null;
		depthInnerPanel = null;
		linkDirectionPanel = null;
		zoomPanel = null;
		zoomInnerPanel = null;
		linePanel = null;
		depthSpacerPanel = null;
		zoomSpacerPanel = null;
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
	public void depthChanged(ExpandSelectionEvent event) {
		ignoreSliderEvents = true;
		depthSlider.setValue(event.getDepth());
		ignoreSliderEvents = false;
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
		if (!ignoreSliderEvents) {
			ExpandSelectionListener[] listeners = getExpandSelectionListeners();
	
			ExpandSelectionEvent	event = new ExpandSelectionEvent(this, depth);
	
			for (ExpandSelectionListener listener : listeners) {
				listener.depthChanged(event);
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

	
	protected class DepthSelectionListener implements ChangeListener, ItemListener, LWSelection.Listener {
		HashSet<LWComponent>	userSelection = new HashSet<LWComponent>(),	// LWComponents selected by the user
								deepSelection = new HashSet<LWComponent>();	// LWComponents selected by this class
		int						previousDepth = 0;
		boolean					ignoreSelectionEvents = false;


		DepthSelectionListener() {}


		// ChangeListener method for depthSlider
		public void stateChanged(ChangeEvent event) {
			JSlider	source = (JSlider)event.getSource();

			if (!source.getValueIsAdjusting()) {
				fireDepthChanged(source.getValue());
				GUI.invokeAfterAWT(sliderMoved);
			}
		}


		// ItemListener for link direction checkboxes
		public void itemStateChanged(ItemEvent event) {
			JCheckBox	source = (JCheckBox)event.getSource();

			if (source == incomingLinksCheckBox &&
					!incomingLinksCheckBox.isSelected() && !outgoingLinksCheckBox.isSelected()) {
				outgoingLinksCheckBox.setSelected(true);
			} else if (source == outgoingLinksCheckBox &&
					!outgoingLinksCheckBox.isSelected() && !incomingLinksCheckBox.isSelected()) {
				incomingLinksCheckBox.setSelected(true);
			}

			GUI.invokeAfterAWT(selectionChanged);
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
						findChildrenToDepth(userSelection, depth, incomingLinksCheckBox.isSelected(), outgoingLinksCheckBox.isSelected(), new Hashtable<LWComponent, Integer>());
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
						findChildrenToDepth(userSelection, depth, incomingLinksCheckBox.isSelected(), outgoingLinksCheckBox.isSelected(), new Hashtable<LWComponent, Integer>());

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


		protected void findChildrenToDepth(Collection<LWComponent> comps, int depth, boolean expandIncoming, boolean expandOutgoing, Hashtable<LWComponent, Integer> alreadyVisited) {
			for (LWComponent comp : comps) {
				Integer		alreadyVisitedAtLevel = alreadyVisited.get(comp);

				// If this component has already been visited at a higher depth, don't revisit it.
				if (alreadyVisitedAtLevel == null || alreadyVisitedAtLevel.intValue() < depth) {
					alreadyVisited.put(comp, new Integer(depth));

					boolean		compIsUserSelected = userSelection.contains(comp),
								compIsLink =  (comp.getClass() == LWLink.class);
					int 		nextDepth = depth - (compIsLink ? (compIsUserSelected ? 1 : 0) : 1);

		
					if (!compIsUserSelected) {
						deepSelection.add(comp);
					}

					if (compIsLink) {
						// Recurse for link's endpoints.

						LWLink		link = (LWLink)comp;

						LWComponent				head = link.getHead(),
												tail = link.getTail();
						int						arrowState = link.getArrowState();
						HashSet<LWComponent>	compsToTraverse = new HashSet<LWComponent>();

						if (head != null &&
								(expandIncoming && arrowState != LWLink.ARROW_HEAD ||
								expandOutgoing && arrowState != LWLink.ARROW_TAIL)) {
							compsToTraverse.add(head);
						}

						if (tail != null && 
								(expandIncoming && arrowState != LWLink.ARROW_TAIL ||
								expandOutgoing && arrowState != LWLink.ARROW_HEAD)) {
							compsToTraverse.add(tail);
						}

						if (!compsToTraverse.isEmpty()) {
							findChildrenToDepth(compsToTraverse, nextDepth, expandIncoming, expandOutgoing, alreadyVisited);
						}
					}

					if (nextDepth > -1) {
						// Recurse for component's links.

						HashSet<LWComponent>		linksToTraverse = new HashSet<LWComponent>();

						if (expandIncoming && expandOutgoing) {
							linksToTraverse.addAll(comp.getLinks());
						} else if (expandIncoming) {
							linksToTraverse.addAll(comp.getIncomingLinks());
						} else if (expandOutgoing) {
							linksToTraverse.addAll(comp.getOutgoingLinks());
						}

						if (!linksToTraverse.isEmpty()) {
							findChildrenToDepth(linksToTraverse, nextDepth, expandIncoming, expandOutgoing, alreadyVisited);
						}
					}
				}
			}
		}
	}
}
