package edu.tufts.vue.editor.text;

import javax.swing.text.*;
import javax.swing.text.html.*;
import java.io.*;
import java.util.*;


public class VUEHTMLWriter extends HTMLWriter {
    private Element elem;        
    
    public VUEHTMLWriter(Writer w, HTMLDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    public VUEHTMLWriter(Writer w, HTMLDocument doc) {
        this(w, doc,0 , doc.getLength());
     }

    /*
     * Fetches the element iterator..
     */   
    protected ElementIterator getElementIterator() {
        if(elem == null)
            return super.getElementIterator();
        return new ElementIterator(elem);
    }

    /**
     * Iterates over the
     * Element tree and controls the writing out of
     * all the tags and its attributes.
     *
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *            location within the document.
     *
     */
    public synchronized void write(Element elem) throws IOException, BadLocationException {
        this.elem = elem;
        try{
            write();
        }
        catch(BadLocationException e){
            elem = null;
            throw e;
        }
        catch(IOException e){
            elem = null;
            throw e;
        }
    }
    /**
     * invoke HTML creation for all children of a given element.
     *
     * @param elem  the element which children are to be written as HTML
     */
    public void writeChildElements(Element elem)
        throws IOException, BadLocationException
    {
      Element para;
      for(int i = 0; i < elem.getElementCount(); i++) {
        para = elem.getElement(i);
        write(para);
      }
    }

    public void startTag(Element elem) throws IOException, BadLocationException {       
        super.startTag(elem);
    }

    //changed visibility on the underlyin htmldocument method
    public void endTag(Element elem) throws IOException {
        super.endTag(elem);
    }

    public void endTag(String elementName) throws IOException{
        write('<');
        write('/');
        write(elementName);
        write('>');
        writeLineSeparator();
    }

    public void startTag(String elementName) throws IOException
    {
    	startTag(elementName,null);
    }
    
    /*HTMLWriter.java
     * indent();
	write('<');
	write(elem.getName());
	writeAttributes(attr);
	write('>');*/
    public void startTag(String elementName, AttributeSet attributes) throws IOException{        
        write('<');
        write(elementName);
        if(attributes != null)
            writeAttributes(attributes);        
        write('>');
        writeLineSeparator();
    }    
}