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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

import tufts.vue.VueConstants;
import tufts.vue.VueResources;

public class JMultiLineToolTip extends JToolTip
{
	private static final String uiClassID = "ToolTipUI";	
	private String tipText;
	private JComponent component;
	private final Color borderColor = new Color(105,105,105);	
	
	public JMultiLineToolTip() {
	    updateUI();	    
	    this.setBorder(BorderFactory.createLineBorder(borderColor,1));
	}
	
	public void updateUI() {
	    setUI(MultiLineToolTipUI.createUI(this));
	}
	
	public void setColumns(int columns)
	{
		this.columns = columns;
		this.fixedwidth = 0;
	}

	public int getColumns()
	{
		return columns;
	}
	
	public void setFixedWidth(int width)
	{
		this.fixedwidth = width;
		this.columns = 0;
	}
	
	public int getFixedWidth()
	{
		return fixedwidth;
	}
	
	protected int columns = 0;
	protected int fixedwidth = 0;
}



class MultiLineToolTipUI extends BasicToolTipUI {
		
	private final Color backgroundColor = new Color(213,223,255);
	private JScrollPane scrollPane;
	static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();	
	static JToolTip tip;
	protected CellRendererPane rendererPane;	
	private static JEditorPane textArea ;
	
	
		
	
	
	public static ComponentUI createUI(JComponent c) {
	    return sharedInstance;
	}
	
	public MultiLineToolTipUI() {
	    super();
	    
	}
	
	public void installUI(JComponent c) {
	    super.installUI(c);
		tip = (JToolTip)c;		
	    rendererPane = new CellRendererPane();
	    
	    c.add(rendererPane);
	}
	
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
		
	    c.remove(rendererPane);
	    rendererPane = null;
	}
	
	public void paint(Graphics g, JComponent c) {
	    Dimension size = c.getSize();
	    textArea.setBackground(backgroundColor);	    
		rendererPane.paintComponent(g, textArea, c, 1, 1,
					    size.width - 1, size.height - 1, true);		
	}
	
	public Dimension getPreferredSize(JComponent c) {
		String tipText = ((JToolTip)c).getTipText();
		if (tipText == null)
			return new Dimension(0,0);
		
		textArea = new JEditorPane("text/html",tipText );
		Font font = new Font("SansSerif", Font.PLAIN, 8);
		textArea.setFont(font);
		scrollPane = new JScrollPane(textArea);
		
		//textArea.setFont(VueConstants.FONT_SMALL);
		textArea.setMargin(new Insets(4,4,4,4));
	    
		rendererPane.removeAll();
		rendererPane.add(scrollPane);
		
		//textArea.setWrapStyleWord(true);
		int width = ((JMultiLineToolTip)c).getFixedWidth();
		int columns = ((JMultiLineToolTip)c).getColumns();
		
	//	if( columns > 0 )
//		{			
		
		//	textArea.setColumns(columns);
		//	textArea.setSize(0,0);
		//textArea.setLineWrap(true);
//			textArea.setSize( textArea.getPreferredSize() );
		//}
	//	else if( width > 0 )
//		{
		//textArea.setLineWrap(true);
	//		Dimension d = textArea.getPreferredSize();
//			d.width = width;
			//d.height++;
		//	textArea.setSize(d);
	//	}
//		else
			//textArea.setLineWrap(false);


		Dimension dim = textArea.getPreferredSize();
		
		dim.height += 1;
		dim.width += 1;
		return dim;
	}
	
	public Dimension getMinimumSize(JComponent c) {
	    return getPreferredSize(c);
	}
	
	public Dimension getMaximumSize(JComponent c) {
	    return getPreferredSize(c);
	}
}

