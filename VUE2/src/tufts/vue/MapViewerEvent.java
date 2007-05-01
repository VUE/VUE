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

/**
 * Broadcast events for the active map viewer.
 *
 * @version $Revision: 1.17 $ / $Date: 2007-05-01 04:32:02 $ / $Author: sfraize $ 
 */

public class MapViewerEvent extends EventRaiser<java.awt.Component>
{
    public static final int DISPLAYED = 1;
    public static final int HIDDEN = 2;
    public static final int PAN = 4;
    public static final int ZOOM = 8;
    public static final int FOCUSED = 16;
    
    public final int id;
    
    public MapViewerEvent(MapViewer mapViewer, int id)
    {
        super(mapViewer, MapViewer.Listener.class);
        this.id = id;
    }

    public int getID() {
        return id;
    }
    
    public MapViewer getMapViewer() {
        return (MapViewer) getSource();
    }

    public boolean isActivationEvent() {
        return (id & (DISPLAYED|FOCUSED)) != 0;
    }

    public void dispatch(java.awt.Component listener)
    {
        ((MapViewer.Listener)listener).mapViewerEventRaised(this);
    }

    public String toString()
    {
        String name = null;
        if (id == DISPLAYED)    name = "DISPLAYED";
        else if (id == HIDDEN)  name = "HIDDEN   ";
        else if (id == PAN)     name = "PAN      ";
        else if (id == ZOOM)    name = "ZOOM     ";
        else if (id == FOCUSED) name = "FOCUSED  ";
        return "MapViewerEvent[" + name + " " + getSource() + "]";
    }
    
    
}


