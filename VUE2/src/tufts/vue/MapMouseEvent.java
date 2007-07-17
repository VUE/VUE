/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.Util;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.event.*;
import javax.swing.JViewport;
import javax.swing.JScrollPane;

/**
 * Extension of MouseEvent for events that happen on an instance of LWMap
 * in a MapViewer.
 *
 * @version $Revision: 1.15 $ / $Date: 2007-07-17 00:53:20 $ / $Author: sfraize $
 * @author Scott Fraize
 */

/*
 * Todo arch: rename LWMouseEvent and package with all the LW model classes as
 * the primary gui interaction channel between the model and a gui panel displaying a map.
 * Will  need to create a viewer interface to generically handle conversions between
 * viewer coordinates and LWMap coordinates.
 */

public class MapMouseEvent extends MouseEvent
{
    private static final int COMMAND_MASK = VueUtil.isMacPlatform() ? InputEvent.META_MASK : InputEvent.ALT_MASK;
    
    float mapX;
    float mapY;
    LWComponent picked;
    boolean pickedSet = false;
    LWComponent dragRequest;
    Rectangle selectorBox;
    int pressX;
    int pressY;
    
    public MapMouseEvent(MouseEvent e, float mapX, float mapY, LWComponent picked, Rectangle selectorBox)
    {
        super(e.getComponent(),
              e.getID(),
              e.getWhen(),
              e.getModifiers() | e.getModifiersEx(),
              e.getX(),
              e.getY(),
              e.getClickCount(),
              e.isPopupTrigger(),
              e.getButton());

        this.mapX = mapX;
        this.mapY = mapY;
        if (DEBUG.PICK) out("created: picked=" + picked);
        //if (picked==null) tufts.Util.printStackTrace("NULL PICKED");
        if (picked != null)
            setPicked(picked);
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
        setPicked(c);
    }
    
    public MapMouseEvent(MouseEvent e, Rectangle selectorBox)
    {
        this(e);
        setSelectorBox(selectorBox);
    }

    public boolean isCommandDown() {
        return (getModifiers() & COMMAND_MASK) != 0;
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
        // todo perf: cache viewer
        Object src = super.getSource();
        if (src instanceof JViewport)
            return (MapViewer) ((JViewport)src).getView();
        else if (src instanceof JScrollPane)
            return (MapViewer) ((JScrollPane)src).getViewport().getView();
        else
            return (MapViewer) src;
    }

    public LWMap getMap() {
        return getViewer().getMap();
    }

    public LWComponent getFocal() {
        return getViewer().getFocal();
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

//     // TODO: these handling new coord system??
//     public float getComponentX() {
//         getPicked();
//         return picked == null ? Float.NaN : (mapX - picked.getX()) / picked.getScaleF();
//     }
//     public float getComponentY() {
//         getPicked();
//         return picked == null ? Float.NaN : (mapY - picked.getY()) / picked.getScaleF();
//     }
//     public Point2D.Float getComponentPoint() {
//         return new Point2D.Float(getComponentX(), getComponentY());
//     }


    public Point2D.Float getLocalPoint(LWComponent local) {
        if (local != picked && picked != null) {
            if (DEBUG.Enabled)
                Util.printStackTrace("warning: local != picked"
                                     + "\n\tpicked: " + picked
                                     + "\n\t local: " + local);
        }
        // todo perf minor: could cache this
        return local.transformMapToLocalPoint(getMapPoint());
    }
    

    public LWComponent getPicked() {
        if (!pickedSet) {
            if (DEBUG.PICK) out("computing picked...");
            setPicked(getViewer().pickNode(mapX, mapY));
        } else
            if (DEBUG.PICK) out("picked is already: " + picked);
        return picked;
    }

    void setPicked(LWComponent c) {
        if (DEBUG.PICK) out("setting picked to " + c);
        this.picked = c;
        this.pickedSet = true;
    }

    private void out(String s) {
        System.out.println("MapMouseEvent@"
                           + Integer.toHexString(hashCode())
                           + "[" +
                           getViewer()
                           + "]: " + s
                           );
    }


    public String paramString() {
        return super.paramString()
//            + ",cx=" + getComponentX()
//            + ",cy=" + getComponentY()
            + ",hit=" + (pickedSet ? "<unset>" : picked);
    }

}
