package tufts.vue.gui;

import tufts.vue.*;
import tufts.vue.action.*;

import java.awt.Component;
import java.awt.Event;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;

import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;

/**
 * The main VUE application menu bar.
 *
 * @version $Revision: 1.20 $ / $Date: 2007-03-19 01:43:10 $ / $Author: dan $
 * @author Scott Fraize
 */
public class VueMenuBar extends javax.swing.JMenuBar
    implements java.awt.event.FocusListener
{
    public static VueMenuBar RootMenuBar;
        
    // this may be created multiple times as a workaround for the inability
    // to support a single JMenuBar for the whole application on the Mac
    public VueMenuBar()
    {
        this(VUE.ToolWindows);
    }

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


    public VueMenuBar(Object[] toolWindows)
    {
        //addFocusListener(this);
    	
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        final JMenu fileMenu = new VueMenu("File");
        final JMenu recentlyOpenedMenu = new VueMenu("Open recent");
        JMenu editMenu = new VueMenu("Edit");
        JMenu viewMenu = new VueMenu("View");
        JMenu formatMenu = new VueMenu("Format");
        JMenu arrangeMenu = new VueMenu("Arrange");
        JMenu windowMenu = null;
        JMenu alignMenu = new VueMenu("Arrange/Align");
        JMenu toolsMenu = new VueMenu("Tools");
        //JMenu optionsMenu = menuBar.add(new VueMenu("Options"))l
        JMenu helpMenu = add(new VueMenu("Help"));

        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open...");
        ExitAction exitAction = new ExitAction("Quit");
        Publish publishAction = new Publish("Export");
        
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
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));
        fileMenu.add(Actions.CloseMap);
        fileMenu.add(printAction);
        fileMenu.add(printAction).setText("Print Visible...");
        fileMenu.add(publishAction);
        //fileMenu.add(compareAction);
        
        rebuildRecentlyOpenedItems(fileMenu, recentlyOpenedMenu, rofm);
      
        // GET RECENT FILES FROM PREFS!
        //fileMenu.add(exportMenu);

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
        GUI.addToMenu(editMenu, Actions.NEW_OBJECT_ACTIONS);
        editMenu.add(Actions.Rename);
        editMenu.add(Actions.Duplicate);
        editMenu.add(Actions.Delete);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.add(Actions.CopyStyle);
        editMenu.add(Actions.PasteStyle);
        editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.DeselectAll);
        if (!tufts.Util.isMacPlatform())
            editMenu.add(Actions.Preferences);
        //editMenu.addSeparator();
        //editMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        if (DEBUG.IMAGE)
            editMenu.add(Images.ClearCacheAction);
        
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomActual);
        viewMenu.add(Actions.ZoomToSelection);

        //if (tufts.Util.getJavaVersion() >= 1.5f)
            viewMenu.add(Actions.ToggleFullScreen);

        formatMenu.add(Actions.FontSmaller);
        formatMenu.add(Actions.FontBigger);
        formatMenu.add(Actions.FontBold);
        formatMenu.add(Actions.FontItalic);
        //formatMenu.add(new JMenuItem("Size"));
        //formatMenu.add(new JMenuItem("Style"));
        //formatMenu.add("Text Justify").setEnabled(false);
        // TODO: ultimately better to break these out in to Node & Link submenus
        formatMenu.addSeparator();
        buildMenu(formatMenu, Actions.NODE_MENU_ACTIONS);
        formatMenu.addSeparator();
        buildMenu(formatMenu, Actions.LINK_MENU_ACTIONS);
        
        buildMenu(alignMenu, Actions.ARRANGE_MENU_ACTIONS);

        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        arrangeMenu.addSeparator();
        arrangeMenu.add(Actions.Group);
        arrangeMenu.add(Actions.Ungroup);
        arrangeMenu.addSeparator();
        arrangeMenu.add(alignMenu);
        
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
        
        //optionsMenu.add(new UserDataAction());
        JMenu compareAction = new VueMenu("Connectivity Analysis");
        //Connectivity Actions
        CreateCM createCMAction = new CreateCM("Create");
        AnalyzeCM analyzeCMAction = new AnalyzeCM("Analyze");
        compareAction.add(createCMAction);
        compareAction.add(analyzeCMAction);
        RDFOpenAction rdfOpen = new RDFOpenAction();
        FedoraOntologyOpenAction fooa = new FedoraOntologyOpenAction("Fedora Ontology Types");
        toolsMenu.add(compareAction);
        toolsMenu.add(rdfOpen);
        toolsMenu.add(fooa);

        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.vueWebsite.label"), VueResources.getString("helpMenu.vueWebsite.url")));
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.userGuide.label"), VueResources.getString("helpMenu.userGuide.url")));
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.feedback.label"), VueResources.getString("helpMenu.feedback.url")));
        
        
        if (tufts.Util.isMacPlatform() == false) {
            // already in standard MacOSX place
            helpMenu.add(new AboutAction());
        }

        helpMenu.add(new ShortcutsAction());

        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.releaseNotes.label"),
                                       VueResources.getURL("helpMenu.releaseNotes.file"),
                                       new String("ReleaseNotes_" + VueResources.getString("vue.version") + ".htm").replace(' ', '_')
                                       ));
        
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(formatMenu);
        add(arrangeMenu);
        if (windowMenu != null)
            add(windowMenu);
        add(toolsMenu);
        add(helpMenu);
            
        if (RootMenuBar == null)
            RootMenuBar = this;
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




    private class RecentOpenAction extends VueAction {
    	
    	private File file;
    	public RecentOpenAction(File f)
    	{
    		
    		super(f.getName());
    		file = f;
    	}
    	
    	public void act()
    	{
    		//File f = new File(fileString);
    		OpenAction.displayMap(file);
    	}
    	
    }
    private static class ShortcutsAction extends VueAction {
        private static DockWindow window;
        ShortcutsAction() {
            super("Short Cuts");
        }

        public void act() {
            if (window == null)
                window = createWindow();
            window.setVisible(true);
        }
        private DockWindow createWindow() {
            return GUI.createDockWindow(VUE.getName() + " Short-Cut Keys", createShortcutsList());
        }

        private JComponent createShortcutsList() {
            String text = new String();
            
            // get tool short-cuts
            VueTool[] tools =  VueToolbarController.getController().getTools();
            for (int i = 0; i < tools.length; i++) {
                VueTool tool = tools[i];
                if (tool.getShortcutKey() != 0)
                {
                    text += " " + tool.getShortcutKey() + " : \t\t" + tool.getToolName() + "\n";
                }
            }
            text += "\n";
            // get action short-cuts
            java.util.Iterator i = getAllActions().iterator();
            while (i.hasNext()) {
                VueAction a = (VueAction) i.next();
                KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                if (k != null) {
                    String keyModifiers = KeyEvent.getKeyModifiersText(k.getModifiers());
                    if (keyModifiers.length() > 0)
                        keyModifiers += " ";

                    String keyName = KeyEvent.getKeyText(k.getKeyCode());
                    if (k.getKeyCode() == KeyEvent.VK_OPEN_BRACKET)
                        keyName = "[";
                    else if (k.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET)
                        keyName = "]";
                    else
                        keyName = KeyEvent.getKeyText(k.getKeyCode());
                    
                    String strokeName = keyModifiers + keyName;

                    if (strokeName.length() < 13)
                        strokeName += ": \t\t";
                    else
                        strokeName += ": \t";
                    text += " " + strokeName + a.getPermanentActionName();
                    text += "\n";
                }
            }
            javax.swing.JTextArea t = new javax.swing.JTextArea();
            t.setFont(VueConstants.FONT_SMALL);
            t.setEditable(false);
          //  t.setFocusable(false);
            t.setText(text);
            t.setOpaque(false);
            return t;
        }
        
    }
    
    
    

    
    
}