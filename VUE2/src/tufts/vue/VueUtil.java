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

package tufts.vue;

import java.io.File;
import java.util.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import javax.swing.*;
import javax.swing.border.*;

/**
 *
 * Various static utility methods for VUE.
 *
 * @version $Revision: 1.101 $ / $Date: 2009-05-14 17:22:58 $ / $Author: brian $
 * @author Scott Fraize
 *
 */
public class VueUtil extends tufts.Util
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueUtil.class);
    
    public static final String DEFAULT_WINDOWS_FOLDER = "vue_2";
    public static final String DEFAULT_MAC_FOLDER = ".vue_2";
    public static final String VueExtension = VueResources.getString("vue.extension", ".vue");
    public static final String VueArchiveExtension = VueResources.getString("vue.archive.extension", ".vpk");
    private static String currentDirectoryPath = "";
    
    public static void openURL(String platformURL)
        throws java.io.IOException
    {
        boolean isMailto = false;
        String logURL = platformURL;
        
        if (platformURL != null && platformURL.startsWith("mailto:")) {
            isMailto = true;
            if (platformURL.length() > 80) {
                // in case there's a big subject or body (e.g, ?subject=Foo&body=Bar in the URL), don't log the whole thing
                logURL = platformURL.substring(0,80) + "...";
            }
            Log.info("openURL[" + logURL + "]");
        } else
            Log.debug("openURL[" + logURL + "]");

        if (isMacPlatform() && VUE.inNativeFullScreen())
            tufts.vue.gui.FullScreen.dropFromNativeToWorking();
        else if (isUnixPlatform() && VUE.inNativeFullScreen())
        		tufts.vue.gui.FullScreen.dropFromNativeToFrame();
        // todo: spawn this in another thread just in case it hangs
        
        if (!isMailto) {
            String lowCaseURL = platformURL.toLowerCase();
                     
            if (lowCaseURL.endsWith(VueExtension) ||
                lowCaseURL.endsWith(VueArchiveExtension) ||
                lowCaseURL.endsWith(".zip") ||
                (DEBUG.Enabled && lowCaseURL.endsWith(".xml")))
            {
                if (lowCaseURL.startsWith("resource:")) {
                    // Special case for startup.vue which can be embedded in the classpath
                    java.net.URL url = VueResources.getURL(platformURL.substring(9));
                    VUE.displayMap(tufts.vue.action.OpenAction.loadMap(url));
                    return;
                }

                final File file = Resource.getLocalFileIfPresent(platformURL);

                if (file != null) {
                    // TODO: displayMap should be changed to take either a URL or a random url/path spec-string,
                    // NOT a local file, as we can open maps at the other end of HTTP url's, and we need an
                    // object that abstracts both.
                    tufts.vue.VUE.displayMap(file);
                } else {
                    final LWMap loadMap = tufts.vue.action.OpenAction.loadMap(new java.net.URL(platformURL));
                    tufts.vue.VUE.displayMap(loadMap);
                    loadMap.setFile(null);
                }
                
                
//                 try {
//                     File file = new File(new java.net.URL(platformURL).getFile());
//                     if(file.exists()) {
//                         tufts.vue.VUE.displayMap(file);
//                     } else{
//                         LWMap loadMap = tufts.vue.action.OpenAction.loadMap(new java.net.URL(platformURL));
//                         tufts.vue.VUE.displayMap(loadMap);
//                         loadMap.setFile(null);
//                     }
//                 } catch (java.net.MalformedURLException e) {
//                     Log.error(e + " " + platformURL);
//                     e.printStackTrace();
//                     try {
//                         tufts.vue.VUE.displayMap(new File(platformURL));
//                     } catch (Exception ex) {
//                         System.out.println(ex + " " + platformURL);
//                         tufts.Util.openURL(platformURL);
//                     }
//                 } catch(Exception ex) {
//                     ex.printStackTrace();
//                 }
                return;
            }
        }

        if (VUE.isApplet()) {
            java.net.URL url = null;
            try {
                url = new java.net.URL(platformURL);
                System.out.println("Applet URL display: " + url);
                VUE.getAppletContext().showDocument(url, "_blank");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            // already handled in Util.openURL
            //if (isMacPlatform() && platformURL.startsWith("/"))
            //    platformURL = "file:" + platformURL;
                
            tufts.Util.openURL(platformURL);
        }
    }
    
    public static void  setCurrentDirectoryPath(String cdp) {
        currentDirectoryPath = cdp;
    }
    
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }    
    
    public static boolean isCurrentDirectoryPathSet() {
        if(currentDirectoryPath.equals("")) 
            return false;
        else
            return true;
    }

    public static File getDefaultUserFolder() {
        File userHome = new File(VUE.getSystemProperty("user.home"));
        if(userHome == null) 
            userHome = new File(VUE.getSystemProperty("java.io.tmpdir"));
        final String vueUserDir = isWindowsPlatform() ? DEFAULT_WINDOWS_FOLDER : DEFAULT_MAC_FOLDER;
        File userFolder = new File(userHome.getPath() + File.separatorChar + vueUserDir);
        if(userFolder.isDirectory())
            return userFolder;
        if(!userFolder.mkdir())
            throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be created");
        return userFolder;
    }
    
    public static void deleteDefaultUserFolder() {
        File userFolder = getDefaultUserFolder();
        File[] files = userFolder.listFiles();
        System.out.println("file count = "+files.length);
        for(int i = 0; i<files.length;i++) {
            if(files[i].isFile() && !files[i].delete()) 
                throw new RuntimeException(files[i].getAbsolutePath()+":cannot be created");
        }
        if(!userFolder.delete()) 
             throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be deleted");
    }

    public static void copyURL(java.net.URL url, java.io.File file)
        throws java.io.IOException
    {
        if (DEBUG.IO) out("VueUtil: copying " + url + " to " + file);
        copyStream(url.openStream(), new java.io.FileOutputStream(file));
    }
        
    public static void copyStream(java.io.InputStream in, java.io.OutputStream out)
        throws java.io.IOException
    {
        int len = 0;
        byte[] buf = new byte[8192];
        while ((len = in.read(buf)) != -1) {
            if (DEBUG.IO) out("VueUtil: copied " + len + " to " + out);
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }



    /**
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */

    public static float[] computeLineIntersection
        (float s1x1, float s1y1, float s1x2, float s1y2,
         float s2x1, float s2y1, float s2x2, float s2y2, float[] result)
    {
        // We are defining a line here using the formula:
        // y = mx + b  -- m is slope, b is y-intercept (where crosses x-axis)
        
        final boolean m1vertical = (s1x1 == s1x2);
        final boolean m2vertical = (s2x1 == s2x2);
        final float m1;
        final float m2;

        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        else
            m1 = Float.NaN;
        
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        else
            m2 = Float.NaN;
        
        // Solve for b using any two points from each line.
        // to solve for b:
        //      y = mx + b
        //      y + -b = mx
        //      -b = mx - y
        //      b = -(mx - y)
        // float b1 = -(m1 * s1x1 - s1y1);
        // float b2 = -(m2 * s2x1 - s2y1);
        // System.out.println("m1=" + m1 + " b1=" + b1);
        // System.out.println("m2=" + m2 + " b2=" + b2);

        // if EITHER line is vertical, the x value of the intersection
        // point will obviously have to be the x value of any point
        // on the vertical line.
        
        float x = 0;
        float y = 0;
        if (m1vertical) {   // first line is vertical
            //System.out.println("setting X to first vertical at " + s1x1);
            float b2 = -(m2 * s2x1 - s2y1);
            x = s1x1; // set x to any x point from the first line
            // using y=mx+b, compute y using second line
            y = m2 * x + b2;
        } else {
            float b1 = -(m1 * s1x1 - s1y1);
            if (m2vertical) { // second line is vertical (has no slope)
                //System.out.println("setting X to second vertical at " + s2x1);
                x = s2x1; // set x to any point from the second line
            } else {
                // second line has a slope (is not veritcal: m is valid)
                float b2 = -(m2 * s2x1 - s2y1);
                x = (b2 - b1) / (m1 - m2);
            }
            // using y=mx+b, compute y using first line
            y = m1 * x + b1;
        }
        //System.out.println("x=" + x + " y=" + y);

        result[0] = x;
        result[1] = y;
        return result;
    }

    public static final float[] NoIntersection = { Float.NaN, Float.NaN, Float.NaN, Float.NaN };
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" }; // for debug
    
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape, java.awt.geom.AffineTransform shapeTransform)
    {
        return computeIntersection(rayX1,rayY1, rayX2,rayY2, shape, shapeTransform, new float[2], 1);
    }
    
    public static Point2D.Float computeIntersection(Line2D.Float l, LWComponent c) {
        float[] p = computeIntersection(l.x1, l.y1, l.x2, l.y2, c.getZeroShape(), c.getZeroTransform(), new float[2], 1);
        return new Point2D.Float(p[0], p[1]);
    }
    public static float[] computeIntersection(float segX1, float segY1, float segX2, float segY2, LWComponent c) {
        return computeIntersection(segX1, segY1, segX2, segY2, c.getZeroShape(), c.getZeroTransform(), new float[2], 1);
    }


    /**
     * Compute the intersection of an arbitrary shape and a line segment
     * that is assumed to pass throught the shape.  Usually used
     * with an endpoint (rayX2,rayY2) that ends in the center of the
     * shape, tho that's not required.
     *
     * @param max - max number of intersections to compute. An x/y
     * pair of coords will put into result up to max times. Must be >= 1.
     *
     * @return float array of size 2: x & y values of intersection,
     * or ff no intersection, returns Float.NaN values for x/y.
     */
    public static float[] computeIntersection(float segX1, float segY1,
                                              float segX2, float segY2,
                                              java.awt.Shape shape, java.awt.geom.AffineTransform shapeTransform,
                                              float[] result, int max)
    {
        java.awt.geom.PathIterator i = shape.getPathIterator(shapeTransform);
        // todo performance: if this shape has no curves (CUBICTO or QUADTO)
        // this flattener is redundant.  Also, it would be faster to
        // actually do the math for arcs and compute the intersection
        // of the arc and the line, tho we can save that for another day.
        i = new java.awt.geom.FlatteningPathIterator(i, 0.5);
        
        float[] seg = new float[6];
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        int cnt = 0;
        int hits = 0;
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX = seg[0];
            float endY = seg[1];
                
            // at cnt == 0, we have only the first point from the path iterator, and so no line yet.
            if (cnt > 0 && Line2D.linesIntersect(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                if (max <= 1) {
                    return computeLineIntersection(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1], result);
                } else {
                    float[] tmp = computeLineIntersection(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1], new float[2]);
                    result[hits*2 + 0] = tmp[0];
                    result[hits*2 + 1] = tmp[1];
                    if (++hits >= max)
                        return result;
                }
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

    /** compute the first two y value crossings of the given x_axis and shape */
    public static float[] computeYCrossings(float x_axis, Shape shape, float[] result) {
        return computeIntersection(x_axis, Integer.MIN_VALUE, x_axis, Integer.MAX_VALUE, shape, null, result, 2);
    }
    
    /** compute 2 y values for crossings of at x_axis, and store result in the given Line2D */
    public static Line2D computeYCrossings(float x_axis, Shape shape, Line2D result) {
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        result.setLine(x_axis, coords[1], x_axis, coords[3]);
        return result;
    }
    
    /**
     * This will clip the given vertical line to the edges of the given shape.
     * Assumes line start is is min y (top), line end is max y (bottom).
     * @param line - line to clip y values if outside edge of given shape
     * @param shape - shape to clip line to
     * @param pad - padding: keep line endpoints at least this many units away from shape edge
     *
     * todo: presumes only 2 crossings: will only handle concave polygons
     * Should be relatively easy to extend this to work for non-vertical lines if the need arises.
     */
    public static Line2D clipToYCrossings(Line2D line, Shape shape, float pad)
    {
        float x_axis = (float) line.getX1();
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        // coords[0] & coords[2], the x values, can be ignored, as they always == x_axis

        if (coords.length < 4) {
            // TODO FIX: if line is outside edge of shape, we're screwed (see d:/test-layout.vue)
            // TODO: we were getting this of NoIntersection being returned (which was only of size
            // 2, and thus give us array bounds exceptions below) -- do we need to do anything
            // here to make sure the NoIntersection case is handled more smoothly?
            System.err.println("clip error " + coords);
            new Throwable("CLIP ERROR shape=" + shape).printStackTrace();
            return null;
        }

        float upper; // y value at top
        float lower; // y value at bottom
        if (coords[1] < coords[3]) {
            // cross1 is min cross (top), cross2 is max cross (bottom)
            upper = coords[1];
            lower = coords[3];
        } else {
            // cross2 is min cross (top), cross1 is max cross (bottom)
            upper = coords[3];
            lower = coords[1];
        }
        upper += pad;
        lower -= pad;
        // clip line to upper & lower (top & bottom)
        float y1 = Math.max(upper, (float) line.getY1());
        float y2 = Math.min(lower, (float) line.getY2());
        line.setLine(x_axis, y1, x_axis, y2);
        return line;
    }

    /** clip the given amount of length off each end of the given line -- negative values will extend the line length */
    public static Line2D.Float clipEnds(final Line2D.Float line, final double clipLength)
    {
        final double rise = line.y1 - line.y2; // delta Y
        final double run = line.x1 - line.x2; // delta X
        final double slope = run / rise; // inverse slope is what works here: due to +y is down in coord system?
        final double theta = Math.atan(slope);
        final double clipX = Math.sin(theta) * clipLength;
        final double clipY = Math.cos(theta) * clipLength;

        if (DEBUG.PATHWAY) {
            out("\nLine: " + fmt(line) + " clipping lenth off ends: " + clipLength);
            out(String.format("XD %.1f YD %.1f Slope %.1f Theta %.2f  clipX %.1f clipY %.1f", run, rise, slope, theta, clipX, clipY));
        }

        if (line.y1 < line.y2) {
            line.x1 += clipX;
            line.x2 -= clipX;
            line.y1 += clipY;
            line.y2 -= clipY;
        } else {
            line.x1 -= clipX;
            line.x2 += clipX;
            line.y1 -= clipY;
            line.y2 += clipY;
        }

        return line;
    }
    

    public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    {
        computeConnectorAndCenterHit(c1, c2, result);
        return result;
    }
    
    
    //public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    /**
     * On a line drawn from the center of c1 to the center of c2, compute the the line segment
     * from the intersection at the edge of shape c1 to the intersection at the edge of shape c2.
     * The returned line will be in the LWMap coordinate space.  If the components overlap sufficiently,
     * the segment returned will either be from the center of one component to the edge of the other,
     * or from center-to-center.
     *
     * @param result: this line will be set to connecting segment
     * @return true if the components overlapped in such a way as to cause the segment to connect at one or
     * or both of the component centers, as opposed to their edges
     */
    public static boolean computeConnectorAndCenterHit(LWComponent head, LWComponent tail, Line2D.Float result)
    {
        // TODO: do these defaults still want to be the map-center now that we do
        // relative coords and parent-local links?  Shouldn't they be the center
        // relative to some desired parent focal? (e.g. a link parent)
        
        final float headX = head.getMapCenterX();
        final float headY = head.getMapCenterY();
        final float tailX = tail.getMapCenterX();
        final float tailY = tail.getMapCenterY();

        // compute intersection at head shape of line from center of head to center of tail shape
        final float[] intersection_at_1 = computeIntersection(headX, headY, tailX, tailY, head);

        boolean overlap = false;

        if (intersection_at_1 == NoIntersection) {
            // default to center of component 1
            result.x1 = headX;
            result.y1 = headY;
            overlap = true;
        } else {
            result.x1 = intersection_at_1[0];
            result.y1 = intersection_at_1[1];
        }
        
        // compute intersection at tail shape of line from prior intersection to center of tail shape
        final float[] intersection_at_2 = computeIntersection(result.x1, result.y1, tailX, tailY, tail);

        if (intersection_at_2 == NoIntersection) {
            // default to center of component 2
            result.x2 = tailX;
            result.y2 = tailY;
            overlap = true;
        } else {
            result.x2 = intersection_at_2[0];
            result.y2 = intersection_at_2[1];
        }
        
        return overlap;
    }

// Old version: could produce "internal" connections if nodes overlapped: directionality of the connector
// would get reversed. E.g., connector would be from the edge of a node back towards it's own center,
// to connect the outer edge of an overlapping node.
    
//     public static boolean computeConnectorAndCenterHit(LWComponent c1, LWComponent c2, Line2D.Float result)
//     {
//         // TODO: do these defaults still want to be the map-center now that we do
//         // relative coords and parent-local links?  Shouldn't they be the center
//         // relative to some desired parent focal? (e.g. a link parent)
        
//         final float segX1 = c1.getMapCenterX();
//         final float segY1 = c1.getMapCenterY();
//         final float segX2 = c2.getMapCenterX();
//         final float segY2 = c2.getMapCenterY();

//         // compute intersection at shape 1 of line from center of shape 1 to center of shape 2
//         final float[] intersection_at_1 = computeIntersection(segX1, segY1, segX2, segY2, c1);
//         // compute intersection at shape 2 of line from center of shape 2 to center of shape 1
//         final float[] intersection_at_2 = computeIntersection(segX2, segY2, segX1, segY1, c2);

//         boolean overlap = false;

//         if (intersection_at_1 == NoIntersection) {
//             // default to center of component 1
//             result.x1 = segX1;
//             result.y1 = segY1;
//             overlap = true;
//         } else {
//             result.x1 = intersection_at_1[0];
//             result.y1 = intersection_at_1[1];
//         }
        
//         if (intersection_at_2 == NoIntersection) {
//             // default to center of component 2
//             result.x2 = segX2;
//             result.y2 = segY2;
//             overlap = true;
//         } else {
//             result.x2 = intersection_at_2[0];
//             result.y2 = intersection_at_2[1];
//         }

//         //System.out.println("connector: " + out(result));
//         //System.out.println("\tfrom: " + c1);
//         //System.out.println("\t  to: " + c2);
        
//         return overlap;
//     }
    

    public static double computeVerticalRotation(Line2D l) {
        return computeVerticalRotation(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }
    
    /**
     * Compute the rotation needed to normalize the line segment to vertical orientation, making it
     * parrallel to the Y axis.  So vertical lines will return either 0 or Math.PI (180 degrees), horizontal lines
     * will return +/- PI/2.  (+/- 90 degrees).  In the rotated space, +y values will move down, +x values will move right.
     */
    public static double computeVerticalRotation(double x1, double y1, double x2, double y2)
    {
        final double xdiff = x1 - x2;
        final double ydiff = y1 - y2;
        final double slope = xdiff / ydiff; // really, inverse slope
        double radians = -Math.atan(slope);

        if (xdiff >= 0 && ydiff >= 0)
            radians += Math.PI;
        else if (xdiff <= 0 && ydiff >= 0)
            radians -= Math.PI;

        return radians;
    }

    /**
     * Move a point a given distance along a line parallel to the
     * ray implied by the the given line.  The direction of projection
     * is parallel to the ray that begins at the first point in the line,
     * and passes through the second point of the line.  The start point
     * does not need to be on the given line.
     * 
     * @return the new point
     */

    public static Point2D projectPoint(float x, float y, Line2D ray, float distance) {

        // todo: this impl could be much simpler

        final Point2D.Float p = new Point2D.Float();

        final double rotation = computeVerticalRotation(ray);

        final java.awt.geom.AffineTransform tx = new java.awt.geom.AffineTransform();

        tx.setToTranslation(x, y);
        tx.rotate(rotation);
        tx.translate(0,distance);
        tx.transform(p,p);

        return p;

        
    }
    
    public static Point2D projectPoint(Point2D.Float p, Line2D ray, float distance) {
        return projectPoint(p.x, p.y, ray, distance);
    }

    
    public static void dumpBytes(String s) {
        try {
            dumpBytes(s.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dumpBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.println("byte " + (i<10?" ":"") + i
                               + " (" + ((char)b) + ")"
                               + " " + pad(' ', 4, new Byte(b).toString())
                               + "  " + pad(' ', 2, Integer.toHexString( ((int)b) & 0xFF))
                               + "  " + pad('X', 8, toBinary(b))
                               );
        }
    }
    
    public static String toBinary(byte b) {
        StringBuffer buf = new StringBuffer(8);
        buf.append((b & (1<<7)) == 0 ? '0' : '1');
        buf.append((b & (1<<6)) == 0 ? '0' : '1');
        buf.append((b & (1<<5)) == 0 ? '0' : '1');
        buf.append((b & (1<<4)) == 0 ? '0' : '1');
        buf.append((b & (1<<3)) == 0 ? '0' : '1');
        buf.append((b & (1<<2)) == 0 ? '0' : '1');
        buf.append((b & (1<<1)) == 0 ? '0' : '1');
        buf.append((b & (1<<0)) == 0 ? '0' : '1');
	return buf.toString();
    }
    
    public static void dumpString(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int cv = (int) chars[i];
            System.out.println("char " + (i<10?" ":"") + i
                               + " (" + chars[i] + ")"
                               + " " + pad(' ', 6, new Integer(cv).toString())
                               + " " + pad(' ', 4, Integer.toHexString(cv))
                               + "  " + pad('0', 16, Integer.toBinaryString(cv))
                               );
        }
    }
    

    
    public static Map getQueryData(String query) {
        String[] pairs = query.split("&");
        Map map = new HashMap();
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            if (DEBUG.DATA || DEBUG.IMAGE) System.out.println("query pair " + pair);
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                String key = pair.substring(0, eqIdx);
                String value = pair.substring(eqIdx+1, pair.length());
                map.put(key.toLowerCase(), value);
            }
        }
        return map;
    }

    public static boolean isTransparent(Color c) {
        return c == null || c.getAlpha() == 0;
    }
    public static boolean isTranslucent(Color c) {
        return c == null || c.getAlpha() != 0xFF;
    }
    
    public static void alert(JComponent parent, String message, String title) {
        VOptionPane.showMessageDialog(parent,
                                      message,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));                                      
    }
    
    public static void alert(Container parent, String message, String title) {
        VOptionPane.showMessageDialog(parent,
                                      message,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));                                      
    }

    public static void alert(Container parent, String message, String title, int messageType) {
        VOptionPane.showMessageDialog(parent,
                                      message,
                                      title,
                                      messageType,
                                      null);
	}

    public static void alert(Component parent, String message, String title, int messageType) {
        VOptionPane.showMessageDialog(parent,
                                      message,
                                      title,
                                      messageType,
                                      null);
	}

    public static void alert(String title, Throwable t) {

        java.io.Writer buf = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(buf));
        JComponent msg = new JTextArea(buf.toString());
        msg.setOpaque(false);
        msg.setFont(new Font("Lucida Grande", Font.PLAIN, 9));

        VOptionPane.showMessageDialog(VUE.getDialogParent(),
                                      msg,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));
        
    }
   
    public static void alert(String message, String title) {
        VOptionPane.showMessageDialog(VUE.getDialogParent(),
                                      message,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));                                      
    }
   
    public static int confirm(String message, String title) {
       return VOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                            message,
                                            title,
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(JComponent parent, String message, String title) {
        return VOptionPane.showConfirmDialog(parent,
                                             message,
                                             title,
                                             JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE,
                                             VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(Container parent, String message, String title, int optionType) {
        return VOptionPane.showConfirmDialog(parent,
                                             message,
                                             title,
                                             optionType,
                                             JOptionPane.QUESTION_MESSAGE,
                                             VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(Container parent, String message, String title, int optionType, int messageType) {
        return VOptionPane.showConfirmDialog(parent,
                                             message,
                                             title,
                                             optionType,
                                             messageType,
                                             null);
    }
}


/**
 * VOptionPane extends JOptionPane for the sole purpose of returning MAX_LINE_LENGTH
 * from getMaxCharactersPerLineCount() so that long messages will wrap.
 */
class VOptionPane extends JOptionPane
{
	static final long	serialVersionUID = 1;
	static final int	MAX_LINE_LENGTH = 80;

	VOptionPane() {
	}

	public int getMaxCharactersPerLineCount() {
		return MAX_LINE_LENGTH;
	}

	static void showMessageDialog(Container parent, Object message, String title, int messageType, Icon icon) {
		showConfirmDialog(parent, message, title, JOptionPane.DEFAULT_OPTION, messageType, icon);
	}

	static int showConfirmDialog(Container parent, Object message, String title, int optionType, int messageType, Icon icon)
		throws HeadlessException {
		VOptionPane		optionPane = new VOptionPane();

		optionPane.setMessage(message);
		optionPane.setOptionType(optionType);
		optionPane.setMessageType(messageType);
		optionPane.setIcon(icon);
		optionPane.setComponentOrientation((parent != null ? parent : getRootFrame()).getComponentOrientation());

		JDialog			dialog = optionPane.createDialog(parent, title);

		dialog.setVisible(true);

		Object			selectedValue = optionPane.getValue();

		return (selectedValue == null || !(selectedValue instanceof Integer) ?
			CLOSED_OPTION : ((Integer)selectedValue).intValue());
	}
}
