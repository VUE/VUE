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
 * The currently selected resource.  Currently only supports a single selected
 * resource at a time.
 *
 * @version $Revision: 1.7 $ / $Date: 2006-07-27 22:25:43 $ / $Author: sfraize $
 */
public class ResourceSelection
{
    private Resource selected = null;
    
    private java.util.List listeners = new java.util.ArrayList();

    public interface Listener extends java.util.EventListener {
        void resourceSelectionChanged(ResourceSelection.Event e);
    }
    public static class Event {
        final public Resource selected;
        final public Object source;
        public Event(Resource r, Object src) {
            selected = r;
            source = src;
        }
    }

    public void addListener(Listener l)
    {
        listeners.add(l);
    }
    public void removeListener(Listener l)
    {
        listeners.remove(l);
    }

    private void notifyListeners(Object source)
    {
        java.util.Iterator i = listeners.iterator();
        Event e = new Event(this.selected, source);
        while (i.hasNext()) {
            Listener l = (Listener) i.next();
            if (DEBUG.SELECTION) System.out.println("ResourceSelection notifying: " + l);
            l.resourceSelectionChanged(e);
        }
    }
       
    public Resource get() {
        return selected;
    }
    
    public void setTo(Resource r) {
        setTo(r, null);
    }
    
    public void setTo(Resource r, Object src) {
        if (selected != r) {
            if (DEBUG.SELECTION) System.out.println("ResourceSelection: set to " + r.getClass() + " " + r);
            selected = r;
            notifyListeners(src);
        }
    }
    
    public void clearAndNotify() {
    	clear0();
    	notifyListeners(null);
    }
    
    public void clear()
    {
        if (clear0())
            notifyListeners(null);
    }

    private boolean clear0()
    {
        if (selected == null)
            return false;
        
        if (DEBUG.SELECTION) System.out.println("ResourceSelection: clear " + selected);
        selected = null;
        return true;
    }
    
}
