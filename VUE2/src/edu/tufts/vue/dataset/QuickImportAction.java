package edu.tufts.vue.dataset;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.Timer;

import edu.tufts.vue.layout.RelRandomLayout;
import edu.tufts.vue.rss.RSSDataSource;

import tufts.vue.LWMap;
import tufts.vue.VUE;
import tufts.vue.VueAction;
import tufts.vue.VueResources;
import tufts.vue.action.RDFOpenAction;
import tufts.vue.gui.VueFileChooser;


public class QuickImportAction extends VueAction{
	public static final long		serialVersionUID = 1;
	private static boolean			openUnderway = false;
	private static final Object		LOCK = new Object();
	protected static final int		COLUMNS = 30,
									GUTTER = 4;
	protected static final String	DATASET_TYPE = VueResources.getString("quickImport.datasetTypes"),
									DATASOURCE_TYPE = VueResources.getString("quickImport.datasourceTypes"),
									ONTOLOGY_TYPE = VueResources.getString("quickImport.ontologyTypes"),
									TYPES[] = {DATASET_TYPE, DATASOURCE_TYPE, ONTOLOGY_TYPE};
	protected static final int		DATASET_TYPE_INDEX = 0,
									DATASOURCE_TYPE_INDEX = 1,
									ONTOLOGY_TYPE_INDEX = 2;
	protected static final boolean	DEBUG_LOCAL = false;

	protected JDialog				dialog = null;
	protected JRadioButton			fileRadioButton = new JRadioButton(VueResources.getString("quickImport.file")),
									URLRadioButton = new JRadioButton(VueResources.getString("quickImport.url"));
	protected JButton				browseButton = new JButton(VueResources.getString("quickImport.browse")),
									cancelButton = new JButton(VueResources.getString("quickImport.cancel")),
									addButton = new JButton(VueResources.getString("quickImport.add"));
	protected JComboBox				typeComboBox = new JComboBox(TYPES);
	protected JTextField			fileTextField = new JTextField();
	protected File					file = null,
									lastDirectory = null;
	protected GridBagConstraints	constraints = new GridBagConstraints();
	protected LWMap					map = null;


	public QuickImportAction() {
		super(VueResources.getString("menu.file.quickimport"));
	}


