package tufts.vue;

import java.util.*;
import java.io.File;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.FlatteningPathIterator;
    

public class VueUtil
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    private static String currentDirectoryPath = "";
    
    // Mac OSX Java 1.4.1 has a bug where stroke's are exactly 0.5
    // units off center (down/right from proper center).  You need to
    // draw a minumim stroke on top of a stroke of more than 1 to see
    // this, because at stroke width 1, this looks appears as a policy
    // of drawing strokes down/right to the line. Note that there are
    // other problems with Mac strokes -- a stroke width of 1.0
    // doesn't appear to scale with the graphics context, but any
    // value just over 1.0 will.
    public static boolean StrokeBug05 = false;
   
    static {
        String osName = System.getProperty("os.name");
        String javaSpec = System.getProperty("java.specification.version");

        try {
            javaVersion = Float.parseFloat(javaSpec);
            System.out.println("Java Version: " + javaVersion);
        } catch (Exception e) {
            System.err.println("Couldn't parse java.specifcaion.version: [" + javaSpec + "]");
            System.err.println(e);
        }

        String osn = osName.toUpperCase();
        if (osn.startsWith("MAC")) {
            MacPlatform = true;
            System.out.println("Mac JVM: " + osName);
            System.out.println("Mac mrj.version: " + System.getProperty("mrj.version"));
        } else if (osn.indexOf("WINDOWS") >= 0) {
            WindowsPlatform = true;
            System.out.println("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }
        //if (isMacPlatform()) 
        //  StrokeBug05 = true; // todo: only if mrj.version < 69.1, where they fixed this bug
        if (StrokeBug05)
            System.out.println("Stroke compensation active (0.5 unit offset bug)");
    }

    public static void main(String args[])
        throws java.io.IOException
    {
        System.out.println("cursor16 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(16,16));
        System.out.println("cursor24 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(24,24));
        System.out.println("cursor32 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(32,32));

                                       //.list(System.out);
        Hashtable props = System.getProperties();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            //System.out.println("[1;36m" + key + "[m=" + props.get(key));
            System.out.println(key + "=" + props.get(key));
        }
        
        if (args.length > 0)
            openURL(args[0]);
        else
            test_OpenURL();
        System.exit(0);
    }

    public static void test_OpenURL()
    {
        try {
            openURL("http://hosea.lib.tufts.edu:8080/fedora/get//tufts:7/demo:60/getThumbnail/");
            //openURL("file:///tmp/two words.txt");       // does not work on OSX 10.2 (does now with %20 space replacement)
            //openURL("\"file:///tmp/two words.txt\"");   // does not work on OSX 10.2
            //openURL("\'file:///tmp/two words.txt\'");   // does not work on OSX 10.2
            //openURL("file:///tmp/two%20words.txt");     // works on OSX 10.2, but not Windows 2000
            //openURL("file:///tmp/foo.txt");
            //openURL("file:///tmp/index.html");
            //openURL("file:///tmp/does_not_exist");
            //openURL("file:///zip/About_Developer_Tools.pdf");
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    /*
     * Be sure to compare this value to a constant *float*
     * value -- e.g. "1.4f" -- just "1.4" as double value may
     * actually appear to be less than 1.4, as that can't
     * be exactly represented by a double.
     */
    public static float getJavaVersion()
    {
        return javaVersion;
    }
       

    public static void openURL(String url)
        throws java.io.IOException
    {
        // todo: spawn this in another thread just in case it hangs
        
        // there appears to be no point in quoting the URL...
        String quotedURL;
        if (true || url.charAt(0) == '\'')
            quotedURL = url;
        else
            quotedURL = "\'" + url + "\'";

        //if (isMacPlatform()) quotedURL = "\'" + url + "\'";

        System.err.println("Opening URL [" + quotedURL + "]");
        if (isMacPlatform())
            openURL_Mac(quotedURL);
        else if (isUnixPlatform())
            openURL_Unix(quotedURL);
        else // default is a windows platform
            openURL_Windows(quotedURL);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url)
        throws java.io.IOException
    {
        String cmd = PC_OPENURL_CMD + " " + url;
        System.err.println("Opening PC URL with: [" + cmd + "]");
        Process p = Runtime.getRuntime().exec(cmd);
        if (false) {
            try {
                System.err.println("waiting...");
                p.waitFor();
            } catch (Exception ex) {
                System.err.println(ex);
            }
            System.err.println("exit value=" + p.exitValue());
        }
    }

    private static java.lang.reflect.Method macOpenURL_Method = null;
    private static void openURL_Mac(String url)
        throws java.io.IOException
    {
        url = url.replaceAll(" ", "%20");
        System.err.println("Opening Mac URL: [" + url + "]");
        if (url.indexOf(':') < 0 && !url.startsWith("/")) {
            // OSX won't default to use current directory
            // for a relative reference, so we prepend
            // the current directory manually.
            url = "file://" + System.getProperty("user.dir") + "/" + url;
            System.err.println("Opening Mac URL: [" + url + "]");
        }
        if (getJavaVersion() >= 1.4f) {
            // Can't call this directly because wont compile on the PC
            //com.apple.eio.FileManager.openURL(url);

            if (macOpenURL_Method == null) {
                try {
                    Class macFileManager = Class.forName("com.apple.eio.FileManager");
                    //Class macFileManager = ClassLoader.getSystemClassLoader().loadClass("com.apple.eio.FileManager");
                    macOpenURL_Method = macFileManager.getMethod("openURL", new Class[] { String.class });
                } catch (Exception e) {
                    System.err.println("Failed to find Mac FileManager or openURL method: will not be able to display URL content.");
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager:openURL " + e);
                }
            }
            
            if (macOpenURL_Method != null) {
                try {
                    macOpenURL_Method.invoke(null, new Object[] { url });
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager.openURL " + e);
                }
            } else
                throw new UnsupportedOperationException("openURL_Mac");


        } else {
            throw new UnsupportedOperationException("mac java <= 1.3 openURL unimplemented");
            // put this back if want to suppor mac java 1.3
            // this has been deprecated in mac java 1.4, so
            // just ignore the warning if using a 1.4 or beyond
            // compiler
            //    com.apple.mrj.MRJFileUtils.openURL(url);
        }
        System.err.println("returned from openURL_Mac " + url);
    }

    private static void openURL_Unix(String url)
        throws java.io.IOException
    {
        throw new java.io.IOException("Unimplemented");
    }


    public static boolean isWindowsPlatform()
    {
        return WindowsPlatform;
    }
    public static boolean isMacPlatform()
    {
        return MacPlatform;
    }
    public static boolean isUnixPlatform()
    {
        return UnixPlatform;
    }

    public static boolean isMacAquaLookAndFeel()
    {
        return isMacPlatform() &&
            javax.swing.UIManager.getLookAndFeel().getName().toLowerCase().indexOf("aqua") >= 0;
    }

    public static java.util.Iterator EmptyIterator = new java.util.Iterator() {
            public boolean hasNext() { return false; }
            public Object next() { throw new NoSuchElementException(); }
            public void remove() { throw new UnsupportedOperationException(); }
        };

    // Single element convience iterator
    public static class SingleIterator implements java.util.Iterator {
        Object object;
        public SingleIterator(Object o) {
            object = o;
        }
        public boolean hasNext() { return object != null; }
        public Object next() { if (object == null) throw new NoSuchElementException(); Object o = object; object = null; return o; }
        public void remove() { throw new UnsupportedOperationException(); }
    };
    
    public static class GroupIterator extends java.util.ArrayList
        implements java.util.Iterator
    {
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator() {}
        
        public GroupIterator(Object i1, Object i2)
        {
            this(i1, i2, null);
        }
        public GroupIterator(Object i1, Object i2, Object i3)
        {
            super(3);
            if (i1 == null || i2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            add(i1);
            add(i2);
            if (i3 != null)
                add(i3);
        }

        public boolean add(Object o) {
            if (!(o instanceof Collection) &&
                !(o instanceof Iterator))
                throw new IllegalArgumentException("Can only add Collection or Iterator: " + o);
            return super.add(o);
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= size())
                    return false;
                Object next = get(iterIndex);
                if (next instanceof Iterator)
                    curIter = (Iterator) next;
                else
                    curIter = ((Collection)next).iterator();
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            // make the call on the underlying iterator
            if (curIter.hasNext()) {
                return true;
            } else {
                curIter = null;
                return hasNext();
            }
        }

        public Object next()
        {
            if (curIter == null)
                return null;
            else
                return curIter.next();
        }

        public void remove()
        {
            if (curIter != null)
                curIter.remove();
            else
                throw new IllegalStateException(this + ": no underlying iterator");
        }
    }
    /*
    public static class GroupIterator implements java.util.Iterator
    {
        private Object[] iterables = new Object[4];
        int nIter = 0;
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator(Object l1, Object l2)
        {
            this(l1, l2, null);
        }
        public GroupIterator(Object l1, Object l2, Object l3)
        {
            if (l1 == null || l2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            iterables[nIter++] = l1;
            iterables[nIter++] = l2;
            if (l3 != null)
                iterables[nIter++] = l3;
            for (int i = 0; i < nIter; i++) {
                if (!(iterables[i] instanceof Collection) &&
                    !(iterables[i] instanceof Iterator))
                    throw new IllegalArgumentException("arg i not Collection or Iterator");
            }
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= nIter)
                    return false;
                if (iterables[iterIndex] instanceof Collection)
                    curIter = ((Collection)iterables[iterIndex]).iterator();
                else
                    curIter = (Iterator) iterables[iterIndex];
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            if (!curIter.hasNext()) {
                curIter = null;
                return hasNext();
            }
            return true;
        }

        public Object next()
        {
            if (curIter == null)
                return null;
            else
                return curIter.next();
        }

        public void remove()
        {
            if (curIter != null)
                curIter.remove();
        }
    }
    */
    
    public static void dumpCollection(Collection c) {
        System.out.println("Collection of size: " + c.size() + " (" + c.getClass().getName() + ")");
        dumpIterator(c.iterator());
    }
    public static void dumpIterator(Iterator i) {
        int x = 0;
        while (i.hasNext()) {
            System.out.println((x<10?" ":"") + x + ": " + i.next());
            x++;
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
        File userHome = new File(System.getProperty("user.home"));
        if(userHome == null) 
            userHome = new File(System.getProperty("java.io.tmpdir"));
        File userFolder = new File(userHome.getPath()+File.separatorChar+"vue");
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

    public static void centerOnScreen(java.awt.Window window)
    {
        if (VueUtil.getJavaVersion() >= 1.4f) {
            java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            p.x -= window.getWidth() / 2;
            p.y -= window.getHeight() / 2;
            window.setLocation(p);
        } else {
            java.awt.Dimension d = window.getToolkit().getScreenSize();
            int x = d.width/2 - window.getWidth()/2;
            int y = d.height/2 - window.getHeight()/2;
            window.setLocation(x, y);
        }
    }

    /** a JPanel that anti-aliases text */
    public static class JPanel_aa extends javax.swing.JPanel {
        public JPanel_aa(java.awt.LayoutManager layout) {
            super(layout, true);
        }
        public JPanel_aa() {}

        public void paint(java.awt.Graphics g) {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }
    
    // This is for testing individual components.
    public static void displayComponent(javax.swing.JComponent comp)
    {
        javax.swing.JFrame frame = new javax.swing.JFrame(comp.getClass().getName());
        comp.setSize(comp.getPreferredSize());
        frame.setContentPane(comp);
        frame.pack();
        frame.validate();
        VueUtil.centerOnScreen(frame);
        frame.show();
    }

    static final double sFactor = 0.9;
    public static Color darkerColor(Color c) {
        return factorColor(c, sFactor);
    }
    public static Color brighterColor(Color c) {
        return factorColor(c, 1/sFactor);
    }
    public static Color factorColor(Color c, double factor)
    {
	return new Color((int)(c.getRed()  *factor),
			 (int)(c.getGreen()*factor),
			 (int)(c.getBlue() *factor));
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
        
        boolean m1vertical = (s1x1 == s1x2);
        boolean m2vertical = (s2x1 == s2x2);
        float m1 = Float.NaN;
        float m2 = Float.NaN;
        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        
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

    
    public static final float[] NoIntersection = { Float.NaN, Float.NaN };
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" }; // for debug
    
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape)
    {
        return computeIntersection(rayX1,rayY1, rayX2,rayY2, shape, new float[2]);
    }
    
    /**
     * Compute the intersection of an arbitrary shape and a line segment
     * that is assumed to pass throught the shape.  Usually used
     * with an endpoint (rayX2,rayY2) that ends in the center of the
     * shape, tho that's not required.
     *
     * @return float array of size 2: x & y values of intersection,
     * or ff no intersection, returns Float.NaN values for x/y.
     */
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape,
                                              float[] result)
    {
        java.awt.geom.PathIterator i = shape.getPathIterator(null);
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
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX, endY;
            //if (segType == PathIterator.SEG_CUBICTO) {
            //    endX = seg[4];
            //    endY = seg[5];
            //} else {
                endX = seg[0];
                endY = seg[1];
            //}
            if (cnt > 0 && Line2D.linesIntersect(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                return computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1], result);
              //return computeLineIntersection(rayX2, rayY2, rayX1, rayY1, lastX, lastY, seg[0], seg[1], result);
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

    /**
     * One a line drawn from the center of c1 to the center of c2, compute the the line segment
     * from the intersection at the edge of shape c1 to the intersection at the edge of shape c2.
     */
    
    public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    {
        float segX1 = c1.getCenterX();
        float segY1 = c1.getCenterY();
        float segX2 = c2.getCenterX();
        float segY2 = c2.getCenterY();

        // compute intersection at shape 2 of ray from center of shape 1 to center of shape 2
        //float[] intersection_at_2 = computeIntersection(segX1, segY1, segX2, segY2, c2.getShape());
        // compute intersection at shape 1 of ray from center of shape 2 to center of shape 1
        //float[] intersection_at_1 = computeIntersection(segX2, segY2, segX1, segY1, c1.getShape());

        // compute intersection at shape 1 of ray from center of shape 1 to center of shape 2
        float[] intersection_at_1 = computeIntersection(segX1, segY1, segX2, segY2, c1.getShape());
        // compute intersection at shape 2 of ray from center of shape 2 to center of shape 1
        float[] intersection_at_2 = computeIntersection(segX2, segY2, segX1, segY1, c2.getShape());

        if (intersection_at_1 == NoIntersection) {
            // default to center of component 1
            result.x1 = segX1;
            result.y1 = segY1;
        } else {
            result.x1 = intersection_at_1[0];
            result.y1 = intersection_at_1[1];
        }
        
        if (intersection_at_2 == NoIntersection) {
            // default to center of component 2
            result.x2 = segX2;
            result.y2 = segY2;
        } else {
            result.x2 = intersection_at_2[0];
            result.y2 = intersection_at_2[1];
        }

        //System.out.println("connector: " + out(result));
        //System.out.println("\tfrom: " + c1);
        //System.out.println("\t  to: " + c2);
        
        return result;
    }
    
    public static String out(Object[] o) {
        return Arrays.asList(o).toString();
    }

    public static String out(java.awt.geom.Point2D p) {
        return (float)p.getX() + "," + (float)p.getY();
    }

    public static String out(java.awt.Dimension d) {
        return d.width + "x" + d.height;
    }

    public String out(java.awt.geom.Rectangle2D r) {
        return ""
            + (float)r.getX() + "," + (float)r.getY()
            + " "
            + (float)r.getWidth() + "x" + (float)r.getHeight()
            ;
    }
    public static String out(java.awt.geom.Line2D l) {
        return ""
            + (float)l.getX1() + "," + (float)l.getY1()
            + " -> "
            + (float)l.getX2() + "," + (float)l.getY2()
            ;
    }
    
    
    public static void alert(javax.swing.JComponent component,String message,String title) {
         javax.swing.JOptionPane.showMessageDialog(component,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);                                      
    }
    
}
