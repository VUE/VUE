package tufts.vue;

public class VueFileFilter extends javax.swing.filechooser.FileFilter
{
    //private final String[] extensions = {".vue", ".xml"};
    //private String[] jpeg = {"jpeg"}, String[] svg = {"svg"}, String[] pdf = {"pdf"}, String[] html = {"html"};
    private String[]
        jpeg = {".jpeg", ".jpg"},
        svg = {".svg"},
        pdf = {".pdf"},
        html = {".html", ".htm"},
        imap = {".imap"}, 
        vue = {".vue", ".xml"},
        zip = {".zip"};
    private String[] extensions;
    private String description;
    
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
        
        else if (description.equals("imap"))
          extensions = imap;
        
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
        for (int i = 0; i < extensions.length; i++)
            if (lname.endsWith(extensions[i]))
                return true;
        return false;
    }
    
    public String getDescription()
    {
        return description;
    }
}

    
