package tufts.vue;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.event.*;

/**
 * MapMouseEvent.java
 *
 * Extension of MouseEvent for events that happen
 * on an instance of LWMap.
 *
 * @author Scott Fraize
 * @version 11/5/03
 */
public class MapMouseEvent extends MouseEvent
{
    float mapX;
    float mapY;
    LWComponent hitComponent;
    LWComponent dragRequest;
    Rectangle selectorBox;
    int pressX;
    int pressY;
    
    
    public MapMouseEvent(MouseEvent e, float mapX, float mapY, LWComponent hitComponent, Rectangle selectorBox)
    {
        super(e.getComponent(),
              e.getID(),
              e.getWhen(),
              e.getModifiers(),
              e.getX(),
              e.getY(),
              e.getClickCount(),
              e.isPopupTrigger(),
              e.getButton());

        this.mapX = mapX;
        this.mapY = mapY;
        this.hitComponent = hitComponent;
        this.selectorBox = selectorBox;
    }

    public MapMouseEvent(MouseEvent e)
    {
        this(e, 0f, 0f, null, null);
        this.mapX = getViewer().screenToMapX(e.getX());
        this.mapY = getViewer().screenToMapY(e.getY());
    }
    
    public MapMouseEvent(MouseEvent e, LWComponent c)
    {
        this(e);
        setHitComponent(c);
    }
    
    public MapMouseEvent(MouseEvent e, Rectangle selectorBox)
    {
        this(e);
        setSelectorBox(selectorBox);
    }

    public void setMousePress(int pressX, int pressY)
    {
        this.pressX = pressX;
        this.pressY = pressY;
    }
    public int getDeltaPressX() { return getX() - pressX; }
    public int getDeltaPressY() { return getY() - pressY; }
    
    public void setDragRequest(LWComponent c) { this.dragRequest = c; }
    public LWComponent getDragRequest() { return this.dragRequest; }

    public Rectangle getSelectorBox() {
        return this.selectorBox;
    }
    void setSelectorBox(Rectangle r) {
        this.selectorBox = r;
    }
    
    public Rectangle2D getMapSelectorBox()
    {
        return getViewer().screenToMapRect(this.selectorBox);
    }


    public MapViewer getViewer() {
        return (MapViewer) super.getSource();
    }
    public LWMap getMap() {
        return getViewer().getMap();
    }

    public float getMapX() {
        return mapX;
    }
    public float getMapY() {
        return mapY;
    }
    public Point2D.Float getMapPoint() {
        return new Point2D.Float(mapX, mapY);
    }

    public float getComponentX() {
        return (mapX - hitComponent.getX()) / hitComponent.getScale();
    }
    public float getComponentY() {
        return (mapY - hitComponent.getY()) / hitComponent.getScale();
    }
    public Point2D.Float getComponentPoint() {
        return new Point2D.Float(getComponentX(), getComponentY());
    }

    public LWComponent getHitComponent() {
        return hitComponent;
    }
    void setHitComponent(LWComponent c) {
        this.hitComponent = c;
    }


    public String toString()
    {
        return super.toString() + " hit=" + hitComponent;
    }
    
}
