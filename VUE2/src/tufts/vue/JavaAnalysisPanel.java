package tufts.vue;

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
import javax.swing.filechooser.FileFilter;

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
	protected static final String		ABSTRACT_KEYWORD = "abstract",
										CLASS_KEYWORD = "class",
										INTERFACE_KEYWORD = "interface",
										EXTENDS_KEYWORD = "extends",
										IMPLEMENTS_KEYWORD = "implements",
										METADATA_CATEGORY = "Java Analysis",
										METADATA_KEYWORD_DECLARED = "declared",
										JAVA_ANALYSIS = VueResources.getString("analyze.java.javaAnalysis"),
										HELP_TEXT = VueResources.getString("dockWindow.JavaAnalysis.helpText"),
										FILE_CHOOSER_TITLE_KEY = "FileChooser.openDialogTitleText",
										JAVA_ANALYSIS_FILE_CHOOSER_TITLE = VueResources.getString("analyze.java.openFileTitle"),
										DEFAULT_FILE_CHOOSER_TITLE = VueResources.getString("FileChooser.openDialogTitleText"),
										ANALYZE_CLASSES = VueResources.getString("analyze.java.analyzeClasses"),
										ANALYZE_INTERFACES = VueResources.getString("analyze.java.analyzeInterfaces"),
										ANALYZE = VueResources.getString("analyze.java.analyze"),
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
										INTERFACES_MASK = 2;
	protected static final int			HALF_GUTTER = 4;
	protected static final Insets		HALF_GUTTER_INSETS = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);
	protected static DockWindow			dock = null;
	protected static final boolean		DEBUG = false;

	protected JPanel					contentPanel = null;
	protected JCheckBox					classesCheckBox = null,
										interfacesCheckBox = null;
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
		interfacesCheckBox = new JCheckBox(ANALYZE_INTERFACES);
		messageTextArea = new JTextArea();
		analyzeButton = new JButton(ANALYZE);

		classesCheckBox.setSelected(true);
		interfacesCheckBox.setSelected(true);

		classesCheckBox.addActionListener(this);
		interfacesCheckBox.addActionListener(this);
		analyzeButton.addActionListener(this);

		classesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		interfacesCheckBox.setFont(tufts.vue.gui.GUI.LabelFace);
		messageTextArea.setFont(tufts.vue.gui.GUI.LabelFace);
		analyzeButton.setFont(tufts.vue.gui.GUI.LabelFace);

		messageTextArea.setBackground(contentPanel.getBackground());

		addToGridBag(contentPanel, classesCheckBox,    0, 0, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, interfacesCheckBox, 0, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, messageTextArea,    0, 2, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.BOTH, 1.0, 1.0, HALF_GUTTER_INSETS);
		addToGridBag(contentPanel, analyzeButton,      0, 3, 1, 1, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, 0.0, 0.0, HALF_GUTTER_INSETS);
		addToGridBag(this, contentPanel,               0, 0, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.BOTH, 1.0, 1.0, HALF_GUTTER_INSETS);

		if (DEBUG) {
			contentPanel.setBackground(Color.CYAN);
			classesCheckBox.setBackground(Color.MAGENTA);
			classesCheckBox.setOpaque(true);
			interfacesCheckBox.setBackground(Color.MAGENTA);
			interfacesCheckBox.setOpaque(true);
			messageTextArea.setBackground(Color.MAGENTA);
			analyzeButton.setBackground(Color.MAGENTA);
			analyzeButton.setOpaque(true);
		}
	}


	public void finalize() {
		contentPanel = null;
		classesCheckBox = null;
		interfacesCheckBox = null;
		analyzeButton = null;
		lastDirectory = null;
		allNodes = null;
		newComps = null;
		classHash = null;
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
				int		foundCount = 0;

				foundCount = parseSourceFile(file, mask, viewerCenterX, viewerCenterY);

				if (foundCount > 0) {
					totalFoundCount += foundCount;
				} else {
					VueUtil.alert(String.format(NO_JAVA_CLASS, file.getName()), JAVA_ANALYSIS);
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
					analyzeButton.setText(ANALYZE);
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
							analyzeInterfaces = (mask & INTERFACES_MASK) == INTERFACES_MASK;

		try {
			FileReader		stream = new FileReader(file);
			BufferedReader	reader = new BufferedReader(stream);
			StreamTokenizer	tokenizer = new StreamTokenizer(reader);
			int				tokenType;

			if (DEBUG) {
				System.out.println("!!!!!!!!!!!! in AnalyzeJava.parseSourceFile(): analyzing " + file.getName() + ".");
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

								newClass(isAbstract, className, extendsClassName, implementsInterfaceNames, x, y);
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

								newInterface(isAbstract, interfaceName, extendsInterfaceNames, x, y);
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


	protected void newClass(boolean isAbstract, String className, String extendsClassName, ArrayList<String> implementsClassNames,
		double x, double y) {
		LWNode		classNode = findOrCreateClassNode(className, x, y);

		if (DEBUG) {
			System.out.print("!!!!!!!!!!!! in AnalyzeJava.parseSourceFile(): found " +
				(isAbstract ? "abstract " : "") + "class " + className);
		}

		// This class has now been declared in a file;  set the visual cues to communicate that.
		setIsDeclared(classNode, isAbstract);

		if (extendsClassName != null) {
			LWNode	extendsNode = findOrCreateClassNode(extendsClassName, x, y);

			findOrCreateExtendsLink(classNode, extendsNode);

			if (DEBUG) {
				System.out.print(" extends " + extendsClassName);
			}
		}

		if (implementsClassNames != null) {
			Iterator<String>	iterator = implementsClassNames.iterator();

			if (DEBUG) {
				System.out.print(" implements ");
			}

			while (iterator.hasNext()) {
				String	implementsClassName= iterator.next();
				LWNode	implementsNode = findOrCreateInterfaceNode(implementsClassName, x, y);

				findOrCreateImplementsLink(classNode, implementsNode);

				if (DEBUG) {
					System.out.print(implementsClassName + (iterator.hasNext() ? ", " : "."));
				}
			}
		}
	}


	protected void newInterface(boolean isAbstract, String interfaceName, ArrayList<String> extendsInterfaceNames,
		double x, double y) {
		LWNode		interfaceNode = findOrCreateInterfaceNode(interfaceName, x, y);

		if (DEBUG) {
			System.out.print("!!!!!!!!!!!! in AnalyzeJava.parseSourceFile(): found " +
				(isAbstract ? "abstract " : "") + "interface " + interfaceName);
		}

		// This interface has now been declared in a file;  set the visual cue to communicate that.
		// Note that declaring an interface abstract is redundant, but it is valid Java syntax.
		setIsDeclared(interfaceNode, isAbstract);

		if (extendsInterfaceNames != null) {
			Iterator<String>	iterator = extendsInterfaceNames.iterator();

			if (DEBUG) {
				System.out.print(" extends ");
			}

			while (iterator.hasNext()) {
				String	extendsInterfaceName = iterator.next();
				LWNode	extendsNode = findOrCreateInterfaceNode(extendsInterfaceName, x, y);

				findOrCreateExtendsLink(interfaceNode, extendsNode);

				if (DEBUG) {
					System.out.print(extendsInterfaceName + (iterator.hasNext() ? ", " : "."));
				}
			}
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
		LWNode		node = new LWNode(nodeName);

		node.setShape(shape);
		node.setCenterAt(x, y);

		node.setFillColor(Color.WHITE);
		node.setTextColor(UNDECLARED_COLOR);
		node.setStrokeColor(UNDECLARED_COLOR);
		node.mStrokeStyle.setTo(NONABSTRACT_STYLE);

		node.getMetadataList().add(METADATA_CATEGORY, metadata);

		node.setToNaturalSize();

		allNodes.add(node);
		newComps.add(node);
		classHash.put(nodeName, node);

		return node;
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


	protected void setIsDeclared(LWNode node, boolean isAbstract) {
		node.setStrokeColor(DECLARED_COLOR);
		node.setTextColor(DECLARED_COLOR);

		for (LWLink link : node.getLinks()) {
			if (link.getTail() == node) {
				link.setStrokeColor(DECLARED_COLOR);
				link.setTextColor(DECLARED_COLOR);
			}
		}

		node.getMetadataList().add(METADATA_CATEGORY, METADATA_KEYWORD_DECLARED);

		if (isAbstract) {
			node.mStrokeStyle.setTo(ABSTRACT_STYLE);
			node.getMetadataList().add(METADATA_CATEGORY, ABSTRACT_KEYWORD);
			isAbstract = false;
		}
	}


	protected File[] showFileChooser() {
		File				files[] = null;

		javax.swing.UIManager.put(FILE_CHOOSER_TITLE_KEY, JAVA_ANALYSIS_FILE_CHOOSER_TITLE);

		try {
			VueFileChooser	chooser = VueFileChooser.getVueFileChooser();

			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(new JavaFileFilter());
			chooser.setMultiSelectionEnabled(true);

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
			javax.swing.UIManager.put(FILE_CHOOSER_TITLE_KEY, DEFAULT_FILE_CHOOSER_TITLE);
		}

		return files;
	}


	// ActionListener method
	public void actionPerformed(ActionEvent event) {
		JComponent		source = (JComponent) event.getSource();

		if (source == classesCheckBox || source == interfacesCheckBox) {
			analyzeButton.setEnabled(classesCheckBox.isSelected() || interfacesCheckBox.isSelected());
		} else if (source == analyzeButton) {
			int		analysisMask = (classesCheckBox.isSelected() ? CLASSES_MASK : 0) |
						(interfacesCheckBox.isSelected() ? INTERFACES_MASK : 0);

			if (analysisThread == null) {
				analysisThread = new AnalysisThread(analysisMask);
				proceedWithAnalysis = true;
				analysisThread.start();
			} else {
				messageTextArea.setText("");
				analyzeButton.setText(ANALYZE);
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


	protected class JavaFileFilter extends FileFilter {
		protected static final String		JAVA_FILE_EXTENSION = ".java";

		public boolean accept(File file) {
			return file.getName().toLowerCase().endsWith(JAVA_FILE_EXTENSION);
		}


		public String getDescription() {
			return JAVA_FILE_EXTENSION;
		}
	}
}

