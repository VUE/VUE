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
package tufts.vue.gui.formattingpalette;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.Utilities;
import javax.swing.text.html.HTML;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.PaletteButton;
import tufts.vue.PaletteButtonItem;
import tufts.vue.TextBox;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.VueTool;
import tufts.vue.VueUtil;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.VueButton.Toggle;

public class TextPropsPane extends JPanel implements CaretListener// ,
																	// KeyListener
{
	private final int topPad = 2;

	private final int botPad = 2;

	private final Insets labelInsets = new Insets(topPad, 25, botPad,
			GUI.LabelGapRight);

	private final Insets tightInsets = new Insets(topPad, 5, botPad, 5);

	private final Insets fieldInsets = new Insets(topPad, 0, botPad, 25);

	private final Object[] sizes = { "8", "9", "10", "11", "12", "14", "16",
			"18", "20", "22", "24", "26", "28", "36", "48", "72", "96" };

	private TextBox textBox;

	// private boolean bulletMode = false;
	private Toggle boldButton = new VueButton.Toggle("font.button.bold");

	private Toggle italicButton = new VueButton.Toggle("font.button.italic");

	private Toggle underlineButton = new VueButton.Toggle(
			"font.button.underline");

	private AlignmentDropDown alignmentButton = new AlignmentDropDown();

	private JComboBox sizeField = new JComboBox(sizes);

	private AbstractButton orderedListButton = new VueButton(
			"list.button.ordered");

	private AbstractButton unorderedListButton = new VueButton(
			"list.button.unordered");

	private JComboBox field = new JComboBox(getFontFaces());
	// private final String START_UNORDEREDLIST_STRING = "\u2022 ";
	// private final String INDENT_STRING = " ";
	// private final String BULLET="\u2022";
	// private int previousAlignment = 1;
	// private String previousRow = "";
	// private int indentNext;
	// private int listDepth =0;
	//private PaletteButton button = null;
	private VueColorButton colorButton = new VueColorButton(new Dimension(18, 18));		
	
	public TextPropsPane() {
		this.setFocusable(false);
		addComponents();
		addListeners();
		activateAll(false);
	}

	public void setActiveTextControl(tufts.vue.TextBox box) {
		// If this has previously been set remove the old listener..
		if (textBox != null)
			textBox.removeCaretListener(this);

		textBox = box;

		if (textBox !=null)
			textBox.addCaretListener(this);
		
		if (textBox == null)
			activateAll(false);
		else
			activateAll(true);
	}

	private void activateAll(boolean b)
	{
		boldButton.setEnabled(b);
		italicButton.setEnabled(b);
		underlineButton.setEnabled(b);
		alignmentButton.getComboBox().setEnabled(b);
		sizeField.setEnabled(b);
		orderedListButton.setEnabled(b);
		unorderedListButton.setEnabled(b);
		field.setEnabled(b);
		colorButton.setEnabled(b);
		
		return;
	}
	
	private Vector getFontFaces() {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		Font[] fonts = ge.getAllFonts();
		Vector list = new Vector();
		// Process each font
		for (int i = 0; i < fonts.length; i++) {
			// Get font's family and face
			String familyName = fonts[i].getFamily();
			String faceName = fonts[i].getName();
			list.add(faceName);
		}

		return list;
	}

	public void refreshOnUpdate()
	{
		disableCaretListener();
		textBox.setText(textBox.getText());
		enableCaretListener();
		textBox.repaint();
	}
	public void disableCaretListener()
	{
		textBox.removeCaretListener(this);
	}
	public void enableCaretListener()
	{
		textBox.addCaretListener(this);
	}
	private void addListeners() {
	/*	
		colorButton.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e)
			{
				AttributeSet paragraphSet = textBox.getParagraphAttributes();
				Color cur = ((Color)paragraphSet.getAttribute(StyleConstants.Foreground));
				if (cur == null)
					cur = Color.black;
				cur = Util.runColorChooser("Choose Font Color", cur, TextPropsPane.this);
				textBox.setForegroundAction(cur);
			}
		});
		boldButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textBox.boldAction.actionPerformed(e);
			}
		});

		underlineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textBox.underlineAction.actionPerformed(e);
			}
		});

		italicButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textBox.italicAction.actionPerformed(e);
			}
		});

		unorderedListButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VUE.getFormattingPanel().getTextPropsPane().disableCaretListener();				
				textBox.actionListUnordered.actionPerformed(e);
				VUE.getFormattingPanel().getTextPropsPane().enableCaretListener();
			}
		});

		orderedListButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VUE.getFormattingPanel().getTextPropsPane().disableCaretListener();
				textBox.actionListOrdered.actionPerformed(e);
				VUE.getFormattingPanel().getTextPropsPane().enableCaretListener();
			}
		});

		alignmentButton.getComboBox().addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {
				try
				{
				if (arg0.getStateChange() == ItemEvent.SELECTED) {
					int val = (new Integer(arg0.getItem().toString()))
							.intValue();
					
					///VUE.getFormattingPanel().getTextPropsPane().disableCaretListener();
					if (val == 0)
						textBox.leftAlignmentAction.actionPerformed(null);
					else if (val == 1)
						textBox.centerAlignmentAction.actionPerformed(null);
					else if (val == 2)
						textBox.rightAlignmentAction.actionPerformed(null);
					
					
				//	textBox.setText(textBox.getText());
				///	VUE.getFormattingPanel().getTextPropsPane().enableCaretListener();
				}
				}
				catch(java.lang.IllegalStateException ise)
				{};
			}
		});

		sizeField.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED)
					textBox.setFontSizeAction(new Integer((String)arg0.getItem()).intValue());
			}

		});
		
		field.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED)
					textBox.setFontFaceAction((String)arg0.getItem());
			}

		});
*/
	}
	
	private void addComponents() {
		this.setLayout(new GridBagLayout());

		// Labels
		JLabel label = new JLabel(VueResources
				.getString("formatting.text.font.label"));
		JLabel fontSizeLabel = new JLabel(VueResources
				.getString("formatting.text.size.label"));
		JLabel fontColorLabel = new JLabel(VueResources
				.getString("formatting.text.color.label"));		

		field.setSelectedItem(new String("Arial"));
		sizeField.setSelectedItem(new String("14"));
		GridBagConstraints c = new GridBagConstraints();
		colorButton.setColor(Color.black);

		// -------------------------------------------------------
		// Add the font face field label
		// -------------------------------------------------------

		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(10, 25, botPad, GUI.LabelGapRight);

		// c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
		c.fill = GridBagConstraints.NONE; // the label never grows
		c.anchor = GridBagConstraints.EAST;

		c.weightx = 0.0; // do not expand
		this.add(label, c);

		// -------------------------------------------------------
		// Add the font face field
		// -------------------------------------------------------

		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER; // last in row
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 0, botPad, 25);
		// c.gridwidth=3;
		this.add(field, c);

		// -------------------------------------------------------
		// Add the size label
		// -------------------------------------------------------

		c.gridx = 0;
		c.gridy = 1;
		c.insets = labelInsets;
		c.gridwidth = 1;
		// c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
		c.fill = GridBagConstraints.NONE; // the label never grows
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0; // do not expand
		this.add(fontSizeLabel, c);

		// -------------------------------------------------------
		// Add the font size field value
		// -------------------------------------------------------

		c.gridx = 1;
		c.gridy = 1;
		// c.gridwidth = GridBagConstraints.REMAINDER; // last in row
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(topPad, 0, botPad, 10);
		c.weightx = 1; // field value expands horizontally to use all space
		c.ipadx=10;
		this.add(sizeField, c);
		c.ipadx=0;
		// -------------------------------------------------------
		// Add the bold button
		// -------------------------------------------------------

		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 1.33;
		this.add(boldButton, c);

		// -------------------------------------------------------
		// Add the italic button
		// -------------------------------------------------------

		c.gridx = 3;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 1.33;
		this.add(italicButton, c);

		// -------------------------------------------------------
		// Add the underline button
		// -------------------------------------------------------

		c.gridx = 4;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 4;
		this.add(underlineButton, c);

		// -------------------------------------------------------
		// Add the font color label
		// -------------------------------------------------------

		c.gridx = 0;
		c.gridy = 2;
		c.insets = labelInsets;
		// c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
		c.fill = GridBagConstraints.NONE; // the label never grows
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0; // do not expand
		this.add(fontColorLabel, c);

		// -------------------------------------------------------
		// Add the font colorfield value
		// -------------------------------------------------------

		c.gridx = 1;
		c.gridy = 2;
		// c.gridwidth = GridBagConstraints.REMAINDER; // last in row
		c.anchor = GridBagConstraints.WEST;
		c.insets = fieldInsets;
		c.weightx = 1.0; // field value expands horizontally to use all space
		this.add(colorButton, c);

		// -------------------------------------------------------
		// Add the unordered list button
		// -------------------------------------------------------

		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 1.33;
		this.add(unorderedListButton, c);

		// -------------------------------------------------------
		// Add the ordered list button
		// -------------------------------------------------------

		c.gridx = 3;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 1.33;
		this.add(orderedListButton, c);

		// -------------------------------------------------------
		// alignment button
		// -------------------------------------------------------

		c.gridx = 4;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = tightInsets;
		c.weightx = 4;
		alignmentButton.setBorder(BorderFactory.createEmptyBorder());
		this.add(alignmentButton, c);

	}

	/*
	 * Return the current line number at the Caret position.
	 */
	/*
	 * private int getLineAtCaret(JTextComponent component) { int caretPosition =
	 * component.getCaretPosition(); Element root =
	 * component.getDocument().getDefaultRootElement(); return
	 * root.getElementIndex( caretPosition ) + 1; }
	 * 
	 * public String getRowOfText() { int offset = textBox.getCaretPosition();
	 * try { int start = Utilities.getRowStart(textBox, offset); return
	 * textBox.getText(start, offset-start); } catch (BadLocationException e) { //
	 * TODO Auto-generated catch block e.printStackTrace(); }
	 * 
	 * return ""; }
	 */
	/*
	 * private int getCaretColumnPosition(JTextComponent comp) { int offset =
	 * comp.getCaretPosition(); int column; try { column = offset -
	 * Utilities.getRowStart(comp, offset); } catch (BadLocationException e) {
	 * column = -1; } return column; }
	 */
	/*
	 * private boolean enterBulletMode() { //If you're already in bullet mode
	 * the user is misusing this, ignore. if (bulletMode) return false;
	 * bulletMode = true; listDepth++; previousAlignment =
	 * textBox.getAlignmentMode();
	 * textBox.setAlignmentMode(TextBox.LEFT_JUSTIFY);
	 * 
	 * return true; }
	 * 
	 * private void exitBulletMode() { bulletMode = false;
	 * textBox.setAlignmentMode(previousAlignment); }
	 * 
	 * private void insertBullet() {
	 * 
	 * Caret c = textBox.getCaret(); int pos = textBox.getCaretPosition(); try {
	 * Document d = textBox.getDocument(); //You're not at the beginngin of a
	 * new line...so you're gonna //have to take what's on the current line and
	 * put it in the list //fill space
	 * 
	 * 
	 * 
	 * if (getCaretColumnPosition(textBox) > 0) {
	 * textBox.getDocument().insertString(pos-getCaretColumnPosition(textBox),START_UNORDEREDLIST_STRING,
	 * null); } else {
	 * textBox.getDocument().insertString(pos,START_UNORDEREDLIST_STRING, null); }
	 * 
	 * 
	 * 
	 * for (int p=0; p< indentNext;p++)
	 * textBox.getDocument().insertString(Utilities.getRowStart(textBox, pos),"
	 * ",null); //
	 * textBox.setCaretPosition(pos+START_UNORDEREDLIST_STRING.length()); }
	 * catch (BadLocationException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); } }
	 * 
	 * 
	 * private boolean isThisAList() { return false; }
	 * 
	 * private void insertTab() {
	 * 
	 * Caret c = textBox.getCaret(); int pos = textBox.getCaretPosition(); try {
	 * Document d = textBox.getDocument(); int begin = pos -
	 * getCaretColumnPosition(textBox);
	 * textBox.getDocument().insertString(begin,INDENT_STRING, null); } catch
	 * (BadLocationException e1) { } }
	 * 
	 * private void matchPreviousIndent() { indentNext =
	 * previousRow.indexOf(BULLET);
	 *  }
	 */
	public void caretUpdate(CaretEvent arg0) {
	//	System.out.println(arg0.toString());
		if (textBox != null) {
			AttributeSet attsSet = textBox.getCharacterAttributes();
			AttributeSet paragraphSet = textBox.getParagraphAttributes();
			
			Boolean b = (Boolean) attsSet.getAttribute(StyleConstants.Bold);

			if (b != null) {
				boldButton.setSelected(b.booleanValue());
			} else
				boldButton.setSelected(false);

			b = (Boolean) attsSet.getAttribute(StyleConstants.Underline);
			if (b != null)
				underlineButton.setSelected(b.booleanValue());
			else
				underlineButton.setSelected(false);

			b = (Boolean) attsSet.getAttribute(StyleConstants.Italic);
			if (b != null)
				italicButton.setSelected(b.booleanValue());
			else
				italicButton.setSelected(false);

			
			Integer ival = ((Integer) attsSet.getAttribute(StyleConstants.Alignment));
			int align =0;
			if (ival != null)
				align = ival.intValue();

			alignmentButton.getComboBox().setSelectedIndex(align);
			
			Color cur = ((Color)attsSet.getAttribute(StyleConstants.Foreground));
			if (cur != null)
			{		
				colorButton.setColor(cur);
			}
			
			int size =0;
			
			Integer sizeInt = ((Integer)attsSet.getAttribute(StyleConstants.FontSize));
			
			if (sizeInt != null)
				size = sizeInt.intValue();
			
			if (size > 0)
			{
				sizeField.setSelectedItem(new Integer(size).toString());
			}
			
			String face =((String)attsSet.getAttribute(StyleConstants.FontFamily));
			
			if (face != null)
			{
				field.setSelectedItem(face);
			}		
		}
	}

	/*
	 * public void keyTyped(KeyEvent e) { if (bulletMode) {
	 * 
	 * 
	 * if ((e.getKeyChar() == KeyEvent.VK_TAB)) e.consume();
	 * 
	 * if ((e.getKeyChar() == KeyEvent.VK_ENTER)) { matchPreviousIndent();
	 * insertBullet(); } }
	 *  }
	 * 
	 * public void keyPressed(KeyEvent arg0) { if ((arg0.getKeyChar() ==
	 * KeyEvent.VK_TAB)) { if (bulletMode) { //if you hit a tab in bullet mode,
	 * and you haven't yet typed any characters you want to //ident the tab.
	 * otherwise you don't want to do anything special insertTab(); listDepth++;
	 * arg0.consume(); } }
	 * 
	 * try { if (bulletMode) { if (arg0.getKeyChar() == KeyEvent.VK_BACK_SPACE &&
	 * (textBox.getDocument().getText(textBox.getCaretPosition()-1,
	 * 1).equals(BULLET))) { System.out.println("removing a list depth level");
	 * listDepth--;
	 * 
	 * if (listDepth ==0) exitBulletMode(); else { int rowStart =
	 * Utilities.getRowStart(textBox, textBox.getCaretPosition());
	 * textBox.setCaretPosition(rowStart); //
	 * textBox.getDocument().remove(textBox.getCaretPosition(), 1);
	 * matchPreviousIndent(); //
	 * textBox.getDocument().insertString(textBox.getCaretPosition(),BULLET,
	 * null); previousRow = getRowOfText(); insertBullet();
	 * System.out.println("move row"); return; } }
	 *  } }catch (BadLocationException e1) {}
	 * 
	 * 
	 * if ((arg0.getKeyChar() == KeyEvent.VK_ENTER)) { previousRow =
	 * getRowOfText(); System.out.println("PREVIOUS ROW: " + previousRow); } }
	 * 
	 * public void keyReleased(KeyEvent e) { // TODO Auto-generated method stub
	 *  }
	 */
	private void out(String s) {
		System.out.println("TextPropsPane@" + Integer.toHexString(hashCode())
				+ " " + s);
	}

	private void out(String s, Dimension d) {
		out(VueUtil.pad(' ', 9, s, true) + " " + tufts.Util.out(d));
	}

	private void out(String s, Dimension d, String s2) {
		out(VueUtil.pad(' ', 9, s, true) + " " + tufts.Util.out(d) + " " + s2);
	}

	private void outc(String s) {
		System.out.println(this + " " + Integer.toHexString(hashCode()) + " "
				+ s);
	}

}
