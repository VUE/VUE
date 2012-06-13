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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JProgressBar;
import javax.swing.ButtonGroup;

import tufts.Util;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.metadata.action.SearchAction;


// Would be nice to have SearchBox that includes a SearchTextField + indeterminate progress bar.
// public class SearchTextField extends javax.swing.JPanel {
public class SearchTextField extends JTextField implements FocusListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SearchTextField.class);

    // "MetadataSearchMainGUI" directly modifies the selected status of these check boxes,
    // which is why they're public static.  Would be better to handle via some kind of posted events.
    public static JMenuItem searcheveryWhereMenuItem;
    public static JMenuItem labelMenuItem;
    public static JMenuItem keywordMenuItem;
    public static JMenuItem categoriesMenuItem;
    public static JMenuItem categoryKeywordMenuItem;
    public static JMenuItem editSettingsMenuItem;

    private static final boolean CATEGORIES_ALONE_OPTION = false;
    
    private final boolean isWindows = VueUtil.isWindowsPlatform();

    private static JPopupMenu popup;
    private static JPopupMenu editPopup;
    
    /** We keep a self reference to JTextField because we've been playing with
     * subclassing JPanel instead of JTextField */
    private final JTextField textField;
    //private final JTextField textField = new JTextField();
    //private final JProgressBar progress = new JProgressBar();
    
    private JTextField subTextField; // an unxeplained hack... used in OSX Tiger, Windows?
    /** a hack to create an a smaller input area leaving room for a Mac-like spyglass icon */
    private JTextField inputField; 
    
    private boolean mouse_over = false;

    private static final String SEARCH_WORD = VueResources.local("search.popup.search");
    private static final String _DEFAULT_TEXT = VueResources.local("search.text.default");
    private String DEFAULT_TEXT = _DEFAULT_TEXT;
    private String MINIMUM_TEXT = "";

    private static SearchTextField Singleton;

    private boolean inUpdate = false;

    private static final boolean DRAW_OVER_STATUS = true;


    // NOTE: this class is used as a singleton: should force that on instancing,
    // or better yet, make it multiply instanceable.
    SearchTextField() {
        // super(VueResources.local("search.text.default"),15);
        //super(11);
        textField = this;
        //add(textField);
        //add(progress);
        GUI.init();
        initMenuSettings();
        
        if (isWindows) {
            initForPlatformWindows();
        } else {
            if (Util.isMacTiger()) {
                initForPlatformMacTiger();
            } else {
                initForPlatformMacLeopardAndLater();
            }
        }
        if (!DRAW_OVER_STATUS) {
            inputField.setText(DEFAULT_TEXT);
            inputField.setForeground(Color.gray);
        } else {
            inputField.setText(MINIMUM_TEXT);
        }

        if (!DRAW_OVER_STATUS || Util.isMacTiger() || isWindows)
            inputField.addFocusListener(this);            
        // progress.setIndeterminate(true);
        // progress.putClientProperty("JProgressBar.style", "circular");
        // progress.setBorderPainted(false);
        // progress.setVisible(false);

        Singleton = this;
    }

    protected void setCursor(int cursorState) {
        textField.setCursor(new Cursor(cursorState));
    }

    private static final boolean SUB_TEXT_HACK = false;
    
    public String getText() {
        return getRawText().trim();
    }
    
    public boolean hasInput() {
        return getForeground().equals(Color.black) && getText().length() > 0;
    }
    
    private String getRawText() {
        if (inputField == this)
            return super.getText();
        else
            return inputField.getText();
    }

    private void resetForInput() {
        // Clear friendly gray reminder note, get ready for input:
        inputField.setText(MINIMUM_TEXT);
        if (!DRAW_OVER_STATUS)
            inputField.setForeground(Color.black);
    }

    /** FocusListener -- only used in certain cases */
    public void focusGained(FocusEvent e) {
        if (DEBUG.FOCUS || DEBUG.SEARCH) Log.debug("focusGained " + GUI.name(e));
        // BUG: if we're getting focus back from the pop-up menu, we DON'T want to do this
        // Apparently we can actually get the focusGained event BEFORE the menu action is run...
        if (getForeground().equals(Color.gray)) {
            resetForInput();
        }
    }
    
    /** FocusListener -- only used in certain cases */
    public void focusLost(FocusEvent e) {
        if (DEBUG.FOCUS || DEBUG.SEARCH) Log.debug("focusLost.. " + GUI.name(e));
        // BUG: if we're losing focus to the pop-up menu, we may not want to do this...
        if (getText().length() <= 0) {
            if (DRAW_OVER_STATUS) {
                resetForInput();
            } else {
                inputField.setForeground(Color.gray);
                updateMessage("focusLost");
            }
        }
    }

    private String getStatusMessage() {
        final JMenuItem searchType = getSelectedSearchTypeMenuItem();
        if (searchType == searcheveryWhereMenuItem)
            return DEFAULT_TEXT;
        else
            return SEARCH_WORD + " " + searchType.getLabel();
    }
    
    private void updateMessage(String src) {
        if (DEBUG.SEARCH) Log.debug("updateMessage src=" + src + "; inUpdate=" + inUpdate);
        if (!inUpdate) {
            if (DRAW_OVER_STATUS)
                repaint(); // display via draw-over -- better as hides the 'x' button
            else
                inputField.setText(getStatusMessage()); // display via input text
        }
    }

    public static void updateSearchType() {
        if (!Singleton.hasInput())
            Singleton.updateMessage("remoteUpdate");
    }

    private JMenuItem getSelectedSearchTypeMenuItem() {
             if (searcheveryWhereMenuItem.isSelected()) return searcheveryWhereMenuItem;
        else if (labelMenuItem.isSelected())            return labelMenuItem;
        else if (keywordMenuItem.isSelected())          return keywordMenuItem;
        else if (categoryKeywordMenuItem.isSelected())  return categoryKeywordMenuItem;
        else if (categoriesMenuItem.isSelected())       return categoriesMenuItem;
        else
            return searcheveryWhereMenuItem;
    }
    
    private final KeyListener SearchKeyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    // It's important to handle this on keyPressed and NOT keyReleased,
                    // and to consume the event, otherwise it can be passed up to the menu
                    // and used to activate a label edit ("Rename" action) if a single
                    // node happens to be selected on the map.
                    ke.consume();
                    runSearch();
                }
            }
        };

    private void runSearch() {
        if (searcheveryWhereMenuItem.isSelected()) {
            setSearchEverywhereAction();
        } else if (labelMenuItem.isSelected()) {
            setLabelSettingsAction();
        } else if (keywordMenuItem.isSelected()) {
            setKeywordSettingsAction();
        } else if (categoryKeywordMenuItem.isSelected()) {
            setKeywordCategorySettingsAction();
        } else if (categoriesMenuItem.isSelected()) {
            setCategorySettingsAction();
        } else {
            setSearchEverywhereAction();
        }
        //else if(editSettingsMenuItem.isSelected()){
        //setEditSettingsAction(); }
    }

    private final MouseAdapter MacMouseListener = 
        new MouseAdapter() {
                public void mousePressed(MouseEvent e) { evalPopup(e); }
                public void mouseReleased(MouseEvent e) { evalPopup(e); }
                private void evalPopup(MouseEvent e) { 
                    if (e.isPopupTrigger())
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    // This is pre DRAW_OVER_STATUS code now handled via FocusEvent's:
                    // else if (getRawText().equals(DEFAULT_TEXT)) {
                    //     // Clear friendly gray reminder note, get ready for input:
                    //     inputField.setText(MINIMUM_TEXT);
                    //     inputField.setForeground(Color.black);
                    // }
                } };
        // comment out old code: at bottom 2012-06-12 19:42.20 Tuesday SFAir.local
    
    
    private void initForPlatformMacTiger() {
        createPopupMenu(isWindows);
        textField.setColumns(15);

        MINIMUM_TEXT = "    "; // leave room for the manual spyglass
        
        if (!DRAW_OVER_STATUS)
            DEFAULT_TEXT = MINIMUM_TEXT + DEFAULT_TEXT;

        if (SUB_TEXT_HACK) {
            textField.setText("");
            subTextField = new JTextField(22);
            subTextField.setBorder(null);
            subTextField.setText(DEFAULT_TEXT);
            subTextField.setForeground(Color.gray);
            inputField = subTextField;
        } else {
            //textField.setText(DEFAULT_TIGER_TEXT); // "indent" over the search icon
            inputField = textField;
            // Insets insets = new Insets(50, 5, 5, 50);
            // textField.setMargin(insets);
            
        }
        inputField.addMouseListener(MacMouseListener);
        inputField.addKeyListener(SearchKeyListener);
        if (SUB_TEXT_HACK) {
            textField.setEditable(false);
            textField.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
            textField.add(subTextField, BorderLayout.CENTER);
        }
    }

    private void initForPlatformMacLeopardAndLater()
    {
        textField.setEditable(true);
        // search variant width will twiggle around if we don't set colums to something
        // (any value will apparently do)
        textField.setColumns(12);
        this.inputField = this.textField;
        
        // Appear to be ignored when using JTextField.variant=search:
        // Insets noInsets = new Insets(0, 30, 0, 25);
        // textField.setMargin(noInsets);
        
        createPopupMenu(isWindows);
        
        textField.putClientProperty("JTextField.variant", "search");
        textField.putClientProperty("JTextField.Search.FindPopup", this.popup);
        // use JTextField.Search.FindAction for an action instead of a pop-up when the spyglass is clicked
        // (e.g., re-run the search)
        textField.putClientProperty("JTextField.Search.CancelAction", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setResetSettingsAction();
                } });
        textField.addMouseListener(MacMouseListener);
        textField.addKeyListener(SearchKeyListener);
    }

    private void initForPlatformWindows() {
        textField.setEditable(true);
        subTextField = new JTextField();
        subTextField.setBorder(null);
        subTextField.setText(VueResources.local("search.text.default"));
        subTextField.setForeground(Color.gray);
        subTextField.setPreferredSize(new Dimension(135,18));
        textField.setPreferredSize(new Dimension(180,23)); // TODO FIX HARDCODE SIZE
        Insets noInsets = new Insets(0, 15, 0, 25);
        textField.setMargin(noInsets);			
        subTextField.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    mouse_over = false;
                    textField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    textField.repaint();
                    textField.revalidate();
                }
                public void mouseExited(MouseEvent e) {					
                    mouse_over = false;
                    textField.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    textField.repaint();
                    textField.revalidate();
                }
                public void mouseClicked(MouseEvent e) {
                    if (subTextField.getText().trim().equals(VueResources.local("search.text.default"))) {
                        subTextField.setText("");
                        subTextField.setForeground(Color.black);
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    //					if (subTextField.getText().trim().equals(
                    //							VueResources.local("search.text.default"))) {
                    //						setText("");
                    //					}
                    if (e.isPopupTrigger()) {
                        createEditPopupMenu();
                        editPopup.show(e.getComponent(), e.getX() + 5, e
                                       .getY());
                    }
                }
            });
        textField.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {					
                    if ((e.getX() < 23)) {						
                        mouse_over = false;
                        textField.setCursor(Cursor
                                  .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        textField.repaint();
                        textField.revalidate();
                    } /*else if (e.getX() < getWidth() - 23) {
                        System.err.println("1");
                        mouse_over = false;
                        setCursor(Cursor
                        .getPredefinedCursor(Cursor.TEXT_CURSOR));
                        repaint();
                        revalidate();
                        }*/ else {						
                        mouse_over = true;
                        textField.setCursor(Cursor
                                  .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        textField.repaint();
                        textField.revalidate();
                    }
                }

                public void mouseExited(MouseEvent e) {		
                    if ((e.getX() < 23)) {
                        mouse_over = false;
                        textField.setCursor(Cursor
                                  .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        textField.repaint();
                        textField.revalidate();
                    } else if (e.getX() < textField.getWidth() - 23) {
                        mouse_over = false;
                        textField.setCursor(Cursor
                                  .getPredefinedCursor(Cursor.TEXT_CURSOR));
                        textField.repaint();
                        textField.revalidate();
                    } else {
                        mouse_over = false;
                        textField.setCursor(Cursor
                                  .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        textField.repaint();
                        textField.revalidate();
                    }

                }
				
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        if ((e.getX() < 23)) {

                        } else if (e.getX() < textField.getWidth() - 23) {
                            createEditPopupMenu();
                            editPopup.show(e.getComponent(), e.getX() + 5, e
                                           .getY());
                        } else {

                        }
                    }
                    if ((e.getX() < 23)) {
                        createPopupMenu(isWindows);
                        popup.show(e.getComponent(), e.getX() + 5, e.getY());
                    } else if (e.getX() < textField.getWidth() - 23) {
                        if (getText().trim().equals(
                                                    VueResources.local("search.text.default"))) {
                            setText("");
                            textField.setForeground(Color.black);

                        }
                    } else {
                        if (searcheveryWhereMenuItem.isSelected()) {
                            setSearchEverywhereAction();
                        }/*
                          * else if(editSettingsMenuItem.isSelected()){
                          * setEditSettingsAction(); }
                          */else if (labelMenuItem.isSelected()) {
                            setLabelSettingsAction();
                        } else if (keywordMenuItem.isSelected()) {
                            setKeywordSettingsAction();
                        } else if (categoryKeywordMenuItem.isSelected()) {
                            setKeywordCategorySettingsAction();
                        } else if (categoriesMenuItem.isSelected()) {
                            setCategorySettingsAction();
                        } else {
                            setSearchEverywhereAction();
                        }
                    }
                }
            });
        subTextField.addKeyListener(SearchKeyListener);
        textField.setEnabled(true);
        textField.setLayout(new BorderLayout());
        //subTextField.setSize(textField.getSize());
        textField.add(subTextField, BorderLayout.CENTER);
			
    }
    
    private JMenuItem makeCheck(String key) {
        return makeCheck(key, VueResources.local(key));
    }
    
    private JMenuItem makeCheck(String key, String label) {
        final JMenuItem item = new JCheckBoxMenuItem(label);
        item.setActionCommand(key);
        // if (actionListener != null)
        //     item.addActionListener(actionListener);
        return item;
    }

    private void initMenuSettings() {
        // searcheveryWhereMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.searcheverywhere"), true);
        // labelMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.labels"));
        // keywordMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.keywords"));
        // categoriesMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.categories"));
        // categoryKeywordMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.categories")
        //                                                 + " + "
        //                                                 + VueResources.local("search.popup.keywords"));
        // editSettingsMenuItem = new JCheckBoxMenuItem(VueResources.local("search.popup.edit.search.settings"));
        
        searcheveryWhereMenuItem  = makeCheck("search.popup.searcheverywhere");
        labelMenuItem             = makeCheck("search.popup.labels");
        keywordMenuItem           = makeCheck("search.popup.keywords");
        categoriesMenuItem        = makeCheck("search.popup.categories");
        categoryKeywordMenuItem   = makeCheck("searchgui.categories_keywords");
        // searchgui.* property key: would better to have one set of property keys for this and MetadaataSearchMainGUI.java

        editSettingsMenuItem      = makeCheck("search.popup.edit.search.settings"); // note this one is NOT part of the group below

        searcheveryWhereMenuItem.setSelected(true);

        ButtonGroup exclusives = new ButtonGroup();
        exclusives.add(searcheveryWhereMenuItem);
        exclusives.add(labelMenuItem);
        exclusives.add(keywordMenuItem);
        exclusives.add(categoriesMenuItem);
        exclusives.add(categoryKeywordMenuItem);
    }

    private JMenuItem popAdd(ActionListener actionListener, String key) {
        return popAdd(actionListener, new JMenuItem(VueResources.local(key)), key);
        // final JMenuItem item = new JMenuItem(VueResources.local(key));
        // item.setActionCommand(key);
        // if (actionListener != null)
        //     item.addActionListener(actionListener);
        // this.popup.add(item);
        // return item;
    }
    
    private JMenuItem popAdd(ActionListener actionListener, JMenuItem item, String key) {
        item.setActionCommand(key);
        if (actionListener != null)
            item.addActionListener(actionListener);
        this.popup.add(item);
        return item;
    }

    /**
     * This method is for Generating Popup menu
     * 
     * @param isWindow
     */
    private void createPopupMenu(boolean isWindows) {
        if (this.popup != null)
            return;

        this.popup = new JPopupMenu();
        final ActionListener actionListener = new PopupActionListener();
        final ActionListener al = actionListener;

        popAdd(al, "search.popup.search").setEnabled(false);

        searcheveryWhereMenuItem.addActionListener(actionListener);
        labelMenuItem.addActionListener(actionListener);
        keywordMenuItem.addActionListener(actionListener);
        categoryKeywordMenuItem.addActionListener(actionListener);
        editSettingsMenuItem.addActionListener(actionListener);

        popup.add(searcheveryWhereMenuItem);
        popup.add(labelMenuItem);
        popup.add(keywordMenuItem);
        if (CATEGORIES_ALONE_OPTION) {
            categoriesMenuItem.addActionListener(actionListener);
            popup.add(categoriesMenuItem);
        }
        popup.add(categoryKeywordMenuItem);
        popup.addSeparator();
        popup.add(editSettingsMenuItem);

        if (!isWindows) {
            popup.addSeparator();
            popAdd(al, "search.popup.clear");
            popAdd(al, "search.popup.reset");
            popAdd(al, "search.popup.select.all");            
            popAdd(al, "search.popup.cut");
            // cutMenuItem.setActionCommand(DefaultEditorKit.cutAction);
            popAdd(al, "search.popup.copy");
            // copyMenuItem.setActionCommand(DefaultEditorKit.copyAction);
            popAdd(al, "search.popup.paste");
            // pasteMenuItem.setActionCommand(DefaultEditorKit.pasteAction);
        }
    }

    class PopupActionListener implements ActionListener {
        // TODO: would be cleaner to handle theses via separate actions
        public void actionPerformed(ActionEvent actionEvent) {
            final String action = actionEvent.getActionCommand();
            if (DEBUG.SEARCH || DEBUG.FOCUS) Log.debug("actionPerformed " + GUI.name(actionEvent));
            if ("search.popup.select.all".equals(action)) {
                inputField.grabFocus();
                inputField.selectAll();
            } else if ("search.popup.cut".equals(action)) {                    inputField.cut();
            } else if ("search.popup.copy".equals(action)) {                   inputField.copy();
            } else if ("search.popup.paste".equals(action)) {
                if (!hasInput())
                    resetForInput(); // be sure to first clear any grey messaging 
                inputField.paste();
                runSearch();
            } else if ("search.popup.clear".equals(action)) {                  inputField.setText(MINIMUM_TEXT);
            } else if ("search.popup.searcheverywhere".equals(action)) {       setSearchEverywhereAction();
            } else if ("search.popup.edit.search.settings".equals(action)) {   setEditSettingsAction();
            } else if ("search.popup.labels".equals(action)) {                 setLabelSettingsAction();
            } else if ("search.popup.keywords".equals(action)) {               setKeywordSettingsAction();
            } else if ("search.popup.reset".equals(action)) {                  setResetSettingsAction();
            } else if ("search.popup.categories".equals(action)) {             setCategorySettingsAction();
            } else if ("searchgui.categories_keywords".equals(action)) {       setKeywordCategorySettingsAction();
            }
        }
    }

    public void setResetSettingsAction() {
        //searcheveryWhereMenuItem.setSelected(true);
                
        searcheveryWhereMenuItem.setSelected(false);
        keywordMenuItem.setSelected(false);
        categoriesMenuItem.setSelected(false);
        categoryKeywordMenuItem.setSelected(false);
        labelMenuItem.setSelected(false);
                
        editSettingsMenuItem.setSelected(false);
                
        //VUE.getMetadataSearchMainGUI().setVisible(false);
        // resetSettingsMenuItem.setSelected(true);
        SearchAction.revertGlobalSearchSelectionFromMSGUI();
        VUE.getActiveViewer().repaint();
    }

    private static final int S_DEFAULT = 0x0;
    private static final int S_BASIC = 0x1;
    private static final int S_TEXT_ONLY = 0x2;
    private static final int S_META_ONLY = 0x4;
    private static final int S_EVERYTHING = 0x8;
    private static final int S_NONE_IS_SPECIAL = 0x16;
    
    private void kickOffSearch(final String typeKey, final int flags)
    {
        if (DEBUG.Enabled) Log.debug("Kicking off search type " + typeKey + "; text[" + getText() + "]");

        if (true) {
            // We're getting concurrent modification exceptions deep
            // down in 3rd party RDIndex code (Jena) when we try
            // running this later on AWT so we can do a wait cursor...
            //progress.setVisible(true);
            _runSearch(typeKey, flags);
            //progress.setVisible(false);
            return;
        }

        // Wait cursor now handled directly in SearchAction
        
        //progress.setVisible(true);
        //revalidate();
        GUI.activateWaitCursor();
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (DEBUG.Enabled) Log.debug("AWT search execute of " + typeKey);
            _runSearch(typeKey, flags);
            if (DEBUG.Enabled) Log.debug("AWT search complete   " + typeKey);
            GUI.clearWaitCursor();
            //progress.setVisible(false);
        }});
    }
    
    private void _runSearch(final String typeKey, final int flags)
    {
        // HOLY CHRIST: just instancing a SearchAction creates a thread which
        // creates an RDF index, and runs an RND index job on the current map /
        // or across all maps, depending on search type.

        if (DEBUG.Enabled) Log.debug("_runSearch[" + typeKey + "] flags=" + flags);
        
        //editSettingsMenuItem.setSelected(false);
        // resetSettingsMenuItem.setSelected(false);
        final List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
        final VueMetadataElement vme = new VueMetadataElement();

        final String inputText = getText();
        
        final String statementObject[] = {
            VueResources.getString("metadata.vue.url") + "#none",
            inputText,
            edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
        vme.setObject(statementObject);
        vme.setType(VueMetadataElement.SEARCH_STATEMENT);
        searchTerms.add(vme);
        
        final SearchAction termsAction = new SearchAction(searchTerms);
        
        termsAction.setBasic            (0 != (flags & S_BASIC));
        termsAction.setTextOnly         (0 != (flags & S_TEXT_ONLY));
        termsAction.setMetadataOnly     (0 != (flags & S_META_ONLY));
        termsAction.setEverything       (0 != (flags & S_EVERYTHING));
        termsAction.setNoneIsSpecial    (0 != (flags & S_NONE_IS_SPECIAL));
        
        termsAction.setOperator(VUE.getMetadataSearchMainPanel().getSelectedOperator());
        
        if (VUE.getMetadataSearchMainPanel() != null)
            setTermsAction(termsAction);

        // ActionEvet arg is ignored.
        // Note that another empty RDFIndex is created in this method
        // and assigned to the SearchAction "index" member -- so
        // the prior index that used to be created via a thread
        // on construction is blown away at that point.
        termsAction.actionPerformed(new ActionEvent(this, 0, "searchFromField"));        

        // I think this was just a hack to call actionPerformed:
        // JButton btn = new JButton();
        // btn.setAction(termsAction);
        // btn.doClick();

        //progress.setVisible(false);
        
    }

    private void updateMetaDataGUI(String typeKey)
    {
        //VUE.getMetadataSearchMainGUI().setVisible(false);
        if (typeKey != null) {
            inUpdate = true;
            try {
                VUE.getMetadataSearchMainPanel().searchTypeCmbBox
                    .setSelectedItem(VueResources.local(typeKey));
            } catch (Exception e) {
                Log.debug("MetaDataGUI update", e);
            } finally {
                inUpdate = false;
            }
        }
    }

    /** set the given search type, and possibly launch / re-launch a search if there's currenlty valid input */
    private void setSearchTypeAndGo(String typeKey, int flags) {
        if (DRAW_OVER_STATUS)
            repaint();
        updateMetaDataGUI(typeKey);
        if (hasInput())
            kickOffSearch(typeKey, flags);
    }
                                     
    private void setKeywordCategorySettingsAction() {
        //categoryKeywordMenuItem.setSelected(true);
        setSearchTypeAndGo("searchgui.categories_keywords", S_DEFAULT);
    }
    private void setKeywordSettingsAction() {
        //keywordMenuItem.setSelected(true);
        setSearchTypeAndGo("searchgui.keywords", S_TEXT_ONLY | S_META_ONLY); // badly named flags!
    }
    private void setLabelSettingsAction() {
        //labelMenuItem.setSelected(true);
        setSearchTypeAndGo("searchgui.labels", S_BASIC);
    }
    private void setSearchEverywhereAction() {
        //searcheveryWhereMenuItem.setSelected(true);
        setSearchTypeAndGo("searchgui.searcheverything", S_TEXT_ONLY);
    }
    private  void setCategorySettingsAction() {
        //categoriesMenuItem.setSelected(true);
        setSearchTypeAndGo(null, S_NONE_IS_SPECIAL); // Old code didn't set have a resource key, so passing null.
    }


    private void setEditSettingsAction() {
        //		searcheveryWhereMenuItem.setSelected(false);
        //		keywordMenuItem.setSelected(false);
        //		categoriesMenuItem.setSelected(false);
        //		categoryKeywordMenuItem.setSelected(false);
        //		labelMenuItem.setSelected(false);
        if (editSettingsMenuItem.isSelected()) {
            VUE.getMetadataSearchMainGUI().setVisible(true);
        } else {
            VUE.getMetadataSearchMainGUI().setVisible(false);
        }
        // resetSettingsMenuItem.setSelected(false);
    }

	private void setTermsAction(SearchAction termsAction) {
		if (VUE.getMetadataSearchMainPanel().mapCmbBox != null
				&& VUE.getMetadataSearchMainPanel().mapCmbBox.getSelectedItem() != null
				&& VUE.getMetadataSearchMainPanel().mapCmbBox
						.getSelectedItem()
						.toString()
						.trim()
						.equals(
								VUE.getMetadataSearchMainPanel().ALL_MAPS_STRING)) {
			termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
		} else {
			termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
		}
		if (VUE.getMetadataSearchMainPanel().resultCmbBox != null
				&& VUE.getMetadataSearchMainPanel().resultCmbBox
						.getSelectedItem() != null) {
			String resultsTypeChoice = VUE.getMetadataSearchMainPanel().resultCmbBox
					.getSelectedItem().toString().trim();
			termsAction.setResultsType(resultsTypeChoice);
		} else {
			termsAction.setResultsType("Select");
		}
	}

	private void createEditPopupMenu() {
		if (editPopup == null) {
			editPopup = new JPopupMenu();
			ActionListener actionListener = new PopupActionListener();

			JMenuItem clearMenuItem = new JMenuItem(VueResources
					.getString("search.popup.clear"));
			clearMenuItem.addActionListener(actionListener);
			editPopup.add(clearMenuItem);

			JMenuItem resetSettingsMenuItem = new JMenuItem(VueResources
					.getString("search.popup.reset"));
			resetSettingsMenuItem.addActionListener(actionListener);
			editPopup.add(resetSettingsMenuItem);

			JMenuItem selectMenuItem = new JMenuItem(VueResources
					.getString("search.popup.select.all"));
			selectMenuItem.addActionListener(actionListener);
			editPopup.add(selectMenuItem);

			JMenuItem cutMenuItem = new JMenuItem(VueResources
					.getString("search.popup.cut"));
			cutMenuItem.addActionListener(actionListener);
			// cutMenuItem.setActionCommand(DefaultEditorKit.cutAction);
			editPopup.add(cutMenuItem);

			JMenuItem copyMenuItem = new JMenuItem(VueResources
					.getString("search.popup.copy"));
			copyMenuItem.addActionListener(actionListener);
			// copyMenuItem.setActionCommand(DefaultEditorKit.copyAction);
			editPopup.add(copyMenuItem);

			JMenuItem pasteMenuItem = new JMenuItem(VueResources
					.getString("search.popup.paste"));
			pasteMenuItem.addActionListener(actionListener);
			// pasteMenuItem.setActionCommand(DefaultEditorKit.pasteAction);
			editPopup.add(pasteMenuItem);

		}
	}

    private static final Image arrowImg = VueResources.getImageIcon("search.downarrowicon").getImage();
    private static final Image clearImg = VueResources.getImageIcon("search.closeicon").getImage();
    private static final Image searchImg = VueResources.getImageIcon("search.searchicon").getImage();
    private static final Image searchOVImg = VueResources.getImageIcon("search.searchicon.ov").getImage();
    private static final Image searchTigerImg = VueResources.getImageIcon("search.tiger.searchicon").getImage();

    // Image searchTigerImgOv =
    // VueResources.getImageIcon("search.tiger.searchicon.ov").getImage();
    // Image clearImgOv =
    // VueResources.getImageIcon("search.closeicon.ov").getImage();
    
    protected void paintComponent(Graphics g)
    {
        // Paint the default look of the button.
        super.paintComponent(g);

        int statusOffsetX = 31; // Good on Mac Leopard + later

        if (isWindows) {
            final int h = textField.getHeight();
            final int w = textField.getWidth();
            final int arrowWidth = arrowImg.getWidth(null);
            g.drawImage(arrowImg, 5, h / 2 - 5,
                        arrowWidth,
                        arrowImg.getHeight(null), this);
            if (!mouse_over) {				
                g.drawImage(searchImg, w - 20, h / 2 - 8,
                            searchImg.getWidth(null),
                            searchImg.getHeight(null), this);
            } else {
                g.drawImage(searchOVImg, w - 20, h / 2 - 8,
                            searchOVImg.getWidth(null),
                            searchOVImg.getHeight(null), this);
            }
            // TODO: need to check and tweak statusOffsetX for Windows -- SMF 2012-06-13
            statusOffsetX = 5 + arrowWidth + 3;
        } else if (Util.isMacTiger()) {
            final int h = textField.getHeight();
            final int w = textField.getWidth();
            final int indent = 6;
            final int spyglassWidth = searchTigerImg.getWidth(null);
            final int spyglassHeight = searchTigerImg.getHeight(null);
            g.drawImage(searchTigerImg, indent, h / 2 - 8, spyglassWidth, spyglassHeight, this);
            if (false && hasInput()) { // disabled for now -- no mouse listener for this
                // draw the 'x' cancel search button
                g.drawImage(clearImg, w - 20, h / 2 - 8,
                            clearImg.getWidth(null),
                            clearImg.getHeight(null), this);
            }
            statusOffsetX = indent + spyglassWidth + 3;
            //statusOffsetX = indent + 2;
            // if(mouse_over){
            // g.drawImage(searchTigerImgOv,5,h/2-7,
            // searchTigerImg.getWidth(null) ,
            // searchTigerImg.getHeight(null), this);
            // g.drawImage(clearImg,w-20,h/2-8, clearImg.getWidth(null) ,
            // clearImg.getHeight(null), this);
            // }else{
            // g.drawImage(searchTigerImg,5,h/2-7,
            // searchTigerImg.getWidth(null) ,
            // searchTigerImg.getHeight(null), this);
            // g.drawImage(clearImg,w-20,h/2-8, clearImg.getWidth(null) ,
            // clearImg.getHeight(null), this);
        }

        if (DRAW_OVER_STATUS && !hasFocus() && !hasInput()) {
            // We rely on the appropriate font having already been set in super.paintComponent
            // This is a better method of displaying the status as w/out text in the
            // the actual field, the 'x' button will hide in the Mac OS X search text variant.
            g.setColor(Color.gray);
            g.drawString(getStatusMessage(), statusOffsetX, 19);
            // todo: ideally set clip region at right so can never drow over the right
            // border -- a rare case that can happen with a narrow main window.
        }
        
        
    }
}


// paste out comments from above 2012-06-12 19:42.39 Tuesday SFAir.local:

        // addMouseListener(new MouseAdapter() {
        // public void mouseEntered(MouseEvent e){
        // if((e.getX()< 23) ){
        // mouse_over = true;
        // repaint();
        // }else if(e.getX() < getWidth()-23){
        // mouse_over = false;
        // repaint();
        // }else{
        // mouse_over = false;
        // repaint();
        // }
        // }
        // public void mouseExited(MouseEvent e){
        // if((e.getX()< 23) ){
        // mouse_over = false;
        // repaint();
        // }else if(e.getX() < getWidth()-23){
        // mouse_over = false;
        // repaint();
        // }else{
        // mouse_over = false;
        // repaint();
        // }
        // }
        //
        // });
