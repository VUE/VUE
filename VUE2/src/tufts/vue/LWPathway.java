/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 */


package tufts.vue;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Area;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathway extends tufts.vue.LWComponent 
    implements Pathway
{
        
    private LinkedList elementList = null;
    private LWComponent current = null;
    private int weight = 1;
    private String comment = "";
    private boolean ordered = false;
    private Color borderColor = Color.blue;
    private LWMap map = null;
    
    /**default constructor used for marshalling*/
    public LWPathway() {
        //added by Daisuke
        elementList = new LinkedList();    
    }
    
    public LWPathway(LWMap map, String label) {
        this(label);
        this.map = map;
        
    }
    
    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(String label) {
        super.setLabel(label);
        elementList = new LinkedList();
        
    }
     
    /** adds an element to the 'end' of the pathway */
    public void addElement(LWComponent element) {
        elementList.add(element);
        if(current == null) setCurrent(element);
    }
    
    /** adds an element at the specified location within the pathway*/
    public void addElement(LWComponent element, int index){
        if(elementList.size() >= index){
            elementList.add(index, element);
            if(current == null) setCurrent(element);
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
    
    public void draw(Graphics2D g){
        /*Iterator iter = this.getElementIterator();
        while(iter.hasNext()){
            LWComponent comp = (LWComponent)iter.next();
            if(comp instanceof LWNode){
                ((LWNode)comp).setPathway(this);
            } else if(comp instanceof LWLink){
                
            }
        }*/
    }
    
    public boolean contains(LWComponent element){
        return elementList.contains(element);
    }
    
    public int length() {
        return elementList.size();
    }
    
    public LWComponent getFirst() {
        
        LWComponent firstElement = null;
        
        try
        {
            firstElement = (LWComponent)elementList.getFirst();
        }
        
        catch(NoSuchElementException ne)
        {
            firstElement = null;
        }
        
        return firstElement;
    }
    
    public boolean isFirst(LWComponent element)
    {
        return (element.equals(getFirst()));
    }
    
    public LWComponent getLast() {
        
        LWComponent lastElement = null;
        
        try
        {
            lastElement = (LWComponent)elementList.getLast();
        }
        
        catch(NoSuchElementException ne)
        {
            lastElement = null;
        }
        
        return lastElement;
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
    
    public void removeElement(LWComponent element) {
        
        if (element.equals(getCurrent()))
        {
            if (isFirst(element))
                setCurrent(getNext(element));
            
            else
                setCurrent(getPrevious(element));
        }
        
        boolean success = elementList.remove(element);
        if(!success)
            System.err.println("LWPathway.removeElement: element does not exist in pathway");
    }
    
    /**accessor methods used also by xml marshalling process*/
    public Color getBorderColor(){
        return borderColor;
    }
    
    public void setBorderColor(Color color){
        this.borderColor = color;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public boolean getOrdered() {
        return ordered;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
    
    public java.util.List getElementList() {
        return elementList;
    }
    
    public void setElementList(java.util.List elementList) {
        this.elementList = (LinkedList)elementList;
        if(elementList.size() >= 1) current = (LWComponent)elementList.get(0);
    }
    
    public LWComponent getCurrent() {
        return current;
    }
    
    public void setCurrent(LWComponent comp){
        current = comp;
    }
    
    public String getComment(){
        return comment;
    }
    
    public void setComment(String comment){
        this.comment = comment;
    }
    
    public void mapViewerEventRaised(MapViewerEvent e) {
        System.out.println("MapViewer event: "+e);
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
        
    }*/
    
}
