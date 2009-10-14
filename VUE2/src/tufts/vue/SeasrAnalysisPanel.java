package tufts.vue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.tufts.seasr.Flow;
import edu.tufts.seasr.FlowGroup;
import edu.tufts.seasr.SeasrConfigLoader;
import edu.tufts.vue.mbs.AnalyzerResult;
import edu.tufts.vue.mbs.SeasrAnalyzer;
import edu.tufts.vue.metadata.MetadataList;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;

public class SeasrAnalysisPanel extends JPanel implements ActionListener, FocusListener, LWSelection.Listener {
	static final long						serialVersionUID = 1;

	protected static SeasrAnalysisPanel		singleton = null;
	protected static DockWindow				dock = null;
	protected static SeasrConfigLoader		scl = null;

	protected static int					COLUMNS = 40,
											GUTTER = 4,
											INDENT = 4,
											BREAK = 4;
	protected static String					SELECT = VueResources.getString("seasr.analysis.select"),
											NEW_NODES = VueResources.getString("seasr.analysis.createnewnodes"),
											NEW_METADATA = VueResources.getString("seasr.analysis.addmetadata"),
											NEW_NOTES = VueResources.getString("seasr.analysis.addnotes");
	protected static boolean				DEBUG_LOCAL = false;

	JTextField								urlTextField = null;
	JComboBox								methodComboBox = null,
											flowComboBox = null;
	JButton									closeButton = null,
											analyzeButton = null;

	protected GridBagConstraints			constraints = new GridBagConstraints();

	public SeasrAnalysisPanel() {
		super(new GridBagLayout());

		JPanel			contentPanel = new JPanel(new GridBagLayout()),
						buttonPanel = new JPanel(new GridBagLayout());
		Color			background = getBackground();
		JTextArea		subtitle = newTextArea(VueResources.getString("seasr.analysis.subtitle"), background, COLUMNS),
						step1Number = newTextArea(VueResources.getString("seasr.analysis.1"), background),
						step2Number = newTextArea(VueResources.getString("seasr.analysis.2"), background),
						step3Number = newTextArea(VueResources.getString("seasr.analysis.3"), background),
						step1Title = newTextArea(VueResources.getString("seasr.analysis.step1"), background, COLUMNS),
						step2Title = newTextArea(VueResources.getString("seasr.analysis.step2"), background, COLUMNS),
						step3Title = newTextArea(VueResources.getString("seasr.analysis.step3"), background, COLUMNS);
		Insets			gutter = new Insets(GUTTER, GUTTER, GUTTER, GUTTER),
						gutterBreakIndent = new Insets(GUTTER + BREAK, GUTTER + INDENT, GUTTER, GUTTER),  // extra space above, indent on the left, 
						gutterBreak = new Insets(GUTTER + BREAK, GUTTER, GUTTER, GUTTER),  // extra space above
						gutterTopPanel = new Insets(GUTTER, GUTTER, 0, GUTTER),  // no gutter below
						gutterBottomPanel = new Insets(0, GUTTER, GUTTER, GUTTER);  // no gutter above

		urlTextField = new JTextField();
		methodComboBox = new JComboBox();
		flowComboBox = new JComboBox();
		closeButton = new JButton(VueResources.getString("seasr.analysis.close"));
		analyzeButton = new JButton(VueResources.getString("seasr.analysis.analyze"));

		addToGridBag(contentPanel, subtitle, 0, 0, 2, 1, GridBagConstraints.LINE_START, gutter);

		addToGridBag(contentPanel, step1Number, 0, 1, 1, 1, GridBagConstraints.LINE_START, gutterBreakIndent);

		addToGridBag(contentPanel, step2Number, 0, 3, 1, 1, GridBagConstraints.LINE_START, gutterBreakIndent);

		addToGridBag(contentPanel, step3Number, 0, 5, 1, 1, GridBagConstraints.LINE_START, gutterBreakIndent);

		addToGridBag(contentPanel, step1Title, 1, 1, 1, 1, GridBagConstraints.LINE_START, gutterBreak);

		urlTextField.setFont(tufts.vue.gui.GUI.LabelFace);
		urlTextField.setColumns(COLUMNS);
		urlTextField.addActionListener(this);
		urlTextField.addFocusListener(this);
		addToGridBag(contentPanel, urlTextField, 1, 2, 1, 1, GridBagConstraints.LINE_START, gutter);

		addToGridBag(contentPanel, step2Title, 1, 3, 1, 1, GridBagConstraints.LINE_START, gutterBreak);

		methodComboBox.setFont(tufts.vue.gui.GUI.LabelFace);
		methodComboBox.addItem(SELECT);
		methodComboBox.addItem(NEW_NODES);
		methodComboBox.addItem(NEW_METADATA);
		methodComboBox.addItem(NEW_NOTES);
		methodComboBox.addActionListener(this);
		addToGridBag(contentPanel, methodComboBox, 1, 4, 1, 1, GridBagConstraints.LINE_START, gutter);

		addToGridBag(contentPanel, step3Title, 1, 5, 1, 1, GridBagConstraints.LINE_START, gutterBreak);

		flowComboBox.setFont(tufts.vue.gui.GUI.LabelFace);
		flowComboBox.addItem(SELECT);
		flowComboBox.setEnabled(false);
		flowComboBox.addActionListener(this);
		addToGridBag(contentPanel, flowComboBox, 1, 6, 1, 1, GridBagConstraints.LINE_START, gutter);

		addToGridBag(this, contentPanel, 0, 0, 1, 1, GridBagConstraints.LINE_START, gutterTopPanel);

		closeButton.setFont(tufts.vue.gui.GUI.LabelFace);
		closeButton.addActionListener(this);
		addToGridBag(buttonPanel, closeButton, 0, 0, 1, 1, GridBagConstraints.LINE_END, gutter);

		analyzeButton.setFont(tufts.vue.gui.GUI.LabelFace);
		analyzeButton.setEnabled(false);
		analyzeButton.addActionListener(this);
		addToGridBag(buttonPanel, analyzeButton, 1, 0, 1, 1, GridBagConstraints.LINE_END, gutter);

		addToGridBag(this, buttonPanel, 0, 1, 1, 1, GridBagConstraints.LINE_END, gutterBottomPanel);

		if (DEBUG_LOCAL) {
			this.setBackground(Color.CYAN);
			contentPanel.setBackground(Color.YELLOW);
			buttonPanel.setBackground(Color.GREEN);

			subtitle.setBackground(Color.MAGENTA);
			step1Number.setBackground(Color.MAGENTA);
			step2Number.setBackground(Color.MAGENTA);
			step3Number.setBackground(Color.MAGENTA);
			step1Title.setBackground(Color.MAGENTA);
			step2Title.setBackground(Color.MAGENTA);
			step3Title.setBackground(Color.MAGENTA);
			methodComboBox.setBackground(Color.MAGENTA);
			flowComboBox.setBackground(Color.MAGENTA);
			closeButton.setBackground(Color.MAGENTA);
			analyzeButton.setBackground(Color.MAGENTA);

			methodComboBox.setOpaque(true);
			flowComboBox.setOpaque(true);
			closeButton.setOpaque(true);
			analyzeButton.setOpaque(true);
		}

		LWSelection		selection = VUE.getSelection();

		selection.addListener(this);

		selectionChanged(selection);
	}


