/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


package tufts.vue.gui;

import tufts.vue.DEBUG;

import java.awt.Window;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A "loose" grouping of DockWindow's: membership is assinged
 * whenver a DockWindow's relvant edge is placed in the the region
 * while visible.
 *
 * @version $Revision: 1.6 $ / $Date: 2008-06-30 20:53:06 $ / $Author: mike $
 * @author Scott Fraize
 */

class DockRegion
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DockRegion.class);

    static final int TOP = 1;
    static final int BOTTOM = 2;

    private static java.util.List sAllRegions = new java.util.ArrayList();
        
    private java.util.List mDockedWindows = new java.util.ArrayList();
    private int mDockY;

    final int mGravity;
    final String mName;

    DockRegion(int y, int gravity, String name) {
        mDockY = y;
        mGravity = gravity;
        mName = name;
        if (gravity < TOP || gravity > BOTTOM)
            throw new UnsupportedOperationException("illegal DockRegion gravity " + gravity);
        sAllRegions.add(this);
        if (DEBUG.DOCK || DEBUG.INIT) out("CREATED");
    }

    public boolean isEmpty() {
        return mDockedWindows.size() == 0;
    }

    void moveToY(int y) {
        if (DEBUG.DOCK) out("moved to line " + y);
        mDockY = y;
    }

    int getY() {
        return mDockY;
    }

    java.util.List getDockedWindows() {
        return mDockedWindows;
    }

    private static DockRegion findRegion(int y, int height) {
        Iterator i = sAllRegions.iterator();
        while (i.hasNext()) {
            DockRegion r = (DockRegion) i.next();
            if (r.mGravity == TOP) {
                if (r.mDockY == y)
                    return r;
            } else {
                // If either the whole window, or the
                // title is riding along the bottom, include
                // it in the bottom dock.
                if (r.mDockY == y + height ||
                    r.mDockY == y + DockWindow.CollapsedHeight)
                    return r;
            }
        }
        return null;
    }

    static DockRegion findRegion(Window w) {
        if (w.isVisible())
            return findRegion(w.getY(), w.getHeight());
        else
            return null;
    }
    private static void clearAll() {
        Iterator i = sAllRegions.iterator();
        while (i.hasNext())
            ((DockRegion)i.next()).mDockedWindows.clear();
    }
    private static void updateAll() {
        Iterator i = sAllRegions.iterator();
        while (i.hasNext()) {
            DockRegion region = (DockRegion) i.next();
            region.sortByX();
            region.updateRolledWidths();
        }
    }
    static void assignAllMembers()
    {
        clearAll();

        if (DEBUG.DOCK) Log.debug("assignAllMemebers");
            
        for (DockWindow dockWindow : DockWindow.AllWindows) {
            DockRegion dockRegion = findRegion(dockWindow.window());
            // dockRegion may, of course, be null
            dockWindow.assignDockRegion(dockRegion);
            if (dockRegion != null)
                dockRegion.mDockedWindows.add(dockWindow);
        }
            
        updateAll();
    }

    //------------------------------------------------------------------
    // End of static DockRegion methods
    //------------------------------------------------------------------

    /*
      void assignMembers()
      {
      mDockedWindows.clear();
            
      Iterator i = DockWindow.sAllWindows.iterator();
      while (i.hasNext()) {
      DockWindow dw = (DockWindow) i.next();
      if (contains(dw)) {
      if (dw.mDockRegion != this) {
      if (dw.mDockRegion != null)
      dw.mDockRegion.remove(dw);
      dw.mDockRegion = this;
      }
      mDockedWindows.add(dw);
      } else if (dw.mDockRegion == this) {
      dw.mDockRegion = null;
      }
      }
      sortByX();
      out("windows: " + mDockedWindows);
      updateRolledWidths();
      }
      private boolean contains(DockWindow dw) {
      return dw.isVisible() && dw.getY() == mDockY;
      }

    */

    int getRolledWidth(DockWindow dw) {
        DockWindow rightSibling = getSiblingToRight(dw);
        if (rightSibling == null)
            return dw.getWidth();
            //return GUI.GScreenWidth - dw.getX();
        else
            return rightSibling.getX() - dw.getX();
    }

    private void updateRolledWidths() {
        Iterator i = mDockedWindows.iterator();
        while (i.hasNext()) {
            DockWindow dw = (DockWindow) i.next();
            if (dw.isRolledUp())
                dw.setSize(getRolledWidth(dw), dw.getHeight());
        }
    }

    private static Comparator SortByX = new Comparator() {
            public int compare(Object w1, Object w2) {
                return ((Window)w1).getX() - ((Window)w2).getX();
            }};
        

    private void sortByX() {
        Object windows[] = mDockedWindows.toArray();

        java.util.Arrays.sort(windows, SortByX);

        java.util.ListIterator i = mDockedWindows.listIterator();
        for (int j = 0;  j < windows.length; j++) {
            i.next();
            i.set(windows[j]);
        }
            
        DockWindow prev = null;
        DockWindow next = null;
            
        for (int k = 0; k < windows.length; k++) {
            DockWindow dw = (DockWindow) windows[k];
            if (k > 0)
                prev = (DockWindow) windows[k - 1];
            if (k + 1 < windows.length)
                next = (DockWindow) windows[k + 1];
            else
                next = null;
                    
            dw.assignDockSiblings(prev, next);
        }
            
        if (DEBUG.DOCK) out("windows: " + mDockedWindows);
    }

    /*
      void add(DockWindow dw) {
      if (dw.mDockRegion == this)
      return;
      out("adding " + dw);
      if (dw.mDockRegion != null)
      dw.mDockRegion.remove(dw);
      dw.mDockRegion = this;
      mWindows.add(dw);
      sortByX();
      out("windows: " + mWindows);
      }
      private void remove(DockWindow dw) {
      if (dw.mDockRegion == this) {
      mDockedWindows.remove(dw);
      out("removed " + dw);
      updateRolledWidths();
      }
      }
    */

    private DockWindow getSiblingToRight(DockWindow dw) {
        int next = mDockedWindows.indexOf(dw) + 1;
        if (mDockedWindows.size() > next)
            return (DockWindow) mDockedWindows.get(next);
        else
            return null;
    }

    private void out(String s) {
        System.out.println(this + " " + s);
    }

    public String toString() {
        return "DockRegion[" + mName + " y=" + mDockY + " sticky=" + (mGravity == TOP ? "top":"bot") + "]";
    }
}
