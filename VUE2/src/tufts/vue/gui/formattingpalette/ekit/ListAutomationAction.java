package tufts.vue.gui.formattingpalette.ekit;

/*
GNU Lesser General Public License

ListAutomationAction
Copyright (C) 2000 Howard Kistler

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.awt.event.ActionEvent;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import tufts.vue.TextBox;
import tufts.vue.VUE;


/** Class for automatically creating bulleted lists from selected text
  */
public class ListAutomationAction extends HTMLEditorKit.InsertHTMLTextAction
{
	private HTML.Tag baseTag;
	private String sListType;
	private HTMLUtilities htmlUtilities;
	private TextBox parentEkit;
	
	public ListAutomationAction(TextBox ekit, String sLabel, HTML.Tag listType)
	{
		super(sLabel, "", listType, HTML.Tag.LI);
		parentEkit = ekit;
		baseTag    = listType;
		htmlUtilities = new HTMLUtilities(ekit);
	}

	public void actionPerformed(ActionEvent ae)
	{
		
		try
		{
//			JEditorPane jepEditor = (JEditorPane)(parentEkit.getTextPane());
			String selTextBase = parentEkit.getSelectedText();
			int textLength = -1;
			if(selTextBase != null)
			{
				textLength = selTextBase.length();				
			}
			if(selTextBase == null || textLength < 1)
			{
				
				int pos = parentEkit.getCaretPosition();
				//System.out.println("POS : " + pos);
				//System.out.println("END : " + parentEkit.getCaret().getDot());
				//System.out.println("CHAR AT: " + parentEkit.getText());
				parentEkit.setCaretPosition(pos);
				//System.out.println("ACTION COMMAND : " + ae.getActionCommand());
				//parentEk
				/*if(!ae.getActionCommand().equals("newListPoint"))
				{					
					if(htmlUtilities.checkParentsTag(HTML.Tag.OL) || htmlUtilities.checkParentsTag(HTML.Tag.UL))
					{						
						//System.err.println("oops");
				//		new SimpleInfoDialog(parentEkit.getFrame(), Translatrix.getTranslationString("Error"), true, Translatrix.getTranslationString("ErrorNestedListsNotSupported"));
					//	return;
					}
				}*/
				String sListType = (baseTag == HTML.Tag.OL ? "ol" : "ul");
				//System.out.println("baseTag " + baseTag);
				StringBuffer sbNew = new StringBuffer();
				if(htmlUtilities.checkParentsTag(baseTag))
				{					
					sbNew.append("<li></li>");					
					insertHTML(parentEkit, (HTMLDocument) parentEkit.getDocument(), parentEkit.getCaretPosition(), sbNew.toString(), 0, 0, HTML.Tag.LI);					
				}
				else
				{
					sbNew.append("<" + sListType + "><li></li></" + sListType + "> <p style=\"margin: 0;\">&nbsp;</p>");
					insertHTML(parentEkit, (HTMLDocument) parentEkit.getDocument(), parentEkit.getCaretPosition(), sbNew.toString(), 0, 0, (sListType.equals("ol") ? HTML.Tag.OL : HTML.Tag.UL));					
				}
				VUE.getFormattingPanel().getTextPropsPane().disableCaretListener();
				parentEkit.setText(parentEkit.getText());
				VUE.getFormattingPanel().getTextPropsPane().enableCaretListener();
				
			}
			else
			{				
				String sListType = (baseTag == HTML.Tag.OL ? "ol" : "ul");				
				HTMLDocument htmlDoc = (HTMLDocument)(parentEkit.getDocument());
				int iStart = parentEkit.getSelectionStart();
				int iEnd   = parentEkit.getSelectionEnd();
				String selText = htmlDoc.getText(iStart, iEnd - iStart);
				StringBuffer sbNew = new StringBuffer();
				String sToken = ((selText.indexOf("\r") > -1) ? "\r" : "\n");
				StringTokenizer stTokenizer = new StringTokenizer(selText, sToken);
				sbNew.append("<" + sListType + ">");
				while(stTokenizer.hasMoreTokens())
				{			
					sbNew.append("<li>");
					sbNew.append(stTokenizer.nextToken());
					sbNew.append("</li>");
				}				
				sbNew.append("</" + sListType + "><p>&nbsp;</p>");
				htmlDoc.remove(iStart, iEnd - iStart);
				insertHTML(parentEkit, htmlDoc, iStart, sbNew.toString(), 1, 1, null);
			}
		}
		catch (BadLocationException ble) {ble.printStackTrace();}
	}
}