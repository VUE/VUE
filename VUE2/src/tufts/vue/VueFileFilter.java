package tufts.vue;

public class VueFileFilter extends javax.swing.filechooser.FileFilter
{
    private final String[] extensions = {".vue", ".xml"};
    public boolean accept(java.io.File f)
    {
        String lname = f.getName().toLowerCase();
        for (int i = 0; i < extensions.length; i++)
            if (lname.endsWith(extensions[i]))
                return true;
        return false;
    }
    public String getDescription()
    {
        return "VUE Files";
    }
}

    
