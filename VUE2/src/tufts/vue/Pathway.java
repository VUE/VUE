package tufts.vue;

public interface Pathway //extends MapItem
{
    public java.util.Iterator getElementIterator();
    public void addElement(Object element);
    public void removeElement(int index);
    public void moveElement(int oldIndex, int newIndex);
    public void setWeight(int weight);
    public int getWeight();
    public void setOrdered(boolean ordered);
    public boolean getOrdered();
    public java.util.List getElementList();
    //public void setElementList(java.util.List elementList);
    public String getLabel();
    public void setLabel(String label);
    public String getNotes();
    public void setNotes(String notes);
}
