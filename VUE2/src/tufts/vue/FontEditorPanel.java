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
import tufts.vue.gui.formattingpalette.AlignmentDropDown;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.StyleSheet;

import com.lightdev.app.shtm.AttributeComponent;
import com.lightdev.app.shtm.SHTMLDocument;
import com.lightdev.app.shtm.SHTMLEditorKit;
import com.lightdev.app.shtm.SHTMLEditorKitActions;
import com.lightdev.app.shtm.SHTMLEditorPane;
import com.lightdev.app.shtm.SHTMLPanelImpl;
import com.lightdev.app.shtm.StyleSelector;
import com.lightdev.app.shtm.Util;



/**
 * This creates a font editor panel for editing fonts in the UI
 *
 * @version $Revision: 1.58 $ / $Date: 2007-08-30 21:50:34 $ / $Author: sfraize $
 *
 */
public class FontEditorPanel extends JPanel
    implements CaretListener
               //,ActiveListener
               //implements LWEditor
//    implements ActionListener, VueConstants//, PropertyChangeListener//implements PropertyChangeListener
{
    private static String[] sFontSizes;
    
    /** the font list **/
    static String[] sFontNames = null;
 	
    /** the Font selection combo box **/
    private final JComboBox mFontCombo;
    private final JComboBox mSizeField;
    private final AbstractButton mBoldButton;
    private final AbstractButton mItalicButton;
    private final AbstractButton mUnderlineButton;
    private final AlignmentDropDown alignmentButton;
    private static ColorMenuButton mTextColorButton;
    private final AbstractButton orderedListButton = new VueButton("list.button.ordered");
    private final AbstractButton unorderedListButton = new VueButton("list.button.unordered");
	
    /** the property name **/
    private final Object mPropertyKey;
 	
    private static final Insets NoInsets = new Insets(0,0,0,0);
    private static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    private static final int VertSqueeze = 5;
    
    //plain text action listener
    final ActionListener styleChangeHandler;
    private FontPropertyHandler fontPropertyHandler = null;
    
    //rich text actions
    private final SHTMLEditorKitActions.BoldAction richBoldAction = new  SHTMLEditorKitActions.BoldAction(null);
    private final SHTMLEditorKitActions.ItalicAction richItalicAction = new SHTMLEditorKitActions.ItalicAction(null);
    private final SHTMLEditorKitActions.UnderlineAction richUnderlineAction = new SHTMLEditorKitActions.UnderlineAction(null);
	private final SHTMLEditorKitActions.ToggleListAction toggleBulletsAction = new SHTMLEditorKitActions.ToggleListAction(null,"toggleListAction",HTML.Tag.UL);
    private final SHTMLEditorKitActions.ToggleListAction toggleNumbersAction = new SHTMLEditorKitActions.ToggleListAction(null, "toggleNumbersAction", HTML.Tag.OL);	  
    private final SHTMLEditorKitActions.ToggleAction paraAlignLeftAction = new SHTMLEditorKitActions.ToggleAction(null, "paraAlignLeftAction",CSS.Attribute.TEXT_ALIGN, com.lightdev.app.shtm.Util.CSS_ATTRIBUTE_ALIGN_LEFT);
    private final SHTMLEditorKitActions.ToggleAction paraAlignCenterAction = new SHTMLEditorKitActions.ToggleAction(null, "paraAlignCenterAction",CSS.Attribute.TEXT_ALIGN, com.lightdev.app.shtm.Util.CSS_ATTRIBUTE_ALIGN_CENTER);
    private final SHTMLEditorKitActions.ToggleAction paraAlignRightAction = new SHTMLEditorKitActions.ToggleAction(null, "paraAlignRightAction",CSS.Attribute.TEXT_ALIGN, com.lightdev.app.shtm.Util.CSS_ATTRIBUTE_ALIGN_RIGHT);
    private final SHTMLEditorKitActions.FontFamilyAction fontFamilyAction = new SHTMLEditorKitActions.FontFamilyAction(null);
    private final SHTMLEditorKitActions.FontSizeAction fontSizeAction = new SHTMLEditorKitActions.FontSizeAction(null);
    private final SHTMLEditorKitActions.FontColorAction fontColorAction = new SHTMLEditorKitActions.FontColorAction(null);
    private final AlignmentListener alignmentListener = new AlignmentListener();
    
//    dynRes.addAction(fontFamilyAction, new SHTMLEditorKitActions.FontFamilyAction(this));
//    dynRes.addAction(fontSizeAction, new SHTMLEditorKitActions.FontSizeAction(this));
//    dynRes.addAction(fontColorAction, new SHTMLEditorKitActions.FontColorAction(this));

    public FontEditorPanel(Object propertyKey)
    {
    	//super(BoxLayout.X_AXIS);
    	setLayout(new GridBagLayout());
    	//ActiveInstance.addAllActiveListener(this);
    	VUE.addActiveListener(LWComponent.class, this);
        mPropertyKey = propertyKey;

        setFocusable(false);
        
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
        mFontCombo.setRenderer(new CustomComboBoxRenderer());        
        Font f = mFontCombo.getFont();
        Font menuFont = f.deriveFont((float) 9);        
        mFontCombo.setFont(menuFont);
        mFontCombo.setPrototypeDisplayValue("Ludica Sans Typewriter"); // biggest font name to bother sizing to
        if (false) {
            Dimension comboSize = mFontCombo.getPreferredSize();
            comboSize.height -= 2; // if too small (eg, -3), will trigger major swing layout bug
            mFontCombo.setMaximumSize(comboSize);
        }
        if (GUI.isMacAqua())
            mFontCombo.setBorder(new EmptyBorder(1,0,0,0));

        fontPropertyHandler = new FontPropertyHandler(mFontCombo);
        mFontCombo.setMaximumRowCount(30);
        mFontCombo.setOpaque(false);
        // Set selected items BEFORE adding action listeners, or during startup
        // we think a user has actually selected this item!
        mFontCombo.setSelectedItem("Arial");
        mFontCombo.addActionListener(fontPropertyHandler);
//         We don't appear to get any events here!
//         mFontCombo.addItemListener(new ItemListener() {
//                 public void itemStateChanged(ItemEvent e) {
//                     System.err.println(mFontCombo + ": itemStateChanged " + e);
//                 }
//             });
        
        //mFontCombo.setBorder(new javax.swing.border.LineBorder(Color.green, 2));
        //mFontCombo.setBackground(Color.white); // handled by L&F tweaks in VUE.java
        //mFontCombo.setMaximumSize(new Dimension(50,50)); // no effect
        //mFontCombo.setSize(new Dimension(50,50)); // no effect
        //mFontCombo.setBorder(null); // already has no border

        //mSizeField = new NumericField( NumericField.POSITIVE_INTEGER, 2 );
        
        if (sFontSizes == null)
            sFontSizes = VueResources.getStringArray("fontSizes");
        mSizeField = new JComboBox(sFontSizes);
        mSizeField.setRenderer(new CustomComboBoxRenderer());
  //      mSizeField.setPrototypeDisplayValue("10000");
        mSizeField.setEditable(true);
        mSizeField.setOpaque(false);
        mSizeField.setMaximumRowCount(30);
        mSizeField.setSelectedItem("13");
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
            sizeEditor.setColumns(3); // not exactly character columns
        }
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
//        }

        //mSizeField.getEditor().getEditorComponent().setSize(30,10);
        
        //mSizeField.addActionListener( this);
        mSizeField.addActionListener(new LWPropertyHandler<Integer>(LWKey.FontSize, mSizeField) {
                public Integer produceValue() { return new Integer((String) mSizeField.getSelectedItem()); }
                public void displayValue(Integer value) { mSizeField.setSelectedItem(""+value); }
            });


        f = mSizeField.getFont();
        //Font sizeFont = f.deriveFont((float) f.getSize()-2);
        Font sizeFont= f.deriveFont((float) 9);        
        mSizeField.setFont( sizeFont);
        //mSizeField.setMaximumSize(mSizeField.getPreferredSize());
        //mSizeField.setBackground(VueTheme.getVueColor());

        styleChangeHandler =
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

                public void setEnabled(boolean enabled) {
                    mBoldButton.setEnabled(enabled);
                    mItalicButton.setEnabled(enabled);
                    //mUnderlineButton.setEnabled(enabled);
                }
        };
 		
        mBoldButton = new VueButton.Toggle("font.button.bold",styleChangeHandler);
        
        mItalicButton = new VueButton.Toggle("font.button.italic", styleChangeHandler);
        mUnderlineButton = new VueButton.Toggle("font.button.underline", styleChangeHandler);
        alignmentButton = new AlignmentDropDown();

        alignmentButton.getComboBox().addActionListener
            (new LWPropertyHandler<LWComponent.Alignment>(LWKey.Alignment, alignmentButton.getComboBox()) {
                public LWComponent.Alignment produceValue() {
                    switch (alignmentButton.getComboBox().getSelectedIndex()) {
                    case 0: return LWComponent.Alignment.LEFT;
                    case 1: return LWComponent.Alignment.CENTER;
                    case 2: return LWComponent.Alignment.RIGHT;
                    }
                    return LWComponent.Alignment.LEFT;
                }
                public void displayValue(LWComponent.Alignment align) {
                    alignmentButton.getComboBox().setSelectedIndex(align.ordinal());
                }
            });

        
         Color[] textColors = VueResources.getColorArray("textColorValues");
        //String[] textColorNames = VueResources.getStringArray("textColorNames");
        mTextColorButton = new ColorMenuButton(textColors, true);
        mTextColorButton.setColor(Color.black);
        mTextColorButton.setPropertyKey(LWKey.TextColor);
        mTextColorButton.setToolTipText("Text Color");
        //mTextColorButton.addPropertyChangeListener(this);
        
        //Set up Labels...
        JLabel styleLabel = new JLabel("Style :");
		styleLabel.setForeground(new Color(51,51,51));
		styleLabel.setFont(tufts.vue.VueConstants.SmallFont);
		
		JLabel textLabel = new JLabel("Text :");
                textLabel.setLabelFor(mFontCombo);
		textLabel.setForeground(new Color(51,51,51));
		textLabel.setFont(tufts.vue.VueConstants.SmallFont);


        textLabel.addMouseListener(new MouseAdapter() {
                // double-click on text color label swaps in with fill color
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() > 1 && e.getClickCount() % 2 == 0) {
                        final Color fill = FillToolPanel.mFillColorButton.getColor();
                        final Color text = mTextColorButton.getColor();
                        FillToolPanel.mFillColorButton.selectValue(text);
                        mTextColorButton.selectValue(fill);
                    }
                }
            });
                
		//Done With Labels..
         
        GridBagConstraints gbc = new GridBagConstraints();
        
        //Layout Panel
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.gridwidth=1;
        gbc.insets = new Insets(1,3,1,1);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.BOTH;               
    	
		add(textLabel,gbc);
		
		
		gbc.gridy=0;
        gbc.gridx=1;        
        gbc.gridwidth=5;
        gbc.insets = new Insets(1,3,1,1);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.BOTH;                                    
        add(mFontCombo,gbc);
        
        gbc.gridy=0;
        gbc.gridx=6;        
        gbc.fill=GridBagConstraints.NONE;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridwidth=1;
        gbc.weightx=0.3;
        //   gbc.gridheight=1;
        gbc.ipady=5;
        //gbc.ipadx=5;        
        gbc.insets=new Insets(1,5,1,1);
        add(mSizeField,gbc);
        
        gbc.gridy=0;
        gbc.gridx=7;
        gbc.fill=GridBagConstraints.REMAINDER;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridheight=1;
        gbc.gridwidth=1;
        gbc.ipadx=0;
        gbc.ipady=0;
        gbc.insets=new Insets(0,1,1,1);
        add(mTextColorButton,gbc);
        
        gbc.fill=GridBagConstraints.NONE;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridwidth=1;
        gbc.gridheight=0;
        gbc.gridy=1;
        gbc.gridx=0;        
        add(styleLabel,gbc);
        
        
        gbc.gridy=1;
        gbc.gridx=1;
        add(mBoldButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=2;
        add(mItalicButton,gbc);
     
        gbc.gridy=1;
        gbc.gridx=3;        
        gbc.fill=GridBagConstraints.NONE;
        mUnderlineButton.setEnabled(false);
        add(mUnderlineButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=4;        
        gbc.fill=GridBagConstraints.NONE;
        orderedListButton.setEnabled(false);
        add(orderedListButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=5;        
        gbc.fill=GridBagConstraints.NONE;
        unorderedListButton.setEnabled(false);
        add(unorderedListButton,gbc);
                                        
        gbc.gridy=1;
        gbc.gridx=7;
        gbc.fill=GridBagConstraints.BOTH;
        gbc.insets = new Insets(5,0,0,0);
        alignmentButton.setBorder(BorderFactory.createEmptyBorder());
        alignmentButton.getComboBox().setBorder(BorderFactory.createEmptyBorder());
        //alignmentButton.getComboBox().setEnabled(false);
        add(alignmentButton,gbc);
 	
        //displayValue(VueConstants.FONT_DEFAULT);

        //initColors(VueTheme.getToolbarColor());
    }

    public static ColorMenuButton getTextColorButton()
    {
    	return mTextColorButton;
    }
    private class FontPropertyHandler extends LWPropertyHandler<String>
    {
    	//new LWPropertyHandler<String>(LWKey.FontName, mFontCombo) {
    	
    	public FontPropertyHandler(JComboBox combo)
    	{
    		super(LWKey.FontName,combo);
    	}
        public String produceValue() { return (String) mFontCombo.getSelectedItem(); }
        public void displayValue(String value) { mFontCombo.setSelectedItem(value); }
    }
    private class CustomComboBoxRenderer extends DefaultListCellRenderer {

    	public Component getListCellRendererComponent( JList list,
    	Object value,
    	int index, boolean isSelected, boolean cellHasFocus) {
    	Color bg = GUI.getTextHighlightColor();
    	if (isSelected) {
    	setBackground(bg);
    	setForeground(Color.black);
    	}
    	else {
    	setBackground(list.getBackground());
    	setForeground(list.getForeground());
    	}

    	setText((value == null) ? "" : ((String) value).toString());    	
    	

    	return this;
    	}
    	}

//     public void XpropertyChange(PropertyChangeEvent e)
//     {
//         System.out.println("FONTEDITORPANEL: " + e);
//         /*
//         if (e instanceof LWPropertyChangeEvent) {

//             final String propertyName = e.getPropertyName();

//             if (mIgnoreEvents) {
//                 if (DEBUG.TOOL) out("propertyChange: skipping " + e + " name=" + propertyName);
//                 return;
//             }
            
//             if (DEBUG.TOOL) out("propertyChange: [" + propertyName + "] " + e);
	  		
//             mIgnoreLWCEvents = true;
//             VueBeans.applyPropertyValueToSelection(VUE.getSelection(), propertyName, e.getNewValue());
//             mIgnoreLWCEvents = false;
            
//             if (VUE.getUndoManager() != null)
//                 VUE.getUndoManager().markChangesAsUndo(propertyName);

//             if (mState != null)
//                 mState.setPropertyValue(propertyName, e.getNewValue());
//             else
//                 out("mState is null");

//             if (mDefaultState != null)
//                 mDefaultState.setPropertyValue(propertyName, e.getNewValue());
//             else
//                 out("mDefaultState is null");

//             if (DEBUG.TOOL && DEBUG.META) out("new state " + mState);

//         } else {
//             // We're not interested in "ancestor" events, icon change events, etc.
//             if (DEBUG.TOOL && DEBUG.META) out("ignored AWT/Swing: [" + e.getPropertyName() + "] from " + e.getSource().getClass());
//         }
//         */

//     }
    
    
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

//     public Object getPropertyKey() {
//         return mPropertyKey;
//     }
 	
//     public Object produceValue() {
//         tufts.Util.printStackTrace(this + " asked to produce aggregate value");
//         return null;
//     }
    
//     public void displayValue(Object value) {
//         tufts.Util.printStackTrace(this + " asked to display aggregate value");
//         /*
//         final Font font = (Font) value;
//         mFontCombo.setSelectedItem(font.getFamily());
//         mItalicButton.setSelected(font.isItalic());
//         mBoldButton.setSelected(font.isBold());
//         mSizeField.setSelectedItem(""+font.getSize());
//         */
//     }

    public String toString() {
        return "FontEditorPanel[" + mPropertyKey + "]";
        //return "FontEditorPanel[" + getPropertyKey() + "]";
        //return "FontEditorPanel[" + getKey() + " " + makeFont() + "]";
    }


    protected void out(Object o) {
        System.out.println(this + ": " + (o==null?"null":o.toString()));
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
 	
    public static void main(String[] args) {
        System.out.println("FontEditorPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.init(args);
        
        //sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts

        VueUtil.displayComponent(new FontEditorPanel(LWKey.Font));
    }


    public void activeChanged(ActiveEvent e, LWComponent active) {
		//Object hasn't changed do nothing...
		if (e.active == e.oldActive)
			return;
		else if (e.active instanceof LWText)
		{
			final LWText text = (LWText)e.active;
			text.getRichLabelBox().addCaretListener(this);
			richBoldAction.setEditorPane(text.getRichLabelBox());
			richItalicAction.setEditorPane(text.getRichLabelBox());
			richUnderlineAction.setEditorPane(text.getRichLabelBox());
			toggleBulletsAction.setEditorPane(text.getRichLabelBox());
			toggleNumbersAction.setEditorPane(text.getRichLabelBox());
			paraAlignLeftAction.setEditorPane(text.getRichLabelBox());
			paraAlignCenterAction.setEditorPane(text.getRichLabelBox());
			paraAlignRightAction.setEditorPane(text.getRichLabelBox());
			fontFamilyAction.setEditorPane(text.getRichLabelBox());
		    fontSizeAction.setEditorPane(text.getRichLabelBox());
		    fontColorAction.setEditorPane(text.getRichLabelBox());
		    		
			mUnderlineButton.setEnabled(true);
			orderedListButton.setEnabled(true);
			unorderedListButton.setEnabled(true);
			//alignmentButton.getComboBox().setEnabled(true);
			
			mBoldButton.removeActionListener(styleChangeHandler);
			mBoldButton.addActionListener(richBoldAction);			
			
			mItalicButton.removeActionListener(styleChangeHandler);
			mItalicButton.addActionListener(richItalicAction);			
			
			mUnderlineButton.removeActionListener(styleChangeHandler);			
			mUnderlineButton.addActionListener(richUnderlineAction);						
			
						
			orderedListButton.addActionListener(toggleNumbersAction);
			unorderedListButton.addActionListener(toggleBulletsAction);
			
			mFontCombo.addActionListener(fontFamilyAction);
			mFontCombo.removeActionListener(fontPropertyHandler);
			
			mSizeField.removeActionListener(fontPropertyHandler);
			mSizeField.addActionListener(fontSizeAction);
			
			//mTextColorButton.removeActionListener(fontPropertyHandler);
			//
			mTextColorButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
				 	Color color =FontEditorPanel.getTextColorButton().getColor();
				    SimpleAttributeSet set = new SimpleAttributeSet();
				        String colorString = "#" + Integer.toHexString(
				                color.getRGB()).substring(2);
				        Util.styleSheet().addCSSAttribute(set,
				                CSS.Attribute.COLOR, colorString);
				        
				            set.addAttribute(HTML.Attribute.COLOR, colorString);
				        				        				 
				            text.getRichLabelBox().applyAttributes(set, false);
				}
			});
			
			alignmentButton.getComboBox().addItemListener(alignmentListener);

			
		}
		else
		{			
			mUnderlineButton.setEnabled(false);
			orderedListButton.setEnabled(false);
			unorderedListButton.setEnabled(false);
			//alignmentButton.setEnabled(false);
			
			mBoldButton.removeActionListener(richBoldAction);
			mBoldButton.addActionListener(styleChangeHandler);			
			
			mItalicButton.removeActionListener(richItalicAction);
			mItalicButton.addActionListener(styleChangeHandler);
			
			mUnderlineButton.removeActionListener(richUnderlineAction);			
			//mUnderlineButton.addActionListener(styleChangeHandler);
			
			mFontCombo.removeActionListener(fontFamilyAction);
			mFontCombo.addActionListener(fontPropertyHandler);
			
			//orderedListButton.removeActionListener(toggleNumbersAction);
			//unorderedListButton.removeActionListener(toggleBulletsAction);
	
		}
				
	}

	private class AlignmentListener implements ItemListener
	{
			public void itemStateChanged(ItemEvent arg0) 
			{
				if (arg0.getStateChange() == ItemEvent.SELECTED)					
				{						
					int itemCase = ((Integer)arg0.getItem()).intValue();
					switch (itemCase)
					{
						case 0:
							paraAlignLeftAction.actionPerformed(null);
							break;
						case 1:
							paraAlignCenterAction.actionPerformed(null);
							break;
						case 2:
							paraAlignRightAction.actionPerformed(null);
							break;
						default:
							//if in doubt
							paraAlignLeftAction.actionPerformed(null);
							break;
					}														
				}
			}			
	}
	
	public AttributeSet getMaxAttributes(RichTextBox text,final int caretPosition) 
	{
		
	    final Element paragraphElement = ((SHTMLDocument)text.getDocument()).getParagraphElement(caretPosition);
	      final StyleSheet styleSheet = ((SHTMLDocument)text.getDocument()).getStyleSheet();
	      return SHTMLPanelImpl.getMaxAttributes(paragraphElement, styleSheet);
	}
	AttributeSet getMaxAttributes(RichTextBox editorPane,
	          String elemName)
	  {
		SHTMLDocument doc = (SHTMLDocument)editorPane.getDocument();
	      Element e = doc.getCharacterElement(editorPane.getSelectionStart());
	      StyleSheet s = doc.getStyleSheet();
	      if(elemName != null && elemName.length() > 0) {
	          e = Util.findElementUp(elemName, e);
	          return SHTMLPanelImpl.getMaxAttributes(e, s);
	      }
	      final MutableAttributeSet maxAttributes = (MutableAttributeSet)SHTMLPanelImpl.getMaxAttributes(e, s);
	      final StyledEditorKit editorKit = (StyledEditorKit)editorPane.getEditorKit();
	      final MutableAttributeSet inputAttributes = editorKit.getInputAttributes();
	      maxAttributes.addAttributes(inputAttributes);
	      return maxAttributes;
	  }

	private final void updateFormatControls(CaretEvent e)
	{
			final RichTextBox text = (RichTextBox)e.getSource();
		/*    SHTMLDocument doc = (SHTMLDocument)text.getDocument();
	        Element element = doc.getParagraphElement(text.getCaretPosition()-1);
	        AttributeSet set = element.getAttributes();
	        Element charElem =doc.getCharacterElement(text.getCaretPosition()-1);
	        AttributeSet charSet = charElem.getAttributes();
	        
	        Enumeration enume = charSet.getAttributeNames();
	        */
	    Component c;
	    Action action;
	    int count = getComponentCount();
	    
	    AttributeSet a = getMaxAttributes(text, null);
	    for(int i = 0; i < count; i++) {
	      c = this.getComponent(i);
	      if(c instanceof AttributeComponent) {
	      /*  if(c instanceof StyleSelector) {
	          SetStyleAction ssa = (SetStyleAction) ((StyleSelector) c).getAction();
	          final AttributeSet oldAttibuteSet = ((AttributeComponent) c).getValue();
	          if(! a.isEqual(oldAttibuteSet)){
	              ssa.setIgnoreActions(true);
	              ((AttributeComponent) c).setValue(a);
	              ssa.setIgnoreActions(false);
	          }
	        }*/
	        //else {
	          ((AttributeComponent) c).setValue(a);
	       // }
	      }
	      else if(c instanceof AbstractButton) {
	        action = ((AbstractButton) c).getAction();
	        if((action != null) && (action instanceof AttributeComponent)) {
	          ((AttributeComponent) action).setValue(a);
	        }
	      }
	    }

	        //while (enume.hasMoreElements())
	       // {
	       // 	Object o = enume.nextElement();
	       // 	System.out.println("name : " + o.toString());
	      //  	System.out.println("val : " + charSet.getAttribute(o));
	       // }
	        /*
	        if (charSet.getAttribute("font-style") != null.equals("italic"))
	        	mItalicButton.setSelected(true);
	        else
	        	mItalicButton.setSelected(false);
	        */
	        //String val = (String)set.getAttribute("text-align");
	       // if (val == null)
	       // 	val = "left";
	        /*
	        if (val.equals("left") && alignmentButton.getComboBox().getSelectedIndex() != 0)	        	
	        	alignmentButton.getComboBox().setSelectedIndex(0);
	       	else if (val.equals("center") && alignmentButton.getComboBox().getSelectedIndex() != 1)
	       		alignmentButton.getComboBox().setSelectedIndex(1);
	        else if (val.equals("right") && alignmentButton.getComboBox().getSelectedIndex() != 2)
	        	alignmentButton.getComboBox().setSelectedIndex(2);
	        */
	        //System.out.println(richItalicAction.getValue().getAttribute("font-style"));
	        
	        
	        /*System.out.println(set.getAttribute(HTML.Attribute.FACE));
	        System.out.println(set.getAttribute(HTML.Attribute.SIZE));
	        System.out.println(set.getAttribute(HTML.Attribute.COLOR));
	        System.out.println(set.getAttribute(HTML.Attribute.ALIGN));
	       */
	        
	       // System.out.println(element.getAttributes());
	}
	
	  public void caretUpdate(final CaretEvent e) {
		  
		      EventQueue.invokeLater(new Runnable(){

		            public void run() {
		                //updateFormatControls(e);
		            }            
		        });		  
		  }

}