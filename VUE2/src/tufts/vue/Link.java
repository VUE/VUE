package tufts.vue;

/**
 * Link.java
 *
 * @author Scott Fraize
 * @version 6/7/03
 */
public interface Link extends MapItem
{
    public MapItem getItem1();
    public MapItem getItem2();
    public void setWeight(int weight);
    public int getWeight();

    /** is this link directional? */
    public void setOrdered(boolean ordered);
    public boolean isOrdered();
}
