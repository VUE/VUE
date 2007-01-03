/*
 * CSSTest.java
 *
 * Created on December 13, 2006, 11:49 AM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue;
import org.w3c.css.sac.*;
import org.w3c.css.sac.helpers.ParserFactory;
import java.net.*;
import java.io.*;

import junit.framework.TestCase;

public class CSSTest extends TestCase implements DocumentHandler {
    boolean inMedia         = false;
    boolean inStyleRule     = false;
    int     propertyCounter = 0;
    
    public void startMedia(SACMediaList media) throws CSSException {
        inMedia = true;
    }
    
    public void endMedia(SACMediaList media) throws CSSException {
        inMedia = false;
    }
    
    public void startSelector(SelectorList patterns) throws CSSException {
        if (!inMedia) {
            inStyleRule = true;
            propertyCounter = 0;
        }
    }
    
    public void endSelector(SelectorList patterns) throws CSSException {
        if (!inMedia) {
  //          System.out.println( "Found " + propertyCounter + " properties.");
        }
        inStyleRule = false;
        
    }
    
    public void property(String name, LexicalUnit value, boolean important)
    throws CSSException {
        if (inStyleRule) {
            propertyCounter++;
  //          System.out.println("Name: "+ name);
        }
    }
    public void endFontFace()
    throws CSSException {
        
    }
    
    public void startFontFace()
    throws CSSException {
        
    }
    public void endPage(java.lang.String name,
            java.lang.String pseudo_page)
            throws CSSException {
        
    }
    
    public void startPage(java.lang.String name,
            java.lang.String pseudo_page)
            throws CSSException {
        
    }
    
    public void importStyle(java.lang.String uri,
            SACMediaList media,
            java.lang.String defaultNamespaceURI)
            throws CSSException {
        
    }
    
    public void namespaceDeclaration(java.lang.String prefix,
            java.lang.String uri)
            throws CSSException {
        
    }
    public void ignorableAtRule(java.lang.String atRule)
    throws CSSException {
        
    }
    
    public void endDocument(InputSource source)
    throws CSSException {
        
    }
    
    public void startDocument(InputSource source)
    throws CSSException {
        
    }
    
    
    public void comment(java.lang.String text)
    throws CSSException {
        
    }
    
    
    /** Creates a new instance of CSSTest */
    public CSSTest() {
    }
    
    public void testManager() {
//        System.setProperty("org.w3c.css.sac.parser","org.apache.batik.css.parser.Parser");
        try {
        InputSource source = new InputSource();
        URL uri = TestResources.getURL("test.css");
        InputStream stream = uri.openStream();
        
        source.setByteStream(stream);
        source.setURI(uri.toString());
        Parser parser = new org.apache.batik.css.parser.Parser();
        
        parser.setDocumentHandler(new CSSTest());
        parser.parseStyleSheet(source);
        stream.close();
        } catch(Exception ex) {}
    }
}