package tufts.vue;

public interface Pathway extends MapItem
{
    public java.util.Iterator getNodeIterator();
    public void addNode(Node node);
    public void removeNode(Node node);
    public void setWeight(int weight);
    public int getWeight();
    public void setOrdered(boolean ordered);
    public boolean isOrdered();
    public java.util.List getNodeList();
    public void setNodeList(java.util.List nodeList);
}
