package tufts.vue.gui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.action.*;

import edu.tufts.vue.ontology.action.OntologyControlsOpenAction;
import edu.tufts.vue.ontology.action.OwlOntologyOpenAction;
import edu.tufts.vue.ontology.action.RDFSOntologyOpenAction;

import java.io.File;

import java.util.Iterator;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;

/**
 * The main VUE application menu bar.
 *
 * @version $Revision: 1.47 $ / $Date: 2007-08-29 01:49:39 $ / $Author: mike $
 * @author Scott Fraize
 */
public class VueMenuBar extends javax.swing.JMenuBar
    implements java.awt.event.FocusListener
{
    public static VueMenuBar RootMenuBar;
    private static JCheckBoxMenuItem fullScreenToolbarItem = null;
    // this may be created multiple times as a workaround for the inability
    // to support a single JMenuBar for the whole application on the Mac
/*    public VueMenuBar()
    {
        this(VUE.ToolWindows);
    }
*/
    /*
      public void paint(Graphics g) {
      System.err.println("\nVueMenuBar: paint");

      }
    */

    private static class VueMenu extends JMenu {
        private boolean unadjusted = true;
        
        VueMenu(String name) {
            super(name);
        }

        public void addNotify() {
            if (unadjusted) {
                GUI.adjustMenuIcons(this);
                unadjusted = false;
            }
            super.addNotify();
        }
    }


    //public VueMenuBar(Object[] toolWindows)
    public VueMenuBar()
    {
        //addFocusListener(this);
    	
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        final JMenu fileMenu = new VueMenu("File");
        final JMenu recentlyOpenedMenu = new VueMenu("Open recent");
        JMenu editMenu = new VueMenu("Edit");
        JMenu viewMenu = new VueMenu("View");
        JMenu formatMenu = new VueMenu("Format");
        JMenu transformMenu = new VueMenu("Font");
        JMenu arrangeMenu = new VueMenu("Arrange");
        JMenu windowMenu = new VueMenu("Windows");
        JMenu alignMenu = new VueMenu("Align");        
        JMenu toolsMenu = new VueMenu("Tools");
        JMenu linkMenu = new VueMenu("Link");
        //JMenu optionsMenu = menuBar.add(new VueMenu("Options"))l
        JMenu helpMenu = add(new VueMenu("Help"));
        
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open...");
        ExitAction exitAction = new ExitAction("Quit");
        Publish publishAction = new Publish("Export...");
        
        // Actions added by the power team
        PrintAction printAction = PrintAction.getPrintAction();
        PDFTransform pdfAction = new PDFTransform("PDF");
        HTMLConversion htmlAction = new HTMLConversion("HTML");
        ImageConversion imageAction = new ImageConversion("JPEG");
        ImageMap imageMap = new ImageMap("IMAP");
        SVGConversion svgAction = new SVGConversion("SVG");
        XMLView xmlAction = new XMLView("XML View");
        
        if (false && DEBUG.Enabled) {
            // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG:
            // we're hitting bug in java (1.4.2, 1.5) on Tiger (OSX 10.4.2) here
            // (apple.laf.ScreenMenuBar array index out of bounds exception)
            JButton u = new JButton(Actions.Undo);
            JButton r = new JButton(Actions.Redo);
            JButton p = new JButton(printAction);
            JButton v = new JButton(printAction);
            v.setText("Print Visible");
            
            u.setBackground(Color.white);
            r.setBackground(Color.white);
            add(u).setFocusable(false);
            add(r).setFocusable(false);
            add(p).setFocusable(false);
            add(v).setFocusable(false);

            //menuBar.add(new tufts.vue.gui.VueButton(Actions.Undo)).setFocusable(false);
            // not picking up icon yet...
        }

        if (false && DEBUG.Enabled) {
            // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG (see above)
            JMenu exportMenu = add(new VueMenu("Export"));
            exportMenu.add(htmlAction);
            exportMenu.add(pdfAction);
            exportMenu.add(imageAction);
            exportMenu.add(svgAction);
            exportMenu.add(xmlAction);
            exportMenu.add(imageMap);
        }
        
        final RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();

        rofm.getPreference().addVuePrefListener(new VuePrefListener()
        {
			public void preferenceChanged(VuePrefEvent prefEvent) {
				rebuildRecentlyOpenedItems(fileMenu, recentlyOpenedMenu, rofm);
				
			}
        	
        });

        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(Actions.CloseMap);
        fileMenu.addSeparator();
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));                
        fileMenu.add(Actions.Revert);
        fileMenu.addSeparator();
        fileMenu.add(publishAction);
        JMenu pdfExportMenu = new JMenu("Create PDF");
        pdfExportMenu.add(Actions.MapAsPDF);
        pdfExportMenu.add(Actions.FullPageSlideNotes);
        pdfExportMenu.add(Actions.Slides8PerPage);
        pdfExportMenu.add(Actions.SpeakerNotes1);
        pdfExportMenu.add(Actions.SpeakerNotes4);
        pdfExportMenu.add(Actions.AudienceNotes);
        pdfExportMenu.add(Actions.SpeakerNotesOutline);
        
        
        fileMenu.add(pdfExportMenu);
               
        fileMenu.addSeparator();
        fileMenu.add(printAction);
        fileMenu.add(printAction).setText("Print Visible...");                
        
        rebuildRecentlyOpenedItems(fileMenu, recentlyOpenedMenu, rofm);
      
        if (VUE.isApplet() || (VUE.isSystemPropertyTrue("apple.laf.useScreenMenuBar") && GUI.isMacAqua())) {
            // Do NOT add quit to the file menu.
            // Either we're an applet w/no quit, or it's already in the mac application menu bar.
            // FYI, MRJAdapter.isSwingUsingScreenMenuBar() is not telling us the truth.
        } else {
            fileMenu.addSeparator();
            fileMenu.add(exitAction);
        }
        
        editMenu.add(Actions.Undo);
        editMenu.add(Actions.Redo);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.add(Actions.Duplicate);
        editMenu.add(Actions.Delete);
        editMenu.addSeparator();
        GUI.addToMenu(editMenu, Actions.NEW_OBJECT_ACTIONS);
        //this isn't in the new comp..but I'm adding it back 7/18
        editMenu.add(Actions.Rename);                
        editMenu.addSeparator();
       // editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.SelectAllNodes);
        editMenu.add(Actions.SelectAllLinks);
        editMenu.add(Actions.Reselect);
        editMenu.add(Actions.DeselectAll);
        if (!tufts.Util.isMacPlatform())
        {   editMenu.addSeparator();
            editMenu.add(Actions.Preferences);
        }

        //editMenu.addSeparator();
        //editMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        
        if (DEBUG.IMAGE)
            editMenu.add(Images.ClearCacheAction);
        
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomActual);
        viewMenu.addSeparator();        
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomToSelection);
        viewMenu.addSeparator();        
        viewMenu.add(Actions.ToggleFullScreen);
        viewMenu.add(Actions.ToggleSplitScreen);
        
        
        if (VUE.getFormatDock() != null)
        {
        	formatMenu.add(createWindowItem(VUE.getFormatDock(),KeyEvent.VK_4,"Formatting Palette"));
        	formatMenu.addSeparator();
        }
        
        formatMenu.add(Actions.CopyStyle);
        formatMenu.add(Actions.PasteStyle);
        formatMenu.addSeparator();        
        
        //build format submenus...
        buildMenu(alignMenu, Actions.ARRANGE_MENU_ACTIONS);
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        transformMenu.add(Actions.FontSmaller);
        transformMenu.add(Actions.FontBigger);
        transformMenu.add(Actions.FontBold);
        transformMenu.add(Actions.FontItalic);
        
        formatMenu.add(transformMenu);        
        formatMenu.add(arrangeMenu);
        formatMenu.add(alignMenu);
        formatMenu.addSeparator();
        formatMenu.add(Actions.Group);
        formatMenu.add(Actions.Ungroup);
        formatMenu.addSeparator();
        buildMenu(linkMenu, Actions.LINK_MENU_ACTIONS);
        formatMenu.add(linkMenu);
        //formatMenu.add(new JMenuItem("Size"));
        //formatMenu.add(new JMenuItem("Style"));
        //formatMenu.add("Text Justify").setEnabled(false);
        // TODO: ultimately better to break these out in to Node & Link submenus
        
        
        
