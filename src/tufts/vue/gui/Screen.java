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
package tufts.vue.gui;

import tufts.Util;
import java.awt.*;

/**
 * The Screen class encapuslates information about a single
 * screen/display.  E.g., a computer with two monitors connected
 * and powered up should return Screen[2] from getAllScreens().
 *
 * Currently does not provide for any handling of HeadlessException's
 *
 * @version $Revision: 1.3 $ / $Date: 2010-02-03 19:15:46 $ / $Author: mike $
 * @author Scott Fraize
 */
public class Screen
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Screen.class);

    /** normally 0,0 for single display systems, but could be anything if there are multiple displays */
    public final int x, y;
    /** logical pixel width and height irrespective of any operating system software margins (e.g., insets for docks, etc) */
    public final int width, height;
    /** The same bounds expressed by x,y,width,height, but as insets. top always == y, left always == x */
    public final int top, left, bottom, right;
    /** The relative pixel insets as determined by Toolkit.getScreenInsets(GraphicsConfiguration)
     * Note that the members of Insets are mutable, but doing so will have no effect except to make toString() lie */
    public final Insets margin;
    /** convience totals: topIn = x + margin.top, leftIn = x + margin.left, rightIn = right - margin.right, etc */
    public final int topIn, leftIn, bottomIn, rightIn;

    /** normally the relevant device, but could be null for user-created logical screens */
    public final GraphicsDevice device;

    /** convenience call to java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment() */
    public static GraphicsEnvironment genv() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment();
    }
    
    public static Screen[] getAllScreens() {
        final GraphicsDevice[] devices = genv().getScreenDevices();
        final Screen[] screens = new Screen[devices.length];
        for (int i = 0; i < devices.length; i++) {
            screens[i] = Screen.create(devices[i]);
        }
        return screens;
    }

    public static GraphicsDevice getDefaultDevice() {
        return genv().getDefaultScreenDevice();
    }

    /** If the given screen # exists, return it, otherwise return null */
    public static Screen getScreen(int n) {
        final GraphicsDevice[] devices = genv().getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            if (i == n)
                return Screen.create(devices[i]);
        }
        return null;
    }

    /** @return the GraphicsDevice the given window is currently displayed on.
     *
     * In the case where the Window currently overlaps two physical or logical devices,
     * the device the window is "on" is determined by the device which is displaying the
     * greatest portion of the total area of the Window
     *
     * If the given Window is null, this return the default device.
     */
    
    public static GraphicsDevice getDeviceForWindow(Window w) 
    {
        if (w == null)
            return getDefaultDevice();
        
        final GraphicsDevice[] devices = genv().getScreenDevices();

        if (devices.length < 2)
            return getDefaultDevice();
        
        return getDeviceForRegion(devices, w.getBounds());
    }
    
    /** @return the GraphicsDevice the given region is currently displayed on.
     *
     * In the case where the region currently overlaps two physical or logical devices,
     * the device the region is "on" is determined by the device which is displaying the
     * greatest portion of the total area of the region.
     *
     * If the given region is null or empty, this returns the default device.
     */
    public static GraphicsDevice getDeviceForRegion(final Rectangle region) 
    {
        if (region == null || region.isEmpty())
            return getDefaultDevice();
        else
            return getDeviceForRegion(genv().getScreenDevices(), region);
    }
    
    private static GraphicsDevice getDeviceForRegion(final GraphicsDevice[] devices, final Rectangle region) 
    {
        if (devices.length < 2 || region == null || region.isEmpty())
            return getDefaultDevice();

        GraphicsDevice selected = null;
        
        // scan all screen devices, and find the one that this
        // window most overlaps:
        
        int maxArea = 0;
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            try {
                GraphicsConfiguration config = device.getDefaultConfiguration();
                Rectangle overlap = config.getBounds().intersection(region);
                int area = overlap.width * overlap.height;
                if (area > maxArea) {
                    maxArea = area;
                    selected = device;
                }
            } catch (Throwable t) {
                Log.error("scanning device: " + Util.tags(device));
            }
        }
        
        return selected == null ? getDefaultDevice() : selected;
    }

    /** @return the device the given point is on, or the default device if not on any screen */
    public static GraphicsDevice getDeviceForPoint(final Point point)
    {
        if (point == null)
            return getDefaultDevice();
        
        final GraphicsDevice[] devices = genv().getScreenDevices();

        if (devices.length < 2)
            return getDefaultDevice();

        // scan all screen devices, and find the one that this point is on:
        
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            try {
                GraphicsConfiguration config = device.getDefaultConfiguration();
                if (config.getBounds().contains(point))
                    return device;
            } catch (Throwable t) {
                Log.error("scanning device: " + Util.tags(device));
            }
        }
        
        return getDefaultDevice();
    }

    public static Screen getScreenForPoint(final Point point) {
        return Screen.create(getDeviceForPoint(point));
    }

    public static Screen getScreenForWindow(Window w) {
        return Screen.create(getDeviceForWindow(w));
    }
    
    public static Screen create(GraphicsDevice device)
    {
        // note: the config changes when the display mode changes (e.g., resolution change)
        final GraphicsConfiguration config = device.getDefaultConfiguration(); 

        // note: the screen insets may change at any time due to user changes (e.g., dock hiding)
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);

        return new Screen(config.getBounds(), insets, device);
    }

    /** create's a purely logical screen definition w/out a device */
    public static Screen create(Rectangle bounds, Insets insets) {
        return new Screen(bounds, insets, null);
    }
    
    /** @param device may be null -- only needed for makeWindowFullScreen, otherwise provided as pass-through information */
    Screen(final Rectangle b, final Insets insets, final GraphicsDevice device) {
        this.device = device;
            
        this.width = b.width;
        this.height = b.height;
            
        this.top = this.y = b.y;
        this.left = this.x = b.x;
        this.bottom = top + height;
        this.right = left + width;
            
        this.topIn = top + insets.top;
        this.leftIn = left + insets.left;
        this.bottomIn = bottom - insets.bottom;
        this.rightIn = right - insets.right;

        this.margin = insets;
    }

    public GraphicsDevice getDevice() {
        return device;
    }

    public void makeWindowCentered(Window w) {
        w.setLocation(x + (width - w.getWidth()) / 2,
                      y + (height - w.getHeight()) / 2);
                               
    }
    
    /** uses getMaxWindowBounds */
    public void makeWindowMaximized(Window w) {
        w.setBounds(getMaxWindowBounds());
    }
    
    /** uses getVisibleBounds */
    public void makeWindowFillVisible(Window w) {
        w.setBounds(getVisibleBounds());
    }
    
    /** uses native full-screen mode if available (e.g., allows menu-bar hiding on the mac, and usually blanks other screens) */
    public void makeWindowFullScreen(Window w) {
        device.setFullScreenWindow(w);
    }
        
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    /** @return the bounds as insets offset in the global space of all screens */
    public Insets getAsInsets() {
        return new Insets(top, left, bottom, right);
    }
        
    /** @return the bounds of the maximum sized window that will not overlap any screen insets (e.g., menu bars, docks, etc) */
    public Rectangle getMaxWindowBounds() {
        return new Rectangle(leftIn, // x + insets.left
                             topIn, // y + insets.top
                             rightIn - leftIn, // width - (insets.left + insets.right)
                             bottomIn - topIn); // height - (insets.top + insets.bottom)
    }
    
    /** for now, this only works to create a window that doesn't get clipped by the mac menu bar, but fills the rest of the screen */
    public Rectangle getVisibleBounds() {
        return new Rectangle(left,
                             topIn, // y + insets.top
                             width,
                             height - (topIn-top)); // margin contents are exposed as mutable, so we re-compute margin.top
    }
    
    // todo: the below atXXX/inXXX could be smarter by accounting for
    // other nearby screens e.g., if off-screen at any side, and there
    // isn't a screen off to the side, those coordinates could be
    // considered to be "at" that side, which would be useful for many
    // cases.  E.g., "atTop" in a vanilla single-screen setup could be
    // true for checking the y coordinate -1, but false if there was a
    // screen above it.

    public boolean atTop(int y) { return y == top; }
    public boolean atLeft(int x) { return x == left; }
    public boolean atBottom(int y) { return y == bottom; }
    public boolean atRight(int x) { return x == right; }

    public boolean inTop(int y) { return y >= top && y <= topIn; }
    public boolean inLeft(int x) { return x >= left && x <= leftIn; }
    public boolean inBottom(int y) { return y <= bottom && y >= bottomIn; }
    public boolean inRight(int x) { return x <= right && x >= rightIn; }

    @Override public String toString() {
        StringBuilder s = new StringBuilder
            (String.format("Screen[%dx%d @ %d,%d %s", width, height, x, y, (device==null?"n/a":Util.quote(device.getIDstring()))));
        // warning: margin is exposed as mutable -- it's possible for this report to be incorrect as to actual behavior
        if (margin.hashCode() == 0) {
            s.append(']');
        } else {
            if (margin.top != 0)
                s.append(" top="+margin.top);
            else
                s.append(' ');
            if (margin.left != 0) s.append(",left="+margin.left);
            if (margin.bottom != 0) s.append(",bottom="+margin.bottom);
            if (margin.right != 0) s.append(",right="+margin.right);
            s.append(']');
        }
        return s.toString();
    }
        
    /** @return the smallest single rectangle that when filled would
     * fill ALL attached displays.  If the displays do not have have
     * perfect logical alignment and compatible sizes, then some
     * regions of the rectangle will actually be off-screen (never
     * visible).  This can be used to size a singe window to cover all
     * displays.
     */
    public static Rectangle getSpaceBounds() {
        return getAllDeviceBounds(genv().getScreenDevices());
    }

    // todo: add something like getSpaceFit that returns the largest
    // rectangle that does NOT leave anything off screen.

    /** @see getSpaceBounds */
    public static Rectangle getAllDeviceBounds(GraphicsDevice[] devices)
    {
        Rectangle bounds = null;
        
        for (int i = 0; i < devices.length; i++) {
            final GraphicsDevice device = devices[i];
            final GraphicsConfiguration config = device.getDefaultConfiguration();
            final Rectangle newBounds = config.getBounds();
            if (bounds == null) {
                bounds = new Rectangle(newBounds);
            } else {
                bounds.add(newBounds);
            }
        }
        return bounds;
    }

    public static Insets boundsToInsets(Rectangle r) 
    {
        return new Insets(r.y,
                          r.x,
                          r.y + r.height,
                          r.x + r.width);

    }

    public static void dumpOut() {
        dump(System.out);
    }

    public static void dump(java.io.PrintStream ps) {
        Rectangle sb = getSpaceBounds();
        ps.println(Screen.class + " report:"
                   + "\n\t environment: " + Util.tags(genv())
                   + "\n\tenviroMaxWin: " + genv().getMaximumWindowBounds()
                   + "\n\t   allBounds: " + sb
                   + "\n\t    asInsets: " + boundsToInsets(sb)
                   );

        Screen[] screens = getAllScreens();
        for (int i = 0; i < screens.length; i++) {
            ps.format("\tscreen %d: %s max(%s)\n", i, screens[i], Util.fmt(screens[i].getMaxWindowBounds()));
        }
    }
}
