/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 *
 * @author  Jay Briedis
 */

package tufts.vue;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.geom.Line2D;
import java.awt.geom.Area;
import java.util.ArrayList;

public class LWPathway extends LWContainer
    implements LWComponent.Listener
{
    private int weight = 1;
    private boolean ordered = false;
    private int mCurrentIndex = -1;
    private boolean open = true;
    private boolean locked = false;
    private boolean mDoingXMLRestore = false;

    private ArrayList elementPropertyList = new ArrayList();

    private static Color[] ColorTable = {
        new Color(153, 51, 51),
        new Color(204, 51, 204),
        new Color(51, 204, 51),
        new Color(51, 204, 204),
        new Color(255, 102, 51),
        new Color(51, 102, 204),
    };
    private static int sColorIndex = 0;
    
    /**default constructor used for marshalling*/
    public LWPathway() {
        mDoingXMLRestore = true;
    }

    LWPathway(String label) {
        this(null, label);
    }

    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(LWMap map, String label) {
        setMap(map);
        setLabel(label);
        setStrokeColor(getNextColor());
    }

    private static Color getNextColor()
    {
        if (sColorIndex >= ColorTable.length)
            sColorIndex = 0;
        return ColorTable[sColorIndex++];
        /*
        LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        System.out.println("manager: " + manager.toString());
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 6);
        }
        System.out.println("pathway border color: " + borderColor.toString());
        */
    }
     
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    private int setIndex(int i)
    {
        System.out.println(this + " setIndex " + i);
        return mCurrentIndex = i;
    }

    /*
    protected void notifyLWCListeners(LWCEvent e)
    {
        // notify our explicit listeners
        super.notifyLWCListeners(e);
        // simulate being parented to our map -- this
        // is how the map knows to redraw itself if
        // something is added/removed to the pathway
        // [already done as we've setParent to the map]
        if (getMap() != null)
            getMap().notifyLWCListeners(e);
    }
    */

    /**
     * Overrides LWContainer addChild.  Pathways aren't true
     * parents, so all we want to do is add a reference to them.
     */
    public void addChild(LWComponent c)
    {
        children.add(c);
        c.addPathwayRef(this);
        new Throwable(this + " addChild " + c).printStackTrace();
        // don't want to add to element props if they already have some!
        // (is happening on restore)
        elementPropertyList.add(new LWPathwayElementProperty(c.getID()));
        if (mCurrentIndex == -1) setIndex(length() - 1);
        c.addLWCListener(this);       
        notify(LWCEvent.ChildAdded, c);
    }
    public void removeChild(LWComponent c)
    {
        children.remove(c);
        c.removePathwayRef(this);
        c.removeLWCListener(this);       
        notify(LWCEvent.ChildRemoved, c);
    }
    
    /** adds an element to the end of the pathway */
    public void add(LWComponent c) {
        addChild(c);
    }

    public void remove(LWComponent c) {
        removeChild(c);
    }

    public void add(Iterator i)
    {
        while (i.hasNext()) add((LWComponent) i.next());
    }
    /*
    public void addElement(LWComponent c) {
        add(c);
    }
    */
    public void LWCChanged(LWCEvent e)
    {
        if (e.getWhat() == LWCEvent.Deleting)
            remove(e.getComponent());
    }
    
    public LWMap getMap(){
        return (LWMap) getParent();
    }
    public void setMap(LWMap map) {
        setParent(map);
    }

    public void setLocked(boolean t) {
        this.locked = t;
    }
    public boolean isLocked(){
        return locked;
    }
    
    public void setOpen(boolean open){
        this.open = open;
    }
    
    public boolean isOpen() {
        return open;
    }

    public boolean contains(LWComponent c) {
        return children.contains(c);
    }

    public int length() {
        return children.size();
    }
    
    public LWComponent getFirst()
    {
        if (length() > 0) {
            setIndex(0);
            return (LWComponent) children.get(0);
        }
        return null;
    }
    
    public boolean isFirst(){
        return (mCurrentIndex == 0);
    }
    
    public LWComponent getLast() {        
        if (length() > 0) {
            setIndex(length() - 1);
            return (LWComponent) children.get(length() - 1);
        }
        return null;
    }
    
    public boolean isLast(){
        return (mCurrentIndex == (length() - 1));
    }
      
    public LWComponent getPrevious(){
        if (mCurrentIndex > 0)
            return (LWComponent) children.get(setIndex(mCurrentIndex - 1));
        else
            return null;
    }
    
    public LWComponent getNext(){
        if (mCurrentIndex < (length() - 1))
            return (LWComponent) children.get(setIndex(mCurrentIndex + 1));
        else 
            return null;
    }

    /*
    public LWComponent getElement(int index){
        LWComponent element = null;
        
        try{
            element = (LWComponent)elementList.get(index);
        }catch (IndexOutOfBoundsException ie){
            element = null;
        }    
        
        return element;
    }
    */

    /** Pathway interface */
    public java.util.Iterator getElementIterator() {
        return children.iterator();
    }
    /*    
    public void removeElement(int index) {
        removeElement(index, false);
    }

    protected void removeElement(int index, boolean deleted) {
        
        //if the current node needs to be deleted and it isn't the first node, 
        //set the current index to the one before, else keep the same index
        if (index == mCurrentIndex && !isFirst())
            //if (!isFirst())
          mCurrentIndex--;
        
        //if the node to be deleted is before the current node, set the current index to the one before
        else if (index < mCurrentIndex)
          mCurrentIndex--;
        
        LWComponent element = (LWComponent)elementList.remove(index);
        
        System.out.println(this + " removing index " + index + " c="+element);

        for(Iterator i = elementPropertyList.iterator(); i.hasNext();)
        {
            if(((LWPathwayElementProperty)i.next()).getElementID().equals(element.getID()))
            {
                i.remove();
                break;
            }
        }
        
        
        if (element == null) {
            System.err.println(this + " removeElement: element does not exist in pathway");
        } else {
            if (!deleted) {
                element.removePathwayRef(this);
                //element.removeLWCListener(this);
            }
        }
    }
       
    public void removeElement(LWComponent element) {
      
       System.out.println("the element version of the remove is being called");
       for(int i = 0; i < elementList.size(); i++){
            LWComponent comp = (LWComponent)elementList.get(i);
            if(comp.equals(element)){
                this.removeElement(i);
                element.removePathwayRef(this);
                //break;
            }
       }
    }
*/
    /**
     * Make sure we've completely cleaned up the pathway when it's
     * been deleted (must get rid of LWComponent references to this
     * pathway)
     */
    protected void removeFromModel()
    {
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.removePathwayRef(this);
       }
    }

    public void moveElement(int oldIndex, int newIndex) {
        throw new UnsupportedOperationException("LWPathway.moveElement");
        // will need to clean up add/remove element code at bottom
        // and track addPathRefs before can put this back in
        /*
        LWComponent element = getElement(oldIndex);
        removeElement(oldIndex);
        addElement(element, newIndex);
        */
    }
    
    public boolean getOrdered() {
        return ordered;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    /**
     * for persistance: override of LWContainer: pathways never save their children
     * as they don't own them -- they only save ID references to them.
     */
    public ArrayList getChildList()
    {
        return null;
    }
    
    public java.util.List getElementList() {
        //System.out.println(this + " getElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
        return super.children;
    }

    /** for persistance */
    public java.util.List getElementPropertyList() {
        return elementPropertyList;
    }
    
    private List idList = new ArrayList();
    /** for XML save/restore only */
    public List getElementIDList() {
        if (mDoingXMLRestore) {
            return idList;
        } else {
            idList.clear();
            Iterator i = getElementIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                idList.add(c.getID());
            }
        }
        System.out.println(this + " getElementIDList: " + idList);
        return idList;
    }


    /*
    public void setElementIDList(List idList) {
        System.out.println(this + " setElementIDList: " + idList);
        this.idList = idList;
    }
    */
    
    void completeXMLRestore(LWMap map)
    {
        System.out.println(this + " completeXMLRestore, map=" + map);
        setParent(map);
        Iterator i = this.idList.iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            LWComponent c = getMap().findChildByID(id);
            System.out.println("\tpath adding " + c);
            add(c);
        }
        mDoingXMLRestore = false;
    }
    
    /** Interface for the linked list used by the Castor mapping file*/
    /**
    public ArrayList getElementArrayList()
    {
        System.out.println("calling get elementarraylist for " + getLabel());
         return new ArrayList(elementList);
    }
    
    public void setElementArrayList(ArrayList list)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList = new LinkedList(list);
    }
    **/
    /*
    public void setElementArrayList(LWComponent component)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList.add(component);
    }
    */
    /** end of Castor Interface */
    
    /** return the current element */
    public LWComponent getCurrent() { 
        LWComponent c = null;
        try {
            c = (LWComponent) children.get(mCurrentIndex);
        } catch (IndexOutOfBoundsException ie){
            c = null;
        }      
        return c;
    }
    
    /** for PathwayTable */
    void setCurrentElement(LWComponent c) {
        setIndex(children.indexOf(c));
        notify(LWCEvent.Repaint);
    }
    
    public int getCurrentIndex(){
        return mCurrentIndex;
    }

    public String getElementNotes(LWComponent c)
    {
        if (c == null) return null;
        
        String notes = null;
        
        for (Iterator i = elementPropertyList.iterator(); i.hasNext();) {
            LWPathwayElementProperty element = (LWPathwayElementProperty) i.next();
            if (element.getElementID().equals(c.getID())) {
                notes = element.getElementNotes();
                break;
            }
        }
        //System.out.println("returning notes for " + c + " [" + notes + "]");
        return notes;
    }

    public void setElementNotes(LWComponent component, String notes)
    {   
        if (notes == null || component == null)
        {
            System.err.println("argument(s) to setElementNotes is null");
            return;
        }
        
        for(Iterator i = elementPropertyList.iterator(); i.hasNext();)
        {
            LWPathwayElementProperty element = (LWPathwayElementProperty)i.next();
            
            if (element.getElementID().equals(component.getID()))
            {
                element.setElementNotes(notes);
                //can elements be in a pathway twice?
                break;
            }
        }
    }

    
    private static final AlphaComposite PathTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    private static final AlphaComposite PathSelectedTranslucence = PathTranslucence;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.Src;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    private static float dash_length = 4;
    private static float dash_phase = 0;
    public void drawPathway(DrawContext dc){
        Iterator i = this.getElementIterator();
        Graphics2D g = dc.g;

        if (dc.getIndex() % 2 == 0)
            dash_phase = 0;
        else
            dash_phase = 0.5f;
        if (DEBUG.PATHWAY) System.out.println("Drawing " + this + " index=" + dc.getIndex() + " phase=" + dash_phase);
        
        g.setColor(getStrokeColor());
        LWComponent last = null;
        Line2D connector = new Line2D.Float();
        BasicStroke connectorStroke =
            new BasicStroke(6, BasicStroke.CAP_BUTT
                            , BasicStroke.JOIN_BEVEL
                            , 0f
                            , new float[] { dash_length, dash_length }
                            , dash_phase);

        final int BaseStroke = 3;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent)i.next();

            int strokeWidth;
            boolean selected = (getCurrent() == c);
            strokeWidth = BaseStroke;

            // because we're drawing under the object, only half of
            // the amount we add to to the stroke width is visible
            // outside the edge of the object, except for links,
            // which are one-dimensional, so we use a narrower
            // stroke width for them.
            if (c instanceof LWLink)
                ;//strokeWidth++;
            else
                strokeWidth *= 2;

            if (selected)
                g.setComposite(PathSelectedTranslucence);
            else
                g.setComposite(PathTranslucence);
        
            strokeWidth += c.getStrokeWidth();
            if (selected) {
                g.setStroke(new BasicStroke(strokeWidth*2));
            } else {
                g.setStroke(new BasicStroke(strokeWidth
                                            , BasicStroke.CAP_BUTT
                                            , BasicStroke.JOIN_BEVEL
                                            , 0f
                                            , new float[] { dash_length, dash_length }
                                            , dash_phase));
            }
            g.draw(c.getShape());

            // If there was an element in the path before this one,
            // draw a connector line from that last component to this
            // one.
            if (last != null) {
                g.setComposite(PathTranslucence);
                connector.setLine(last.getCenterPoint(), c.getCenterPoint());
                g.setStroke(connectorStroke);
                g.draw(connector);
            }
            last = c;
        }
    }
    
    public String toString()
    {
        return "LWPathway[" + label
            + " n="
            + (children==null?-1:children.size())
            + " idx="+mCurrentIndex
            + " map=" + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }

}
