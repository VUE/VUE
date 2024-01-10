/*
 * Layout.java
 *
 * Created on August 6, 2008, 1:08 PM
 *
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
/**
 *
 * @author akumar03
 */
package edu.tufts.vue.layout;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.dataset.*;

public abstract class Layout {

    public double MAP_SIZE = 500;
    public int MAX_SIZE = 5000;
    public static final double X_COL_SIZE = VueResources.getDouble("layout.node_width");
    public static final double Y_COL_SIZE = VueResources.getDouble("layout.node_height");
    public static final double X_SPACING = VueResources.getDouble("layout.x_spacing");
    public static final double Y_SPACING = VueResources.getDouble("layout.y_spacing");

    /** Creates a new instance of Layout */
    public Layout() {
    }

    public abstract LWMap createMap(Dataset ds, String mapName) throws Exception;

    public abstract void layout(LWSelection s) throws Exception;
}