	public void actionPerformed(ActionEvent event) {
		Object	source = event.getSource();

		if (source == fileRadioButton) {
			browseButton.setVisible(true);
			fileTextField.setText(file != null ? file.getPath() : "");
			addButton.setEnabled(file != null);
		} else if (source == URLRadioButton) {	
			browseButton.setVisible(false);
			fileTextField.setText("http://");
			fileTextField.requestFocus();
			addButton.setEnabled(true);
		} else if (source == browseButton) {
			file = showFileChooser();
			fileTextField.setText(file != null ? file.getPath() : "");
			addButton.setEnabled(file != null);
		} else if (source == cancelButton) {	
			dialog.setVisible(false);
		} else if (source == addButton) {	
			addDataset();
			dialog.setVisible(false);
		} else if (source == fileTextField) {
			addButton.doClick();
		} else if (source instanceof Timer) {
			showMap();
		} else {
			try {
				showDialog();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}


	protected void showDialog() {
		if (dialog == null) {
			JPanel			outerPanel = new JPanel(new GridBagLayout()),
							innerPanel = new JPanel(new GridBagLayout()),
							locationPanel = new JPanel(new GridBagLayout()),
							locationRadioPanel = new JPanel(new GridBagLayout()),
							locationRemainderPanel = new JPanel(),
							selectFilePanel = new JPanel(new GridBagLayout()),
							cancelAddPanel = new JPanel(new GridBagLayout());
			JLabel			infoLabel = new JLabel(tufts.vue.VueResources.getIcon("helpIcon.raw")),
							locationLabel = new JLabel(VueResources.getString("quickImport.location")),
							selectFileLabel = new JLabel(VueResources.getString("quickImport.selectfile"));
			ButtonGroup		locationGroup = new ButtonGroup();
			Insets			gutter = new Insets(GUTTER, GUTTER, GUTTER, GUTTER),
							noGutter = new Insets(0, 0, 0, 0);

			locationLabel.setFont(tufts.vue.gui.GUI.LabelFace);
			addToGridBag(innerPanel, locationLabel, 0, 0, 1, 1,
				GridBagConstraints.LINE_END, GridBagConstraints.NONE, gutter);

			locationGroup.add(fileRadioButton);
			locationGroup.add(URLRadioButton);

			fileRadioButton.setFont(tufts.vue.gui.GUI.LabelFace);
			fileRadioButton.setSelected(true);
			fileRadioButton.addActionListener(this);
			addToGridBag(locationRadioPanel, fileRadioButton, 0, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			URLRadioButton.setFont(tufts.vue.gui.GUI.LabelFace);
			URLRadioButton.addActionListener(this);
			addToGridBag(locationRadioPanel, URLRadioButton, 1, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			typeComboBox.setFont(tufts.vue.gui.GUI.LabelFace);
			typeComboBox.setSelectedIndex(DATASET_TYPE_INDEX);
			addToGridBag(locationRadioPanel, typeComboBox, 2, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			addToGridBag(locationPanel, locationRadioPanel, 0, 0, 1, 1,
					GridBagConstraints.LINE_START, GridBagConstraints.NONE, noGutter);

			addToGridBag(locationPanel, locationRemainderPanel, 1, 0, 1, 1,
					GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, noGutter);

			infoLabel.setToolTipText(VueResources.getString("quickImport.help"));
			addToGridBag(locationPanel, infoLabel, 2, 0, 1, 1,
					GridBagConstraints.LINE_END, GridBagConstraints.NONE, gutter);

			addToGridBag(innerPanel, locationPanel, 1, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, noGutter);
	
	
			selectFileLabel.setFont(tufts.vue.gui.GUI.LabelFace);
			addToGridBag(innerPanel, selectFileLabel, 0, 1, 1, 1,
				GridBagConstraints.LINE_END, GridBagConstraints.NONE, gutter);

			fileTextField.setFont(tufts.vue.gui.GUI.LabelFace);
			fileTextField.setColumns(COLUMNS);
			fileTextField.addActionListener(this);
			fileTextField.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent event){
					addButton.setEnabled(fileTextField.getText().length() > 0);
				}
			});
			addToGridBag(selectFilePanel, fileTextField, 0, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, gutter);

			browseButton.setFont(tufts.vue.gui.GUI.LabelFace);
			browseButton.addActionListener(this);
			addToGridBag(selectFilePanel, browseButton, 1, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			addToGridBag(innerPanel, selectFilePanel, 1, 1, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, noGutter);


			cancelButton.setFont(tufts.vue.gui.GUI.LabelFace);
			cancelButton.addActionListener(this);
			addToGridBag(cancelAddPanel, cancelButton, 0, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			addButton.setFont(tufts.vue.gui.GUI.LabelFace);
			addButton.setEnabled(false);
			addButton.addActionListener(this);
			addToGridBag(cancelAddPanel, addButton, 1, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, gutter);

			addToGridBag(innerPanel, cancelAddPanel, 1, 2, 1, 1,
				GridBagConstraints.LINE_END, GridBagConstraints.NONE, noGutter);


			addToGridBag(outerPanel, innerPanel, 0, 0, 1, 1,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, gutter);

			dialog = new JDialog(tufts.vue.VUE.getApplicationFrame(), VueResources.getString("quickImport.dialogtitle"));
			dialog.setContentPane(outerPanel);
			dialog.pack();
			dialog.setModal(true);
			dialog.setResizable(false);
			dialog.setLocationRelativeTo(null);  // centers the dialog

			if (DEBUG_LOCAL) {
				dialog.setBackground(Color.RED);
				outerPanel.setBackground(Color.CYAN);
				innerPanel.setBackground(Color.YELLOW);
				locationPanel.setBackground(Color.BLUE);
				locationRadioPanel.setBackground(Color.RED);
				locationRemainderPanel.setBackground(Color.MAGENTA);
				selectFilePanel.setBackground(Color.GREEN);
				cancelAddPanel.setBackground(Color.BLUE);
				locationLabel.setBackground(Color.MAGENTA);
				selectFileLabel.setBackground(Color.MAGENTA);
				fileRadioButton.setBackground(Color.MAGENTA);
				URLRadioButton.setBackground(Color.MAGENTA);
				typeComboBox.setBackground(Color.MAGENTA);
				browseButton.setBackground(Color.MAGENTA);
				cancelButton.setBackground(Color.MAGENTA);
				addButton.setBackground(Color.MAGENTA);

				locationLabel.setOpaque(true);
				selectFileLabel.setOpaque(true);
				fileRadioButton.setOpaque(true);
				URLRadioButton.setOpaque(true);
				typeComboBox.setOpaque(true);
				browseButton.setOpaque(true);
				cancelButton.setOpaque(true);
				addButton.setOpaque(true);
			}
		}

		dialog.setVisible(true);
	}


	protected File showFileChooser() {
		File	file = null;

		javax.swing.UIManager.put("FileChooser.openDialogTitleText",
				VueResources.getString("quickImport.filechoosertitle"));

		try {
			VueFileChooser	chooser = VueFileChooser.getVueFileChooser();

			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			if (lastDirectory != null) {
				chooser.setCurrentDirectory(lastDirectory);
			}

			if (chooser.showOpenDialog(VUE.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();

				File	parentFile = file.getParentFile();

				if (parentFile.isDirectory()) {
					lastDirectory = parentFile;
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			javax.swing.UIManager.put("FileChooser.openDialogTitleText",
					VueResources.getString("FileChooser.openDialogTitleText"));
		}

		return file;
	}


	protected void addDataset() {
		synchronized (LOCK) {
			if (!openUnderway) {
				openUnderway = true;
				VUE.activateWaitCursor();

				try {
					boolean	isFile = fileRadioButton.isSelected();

					if (isFile && file == null) {
						file = new File(fileTextField.getText());
					}

					switch (typeComboBox.getSelectedIndex()) {
					case DATASET_TYPE_INDEX:
					case DATASOURCE_TYPE_INDEX:
					default:
						boolean isFolder = isFile && file.isDirectory();

						Dataset	dataset = (isFolder ? new FolderDataset() : new ListDataset());

						dataset.setFileName(isFile ? file.getAbsolutePath() : fileTextField.getText());
						dataset.loadDataset();

						if (isFolder) {
							map = dataset.createMap();
							showMap();
						} else {
							// For ListDataset call createMap(this), so that this.actionPerformed(ActionEvent) will be called
							// when the dataset has finished loading.
							map = dataset.createMap(this);
						}

						break;

					case ONTOLOGY_TYPE_INDEX:

						map = RDFOpenAction.loadMap(isFile ? file.getAbsolutePath() : fileTextField.getText());
						showMap();
						break;
					}

					file = null;

				} catch(Exception ex) {
					ex.printStackTrace();
					VUE.clearWaitCursor();
					openUnderway = false;
				}
			}
		}
	}


	protected void showMap() {
		if (map != null) {
			VUE.displayMap(map);
			map.markAsModified();

			map = null;
		}

		VUE.clearWaitCursor();
		openUnderway = false;
	}


	protected void addToGridBag(Container container, Component component, int gridx, int gridy, int gridwidth, int gridheight,
			int anchor, int fill, Insets insets) {
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.gridwidth = gridwidth;
		constraints.gridheight = gridheight;
		constraints.anchor = anchor;
		constraints.fill = fill;
		constraints.weightx = (fill == GridBagConstraints.BOTH || fill == GridBagConstraints.HORIZONTAL ? 1.0 : 0.0);
		constraints.weighty = (fill == GridBagConstraints.BOTH || fill == GridBagConstraints.VERTICAL ? 1.0 : 0.0);
		constraints.insets = insets;

		container.add(component, constraints);
	}
}
