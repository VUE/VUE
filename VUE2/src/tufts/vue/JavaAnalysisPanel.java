package tufts.vue;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.tufts.vue.metadata.MetadataList;

import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LayoutAction;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.VueUtil;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFileChooser;

public class JavaAnalysisPanel extends JPanel implements ActionListener {
	private static final long			serialVersionUID = 1L;
	protected static final String		DOT_JAVA = ".java",
										ABSTRACT_KEYWORD = "abstract",
										CLASS_KEYWORD = "class",
										INTERFACE_KEYWORD = "interface",
										EXTENDS_KEYWORD = "extends",
										IMPLEMENTS_KEYWORD = "implements",
										METADATA_CATEGORY = "Java Analysis",
										METADATA_KEYWORD_UNDECLARED = "undeclared",
										METADATA_KEYWORD_INNER = "inner",
										JAVA_ANALYSIS = VueResources.getString("analyze.java.javaAnalysis"),
										HELP_TEXT = VueResources.getString("dockWindow.JavaAnalysis.helpText"),
										DEFAULT_FILE_CHOOSER_TITLE = VueResources.getString("FileChooser.openDialogTitleText"),
										DEFAULT_FILE_CHOOSER_OPEN_BUTTON = VueResources.getString("FileChooser.openButtonText"),
										DEFAULT_FILE_CHOOSER_OPEN_BUTTON_TOOLTIP = VueResources.getString("FileChooser.openButtonToolTipText"),
										CHOOSE_FILES = VueResources.getString("analyze.java.chooseFiles"),
										ANALYZE_CLASSES = VueResources.getString("analyze.java.analyzeClasses"),
										ANALYZE_INNER_CLASSES = VueResources.getString("analyze.java.analyzeInnerClasses"),
										ANALYZE_INTERFACES = VueResources.getString("analyze.java.analyzeInterfaces"),
										ANALYZE_INNER_INTERFACES = VueResources.getString("analyze.java.analyzeInnerInterfaces"),
										ANALYZE = VueResources.getString("analyze.java.analyze"),
										ANALYZE_TOOLTIP = VueResources.getString("analyze.java.analyzeTooltip"),
										STOP = VueResources.getString("analyze.java.stop"),
										PARSING = VueResources.getString("analyze.java.parsing"),
										DISPLAYING = VueResources.getString("analyze.java.displaying"),
										NO_JAVA_CLASS = VueResources.getString("analyze.java.noJavaClass"),
										JAVA_ANALYSIS_ERROR = VueResources.getString("analyze.java.error");
	protected static final Color		DECLARED_COLOR = Color.BLACK,
										UNDECLARED_COLOR = Color.LIGHT_GRAY;
	protected static final LWComponent.StrokeStyle
										ABSTRACT_STYLE = LWComponent.StrokeStyle.DASHED,
										NONABSTRACT_STYLE = LWComponent.StrokeStyle.SOLID;
	protected static final Class<? extends RectangularShape>
										CLASS_SHAPE = Ellipse2D.Float.class,
										INTERFACE_SHAPE = Rectangle2D.Float.class;
	protected static final char			DOLLAR_TOKEN = '$',
										UNDERSCORE_TOKEN = '_',
										COMMA_TOKEN = ',',
										SEMICOLON_TOKEN = ';';
	protected static final int			CLASSES_MASK = 1,
										INNER_CLASSES_MASK = 2,
										INTERFACES_MASK = 4,
										INNER_INTERFACES_MASK = 8;
	protected static final int			HALF_GUTTER = 4;
	protected static final Insets		HALF_GUTTER_INSETS = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);
	protected static DockWindow			dock = null;
	protected static final boolean		DEBUG = false;

	protected JPanel					contentPanel = null;
	protected JCheckBox					classesCheckBox = null,
										innerClassesCheckBox = null,
										interfacesCheckBox = null,
										innerInterfacesCheckBox = null;
	protected JTextArea					messageTextArea = null;
	protected JButton					analyzeButton = null;
	protected File						lastDirectory = null;
	protected List<LWNode>				allNodes= null;
	protected List<LWComponent>			newComps= null;
	protected Hashtable<String, LWNode>	classHash = null;
	protected int						totalFoundCount = 0;
	protected boolean					isAbstract = false;
	protected boolean					proceedWithAnalysis = true;
	AnalysisThread						analysisThread = null;


	public JavaAnalysisPanel() {
		super(new GridBagLayout());

		contentPanel = new JPanel(new GridBagLayout());
		classesCheckBox = new JCheckBox(ANALYZE_CLASSES);
		innerClassesCheckBox = new JCheckBox(ANALYZE_INNER_CLASSES);
		interfacesCheckBox = new JCheckBox(ANALYZE_INTERFACES);
		innerInterfacesCheckBox = new JCheckBox(ANALYZE_INNER_INTERFACES);
		messageTextArea = new JTextArea();
		analyzeButton = new JButton(CHOOSE_FILES);

		classesCheckBox.setSelected(true);
		innerClassesCheckBox.setSelected(false);
		interfacesCheckBox.setSelected(true);
		innerInterfacesCheckBox.setSelected(false);

		classesCheckBox.addActionListener(this);
		innerClassesCheckBox.addActionListener(this);
		interfacesCheckBox.addActionListener(this);
		innerInterfacesCheckBox.addActionListener(this);
		analyzeButton.addActionListener(this);

		classesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		innerClassesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		interfacesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		innerInterfacesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		messageTextArea.setFont(tufts.vue.gui.GUI.LabelFace);
		analyzeButton.setFont(tufts.vue.gui.GUI.LabelFace);

		messageTextArea.setBackground(contentPanel.getBackground());

		int			checkbox_width = (int)(innerClassesCheckBox.getPreferredSize().getHeight());
		Insets		HALF_GUTTER_INSETS_INDENTED = new Insets(0, checkbox_width, HALF_GUTTER, HALF_GUTTER);

		addToGridBag(contentPanel, classesCheckBox,         0, 0, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, innerClassesCheckBox,    0, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS_INDENTED);
		addToGridBag(contentPanel, interfacesCheckBox,      0, 2, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, innerInterfacesCheckBox, 0, 3, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS_INDENTED);
		addToGridBag(contentPanel, messageTextArea,         0, 4, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.BOTH, 1.0, 1.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, analyzeButton,           0, 5, 1, 1, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(this, contentPanel,                    0, 0, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.BOTH, 1.0, 1.0, HALF_GUTTER_INSETS);

		if (DEBUG) {
			contentPanel.setBackground(Color.CYAN);
			classesCheckBox.setBackground(Color.MAGENTA);
			classesCheckBox.setOpaque(true);
			innerClassesCheckBox.setBackground(Color.MAGENTA);
			innerClassesCheckBox.setOpaque(true);
			interfacesCheckBox.setBackground(Color.MAGENTA);
			interfacesCheckBox.setOpaque(true);
			innerInterfacesCheckBox.setBackground(Color.MAGENTA);
			innerInterfacesCheckBox.setOpaque(true);
			messageTextArea.setBackground(Color.MAGENTA);
			analyzeButton.setBackground(Color.MAGENTA);
			analyzeButton.setOpaque(true);
		}
	}


	public void finalize() {
		contentPanel = null;
		classesCheckBox = null;
		innerClassesCheckBox = null;
		interfacesCheckBox = null;
		innerInterfacesCheckBox = null;
		messageTextArea = null;
		analyzeButton = null;
		lastDirectory = null;
		allNodes = null;
		newComps = null;
		classHash = null;
		analysisThread = null;
	}


	public static DockWindow getJavaAnalysisDock() {
		if (dock == null) {
			JavaAnalysisPanel	analysisPanel = new JavaAnalysisPanel();

			dock = GUI.createDockWindow(JAVA_ANALYSIS, HELP_TEXT, analysisPanel);
			dock.pack();
			dock.setResizeEnabled(true);
		}

		return dock;
	}


	protected void analyze(int mask) {
		// Parse each file, then display the results.
		File		files[] = showFileChooser();

		if (files != null) {
			analyzeButton.setText(STOP);

			int			fileCount = files.length,
						fileIndex;
			Point2D		viewerCenter = VUE.getActiveViewer().getVisibleMapCenter();
			double		viewerCenterX = viewerCenter.getX(),
						viewerCenterY = viewerCenter.getY();

			if (DEBUG) {
				System.out.println("!!!!!!!!!!!! in AnalyzeJava.actionPerformed(): analyzing " + 
					fileCount + " file" + (fileCount == 1 ? "." : "s."));
			}

			newComps = new ArrayList<LWComponent>();
			findNodesOnMap();
			totalFoundCount = 0;

			messageTextArea.setText(PARSING);

			for (fileIndex = 0; fileIndex < fileCount && proceedWithAnalysis; fileIndex++) {
				File	file = files[fileIndex];
				int		foundCount;

				if (file.isDirectory()) {
					File	filesInFolder[] = file.listFiles(new JavaFileFilter());
					int		fileInFolderCount = filesInFolder.length,
							fileInFolderIndex;

					for (fileInFolderIndex = 0; fileInFolderIndex < fileInFolderCount && proceedWithAnalysis; fileInFolderIndex++) {
						File	fileInFolder = filesInFolder[fileInFolderIndex];

						if (!fileInFolder.isDirectory()) {
							foundCount = parseSourceFile(fileInFolder, mask, viewerCenterX, viewerCenterY);

							if (foundCount > 0) {
								totalFoundCount += foundCount;
							} else {
								VueUtil.alert(String.format(NO_JAVA_CLASS, file.getName()), JAVA_ANALYSIS);
							}
						}
					}
				}
				else {
					foundCount = parseSourceFile(file, mask, viewerCenterX, viewerCenterY);

					if (foundCount > 0) {
						totalFoundCount += foundCount;
					} else {
						VueUtil.alert(String.format(NO_JAVA_CLASS, file.getName()), JAVA_ANALYSIS);
					}
				}
			}

			if (proceedWithAnalysis) {
				messageTextArea.setText(DISPLAYING);
				analyzeButton.setEnabled(false);

				GUI.invokeAfterAWT(new Runnable() { public void run() {
					displayResults();

					allNodes = null;
					newComps = null;
					classHash = null;
					totalFoundCount = 0;

					messageTextArea.setText("");
					analyzeButton.setText(CHOOSE_FILES);
					analyzeButton.setEnabled(true);
				}});
			}
		}
	}


	protected void findNodesOnMap() {
		Iterable<LWComponent>	comps = VUE.getActiveMap().getAllDescendents();

		allNodes = new ArrayList<LWNode>();
		classHash = new Hashtable<String, LWNode>();

		for (LWComponent comp : comps) {
			if (comp instanceof LWNode) {
				String	label = comp.getLabel();

				if (DEBUG) {
					System.out.println("!!!!!!!!!!!! in AnalyzeJava.nodesOnMap(): found " + label + ".");
				}

				allNodes.add((LWNode)comp);
				classHash.put(label, (LWNode)comp);
			}
		}
	}


	protected int parseSourceFile(File file, int mask, double x, double y) {
		// Open and read the file, looking for declarations of Java classes or interfaces.
		int					foundCount = 0;
		boolean				analyzeClasses = (mask & CLASSES_MASK) == CLASSES_MASK,
							analyzeInnerClasses = (mask & INNER_CLASSES_MASK) == INNER_CLASSES_MASK,
							analyzeInterfaces = (mask & INTERFACES_MASK) == INTERFACES_MASK,
							analyzeInnerInterfaces = (mask & INNER_INTERFACES_MASK) == INNER_INTERFACES_MASK;

		try {
			FileReader		stream = new FileReader(file);
			BufferedReader	reader = new BufferedReader(stream);
			StreamTokenizer	tokenizer = new StreamTokenizer(reader);
			int				tokenType;
			String			mainName = file.getName().replace(DOT_JAVA, "");

			if (DEBUG) {
				System.out.println("!!!!!!!!!!!! in AnalyzeJava.parseSourceFile(): analyzing " + file.getName() + " for" +
					(analyzeClasses ? " classes" + (analyzeInnerClasses ? " (including inner classes)" : "") : "") +
					(analyzeInterfaces ? (analyzeClasses ? "," : "") + " interfaces" + ((analyzeInnerInterfaces ? " (including inner interfaces)" : "")) : "") + ".");
			}

			tokenizer.wordChars(DOLLAR_TOKEN, DOLLAR_TOKEN);
			tokenizer.wordChars(UNDERSCORE_TOKEN, UNDERSCORE_TOKEN);
			tokenizer.wordChars('0', '9');
			tokenizer.slashSlashComments(true);
			tokenizer.slashStarComments(true);

			while ((tokenType = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
				if (tokenType == SEMICOLON_TOKEN) {
					// This is the end of a declaration (or statement), so the abstract keyword --
					// if it had been found -- is not applicable for the next declaration.
					isAbstract = false;
				} else if (tokenType == StreamTokenizer.TT_WORD) {
					if (tokenizer.sval.equals(ABSTRACT_KEYWORD)) {
						isAbstract = true;
					} else if (tokenizer.sval.equals(CLASS_KEYWORD)) {
						if (analyzeClasses) {
							if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
								String				className = tokenizer.sval,
													extendsClassName = null;
								ArrayList<String>	implementsInterfaceNames = null;

								if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD &&
									tokenizer.sval.equals(EXTENDS_KEYWORD)) {
									if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
										extendsClassName = tokenizer.sval;
									}
								}

								if (analyzeInterfaces) {
									if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD &&
										tokenizer.sval.equals(IMPLEMENTS_KEYWORD)) {
										boolean		parsingInterfaces = true;

										implementsInterfaceNames = new ArrayList<String>();

										while (parsingInterfaces) {
											if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
												implementsInterfaceNames.add(tokenizer.sval);
											} else {
												parsingInterfaces = false;
											}

											if ((tokenType = tokenizer.nextToken()) != COMMA_TOKEN) {
												parsingInterfaces = false;
											}
										}
									}
								}

								boolean		isInner = !className.equals(mainName);

								if (analyzeInnerClasses || !isInner) {
									newClass(isAbstract, isInner, mainName, className, extendsClassName, implementsInterfaceNames, x, y);
								}
							}
						}

						foundCount++;
						isAbstract = false;
					} else if (tokenizer.sval.equals(INTERFACE_KEYWORD)) {
						if (analyzeInterfaces) {
							if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
								String				interfaceName = tokenizer.sval;
								ArrayList<String>	extendsInterfaceNames = null;

								if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD &&
									tokenizer.sval.equals(EXTENDS_KEYWORD)) {
									boolean		parsingInterfaces = true;

									extendsInterfaceNames = new ArrayList<String>();

									while (parsingInterfaces) {
										if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
											extendsInterfaceNames.add(tokenizer.sval);
										} else {
											parsingInterfaces = false;
										}

										if ((tokenType = tokenizer.nextToken()) != COMMA_TOKEN) {
											parsingInterfaces = false;
										}
									}
								}

								boolean		isInner = !interfaceName.equals(mainName);

								if (analyzeInnerInterfaces || !isInner) {
									newInterface(isAbstract, isInner, mainName, interfaceName, extendsInterfaceNames, x, y);
								}
							}
						}

						foundCount++;
						isAbstract = false;
					}
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		return foundCount;
	}


	protected void displayResults() {
		// Add the nodes and links to the map and apply layouts.
		LWMap		activeMap = VUE.getActiveMap();

		activeMap.addChildren(newComps);
		VUE.getSelection().setTo(allNodes);

		if (totalFoundCount > 1) {
			LayoutAction.circle.act(allNodes);
		}

		LayoutAction.hierarchical.act(allNodes);

		VUE.getActiveViewer().setZoomFit();
		VUE.getUndoManager().mark(JAVA_ANALYSIS);
	}


	protected void newClass(boolean isAbstract, boolean isInner, String mainName, String className,
			String extendsClassName, ArrayList<String> implementsInterfaceNames, double x, double y) {
		if (isInner) {
			className = mainName + "." + className;
		}

		LWNode		classNode = findOrCreateClassNode(className, x, y);

		if (DEBUG) {
			System.out.print("!!!!!!!!!!!! in AnalyzeJava.newClass(): found " +
				(isAbstract ? "abstract " : "") + (isInner ? "inner " : "") +
				"class " + className);
		}

		// This class has now been declared in a file;  set the visual cues to communicate that.
		setIsDeclared(classNode, isAbstract, isInner);

		if (extendsClassName != null) {
			LWNode	extendsNode = null;

			if (isInner) {
				// First see if this inner class might be extending an already-defined inner class.
				extendsNode = classHash.get(mainName + "." + extendsClassName);
			}

			if (extendsNode == null) {
				extendsNode = findOrCreateClassNode(extendsClassName, x, y);
			}

			findOrCreateExtendsLink(classNode, extendsNode);

			if (DEBUG) {
				System.out.print(" extends " + extendsNode.label);
			}
		}

		if (implementsInterfaceNames != null) {
			Iterator<String>	iterator = implementsInterfaceNames.iterator();

			if (DEBUG) {
				System.out.print(" implements ");
			}

			while (iterator.hasNext()) {
				String	implementsInterfaceName = iterator.next();
				LWNode	implementsNode = null;

				if (isInner) {
					// First see if this inner class might be implementing an already-defined inner interface.
					implementsNode = classHash.get(mainName + "." + implementsInterfaceName);
				}

				if (implementsNode == null) {
					implementsNode = findOrCreateInterfaceNode(implementsInterfaceName, x, y);
				}

				findOrCreateImplementsLink(classNode, implementsNode);

				if (DEBUG) {
					System.out.print(implementsNode.label + (iterator.hasNext() ? ", " : ""));
				}
			}
		}

		if (DEBUG) {
			System.out.print(".");
		}
	}


	protected void newInterface(boolean isAbstract, boolean isInner, String mainName, String interfaceName,
			ArrayList<String> extendsInterfaceNames, double x, double y) {
		if (isInner) {
			interfaceName = mainName + "." + interfaceName;
		}

		LWNode		interfaceNode = findOrCreateInterfaceNode(interfaceName, x, y);

		if (DEBUG) {
			System.out.print("!!!!!!!!!!!! in AnalyzeJava.newInterface(): found " +
				(isAbstract ? "abstract " : "") + (isInner ? "inner " : "") +
				"interface " + interfaceName);
		}

		// This interface has now been declared in a file;  set the visual cue to communicate that.
		// Note that declaring an interface abstract is redundant, but it is valid Java syntax.
		setIsDeclared(interfaceNode, isAbstract, isInner);

		if (extendsInterfaceNames != null) {
			Iterator<String>	iterator = extendsInterfaceNames.iterator();

			if (DEBUG) {
				System.out.print(" extends ");
			}

			while (iterator.hasNext()) {
				String	extendsInterfaceName = iterator.next();
				LWNode	extendsNode = null;

				if (isInner) {
					// First see if this inner interface might be extending an already-defined inner interface.
					extendsNode = classHash.get(mainName + "." + extendsInterfaceName);
				}

				if (extendsNode == null) {
					extendsNode = findOrCreateInterfaceNode(extendsInterfaceName, x, y);
				}

				findOrCreateExtendsLink(interfaceNode, extendsNode);

				if (DEBUG) {
					System.out.print(extendsNode.label + (iterator.hasNext() ? ", " : ""));
				}
			}
		}

		if (DEBUG) {
			System.out.print(".");
		}
	}


	protected LWNode findOrCreateClassNode(String className, double x, double y) {
		LWNode		classNode = classHash.get(className);

		if (classNode == null) {
			// Create a node to represent the class and place it in the center of the
			// visible part of the map.
			classNode = newNode(className, x, y, CLASS_SHAPE, CLASS_KEYWORD);
		}

		return classNode;
	}


	protected LWNode findOrCreateInterfaceNode(String interfaceName, double x, double y) {
		LWNode		interfaceNode = classHash.get(interfaceName);

		if (interfaceNode == null) {
			// Create a node to represent the interface and place it in the center of the
			// visible part of the map.
			interfaceNode = newNode(interfaceName, x, y, INTERFACE_SHAPE, INTERFACE_KEYWORD);
		}

		return interfaceNode;
	}


	protected LWNode newNode(String nodeName, double x, double y, Class<? extends RectangularShape> shape, String metadata) {
		LWNode			node = new LWNode(nodeName);
		MetadataList	metadataList = node.getMetadataList();

		node.setShape(shape);
		node.setCenterAt(x, y);

		node.setFillColor(Color.WHITE);
		node.setTextColor(UNDECLARED_COLOR);
		node.setStrokeColor(UNDECLARED_COLOR);
		node.mStrokeStyle.setTo(NONABSTRACT_STYLE);

		metadataList.add(METADATA_CATEGORY, metadata);
		metadataList.add(METADATA_CATEGORY, METADATA_KEYWORD_UNDECLARED);

		node.setToNaturalSize();

		allNodes.add(node);
		newComps.add(node);
		classHash.put(nodeName, node);

		return node;
	}


	protected void setIsDeclared(LWNode node, boolean isAbstract, boolean isInner) {
		node.setStrokeColor(DECLARED_COLOR);
		node.setTextColor(DECLARED_COLOR);

		for (LWLink link : node.getLinks()) {
			if (link.getTail() == node) {
				link.setStrokeColor(DECLARED_COLOR);
				link.setTextColor(DECLARED_COLOR);
			}
		}

		node.getMetadataList().remove(METADATA_CATEGORY, METADATA_KEYWORD_UNDECLARED);

		if (isAbstract) {
			node.mStrokeStyle.setTo(ABSTRACT_STYLE);
			node.getMetadataList().add(METADATA_CATEGORY, ABSTRACT_KEYWORD);
		}

		if (isInner) {
			node.getMetadataList().add(METADATA_CATEGORY, METADATA_KEYWORD_INNER);		
		}
	}


	protected LWLink findOrCreateExtendsLink(LWNode node, LWNode parentNode) {
		return findOrCreateLink(node, parentNode, EXTENDS_KEYWORD);
	}


	protected LWLink findOrCreateImplementsLink(LWNode node, LWNode parentNode) {
		return findOrCreateLink(node, parentNode, IMPLEMENTS_KEYWORD);
	}


	protected LWLink findOrCreateLink(LWNode node, LWNode parentNode, String label) {
		LWLink		link = null;

		for (LWLink existingLink : node.getLinks()) {
			if (existingLink.hasEndpoint(parentNode) && existingLink.getLabel().equals(label)) {
				link = existingLink;
				break;
			}
		}

		if (link == null) {
			Color	parentNodeColor = parentNode.getStrokeColor();

			// Locate the node under its parent to avoid a warning from LWLink code.
			node.setY(parentNode.getY() + (2 * parentNode.getHeight()));

			link = new LWLink(node, parentNode);

			link.setLabel(label);
			link.setStrokeColor(parentNodeColor);
			link.setTextColor(parentNodeColor);

			newComps.add(link);
		}

		return link;
	}


	protected File[] showFileChooser() {
		File				files[] = null;
		VueFileChooser		chooser = VueFileChooser.getVueFileChooser();

		try {
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setFileFilter(new JavaFileFilter());
			chooser.setMultiSelectionEnabled(true);
			chooser.setDialogTitle(CHOOSE_FILES);
			chooser.setApproveButtonText(ANALYZE);
			chooser.setApproveButtonToolTipText(ANALYZE_TOOLTIP);

			if (lastDirectory != null) {
				chooser.setCurrentDirectory(lastDirectory);
			}

			if (chooser.showOpenDialog(VUE.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
				files = chooser.getSelectedFiles();

				File		parentFile = files[0].getParentFile();

				if (parentFile.isDirectory()) {
					lastDirectory = parentFile;
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			chooser.setDialogTitle(DEFAULT_FILE_CHOOSER_TITLE);
			chooser.setApproveButtonText(DEFAULT_FILE_CHOOSER_OPEN_BUTTON);
			chooser.setApproveButtonToolTipText(DEFAULT_FILE_CHOOSER_OPEN_BUTTON_TOOLTIP);
		}

		return files;
	}


	// ActionListener method
	public void actionPerformed(ActionEvent event) {
		JComponent		source = (JComponent) event.getSource();

		if (source == classesCheckBox || source == interfacesCheckBox) {
			analyzeButton.setEnabled(classesCheckBox.isSelected() || interfacesCheckBox.isSelected());

			if (source == classesCheckBox) {
				innerClassesCheckBox.setEnabled(classesCheckBox.isSelected());
			} else if (source == interfacesCheckBox) {
				innerInterfacesCheckBox.setEnabled(interfacesCheckBox.isSelected());
			}
		} else if (source == analyzeButton) {
			int		analysisMask = (classesCheckBox.isSelected() ? CLASSES_MASK : 0) |
						(innerClassesCheckBox.isEnabled() && innerClassesCheckBox.isSelected() ? INNER_CLASSES_MASK : 0) |
						(interfacesCheckBox.isSelected() ? INTERFACES_MASK : 0) |
						(innerInterfacesCheckBox.isEnabled() && innerInterfacesCheckBox.isSelected() ? INNER_INTERFACES_MASK : 0);

			if (analysisThread == null) {
				analysisThread = new AnalysisThread(analysisMask);
				proceedWithAnalysis = true;
				analysisThread.start();
			} else {
				messageTextArea.setText("");
				analyzeButton.setText(CHOOSE_FILES);
				proceedWithAnalysis = false;
				analysisThread.interrupt();
				analysisThread = null;
			}
		}
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


	private class AnalysisThread extends Thread {
		int		analysisMask = 0;


		public AnalysisThread(int analysisMask) {
			super();

			this.analysisMask = analysisMask;
		}


		public void run() {
			try {
				analyze(analysisMask);
			} catch(Exception ex) {
				ex.printStackTrace();
				VueUtil.alert(ex.getMessage(), JAVA_ANALYSIS_ERROR);
			}
			finally
			{
				analysisThread = null;
			}
		}
	}


	protected class JavaFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		// Note that either extending javax.swing.filechooser.FileFilter or implementing
		// java.io.FileFilter should suffice, except that the javax.swing.filechooser.setFileFilter
		// method insists on an argument of type javax.swing.filechooser.FileFilter, which, perversely,
		// doesn't implement FileFilter, and the java.io.File.listFile method more correctly wants as an
		// argument a class that implements FileFilter.  Therefore, this class does both.
		protected static final String		JAVA_FILE_EXTENSION = ".java";


		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(JAVA_FILE_EXTENSION);
		}


		public String getDescription() {
			return JAVA_FILE_EXTENSION;
		}
	}
}
