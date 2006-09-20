 /*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


package tufts.vue;

public class VueFileFilter extends javax.swing.filechooser.FileFilter
{
    //private final String[] extensions = {".vue", ".xml"};
    //private String[] jpeg = {"jpeg"}, String[] svg = {"svg"}, String[] pdf = {"pdf"}, String[] html = {"html"};
    private String[]
        jpeg = {"jpeg", "jpg"},
        svg = {"svg"},
        pdf = {"pdf"},
        html = {"html","htm"},
        imap = {"imap"}, 
        vue = {"vue", "xml"},
        rdf = {"rdf"},
        zip = {"zip"},
		rli = {"rli.xml"};
    private String[] extensions;
    private String description;
    
    public VueFileFilter() {
        super();
    }
    
    public VueFileFilter(String description)
    {
        super();
        this.description = description;
        
        if (description.equals("jpeg"))
          extensions = jpeg;
        
        else if (description.equals("svg"))
          extensions = svg;
        
        else if (description.equals("pdf"))
          extensions = pdf;
        
        else if (description.equals("html"))
          extensions = html;
        
        else if (description.equals("Image Map"))
          extensions = imap;
        
        else if (description.equals("IMS Resource List"))
			extensions = rli;
        
        else if (description.equals("vue") || description.equals("xml")) {
            extensions = vue;
            description = "VUE Files";
        } else if (description.equals("zip")) {
            this.extensions = zip;
        }
    }
    
    public boolean accept(java.io.File f)
    {
        if (f.isDirectory())
            return true;
        String lname = f.getName().toLowerCase();
        if(extensions != null)
        for (int i = 0; i < extensions.length; i++)
            if (lname.endsWith(extensions[i]))
                return true;
        return false;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public String[] getExtensions() {
        return extensions;
    }
}

    
