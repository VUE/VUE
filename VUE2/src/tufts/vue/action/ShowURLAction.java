package tufts.vue.action;

/**
 * Display a given URL in an external browser.
 */
public class ShowURLAction extends javax.swing.AbstractAction
{
    private String url;
    
    public ShowURLAction(String url) {
        this(url, url);
    }
    public ShowURLAction(String name, String url) {
        super(name);
        this.url = url;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent ae)
    {
        try {
            tufts.vue.VueUtil.openURL(url);
        } catch (Exception ex) {
            System.out.println("ShowURLAction " + this + " failed to display [" + url + "] on " + ae);
        }
    }
}