package tufts.vue;

public class Link extends MapItem
{
    private Node node1;
    private Node node2;
    private int weight = 0;
    private boolean ordered = true;

    public Link(Node n1, Node n2)
    {
        setNode1(n1);
        setNode2(n2);
    }
    
    public void setNode1(Node node)
    {
        this.node1 = node;
    }
    public Node getNode1()
    {
        return this.node1;
    }

    public void setNode2(Node node)
    {
        this.node2 = node;
    }
    public Node getNode2()
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

    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    
    public boolean isOrdered()
    {
        return this.ordered;
    }
    
}
