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

package tufts.vue;

import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;


/**
 * This creates a font editor panel for editing fonts in the UI
 *
 * @version $Revision: 1.38 $ / $Date: 2007-02-06 21:50:39 $ / $Author: sfraize $
 *
 */
public class FontEditorPanel extends Box
                                     //implements LWEditor
//    implements ActionListener, VueConstants//, PropertyChangeListener
{
    private static String[] sFontSizes;
    
    /** the font list **/
    static String[] sFontNames = null;
 	
    /** the Font selection combo box **/
    private final JComboBox mFontCombo;
    private final JComboBox mSizeField;
    private final AbstractButton mBoldButton;
    private final AbstractButton mItalicButton;
 	
    /** the property name **/
    private final Object mPropertyKey;
 	
    private static final Insets NoInsets = new Insets(0,0,0,0);
    private static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    private static final int VertSqueeze = 5;

    public FontEditorPanel(Object propertyKey)
    {
	super(BoxLayout.X_AXIS);

        mPropertyKey = propertyKey;

        //setFocusable(false);
        
        //Box box = Box.createHorizontalBox();
        // we set this border only to create a gap around these components
        // so they don't expand to 100% of the height of the region they're
        // in -- okay, that's not good enough -- will have to find another
        // way to constrain the combo-box.
        /*
        if (true)
            setBorder(new LineBorder(Color.pink, VertSqueeze));
        else
            setBorder(new EmptyBorder(VertSqueeze,1,VertSqueeze,1));//t,l,b,r
        */

        mFontCombo = new JComboBox(getFontNames());
        Font f = mFontCombo.getFont();
        Font menuFont = f.deriveFont((float) f.getSize()-2);        
        mFontCombo.setFont(menuFont);
        mFontCombo.setPrototypeDisplayValue("Ludica Sans Typewriter"); // biggest font name to bother sizing to
        if (false) {
            Dimension comboSize = mFontCombo.getPreferredSize();
            comboSize.height -= 2; // if too small (eg, -3), will trigger major swing layout bug
            mFontCombo.setMaximumSize(comboSize);
        }
        if (GUI.isMacAqua())
            mFontCombo.setBorder(new EmptyBorder(1,0,0,0));

        mFontCombo.setOpaque(false);
        mFontCombo.addActionListener(new LWPropertyHandler<String>(LWKey.FontName) {
                public String produceValue() { return (String) mFontCombo.getSelectedItem(); }
                public void displayValue(String value) { mFontCombo.setSelectedItem(value); }
            });
        
        //mFontCombo.setBorder(new javax.swing.border.LineBorder(Color.green, 2));
        //mFontCombo.setBackground(Color.white); // handled by L&F tweaks in VUE.java
        //mFontCombo.setMaximumSize(new Dimension(50,50)); // no effect
        //mFontCombo.setSize(new Dimension(50,50)); // no effect
        //mFontCombo.setBorder(null); // already has no border

        //mSizeField = new NumericField( NumericField.POSITIVE_INTEGER, 2 );
        
        if (sFontSizes == null)
            sFontSizes = VueResources.getStringArray("fontSizes");
        mSizeField = new JComboBox(sFontSizes);
        mSizeField.setEditable(true);
        mSizeField.setOpaque(false);
        
        /*
        if (GUI.isMacAqua()) {
            mFontCombo.setBackground(VueTheme.getToolbarColor());
            mSizeField.setBackground(VueTheme.getToolbarColor());
        } else {
            mFontCombo.setBackground(VueTheme.getVueColor());
            mSizeField.setBackground(VueTheme.getVueColor());
        }
        */
        
        //mSizeField.setPrototypeDisplayValue("100"); // no help in making it smaller
        //System.out.println("EDITOR " + mSizeField.getEditor());
        //System.out.println("EDITOR-COMP " + mSizeField.getEditor().getEditorComponent());

        if (mSizeField.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField sizeEditor = (JTextField) mSizeField.getEditor().getEditorComponent();
            sizeEditor.setColumns(2); // not exactly character columns

            /*
            if (!GUI.isMacAqua()) {
                try {
                    sizeEditor.setBackground(VueTheme.getTheme().getMenuBackground());
                } catch (Exception e) {
                    System.err.println("FontEditorPanel: " + e);
                }
            }
            */
            //sizeEditor.setPreferredSize(new Dimension(20,10)); // does squat
            
            // the default size for a combo-box editor field is 9 chars
            // wide, and it's NOT configurable thru system L&F properties
            // -- it's hardcoded into Basic and Metal look and feels!  God
            // knows what will happen on windows L&F.  BTW: windows look
            // and feel has better combo-boxes -- they display menu
            // contents in sizes bigger than the top display box
            // (actually, both do that when they can resize), and they're
            // picking up more of our color override settings (and the
            // at-right button appears closer to Melanie's comps).
        }

        //mSizeField.getEditor().getEditorComponent().setSize(30,10);
        
        //mSizeField.addActionListener( this);
        mSizeField.addActionListener(new LWPropertyHandler<Integer>(LWKey.FontSize) {
                //public Object getPropertyValue() { return producePropertyValue(); }
                //public void setPropertyValue(Object o) { loadPropertyValue(o); }
                //Object producePropertyValue() { return new Integer((String) mSizeField.getSelectedItem()); }
                public Integer produceValue() { return new Integer((String) mSizeField.getSelectedItem()); }
                public void displayValue(Integer value) { mSizeField.setSelectedItem(""+value); }
            });


        f = mSizeField.getFont();
        Font sizeFont = f.deriveFont((float) f.getSize()-2);        
        mSizeField.setFont( sizeFont);
        //mSizeField.setMaximumSize(mSizeField.getPreferredSize());
        //mSizeField.setBackground(VueTheme.getVueColor());

        final ActionListener styleChangeHandler =
            new LWPropertyHandler<Integer>(LWKey.FontStyle) {
                public Integer produceValue() {
                    int style = Font.PLAIN;
                    if (mItalicButton.isSelected())
                        style |= Font.ITALIC;
                    if (mBoldButton.isSelected())
                        style |= Font.BOLD;
                    return style;
                }
                public void displayValue(Integer value) {
                    final int style = value;
                    mBoldButton.setSelected((style & Font.BOLD) != 0);
                    mItalicButton.setSelected((style & Font.ITALIC) != 0);
                }
        };
 		
        mBoldButton = new VueButton.Toggle("font.button.bold", styleChangeHandler);
        mItalicButton = new VueButton.Toggle("font.button.italic", styleChangeHandler);

        /*
          Color [] textColors = VueResources.getColorArray("textColorValues");
          String [] textColorNames = VueResources.getStringArray("textColorNames");
          mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
          //mTextColorButton.setBackground( bakColor);
          ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
          BlobIcon textBlob = new BlobIcon();
          textBlob.setOverlay( textIcon );
          mTextColorButton.setIcon(textBlob);
          mTextColorButton.setPropertyName( LWKey.TextColor);
          mTextColorButton.setBorderPainted(false);
          mTextColorButton.setMargin(ButtonInsets);
          mTextColorButton.addActionListener(this);
        */
         
        if (false) {
            JLabel label = new JLabel("   Text: ");
            label.setFont(VueConstants.FONT_SMALL);
            add(label);
        } else {
            add(Box.createHorizontalStrut(4));
        }
        add(mFontCombo);
        add(mSizeField);
        add(Box.createHorizontalStrut(1)); // add this affects vertical preferred size of mSizeField!
        add(mBoldButton);
        add(mItalicButton);
        //add(mTextColorButton);
 	
        //displayValue(VueConstants.FONT_DEFAULT);

        //initColors(VueTheme.getToolbarColor());
    }

    public void XpropertyChange(PropertyChangeEvent e)
    {
        System.out.println("FONTEDITORPANEL: " + e);
        /*
        if (e instanceof LWPropertyChangeEvent) {

            final String propertyName = e.getPropertyName();

            if (mIgnoreEvents) {
                if (DEBUG.TOOL) out("propertyChange: skipping " + e + " name=" + propertyName);
                return;
            }
            
            if (DEBUG.TOOL) out("propertyChange: [" + propertyName + "] " + e);
	  		
            mIgnoreLWCEvents = true;
            VueBeans.applyPropertyValueToSelection(VUE.getSelection(), propertyName, e.getNewValue());
            mIgnoreLWCEvents = false;
            
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(propertyName);

            if (mState != null)
                mState.setPropertyValue(propertyName, e.getNewValue());
            else
                out("mState is null");

            if (mDefaultState != null)
                mDefaultState.setPropertyValue(propertyName, e.getNewValue());
            else
                out("mDefaultState is null");

            if (DEBUG.TOOL && DEBUG.META) out("new state " + mState);

        } else {
            // We're not interested in "ancestor" events, icon change events, etc.
            if (DEBUG.TOOL && DEBUG.META) out("ignored AWT/Swing: [" + e.getPropertyName() + "] from " + e.getSource().getClass());
        }
        */

    }
    
    
    private void initColors( Color pColor) {
        mBoldButton.setBackground( pColor);
        mItalicButton.setBackground( pColor);
    }

    public void addNotify()
    {
        if (GUI.isMacAqua()) {
            // font size edit box asymmetrically tall if left to default
            Dimension d = mSizeField.getPreferredSize();
            d.height += 1;
            mSizeField.setMaximumSize(d);
        }
        
        /* still to risky
        Dimension comboSize = mFontCombo.getPreferredSize();
        comboSize.height -= 2; // if too small (eg, -3), will trigger major swing layout bug
        mFontCombo.setMaximumSize(comboSize);
        */

        /*
        if (mSizeField.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField sizeEditor = (JTextField) mSizeField.getEditor().getEditorComponent();
            //sizeEditor.setColumns(2); // not exactly character columns
            sizeEditor.setBackground(VueTheme.getTheme().getMenuBackground());
        }
        */

        /* too risky: still can trigger massive swing layout bug
        Dimension d = mSizeField.getPreferredSize();
        // Swing layout is royally buggy: if we add the horizontal strut above,
        // we need height-2, if we take it out, height-1 -- !
        d.height -= 2;
        mSizeField.setMaximumSize(d);
        */
        mSizeField.setEditable(true);

        //System.out.println(this + " adding as parent of " + getParent());
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
 	

    public Object getPropertyKey() {
        return mPropertyKey;
    }
 	
    public Object produceValue() {
        tufts.Util.printStackTrace(this + " asked to produce aggregate value");
        return null;
    }
    
    public void displayValue(Object value) {
        tufts.Util.printStackTrace(this + " asked to display aggregate value");
        /*
        final Font font = (Font) value;
        mFontCombo.setSelectedItem(font.getFamily());
        mItalicButton.setSelected(font.isItalic());
        mBoldButton.setSelected(font.isBold());
        mSizeField.setSelectedItem(""+font.getSize());
        */
    }

    /*
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
    */
 	
    /*
     * @return the current Font represented by the state of the FontEditorPanel

    public Font getFontValue() {
        return makeFont();
    }
     */ 	

    /*
     * setValue
     * Generic property editor access

    // get rid of this: should be handled in LWPropertyHandler
    public void setValue( Object pValue) {
        if( pValue instanceof Font)
            setFontValue((Font) pValue);
    }
    **/
    
    /*
    private void fireFontChanged( Font pOld, Font pNew) {
        PropertyChangeListener [] listeners = getPropertyChangeListeners() ;
        PropertyChangeEvent  event = new LWPropertyChangeEvent(this, getPropertyKey(), pOld, pNew);
        if (listeners != null) {
            for( int i=0; i<listeners.length; i++) {
                if (DEBUG.TOOL) System.out.println(this + " fireFontChanged to " + listeners[i]);
                listeners[i].propertyChange( event);
            }
        }
    }
    
    public void actionPerformed(ActionEvent pEvent) {
        // we've never track the old font value here -- S.B. never finished this code
        // todo: so this whole thing could probably use a freakin re-write

        //new Throwable().printStackTrace();
        if (mIgnoreActionEvents) {
            if (DEBUG.TOOL) System.out.println(this + " ActionEvent ignored: " + pEvent.getActionCommand());
        } else {
            if (DEBUG.TOOL) System.out.println(this + " actionPerformed " + pEvent);
            fireFontChanged(null, makeFont());
        }
    }
 	
    /**
     * @return a Font constructed from the current state of the gui elements

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
    */
    
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

    public String toString() {
        return "FontEditorPanel[" + getPropertyKey() + "]";
        //return "FontEditorPanel[" + getKey() + " " + makeFont() + "]";
    }

    public static void main(String[] args) {
        System.out.println("FontEditorPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.init(args);
        
        //sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts

        VueUtil.displayComponent(new FontEditorPanel(LWKey.Font));
    }
     
}
