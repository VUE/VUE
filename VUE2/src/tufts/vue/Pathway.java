package tufts.vue;

public class Pathway extends MapItem
{
    private java.util.List nodeList = new java.util.ArrayList();
    private int weight = 0;
    private boolean ordered = true;

    public java.util.Iterator getNodeIterator()
    {
        return this.nodeList.iterator();
    }

    public void addNode(Node node)
    {
        nodeList.add(node);
    }

    public void removeNode(Node node)
    {
        nodeList.remove(node);
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
     public java.util.Vector getNodeList() {
        return (java.util.Vector)nodeList;
    }
    
    public void setNodeList(java.util.Vector nodeList) {
        this.nodeList = (java.util.List)nodeList;
    }
}
