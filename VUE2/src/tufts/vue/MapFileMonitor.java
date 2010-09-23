package tufts.vue;

/*
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA.
 */

import java.util.*;
import java.io.File;
import java.lang.ref.WeakReference;



/**
 * Class for monitoring changes in disk files.
 * Usage:
 *
 *    1. Implement the MapFileListener interface.
 *    2. Create a FileMonitor instance.
 *    3. Add the file(s)/directory(ies) to listen for.
 *
 * fileChanged() will be called when a monitored file is created,
 * deleted or its modified time changes.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */   
public class MapFileMonitor
{
  private Timer       timer_;
  //private HashMap     files_;       // File -> Long
  // HO 29/07/2010 BEGIN
  private HashMap maps_;
  // HO 29/07/2010 END
  private Collection  listeners_;   // of WeakReference(MapFileListener)
   

  /**
   * Create a file monitor instance with specified polling interval.
   * 
   * @param pollingInterval  Polling interval in milli seconds.
   */
  public MapFileMonitor (long pollingInterval)
  {
    //files_     = new HashMap();
    // HO 29/07/2010 BEGIN
    maps_ = new HashMap();
    // HO 29/07/2010 END
    listeners_ = new ArrayList();

    timer_ = new Timer (true);
    timer_.schedule (new MapFileMonitorNotifier(), 0, pollingInterval);
  }


  
  /**
   * Stop the file monitor polling.
   */
  public void stop()
  {
    timer_.cancel();
  }
  

  /**
   * Add file to listen for. File may be any java.io.File (including a
   * directory) and may well be a non-existing file in the case where the
   * creating of the file is to be trepped.
   * <p>
   * More than one file can be listened for. When the specified file is
   * created, modified or deleted, listeners are notified.
   * 
   * @param file  File to listen for.
   */
  /* public void addFile (File file)
  {
    if (!files_.containsKey (file)) {
      long modifiedTime = file.exists() ? file.lastModified() : -1;
      files_.put (file, new Long (modifiedTime));
    }
  } */
  
  /**
   * Add map to listen for. Map may be any LWMap and may well be 
   * a non-existing file in the case where the
   * creating of the file is to be trapped.
   * <p>
   * More than one map can be listened for. When the specified map file is
   * created, modified or deleted, listeners are notified.
   * 
   * @param map  Map to listen for.
   */
  public void addMap (LWMap map)
  {
    if (!maps_.containsKey (map)) {
    	File mapFile = map.getFile();
    	maps_.put (map, mapFile);
    }
  } 

  

  /**
   * Remove specified file for listening.
   * 
   * @param file  File to remove.
   */
  /* public void removeFile (File file)
  {
    files_.remove (file);
  } */
  
  /**
   * Remove specified map for listening.
   * 
   * @param map  Map to remove.
   */
  public void removeMap (LWMap map)
  {
    maps_.remove (map);
  } 


  
  /**
   * Add listener to this file monitor.
   * 
   * @param fileListener  Listener to add.
   */
  public void addListener (MapFileListener mapFileListener)
  {
    // Don't add if its already there
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
      WeakReference reference = (WeakReference) i.next();
      MapFileListener listener = (MapFileListener) reference.get();
      if (listener == mapFileListener)
        return;
    }

    // Use WeakReference to avoid memory leak if this becomes the
    // sole reference to the object.
    listeners_.add (new WeakReference (mapFileListener));
  }


  
  /**
   * Remove listener from this file monitor.
   * 
   * @param fileListener  Listener to remove.
   */
  public void removeListener (MapFileListener mapFileListener)
  {
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
      WeakReference reference = (WeakReference) i.next();
      MapFileListener listener = (MapFileListener) reference.get();
      if (listener == mapFileListener) {
        i.remove();
        break;
      }
    }
  }


  
  /**
   * This is the timer thread which is executed every n milliseconds
   * according to the setting of the file monitor. It investigates the
   * file in question and notify listeners if changed.
   */
  private class MapFileMonitorNotifier extends TimerTask
  {
    public void run()
    {
      // Loop over the registered maps and see which have changed.
      // Use a copy of the list in case listener wants to alter the
      // list within its fileChanged method.
      //Collection files = new ArrayList (files_.keySet());
      //HO 29/07/2010 BEGIN
      Collection maps = new ArrayList(maps_.keySet());
      //HO 29/07/2010 END
      
      /*for (Iterator i = files.iterator(); i.hasNext(); ) {
        File file = (File) i.next();
        long lastModifiedTime = ((Long) files_.get (file)).longValue();
        long newModifiedTime  = file.exists() ? file.lastModified() : -1;

        // Chek if file has changed
        if (newModifiedTime != lastModifiedTime) {

          // Register new modified time
          files_.put (file, new Long (newModifiedTime));

          // Notify listeners
          for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
            WeakReference reference = (WeakReference) j.next();
            MapFileListener listener = (MapFileListener) reference.get();

            // Remove from list if the back-end object has been GC'd
            if (listener == null)
              j.remove();
            else
              listener.fileChanged (file);
          }
        }
      }*/
      
      // HO 29/07/2010 BEGIN
      for (Iterator i = maps.iterator(); i.hasNext(); ) {
          LWMap lwmap = (LWMap) i.next();
          // get the last stored file for the given map
          File lastFile = (File)maps_.get (lwmap);
          // get the actual latest file from the given map
          File newFile = lwmap.getFile();

          // Check if file has changed
          if (!newFile.equals(lastFile)) {

            // Register new modified file
            maps_.put (lwmap, newFile);

            // Notify listeners
            for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
              WeakReference reference = (WeakReference) j.next();
              MapFileListener listener = (MapFileListener) reference.get();

              // Remove from list if the back-end object has been GC'd
              if (listener == null)
                j.remove();
              else
                listener.mapChanged (lwmap);
            }
          }
        }      
      // HO 29/07/2010 END
    }
  }


  /**
   * Test this class.
   * 
   * @param args  Not used.
   */
  public static void main (String args[])
  {
    // Create the monitor
    MapFileMonitor monitor = new MapFileMonitor (1000);

    // Add some maps to listen for
    monitor.addMap (new LWMap ("test map 1"));
    monitor.addMap (new LWMap ("test map 2"));
    monitor.addMap (new LWMap ("test map 3"));    

    // Add a dummy listener
    monitor.addListener (monitor.new TestListener());

    // Avoid program exit
    while (!false) ;
  }

  
  private class TestListener
    implements MapFileListener
  {
    public void mapChanged (LWMap lwmap)
    {
      System.out.println ("File changed: " + lwmap);
    }
  }
}


