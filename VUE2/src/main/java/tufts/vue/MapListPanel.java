
/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

/*
 * MapListPanel.java
 *
 * Created on January 22, 2007, 4:01 PM
 *
 * @author dhelle01
 */

package tufts.vue;

import java.awt.*;
import java.io.File;
import java.util.Iterator;
import javax.swing.*;

import tufts.vue.action.ActionUtil;

public class MapListPanel extends JPanel implements Scrollable {
    
    /** Creates a new instance of MapListPanel */
    public MapListPanel() {
        setLayout(new GridLayout(0,1));
    }
    
    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(200,200);
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRect,int orientation,int direction)
    {
        return 20;
    }
    
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
    
    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }
    
    public int getScrollableUnitIncrement(Rectangle visibleRect,int orientation,int direction)
    {
        return 20;
    }
    
    public void adjustColors()
    {
        for(int i=0;i<getComponentCount();i++)
        {
            ((MapListElementPanel)getComponent(i)).adjustColor(i);
        }
    }
    
    public void clearMaps()
    {
        for(int i=0;i<getComponentCount();i++)
        {
            remove(getComponent(i));
        }
    }
    
    public void loadMaps(final java.util.List<File> maps)
    {
        /*javax.swing.SwingUtilities.invokeLater(new Runnable(){
        public void run()
        {
          clearMaps();
          Iterator<File> fileList = maps.iterator();
          while(fileList.hasNext())
          { 
            File mapFile = fileList.next();
            LWMap map = null;
            try
            {        
              map = ActionUtil.unmarshallMap(mapFile);
            }
            catch(Exception ex)
            {
              ex.printStackTrace();
            }
            MapListElementPanel mlep = new MapListElementPanel(map);
            mlep.adjustColor(getComponentCount());
            add(mlep);
          }
        }});*/
    }
    
}
