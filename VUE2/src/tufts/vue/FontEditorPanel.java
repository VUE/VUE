package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import tufts.vue.beans.VueLWCPropertyMapper;


/**
 * FontEditorPanel
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
public class FontEditorPanel extends Box implements ActionListener, VueConstants
{
    /*
    static Icon sItalicOn = VueResources.getImageIcon("italicOnIcon");
    static Icon sItalicOff = VueResources.getImageIcon("italicOffIcon");
    static Icon sBoldOn = VueResources.getImageIcon("boldOnIcon");
    static Icon sBoldOff = VueResources.getImageIcon("boldOffIcon");
    */
    
    //private static String [] sFontSizeMenuLabels = { };
    private static String [] sFontSizes = { "8","9","10","12","14","18","24","36","48"};
    
    /** the font list **/
    static String[] sFontNames = null;
 
 	
    /** Text color editor button **/
    private ColorMenuButton mColorButton = null;
 	
    /** the Font selection combo box **/
    private JComboBox mFontCombo = null;
    private JComboBox mSizeField;
    private AbstractButton mBoldButton;
    private AbstractButton mItalicButton;
 	
    /** the font size **/
    private int mSize = 14;
 	
    /* Text color menu editor **/
    //ColorMenuButton mTextColorButton = null;
    
    /** the property name **/
    private String mPropertyName = LWKey.Font;
 	
    /** the font **/
    private Font mFont = null;
 	
 	
    private static final boolean debug = false;
    private static final Insets NoInsets = new Insets(0,0,0,0);
    private static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    private static final int VertSqueeze = 5;
                
    public FontEditorPanel()
    {
	super(BoxLayout.X_AXIS);

        setFocusable(false);
        if (debug) setBackground(Color.blue);
        
        //Box box = Box.createHorizontalBox();
        /*
        // we set this border only to create a gap around these components
        // so they don't expand to 100% of the height of the region they're
        // in -- okay, that's not good enough -- will have to find another
        // way to constrain the combo-box.

        if (debug)
        setBorder(new javax.swing.border.LineBorder(Color.pink, VertSqueeze));
        else
        setBorder(new javax.swing.border.EmptyBorder(VertSqueeze,1,VertSqueeze,1));//t,l,b,r
        */

        mFontCombo = new JComboBox(getFontNames());
        mFontCombo.addActionListener( this );
        Font f = mFontCombo.getFont();
        Font menuFont = new Font( f.getFontName(), f.getStyle(), f.getSize() - 2);
        mFontCombo.setFont(menuFont);
        mFontCombo.setPrototypeDisplayValue("Ludica Sans Typewriter"); // biggest font name to bother sizing to
        mFontCombo.setMaximumSize(mFontCombo.getPreferredSize());

        //mFontCombo.setBorder(new javax.swing.border.LineBorder(Color.green, 2));
        //mFontCombo.setBackground(Color.white); // handled by L&F tweaks in VUE.java
        //mFontCombo.setMaximumSize(new Dimension(50,50)); // no effect
        //mFontCombo.setSize(new Dimension(50,50)); // no effect
        //mFontCombo.setBorder(null); // already has no border

        //mSizeField = new NumericField( NumericField.POSITIVE_INTEGER, 2 );
        
        mSizeField = new JComboBox(sFontSizes);
        mSizeField.setEditable(true);
        
        //mSizeField.setPrototypeDisplayValue("100"); // no help in making it smaller
        //System.out.println("EDITOR " + mSizeField.getEditor());
        //System.out.println("EDITOR-COMP " + mSizeField.getEditor().getEditorComponent());

        JTextField sizeEditor = null;
        if (mSizeField.getEditor().getEditorComponent() instanceof JTextField) {
            sizeEditor = (JTextField) mSizeField.getEditor().getEditorComponent();
            sizeEditor.setColumns(2); // not exactly character columns
            sizeEditor.setBackground(VueTheme.getTheme().getMenuBackground());
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
        
        mSizeField.addActionListener( this);
        f = mSizeField.getFont();
        Font sizeFont = new Font( f.getFontName(), f.getStyle(), f.getSize() - 2);
        mSizeField.setFont( sizeFont);
        mSizeField.setMaximumSize(mSizeField.getPreferredSize());
 		
        mBoldButton = new VueButton.Toggle("font.button.bold", this);
        mItalicButton = new VueButton.Toggle("font.button.italic", this);

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
         
        JLabel label = new JLabel("   Text: ");
        label.setFont(VueConstants.FONT_SMALL);
        add(label);
        add(mFontCombo);
        add(mSizeField);
        add(mBoldButton);
        add(mItalicButton);
        //add(mTextColorButton);
 	
        setFontValue(FONT_DEFAULT);
        this.initColors( VueResources.getColor("toolbar.background") );
    }

    public void addNotify()
    {
        super.addNotify();
        //System.out.println(this + " adding as parent of " + getParent());
    }

    // as this can sometimes take a while, we can call this manually
    // during startup to control when we take the delay.
    private static Object sFontNamesLock = new Object();
    static String[] getFontNames()
    {
        synchronized (sFontNamesLock) {
            if (sFontNames == null){
                if (DEBUG.Enabled)
                    new Throwable("FYI: loading system fonts...").printStackTrace();
                sFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            }
        }
        return sFontNames;
    }
 	
 	
    ////////////////
    // Methods
    /////////////////
 	
 	
 	
    public void setPropertyName( String pName) {
        mPropertyName = pName;
    }
 	
    public String getPropertyName() {
        return mPropertyName;
    }
 	
    /**
     * getFontValue()
     **/
    public Font getFontValue() {
        return mFont;
    }
 	
    /**
     * setFontValue()
     **/
    public void setFontValue( Font pFont) {
        setValue( pFont);
 	
    }
 	
 	
    /**
     * setValue
     * Generic property editor access
     **/
    public void setValue( Object pValue) {
        if (DEBUG.SELECTION) System.out.println(this + " setValue " + pValue);
        //new Throwable().printStackTrace();
 		
        if( pValue instanceof Font) {
            Font font = (Font) pValue;
 			
            String familyName = font.getFamily();
            mFontCombo.setSelectedItem( familyName );
            mItalicButton.setSelected( (Font.ITALIC & font.getStyle()) == Font.ITALIC );
            mBoldButton.setSelected( font.isBold() );
            //mSizeField.setValue( font.getSize() );
            mSizeField.setSelectedItem( ""+font.getSize() );
 			
        }
 	
    }
 	
    private void initColors( Color pColor) {
        //mFontCombo.setBackground( pColor);
        mBoldButton.setBackground( pColor);
        mItalicButton.setBackground( pColor);
    }
    
    public void fireFontChanged( Font pOld, Font pNew) {
        PropertyChangeListener [] listeners = getPropertyChangeListeners() ;
        PropertyChangeEvent  event = new PropertyChangeEvent( this, getPropertyName(), pOld, pNew);
        if (listeners != null) {
            for( int i=0; i<listeners.length; i++) {
                listeners[i].propertyChange( event);
            }
        }
    }
    
    public void actionPerformed( ActionEvent pEvent) {
        //System.out.println("FEP: actionPerformed " + pEvent);
        Font old = mFont;
        Font font = makeFont();
        if( (old == null) || ( !old.equals( font)) ) {
            fireFontChanged( old, font);
        }
    }
 	
    /**
     * makeFont
     *
     **/
    public Font makeFont() {
 	 
        String name = (String) mFontCombo.getSelectedItem() ;
 	 	
        int style = Font.PLAIN;
        if ( mItalicButton.isSelected() ) {
            style = style + Font.ITALIC;
        }
        if ( mBoldButton.isSelected() ) {
            style =  style + Font.BOLD;
        }
        //int size = (int) mSizeField.getValue();
        int size = 12;
        try {
            size = Integer.parseInt((String) mSizeField.getSelectedItem());
        } catch (Exception e) {
            System.err.println(e);
        }
 	 		
        Font font = new Font( name, style, size);
        return font;
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

    public static void main(String[] args) {
        System.out.println("FontEditorPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.initUI(true);
        
        sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts

        VueUtil.displayComponent(new FontEditorPanel());
    }
     
}
