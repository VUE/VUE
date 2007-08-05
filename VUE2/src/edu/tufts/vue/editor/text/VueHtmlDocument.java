package edu.tufts.vue.editor.text;

import java.io.IOException;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.UndoableEdit;

public class VueHtmlDocument extends HTMLDocument{

	public VueHtmlDocument(StyleSheet ss) {
		super(ss);
	}

	public int getLastDocumentPosition(){
	    final int length = getLength();	    // >=0
	    return length > 1 ? length - 1 : length;
	}
	
	  public void replaceHTML(Element firstElement, String htmlText) throws
	  BadLocationException, IOException {
	          if (firstElement != null && firstElement.getParentElement() != null &&
	                  htmlText != null) {
	              int start = firstElement.getStartOffset();
	              Element parent = firstElement.getParentElement();
	              int removeIndex = parent.getElementIndex(start);
	                  removeElements(parent, removeIndex, 1);
	                  setOuterHTML(parent.getElement(removeIndex), htmlText);
	          }	      
	 }

//remainder methods were private in HTMLDocument so i took them directly from the src
	  
	  /**
	     * Removes child Elements of the passed in Element <code>e</code>. This
	     * will do the necessary cleanup to ensure the element representing the
	     * end character is correctly created.
	     * <p>This is not a general purpose method, it assumes that <code>e</code>
	     * will still have at least one child after the remove, and it assumes
	     * the character at <code>e.getStartOffset() - 1</code> is a newline and
	     * is of length 1.
	     */
	    private void removeElements(Element e, int index, int count) throws BadLocationException {
		writeLock();
		try {
		    int start = e.getElement(index).getStartOffset();
		    int end = e.getElement(index + count - 1).getEndOffset();
		    if (end > getLength()) {
			removeElementsAtEnd(e, index, count, start, end);
		    }
		    else {
			removeElements(e, index, count, start, end);
		    }
		} finally {
		    writeUnlock();
		}
	    }
	    
	    /**
	     * Called to remove child elements of <code>e</code> when one of the
	     * elements to remove is representing the end character.
	     * <p>Since the Content will not allow a removal to the end character
	     * this will do a remove from <code>start - 1</code> to <code>end</code>.
	     * The end Element(s) will be removed, and the element representing
	     * <code>start - 1</code> to <code>start</code> will be recreated. This
	     * Element has to be recreated as after the content removal its offsets
	     * become <code>start - 1</code> to <code>start - 1</code>.
	     */
	    private void removeElementsAtEnd(Element e, int index, int count,
				 int start, int end) throws BadLocationException {
		// index must be > 0 otherwise no insert would have happened.
		boolean isLeaf = (e.getElement(index - 1).isLeaf());
	        DefaultDocumentEvent dde = new DefaultDocumentEvent(
	                       start - 1, end - start + 1, DocumentEvent.
	                       EventType.REMOVE);

		if (isLeaf) {
	            Element endE = getCharacterElement(getLength());
	            // e.getElement(index - 1) should represent the newline.
	            index--;
	            if (endE.getParentElement() != e) {
	                // The hiearchies don't match, we'll have to manually
	                // recreate the leaf at e.getElement(index - 1)
	                replace(dde, e, index, ++count, start, end, true, true);
	            }
	            else {
	                // The hierarchies for the end Element and
	                // e.getElement(index - 1), match, we can safely remove
	                // the Elements and the end content will be aligned
	                // appropriately.
	                replace(dde, e, index, count, start, end, true, false);
	            }
	        }
	        else {
		    // Not a leaf, descend until we find the leaf representing
		    // start - 1 and remove it.
		    Element newLineE = e.getElement(index - 1);
		    while (!newLineE.isLeaf()) {
			newLineE = newLineE.getElement(newLineE.getElementCount() - 1);
		    }
	            newLineE = newLineE.getParentElement();
	            replace(dde, e, index, count, start, end, false, false);
	            replace(dde, newLineE, newLineE.getElementCount() - 1, 1, start,
	                    end, true, true);
	        }
		postRemoveUpdate(dde);
		dde.end();
		fireRemoveUpdate(dde);
	        fireUndoableEditUpdate(new UndoableEditEvent(this, dde));
	    }

	    /**
	     * This is used by <code>removeElementsAtEnd</code>, it removes
	     * <code>count</code> elements starting at <code>start</code> from
	     * <code>e</code>.  If <code>remove</code> is true text of length
	     * <code>start - 1</code> to <code>end - 1</code> is removed.  If
	     * <code>create</code> is true a new leaf is created of length 1.
	     */
	    private void replace(DefaultDocumentEvent dde, Element e, int index,
	                         int count, int start, int end, boolean remove,
	                         boolean create) throws BadLocationException {
	        Element[] added;
	        AttributeSet attrs = e.getElement(index).getAttributes();
	        Element[] removed = new Element[count];

	        for (int counter = 0; counter < count; counter++) {
	            removed[counter] = e.getElement(counter + index);
	        }
	        if (remove) {
	            UndoableEdit u = getContent().remove(start - 1, end - start);
	            if (u != null) {
	                dde.addEdit(u);
	            }
	        }
	        if (create) {
	            added = new Element[1];
	            added[0] = createLeafElement(e, attrs, start - 1, start);
	        }
	        else {
	            added = new Element[0];
	        }
	        dde.addEdit(new ElementEdit(e, index, removed, added));
	        ((AbstractDocument.BranchElement)e).replace(
	                                             index, removed.length, added);
	    }

	    /**
	     * Called to remove child Elements when the end is not touched.
	     */
	    private void removeElements(Element e, int index, int count,
				     int start, int end) throws BadLocationException {
		Element[] removed = new Element[count];
		Element[] added = new Element[0];
		for (int counter = 0; counter < count; counter++) {
		    removed[counter] = e.getElement(counter + index);
		}
		DefaultDocumentEvent dde = new DefaultDocumentEvent
			(start, end - start, DocumentEvent.EventType.REMOVE);
		((AbstractDocument.BranchElement)e).replace(index, removed.length,
							    added);
		dde.addEdit(new ElementEdit(e, index, removed, added));
		UndoableEdit u = getContent().remove(start, end - start);
		if (u != null) {
		    dde.addEdit(u);
		}
		postRemoveUpdate(dde);
		dde.end();
		fireRemoveUpdate(dde);
		if (u != null) {
		    fireUndoableEditUpdate(new UndoableEditEvent(this, dde));
		}
	    }



}
