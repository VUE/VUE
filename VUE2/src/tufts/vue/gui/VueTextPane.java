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

package tufts.vue.gui;

import tufts.vue.*;
import tufts.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/** 
 * Provides an editable multi-line text panel for a named LWComponent property.
 * Automatically saves the text upon focus loss if there was any change,
 * and enters an undo entry.
 *
 * @author Scott Fraize
 * @version $Revision: 1.20 $ / $Date: 2010-02-03 19:15:46 $ / $Author: mike $
 */

// todo: create an abstract class for handling property & undo code, and subclass this and VueTextField from it.
// or: a handler/listener that can be attached to any text field.

// todo: consume all key events
// todo: rename LWTextPane, as is specific to to LWComponent text properties
// todo: should just be an LWEditor (might create a fancier global mechanism for updating them)

public class VueTextPane extends JTextPane
    implements LWComponent.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueTextPane.class);
    
    private LWComponent lwc;
    private Object propertyKey;
    /** was a key pressed since we loaded the current text? */
    private boolean keyWasPressed = false; // TODO: also need to know if cut or paste happened!
    private boolean styledText = false;
    private String undoName;
    private String loadedText;
	
    public VueTextPane(LWComponent c, Object propertyKey, String undoName)
    {
        addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) { saveText(e); }
            });

        if (c != null && propertyKey != null)
            attachProperty(c, propertyKey);
        setUndoName(undoName);

        GUI.installBorder(this);

        if (propertyKey != null)
            setName(propertyKey.toString());
        else if (undoName != null)
            setName(undoName);
    }

    public VueTextPane(String undoName) {
        this(null, null, undoName);
    }
    
    public VueTextPane() {
        this(null, null, null);
    }

    private void debug(String s) {
        Log.debug(String.format("%08X[%s/%s] %s", System.identityHashCode(this), getName(), propertyKey, s));;
    }
    
    /** We override this to do nothing, so that default focus traversal keys are left in
     * place (and so you can't use TAB in this class.  See java 1.4 JEditorPane constructor
     * where it installs JComponent.getManagingFocus{Forward,Backward}TraversalKeys().
     * This doesn't work for java 1.5 -- will have to override LookAndFeel.installProperty
     * for that.
     */
    @Override
    public void setFocusTraversalKeys(int id, java.util.Set keystrokes) {
        if (DEBUG.FOCUS) System.out.println(this + " ignoring setFocusTraversalKeys " + id + " " + keystrokes);
    }

    public void XsetName(String s) {
        tufts.Util.printStackTrace("setName " + s);
        super.setName(s);
    }

    @Override public void setText(String s) {
        if (DEBUG.TEXT) debug("setText: curText=" + Util.tags(getText()) + " newText=" + Util.tags(s));
        super.setText(s);
    }
    
    
    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (DEBUG.KEYS && e.getID() == KeyEvent.KEY_PRESSED) Log.debug("processKeyEvent " + e.paramString() + "; " + this);
        // if any key activity, assume it may have changed
        // (to make sure we catch cut's and paste's as well newly input characters)
        //if (DEBUG.TEXT) debug("curTextA " + Util.tags(getText()));

        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) {
            saveText("numpad-enter");
        } else {
            keyWasPressed = true;
            super.processKeyEvent(e);
        }
        //if (DEBUG.TEXT) debug("curTextZ " + Util.tags(getText()));
    }

    public void attachProperty(LWComponent c, Object key) {
        if (c == null || key == null)
            throw new IllegalArgumentException("component=" + c + " propertyKey="+key + " neither can be null");
        saveText("attach");
        if (lwc == c && propertyKey == key)
            return;
        if (lwc != null)
            lwc.removeLWCListener(this);
        lwc = c;
        propertyKey = key;
        loadPropertyValue();
        if (DEBUG.TEXT||DEBUG.EVENTS) debug("attachProperty " + Util.tags(key) + "; " + c);
        lwc.addLWCListener(this, propertyKey, LWKey.Deleting);
        keyWasPressed = false;
    }

    public void detachProperty() {
        //saveText("detach"); // overkill & causing problems w/multi-label text edit (check note panel for any possible new problems) [BREAKS NOTES!]
        saveText("detach");
        if (lwc != null) {
            if (DEBUG.TEXT||DEBUG.EVENTS) debug("detach from " + lwc);
            //Util.printStackTrace("DETACH");
            lwc.removeLWCListener(this);
            lwc = null;
        }
        setText("");
        //saveText("autoDetach");
    }

    /** an optional special undo name for this property */
    public void setUndoName(String name) {
        undoName = name;
    }


    // TODO: DROP OF TEXT (this is a paste, but with no keypress!)
    protected void saveText(Object src) {
        final String currentText = getText();
        if (DEBUG.TEXT||DEBUG.EVENTS) debug("saveText;"
                                  + "\n\tsrc=" + tufts.Util.tags(src)
                                  + "\n\t" + this
                                  + "\n\tcurText=[" + currentText + "]"
                                  );

        // TODO: this is missing cases where text hasn't changed,
        // causing multple needless text-set events (e.g., hit
        // "enter", then focus-loss w/out changing text, and an apply
        // cycle still takes place)

        if (keyWasPressed || !currentText.equals(loadedText)) {
        //if (lwc != null && (keyWasPressed || !currentText.equals(loadedText))) {
            //if (DEBUG.KEYS||DEBUG.TEXT|DEBUG.EVENTS) Log.debug("saveText: APPLYING TEXT: " + this);

            // rtfTestHack();

            if (DEBUG.EVENTS) debug(String.format("saveText: apply %s -> %s",  Util.tags(propertyKey), lwc));
            applyText(currentText);
            
        }	
    }
    
    public void loadText(String text) {
        setText(text);
        loadedText = text;
    }

    protected void applyText(String text) {

        if (lwc == null)
            return;
        
        if (DEBUG.EVENTS || DEBUG.TEXT) debug(String.format("applyText: '%.16s'... to: %s -> %s", text, Util.tags(propertyKey), lwc));
        lwc.setProperty(propertyKey, text);
        loadedText = text;
        if (undoName != null)
            VUE.markUndo(undoName);
        else
            VUE.markUndo();
    }



