/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import tufts.vue.gui.*;
import tufts.vue.gui.formattingpalette.AlignmentDropDown;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.StyleSheet;
import com.lightdev.app.shtm.SHTMLDocument;
import com.lightdev.app.shtm.SHTMLEditorKitActions;
import com.lightdev.app.shtm.SHTMLPanelImpl;
import com.lightdev.app.shtm.Util;



/**
 * This creates a font editor panel for editing fonts in the UI
 *
 * @version $Revision: 1.85 $ / $Date: 2008-12-19 16:42:34 $ / $Author: sraphe01 $
 *
 */
public class FontEditorPanel extends JPanel
    implements CaretListener, ComponentListener
               //,ActiveListener
               //implements LWEditor
//    implements ActionListener, VueConstants//, PropertyChangeListener//implements PropertyChangeListener
{
    private boolean DEBUG_FONTPANEL=false;
    private static String[] sFontSizes;
    
    /** the font list **/
    static String[] sFontNames = null;
 	
    public final static String SIZE_FIELD_NAME ="richFontSizeEditor";
    /** the Font selection combo box **/
    public final JComboBox mFontCombo;
    public final JComboBox mSizeField;
    private final AbstractButton mBoldButton;
    private final AbstractButton mItalicButton;
    private final AbstractButton mUnderlineButton;
    //private final AlignmentDropDown alignmentButton;
    private final AbstractButton mLeftAlignButton;
    private final AbstractButton mCenterAlignButton;
    private final AbstractButton mRightAlignButton;
    public static ColorMenuButton mTextColorButton;
    private final AbstractButton orderedListButton = new VueButton("list.button.ordered");
    private final AbstractButton unorderedListButton = new VueButton("list.button.unordered");
	
    /** the property name **/
    private final Object mPropertyKey; 
    private final ButtonGroup buttonGroup = new ButtonGroup();
    //plain text action listener
    final ActionListener styleChangeHandler;
    final ActionListener underlineChangeHandler;
    ActionListener alignmentHandler;      
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
   // private final AlignmentListener alignmentListener = new AlignmentListener();
    private LWPropertyHandler sizeHandler = null;
    
    private ActionListener globalSizeListener=null;
    private ActionListener globalFaceListener=null;
    private ActionListener globalBoldListener=null;
    private ActionListener globalItalicListener=null;
    private ActionListener globalUnderlineListener=null;	
    private static boolean suspendItemListeners = false;
	
    private LWText lwtext = null;
//    dynRes.addAction(fontFamilyAction, new SHTMLEditorKitActions.FontFamilyAction(this));
//    dynRes.addAction(fontSizeAction, new SHTMLEditorKitActions.FontSizeAction(this));
//    dynRes.addAction(fontColorAction, new SHTMLEditorKitActions.FontColorAction(this));

    public FontEditorPanel(Object propertyKey)
    {
    	//super(BoxLayout.X_AXIS);
    	setLayout(new GridBagLayout());
    	VUE.getFormatDock().addComponentListener(this);
    	//ActiveInstance.addAllActiveListener(this);
    	//VUE.addActiveListener(LWComponent.class, this);
    	VUE.addActiveListener(RichTextBox.class, this);
    	VUE.addActiveListener(LWComponent.class, this);
  //  	VUE.addActiveListener(LWComponent.class,this);
    	
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
        String[] sFontNames = getFontNames();
        mFontCombo = new JComboBox(sFontNames);
        
        
        mFontCombo.setRenderer(new CustomComboBoxRenderer());
        mFontCombo.setEditable(true);
        mFontCombo.getEditor().getEditorComponent().setFocusable(false);
        Font f = mFontCombo.getFont();
        Font menuFont = f.deriveFont((float) 9);        
        mFontCombo.setFont(menuFont);
        Component[] c =mFontCombo.getComponents();
        
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
        /* LWText ActionListeners Start */
       
        globalBoldListener = new ActionListener(){
            
        	public void actionPerformed(ActionEvent fe) 
        	{
        		if (lwtext == null)
        			return;

        		//System.out.println("BOLD : " + countf++);
        		lwtext.richLabelBox.selectAll();
        		
        		//Do Some basic set up.
        		SimpleAttributeSet set = new SimpleAttributeSet();
        		SHTMLDocument srcDoc = (SHTMLDocument)lwtext.richLabelBox.getDocument();
        		Element charElem = srcDoc.getCharacterElement(1);
        	    AttributeSet charSet = charElem.getAttributes();
        	    Enumeration characterAttributeEnum = charSet.getAttributeNames();
        	  
        	    boolean isBold = false;
        	    while (characterAttributeEnum.hasMoreElements())
        	    {
        	      	Object o = characterAttributeEnum.nextElement();
        	      	
        	    	if ((o.toString().equals("font-weight") && charSet.getAttribute(o).toString().equals("bold")) || o.toString().equals("b"))
            		{	
        	    		isBold = true;
            		}
        	    }
        	    String bold = !isBold ? "bold" : "normal";
        	    //System.out.println("bold? : "+ !isBold + " ,, " + bold);
        		Util.styleSheet().addCSSAttribute(set,CSS.Attribute.FONT_WEIGHT,bold);
        	//	set.addAttribute(HTML.Attribute.TEXT_WEIGHT, );
        		lwtext.richLabelBox.applyAttributes(set, false);
        		lwtext.richLabelBox.select(0,0);
        	
        		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
         		
         		   if (lwtext.getParent() !=null)
         	    		lwtext.getParent().layout();         		           		           		 
      	        
         		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint);            		   
         		
         		
        	}
         };  
         globalItalicListener = new ActionListener(){
             
         	public void actionPerformed(ActionEvent fe) 
         	{
         		
         		if (lwtext == null)
        			return;

        		//System.out.println("BOLD : " + countf++);
        		lwtext.richLabelBox.selectAll();
        		
        		//Do Some basic set up.
        		SimpleAttributeSet set = new SimpleAttributeSet();
        		SHTMLDocument srcDoc = (SHTMLDocument)lwtext.richLabelBox.getDocument();
        		Element charElem = srcDoc.getCharacterElement(1);
        	    AttributeSet charSet = charElem.getAttributes();
        	    Enumeration characterAttributeEnum = charSet.getAttributeNames();
        	  
        	    boolean isBold = false;
        	    while (characterAttributeEnum.hasMoreElements())
        	    {
        	      	Object o = characterAttributeEnum.nextElement();
        	      	
        	    	if ((o.toString().equals("font-style") && charSet.getAttribute(o).toString().equals("italic")) || o.toString().equals("i"))
            		{	
        	    		isBold = true;
            		}
        	    }
        	    String bold = !isBold ? "italic" : "normal";
        	    //System.out.println("bold? : "+ !isBold + " ,, " + bold);
        		Util.styleSheet().addCSSAttribute(set,CSS.Attribute.FONT_STYLE,bold);
        	//	set.addAttribute(HTML.Attribute.TEXT_WEIGHT, );
        		lwtext.richLabelBox.applyAttributes(set, false);
        		lwtext.richLabelBox.select(0,0);
        	
        		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
         		
         		   if (lwtext.getParent() !=null)
         	    		lwtext.getParent().layout();         		           		           		 
      	        
         		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint);       
          		
         	}
          };  
          globalUnderlineListener = new ActionListener(){
              
          	public void actionPerformed(ActionEvent fe) 
          	{
          		
          		if (lwtext == null)
        			return;

        		//System.out.println("BOLD : " + countf++);
        		lwtext.richLabelBox.selectAll();
        		
        		//Do Some basic set up.
        		SimpleAttributeSet set = new SimpleAttributeSet();
        		SHTMLDocument srcDoc = (SHTMLDocument)lwtext.richLabelBox.getDocument();
        		Element charElem = srcDoc.getCharacterElement(1);
        	    AttributeSet charSet = charElem.getAttributes();
        	    Enumeration characterAttributeEnum = charSet.getAttributeNames();
        	  
        	    boolean isBold = false;
        	    while (characterAttributeEnum.hasMoreElements())
        	    {
        	      	Object o = characterAttributeEnum.nextElement();
        	      	
        	    	if ((o.toString().equals("text-decoration") && charSet.getAttribute(o).toString().equals("underline")) || o.toString().equals("u"))
            		{	
        	    		isBold = true;
            		}
        	    }
        	    String bold = !isBold ? "underline" : "normal";
        	    //System.out.println("bold? : "+ !isBold + " ,, " + bold);
        		Util.styleSheet().addCSSAttribute(set,CSS.Attribute.TEXT_DECORATION,bold);
        	//	set.addAttribute(HTML.Attribute.TEXT_WEIGHT, );
        		lwtext.richLabelBox.applyAttributes(set, false);
        		lwtext.richLabelBox.select(0,0);
        	
        		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
         		
         		   if (lwtext.getParent() !=null)
         	    		lwtext.getParent().layout();         		           		           		 
      	        
         		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint);       
          	}
           };
        globalSizeListener = new ActionListener(){
        
        	public void actionPerformed(ActionEvent fe) 
        	{
        		
        		if (suspendItemListeners)
        			return;
        		
        		if (lwtext == null)
        			return;

        		lwtext.richLabelBox.selectAll();
        		final String textSize = mSizeField.getSelectedItem().toString();
        		SHTMLDocument doc = (SHTMLDocument)lwtext.richLabelBox.getDocument();
        		SimpleAttributeSet set = new SimpleAttributeSet();
                Util.styleSheet().addCSSAttribute(set, CSS.Attribute.FONT_SIZE,
            				 textSize);
                set.addAttribute(HTML.Attribute.SIZE, textSize);
                lwtext.richLabelBox.applyAttributes(set, false);
                
        		lwtext.richLabelBox.select(0,0);
        		//lwtext.setLabel0(lwtext.richLabelBox.getRichText(), false);
        		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
         		
         		   if (lwtext.getParent() !=null)
         	    		lwtext.getParent().layout();         		           		           		 
      	        
         		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint);            		   
         		
         		
        	}
         };
            		   
          globalFaceListener = new ActionListener(){ 
          	public void actionPerformed(ActionEvent fe) 
          	{          		
          		if (suspendItemListeners)
        			return;
        		
          		if (lwtext == null)
        			return;
          		lwtext.richLabelBox.selectAll();
        		SHTMLDocument doc = (SHTMLDocument)lwtext.richLabelBox.getDocument();
        		SimpleAttributeSet set = new SimpleAttributeSet();
                Util.styleSheet().addCSSAttribute(set, CSS.Attribute.FONT_FAMILY,
            				 mFontCombo.getSelectedItem().toString());
                set.addAttribute(HTML.Attribute.FACE, mFontCombo.getSelectedItem().toString());
                lwtext.richLabelBox.applyAttributes(set, false);
                
        		lwtext.richLabelBox.select(0,0);
       
        		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
         		
         		   if (lwtext.getParent() !=null)
         	    		lwtext.getParent().layout();
         		  
         		  
         		 
      	        
         		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint); 
          	}
           };
        /* End LWText ActionListeners */
        //mFontCombo.setBorder(new javax.swing.border.LineBorder(Color.green, 2));
        //mFontCombo.setBackground(Color.white); // handled by L&F tweaks in VUE.java
        //mFontCombo.setMaximumSize(new Dimension(50,50)); // no effect
        //mFontCombo.setSize(new Dimension(50,50)); // no effect
        //mFontCombo.setBorder(null); // already has no border

        //mSizeField = new NumericField( NumericField.POSITIVE_INTEGER, 2 );
        
        if (sFontSizes == null)
            sFontSizes = VueResources.getStringArray("fontSizes");
        
        
      
        
        mSizeField = new JComboBox(sFontSizes);
        //mSizeField.setModel(new FontComboBoxModel(sFontSizes));
        mSizeField.setRenderer(new CustomComboBoxRenderer());
  //      mSizeField.setPrototypeDisplayValue("10000");
        mSizeField.setEditable(true);
        mSizeField.setOpaque(false);
        mSizeField.setMaximumRowCount(30);
        mSizeField.setSelectedItem("13");
        mSizeField.setFocusable(true);
        mSizeField.getEditor().getEditorComponent().setName(SIZE_FIELD_NAME);
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
        sizeHandler  = new LWPropertyHandler<Integer>(LWKey.FontSize, mSizeField) {
                public Integer produceValue() { 
                	return new Integer(mSizeField.getSelectedItem().toString()); 
                	}
                public void displayValue(Integer value) { 
               	mSizeField.setSelectedItem(""+value); 
             
                }
            };

        mSizeField.addActionListener(sizeHandler);
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
                   
                }
        };
        
        underlineChangeHandler =
            new LWPropertyHandler<String>(LWKey.FontUnderline) {
                public String produceValue() {                
                	String style="";
                    if (mUnderlineButton.isSelected())
                    	style="underline";
                    else
                    	style="normal";
                    return style;
                }
                

                public void setEnabled(boolean enabled) {
              
                    mUnderlineButton.setEnabled(enabled);
                }
				public void displayValue(String propertyValue) {
					 final String style = propertyValue;
					 if (style.equals("underline"))
	                    mUnderlineButton.setSelected(true);
					 else
						 mUnderlineButton.setSelected(false);
					
				}
        };
 		
        mBoldButton = new VueButton.Toggle("font.button.bold",styleChangeHandler);
        
        mItalicButton = new VueButton.Toggle("font.button.italic", styleChangeHandler);
        mUnderlineButton = new VueButton.Toggle("font.button.underline",underlineChangeHandler); // regular components don't support this style
        //mUnderlineButton = new VueButton.Toggle("font.button.underline", styleChangeHandler);
        //alignmentButton = new AlignmentDropDown();
        mLeftAlignButton = new VueButton.Toggle("list.button.leftalignment");
        mCenterAlignButton = new VueButton.Toggle("list.button.centeralignment");
        mRightAlignButton = new VueButton.Toggle("list.button.rightalignment");
        buttonGroup.add(mLeftAlignButton);
    	buttonGroup.add(mCenterAlignButton);
    	buttonGroup.add(mRightAlignButton);
        // We can set these once -- they're not property based:
        mUnderlineButton.addActionListener(richUnderlineAction);						
        orderedListButton.addActionListener(toggleNumbersAction);
        unorderedListButton.addActionListener(toggleBulletsAction);
        
        alignmentHandler  = new LWPropertyHandler<LWComponent.Alignment>(LWKey.Alignment) 
        {
            public LWComponent.Alignment produceValue() {
                if (mLeftAlignButton.isSelected())
                	return LWComponent.Alignment.LEFT;
                else if (mCenterAlignButton.isSelected())
                	return LWComponent.Alignment.CENTER;
                else if (mRightAlignButton.isSelected())
                	return LWComponent.Alignment.RIGHT;
                else
                	return LWComponent.Alignment.LEFT;
            }
            public void displayValue(LWComponent.Alignment align) {
            	//clearAlignmentSelection();
            	 //mLeftAlignButton.setSelected(align.ordinal() == LWComponent.Alignment.LEFT.ordinal() ? true : false);
            	 //mCenterAlignButton.setSelected(align.ordinal() == LWComponent.Alignment.CENTER.ordinal() ? true : false);
            	 //mRightAlignButton.setSelected(align.ordinal() == LWComponent.Alignment.RIGHT.ordinal() ? true : false);

            }
    
            public void setEnabled(boolean enabled) {
            	//clearAlignmentSelection();
                mLeftAlignButton.setEnabled(false);
                mCenterAlignButton.setEnabled(false);
                mRightAlignButton.setEnabled(false);
            }
        };

        mLeftAlignButton.addActionListener(alignmentHandler);
        mCenterAlignButton.addActionListener(alignmentHandler);
        mRightAlignButton.addActionListener(alignmentHandler);
      
        mLeftAlignButton.setEnabled(false);
    	mCenterAlignButton.setEnabled(false);
    	mRightAlignButton.setEnabled(false);
    	
         Color[] textColors = VueResources.getColorArray("textColorValues");
        //String[] textColorNames = VueResources.getStringArray("textColorNames");
        mTextColorButton = new ColorMenuButton(textColors, true);
        mTextColorButton.setColor(Color.black);
        mTextColorButton.setPropertyKey(LWKey.TextColor);
        mTextColorButton.setToolTipText("Text Color");
        
        mTextColorButton.addPropertyChangeListener(RichTextColorChangeListener);


        
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
        gbc.fill=GridBagConstraints.HORIZONTAL;
        gbc.anchor=GridBagConstraints.CENTER;
        gbc.gridwidth=3;
        gbc.weightx=0.5;
        //   gbc.gridheight=1;
        gbc.ipady=5;
        //gbc.ipadx=5;        
        gbc.insets=new Insets(1,5,1,1);
        add(mSizeField,gbc);
        
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridy=0;
        gbc.gridx=9;
        gbc.fill=GridBagConstraints.REMAINDER;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridheight=1;
        gbc.gridwidth=1;
        gbc.ipadx=0;
        gbc.ipady=0;
        gbc.insets=new Insets(0,1,1,1);
        //add(mTextColorButton,gbc);
        
        gbc.fill=GridBagConstraints.NONE;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.gridwidth=1;
        gbc.gridheight=0;
        gbc.gridy=1;
        gbc.gridx=0;        
        add(styleLabel,gbc);
        
        gbc.gridy=1;
        gbc.gridx=1;
        mTextColorButton.setPreferredSize(new Dimension(20,20));
        boolean isWindows = VueUtil.isWindowsPlatform();    		    		
		if(!isWindows){
			gbc.insets=new Insets(0,5,0,0);
		}
        add(mTextColorButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=2;
        add(mBoldButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=3;
        add(mItalicButton,gbc);
     
        gbc.gridy=1;
        gbc.gridx=4;        
        gbc.fill=GridBagConstraints.NONE;
        mUnderlineButton.setEnabled(false);
        add(mUnderlineButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=5;        
        gbc.fill=GridBagConstraints.NONE;
        orderedListButton.setEnabled(false);
        add(orderedListButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=6;        
        gbc.fill=GridBagConstraints.NONE;
        unorderedListButton.setEnabled(false);
        add(unorderedListButton,gbc);
                                        
        gbc.gridy=1;
        gbc.gridx=6;
        gbc.insets=new Insets(1,5,1,1);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.NONE;
        add(mLeftAlignButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=8;
        gbc.insets=new Insets(1,1,1,1);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.NONE;
        add(mCenterAlignButton,gbc);
        
        gbc.gridy=1;
        gbc.gridx=9;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.fill=GridBagConstraints.NONE;
        add(mRightAlignButton,gbc);
        
        
 	
        //displayValue(VueConstants.FONT_DEFAULT);

        //initColors(VueTheme.getToolbarColor());
        VUE.getFormatDock().setFocusable(true);
   		VUE.getFormatDock().setFocusableWindowState(true);
    }
    static int countf = 0;
   
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

    private void enableSupportedEditors() {
        setSpecialEditorsEnabled(true);
        
        mBoldButton.setEnabled(true);
        mItalicButton.setEnabled(true);
        mFontCombo.setEnabled(true);
        mSizeField.setEnabled(true);
        mTextColorButton.setEnabled(true);
        mLeftAlignButton.setEnabled(true);
        mCenterAlignButton.setEnabled(true);
        mRightAlignButton.setEnabled(true);
    }

    private void disableSpecialEditors() {

        // we only need to disable the editors not associated with
        // a property (those special to LWText) -- the rest of
        // the editors will set their enabled state based on what's
        // next selected.
        
        setSpecialEditorsEnabled(false);
    }

    public String colorToString(Color c) {
		char[] buf = new char[7];
		buf[0] = '#';
		String s = Integer.toHexString(c.getRed());
		if (s.length() == 1) {
			buf[1] = '0';
			buf[2] = s.charAt(0);
		}
		else {
			buf[1] = s.charAt(0);
			buf[2] = s.charAt(1);
		}
		s = Integer.toHexString(c.getGreen());
		if (s.length() == 1) {
			buf[3] = '0';
			buf[4] = s.charAt(0);
		}
		else {
			buf[3] = s.charAt(0);
			buf[4] = s.charAt(1);
		}
		s = Integer.toHexString(c.getBlue());
		if (s.length() == 1) {
			buf[5] = '0';
			buf[6] = s.charAt(0);
		}
		else {
			buf[5] = s.charAt(0);
			buf[6] = s.charAt(1);
		}
		return String.valueOf(buf);
	}
    
    private void setSpecialEditorsEnabled(boolean enabled) {
        mUnderlineButton.setEnabled(enabled); 
        orderedListButton.setEnabled(enabled);
        unorderedListButton.setEnabled(enabled);    	
    	Color c = null;
    	if (VUE.getActiveViewer().getFocal() instanceof LWSlide)
    		c = ((LWSlide)VUE.getActivePathway().getMasterSlide()).getMasterSlide().getTextStyle().getTextColor();
    	else    		
    		c = mTextColorButton.getColor();
    		toggleBulletsAction.setColor(colorToString(c));
    		toggleNumbersAction.setColor(colorToString(c)); 
    }


//     private final ActionListener TextColorListener =
//         new ActionListener() {
//             public void actionPerformed(ActionEvent e)
//             {
//                 Color color =FontEditorPanel.getTextColorButton().getColor();
//                 SimpleAttributeSet set = new SimpleAttributeSet();
//                 String colorString = "#" + Integer.toHexString(color.getRGB()).substring(2);
//                 Util.styleSheet().addCSSAttribute(set,
//                                                   CSS.Attribute.COLOR, colorString);
                
//                 set.addAttribute(HTML.Attribute.COLOR, colorString);
                
//                 //text.getRichLabelBox().applyAttributes(set, false);
//                 ((RichTextBox)VUE.getActive(RichTextBox.class)).applyAttributes(set, false);
//             }
//         };

    /**
     * We need to use a property change listener instead of an action listener for the
     * ColorMenuButton, as the action fires there to detect popping the menu, not
     * a color set.  When a color is picked, it will fire off property change events
     * to any listeners.  We set ourselves up to listen constantly here, and
     * if there is an active RichTextBox editing, then we apply it, otherwise we
     * just ignore it.
     */
    private final PropertyChangeListener RichTextColorChangeListener =
        new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {

                if (e instanceof LWPropertyChangeEvent == false)
                    return;
                
                final RichTextBox activeRTB = (RichTextBox) VUE.getActive(RichTextBox.class);
                
                
                if (activeRTB == null && lwtext == null) {
                    // no problem: just ignore if there's no active edit
                    //tufts.Util.printStackTrace("FEP propertyChange: no active RichTextBox: " + e);
                    return;
                }
                
                if (lwtext !=null)
                	lwtext.richLabelBox.selectAll();
                
               
                final Color color = (Color) e.getNewValue();
                final SimpleAttributeSet set = new SimpleAttributeSet();
                final String colorString = "#" + Integer.toHexString(color.getRGB()).substring(2);
                toggleBulletsAction.setColor(colorString);
                toggleNumbersAction.setColor(colorString);
                Util.styleSheet().addCSSAttribute(set, CSS.Attribute.COLOR, colorString);
                
                set.addAttribute(HTML.Attribute.COLOR, colorString);
                if (activeRTB != null)
                	activeRTB.applyAttributes(set, false);
                else
                	lwtext.richLabelBox.applyAttributes(set, false);
                
                if (lwtext !=null)
                {
                	lwtext.richLabelBox.select(0,0);
                    
                	lwtext.richLabelBox.select(0,0);
            		//lwtext.setLabel0(lwtext.richLabelBox.getRichText(), false);
            		lwtext.richLabelBox.setSize(lwtext.richLabelBox.getPreferredSize());        		        	         		
             		
             		   if (lwtext.getParent() !=null)
             	    		lwtext.getParent().layout();
             		  
             		  
             		 
          	        
             		lwtext.notify(lwtext.richLabelBox, LWKey.Repaint); 
                }
            }
        };
        
    private void setEditorPanes(RichTextBox activeText)
    {
       richBoldAction.setEditorPane(activeText);
       richItalicAction.setEditorPane(activeText);
       richUnderlineAction.setEditorPane(activeText);
       toggleBulletsAction.setEditorPane(activeText);
       toggleNumbersAction.setEditorPane(activeText);
       paraAlignLeftAction.setEditorPane(activeText);
       paraAlignCenterAction.setEditorPane(activeText);
       paraAlignRightAction.setEditorPane(activeText);
       fontFamilyAction.setEditorPane(activeText);
       fontSizeAction.setEditorPane(activeText);
       fontColorAction.setEditorPane(activeText);
    }
    private boolean LWTextListenersAdded = false;
    private boolean RTBListenersAdded =false;
    private boolean defaultListenersAdded=false;
    
    private void establishLWTextListeners(LWComponent activeText)
    {
    	if (DEBUG_FONTPANEL)
    		System.out.println("ESTABLISH LWT Listeners");
    	suspendItemListeners=true;
    	breakdownDefaultListeners();
    	//clearAlignmentSelection();
    	VUE.getFormatDock().setFocusable(false);
   		VUE.getFormatDock().setFocusableWindowState(false);
   		
   		mLeftAlignButton.setEnabled(false);
    	mCenterAlignButton.setEnabled(false);
    	mRightAlignButton.setEnabled(false);
    	
    	//System.out.println("lw text");
        mFontCombo.setEnabled(true);
        mSizeField.setEnabled(true);
        mTextColorButton.setEnabled(true);             
        
        lwtext = (LWText)activeText;
        updateFormatControlsTB(lwtext.richLabelBox);
        
        lwtext.richLabelBox.removeCaretListener(this);
        
        //mFontCombo.removeActionListener(fontPropertyHandler);             
        //mSizeField.removeActionListener(sizeHandler);
        
        //EditorManager.unregisterEditor(sizeHandler);
        //EditorManager.unregisterEditor(fontPropertyHandler);

        mBoldButton.addActionListener(globalBoldListener);
        mUnderlineButton.addActionListener(globalUnderlineListener);
        mItalicButton.addActionListener(globalItalicListener);
        
        mSizeField.addActionListener(globalSizeListener);
        mFontCombo.addActionListener(globalFaceListener);
        LWTextListenersAdded = true;
        suspendItemListeners=false;
    }
    
    private void breakdownLWTextListeners()
    {
    	if (DEBUG_FONTPANEL)
    		System.out.println("BREAKDOWN LWT Listeners");
    	lwtext=null;
    	suspendItemListeners=true;
		//System.out.println("lost lw text");
    //	clearAlignmentSelection();
    	mLeftAlignButton.setEnabled(false);
      	mCenterAlignButton.setEnabled(false);
      	mRightAlignButton.setEnabled(false);
		
    
      	mSizeField.removeActionListener(globalSizeListener);
        mFontCombo.removeActionListener(globalFaceListener);
        stripAllListeners(mBoldButton);
        stripAllListeners(mItalicButton);
        stripAllListeners(mUnderlineButton);
    
          //    mSizeField.addActionListener(fontPropertyHandler);
        
    //    if (!EditorManager.isRegistered(sizeHandler))
    //    	EditorManager.registerEditor(sizeHandler);
     //   if (!EditorManager.isRegistered(fontPropertyHandler))
      //  	EditorManager.registerEditor(fontPropertyHandler);
        LWTextListenersAdded = false;
        establishDefaultListeners();
        suspendItemListeners=false;

    }
    
    private void breakdownRichTextListeners()
    {
    
    	if (DEBUG_FONTPANEL)
    		System.out.println("BREAKDOWN RTB Listeners");
    	suspendItemListeners=true;
    	//System.out.println("lost rtb rich text box");
        disableSpecialEditors();
        clearAlignmentSelection();
        mLeftAlignButton.setEnabled(false);
    	mCenterAlignButton.setEnabled(false);
    	mRightAlignButton.setEnabled(false);
    	
        VUE.getFormatDock().setFocusable(true);
		VUE.getFormatDock().setFocusableWindowState(true);
		
		stripAllListeners(mBoldButton);
	    stripAllListeners(mItalicButton);
	    stripAllListeners(mUnderlineButton);
	        
        
        mFontCombo.removeActionListener(fontFamilyAction);
      //  mFontCombo.addActionListener(fontPropertyHandler);
        
        mSizeField.removeActionListener(fontSizeAction);
      //  mSizeField.addActionListener(fontPropertyHandler);

        mLeftAlignButton.removeActionListener(paraAlignLeftAction);
        mCenterAlignButton.removeActionListener(paraAlignCenterAction);
        mRightAlignButton.removeActionListener(paraAlignRightAction);
        RTBListenersAdded = false;
        //establishDefaultListeners();
        suspendItemListeners=false;
    }
    
    private void stripAllListeners(AbstractButton b)
    {
    	  ActionListener[] l = b.getListeners(ActionListener.class);
          for (int i=0;i<l.length;i++)
          	b.removeActionListener(l[i]);
          
          return;
    }
     
    private void establishRichTextListeners(RichTextBox activeText)
    {
    	if (DEBUG_FONTPANEL)
    		System.out.println("ESTABLISH RTB Listeners");
    	suspendItemListeners=true;
    	breakdownDefaultListeners();
    	
    	//System.out.println("rtb rich text box");
        activeText.addCaretListener(this);
        setEditorPanes(activeText);
     //   clearAlignmentSelection();
        mLeftAlignButton.setEnabled(true);
    	mCenterAlignButton.setEnabled(true);
    	mRightAlignButton.setEnabled(true);
    	
        activeText.setToggleBulletList(toggleBulletsAction);
        activeText.setNumberList(toggleNumbersAction);
        
        mBoldButton.addActionListener(richBoldAction);			
        mItalicButton.addActionListener(richItalicAction);			
		mUnderlineButton.addActionListener(richUnderlineAction);
		
        mFontCombo.addActionListener(fontFamilyAction);
        mSizeField.addActionListener(fontSizeAction);
        
        mLeftAlignButton.addActionListener(paraAlignLeftAction);
        mCenterAlignButton.addActionListener(paraAlignCenterAction);
        mRightAlignButton.addActionListener(paraAlignRightAction);
        
        enableSupportedEditors();
        RTBListenersAdded = true;
        suspendItemListeners=false;
    }
    
    public void establishDefaultListeners()
    {              
    	
    	if (DEBUG_FONTPANEL)
    		System.out.println("ESTABLISH Default Listeners");
    	suspendItemListeners=true;
      VUE.getFormatDock().setFocusable(true);
  	  VUE.getFormatDock().setFocusableWindowState(true);
  	clearAlignmentSelection();
  	  mLeftAlignButton.setEnabled(false);
  	  mCenterAlignButton.setEnabled(false);
  	  mRightAlignButton.setEnabled(false);
  	  
      if (!EditorManager.isRegistered(sizeHandler))
         EditorManager.registerEditor(sizeHandler);
      if (!EditorManager.isRegistered(fontPropertyHandler))
    	 EditorManager.registerEditor(fontPropertyHandler);
    	        
      mBoldButton.addActionListener(styleChangeHandler);
      mItalicButton.addActionListener(styleChangeHandler);  
      mUnderlineButton.addActionListener(underlineChangeHandler);  
      mFontCombo.addActionListener(fontPropertyHandler);    	          	
      mSizeField.addActionListener(fontPropertyHandler);
      mLeftAlignButton.addActionListener(alignmentHandler);
      mCenterAlignButton.addActionListener(alignmentHandler);
      mRightAlignButton.addActionListener(alignmentHandler);
      defaultListenersAdded=true;
      suspendItemListeners=false;
    }

    public void breakdownDefaultListeners()
    {
    	if (DEBUG_FONTPANEL)
    		System.out.println("BREAKDOWN Default Listeners");
    	suspendItemListeners=true;
     EditorManager.unregisterEditor(sizeHandler);
     EditorManager.unregisterEditor(fontPropertyHandler);
     //clearAlignmentSelection(); 
     mLeftAlignButton.setEnabled(false);
 	 mCenterAlignButton.setEnabled(false);
 	 mRightAlignButton.setEnabled(false);

     mLeftAlignButton.removeActionListener(alignmentHandler);
     mCenterAlignButton.removeActionListener(alignmentHandler);
     mRightAlignButton.removeActionListener(alignmentHandler);
     
     stripAllListeners(mBoldButton);
     stripAllListeners(mItalicButton);
     stripAllListeners(mUnderlineButton);
     
     mFontCombo.removeActionListener(fontPropertyHandler);
     mSizeField.removeActionListener(fontPropertyHandler);
     defaultListenersAdded=false;
     suspendItemListeners=false;
    }
    public void clearAlignmentSelection()
    {
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run()
    		{
    			buttonGroup.remove(mLeftAlignButton);
    			buttonGroup.remove(mCenterAlignButton);
    			buttonGroup.remove(mRightAlignButton);
    			mLeftAlignButton.setSelected(false);
    			mCenterAlignButton.setSelected(false);
    			mRightAlignButton.setSelected(false);
    			buttonGroup.add(mLeftAlignButton);
    			buttonGroup.add(mCenterAlignButton);
    			buttonGroup.add(mRightAlignButton);
    		}
    	});
    }
    private static boolean isRichTextActive = false;
    public void activeChanged(final ActiveEvent e, LWComponent activeText)
    {
    	
    	//System.out.println("lwtext new active : " + e.active + " Old active : " + e.oldActive);
    	suspendItemListeners=true;
    	if (RTBListenersAdded)
    		breakdownRichTextListeners();
    	
    	if (e.active != null && e.oldActive != null && e.active instanceof LWText && e.oldActive instanceof LWText)
    	{
    		breakdownLWTextListeners();
    		establishLWTextListeners(activeText);
    		((LWText)e.active).richLabelBox.setCaretPosition(2);
    		updateFormatControlsTB(((LWText)e.active).richLabelBox);
    	//	System.out.println("OUT");
    		suspendItemListeners=false;
    		return;
    	}
    	if (e.active instanceof LWText)
    	{    
    		
    		if (RTBListenersAdded)
    			breakdownRichTextListeners();
    			
    		if (!LWTextListenersAdded)
    		{    		
    			establishLWTextListeners(activeText);
    	 	}
    		((LWText)e.active).richLabelBox.setCaretPosition(2);
    		updateFormatControlsTB(((LWText)e.active).richLabelBox);
    		
    	}
    	else if (e.oldActive instanceof LWText)
    	{              
    		if (LWTextListenersAdded)
    			breakdownLWTextListeners();            
    	}
    	else
    	{
    		if (!defaultListenersAdded)
    			establishDefaultListeners();
    	}
    	suspendItemListeners=false;
    }
    
    public void activeChanged(ActiveEvent e, RichTextBox activeText)
    {    	
        if (e.active == e.oldActive)
            return;
        
        if (e.active == null)
        	return;
        suspendItemListeners=true;
      // System.out.println("new active : " + e.active + " Old active : " + e.oldActive);
        if (e.active != null) 
        {        
        	if (LWTextListenersAdded)
        		breakdownLWTextListeners();
        	if (!RTBListenersAdded)
        		establishRichTextListeners(activeText);
			
        } 
        else if (e.oldActive instanceof RichTextBox)
        {       
        	if (RTBListenersAdded)
        		breakdownRichTextListeners();
        }
        suspendItemListeners=false;		
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
		if (e != null)
			updateFormatControlsTB((RichTextBox)e.getSource());
		else
			updateFormatControlsTB(null);
	}
	
	public final void updateFormatControlsTB(RichTextBox text)
	{
		SHTMLDocument doc = (SHTMLDocument)text.getDocument();
	    
		int so = text.getSelectionStart();
		int eo = text.getSelectionEnd();		
	    
	    int caretPosition = 0;
		if (so != eo && Math.abs(eo-so) > 1)
	    {
			caretPosition=so;			
	    }
		else
		{
			caretPosition = text.getCaretPosition();
		}
	    //Assembling appropriate attribute sets
		//System.out.println("Caret position : " + text.getCaretPosition());	
		Element paragraphElement = doc.getParagraphElement(caretPosition);
		
		if (paragraphElement.getName().equals("p-implied")) //we're in a list item
			paragraphElement = paragraphElement.getParentElement();
	    
		AttributeSet paragraphAttributeSet = paragraphElement.getAttributes();
		Element charElem = null;
		if (so != eo && Math.abs(eo-so) > 1)
			charElem = doc.getCharacterElement(caretPosition);
		else
			charElem = doc.getCharacterElement(caretPosition > 0 ? caretPosition-1 : 1);
	    AttributeSet charSet = charElem.getAttributes();
	    Enumeration characterAttributeEnum = charSet.getAttributeNames();
	    Enumeration elementEnum = paragraphAttributeSet.getAttributeNames();
	    
	    //	  Done Assembling appropriate attribute sets
	    
	 
	
		//************************************ SET DEFAULTS
	  
		
	    //Start with all these turned off.
	    mBoldButton.setSelected(false);
		mItalicButton.setSelected(false);
		mUnderlineButton.setSelected(false);
		
	    
		//Set Font size / face defaults.
		if (VUE.getActiveViewer().getFocal() instanceof LWSlide)
		{
			Font f = ((LWSlide)VUE.getActivePathway().getMasterSlide()).getMasterSlide().getTextStyle().getFont();
			mSizeField.getEditor().setItem(Integer.toString(f.getSize()));
			mFontCombo.getEditor().setItem(f.getFontName());			
		}
		else
		{
			Style r = doc.getStyleSheet().getRule("ul");
			
			mSizeField.getEditor().setItem(r.getAttribute(CSS.Attribute.FONT_SIZE));
			String s = r.getAttribute(CSS.Attribute.FONT_FAMILY).toString();
			int p = s.indexOf(",");
			s = s.substring(0,p > 0 ? p : s.length());
			if (s.length() > 1)
				mFontCombo.getEditor().setItem(s);
		}
		//end font size /face defaults.
		
		//set the default color
		if (VUE.getActiveViewer().getFocal() instanceof LWSlide)
		{
			Color c = ((LWSlide)VUE.getActivePathway().getMasterSlide()).getMasterSlide().getTextStyle().getTextColor();
			//System.out.println("LW SLIDE : " + c.toString());
			mTextColorButton.setColor(c);
		}
		else
			mTextColorButton.setColor(Color.black);
		//done setting the default color.
		//**********************************************DONE SETTING DEFAULTS
		
		    
		//If you're looking at a selection blank out the size and face combos unselect
		//every other relevant field and return there's not much we can do here.
		//if (so != eo && Math.abs(eo-so) > 1)
	//    {
		//	System.out.println("so : " + so + " eo : " + eo);
	    	//blank out the appropriate fields, set stuff unselected and return
	    	//mSizeField.getEditor().setItem("");
			//mFontCombo.getEditor().setItem("");
//			while (elementEnum.hasMoreElements())
	//	    {
		    	
//		    	Object o = elementEnum.nextElement();
	//	       	if (o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("left"))
		//       		mLeftAlignButton.setSelected(true);
		  //     	else if (o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("center"))
		    //   		mCenterAlignButton.setSelected(true);
		      // 	else if	(o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("right"))
		       	//	mRightAlignButton.setSelected(true);		        			       		 	        
//		    }
	//    }
	    	    	    	     	    	   		
	    while (elementEnum.hasMoreElements())
	    {
	    	
	    	Object o = elementEnum.nextElement();
	    	//System.out.println("pargraph element : " + o.toString() + " , " + paragraphAttributeSet.getAttribute(o));
	    	//if (o.toString().equals("text-align"))
	    		//System.out.println(" P: " + o.toString() + "  ***  " + .getAttribute(o).toString());
	    	boolean isAlignSet = false;
	       	if (o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("left") && !isAlignSet)
	       	{//	System.out.println("SET LEFT");
	       		//isAlignSet = true;
	       		mLeftAlignButton.setSelected(true);
	       	}
	       	else if (o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("center")&& !isAlignSet)
	       	{
	       		//System.out.println("SET CENTER");
	       		//isAlignSet = true;
	       		mCenterAlignButton.setSelected(true);
	       		
	       	}
	       	else if	(o.toString().equals("text-align") && paragraphAttributeSet.getAttribute(o).toString().equals("right")&& !isAlignSet)
	       	{
	       	//	isAlignSet = true;
	       		mRightAlignButton.setSelected(true);
	       	}
	       	
	       	if ((o.toString().equals("font-size")) ||(o.toString().equals("size")))
	       	{
	       		mSizeField.getEditor().setItem(paragraphAttributeSet.getAttribute(o).toString());
	       		suspendItemListeners = true;
	       		mSizeField.getModel().setSelectedItem(paragraphAttributeSet.getAttribute(o).toString());
	       		suspendItemListeners = false;
	       	}
	        			
	       	else if ((o.toString().equals("font-family")) || (o.toString().equals("font-face")) || (o.toString().equals("face")))
	       	{

	       		mFontCombo.getEditor().setItem(paragraphAttributeSet.getAttribute(o).toString());
	       		suspendItemListeners = true;
	       		mFontCombo.getModel().setSelectedItem(paragraphAttributeSet.getAttribute(o).toString());
	       		suspendItemListeners = false;
	       	}	       		       	 	        
	    }
	    
	
		
	    while (characterAttributeEnum.hasMoreElements())
	    {
	    
	      	Object o = characterAttributeEnum.nextElement();
	      	//System.out.println("Character element : " + o.toString() + " , " + charSet.getAttribute(o));
        	if ((o.toString().equals("color")))        
        		mTextColorButton.setColor(edu.tufts.vue.style.Style.hexToColor(charSet.getAttribute(o).toString()));        	
        	
        	if ((o.toString().equals("font-size")) ||(o.toString().equals("size")))
        	{
        		mSizeField.getEditor().setItem(charSet.getAttribute(o).toString());     
        		suspendItemListeners = true;
        		mSizeField.getModel().setSelectedItem(charSet.getAttribute(o).toString());
        		suspendItemListeners = false;
        	}        		
	        			
        	if ((o.toString().equals("font-family")) || (o.toString().equals("font-face")) || (o.toString().equals("face")))
        	{
        		mFontCombo.getEditor().setItem(charSet.getAttribute(o).toString());
        		suspendItemListeners = true;
        		mFontCombo.getModel().setSelectedItem(charSet.getAttribute(o).toString());
        		suspendItemListeners = false;
        	}
        	
        	if ((o.toString().equals("font-weight") && charSet.getAttribute(o).toString().equals("bold")) || o.toString().equals("b"))
        		mBoldButton.setSelected(true);

	        		
	        if ((o.toString().equals("font-style") && charSet.getAttribute(o).toString().equals("italic")) || o.toString().equals("i"))	       
	        	mItalicButton.setSelected(true);
	        	
	        if ((o.toString().equals("text-decoration") && charSet.getAttribute(o).toString().equals("underline")) || o.toString().equals("u"))	        		        
        		mUnderlineButton.setSelected(true);	        		        			        		            	        	
        }//done looking at character attributes	        	   	        	        	      
	}
	
	  public void caretUpdate(final CaretEvent e) {
		  
		      EventQueue.invokeLater(new Runnable(){

		            public void run() {
		                updateFormatControls(e);
		            }            
		        });		  
		  }

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentMoved(ComponentEvent arg0) {
		if (mTextColorButton.getPopupWindow().isVisible())
			mTextColorButton.getPopupWindow().setVisible(false);
		
	}

	public void componentResized(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}