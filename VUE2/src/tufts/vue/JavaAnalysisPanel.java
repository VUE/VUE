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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
	protected static final org.apache.log4j.Logger
										Log = org.apache.log4j.Logger.getLogger(JavaAnalysisPanel.class);
	protected static final String		PUBLIC_KEYWORD = "public",
										ABSTRACT_KEYWORD = "abstract",
										FINAL_KEYWORD = "final",
										CLASS_KEYWORD = "class",
										INTERFACE_KEYWORD = "interface",
										EXTENDS_KEYWORD = "extends",
										IMPLEMENTS_KEYWORD = "implements",
										METADATA_CATEGORY = "Java Analysis",
										METADATA_KEYWORD_UNDECLARED = "undeclared",
										METADATA_KEYWORD_DECLARED = "declared in ",
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
										ABSTRACT_STROKE_STYLE = LWComponent.StrokeStyle.DASHED,
										NONABSTRACT_STROKE_STYLE = LWComponent.StrokeStyle.SOLID;
	protected static final float		FINAL_STROKE_WIDTH = 2f,
										NONFINAL_STROKE_WIDTH = 1f;
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
										INNER_INTERFACES_MASK = 8,
										HALF_GUTTER = 4;
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
	protected boolean					proceedWithAnalysis = true;
	AnalysisThread						analysisThread = null;
	FileFilter							currentFileFilter = null;


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
		currentFileFilter = null;
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
				Log.info("analyzing " + fileCount + " file" + (fileCount == 1 ? "." : "s."));
			}

			newComps = new ArrayList<LWComponent>();
			findNodesOnMap();
			totalFoundCount = 0;

			messageTextArea.setText(PARSING);

			for (fileIndex = 0; fileIndex < fileCount && proceedWithAnalysis; fileIndex++) {
				File	file = files[fileIndex];
				int		foundCount;

				if (file.isDirectory()) {
					File	filesInFolder[] = file.listFiles(currentFileFilter);
					int		fileInFolderCount = filesInFolder.length,
							fileInFolderIndex;

					for (fileInFolderIndex = 0; fileInFolderIndex < fileInFolderCount && proceedWithAnalysis; fileInFolderIndex++) {
						File	fileInFolder = filesInFolder[fileInFolderIndex];

						if (!fileInFolder.isDirectory()) {
							foundCount = parseFile(fileInFolder, mask, viewerCenterX, viewerCenterY);

							if (foundCount > 0) {
								totalFoundCount += foundCount;
							} else {
								VueUtil.alert(String.format(NO_JAVA_CLASS, fileInFolder.getName()), JAVA_ANALYSIS);
							}
						}
					}
				}
				else {
					foundCount = parseFile(file, mask, viewerCenterX, viewerCenterY);

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
					Log.info("nodesOnMap() found " + label + ".");
				}

				allNodes.add((LWNode)comp);
				classHash.put(label, (LWNode)comp);
			}
		}
	}


	protected int parseFile(File file, int mask, double x, double y) {
		int		foundCount = 0;
		String	filename = file.getName();

		try {
			if (filename.endsWith(JavaSourceFileFilter.FILE_EXTENSION)) {
				foundCount = parseSourceFile(file, filename, mask, x, y);
			} else if (filename.endsWith(JavaClassFileFilter.FILE_EXTENSION)) {
				FileInputStream		stream = new FileInputStream(file);
				DataInputStream		dataStream = new DataInputStream(stream);

				foundCount = parseClassFile(dataStream, filename, mask, x, y);
			} else if (filename.endsWith(JavaArchiveFileFilter.FILE_EXTENSION)) {
				JarFile					jar = new JarFile(file);
				Enumeration<JarEntry>	jarEntries = jar.entries();

				while (jarEntries.hasMoreElements()) {
					try {
						JarEntry	jarEntry = jarEntries.nextElement();
						String		name = jarEntry.getName();

						if (name.endsWith(JavaClassFileFilter.FILE_EXTENSION)) {
							InputStream			stream = jar.getInputStream(jarEntry);
							DataInputStream		dataStream = new DataInputStream(stream);

							foundCount += parseClassFile(dataStream, name, mask, x, y);
						}
					} catch(Exception ex) {
						ex.printStackTrace();
						VueUtil.alert(ex.getMessage(), JAVA_ANALYSIS_ERROR);
					}
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			VueUtil.alert(ex.getMessage(), JAVA_ANALYSIS_ERROR);
		}

		return foundCount;
	}


	protected int parseSourceFile(File file, String filename, int mask, double x, double y) throws java.io.IOException {
		// Open and read the file, looking for declarations of Java classes or interfaces.
		int					foundCount = 0;
		boolean				analyzeClasses = (mask & CLASSES_MASK) == CLASSES_MASK,
							analyzeInnerClasses = (mask & INNER_CLASSES_MASK) == INNER_CLASSES_MASK,
							analyzeInterfaces = (mask & INTERFACES_MASK) == INTERFACES_MASK,
							analyzeInnerInterfaces = (mask & INNER_INTERFACES_MASK) == INNER_INTERFACES_MASK,
							isPublic = false,
							isAbstract = false,
							isFinal = false;

		FileReader		stream = new FileReader(file);
		BufferedReader	reader = new BufferedReader(stream);
		StreamTokenizer	tokenizer = new StreamTokenizer(reader);
		int				tokenType;
		String			sourceName = filename.replace(JavaSourceFileFilter.FILE_EXTENSION, "");

		if (DEBUG) {
			Log.info("parseSourceFile() analyzing " + filename + " for" +
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
				// This is the end of a declaration (or statement), so the public, abstract and final
				// keywords -- if they had been found -- are not applicable for the next declaration.
				isPublic = false;
				isAbstract = false;
				isFinal = false;
			} else if (tokenType == StreamTokenizer.TT_WORD) {
				if (tokenizer.sval.equals(PUBLIC_KEYWORD)) {
					isPublic = true;
				} else if (tokenizer.sval.equals(ABSTRACT_KEYWORD)) {
					isAbstract = true;
				} else if (tokenizer.sval.equals(FINAL_KEYWORD)) {
					isFinal = true;
				} else if (tokenizer.sval.equals(CLASS_KEYWORD)) {
					if (analyzeClasses) {
						if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
							String				className = tokenizer.sval,
												extendsClassName = null;
							ArrayList<String>	implementsInterfaceNames = null;

							if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
								if (tokenizer.sval.equals(EXTENDS_KEYWORD)) {
									if ((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD) {
										extendsClassName = tokenizer.sval;
									}
								}

								if (analyzeInterfaces) {
									// Look at the current token first to see if it is "implements", which could happen
									// in the absence of an "extents" keyword.  If that's not the case, also look at the
									// next token to see if it is "implements".
									if ((tokenType == StreamTokenizer.TT_WORD &&
										tokenizer.sval.equals(IMPLEMENTS_KEYWORD)) || 
										((tokenType = tokenizer.nextToken()) == StreamTokenizer.TT_WORD &&
										tokenizer.sval.equals(IMPLEMENTS_KEYWORD))) {
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
							}

							boolean		isNotInner = className.equals(sourceName);

							if (analyzeInnerClasses || isNotInner) {
								newClass(isPublic, isAbstract, isFinal, (isNotInner ? null : sourceName), className, extendsClassName, implementsInterfaceNames, x, y);
							}
						}
					}

					foundCount++;
					isPublic = false;
					isAbstract = false;
					isFinal = false;
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

							boolean		isNotInner = interfaceName.equals(sourceName);

							if (analyzeInnerInterfaces || isNotInner) {
								newInterface(isPublic, isAbstract, isFinal, (isNotInner ? null : sourceName), interfaceName, extendsInterfaceNames, x, y);
							}
						}
					}

					foundCount++;
					isPublic = false;
					isAbstract = false;
					isFinal = false;
				}
			}
		}

		return foundCount;
	}


	protected int parseClassFile(DataInputStream stream, String filename, int mask, double x, double y) throws java.io.IOException {
		// Open and read the file, looking for declarations of Java classes or interfaces.
		int					foundCount = 0;
		boolean				analyzeClasses = (mask & CLASSES_MASK) == CLASSES_MASK,
							analyzeInnerClasses = (mask & INNER_CLASSES_MASK) == INNER_CLASSES_MASK,
							analyzeInterfaces = (mask & INTERFACES_MASK) == INTERFACES_MASK,
							analyzeInnerInterfaces = (mask & INNER_INTERFACES_MASK) == INNER_INTERFACES_MASK;

		if (DEBUG) {
			Log.info("parseClassFile() analyzing " + filename + " for" +
				(analyzeClasses ? " classes" + (analyzeInnerClasses ? " (including inner classes)" : "") : "") +
				(analyzeInterfaces ? (analyzeClasses ? "," : "") + " interfaces" + ((analyzeInnerInterfaces ? " (including inner interfaces)" : "")) : "") + ".");
		}

		ClassInfo			classInfo = new ClassInfo(stream);

		if (classInfo.isJavaClass) {
			ConstantPool	pool = new ConstantPool(stream);
			AccessFlags		accessFlags = new AccessFlags(stream);
			ClassRef		classRef = new ClassRef(stream, pool);
			SuperclassRef	superclassRef = new SuperclassRef(stream, pool);
			Interfaces		interfaces = new Interfaces(stream, pool);
			FieldInfo		fieldInfo = new FieldInfo(stream);
			MethodInfo		methodInfo = new MethodInfo(stream);
			AttributeInfo	attributeInfo = new AttributeInfo(stream, pool);

			String			classNameWithoutPackage = classRef.nameWithoutPackage,
							sourceName = attributeInfo.sourceName;
			boolean			isNotInner = classNameWithoutPackage.equals(sourceName);

			if (accessFlags.isInterface) {
				if (analyzeInterfaces && (analyzeInnerInterfaces || isNotInner)) {
					newInterface(accessFlags.isPublic, accessFlags.isAbstract, accessFlags.isFinal,
						(isNotInner ? null : sourceName), classRef.name, interfaces.names, x, y);
				}
			} else {
				if (analyzeClasses && (analyzeInnerClasses || isNotInner)) {
					newClass(accessFlags.isPublic, accessFlags.isAbstract, accessFlags.isFinal,
						(isNotInner ? null : sourceName), classRef.name, superclassRef.name,
						(analyzeInterfaces ? interfaces.names : null), x, y);
				}
			}

			foundCount = 1;
		}

		return foundCount;
	}


	protected void displayResults() {
		// Add the nodes and links to the map and apply layouts.
		LWMap		activeMap = VUE.getActiveMap();

		activeMap.addChildren(newComps);

		if (allNodes.size() > 0) {
			LayoutAction.circle.act(allNodes);
			LayoutAction.hierarchical.act(allNodes);
		}

		VUE.getSelection().setTo(allNodes);
		VUE.getActiveViewer().setZoomFit();

		VUE.getSelection().setTo(newComps);

		VUE.getUndoManager().mark(JAVA_ANALYSIS);
	}


	protected void newClass(boolean isPublic, boolean isAbstract, boolean isFinal, String sourceName, String className,
			String extendsClassName, ArrayList<String> implementsInterfaceNames, double x, double y) {
		StringBuffer	debugMessage = null;

		LWNode		classNode = findOrCreateClassNode(className, x, y);

		if (DEBUG) {
			debugMessage = new StringBuffer("newClass() found " +
				(isPublic ? "public " : "") + (isAbstract ? "abstract " : "") + (isFinal ? "final " : "") +
				"class " + className);
		}

		// This class has now been declared in a file;  set the visual cues to communicate that, and add metadata.
		setIsDeclared(classNode, isPublic, isAbstract, isFinal, sourceName);

		if (extendsClassName != null) {
			LWNode	extendsNode = findOrCreateClassNode(extendsClassName, x, y);

			findOrCreateExtendsLink(classNode, extendsNode);

			if (DEBUG) {
				debugMessage.append(" extends " + extendsNode.label);
			}
		}

		if (implementsInterfaceNames != null) {
			Iterator<String>	iterator = implementsInterfaceNames.iterator();

			if (DEBUG) {
				debugMessage.append(" implements ");
			}

			while (iterator.hasNext()) {
				String	implementsInterfaceName = iterator.next();
				LWNode	implementsNode = findOrCreateInterfaceNode(implementsInterfaceName, x, y);

				findOrCreateImplementsLink(classNode, implementsNode);

				if (DEBUG) {
					debugMessage.append(implementsNode.label + (iterator.hasNext() ? ", " : ""));
				}
			}
		}

		if (DEBUG) {
			debugMessage.append(sourceName != null ? "(declared in " + sourceName + ")." : ".");
			Log.info(debugMessage.toString());
		}
	}


	protected void newInterface(boolean isPublic, boolean isAbstract, boolean isFinal, String sourceName, String interfaceName,
			ArrayList<String> extendsInterfaceNames, double x, double y) {
		StringBuffer	debugMessage = null;

		LWNode		interfaceNode = findOrCreateInterfaceNode(interfaceName, x, y);

		if (DEBUG) {
			debugMessage = new StringBuffer("newInterface() found " +
				(isPublic ? "public " : "") + (isAbstract ? "abstract " : "") + (isFinal ? "final " : "") +
				"interface " + interfaceName);
		}

		// This interface has now been declared in a file;  set the visual cue to communicate that.
		// Note that declaring an interface abstract is redundant, but it is valid Java syntax.
		setIsDeclared(interfaceNode, isPublic, isAbstract, isFinal, sourceName);

		if (extendsInterfaceNames != null) {
			Iterator<String>	iterator = extendsInterfaceNames.iterator();

			if (DEBUG) {
				debugMessage.append(" extends ");
			}

			while (iterator.hasNext()) {
				String	extendsInterfaceName = iterator.next();
				LWNode	extendsNode = findOrCreateInterfaceNode(extendsInterfaceName, x, y);

				findOrCreateExtendsLink(interfaceNode, extendsNode);

				if (DEBUG) {
					debugMessage.append(extendsNode.label + (iterator.hasNext() ? ", " : ""));
				}
			}
		}

		if (DEBUG) {
			debugMessage.append(sourceName != null ? "(declared in " + sourceName + ")." : ".");
			Log.info(debugMessage.toString());
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
		node.mStrokeStyle.setTo(NONABSTRACT_STROKE_STYLE);
		node.mStrokeWidth.setTo(NONFINAL_STROKE_WIDTH);

		metadataList.add(METADATA_CATEGORY, metadata);
		metadataList.add(METADATA_CATEGORY, METADATA_KEYWORD_UNDECLARED);

		node.setToNaturalSize();

		allNodes.add(node);
		newComps.add(node);
		classHash.put(nodeName, node);

		return node;
	}


	protected void setIsDeclared(LWNode node, boolean isPublic, boolean isAbstract, boolean isFinal, String sourceName) {
		node.setStrokeColor(DECLARED_COLOR);
		node.setTextColor(DECLARED_COLOR);

		for (LWLink link : node.getLinks()) {
			if (link.getTail() == node) {
				link.setStrokeColor(DECLARED_COLOR);
				link.setTextColor(DECLARED_COLOR);
			}
		}

		MetadataList	metadataList = node.getMetadataList();

		metadataList.remove(METADATA_CATEGORY, METADATA_KEYWORD_UNDECLARED);

		if (isPublic && !metadataList.contains(METADATA_CATEGORY, PUBLIC_KEYWORD)) {
			metadataList.add(METADATA_CATEGORY, PUBLIC_KEYWORD);		
		}

		if (isAbstract && !metadataList.contains(METADATA_CATEGORY, ABSTRACT_KEYWORD)) {
			node.mStrokeStyle.setTo(ABSTRACT_STROKE_STYLE);
			metadataList.add(METADATA_CATEGORY, ABSTRACT_KEYWORD);
		}

		if (isFinal && !metadataList.contains(METADATA_CATEGORY, FINAL_KEYWORD)) {
			node.mStrokeWidth.setTo(FINAL_STROKE_WIDTH);
			metadataList.add(METADATA_CATEGORY, FINAL_KEYWORD);
		}

		if (sourceName != null) {
			metadataList.add(METADATA_CATEGORY, METADATA_KEYWORD_DECLARED + sourceName);
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
			chooser.setFileFilter(new JavaSourceFileFilter());
			chooser.setFileFilter(new JavaClassFileFilter());
			chooser.setFileFilter(new JavaArchiveFileFilter());
			chooser.setMultiSelectionEnabled(true);
			chooser.setDialogTitle(CHOOSE_FILES);
			chooser.setApproveButtonText(ANALYZE);
			chooser.setApproveButtonToolTipText(ANALYZE_TOOLTIP);

			if (lastDirectory != null) {
				chooser.setCurrentDirectory(lastDirectory);
			}

			if (chooser.showOpenDialog(VUE.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
				files = chooser.getSelectedFiles();
				currentFileFilter = (FileFilter)chooser.getFileFilter();

				File		parentFile = files[0].getParentFile();

				if (parentFile.isDirectory()) {
					lastDirectory = parentFile;
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			VueUtil.alert(ex.getMessage(), JAVA_ANALYSIS_ERROR);
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


	private class ClassInfo {
		protected static final int	JAVA_CLASS_FILE_MAGIC_NUMBER = 0xCAFEBABE;
		public boolean				isJavaClass = false;

		ClassInfo(DataInputStream stream) throws java.io.IOException {
			int					magicNumber = stream.readInt();

			isJavaClass = magicNumber == JAVA_CLASS_FILE_MAGIC_NUMBER;

			if (DEBUG) {
				Log.info("magicNumber " + magicNumber + " is " + (isJavaClass ? "" : "NOT ") + "java magic number.");
			}

			if (isJavaClass) {
				short			compilerMinorVersion = stream.readShort(),
								compilerMajorVersion = stream.readShort();
				if (DEBUG) {
					Log.info("compilerMinorVersion is " + compilerMinorVersion);
					Log.info("compilerMajorVersion is " + compilerMajorVersion);
				}
			}
		}
	}


	private class ConstantPool {
		public ConstantPoolItem			items[];

		ConstantPool(DataInputStream stream) throws java.io.IOException {
			short	constantPoolCount = (short)(stream.readShort() - 1);

			if (DEBUG) {
				Log.info("constantPoolCount is " + constantPoolCount);
			}

			// Indecies into the constant pool are one-based, so make the array
			// one larger than the number of items and put the first item at [1].
			items = new ConstantPoolItem[constantPoolCount + 1];

			for (int index = 1; index <= constantPoolCount; index++) {
				byte				type = stream.readByte();
				ConstantPoolItem	newItem = null;

				switch (type) {
				case 1:
					newItem = new ConstantPoolItemUTF8(stream);
					break;

				case 2:
					newItem = new ConstantPoolItemUnicode(stream);
					break;

				case 3:
					newItem = new ConstantPoolItemInteger(stream);
					break;

				case 4:
					newItem = new ConstantPoolItemFloat(stream);
					break;

				case 5:
					newItem = new ConstantPoolItemLong(stream);
					break;

				case 6:
					newItem = new ConstantPoolItemDouble(stream);
					break;

				case 7:
					newItem = new ConstantPoolItemClass(stream);
					break;

				case 8:
					newItem = new ConstantPoolItemString(stream);
					break;

				case 9:
					newItem = new ConstantPoolItemFieldRef(stream);
					break;

				case 10:
					newItem = new ConstantPoolItemMethodRef(stream);
					break;

				case 11:
					newItem = new ConstantPoolItemInterfaceMethodRef(stream);
					break;

				case 12:
					newItem = new ConstantPoolItemNameAndType(stream);
					break;

				default:
					VueUtil.alert("Unknown constant pool type: '" + type + "'", JAVA_ANALYSIS_ERROR);
				}

				items[index] = newItem;

				if (DEBUG) {
					Log.info("          ConstantPool[" + index + "] (type " + type + ") is " + newItem.toString());
				}

				if (type == 5 || type == 6) {
					// Long and double items are entered in the constant pool twice.
					// It's totally unclear why this is so.  That's just the way it is.
					index++;
					items[index] = newItem;
				}
			}
		}
	}


	private abstract class ConstantPoolItem {
	}


	private class ConstantPoolItemUTF8 extends ConstantPoolItem {
		public String	value = null;

		ConstantPoolItemUTF8(DataInputStream stream) throws java.io.IOException {
			short	length = stream.readShort();
			byte	bytes[] = new byte[length];

			for (int index = 0; index < length; index++) {
				bytes[index] = stream.readByte();
			}

			value = new String(bytes);
		}


		public String toString() {
			return value;
		}
	}


	private class ConstantPoolItemUnicode extends ConstantPoolItem {
		public String	value = null;

		ConstantPoolItemUnicode(DataInputStream stream) throws java.io.IOException {
			short	length = stream.readShort();
			byte	bytes[] = new byte[length];

			for (int index = 0; index < length; index++) {
				bytes[index] = stream.readByte();
			}

			value = new String(bytes);
		}


		public String toString() {
			return value;
		}
	}


	private class ConstantPoolItemInteger extends ConstantPoolItem {
		public int		value;

		ConstantPoolItemInteger(DataInputStream stream) throws java.io.IOException {
			value = stream.readInt();
		}


		public String toString() {
			return Integer.toString(value);
		}
	}


	private class ConstantPoolItemFloat extends ConstantPoolItem {
		public float	value;

		ConstantPoolItemFloat(DataInputStream stream) throws java.io.IOException {
			value = stream.readFloat();
		}


		public String toString() {
			return Float.toString(value);
		}
	}


	private class ConstantPoolItemLong extends ConstantPoolItem {
		public long	value;

		ConstantPoolItemLong(DataInputStream stream) throws java.io.IOException {
			value = stream.readLong();
		}


		public String toString() {
			return Long.toString(value);
		}
	}


	private class ConstantPoolItemDouble extends ConstantPoolItem {
		public double	value;

		ConstantPoolItemDouble(DataInputStream stream) throws java.io.IOException {
			value = stream.readDouble();
		}


		public String toString() {
			return Double.toString(value);
		}
	}


	private class ConstantPoolItemClass extends ConstantPoolItem {
		public short	nameIndex;

		ConstantPoolItemClass(DataInputStream stream) throws java.io.IOException {
			nameIndex = stream.readShort();
		}


		public String toString() {
			return Short.toString(nameIndex);
		}
	}


	private class ConstantPoolItemString extends ConstantPoolItem {
		public short	stringIndex;

		ConstantPoolItemString(DataInputStream stream) throws java.io.IOException {
			stringIndex = stream.readShort();
		}


		public String toString() {
			return Short.toString(stringIndex);
		}
	}


	private class ConstantPoolItemFieldRef extends ConstantPoolItem {
		public short	classIndex,
						nameAndTypeIndex;

		ConstantPoolItemFieldRef(DataInputStream stream) throws java.io.IOException {
			classIndex = stream.readShort();
			nameAndTypeIndex = stream.readShort();
		}


		public String toString() {
			return "classIndex " + Short.toString(classIndex) + ", nameAndTypeIndex " + Short.toString(nameAndTypeIndex);
		}
	}


	private class ConstantPoolItemMethodRef extends ConstantPoolItem {
		public short	classIndex,
						nameAndTypeIndex;

		ConstantPoolItemMethodRef(DataInputStream stream) throws java.io.IOException {
			classIndex = stream.readShort();
			nameAndTypeIndex = stream.readShort();
		}


		public String toString() {
			return "classIndex " + Short.toString(classIndex) + ", nameAndTypeIndex " + Short.toString(nameAndTypeIndex);
		}
	}


	private class ConstantPoolItemInterfaceMethodRef extends ConstantPoolItem {
		public short	classIndex,
						nameAndTypeIndex;

		ConstantPoolItemInterfaceMethodRef(DataInputStream stream) throws java.io.IOException {
			classIndex = stream.readShort();
			nameAndTypeIndex = stream.readShort();
		}


		public String toString() {
			return "classIndex " + Short.toString(classIndex) + ", nameAndTypeIndex " + Short.toString(nameAndTypeIndex);
		}
	}


	private class ConstantPoolItemNameAndType extends ConstantPoolItem {
		public short	nameIndex,
						descriptorIndex;

		ConstantPoolItemNameAndType(DataInputStream stream) throws java.io.IOException {
			nameIndex = stream.readShort();
			descriptorIndex = stream.readShort();
		}


		public String toString() {
			return "nameIndex " + Short.toString(nameIndex) + ", descriptorIndex " + Short.toString(descriptorIndex);
		}
	}


	private class AccessFlags {
		protected static final int	ACC_PUBLIC      = 0x0001,
									ACC_FINAL       = 0x0010,
									ACC_SUPER       = 0x0020,
									ACC_INTERFACE   = 0x0200,
									ACC_ABSTRACT    = 0x0400;
		public boolean				isInterface,
									isPublic,
									isAbstract,
									isFinal;

		AccessFlags(DataInputStream stream) throws java.io.IOException {
			short			accessFlags = stream.readShort();

			isInterface = (accessFlags & ACC_INTERFACE) != 0;
			isPublic = (accessFlags & ACC_PUBLIC) != 0;
			isAbstract = (accessFlags & ACC_ABSTRACT) != 0;
			isFinal = (accessFlags & ACC_FINAL) != 0;

			if (DEBUG) {
				Log.info("isInterface is " + (isInterface ? "true" : "false"));
				Log.info("isPublic is " + (isPublic ? "true" : "false"));
				Log.info("isAbstract is " + (isAbstract ? "true" : "false"));
				Log.info("isFinal is " + (isFinal ? "true" : "false"));
			}
		}
	}


	private class ClassRef {
		public String	name = null,
						nameWithoutPackage = null;

		ClassRef(DataInputStream stream, ConstantPool pool) throws java.io.IOException {
			short classIndex = stream.readShort();

			ConstantPoolItemClass	classItem = (ConstantPoolItemClass)pool.items[classIndex];
			ConstantPoolItemUTF8	classNameItem =	(ConstantPoolItemUTF8)pool.items[classItem.nameIndex];

			name = classNameItem.value.replace('/', '.');

			int		packageNameLength = name.lastIndexOf('.') + 1;

			nameWithoutPackage = (packageNameLength > 0 ? name.substring(packageNameLength) : name);

			if (DEBUG) {
				Log.info("Class index is " + classIndex + " and name is " + name);
			}
		}
	}


	private class SuperclassRef {
		public String	name = null;

		SuperclassRef(DataInputStream stream, ConstantPool pool) throws java.io.IOException {
			short classIndex = stream.readShort();

			ConstantPoolItemClass	classItem = (ConstantPoolItemClass)pool.items[classIndex];
			ConstantPoolItemUTF8	classNameItem =	(ConstantPoolItemUTF8)pool.items[classItem.nameIndex];

			name = classNameItem.value.replace('/', '.');

			if (DEBUG) {
				Log.info("Superclass index is " + classIndex + " and name is " + name);
			}
		}
	}


	private class Interfaces {
		public ArrayList<String>	names = null;

		Interfaces(DataInputStream stream, ConstantPool pool) throws java.io.IOException {
			short			interfaceCount = stream.readShort();

			if (DEBUG) {
				Log.info("interfaceCount is " + interfaceCount);
			}

			if (interfaceCount > 0) {
				names = new ArrayList<String>(interfaceCount);

				for (int index = 0; index < interfaceCount; index++) {
					short					interfaceIndex = stream.readShort();
					ConstantPoolItemClass	interfaceItem = (ConstantPoolItemClass)pool.items[interfaceIndex];
					ConstantPoolItemUTF8	interfaceNameItem =	(ConstantPoolItemUTF8)pool.items[interfaceItem.nameIndex];
					String					interfaceName = interfaceNameItem.value.replace('/', '.');

					names.add(index, interfaceName);

					if (DEBUG) {
						Log.info("interfaceName[" + index + "] is " + interfaceName);
					}
				}
			}
		}
	}


	private class FieldInfo {
		// Note that field info is currently unused by JavaAnalysisPanel, so it's read
		// and discarded in order to get to the AttributeInfo that is needed.

		FieldInfo(DataInputStream stream) throws java.io.IOException {
			short			fieldCount = stream.readShort();

			if (DEBUG) {
				Log.info("fieldCount is " + fieldCount);
			}

			for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
				short			accessFlags = stream.readShort(),
								nameIndex = stream.readShort(),
								descriptorIndex = stream.readShort(),
								attributeCount = stream.readShort();

				for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++) {
					short		attributeNameIndex = stream.readShort();
					int			attributeLength = stream.readInt();

					for (int infoIndex = 0; infoIndex < attributeLength; infoIndex++) {
						byte	info = stream.readByte();
					}
				}
			}
		}
	}


	private class MethodInfo {
		// Note that method info is currently unused by JavaAnalysisPanel, so it's read
		// and discarded in order to get to the AttributeInfo that is needed.
		MethodInfo(DataInputStream stream) throws java.io.IOException {
			short			methodCount = stream.readShort();

			if (DEBUG) {
				Log.info("methodCount is " + methodCount);
			}

			for (int methodIndex = 0; methodIndex < methodCount; methodIndex++) {
				short			accessFlags = stream.readShort(),
								nameIndex = stream.readShort(),
								descriptorIndex = stream.readShort(),
								attributeCount = stream.readShort();

				for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++) {
					short		attributeNameIndex = stream.readShort();
					int			attributeLength = stream.readInt();

					for (int infoIndex = 0; infoIndex < attributeLength; infoIndex++) {
						byte	info = stream.readByte();
					}
				}
			}
		}
	}


	private class AttributeInfo {
		// Note that the only attribute currently used by JavaAnalysisPanel is SourceFile, so
		// other attributes are read and discarded.
		
		protected static final String	ATTRIBUTE_SOURCE_FILE = "SourceFile";
		public String					sourceName = null;

		AttributeInfo(DataInputStream stream, ConstantPool pool) throws java.io.IOException {
			short			attributeCount = stream.readShort();

			if (DEBUG) {
				Log.info("attributeCount is " + attributeCount);
			}

			for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++) {
				short		nameIndex = stream.readShort();
				int			length = stream.readInt();

				if (((ConstantPoolItemUTF8)pool.items[nameIndex]).value.equals(ATTRIBUTE_SOURCE_FILE)) {
					short					sourceFileIndex = stream.readShort();
					ConstantPoolItemUTF8	sourceNameItem = (ConstantPoolItemUTF8)pool.items[sourceFileIndex];

					sourceName = sourceNameItem.value.replace(JavaSourceFileFilter.FILE_EXTENSION, "");

					if (DEBUG) {
						Log.info("sourceName is " + sourceName);
					}

					// Any remaining attributes are currently unused, so don't bother reading them.
						break;
				} else {
					for (int infoIndex = 0; infoIndex < length; infoIndex++) {
						byte	info = stream.readByte();
					}
				}
			}
		}
	}


	protected class JavaSourceFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		// Note that either extending javax.swing.filechooser.FileFilter or implementing
		// java.io.FileFilter should suffice, except that the javax.swing.filechooser.setFileFilter
		// method insists on an argument of type javax.swing.filechooser.FileFilter, which, perversely,
		// doesn't implement FileFilter, and the java.io.File.listFile method more correctly wants as an
		// argument a class that implements FileFilter.  Therefore, this class does both.
		public static final String		FILE_EXTENSION = ".java";


		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(FILE_EXTENSION);
		}


		public String getDescription() {
			return FILE_EXTENSION;
		}
	}


	protected class JavaClassFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		// Note that either extending javax.swing.filechooser.FileFilter or implementing
		// java.io.FileFilter should suffice, except that the javax.swing.filechooser.setFileFilter
		// method insists on an argument of type javax.swing.filechooser.FileFilter, which, perversely,
		// doesn't implement FileFilter, and the java.io.File.listFile method more correctly wants as an
		// argument a class that implements FileFilter.  Therefore, this class does both.
		public static final String		FILE_EXTENSION = ".class";


		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(FILE_EXTENSION);
		}


		public String getDescription() {
			return FILE_EXTENSION;
		}
	}


	protected class JavaArchiveFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		// Note that either extending javax.swing.filechooser.FileFilter or implementing
		// java.io.FileFilter should suffice, except that the javax.swing.filechooser.setFileFilter
		// method insists on an argument of type javax.swing.filechooser.FileFilter, which, perversely,
		// doesn't implement FileFilter, and the java.io.File.listFile method more correctly wants as an
		// argument a class that implements FileFilter.  Therefore, this class does both.
		public static final String		FILE_EXTENSION = ".jar";


		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(FILE_EXTENSION);
		}


		public String getDescription() {
			return FILE_EXTENSION;
		}
	}
}
