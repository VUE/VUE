/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 */


package tufts.vue;

import java.util.*;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathway extends tufts.vue.LWComponent 
    implements Pathway{
        
    private LinkedList elementList = null;
    private LWComponent currentElement = null;
    private int weight = 1;
    private String comment = "";
    private boolean ordered = false;
    private Color borderColor = Color.blue;
    private LWPathwayManager manager = null;
    
    public LWPathway() {
        elementList = new LinkedList();
        manager = LWPathwayManager.getInstance();
        manager.addPathway(this);
        manager.setCurrentPathway(this);
    }
    
    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(String label) {
        this();
        super.setLabel(label);
    }
    
    /** adds an element to the 'end' of the pathway */
    public void addElement(LWComponent element) {
        elementList.add(element);
        if(currentElement == null) currentElement = element;
    }
    
    /** adds an element at the specified location within the pathway*/
    public void addElement(LWComponent element, int index){
        if(elementList.size() >= index){
            elementList.add(index, element);
            if(currentElement == null) currentElement = element;
        }else{
            System.out.println("LWPathway.addElement(element, index), index out of bounds");
        }
    }
    
    /** adds an element in between two other elements, if they are adjacent*/
    public void addElement(LWComponent element, LWComponent adj1, LWComponent adj2){
        int index1 = elementList.indexOf(adj1);
        int index2 = elementList.indexOf(adj2);
        int dif = index1 - index2;
        if(elementList.size() >= index1 && elementList.size() >= index2){
            if(Math.abs(dif) == 1){
                if(dif == -1)
                    elementList.add(index2, element);
                else
                    elementList.add(index1, element);
            }
        }else{
            System.out.println("LWPathway.addElement(element,adj1,adj2), index out of bounds");
        }
    }
    
    public void draw(Graphics2D g)
    {
        //do nothing
    }
    
    public boolean contains(LWComponent element){
        return elementList.contains(element);
    }
    
    public int length() {
        return elementList.size();
    }
    
    public Color getBorderColor(){
        return borderColor;
    }
    
    public LWComponent getFirst() {
        
        return (LWComponent)elementList.getFirst();
    }
    
    public boolean isFirst(LWComponent element)
    {
        return (element.equals(getFirst()));
    }
    
    public LWComponent getLast() {
        return (LWComponent)elementList.getLast();
    }
    
    public boolean isLast(LWComponent element)
    {
        return (element.equals(getLast()));
    }
    
    public LWComponent getPrevious(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index > 0)
          return (LWComponent)elementList.get(--index);
       
        else
          return null;
        
    }
    
    public LWComponent getNext(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index >= 0 && index < (length() - 1))
          return (LWComponent)elementList.get(++index);
        
        else
          return null;
    }
   
    public LWComponent getElement(int index)
    {
        return (LWComponent)elementList.get(index);
    }
    
    public java.util.Iterator getElementIterator() {
        return elementList.iterator();
    }
    
    public java.util.List getElementList() {
        return elementList;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public boolean isOrdered() {
        return ordered;
    }
    
    public LWComponent getCurrent() {
        return currentElement;
    }
    
    public void removeElement(LWComponent element) {
        boolean success = elementList.remove(element);
        if(!success)
            System.err.println("LWPathway.removeElement: element does not exist in pathway");
    }
    
    public void setElementList(java.util.List elementList) {
        this.elementList = (LinkedList)elementList;
        if(elementList.size() >= 1) currentElement = (LWComponent)elementList.get(0);
    }
    
    public void setCurrent(LWComponent comp){
        currentElement = comp;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
    
    public void setBorderColor(Color color){
        this.borderColor = color;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public String getComment(){
        return comment;
    }
    
    public void setComment(String comment){
        this.comment = comment;
    }
    
    //testing constructor
    /*public LWPathway(int i)
    {
        this();
        
        if (i == 0)
        {
            LWNode node1 = new LWNode("Node 1");
            LWNode node2 = new LWNode("Node 2");
            LWNode node3 = new LWNode("Node 3");
            LWNode node4 = new LWNode("Node 4");
        
            this.addNode(node1);
            this.addNode(node2);
            this.addNode(node3);
            this.addNode(node4);
        }
        
        else
        {
            LWNode node1 = new LWNode("AT");
            LWNode node2 = new LWNode("Power Team");
            LWNode node3 = new LWNode("VUE");
            LWNode node4 = new LWNode("Pathway");
        
            this.addNode(node1);
            this.addNode(node2);
            this.addNode(node3);
            this.addNode(node4);
            
            this.setComment("Testing new notes section with a long string." +
                "This string needs to be much longer than this.");
        }
    }*/
    
    /*
    public void dividePathway(Node node1, Node node2){
        LWPathway path1 = new LWPathway();
        LWPathway path2 = new LWPathway();
        //path1.setNodeList(this.nodeList.subList(fromIndex
    }*/
    
}
