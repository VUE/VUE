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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.tufts.seasr.Flow;
import edu.tufts.seasr.FlowGroup;
import edu.tufts.seasr.SeasrConfigLoader;
import edu.tufts.vue.mbs.SeasrAnalyzer;

import tufts.vue.AnalyzerAction.SeasrAction;
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
											CREATE_NODES = VueResources.getString("seasr.analysis.createnewnodes"),
											ADD_METADATA = VueResources.getString("seasr.analysis.addmetadata"),
											ADD_NOTES = VueResources.getString("seasr.analysis.addnotes");
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
						step1Number = newTextArea("1", background),
						step2Number = newTextArea("2", background),
						step3Number = newTextArea("3", background),
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
		methodComboBox.addItem(CREATE_NODES);
		methodComboBox.addItem(ADD_METADATA);
		methodComboBox.addItem(ADD_NOTES);
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
		String	url = urlTextField.getText();

		analyzeButton.setEnabled(looksLikeURL(url) && flowComboBox.isEnabled());
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

			if (method == CREATE_NODES) {
				flowType = SeasrConfigLoader.CREATE_NODES;
			} else if (method == ADD_METADATA) {
				flowType = SeasrConfigLoader.ADD_METADATA;
			} else if (method == ADD_NOTES) {
				flowType = SeasrConfigLoader.ADD_NOTES;
			}

			FlowGroup	fg = scl.getFlowGroup(flowType);

			for (Flow flow: fg.getFlowList()) {
//				flowComboBox.addItem(flow.getLabel());
				flowComboBox.addItem((Object)(new SeasrAction(new SeasrAnalyzer(flow), flow.getLabel(), null));
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
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
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ANALYZE");
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
			analyze();
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
}
