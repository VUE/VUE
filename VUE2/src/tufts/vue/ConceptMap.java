package tufts.vue;

/**
 * ConceptMap.java
 *
 * Interface for generically accessing basic map data.
 *
 * @author Scott Fraize
 * @version 6/7/03
 */

public interface ConceptMap extends MapItem
{
    public java.util.Iterator getNodeIterator();
    public java.util.Iterator getLinkIterator();
    public java.util.Iterator getPathwayIterator();
    
    /*
    public java.util.List getNodeList();
    public java.util.List getLinkList();
    public java.util.List getPathwayList();
    */
}
