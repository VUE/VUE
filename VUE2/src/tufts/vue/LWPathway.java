/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 */


package tufts.vue;

import java.util.*;
import java.awt.Color;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathway extends tufts.vue.LWComponent 
    implements Pathway{
        
    LinkedList nodeList = null;
    int weight = 1;
    boolean ordered = false;
    Color borderColor = Color.green;
    
    public LWPathway() {
        super.setLabel("DEFAULT PATHWAY");
    }
    
    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(String label) {
        super.setLabel(label);
    }
    
    /** adds a node to the 'end' of the pathway */
    public void addNode(Node node) {
        nodeList.add(node);
    }
    
    /** adds a node at the specified location within the pathway*/
    public void addNode(Node node, int index){
        nodeList.add(index, node);
    }
    
    /** adds a node in between two other nodes, if they are adjacent*/
    public void addNode(Node node, Node adj1, Node adj2){
        int index1 = nodeList.indexOf(adj1);
        int index2 = nodeList.indexOf(adj2);
        int dif = index1 - index2;
        if(Math.abs(dif) == 1){
            if(dif == -1)
                nodeList.add(index2, node);
            else
                nodeList.add(index1, node);
        }
    }
    
    public boolean contains(Node node){
        return nodeList.contains(node);
    }
    /*
    public void dividePathway(Node node1, Node node2){
        LWPathway path1 = new LWPathway();
        LWPathway path2 = new LWPathway();
        //path1.setNodeList(this.nodeList.subList(fromIndex
    }*/
    
    public int length() {
        return nodeList.size();
    }
    
    public Color getBorderColor(){
        return borderColor;
    }
    
    public Node getFirst() {
        return (Node)nodeList.getFirst();
    }
    
    public Node getLast() {
        return (Node)nodeList.getLast();
    }
    
    public java.util.Iterator getNodeIterator() {
        return nodeList.iterator();
    }
    
    public java.util.List getNodeList() {
        return nodeList;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public boolean isOrdered() {
        return ordered;
    }
    
    public void removeNode(Node node) {
        boolean success = nodeList.remove(node);
        if(!success)
            System.err.println("LWPathway.removeNode: node does not exist in pathway");
    }
    
    public void setNodeList(java.util.List nodeList) {
        this.nodeList = (LinkedList)nodeList;
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
    
}
