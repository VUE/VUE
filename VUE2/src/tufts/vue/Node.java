package tufts.vue;

/**
 * Node.java
 *
 * @author Scott Fraize
 * @version 6/7/03
 */
public interface Node extends MapItem
{
    public java.util.Iterator getChildIterator();
    //public java.util.List getChildList();
    
    public void setIcon(javax.swing.ImageIcon icon);
    public javax.swing.ImageIcon getIcon();

}
