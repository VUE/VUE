
package tufts.vue.gui;

import tufts.vue.*;

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
 * @version August 2005
 */

// todo: create an abstract class for handling property & undo code, and subclass this and VueTextField from it.
public class VueTextPane extends JTextPane
    implements LWComponent.Listener
{
    private LWComponent lwc;
    private Object propertyKey;
    /** was a key pressed since we loaded the current text? */
    private boolean keyWasPressed = false;
    private boolean styledText = false;
    private String undoName;
	
    public VueTextPane(LWComponent c, Object propertyKey, String undoName)
    {
        addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) { saveText(); }
            });
        addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (DEBUG.KEYS) System.out.println(e);
                    keyWasPressed = true;
                }
            });

        if (c != null && propertyKey != null)
            attachToProperty(c, propertyKey);
        setUndoName(undoName);
    }

    public VueTextPane(String undoName) {
        this(null, null, undoName);
    }
    
    public VueTextPane() {
        this(null, null, null);
    }
    
    public void attachToProperty(LWComponent c, Object key) {
        if (c == null || key == null)
            throw new IllegalArgumentException("component=" + c + " propertyKey="+key + " neither can be null");
        saveText();
        if (lwc == c && propertyKey == key)
            return;
        if (lwc != null)
            lwc.removeLWCListener(this);
        lwc = c;
        propertyKey = key;
        loadPropertyValue();
        lwc.addLWCListener(this, new Object[] { propertyKey, LWKey.Deleting } );
        keyWasPressed = false;
    }

    /** an optional special undo name for this property */
    public void setUndoName(String name) {
        undoName = name;
    }


    protected void saveText() {
        if (keyWasPressed && lwc != null) {
            if (DEBUG.KEYS) System.out.println(this + " saveText [" + getText() + "]");
            Document doc = getDocument();
            String text = null;
            try {
                if (DEBUG.KEYS) System.out.println(this + " saveText [" + doc.getText(0, doc.getLength()) + "]");
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                getEditorKit().write(buf, doc, 0, doc.getLength());
                text = buf.toString();
                if (DEBUG.KEYS) System.out.println(this + " EDITOR KIT OUTPUT [" + text + "]");
            } catch (Exception e) {
                e.printStackTrace();
            }
            lwc.setProperty(propertyKey, text);
            if (undoName != null)
                VUE.markUndo(undoName);
            else
                VUE.markUndo();
        }	
    }

    private void loadPropertyValue() {
        String text = null;
        if (lwc != null) {
            try {
                text = (String) lwc.getPropertyValue(propertyKey);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("VueTextPane only handles properties of type String");
            }
            setEditable(true);
        } else {
            setEditable(false);
        }
        if (text == null)
            setText("");
        else
            setText(text);
    }
    
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() == lwc) {
            if (e.getWhat() == LWKey.Deleting) {
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

    

    public String toString()
    {
        return "VueTextPane[" + propertyKey + " " + lwc + "]";
    }

}
