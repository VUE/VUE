/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue.gui;

import tufts.vue.LWKey;
import tufts.vue.LWPropertyProducer;
import tufts.vue.LWPropertyChangeEvent;
import tufts.vue.VueResources;
import tufts.vue.DEBUG;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;


/**
 * A group of GUI components that collectively represent
 * an entire AWT Font value, including font family, size,
 * and style (bold/italic).
 */
public class FontPropertyPanel extends JPanel
    implements ActionListener, LWPropertyProducer, PropertyChangeListener
{
    private static String[] sFontSizes;
    
    /** the font list **/
    static String[] sFontNames = null;
 	
    /** Text color editor button **/
    private ColorMenuButton mColorButton = null;
 	
    /** the Font selection combo box **/
    private JComboBox mFontCombo = null;
    private JComboBox mSizeField;
    private AbstractButton mBoldButton;
    private AbstractButton mItalicButton;
 	
    /** the property name **/
    private Object mPropertyKey;
 	
    private static final Insets NoInsets = new Insets(0,0,0,0);
    private static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    private static final int VertSqueeze = 5;
                
    public FontPropertyPanel()
    {
        setPropertyKey(LWKey.Font); // only font key we have for now

        mFontCombo = new JComboBox(getFontNames());
        mFontCombo.addActionListener(this);
        //mFontCombo.addActionListener(GUI.ActionListener);
        //GUI.handleActionEventsFor(mFontCombo);
        
        Font f = mFontCombo.getFont();
        Font menuFont = f.deriveFont((float) f.getSize()-2);        
        mFontCombo.setFont(menuFont);
        mFontCombo.setPrototypeDisplayValue("Ludica Sans Typewriter"); // biggest font name to bother sizing to
        
        if (GUI.isMacAqua()) // bring it down one to line up with editor in Aqua
            mFontCombo.setBorder(new EmptyBorder(1,0,0,0));
        
        if (sFontSizes == null)
            sFontSizes = VueResources.getStringArray("fontSizes");
        mSizeField = new JComboBox(sFontSizes);
        mSizeField.setEditable(true);
        
        if (GUI.isMacAqua()) {
            //mFontCombo.setBackground(GUI.getToolbarColor());
            //mSizeField.setBackground(GUI.getToolbarColor());
        } else {
            mFontCombo.setBackground(GUI.getVueColor());
            mSizeField.setBackground(GUI.getVueColor());
        }
        
        if (mSizeField.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField sizeEditor = (JTextField) mSizeField.getEditor().getEditorComponent();
            sizeEditor.setColumns(2); // not exactly character columns

            /*
            if (GUI.isMacAqua() == false) {
                try {
                    sizeEditor.setBackground(GUI.getMenuBackground());
                    //sizeEditor.setBackground(GUI.getTheme().getMenuBackground());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            */
        }

        mSizeField.addActionListener(this);
        f = mSizeField.getFont();
        Font sizeFont = f.deriveFont((float) f.getSize()-2);        
        mSizeField.setFont( sizeFont);
 		
        mBoldButton = new VueButton.Toggle("font.button.bold", this);
        mItalicButton = new VueButton.Toggle("font.button.italic", this);
        //mBoldButton = new VueButton.Toggle("font.button.bold");
        //mItalicButton = new VueButton.Toggle("font.button.italic");

        add(Box.createHorizontalStrut(4));
        add(mFontCombo);
        add(mSizeField);
        add(Box.createHorizontalStrut(1)); // add this affects vertical preferred size of mSizeField!
        add(mBoldButton);
        add(mItalicButton);
        //add(mTextColorButton);
 	
        setFontValue(GUI.FONT_DEFAULT);

        initColors(GUI.getToolbarColor());

        //addPropertyChangeListener(this);
        
    }

    /** interface PropertyChangeListener */
    public void propertyChange(PropertyChangeEvent e) {
        System.out.println(this + " " + e);
    }
    
    private void initColors( Color pColor) {
        mBoldButton.setBackground( pColor);
        mItalicButton.setBackground( pColor);
    }

    public void XaddNotify()
    {
        if (GUI.isMacAqua()) {
            // font size edit box asymmetrically tall if left to default
            Dimension d = mSizeField.getPreferredSize();
            d.height += 1;
            mSizeField.setMaximumSize(d);
        }
        mSizeField.setEditable(true);
        super.addNotify();
    }

    // as this can sometimes take a while, we can call this manually
    // during startup to control when we take the delay.
    private static Object sFontNamesLock = new Object();
    static String[] getFontNames()
    {
        synchronized (sFontNamesLock) {
            if (sFontNames == null){
                if (DEBUG.INIT)
                    new Throwable("FYI: loading system fonts...").printStackTrace();
                sFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            }
        }
        return sFontNames;
    }
 	
    public void setPropertyKey(Object key) {
        mPropertyKey = key;
    }

    /** interface LWPropertyProducer */
    public Object getPropertyKey() {
        return mPropertyKey;
    }
 	
    /** interface LWPropertyProducer */
    public Object getPropertyValue() {
        return getFontValue();
    }
    
    /** interface LWPropertyProducer */
    public void setPropertyValue(Object value) {
        setFontValue((Font) value);
    }
 	
    /**
     * @return the current Font represented by the state of the FontEditorPanel
     **/
    public Font getFontValue() {
        return makeFont();
    }
 	
    /**
     * setFontValue()
     **/

    private boolean mIgnoreActionEvents = false;
    public void setFontValue(Font font) {
        if (DEBUG.TOOL) System.out.println(this + " setFontValue " + font);

        String familyName = font.getFamily();

        mIgnoreActionEvents = true;

        mFontCombo.setSelectedItem(familyName);
        mItalicButton.setSelected(font.isItalic());
        mBoldButton.setSelected(font.isBold());
        mSizeField.setSelectedItem(""+font.getSize());
        
        mIgnoreActionEvents = false;
    }
 	
    public void actionPerformed(ActionEvent e) {
        if (mIgnoreActionEvents) {
            if (DEBUG.TOOL) System.out.println(this + " ActionEvent ignored: " + e.getActionCommand());
        } else {
            if (DEBUG.TOOL) System.out.println(this + " actionPerformed " + e);
            GUI.propertyProducerChanged(this);
            //fireFontChanged(null, makeFont());
        }
    }
 	
    /**
     * @return a Font constructed from the current state of the gui elements
     **/
    private Font makeFont()
    {
        String name = (String) mFontCombo.getSelectedItem();
 	 	
        int style = Font.PLAIN;
        if (mItalicButton.isSelected())
            style |= Font.ITALIC;
        if (mBoldButton.isSelected())
            style |= Font.BOLD;
        //int size = (int) mSizeField.getValue();
        int size = 12;
        try {
            size = Integer.parseInt((String) mSizeField.getSelectedItem());
        } catch (Exception e) {
            System.err.println(e);
        }
 	 		
        return new Font(name, style, size);
    }
 	
    private int findFontName( String name) {
 		
        //System.out.println("!!! Searching for font: "+name);
        for( int i=0; i< sFontNames.length; i++) {
            if( name.equals(  sFontNames[i]) ) {
                //System.out.println("  FOUND: "+name+" at "+i);
                return i;
            }
        }
        return -1;
    }

    /*
    private void fireFontChanged( Font pOld, Font pNew) {
        PropertyChangeListener [] listeners = getPropertyChangeListeners() ;
        PropertyChangeEvent  event = new LWPropertyChangeEvent(this, getPropertyKey(), pOld, pNew);
        if (listeners != null) {
            for( int i=0; i<listeners.length; i++) {
                if (DEBUG.TOOL) System.out.println(this + " fireFontChanged to " + listeners[i]);
                listeners[i].propertyChange(event);
            }
        }
    }
    */
    
    public String toString() {
        return "FontPropertyPanel[" + getPropertyKey() + " " + makeFont() + "]";
    }

    public static void main(String[] args) {
        System.out.println("FontEditorPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        tufts.vue.VUE.init(args);
        
        //so doesn't bother to load system fonts
        //sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" };

        tufts.Util.displayComponent(new FontPropertyPanel());
    }
     
}
