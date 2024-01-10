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
 * PolygonIcon.java
 *
 * Created on September 17, 2003, 4:49 PM
 */

package tufts.vue;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author  akumar03
 */
public class PolygonIcon implements Icon {
    
    
    private final int DEFAULT_HEIGHT = 5;
    private final int DEFAULT_WIDTH = 5;
    private Color color = Color.BLACK;
    private int height  = DEFAULT_HEIGHT;;
    private int width = DEFAULT_WIDTH;
    private Shape shape;
    /** Creates a new instance of PolygonIcon */
    public PolygonIcon() {
    }
    
    public PolygonIcon(Color color) {
        this.color = color;
    }
    
    public PolygonIcon(Shape shape,Color color){
        this.color = color;
        this.shape = shape;
        
    }
    public void setIconWidth(int width){
        
        this.width = width;
                
    }  
      public void setIconHeight(int height){
        
        this.height = height;
        
    }  
        
    public int getIconWidth() {
        
        return width;
    }
    
    public int getIconHeight() {
        return height;
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
       Graphics2D g2d = (Graphics2D) g;
       if(shape == null){
           shape = new Rectangle(x,y,width,height);
       }
       g2d.setColor(color);
       g2d.fill(shape);
    }
    
}