//     private void rtfTestHack() {
//         Document doc = getDocument();
//         String text = null;
//         try {
//             if (DEBUG.KEYS) System.out.println(this + " saveText [" + doc.getText(0, doc.getLength()) + "]");
//             java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();                
//             //java.io.CharArrayWriter buf = new java.io.CharArrayWriter(); // RTFEditorKit won't write 16 bit characters.
//             // But it turns out it still handles unicode via self-encoding the special chars.
//             getEditorKit().write(buf, doc, 0, doc.getLength());
//             text = buf.toString();
//             if (DEBUG.KEYS) System.out.println(this + " EDITOR KIT OUTPUT [" + text + "]");
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         lwc.setProperty(propertyKey, text);
//     }

    
    private void loadPropertyValue() {
        String text = null;
        if (lwc != null) {
            try {
                if (propertyKey == LWKey.Label && lwc.getLabelFormat() != null) {
                    text = lwc.getLabelFormat();
                } else {
                    text = (String) lwc.getPropertyValue(propertyKey);
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("VueTextPane only handles properties of type String");
            }
            setEnabled(lwc.supportsProperty(propertyKey));
        } else {
            setEnabled(false);
        }
        if (text == null) {
            loadText("");
        } else {
            loadText(text);
        }
    }
    
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() == lwc) {
            if (e.key == LWKey.Deleting) {
                lwc.removeLWCListener(this);
                lwc = null;
                propertyKey = null;
            }
            loadPropertyValue();
        }
    }


    private void enableStyledText() {
        if (styledText)
            return;
        styledText = true;
        String text = getText();
        System.out.println("text[" + text + "]");
        setContentType("text/rtf");
        try {
            read(new java.io.StringReader(text), "description");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //setText(text);
    }

    private class BoldAction extends StyledEditorKit.BoldAction {
        public void actionPerformed(ActionEvent e) {
            //enableStyledText();
	    super.actionPerformed(e);
        }
    }
    

    public void addNotify() {
        super.addNotify();

        if (getContentType().equalsIgnoreCase("text/rtf")) {
        
            int COMMAND = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;

            System.out.println("ADDING STYLED TEXT KEYMAP");
            Keymap parentMap = getKeymap();
            Keymap map = JTextComponent.addKeymap("MyFontStyleMap", parentMap);
            
            // Add CTRL-B for Bold
            KeyStroke boldStroke = KeyStroke.getKeyStroke(KeyEvent.VK_B, COMMAND, false);
            map.addActionForKeyStroke(boldStroke, new StyledEditorKit.BoldAction());
            
            // Add CTRL-I for Italic
            KeyStroke italicStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, COMMAND, false);
            map.addActionForKeyStroke(italicStroke, new StyledEditorKit.ItalicAction());
            
            // Add CTRL-U for Underline
            KeyStroke underlineStroke = KeyStroke.getKeyStroke(KeyEvent.VK_U, COMMAND, false);
            map.addActionForKeyStroke(underlineStroke, new StyledEditorKit.UnderlineAction());
            
            setKeymap(map);
        }
    }

    public static void main(String args[]) {
        VUE.init(args);
        VueUtil.displayComponent(new VueTextField("some text"));
        DockWindow w = GUI.createDockWindow(VueResources.getString("dockWindow.vueTextpanetest.title"));
        javax.swing.JPanel panel = new javax.swing.JPanel();
        VueTextPane tp1 = new VueTextPane();
        VueTextPane tp2 = new VueTextPane();
        VueTextPane tp3 = new VueTextPane();
        panel.add(tp1);
        panel.add(tp2);
        panel.add(tp3);
        //vtp.setEditable(true);
        w.setContent(panel);
        w.setVisible(true);
        VueUtil.displayComponent(new VueTextPane(new LWMap("Test Map"), LWKey.Notes, null));
    }
    

    public String toString()
    {
        return "VueTextPane[" + propertyKey + "; " + lwc + "; keyWasPressed=" + keyWasPressed + "]";
    }

}