	public void finalize() {
		urlTextField = null;
		methodComboBox = null;
		flowComboBox = null;
		closeButton = null;
		analyzeButton = null;
		constraints = null;
	}


	public static synchronized DockWindow getSeasrAnalysisDock() {
		if (dock == null) {
			singleton = new SeasrAnalysisPanel();

			dock = GUI.createDockWindow(VueResources.getString("seasr.analysis.title"), singleton);
			dock.pack();
			dock.setResizeEnabled(false);

			scl = new SeasrConfigLoader();
		}

		return dock;
	}


	protected void enableAnalyzeButton() {
		String			url = urlTextField.getText();
		boolean			newNodeMethod = (methodComboBox.getSelectedItem() == NEW_NODES);
		LWSelection		selection = VUE.getSelection();
		LWComponent		selectedNode = selection != null && selection.size() == 1 ? selection.first() : null;

		analyzeButton.setEnabled(looksLikeURL(url) && flowComboBox.isEnabled() && (newNodeMethod || selectedNode != null));
	}


	protected void methodChosen() {
		Object	method = methodComboBox.getSelectedItem();

		if (method != SELECT && methodComboBox.getItemAt(0) == SELECT) {
				methodComboBox.removeItemAt(0);
		}

		flowComboBox.removeAllItems();
		flowComboBox.addItem(SELECT);

		try {
			String		flowType = "";

			if (method == NEW_NODES) {
				flowType = SeasrConfigLoader.CREATE_NODES;
			} else if (method == NEW_METADATA) {
				flowType = SeasrConfigLoader.ADD_METADATA;
			} else if (method == NEW_NOTES) {
				flowType = SeasrConfigLoader.ADD_NOTES;
			}

			FlowGroup	fg = scl.getFlowGroup(flowType);

			for (Flow flow: fg.getFlowList()) {
				flowComboBox.addItem(flow);				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			VueUtil.alert(ex.getMessage(), VueResources.getString("seasr.analysis.error"));
		}

		flowComboBox.setEnabled(true);
		analyzeButton.setEnabled(false);
	}


	protected void flowChosen() {
		Object	method = flowComboBox.getSelectedItem();

		if (method != SELECT && flowComboBox.getItemAt(0) == SELECT) {
				flowComboBox.removeItemAt(0);
		}

		enableAnalyzeButton();
	}


	protected void analyze() {
		Object							method = methodComboBox.getSelectedItem();
		Flow							flow = (Flow)flowComboBox.getSelectedItem();
		LWSelection						selection = VUE.getSelection();
		LWComponent						selectedNode = selection != null && selection.size() == 1 ? selection.first() : null;
		LWMap							activeMap = VUE.getActiveMap();
		try {
			SeasrAnalyzer				analyzer = new SeasrAnalyzer(flow);
			List<AnalyzerResult>		resultList = analyzer.analyze(urlTextField.getText(), true);
/*
// for debugging, comment out the above line and do something like this:
List<AnalyzerResult> resultList = new ArrayList<AnalyzerResult>();
resultList.add(new AnalyzerResult("foo", "result1"));
resultList.add(new AnalyzerResult("foo", "result2"));
*/
			Iterator<AnalyzerResult>	resultIter = resultList.iterator();

			if (method == NEW_NODES) {
				List<LWComponent>	nodes = new ArrayList<LWComponent>(),
									links = new ArrayList<LWComponent>();

				while (resultIter.hasNext()) {
					AnalyzerResult	analyzerResult = resultIter.next();
					LWNode			node = new LWNode(analyzerResult.getType());

					nodes.add(node);
					node.layout();

					if (selectedNode != null) {
						LWLink		link = new LWLink(selectedNode, node);

						node.setLocation(selectedNode.getLocation());
						links.add(link);
						link.layout();
					}
				}

				activeMap.addChildren(nodes);
				activeMap.addChildren(links);

				if (selectedNode == null) {
					LayoutAction.circle.act(nodes);
					selection.setTo(nodes);
				} else {
					Actions.MakeCluster.clusterNodesAbout(selectedNode, nodes);
				}
			} else if (method == NEW_METADATA && selectedNode != null) {
				MetadataList		mList = selectedNode.getMetadataList();

				while (resultIter.hasNext()) {
					AnalyzerResult	analyzerResult = resultIter.next();

					mList.add(analyzerResult.getType(), analyzerResult.getValue());
				}
				
				selectedNode.layout();
				selectedNode.notify("meta-data");
			} else if (method == NEW_NOTES && selectedNode != null) {
				String OUT_TITLE = "Output from SEASR Flow: ";
				String		separator = ": ";
				String info = OUT_TITLE+analyzer.getFlow().getLabel()+"\n\n";
		    	
				while (resultIter.hasNext()) {		
					AnalyzerResult	analyzerResult = resultIter.next();
					info += analyzerResult.getType()+" - "+analyzerResult.getValue()+"\n";
		    	}

				String				notes = selectedNode.getNotes();

				selectedNode.setNotes((notes != null && notes.length() != 0 ? notes + "\n" : "") + info);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			VueUtil.alert(ex.getMessage(), VueResources.getString("seasr.analysis.error"));
		}

		activeMap.getUndoManager().mark(VueResources.getString("seasr.analysis.title"));
		activeMap.markAsModified();
	}


	protected boolean looksLikeURL(String string) {
		return string != null && (string.startsWith("http://") || string.startsWith("https://"));
	}


	protected JTextArea newTextArea(String string, Color background) {
		JTextArea	textArea = new JTextArea(string);

		textArea.setFont(tufts.vue.gui.GUI.LabelFace);
		textArea.setBackground(background);
		textArea.setEditable(false);

		return textArea;
	}


	protected JTextArea newTextArea(String string, Color background, int columns) {
		JTextArea	textArea = newTextArea(string, background);

		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setColumns(columns);

		return textArea;
	}


	protected void addToGridBag(Container container, Component component, int gridx, int gridy, int gridwidth, int gridheight,
			int anchor, Insets insets) {
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.gridwidth = gridwidth;
		constraints.gridheight = gridheight;
		constraints.anchor = anchor;
		constraints.insets = insets;

		container.add(component, constraints);
	}


	// ActionListener method
	public void actionPerformed(ActionEvent event) {
		Object	source = event.getSource();

		if (source == urlTextField) {
			enableAnalyzeButton();
		} else if(source == methodComboBox) {
			methodChosen();
		} else if (source == flowComboBox) {
			flowChosen();
		} else if (source == closeButton) {
			dock.setVisible(false);
		} else if (source == analyzeButton) {
			AnalyzeThread thread = new AnalyzeThread();
			thread.start();
		}
	}


	// LWSelection.Listener method
	public void selectionChanged(LWSelection selection) {
		String		url = "";

		if (selection.size() == 1) {
			LWComponent		component = selection.first();

			if (component instanceof LWNode) {
				Resource		resource = component.getResource();

				if (resource != null) {
					String	spec = resource.getSpec();

					if (looksLikeURL(spec)) {
						url = spec;
					}
				}

				if (url == "") {
					String	label = component.getLabel();

					if (looksLikeURL(label)) {
						url = label;
					}
				}
			}
		}

		urlTextField.setText(url);

		enableAnalyzeButton();
	}


	// FocusListener methods
	public void focusGained(FocusEvent event) {}

	public void focusLost(FocusEvent event) {
		enableAnalyzeButton();
	}


	// private classes

	private class AnalyzeThread extends Thread {
		public AnalyzeThread() {
			super();
		}

		public void run() {
			try {
				analyze();
			} catch(Exception ex) {
				ex.printStackTrace();
				VueUtil.alert(ex.getMessage(), VueResources.getString("seasr.analysis.error"));
			}
		}
	}
}
