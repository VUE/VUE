package tufts.vue;

public class Link extends MapItem
{
    private MapItem item1;
    private MapItem item2;
    private int weight = 1;
    private boolean ordered = false;
    private boolean fixed = false;

    // these used only during restore
    private String item1_ID;
    private String item2_ID;
    
    /**
     * null constructor to support restore only
     */
    public Link() {}
    
    public Link(MapItem i1, MapItem i2)
    {
        setItem1(i1);
        setItem2(i2);
    }
    
    public void setItem1(MapItem node)
    {
        this.item1 = node;
    }
    public MapItem getItem1()
    {
        return this.item1;
    }

    public void setItem2(MapItem node)
    {
        this.item2 = node;
    }
    public MapItem getItem2()
    {
        return this.item2;
    }

    public String getItem1_ID()
    {
        //System.err.println("getItem1_ID called for " + this);
        if (getItem1() == null) {
            return item1_ID;
        } else
            return getItem1().getID();
    }
    public String getItem2_ID()
    {
        //System.err.println("getItem2_ID called for " + this);
        if (getItem2() == null) {
            return item2_ID;
        } else
            return getItem2().getID();
    }

    // used only during restore
    public void setItem1_ID(String s)
    {
        this.item1_ID = s;
    }
    // used only during restore
    public void setItem2_ID(String s)
    {
        this.item2_ID = s;
    }
    
    public void setWeight(int weight)
    {
        this.weight = weight;
    }

    public int getWeight()
    {
        return this.weight;
    }

    public int incrementWeight()
    {
        this.weight += 1;
        return this.weight;
    }

    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    
    public boolean isOrdered()
    {
        return this.ordered;
    }
    public void setFixed(boolean fixed)
    {
        this.fixed = fixed;
    }
    
    public boolean isFixed()
    {
        return this.fixed;
    }
    
}
