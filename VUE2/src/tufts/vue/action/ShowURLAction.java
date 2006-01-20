package tufts.vue.action;

/**
 * Display a given URL in an external browser.
 */
public class ShowURLAction extends tufts.vue.VueAction
{
    private String url;
    
    public ShowURLAction(String url) {
        this(url, url);
    }
    public ShowURLAction(String name, String url) {
        super(name);
        this.url = url;
    }
    
    public void act()
    {
        try {
            tufts.vue.VueUtil.openURL(url);
        } catch (Exception ex) {
            //System.out.println("ShowURLAction " + this + " failed to display [" + url + "] on " + ae);
            throw new RuntimeException("ShowURLAction " + this + " failed to display [" + url + "]", ex);
            
        }
    }
}