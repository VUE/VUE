package tufts.vue;

public class Link extends MapItem
{
    private MapItem node1;
    private MapItem node2;
    private int weight = 1;
    private boolean ordered = true;
    private boolean fixed = false;

    public Link() {
        
    }
    
    public Link(MapItem i1, MapItem i2)
    {
        setItem1(i1);
        setItem2(i2);
    }
    
    public void setItem1(MapItem node)
    {
        this.node1 = node;
    }
    public MapItem getItem1()
    {
        return this.node1;
    }

    public void setItem2(MapItem node)
    {
        this.node2 = node;
    }
    public MapItem getItem2()
    {
        return this.node2;
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
