package tufts.vue.gui;

import tufts.vue.*;
import tufts.vue.action.*;

import java.awt.Event;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;

/**
 * The main VUE application menu bar.
 * @version $Revision: 1.1 $ / $Date: 2005-11-27 16:15:39 $ / $Author: sfraize $ 
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
        
    public VueMenuBar(ToolWindow[] toolWindows)
    {
        addFocusListener(this);
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu formatMenu = new JMenu("Format");
        JMenu arrangeMenu = new JMenu("Arrange");
        JMenu windowMenu = null;
        JMenu alignMenu = new JMenu("Arrange/Align");
        //JMenu optionsMenu = menuBar.add(new JMenu("Options"))l
        JMenu helpMenu = add(new JMenu("Help"));

        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open Map...");
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
            JMenu exportMenu = add(new JMenu("Export"));
            exportMenu.add(htmlAction);
            exportMenu.add(pdfAction);
            exportMenu.add(imageAction);
            exportMenu.add(svgAction);
            exportMenu.add(xmlAction);
            exportMenu.add(imageMap);
        }
        
        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));
        fileMenu.add(Actions.CloseMap);
        fileMenu.add(printAction);
        fileMenu.add(printAction).setText("Print Visible...");
        fileMenu.add(publishAction);
        // GET RECENT FILES FROM PREFS!
        //fileMenu.add(exportMenu);

        if (VUE.isApplet() || (VUE.isSystemPropertyTrue("apple.laf.useScreenMenuBar") && VueUtil.isMacAquaLookAndFeel())) {
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
        editMenu.add(Actions.NewNode);
        editMenu.add(Actions.NewText);
        editMenu.add(Actions.Rename);
        editMenu.add(Actions.Duplicate);
        editMenu.add(Actions.Delete);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.DeselectAll);
        editMenu.addSeparator();
        editMenu.add(Actions.editDataSource);
        editMenu.addSeparator();
        editMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomActual);
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

            windowMenu = add(new JMenu("Window"));
                
            for (int i = 0; i < toolWindows.length; i++) {
                //System.out.println("adding " + toolWindows[i]);
                ToolWindow toolWindow = toolWindows[i];
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
        
        helpMenu.add(new ShowURLAction("VUE Online", "http://vue.tccs.tufts.edu/"));
        helpMenu.add(new ShowURLAction("User Guide", "http://vue.tccs.tufts.edu/userdoc/"));
        helpMenu.add(new AboutAction());
        helpMenu.add(new ShortcutsAction());

        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(formatMenu);
        add(arrangeMenu);
        if (windowMenu != null)
            add(windowMenu);
        add(helpMenu);
            
        if (RootMenuBar == null)
            RootMenuBar = this;
    }

    public boolean doProcessKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        return super.processKeyBinding(ks, e, condition, pressed);
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
        return buildMenu(new JMenu(name), actions);
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



    private static class WindowDisplayAction extends javax.swing.AbstractAction {
        private AbstractButton mLinkedButton;
        private Window mWindow;
        private String mTitle;
        private boolean firstDisplay = true;
        private static final boolean showActionLabel = false;
        
        public WindowDisplayAction(Window w) {
            super("window: " + w.getName());
            init(extractTitle(w), w);
        }
        
        public WindowDisplayAction(ToolWindow tw) {
            super("window: " + tw.getWindow().getName());
            init(tw.getTitle(), tw.getWindow());
        }

        private void init(String title, Window w) {
            mTitle = title;
            updateActionTitle(true);
            mWindow = w;
            mWindow.addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) { handleShown(); }
                    public void componentHidden(ComponentEvent e) { handleHidden(); }
            });
        }

        /*
        private String getTitle() {
            return mTitle;
            //return extractTitle(mWindow);
        }
        */

        private static String extractTitle(Window w) {
            if (w instanceof java.awt.Frame)
                return ((java.awt.Frame)w).getTitle();
            else if (w instanceof java.awt.Dialog)
                return ((java.awt.Dialog)w).getTitle();
            else
                return ((java.awt.Window)w).getName();
        }

        private void handleShown() {
            //out("handleShown [" + getTitle() + "]");
            setButtonState(true);
            updateActionTitle(false);
        }
        private void handleHidden() {
            //out("handleHidden [" + getTitle() + "]");
            setButtonState(false);
            updateActionTitle(false);
        }
        
        private void updateActionTitle(boolean firstTime) {
            if (!firstTime && !showActionLabel)
                return;
            if (showActionLabel) {
                String action = "Show ";
                if (mLinkedButton != null && mLinkedButton.isSelected())
                    action = "Hide ";
                putValue(Action.NAME, action + mTitle);
            } else {
                putValue(Action.NAME, mTitle);
            }
        }
        void setLinkedButton(AbstractButton b) {
            mLinkedButton = b;
        }
        private void setButtonState(boolean tv) {
            if (mLinkedButton != null)
                mLinkedButton.setSelected(tv);
        }
        public void actionPerformed(ActionEvent e) {
            if (mLinkedButton == null)
                mLinkedButton = (AbstractButton) e.getSource();
            if (firstDisplay && mWindow.getX() == 0 && mWindow.getY() == 0) {
                mWindow.setLocation(20,22);
            }
            firstDisplay = false;
            if (mLinkedButton.isSelected()) {
                mWindow.setVisible(true);
                mWindow.toFront();
                //VUE.ensureToolWindowVisibility(mTitle);
            } else {
                mWindow.setVisible(false);
                //VUE.ensureToolWindowVisibility(null);
            }
            
        }
    }




    private static class ShortcutsAction extends VueAction {
        private static ToolWindow window;
        ShortcutsAction() {
            super("Short Cuts");
        }

        void act() {
            if (window == null)
                window = createWindow();
            window.setVisible(true);
        }
        private ToolWindow createWindow() {
            return VUE.createToolWindow(VUE.NAME + " Short-Cut Keys", createShortcutsList());
        }

        private JComponent createShortcutsList() {
            String text = new String();
            
            // get tool short-cuts
            VueTool[] tools =  VueToolbarController.getController().getTools();
            for (int i = 0; i < tools.length; i++) {
                VueTool tool = tools[i];
                if (tool.getShortcutKey() != 0)
                    text += " " + tool.getShortcutKey() + " - " + tool.getToolName() + "\n";
            }
            text += "\n";
            // get action short-cuts
            java.util.Iterator i = getAllActions().iterator();
            while (i.hasNext()) {
                VueAction a = (VueAction) i.next();
                KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                if (k != null) {
                    String keyName = " "
                        + KeyEvent.getKeyModifiersText(k.getModifiers())
                        + " "
                        + KeyEvent.getKeyText(k.getKeyCode());
                    if (keyName.length() < 10)
                        keyName += ": \t\t";
                    else
                        keyName += ": \t";
                    text += keyName + a.getPermanentActionName();
                    text += "\n";
                }
            }
            javax.swing.JTextArea t = new javax.swing.JTextArea();
            t.setFont(VueConstants.FONT_SMALL);
            t.setEditable(false);
            t.setFocusable(false);
            t.setText(text);
            t.setOpaque(false);
            return t;
        }
        
    }
    
    
    

    
    
}
