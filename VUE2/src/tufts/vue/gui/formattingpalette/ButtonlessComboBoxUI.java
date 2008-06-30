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
package tufts.vue.gui.formattingpalette;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class ButtonlessComboBoxUI extends BasicComboBoxUI{
	
	public ButtonlessComboBoxUI()
	{
		super();
		
	}		
	
	protected JButton createArrowButton()
	{
		JButton b = new JButton();
		b.setMinimumSize(new Dimension(0,0));
		b.setMaximumSize(new Dimension(0,0));
		b.setOpaque(false);
		b.setBorder(BorderFactory.createEmptyBorder());
		b.setPreferredSize(new Dimension(0,0));
		b.setVisible(false);
		b.setContentAreaFilled(false);
		b.setSize(new Dimension(0,0));
		return b ;
	}
		
	protected void installComponents()
	{
		if ( comboBox.isEditable() )		
			addEditor();
	
		comboBox.add( currentValuePane );
		Component[] c = comboBox.getComponents();		
	
	}
	
	  protected Rectangle rectangleForCurrentValue()
	  {
	    int w = comboBox.getWidth();
	    int h = comboBox.getHeight();
	    Insets i = comboBox.getInsets();
	
	    return new Rectangle(i.left, i.top, w -10,
	                         h - (i.top + i.left));
	  }
}
