package tufts.vue.gui.formattingpalette.ekit;

/*
GNU Lesser General Public License

PropertiesDialog
Copyright (C) 2003 Frits Jalvingh, Jerry Pommer & Howard Kistler

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


import java.util.Enumeration;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.UndoableEdit;

public class ExtendedHTMLDocument extends HTMLDocument
{

  public ExtendedHTMLDocument(AbstractDocument.Content c, StyleSheet styles)
  {
    super(c, styles);
  }

  public ExtendedHTMLDocument(StyleSheet styles)
  {
    super(styles);
  }

  public ExtendedHTMLDocument() { ; }

/** Überschreibt die Attribute des Elements.
  *
  * @param e Element bei dem die Attribute geändert werden sollen
  * @param a AttributeSet mit den neuen Attributen
  * @param tag Angabe was für ein Tag das Element ist
  */
	public void replaceAttributes(Element e, AttributeSet a, HTML.Tag tag)
	{
		if((e != null) && (a != null))
		{
			try
			{
				writeLock();
				int start = e.getStartOffset();
				DefaultDocumentEvent changes = new DefaultDocumentEvent(start, e.getEndOffset() - start, DocumentEvent.EventType.CHANGE);
				AttributeSet sCopy = a.copyAttributes();
				changes.addEdit(new AttributeUndoableEdit(e, sCopy, false));
				MutableAttributeSet attr = (MutableAttributeSet) e.getAttributes();
				Enumeration aNames = attr.getAttributeNames();
				Object value;
				Object aName;
				while (aNames.hasMoreElements())
				{
					aName = aNames.nextElement();
					value = attr.getAttribute(aName);
					if(value != null && !value.toString().equalsIgnoreCase(tag.toString()))
					{
						attr.removeAttribute(aName);
					}
				}
				attr.addAttributes(a);
				changes.end();
				fireChangedUpdate(changes);
				fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
			}
			finally
			{
				writeUnlock();
			}
		}
	}

	public void removeElements(Element e, int index, int count)
	throws BadLocationException
	{
		writeLock();
		int start = e.getElement(index).getStartOffset();
		int end = e.getElement(index + count - 1).getEndOffset();
		try
		{
			Element[] removed = new Element[count];
			Element[] added = new Element[0];
			for (int counter = 0; counter < count; counter++)
			{
				removed[counter] = e.getElement(counter + index);
			}
			DefaultDocumentEvent dde = new DefaultDocumentEvent(start, end - start, DocumentEvent.EventType.REMOVE);
			((AbstractDocument.BranchElement)e).replace(index, removed.length, added);
			dde.addEdit(new ElementEdit(e, index, removed, added));
			UndoableEdit u = getContent().remove(start, end - start);
			if(u != null)
			{
				dde.addEdit(u);
			}
			postRemoveUpdate(dde);
			dde.end();
			fireRemoveUpdate(dde);
			if(u != null)
			{
				fireUndoableEditUpdate(new UndoableEditEvent(this, dde));
			}
		}
		finally
		{
			writeUnlock();
		}
	}
}