//      optionsMenu.add(new UserDataAction());
        //JMenu compareAction = new VueMenu("Connectivity Analysis");
        //Connectivity Actions
        CreateCM createCMAction = new CreateCM("Connectivity Analysis");
        AnalyzeCM analyzeCMAction = new AnalyzeCM("Merge Maps");
        //compareAction.add(createCMAction);
        //compareAction.add(analyzeCMAction);
        RDFOpenAction rdfOpen = new RDFOpenAction();
        OntologyControlsOpenAction ontcontrls = new OntologyControlsOpenAction("Ontologies");
        //FedoraOntologyOpenAction fooa = new FedoraOntologyOpenAction("Fedora Ontology Types");
        //toolsMenu.add(Actions.AddResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        toolsMenu.add(Actions.AddResource);
        toolsMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        toolsMenu.addSeparator();
        
        if (VUE.getPresentationDock() != null)
        {
        	toolsMenu.add(createWindowItem(VUE.getPresentationDock(),KeyEvent.VK_6, null));        
        	toolsMenu.addSeparator();
        }
        
        toolsMenu.add(analyzeCMAction);
        toolsMenu.add(createCMAction);        
        toolsMenu.addSeparator();
        toolsMenu.add(rdfOpen);
        //toolsMenu.add(fooa);
        toolsMenu.add(ontcontrls);
        
        toolsMenu.addSeparator();
        toolsMenu.add(Actions.SearchFilterAction);
        
        //toolsMenu.add(fooa);
     //   windowMenu = add(new VueMenu("Window"));
        windowMenu.add(Actions.KeywordAction);
        
        if (VUE.getInfoDock() !=null)
        	windowMenu.add(createWindowItem(VUE.getInfoDock(),KeyEvent.VK_2, "Info"));
        if (VUE.getMapInfoDock() !=null)
        	windowMenu.add(createWindowItem(VUE.getMapInfoDock(),KeyEvent.VK_3, "Map Info"));
        
        windowMenu.add(Actions.NotesAction);
        if (VUE.getFormatDock() != null)        
        	windowMenu.add(createWindowItem(VUE.getFormatDock(),KeyEvent.VK_4,"Formatting Palette"));                
        if (VUE.getOutlineDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getOutlineDock(),KeyEvent.VK_7, "Outline"));
        if (VUE.getPannerDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getPannerDock(),KeyEvent.VK_5, "Panner"));
        if (VUE.getContentDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getContentDock(),KeyEvent.VK_1, "Resources"));    
        if (VUE.getSlideDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getSlideDock(),KeyEvent.VK_8, "Slide Viewer"));
        if (VUE.getFloatingZoomDock()!=null)
        {
        	fullScreenToolbarItem = createWindowItem(VUE.getFloatingZoomDock(),KeyEvent.VK_9, "FullScreen Toolbar");
        	fullScreenToolbarItem.setEnabled(false);
        	windowMenu.add(fullScreenToolbarItem);        	
        }
    /* 
    	ObjectInspector,        	
    
    	MapInspector,
    	outlineDock,
    	pannerDock,        	
    	DR_BROWSER_DOCK,
    	slideDock, 
    	*/
        /*
         * I have a feeling this may come back so I'm leaving it in.
        int index = 0;
        if (toolWindows != null) {

            windowMenu = add(new VueMenu("Window"));
                
            for (int i = 0; i < toolWindows.length; i++) {
                //System.out.println("adding " + toolWindows[i]);
                Object toolWindow = toolWindows[i];
                if (toolWindow == null)
                    continue;
                final WindowDisplayAction windowAction = new WindowDisplayAction(toolWindow);
                final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(KeyEvent.VK_1 + index++, Actions.COMMAND);
                windowAction.putValue(Action.ACCELERATOR_KEY, acceleratorKey);
                JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
                windowAction.setLinkedButton(checkBox);
                windowMenu.add(checkBox);
            }
        }
        */
        
        if (tufts.Util.isMacPlatform() == false) {
            // already in standard MacOSX place
            helpMenu.add(new AboutAction());
            helpMenu.addSeparator();
        }
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.userGuide.label"), VueResources.getString("helpMenu.userGuide.url")));        
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.feedback.label"), VueResources.getString("helpMenu.feedback.url")));
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.vueWebsite.label"), VueResources.getString("helpMenu.vueWebsite.url")));
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.mymaps.label"), VueResources.getString("helpMenu.mymaps.url")));
        

        
        
        helpMenu.addSeparator();
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.releaseNotes.label"),
                VueResources.getURL("helpMenu.releaseNotes.file"),
                new String("ReleaseNotes_" + VueResources.getString("vue.version") + ".htm").replace(' ', '_')
                ));
        helpMenu.addSeparator();
        helpMenu.add(new ShortcutsAction());
        
        helpMenu.addSeparator();
        helpMenu.add(new ShowLogAction());
        
        
        //build out the main menus..
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(formatMenu);
       // add(arrangeMenu);
        add(toolsMenu);
        //if (windowMenu != null)
            add(windowMenu);
        //add(toolsMenu);
        add(helpMenu);
            
        if (RootMenuBar == null)
            RootMenuBar = this;
    }
    
    public static void toggleFullScreenTools()
    {
    	fullScreenToolbarItem.setEnabled(FullScreen.inFullScreen());
    }
    
    public JCheckBoxMenuItem createWindowItem(DockWindow dock,int accelKey, String text)
    {
    	final WindowDisplayAction windowAction = new WindowDisplayAction(dock);
    	final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(accelKey, Actions.COMMAND);
    	windowAction.putValue(Action.ACCELERATOR_KEY, acceleratorKey);    	
    	if (text !=null)
    		windowAction.setTitle(text);
    	
    	JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
    	
    	
    		//checkBox.setText(text);
    	windowAction.setLinkedButton(checkBox);
    	
    	return checkBox;
    }
    protected void rebuildRecentlyOpenedItems(JMenu fileMenu, JMenu recentlyOpenedMenu, RecentlyOpenedFilesManager rofm) {
    	if (rofm.getFileListSize() > 0)
        {
        	List files = rofm.getRecentlyOpenedFiles();
        	Iterator i = files.iterator();

        	recentlyOpenedMenu.removeAll();
        	
        	while (i.hasNext())
        	{
        		
        		File f = new File((String)i.next());
        		if (f.exists())
        			recentlyOpenedMenu.add(new RecentOpenAction(f));
        		else 
        			f=null;
        	}
        	fileMenu.remove(recentlyOpenedMenu);
        	fileMenu.add(recentlyOpenedMenu,2);
        }
		
	}


	private KeyEvent alreadyProcessed;

    /*
    public boolean doProcessKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        //return super.processKeyBinding(ks, e, condition, pressed);
        //if (e != alreadyProcessed) {
            System.out.println("VueMenuBar: handling relayed " + ks);
            return processKeyBinding(ks, e, condition, pressed);
            //}
            //return true;
    }
    */

    public void processKeyEvent(KeyEvent e) {
        if (!e.isConsumed())
            super.processKeyEvent(e);
        else
            System.out.println("VueMenuBar: processKeyEvent: already consumed " + e);
    }
    
    void doProcessKeyEvent(KeyEvent e) {
        if (e != alreadyProcessed) {
            if (DEBUG.KEYS) System.out.println("VueMenuBar: doProcessKeyEvent " + e);
            processKeyEvent(e);
        }
        else if (DEBUG.KEYS) System.out.println("VueMenuBar: already processed " + e);
    }
    
    // todo: this doesn't work: safer if can get working instead of above
    void doProcessKeyPressEventToBinding(KeyEvent e) {

        if (e != alreadyProcessed) {
            //System.out.println("VueMenuBar: doProcessKeyPressEventToBinding " + e);
            System.out.println("VueMenuBar: KEY->BIND " + e);
            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), false);
            super.processKeyBinding(ks, e, WHEN_FOCUSED, true);
        }
        else System.out.println("VueMenuBar: already processed " + e);
    }
    
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (e.isConsumed())
            System.out.println("VueMenuBar: GOT CONSUMED " + ks);

        if (!pressed) // we only ever handle on key-press
            return false;
            
        boolean didAction = super.processKeyBinding(ks, e, condition, pressed);
        if (DEBUG.KEYS) {
            String used = didAction ?
                "CONSUMED " :
                "NOACTION ";
            System.out.println("VueMenuBar: processKeyBinding " + used + ks + " " + e.paramString());
        }
        if (didAction)
            e.consume();
        alreadyProcessed = e;
        return didAction;
    }
    

    public void setVisible(boolean b) {
        VUE.Log.debug("VMB: setVisible: " + b);
        super.setVisible(b);
    }
    public void focusGained(java.awt.event.FocusEvent e) {
        VUE.Log.debug("VMB: focusGained from " + e.getOppositeComponent());
    }
    public void focusLost(java.awt.event.FocusEvent e) {
        VUE.Log.debug("VMB: focusLost to " + e.getOppositeComponent());
    }


    public static JMenu buildMenu(String name, Action[] actions) {
        return buildMenu(new VueMenu(name), actions);
    }
    public static JMenu buildMenu(JMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        return menu;
    }




    private static class RecentOpenAction extends VueAction {
    	private File file;
    	public RecentOpenAction(File f) {
            super(f.getName());
            file = f;
    	}

        @Override
        public boolean isUserEnabled() { return true; }
    	
    	public void act()
    	{
            OpenAction.displayMap(file);
    	}
    	
    }

    private static class ShowLogAction extends VueAction {
        private static DockWindow errorDock;
        private static JTextArea textArea;

        //private static final String ReportAddress = "vue-help@elist.tufts.edu";
        private static final String ReportAddress = "vue-report@fraize.org";
        
    	public ShowLogAction() {
            super("VUE Log");
    	}

        @Override
        public boolean isUserEnabled() { return true; }
    	
    	public void act() {
            if (errorDock == null)
                buildGUI();
            textArea.setText(tufts.Util.getExceptionLog().toString());
            errorDock.setVisible(true);
    	}

        private void buildGUI() {
            final JPanel panel = new JPanel(new BorderLayout());
            textArea = new JTextArea();
            textArea.setFont(VueConstants.SmallFixedFont);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

            if (!Util.isWindowsPlatform())
            // can't get enough log into the email for now on Windows to be useful.
                panel.add(new JButton("Submit Report") {
                    @Override
                    protected void fireActionPerformed(ActionEvent ae) {                        
                        //System.out.println("ACTION " + ae);
                        
                        final String body;

                        if (Util.isWindowsPlatform()) {
                            // There's a 2048 byte WinXP argument limit for url.dll,FileProtocolHandler,
                            // so don't add anything extra...
                            body = Util.getExceptionLog().toString();
                        } else {
                            body =
                                "Thank you for submitting a problem report.\n"
                                + "Please feel free to add any comments/feedback here:\n"
                                + "\n\n\n"
                                + Util.getExceptionLog()
                                + "\nEnd report: " + new java.util.Date() + ".\n";
                        }
                            
                        final String subject =
                            "VUE Log Report from " + VUE.getSystemProperty("user.name");
                        
                        try {
                            VueUtil.openURL(Util.makeQueryURL("mailto:" + ReportAddress,
                                                              "subject", subject,
                                                              "body", body));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                },
                BorderLayout.SOUTH);
            errorDock = GUI.createDockWindow("VUE Log", panel);
            errorDock.setSize(800,600);
        }
    }
    
    private static class ShortcutsAction extends VueAction {
        private static DockWindow window;
        ShortcutsAction() {
            super("Keyboard Shortcuts");
        }

        public void act() {
            if (window == null)
                window = createWindow();
            //window.setWidth(400);
            window.pack(); // fit to widest line
            window.setVisible(true);
        }
        private DockWindow createWindow() {
            return GUI.createDockWindow(VUE.getName() + " Short-Cut Keys", createShortcutsList());
        }

        private static String keyCodeName(int keyCode) {
            return keyCodeName(keyCode, false);
        }
        
        private static String keyCodeName(int keyCode, boolean lowerCase) {
            
            if (lowerCase && keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
                return String.valueOf((char)keyCode).toLowerCase();
            }
            
            if (keyCode == KeyEvent.VK_OPEN_BRACKET)
                return "[";
            else if (keyCode == KeyEvent.VK_CLOSE_BRACKET)
                return "]";
            else if (keyCode == 0)
                return "";
            else
                return KeyEvent.getKeyText(keyCode);
        }

        private JComponent createShortcutsList() {
            String text = new String();
            
            // get tool short-cuts
            for (VueTool t : VueTool.getTools()) {
                final int downKey = t.getActiveWhileDownKeyCode();
                if (DEBUG.TOOL) {
                    text += String.format(" %-25s (%c) %-12s %-23s %s \n",
                                          t.getID()+":",
                                          t.getShortcutKey() == 0 ? ' ' : t.getShortcutKey(),
                                          "("+keyCodeName(downKey) + ")",
                                          t.getToolName(),
                                          t.getClass().getName()
                                          );
                } else if (t.getShortcutKey() != 0) {
                    text += String.format(" (%c) %-12s %s \n",
                                          t.getShortcutKey(),
                                          downKey == 0 ? "" : "(" + keyCodeName(downKey, true) + ")",
                                          t.getToolName());
                }
            }
            
            text += "\n";

            // get action short-cuts
            for (VueAction a : getAllActions()) {
                
                KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                if (k == null)
                    continue;
                
                String keyModifiers = KeyEvent.getKeyModifiersText(k.getModifiers());
                if (keyModifiers.length() > 0)
                    keyModifiers += " ";

                String strokeName = keyModifiers + keyCodeName(k.getKeyCode());

                text += String.format(" %-20s %s \n", strokeName, a.getPermanentActionName());
            }
            
            javax.swing.JTextArea t = new javax.swing.JTextArea();
//             javax.swing.JTextArea t = new javax.swing.JTextArea() {
//                     public void paint(java.awt.Graphics g) {
//                         ((java.awt.Graphics2D)g).setRenderingHint
//                             (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
//                              java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//                         super.paint(g);
//                     }};
            
            t.setFont(VueConstants.SmallFixedFont);
            t.setEditable(false);
          //  t.setFocusable(false);
            t.setText(text);
            t.setOpaque(false);
            return new JScrollPane(t,
                                   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                   );
        }
        
    }
    
    
    

    
    
}
