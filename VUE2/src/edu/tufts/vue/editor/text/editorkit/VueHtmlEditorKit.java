package edu.tufts.vue.editor.text.editorkit;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class VueHtmlEditorKit extends HTMLEditorKit
{
	//needed to override default document for saner list handling if you do nothing else
	public Document createDefaultDocument()
	{
		StyleSheet ss = new StyleSheet();
		try {
		  ss.importStyleSheet(getClass().forName("javax.swing.text.html.HTMLEditorKit").getResource(DEFAULT_CSS));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		HTMLDocument doc = new HTMLDocument(ss);
		doc.setParser(getParser());
		try
        {
            read(new StringReader("<html><head></head><body><p>New Text</p></body>"), doc, 0);
        }
        catch(IOException ioexception)
        {
            throw new RuntimeException("IOError initializing document.");
        }
        catch(BadLocationException badlocationexception)
        {
            throw new RuntimeException("BadLocationException initializing document.");
        }	
        return doc;
	}
}
