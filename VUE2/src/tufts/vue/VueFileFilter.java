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


package tufts.vue;

public class VueFileFilter extends javax.swing.filechooser.FileFilter
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueFileFilter.class);

    //private final String[] extensions = {".vue", ".xml"};
    //private String[] jpeg = {"jpeg"}, String[] svg = {"svg"}, String[] pdf = {"pdf"}, String[] html = {"html"};
	
	public static final String JPEG_DESCRIPTION=VueResources.getString("vueFileFilter.jpeg.text");
	public static final String SVG_DESCRIPTION=VueResources.getString("vueFileFilter.svg.text");
	public static final String IMAGEMAP_DESCRIPTION=VueResources.getString("vueFileFilter.imagemap.text");
	public static final String IMS_DESCRIPTION=VueResources.getString("vueFileFilter.ims.text");
	public static final String VUE_DESCRIPTION=VueResources.getString("vueFileFilter.vue.text");
	public static final String XML_DESCRIPTION=VueResources.getString("vueFileFilter.vue.text");
	public static final String ZIP_DESCRIPTION=VueResources.getString("vueFileFilter.zip.text");
	public static final String PNG_DESCRIPTION=VueResources.getString("vueFileFilter.png.text");
	public static final String VPK_DESCRIPTION=VueResources.getString("vueFileFilter.vpk.text");

    public static final String VuePackage = VueUtil.VueArchiveExtension.substring(1);
    

	private static final String[]
            jpeg = {"jpeg", "jpg"},
            svg = {"svg"},
            pdf = {"pdf"},
            html = {"html","htm"},
            imap = {"imap"},
            png = {"png"},
            vue = {"vue", "xml", VuePackage },
            rdf = {"rdf","owl","rdfs"},
            txt = {"txt"},
            zip = {"zip"},
            rli = {"rli.xml"},
            VuePackageExt = { VuePackage };
    
    private final String[] extensions;
    private final String description;
    
//     public VueFileFilter() {
//         super();
//     }

    private void debug(String msg) {
        Log.debug(msg + ": desc=\"" + description + "\"; extensions=" + java.util.Arrays.asList(extensions));
    }

    
    public VueFileFilter(String desc, String ... ext) {
        description = desc;
        extensions = ext;
        if (DEBUG.Enabled) debug("explict extensions");
    }
    
    public VueFileFilter(String description)
    {
        super();
        this.description = description;
        
        if (description.equalsIgnoreCase(JPEG_DESCRIPTION))
          extensions = jpeg;
        
        else if (description.equalsIgnoreCase(VPK_DESCRIPTION))
            extensions = VuePackageExt;
        
        else if (description.equalsIgnoreCase(SVG_DESCRIPTION))
          extensions = svg;
        
        //else if (description.equals("pdf"))
         // extensions = pdf;
        
        //else if (description.equals("html"))
        //  extensions = html;
        
        else if (description.equalsIgnoreCase("rdf")) 
            extensions = rdf;
        else if (description.equalsIgnoreCase("pdf"))
            extensions = pdf;
        else if (description.equalsIgnoreCase(ZIP_DESCRIPTION))
            extensions=zip;
        else if (description.equalsIgnoreCase(IMAGEMAP_DESCRIPTION))
        {
          //extensions = imap;
            extensions = html;
        }
        else if (description.equalsIgnoreCase(PNG_DESCRIPTION))
    			extensions = png;
        else if (description.equalsIgnoreCase(IMS_DESCRIPTION))
			extensions = rli;
        
        else if (description.equalsIgnoreCase(VUE_DESCRIPTION) || description.equalsIgnoreCase(XML_DESCRIPTION)) {
            extensions = vue;
            description = "VUE Files";
        } else if (description.equalsIgnoreCase("zip")) {
            this.extensions = zip;
        } else if( description.equalsIgnoreCase("txt")) {
            this.extensions = txt;
        } else
            extensions = null;

        if (DEBUG.Enabled) debug("extensions by description");
        
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

    
