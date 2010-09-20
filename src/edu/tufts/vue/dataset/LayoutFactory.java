/*
 * LayoutFactory.java
 *
 * Created on July 15, 2008, 6:17 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.dataset;

import java.io.*;
import java.util.*;

public class LayoutFactory {
    
    private static LayoutFactory factory = null;
    /** Creates a new instance of LayoutFactory */
    protected LayoutFactory() {
    }
    
    public static  LayoutFactory getInstance() {
        if(factory == null) {
            factory = new LayoutFactory();
        }
        return factory;
    }
    
    public List<AbstractLayout> getAvailableLayouts() {
        List<AbstractLayout> list = new ArrayList<AbstractLayout>();
        list.add(new RandomLayout());
        list.add(new CircularLayout());
        list.add(new GravitationalLayout());
        list.add(new HierarchicalLayout());
        list.add(new TabularLayout());
        list.add(new DoubleBipartiteLayout());
        list.add(new DoubleCircularLayout());
        return list;
    }
}
