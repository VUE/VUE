
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

/*
 * includes ColorBrewer Color schemes under the following license and conditions:
 *
 * Apache-Style Software License for ColorBrewer software and ColorBrewer Color Schemes
 *
 *  Copyright (c) 2002 Cynthia Brewer, Mark Harrower, and The Pennsylvania State University.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions as source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * "This product includes color specifications and designs developed by Cynthia Brewer (http://colorbrewer.org/)."
 * Alternately, this acknowledgment may appear in the software itself, if and wherever such third-party acknowledgments normally appear.
 * 4. The name "ColorBrewer" must not be used to endorse or promote products derived from this software without prior written permission. For written permission, please contact Cynthia Brewer at cbrewer@psu.edu.
 * 5. Products derived from this software may not be called "ColorBrewer", nor may "ColorBrewer" appear in their name, without prior written permission of Cynthia Brewer.
 *
 **/

package edu.tufts.vue.compare.ui;

import java.awt.Color;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import tufts.vue.VueResources;
        

/*
 * Colors.java
 *
 * Created on February 22, 2007, 2:35 PM
 *
 * @author dhelle01
 */
public class Colors {
    
    private List<Color> colors;
    private String name;  
    
   /* public static final Colors one = new Colors("BuGn #5",
                                                new Color(237,248,251),
                                                new Color(178,226,226),
                                                new Color(102,194,164),
                                                new Color(44,162,95),
                                                new Color(0,109,44));
    
    public static final Colors two = new Colors("GnBu #3",
                                                new Color(240,249,232),
                                                new Color(186,228,188),
                                                new Color(123,204,196),
                                                new Color(67,162,202),
                                                new Color(8,104,172));
    
    public static final Colors three = new Colors("PuBu #1",
                                                new Color(241,238,246),
                                                new Color(189,201,225),
                                                new Color(116,169,207),
                                                new Color(43,140,190),
                                                new Color(4,90,141));
    
    public static final Colors four = new Colors("Reds #10",
                                                new Color(254,229,217),
                                                new Color(252,174,145),
                                                new Color(251,106,74),
                                                new Color(222,45,38),
                                                new Color(165,15,21));
    
    public static final Colors five = new Colors("YlOrRd #9",
                                                new Color(255,255,178),
                                                new Color(254,204,92),
                                                new Color(253,141,60),
                                                new Color(240,59,32),
                                                new Color(189,0,38));
    
    public static final Colors six = new Colors("BuPu #8",
                                                new Color(237,248,251),
                                                new Color(179,205,227),
                                                new Color(140,150,198),
                                                new Color(136,86,167),
                                                new Color(129,15,124));
    */
    
    public static final Colors one = new Colors(5,6,1);
    public static final Colors two = new Colors(5,6,2);
    public static final Colors three = new Colors(5,6,3);
    public static final Colors four = new Colors(5,6,4);
    public static final Colors five = new Colors(5,6,5);
    public static final Colors six = new Colors(5,6,6);
    
    public Colors(int intervals,int numColors,int scheme)
    {
        
        System.out.println("merge.weight.colorscheme." + intervals + "." + numColors + "." + scheme);
        name = VueResources.getString("merge.weight.colorscheme." + intervals + "." + numColors + "." + scheme);
        
        colors = new ArrayList();
        
        System.out.println("merge.weight.colors." + intervals + "." + numColors + "." + scheme);
        Color[] arr = VueResources.getColorArray("merge.weight.colors." + intervals + "." + numColors + "." + scheme);
        
        for(int i=0;i<intervals;i++)
        {
            colors.add(arr[i]);
        }
    }
    
    public Colors(String name,List<Color> colors)
    {
       this.colors = colors;
       this.name = name;
    }
    
    public Colors(String name,Color... colors)
    {
       this.colors = Arrays.asList(colors);
       this.name = name;
    }
    
    public List<Color> getColors()
    {
       return colors;
    }
    
    public String getName()
    {
       return name;
    }
    
}
