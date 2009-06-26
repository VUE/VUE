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


package tufts.vue.gui;

import tufts.Util;
import tufts.vue.Resource;
import tufts.vue.EventRaiser;
import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.DEBUG;

import java.util.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.border.*;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

/**
 * Various constants for GUI variables and static method helpers.
 *
 * @version $Revision: 1.145 $ / $Date: 2009-06-26 21:35:02 $ / $Author: sfraize $
 * @author Scott Fraize
 */

// todo: move most of VueConstants to here
public class GUI
    implements tufts.vue.VueConstants/*, java.beans.PropertyChangeListener*/
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(GUI.class);

    /** special property key for sending a finalize message to JComponents */
    public static final String FINALIZE = "GUI_FINALIZE";
    
    public static Font LabelFace; 
    public static Font ValueFace;
    public static Font TitleFace;
    public static Font FixedFace;
    public static Font ErrorFace;
    public static Font StatusFace;
    public static Font DataFace;
    public static Font ContentFace;
    
    public static final Color LabelColor = new Color(61,61,61);
    public static final int LabelGapRight = 6;
    public static final int FieldGapRight = 6;
    public static final Insets WidgetInsets = new Insets(8,6,8,6);
    public static final Insets WidgetInsets2 = new Insets(16,12,16,12);
    public static final Insets WidgetInsets3 = new Insets(20,20,20,20);
    public static final Border WidgetInsetBorder = DEBUG.BOXES
        ? new MatteBorder(WidgetInsets, Color.yellow)
        : new EmptyBorder(WidgetInsets);
    public static final Border WidgetInsetBorder2 = DEBUG.BOXES
        ? new MatteBorder(WidgetInsets2, Color.yellow)
        : new EmptyBorder(WidgetInsets2);
    public static final Border WidgetInsetBorder3 = DEBUG.BOXES
        ? new MatteBorder(WidgetInsets3, Color.yellow)
        : new EmptyBorder(WidgetInsets3);
    //public static final Border WidgetBorder = new MatteBorder(WidgetInsets, Color.orange);
    
    /** the special name AWT/Swing gives to pop-up Windows (menu's, rollovers, etc) */
    public static final String OVERRIDE_REDIRECT = "###overrideRedirect###";
    public static final String POPUP_NAME = "###VUE-POPUP###";

    public static final Color AquaFocusBorderLight = new Color(157, 191, 222);
    public static final Color AquaFocusBorderDark = new Color(137, 170, 201);
    public static final boolean ControlMaxWindow = false;

    //public static final Image NoImage32 = VueResources.getImage("NoImage");
    public static final Image NoImage32 = VueResources.getImage("/icon_noimage32.gif");
    public static final Image NoImage64 = VueResources.getImage("/icon_noimage64.gif");
    public static final Image NoImage128 = VueResources.getImage("/icon_noimage128.gif");
    
    
    public static boolean UseAlwaysOnTop = false;
    
    static Toolkit GToolkit;
    static GraphicsEnvironment GEnvironment;
    static GraphicsDevice GDevice;
    static GraphicsDevice[] GScreenDevices;
    static GraphicsConfiguration GConfig;
    static Rectangle GBounds;
    static Rectangle GMaxWindowBounds;
    public static Insets GInsets; // todo: this should be at least package private
    public static int GScreenWidth;
    public static int GScreenHeight;

    public static final Dimension MaxWidth = new Dimension(Short.MAX_VALUE, 1);
    public static final Dimension MaxHeight = new Dimension(1, Short.MAX_VALUE);
    public static final Dimension MaxSize = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    public static final Dimension ZeroSize = new Dimension(0,0);

    private static Window FullScreenWindow;

    private static boolean initUnderway = true;
    private static boolean isMacAqua;
    private static boolean isMacAquaBrushedMetal;
    private static boolean isOceanTheme = false;
    private static javax.swing.plaf.metal.MetalTheme Theme;

    private static Color ToolbarColor = null;
    static final Color VueColor = new ColorUIResource(VueResources.getColor("menubarColor", Color.blue));
    // private static final Color VueColor = new ColorUIResource(new Color(128,0,0)); // test

    private static boolean SKIP_CUSTOM_LAF = false; // test: don't install our L&F customizations
    private static boolean SKIP_OCEAN_THEME = false; // test: in java 1.5, use default java Metal theme instead of new Ocean theme
    private static boolean FORCE_WINDOWS_LAF = false; // test: on mac, use windows look (java metal), on windows, use native windows L&F
    private static boolean SKIP_WIN_NATIVE_LAF = false;


    public static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-skip_custom_laf")) {
                SKIP_CUSTOM_LAF = true;
            } else if (args[i].equals("-skip_ocean_theme")) {
                SKIP_OCEAN_THEME = true;
            } else if (args[i].equals("-win") || args[i].equals("-nativeWindowsLookAndFeel")) {
                FORCE_WINDOWS_LAF = true;
            } else if (args[i].equals("-nowin")) {
                SKIP_WIN_NATIVE_LAF = true;
            }
        }
    }

    public static Color getVueColor() {
        return VueColor;
    }
    
    public static Color getToolbarColor() {
        if (initUnderway) throw new InitError();
        return ToolbarColor;
    }
    
    public static void applyToolbarColor(JComponent c) {
        if (isMacAqua || Util.isUnixPlatform())
            c.setOpaque(false);
        else
            c.setBackground(getToolbarColor());
    }
    
    public static Color getTextHighlightColor() {
        if (initUnderway) throw new InitError();
        if (Theme == null)
        {
           	//return Color.yellow;
        	if (Util.isWindowsPlatform())
        		return VueResources.getColor("gui.text.highlightcolor");
        	else
        		return SystemColor.textHighlight;
        }
        else
            return Theme.getTextHighlightColor();
    }
    
    private static Color initToolbarColor() {
        //if (true) return new ColorUIResource(Color.orange); // test
        
        if (isMacAqua) {
            //if (true) return new ColorUIResource(Color.red);
            if (false && isMacAquaBrushedMetal) {
                // FYI/BUG: Mac OS X 10.4+ Java 1.5: applying SystemColor.window is no longer 
                // working for some components (e.g., palette button menu's (a JPopupMenu))
                return new ColorUIResource(SystemColor.window);
            } else
                return new ColorUIResource(SystemColor.control);
        } else
            //return new ColorUIResource(VueResources.getColor("toolbar.background"));
            return new ColorUIResource(SystemColor.control);
    }

    public static void init()
    {
        if (!initUnderway) {
            if (DEBUG.INIT) out("init: already run");
            return;
        }
        
        Log.debug("init");

        if (FORCE_WINDOWS_LAF || SKIP_CUSTOM_LAF || SKIP_OCEAN_THEME || SKIP_WIN_NATIVE_LAF)
            System.out.println("GUI.init; test parameters:"
                              + "\n\tforceWindowsLookAndFeel=" + FORCE_WINDOWS_LAF
                              + "\n\tskip_custom_laf=" + SKIP_CUSTOM_LAF
                              + "\n\tskip_ocean_theme=" + SKIP_OCEAN_THEME
                              + "\n\tskip_win_native_laf=" + SKIP_WIN_NATIVE_LAF
                               );

        if (SKIP_CUSTOM_LAF)
            Log.info("INIT: skipping installation of custom VUE Look & Feels");
        
        /* VUE's JIDE open-source license if we end up using this:
        tufts.Util.executeIfFound("com.jidesoft.utils.Lm", "verifyLicense",
                                  new Object[] { "Scott Fraize",
                                                 "VUE",
                                                 "p0HJOS:Y049mQb8BLRr9ntdkv9P6ihW" });
        */

        isMacAqua = Util.isMacPlatform() && !FORCE_WINDOWS_LAF && !VUE.isApplet();

        isMacAquaBrushedMetal =
            isMacAqua && !VUE.isApplet() &&
            VUE.isSystemPropertyTrue("apple.awt.brushMetalLook");

        ToolbarColor = initToolbarColor();

        // Note that it is essential that the theme be set before a single GUI object of
        // any kind is created.  If, for instance, a static member in any class
        // initializes a swing gui object, this will end up having no effect here, and
        // the entire theme will be silently ignored.  This includes the call below to
        // UIManager.setLookAndFeel, or even UIManager.getLookAndFeel, which is also why
        // we need to tell the VueTheme about LAF depended variable sinstead of having
        // it ask for the LAF itself, as it may not have been set yet.  Note that when
        // using the Mac Aqua L&F, we don't need to set the theme for Metal (as it's not
        // being used and would have no effect), but we still need to initialize the
        // theme, as it's still queried througout some of the older code.
        

        if (false)
            installUIDefaults();

        if (VUE.isApplet())
        {
        	   try {
        		   UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
        		      UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
        		   } catch (Exception e) 
        		   {
        			   Log.error("Couldn't load jlooks look and feel");
        		   }

        }
        else if (Util.isMacPlatform() && !VUE.isApplet()) {

            if (!SKIP_CUSTOM_LAF) {

                try {
                    UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
                    System.setProperty("Quaqua.design", "panther");
                }
                catch (Throwable t) {
                    Log.error("Couldn't load quaqua look and feel", t);
                }
            }
            
        } else {

            // We're leaving the default look and feel alone (e.g. Windows)

            // if on Windows and forcing windows look, these meants try the native win L&F
            //if (FORCE_WINDOWS_LAF && Util.isWindowsPlatform())
            if (Util.isWindowsPlatform() && !SKIP_WIN_NATIVE_LAF)
                setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            else if (Util.isMacPlatform() && VUE.isApplet())
            {
            	/////Load up some JLooks stuff here.
            }
            else {
                if (!SKIP_CUSTOM_LAF)
                    installMetalTheme();
            }
        }

        UIManager.put("FileChooser.filesOfTypeLabelText","Format:");

        if (!VUE.isApplet())
        	javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        if (Util.getJavaVersion() < 1.5f)
            UseAlwaysOnTop = false;

        String fontName;
        int fontSize;
        int fontSize2;
        if (isMacAqua) {
            fontName = "Lucida Grande";
            fontSize = 11;
            fontSize2 = fontSize + 2;
        } else {
            fontName = "SansSerif";
            fontSize = 11;
            fontSize2 = 11;
        }

        final String fixedFont = "Lucida Sans Typewriter";
        //final String fixedFont = "Courier New";
        //final String fixedFont = "Courier";

        // Verdana is not fully a fixed font, but it's still much easier to read
        // than any of the truly fixed fonts, and it's not nearly as scary
        // for users to look at.
        final String errorFont = "Verdana";
        
        LabelFace = new GUI.Face(fontName, Font.PLAIN, fontSize, GUI.LabelColor);
        ValueFace = new GUI.Face(fontName, Font.PLAIN, fontSize, Color.black);
        TitleFace = new GUI.Face(fontName, Font.BOLD, fontSize, GUI.LabelColor);
        FixedFace = new GUI.Face(fixedFont, Font.PLAIN, fontSize, GUI.LabelColor);
        
        //StatusFace = new GUI.Face(null, 0, 0, Color.darkGray, SystemColor.control);
        //StatusFace = new GUI.Face(null, 0, 0, Color.darkGray);
          StatusFace = new GUI.Face(fontName, Font.PLAIN, fontSize2, Color.darkGray);
        //StatusFace = new GUI.Face(fontName, Font.PLAIN, fontSize2, Color.darkGray, SystemColor.control);

          ErrorFace = StatusFace;
        //ErrorFace = new GUI.Face(errorFont, Font.PLAIN, fontSize+1, Color.darkGray, SystemColor.control);
          
        DataFace = new GUI.Face("Verdana", Font.PLAIN, fontSize, null);

        // Verdana & Trebuchet are designed for the screen sans-serif font 
        // Georgia a designed for the screen serif font
        
        // Both are especially sensitive to handling bolding, small sizes, and character
        // differentiation Studies have not found any actual difference in reading
        // speed, though there may be reduced eye strain.
        
        // And of course Arial (Microsoft's virtual Helevetica) is a bit more
        // compressed, and reliable in at least that everyone's read it
        // countless times.
        
        ContentFace = new GUI.Face("Arial", Font.PLAIN, 14, null); // shows underscores below underline
        //ContentFace = new GUI.Face("Verdana", Font.PLAIN, 13, null); // underline hides underscores

        FocusManager.install();
        //tufts.Util.executeIfFound("tufts.vue.gui.WindowManager", "install", null);

        org.apache.log4j.Level level = org.apache.log4j.Level.DEBUG;

        // if (!skipCustomLAF) level = org.apache.log4j.Level.DEBUG; // always show for the moment
        
        Log.log(level, "LAF  name: " + UIManager.getLookAndFeel().getName());
        Log.log(level, "LAF descr: " + UIManager.getLookAndFeel().getDescription());
        Log.log(level, "LAF class: " + UIManager.getLookAndFeel().getClass());

        initUnderway = false;
    }

    private static void installUIDefaults()
    {
        // an experiment in overriding ScrollPane control keys
        
        if (DEBUG.Enabled) UIManager.put("ScrollBar.width", new Integer(64)); // just to know we've taken effect

        // We override scroll-pane input map to turn off arrow keys, so
        // the can be used for VUE actions (e.g., Nudge)
        UIManager.put("ScrollPane.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                       /*
                           "RIGHT", "unitScrollRight",
		        "KP_RIGHT", "unitScrollRight",
                            "DOWN", "unitScrollDown",
		         "KP_DOWN", "unitScrollDown",
                            "LEFT", "unitScrollLeft",
		         "KP_LEFT", "unitScrollLeft",
                              "UP", "unitScrollUp",
		           "KP_UP", "unitScrollUp",
                       */
		         "PAGE_UP", "scrollUp",
		       "PAGE_DOWN", "scrollDown",
		    "ctrl PAGE_UP", "scrollLeft",
		  "ctrl PAGE_DOWN", "scrollRight",
                            "HOME", "scrollHome",
                             "END", "scrollEnd"
                   }));
    }

    

    private static void installMetalTheme() {
        
        CommonMetalTheme common = new CommonMetalTheme();

        if (Util.getJavaVersion() >= 1.5f && !SKIP_OCEAN_THEME) {
            //Theme = new OceanMetalTheme(common);
            String className = "tufts.vue.gui.OceanMetalTheme";
            try {
                Theme = (javax.swing.plaf.metal.MetalTheme)
                    Class.forName(className).newInstance();
                Util.execute(Theme, className, "setCommonTheme", new Object[] { common });
                isOceanTheme = true;
            } catch (Exception e) {
                Theme = null;
                e.printStackTrace();
                out("failed to load java 1.5+ Ocean theme: " +  className);
            }
        }

        if (Theme == null)
            Theme = new DefaultMetalTheme(common);

        MetalLookAndFeel.setCurrentTheme(Theme);
        
        Log.debug("installed theme: " + Theme);
    }

    /** @return true if was successful */
    private static boolean setLookAndFeel(String name) {
        try {
            if (name != null)
                javax.swing.UIManager.setLookAndFeel(name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
/*
    private static void installAquaLAFforVUE() {
        try {
            javax.swing.UIManager.setLookAndFeel(new VueAquaLookAndFeel());
        } catch (javax.swing.UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }
*/
    private static class InitError extends Error {
        InitError(String s) {
            super("GUI.init hasn't run; indeterminate result for: " + s);
        }
        InitError() {
            super("GUI.init hasn't run; indeterminate result");
        }
    }
    public static boolean isGUIInited()
    {
    	return !initUnderway;
    }
    public static boolean isMacAqua() {
        if (initUnderway) throw new InitError("isMacAqua");
        return isMacAqua;
    }
    
    public static boolean isMacBrushedMetal() {
        if (initUnderway) throw new InitError("isMacAquaBrushedMetal");
        return isMacAquaBrushedMetal;
    }
    
    public static boolean isOceanTheme() {
        if (initUnderway) throw new InitError("isOceanTheme");
        return isOceanTheme;
    }

    private static void initGraphicsInfo() {
        GToolkit = Toolkit.getDefaultToolkit();
        GEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GDevice = GEnvironment.getDefaultScreenDevice();
        GScreenDevices = GEnvironment.getScreenDevices();
        GConfig = GDevice.getDefaultConfiguration(); // this changes when display mode changes
        GBounds = GConfig.getBounds();
        GInsets = GToolkit.getScreenInsets(GConfig); // this may change at any time
        GScreenWidth = GBounds.width;
        GScreenHeight = GBounds.height;
        GMaxWindowBounds = GEnvironment.getMaximumWindowBounds();

        if (DEBUG.INIT) dumpGraphicsConfig();
    }

    public static void dumpGraphicsConfig() {
        System.out.println("GUI.initGraphicsInfo:"
                           + "\n\t    toolkit: " + GToolkit
                           + "\n\tenvironment: " + GEnvironment
                           + "\n\t  envMaxWin: " + GEnvironment.getMaximumWindowBounds()
                           );

        dumpGraphicsDevice(GDevice, "DEFAULT");

        for (int i = 0; i < GScreenDevices.length; i++) {
            if (GScreenDevices[i] != GDevice)
                dumpGraphicsDevice(GScreenDevices[i], "#"+i);
        }
    }

    public static void dumpGraphicsDevice(GraphicsDevice d, String key) {
        System.out.println("\tDevice " + key + ": " + d + " ID=" + d.getIDstring()
                           + "\n\t\t        config: " + d.getDefaultConfiguration()
                           + "\n\t\t config bounds: " + d.getDefaultConfiguration().getBounds()
                           + "\n\t\t screen insets: " + GToolkit.getScreenInsets(d.getDefaultConfiguration())
                           );
    }
    
    public static int getScreenWidth() {
        if (GScreenWidth == 0)
            initGraphicsInfo();
        return GScreenWidth;
    }
    
    public static int getScreenHeight() {
        if (GScreenHeight == 0)
            initGraphicsInfo();
        return GScreenHeight;
    }

    

    public static Rectangle getMaximumWindowBounds() {
        refreshGraphicsInfo();
        if (ControlMaxWindow)
            return VueMaxWindowBounds(GMaxWindowBounds);
        else
            return GMaxWindowBounds;
    }

    /** Return the max window bounds for the given window, for screen device it's currently displayed on */
    public static Rectangle getMaximumWindowBounds(Window w) {
        refreshGraphicsInfo();
        GraphicsDevice d = getDeviceForWindow(w);
        Rectangle bounds = d.getDefaultConfiguration().getBounds();
        Insets insets = GToolkit.getScreenInsets(d.getDefaultConfiguration());
        //out("selected bounds " + bounds);
        //out("selected insets " + insets);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left;
        bounds.height -= insets.top;
        bounds.width -= insets.right;
        bounds.height -= insets.bottom;
        //out("result bounds " + bounds);
        return bounds;
    }

    /** @return the GraphicsDevice the given window is currently displayed on.
     *
     * In the case where the Window currently overlaps two physical devices, the device
     * the window is "on" is determined by the device which is displaying the greatest
     * portion of the total area of the Window
     */
    
    public static GraphicsDevice getDeviceForWindow(Window w) {
        refreshGraphicsInfo();
        Rectangle windowBounds = w.getBounds();
        GraphicsDevice selected = null;
        int maxArea = 0;
        for (int i = 0; i < GScreenDevices.length; i++) {
            GraphicsDevice device = GScreenDevices[i];
            GraphicsConfiguration config = device.getDefaultConfiguration();
            Rectangle overlap = config.getBounds().intersection(windowBounds);
            int area = overlap.width * overlap.height;
            if (area > maxArea) {
                maxArea = area;
                selected = device;
            }
        }
        return selected == null ? GDevice : selected;
    }

    public static GraphicsConfiguration getDeviceConfigForWindow(Window w) {
        return getDeviceForWindow(w).getDefaultConfiguration();
    }
    

    public static void refreshGraphicsInfo() {
        // GraphicsConfiguration changes on DisplayMode change -- update everything.
        if (GDevice == null) {
            initGraphicsInfo();
        } else {
            GraphicsConfiguration currentConfig = GDevice.getDefaultConfiguration();
            if (currentConfig != GConfig)
                initGraphicsInfo();
            else
                GInsets = GToolkit.getScreenInsets(GConfig); // this may change at any time
            if (DEBUG.FOCUS) dumpGraphicsConfig();
        }

        if (!VUE.isStartupUnderway() && VUE.getApplicationFrame() != null) {
            if (ControlMaxWindow)
                VUE.getApplicationFrame().setMaximizedBounds(VueMaxWindowBounds(GMaxWindowBounds));

            if (DockWindow.MainDock != null) {
                if (DockWindow.MainDock.mGravity == DockRegion.BOTTOM) {
                    DockWindow.MainDock.moveToY(VUE.getApplicationFrame().getY());
                } else {
                    Point contentLoc = VUE.mViewerSplit.getLocation();
                    SwingUtilities.convertPointToScreen(contentLoc, VUE.getApplicationFrame().getContentPane());
                    DockWindow.MainDock.moveToY(contentLoc.y);
                }
            }
        }
    }


    public static int stringLength(Font font, String s) {
        return (int) Math.ceil(stringWidth(font, s));
    }
    
    public static double stringWidth(Font font, String s) {
        if (font == null) {
            String msg = "GUI.stringWidth: null font";
            if (DEBUG.Enabled)
                tufts.Util.printStackTrace(msg);
            else
                Log.error(msg);
        }
        return font.getStringBounds(s, GUI.DefaultFontContext).getWidth();
    }
    


    public static int getMaxWindowHeight() {
        refreshGraphicsInfo();
        return GMaxWindowBounds.height;
    }

    /** VUE specific threshold for configuring UI */
    public static boolean isSmallScreen() {
        refreshGraphicsInfo();
        return GScreenWidth < 1024;
    }
    
    /** center the given window on default physical screen, but never let it go above whatever
     * our max window bounds are.
     */
    public static void centerOnScreen(java.awt.Window window)
    {
        refreshGraphicsInfo();

        int x = GScreenWidth/2 - window.getWidth()/2;
        int y = GScreenHeight/2 - window.getHeight()/2;

        Rectangle wb = getMaximumWindowBounds();

        if (y < wb.y)
            y = wb.y;
        if (x < wb.x)
            x = wb.x;

        window.setLocation(x, y);
    }
    
    private static Rectangle VueMaxWindowBounds(Rectangle systemMax) {
        Rectangle r = new Rectangle(systemMax);
        
        // force 0 at left, ignoring any left inset
        //r.width += r.x;
        //r.x = 0;
        // MacOSX won't let us override this when we set a frame's state to MAXIMIZED_BOTH,.
        // although the window can still be manually positioned there (user or setLocation)
        
        int min =
            DockWindow.isTopDockEmpty() ?
            0 : 
            DockWindow.getCollapsedHeight();

        //Util.printStackTrace("min="+min);

        min += DockWindow.ToolbarHeight;
        
        r.y += min;
        r.height -= min;
        
        /*
        //int dockHeight = DockWindow.getCollapsedHeight();
        if (!DockWindow.TopDock.isEmpty()) {
            r.y += dockHeight;
            r.height -= dockHeight;
        }
        */

        // At least on mac, it's not allowing us to reduce the max window
        // bounds at the bottom: any reduction in height is taken off the top.
        // In short: the y-value in the bounds is completely ignored.
        
        //if (!DockWindow.BottomDock.isEmpty())
        //r.height -= dockHeight;

        // ignore dock gap
        /*
          // apparently can't override this on at least mac
        if (r.x <= 4) {
            r.width += r.x;
            r.x = 0;
        }
        */

        // System.out.println(r);

        return r;
        
    }

    public static Image getSystemIconForExtension(String ext) {
        return getSystemIconForExtension(ext, 32);
    }

//     private static final Image UNKNOWN_TYPE;
//     static {
//         if (Util.isMacPlatform())
//             UNKNOWN_TYPE = tufts.macosx.MacOSX.getIconForExtension("", 16);
//         else
//             UNKNOWN_TYPE = null;
//     }

    private static final Image NULL_IMAGE = NoImage32; // any image would do

    private static class ImageIconCache {

        final Map map = new java.util.concurrent.ConcurrentHashMap();
        
        public void put(String key, Object image) {
            if (DEBUG.IO||DEBUG.IMAGE) Log.debug(String.format("caching %10s: %s", key, Util.tags(image)));
            if (image == null)
                map.put(key, NULL_IMAGE);
            else
                map.put(key, image);
        }
        
        public void put(String ext, int size, Object image) {
            put(ext + "." + size, image);
        }

        public Image get(String key) {
            return (Image) map.get(key);
        }
        
        public Image get(String ext, int size)
        {
            final String key = ext + "." + size;
            final Object entry = map.get(key);
            Image image = null;

            if (entry instanceof Image) {
            	   image = (Image) entry;
            }
            else if (entry !=null)
            {          	
				try {
	                if (size == 16)
	                	image =	(Image) getIconMethod().invoke(entry, new Object[]{Boolean.FALSE});			                
					else
	                    image =	(Image) getIconMethod().invoke(entry, new Object[]{Boolean.TRUE});

	                // now that we've unpacked the ShellFolder version,
	                // we can put the real image into the cache:
	                put(key, image);            

				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            return image;
        }
    }


        
    private static final ImageIconCache IconCache = new ImageIconCache(); 

    private static final boolean LARGE_ICON = true;
    private static final boolean SMALL_ICON = false;

    private static final String TmpIconDir = VUE.getSystemProperty("java.io.tmpdir");

    private static final Image RSSIcon = VueResources.getImage("dataSourceRSS");    
    private static Class cShellFolder;


    private static Object sf;
    static Object large;

	static Object small;

    public static Image getSystemIconForExtension(String ext, int sizeRequest)
    {
        if (DEBUG.IO && DEBUG.META) Log.debug("icon request: " + ext + "@" + sizeRequest);

        if (ext == null)
            return null;
        
        ext = ext.toLowerCase();
        
        if (ext.equals(Resource.EXTENSION_VUE)) {
            final String key = "vueIcon" + sizeRequest;
            Image image = IconCache.get(key);
            if (image == null)  {
                image = tufts.vue.VueResources.getImage(key);
                if (image == null)
                    image = tufts.vue.VueResources.getImage("vueIcon64");
                IconCache.put(key, image);
            }
            return image;
        } else if (ext.equals("rss"))
            return RSSIcon;

        if (ext == Resource.EXTENSION_HTTP)
            ext = "htm";

        if (Util.isMacPlatform() && !VUE.isApplet()) {

            if (false && Util.isMacLeopard() && Util.getJavaVersion() > 1.5) {
                ; // ShellFolder/FileSystemView method still doesn't work
            } else {
                if (tufts.macosx.MacOSX.supported())
                    return tufts.macosx.MacOSX.getIconForExtension(ext, sizeRequest);
                else
                    return null;
            }
            
//             Image image = tufts.macosx.MacOSX.getIconForExtension(ext, sizeRequest);
//             // May need an unknown type for each likely sizeRequest
//             if ((image == null || image == UNKNOWN_TYPE) && ("readme".equals(ext) || "msg".equals(ext)))
//                 image = tufts.macosx.MacOSX.getIconForExtension("txt", sizeRequest);
//             return image;
        }

        
        //-----------------------------------------------------------------------------
        // Below happens for non-Mac platforms only: Windows, Linux, (etc?)
        //-----------------------------------------------------------------------------

        Image image = IconCache.get(ext, sizeRequest);
        
        if (image != null) {
            if (DEBUG.IO) Log.debug(String.format("cache hit %8s: %s", ext + "." + sizeRequest, Util.tags(image)));
            if (image == NULL_IMAGE) {
                return null;
            } else {
                return image;
            }
        }

        // proceed the SLOW way, but all we can do until we have jdic/jdesktop and they actually handle this for us:

        File file = null;
        
        try {

            if ("dir".equals(ext)) {
                file = new File(TmpIconDir); // a guaranteed vanilla directory we should be able to find
                if (DEBUG.IO) Log.debug(" trying " + file);
            } else {
                file = new File(TmpIconDir + File.separator + "vueIcon." + ext);

                if (DEBUG.IO) Log.debug(" trying " + file);

                if (file.createNewFile()) 
                    if (DEBUG.Enabled) Log.debug("created " + file);
                // we deliberately leave the above files behind, so that hopefully
                // future runs of VUE on this machine will be faster
            }

            // You MUST have found or created a real, actual file for ShellFolder
            // to work.
            if (cShellFolder == null)
            	cShellFolder = Class.forName("sun.awt.shell.ShellFolder");

            if (cShellFolder !=null)
            {
            	if (sizeRequest <= 24) {
            		//false
            		sf = getShellFolderMethod().invoke(null, new Object[]{file});
            		image = (Image) getIconMethod().invoke(sf, new Object[]{Boolean.FALSE});//shellFolder.getIcon(SMALL_ICON);
                
            		small = image;
            		large = sf; //shellFolder; // can be fetched later
                
            	} else {
            		//true
            		sf = getShellFolderMethod().invoke(null, new Object[]{file});
            		image = (Image) getIconMethod().invoke(sf, new Object[]{Boolean.TRUE});//shellFolder.getIcon(LARGE_ICON);
                
            		small = sf;//shellFolder;  // can be fetched later
            		large = image;
            	}
            }
            else
            {
            	//you're not on a Sun JVM and not on a mac at this point.
            	
            	small = null;
            	large = null;
            	
            }

            // We assume here that on Windows platforms, small is always 16x16, and large is always 32x32
            // See IconCache code that also depends on this.

            IconCache.put(ext, 16, small); 
            IconCache.put(ext, 32, large);
            
            if (image != null) {
                if (image.getHeight(null) != sizeRequest) {
                    if (DEBUG.IO || DEBUG.IMAGE)
                        Log.debug(Util.TERM_RED
                                  + "image size doesn't match request size for key [" + ext + "." + sizeRequest + "]: "
                                  + Util.tags(image)
                                  + Util.TERM_CLEAR);
                    if (DEBUG.IO) Log.debug("caching: " + ext + "@" + sizeRequest + ": " + Util.tags(image));
                    IconCache.put(ext, sizeRequest, image);
                }
            } else {
                throw new NullPointerException("no Image");
            }
            
        } catch (Throwable t) {
            //Log.warn("could not get Icon for filetype: " + ext + "." + sizeRequest + "; ", t);
            
            Icon fsIcon = null;
            try {
                // debug: does this return us anything interesting?  java 1.5 default impl delegates to ShellFolder,
                // so result should presumably be the same
                fsIcon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(file);
            } catch (Throwable _) {}
            
            Log.warn("could not get Icon for filetype: " + ext + "." + sizeRequest + "; " + t + "; fsIcon=" + fsIcon);
            
            IconCache.put(ext, sizeRequest, null);
        }

        return image;
    }

    private static Method iconMethod = null;
    private static Method getShellFolder = null;
    
    private static Method getIconMethod()
    {
       if (iconMethod == null)           
 	   {
			try {
			    iconMethod = cShellFolder.getDeclaredMethod("getIcon", new Class[]{Boolean.TYPE});
			} catch (SecurityException e) {
				Log.info(e.toString() + "while trying to invoke ShellFolder, possibly non-sun JVM");
			} catch (NoSuchMethodException e) {
				Log.info(e.toString() + "while trying to invoke ShellFolder, possibly non-sun JVM");				} catch (IllegalArgumentException e) {
			}
 	   }
       return iconMethod;	  
    }
    
    private static Method getShellFolderMethod()
    {
       if (getShellFolder == null)           
 	   { 		  
			try {
				getShellFolder = cShellFolder.getDeclaredMethod("getShellFolder", new Class[]{File.class});			
			} catch (SecurityException e) {
				Log.info(e.toString() + "while trying to invoke ShellFolder, possibly non-sun JVM");
			} catch (NoSuchMethodException e) {
				Log.info(e.toString() + "while trying to invoke ShellFolder, possibly non-sun JVM");				} catch (IllegalArgumentException e) {
			}
 	   }
       return getShellFolder;
    	  
    }
    
    private static Image getIconFromReflection(String file)
    {
    	
    	   if (cShellFolder == null){
			try {
				cShellFolder = Class.forName("sun.awt.shell.ShellFolder");
			} catch (ClassNotFoundException e1) {
				Log.info(e1.toString() + "while trying to load ShellFolder, possibly non-sun JVM");
			}
    	   }
    	   
           if (cShellFolder !=null)
           {
        	   
        	   
        	   Image i = null;
        	   try {
    			   sf = getShellFolderMethod().invoke(null, new Object[]{file});
        		   i = (Image) getIconMethod().invoke(sf, new Object[]{Boolean.FALSE});
        	   } catch (IllegalArgumentException e) {
        		   Log.info(e.toString() + "while trying to load ShellFolder, possibly non-sun JVM");
        	   } catch (IllegalAccessException e) {
        		   Log.info(e.toString() + "while trying to load ShellFolder, possibly non-sun JVM");
        	   } catch (InvocationTargetException e) {
        		   Log.info(e.toString() + "while trying to load ShellFolder, possibly non-sun JVM");
        	   }
               return i;
           	
           }
           else
        	   return null;

          
    }
    /** these may change at any time, so we must fetch them newly each time */
    public static Insets getScreenInsets() {
        refreshGraphicsInfo();
        return GInsets;
    }
    
    
    /**
     * Factory method for creating frames in VUE.  On PC, this
     * is same as new new JFrame().  In Mac Look & Feel it adds a duplicate
     * menu-bar to the frame as every frame needs one
     * or we lose the mebu-bar (todo: no longer true)
     */
    public static JFrame createFrame()
    {
        return createFrame(null);
    }
    
    public static JFrame createFrame(String title)
    {
        JFrame newFrame = new JFrame(title);
        /*
        if (isMacAqua()) {
            JMenuBar menu = new VueMenuBar();
            newFrame.setJMenuBar(menu);
        }
        */
        return newFrame;
    }

    
    /** @return a new VUE application DockWindow */
    public static DockWindow createDockWindow(String title, boolean asToolbar,boolean showCloseButton) {

        // In Java 1.5 we can set the Window's to be always on top.  In this case, we
        // can use a hidden parent for the DockWindow, set them to be never focusable, and
        // use our FocusManager to force focus to it as needed.  We also need our
        // WindowManager in this case to hide the DockWindow's when the application goes
        // inactive, otherwise they'll stay on top of all other applications.

        // Doing this allows the main VUE frame to keep application focus even when
        // using tool widges (the title bar stays active), will allow us to create an
        // MDI (multiple document interface) because the DockWindow's aren't parented to
        // just one window, and allows DockWindows to be used in full-screen mode for
        // the same reason: they always stay on top, and don't need to be children of
        // the currently active window to be seen.  If we could re-parent java Window's
        // on the fly we could avoiid all this, but alas, we can't.  (Java Window's
        // always stay on top of their parent, but not on top of other windows,
        // including "grandparents".)
        
    	if ((Util.isUnixPlatform() && Util.getJavaVersion() >= 1.5f ) || VUE.isApplet()	)
    	{
    		//There are a bunch of focus problems on Unix, hopefully this clears it up.
    		return new DockWindow(title,VUE.getRootWindow(),null,asToolbar,showCloseButton);
    	}
    	else if (UseAlwaysOnTop && Util.getJavaVersion() >= 1.5f) {
            DockWindow dockWindow
                = new DockWindow(title, DockWindow.getHiddenFrame(), null, asToolbar,showCloseButton);
            dockWindow.setFocusableWindowState(false);
            setAlwaysOnTop(dockWindow.window(), true);
            return dockWindow;
        } else {
            // TODO: create method in VUE for getting DockWindow parent for use elsewhere
            return new DockWindow(title, getFullScreenWindow(), null, asToolbar,showCloseButton);
            //return new DockWindow(title, VUE.getRootWindow());
        }
    }

    public static DockWindow createDockWindow(String title, boolean asToolbar) {
    	return createDockWindow(title,asToolbar,true);
    }
    public static DockWindow createDockWindow(String title) {
        return createDockWindow(title, false);
    }

    /** the given panel must have it's title set via setName */
    public static DockWindow createDockWindow(JComponent c) {
        return createDockWindow(c.getName(), c);
    }
    
    
    /**
     * Convience method.
     * @return a new VUE application DockWindow
     * @param content - a component to put in the dock window
     */
    public static DockWindow createDockWindow(String title, javax.swing.JComponent content) {
        DockWindow dw = createDockWindow(title);
        dw.setContent(content);
        return dw;
    }

    /**
     * @return a new VUE application Dockable Toolbar
     * @param content - a component to use for the roolbar
     */
    public static DockWindow createToolbar(String title, javax.swing.JComponent content) {
        DockWindow dw = createDockWindow(title, true);
        dw.setContent(content);
        return dw;
    }

    public static Frame HiddenDialogParent = null;

    // based on SwingUtilities.getSharedOwnerFrame() 
    public static Frame getHiddenDialogParentFrame() { 
        if (HiddenDialogParent == null) {
            HiddenDialogParent = new Frame() {
                    public void show() {} // This frame can never be shown
                    // this will prevent children from going behind other windows?
                    public boolean isShowing() { return true; }
                    public String toString() { return name(this); }
                };
            HiddenDialogParent.setName("*VUE-DIALOG-PARENT*");
        }
        return HiddenDialogParent;
    }

    public static Window getFullScreenWindow() {
        if (FullScreenWindow == null) {

            //FullScreenWindow = new Window(VUE.getRootFrame());
            //FullScreenWindow = new JWindow(VUE.getRootFrame());
            //FullScreenWindow = new DockWindow("***VUE-FULLSCREEN***", VUE.getRootFrame());

            FullScreenWindow = new FullScreen.FSWindow();
        }
        return FullScreenWindow;
    }

//     public static Window getCurrentRootWindow() {
//         if (FullScreen.inFullScreen())
//             return FullScreenWindow;
//         else
//             return VUE.getRootWindow();
//     }

    /**
     * Find the containing Window of the given Component, and make sure it's displayed
     * on screen.
     */
    public static void makeVisibleOnScreen(Component c)
    {
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(c);
        if (w != null && !w.isShowing()) {
            if (DEBUG.WIDGET) {
                out(GUI.name(c) + " showing containing window " + GUI.name(w));
                if (DEBUG.META) tufts.Util.printStackTrace("showing containing window " + GUI.name(w));
            }
            DockWindow.flickerAnchorDock();
            w.setVisible(true);
            w.toFront();
        }
    }

    /**
     * Find the given class in the AWT hierarchy, and make sure it's containing Window
     * is visible on the screen.  If the found instance is a Widget, it is expanded.
     * If it's parent is a JTabbedPane, it's tab is selected.
     *
     * @param clazz - a class that must be a subclass of Component (or nothing will be found)
     */
    public static void makeVisibleOnScreen(Object requestor, Class clazz)
    {
        new EventRaiser<java.awt.Component>(requestor, clazz) {
            public void dispatch(Component c) {
                if (isWidget(c)) {
                    Widget.setExpanded((JComponent)c, true);
                } else {
                    if (c.getParent() instanceof JTabbedPane)
                        ((JTabbedPane)c.getParent()).setSelectedComponent(c);
                    makeVisibleOnScreen(c);
                }
            }
        }.raise();
    }

    public static boolean isWidget(Component c) {
        return c instanceof JComponent && Widget.isWidget((JComponent)c);
    }
    
    
    
    /*
    private static Cursor oldRootCursor;
    private static Cursor oldViewerCursor;
    private static tufts.vue.MapViewer waitedViewer;

    private static synchronized void activateWaitCursor(final Component component) {

        if (oldRootCursor != null) {
            if (DEBUG.FOCUS) out("multiple wait-cursors: already have " + oldRootCursor + "\n");
            return;
        }
        if (VUE.getActiveViewer() != null) {
            waitedViewer = VUE.getActiveViewer();
            oldViewerCursor = waitedViewer.getCursor();
            waitedViewer.setCursor(CURSOR_WAIT);
        }
        JRootPane root = SwingUtilities.getRootPane(component);
        if (root != null) {
            if (true||DEBUG.Enabled) out("ACTIVATING WAIT CURSOR: current =  " + oldRootCursor
                                   + "\n\t on JRootPane " + root);
            oldRootCursor = root.getCursor();
            root.setCursor(CURSOR_WAIT);
        }
    }
    
    private static void _clearWaitCursor(Component component) {
        //out("restoring old cursor " + oldRootCursor + "\n");
        if (oldRootCursor == null)
            return;
        if (waitedViewer != null) {
            waitedViewer.setCursor(oldViewerCursor);
            waitedViewer = null;
        }
        SwingUtilities.getRootPane(VUE.ApplicationFrame).setCursor(oldRootCursor);
        oldRootCursor = null;
    }
    */
    
    private static Map CursorMap = new HashMap();
    private static boolean WaitCursorActive = false;
    private static Component ViewerWithWaitCursor;
    
    public static synchronized void activateWaitCursor() {
        //tufts.Util.printStackTrace("ACTIAVTE WAIT-CURSOR");
        if (DEBUG.THREAD) Log.info("ACTIVATE WAIT CURSOR");
        activateWaitCursorInAllWindows();
    }
    
    private static synchronized void activateWaitCursorInAllWindows() {

        synchronized (CursorMap) {
            if (DEBUG.THREAD) Log.info("ACTIVATE WAIT CURSOR IN ALL WINDOWS");

            //if (!CursorMap.isEmpty()) {
            if (WaitCursorActive) {
                //VUE.Log.error("attempting to activate wait cursor while one is already active");
                if (DEBUG.THREAD) Log.info("FYI, activating wait cursor for all windows while one is already active");
                //return;
            }

            // We must activate on the MapViewer first, as is a child of ApplicationFrame or
            // FullScreenWindow, and setting the wait cursor on those first will cause the
            // viewer "old" cursor to already think it's the wait cursor.  We need to handle
            // the view as it's own case as it may have a tool active that has a different
            // cursor than the default currently active.
            
            //activateWaitCursor(ViewerWithWaitCursor = VUE.getActiveViewer());
            activateWaitCursor(VUE.getApplicationFrame());
            activateWaitCursor(getFullScreenWindow());
            for (DockWindow dw : DockWindow.AllWindows)
                activateWaitCursor(dw.window());

            WaitCursorActive = true;
        }
    }
    
    private static void activateWaitCursor(final Component c) {
        if (c == null)
            return;
        Cursor curCursor = c.getCursor();
        
        if (curCursor != CURSOR_DEFAULT && curCursor != CURSOR_WAIT)
            CursorMap.put(c, curCursor);
        
        if (c instanceof JComponent)
        {
        	RootPaneContainer root = (RootPaneContainer) ((JComponent)c).getTopLevelAncestor();
        	root.getGlassPane().setCursor(CURSOR_WAIT);
        	root.getGlassPane().setVisible(true);
        }
        else if (c instanceof JFrame)
        {        	        
        	((JFrame)c).getGlassPane().setCursor(CURSOR_WAIT);
        	((JFrame)c).getGlassPane().setVisible(true);        
        }
        else
        	c.setCursor(CURSOR_WAIT);
        
        if (DEBUG.THREAD) Log.info("set wait cursor on " + name(c) + " (old=" + curCursor + ")");
    }
          
    public static synchronized void clearWaitCursor() {
        //tufts.Util.printStackTrace("CLEAR WAIT-CURSOR");
        if (DEBUG.THREAD) Log.info("CLEAR WAIT CURSOR SCHEDULED");
        VUE.invokeAfterAWT(new Runnable() { public void run() { clearAllWaitCursors(); }});
    }
    
    private static synchronized void clearAllWaitCursors() {

        synchronized (CursorMap) {
            if (DEBUG.THREAD) Log.info("CLEAR ALL WAIT CURSORS");
            if (!WaitCursorActive) {
                if (DEBUG.THREAD) Log.info("\t(wait cursors already cleared)");
                return;
            }
            //clearWaitCursor(ViewerWithWaitCursor);
            clearWaitCursor(VUE.getApplicationFrame());
            clearWaitCursor(getFullScreenWindow());
            for (DockWindow dw : DockWindow.AllWindows)
                clearWaitCursor(dw.window());
            CursorMap.clear();
            WaitCursorActive = false;
        }
    }

    private static void clearWaitCursor(Component c) {
        if (c == null)
            return;
        Object oldCursor = CursorMap.get(c);
        if (oldCursor == null || oldCursor == CURSOR_WAIT) {
            if (oldCursor == CURSOR_WAIT)
                Log.error("old cursor on " + name(c) + " was wait cursor!  Restoring to default.");
            if (DEBUG.THREAD) Log.info("cleared wait cursor on " + name(c) + " to default");
            
            if (c instanceof JComponent)
            {
            	RootPaneContainer root = (RootPaneContainer) ((JComponent)c).getTopLevelAncestor();
            	root.getGlassPane().setCursor(CURSOR_DEFAULT);
            	root.getGlassPane().setVisible(false);
            }
            else if (c instanceof JFrame)
            {
            	((JFrame)c).getGlassPane().setCursor(CURSOR_DEFAULT);
            	((JFrame)c).getGlassPane().setVisible(false);    
            }
            	
                        
            c.setCursor(CURSOR_DEFAULT);
        } else {
            if (DEBUG.THREAD) Log.info("cleared wait cursor on " + name(c) + " to old: " + oldCursor);
            
            if (c instanceof JComponent)
            {
            	RootPaneContainer root = (RootPaneContainer) ((JComponent)c).getTopLevelAncestor();
            	root.getGlassPane().setCursor((Cursor) oldCursor);
            	root.getGlassPane().setVisible(false);
            }
            else if (c instanceof JFrame)            
            {
            	((JFrame)c).getGlassPane().setCursor((Cursor) oldCursor);
            	((JFrame)c).getGlassPane().setVisible(false);
            }
            	
            
            c.setCursor((Cursor) oldCursor);
        }
    }
    
    
    /**
     * Size a normal Window to the maximum size usable in the current
     * platform & desktop configuration, <i>without</i> using the java
     * special full-screen mode, which can't have any other windows
     * on top of it, and changes the way user input events are handled.
     * On the PC, this will just be the whole screen (bug: probably
     * not good enough if they have non-hiding menu bar set to always-
     * on-top). On the Mac, it will be adjusted for the top menu
     * bar and the dock if it's visible.
     */
    // todo: test in multi-screen environment
    private static void XsetFullScreen(Window window)
    {
        refreshGraphicsInfo();
        
        Dimension screen = window.getToolkit().getScreenSize();
        
        if (Util.isMacPlatform()) {
            // mac won't layer a regular window over the menu bar, so
            // we need to limit the size
            Rectangle desktop = GEnvironment.getMaximumWindowBounds();
            out("setFullScreen: mac maximum bounds  " + Util.out(desktop));
            if (desktop.x > 0 && desktop.x <= 4) {
                // hack for smidge of space it attempts to leave if the dock is
                // at left and auto-hiding
                desktop.width += desktop.x;
                desktop.x = 0;
            } else {
                // dock at bottom & auto-hiding
                int botgap = screen.height - (desktop.y + desktop.height);
                if (botgap > 0 && botgap <= 4) {
                    desktop.height += botgap;
                } else {
                    // dock at right & auto-hiding
                    int rtgap = screen.width - desktop.width;
                    if (rtgap > 0 && rtgap <= 4)
                        desktop.width += rtgap;
                }
            }
            
            if (DEBUG.FOCUS) desktop.height /= 5;
            
            out("setFullScreen: mac adjusted bounds " + Util.out(desktop));

            window.setLocation(desktop.x, desktop.y);
            window.setSize(desktop.width, desktop.height);
        } else {
            if (DEBUG.FOCUS) screen.height /= 5;
            window.setLocation(0, 0);
            window.setSize(screen.width, screen.height);
        }
        out("setFullScreen: set to " + window);
    }
    
    private static Rectangle getFullScreenBounds()
    {
        refreshGraphicsInfo();

        //out(" GBounds: " + GBounds);

        return new Rectangle(GBounds);

    }

    public static Icon getIcon(String name) {
        return VueResources.getIcon(GUI.class, "icons/" + name);
    }


    public static Rectangle getFullScreenWindowBounds()
    {
        Rectangle bounds = getFullScreenBounds();

        if (Util.isMacPlatform()) {
            // place us under the mac menu bar
            bounds.y += GInsets.top;
            bounds.height -= GInsets.top;
        }

        if (DEBUG.PRESENT) {
            // so we can see underlying diagnostic windows
            bounds.x += bounds.width / 4;
            //bounds.y += bounds.height / 16;
            //bounds.y += 22;
            bounds.width /= 2;
            bounds.height /= 3;
        }
        //out("FSBounds: " + bounds);
        return bounds;
    }
    

    /**
     * In case window is off screen, size it, then set visible on screen, then place it.
     * This avoids reshape flashing -- when a window is setVisible, it sometimes
     * displays for a moment before it's taken it's new size and been re-validated.
     */
    
    public static void setFullScreenVisible(Window window)
    {
        final Rectangle bounds = getFullScreenWindowBounds();

        window.setSize(bounds.width, bounds.height);
        window.setVisible(true);
        window.setLocation(bounds.x, bounds.y);
    }

    public static void setFullScreen(Window window) {
        final Rectangle bounds = getFullScreenWindowBounds();

        window.setSize(bounds.width, bounds.height);
        window.setLocation(bounds.x, bounds.y);
    }
    
    
//     public static void setFullScreenVisible(Window window) {
//         setFullScreenSize(window);
//         setFullScreen
//     }
    

    
    /** set window to be as off screen as possible */
    public static void setOffScreen(java.awt.Window window)
    {
        refreshGraphicsInfo();
        window.setLocation(-999, 8000);
    }
    
    public static void setAlwaysOnTop(Window w, boolean onTop) {
        Log.debug("setAlwaysOnTop " + onTop + " " + name(w));
        // this was needed for pre Java 1.5 support:
        //Util.invoke(w, "setAlwaysOnTop", onTop ? Boolean.TRUE : Boolean.FALSE);
        w.setAlwaysOnTop(onTop);
    }

    /**
     * Given location and size (of presumably a Window) modify location
     * such that the resulting bounds are on screen.  If the window
     * is bigger than the screen, it will keep the upper left corner
     * visible.
     * 
     * Can be used to keep tool-tips on-screen.
     *
     * Note: this does not refresh the graphics config as may be used
     * several times while attempting a placement.
     */
    public static void keepLocationOnScreen(Point loc, Dimension size) {

        // if would go off bottom, move up
        if (loc.y + size.height >= GScreenHeight)
            loc.y = GScreenHeight - (size.height + 1);
        
        // if would go off top, move back down
        // Avoid top insets as windows don't normally go
        // over the menu bar at the top.
        if (loc.y < GUI.GInsets.top)
            loc.y = GUI.GInsets.top;
        
        // if would go off right, move back left
        if (loc.x + size.width > GScreenWidth)
            loc.x = GScreenWidth - size.width;

        // if would go off left, just put at left
        if (loc.x < 0)
            loc.x = 0;
    }

    /*
    public interface KeyBindingRelayer {
        public boolean processKeyBindingUp(KeyStroke ks, KeyEvent e, int condition, boolean pressed);
    }


     * We need this to hand off command keystrokes to the VUE menu bar.  This isn't as
     * important given the way do things in java 1.5, but we really need in 1.4 and on
     * the PC -- otherwise when a DockWindow has the focus, you can't use all the global
     * short-cut keys, such as the ability to hide or show a DockWindow with a shortcut!
     *
     * Allowing this in java 1.5 means we can access the VueMenuBar
     * shortcuts even while editing a text field.

    // todo: we could probably have our FocusManager handle this for us, which may be a bit cleaner
    // todo: or, we could just get the input map from menu bar and invoke the action ourselves...
    public static boolean processKeyBindingToMenuBar(KeyBindingRelayer relayer,
                                                     KeyStroke ks,
                                                     KeyEvent e,
                                                     int condition,
                                                     boolean pressed)
    {
        // We want to ignore vanilla typed text as a quick-reject culling mechanism.
        // So only if any modifier bits are on (except SHIFT), do we attempt to
        // send the KeyStroke to the JMenuBar to check against any accelerators there.
            
        final int PROCESS_MASK =
            InputEvent.CTRL_MASK |
            InputEvent.META_MASK |
            InputEvent.ALT_MASK |
            InputEvent.ALT_GRAPH_MASK;

        // On Mac java 1.4.2, and on the PC (TODO: also java 1.5 or only java 1.4.2?)
        // we have to manually redirect the key events to the main VueMenuBar.
            
        if ((e.getModifiers() & PROCESS_MASK) == 0 && !e.isActionKey())
            return relayer.processKeyBindingUp(ks, e, condition, pressed);
                
        if (DEBUG.TOOL||DEBUG.FOCUS) out(name(relayer) + " processKeyBinding [" + ks + "] condition="+condition);
            
        // processKeyBinding never appears to return true, and this key event never
        // gets marked as consumed (I guess we'd have to listen for that in our text fields
        // and consume it), so we're always passing the character up the tree...
            
        if (relayer.processKeyBindingUp(ks, e, condition, pressed) || e.isConsumed())
            return true;
            
        // Condition usually comes in as WHEN_ANCESTOR_OF_FOCUSED_COMPONENT (1), which doesn't do it for us.
        // We need condition (2): WHEN_IN_FOCUSED_WINDOW
            
        int newCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;
            
        if (DEBUG.TOOL||DEBUG.FOCUS)
            out(name(relayer) + " processKeyBinding [" + ks + "] handing to VueMenuBar w/condition=" + newCondition);
            
        try {
            return false; //return VUE.getJMenuBar().doProcessKeyBinding(ks, e, newCondition, pressed);
        } catch (NullPointerException ex) {
            out("no menu bar: " + ex);
            return false;
        }
    }
     */
    
    public static void invokeAfterAWT(Runnable runnable) {
        java.awt.EventQueue.invokeLater(runnable);
    }
    public static void invokeOnEDT(Runnable runnable) {
        if (java.awt.EventQueue.isDispatchThread())
            runnable.run();
        else 
            java.awt.EventQueue.invokeLater(runnable);
    }
    
    public static void messageAfterAWT(final String s) {
        java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() { System.out.println(s); }
            });
    }

    public static void postEvent(AWTEvent e) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }
    
    public static void postEventLater(final AWTEvent e) {
        invokeAfterAWT(new Runnable() { public void run() { postEvent(e); }});
    }


    public static void dumpSizes(Component c) {
        dumpSizes(c, "");
    }
    
    public static void dumpSizes(Component c, String msg) {
        //if (DEBUG.META) tufts.Util.printStackTrace("java 1.5 only");
        final boolean prfSet, maxSet , minSet;
        final String     sp = "     ";
        final String setMsg = " SET ";
        
        prfSet = c.isPreferredSizeSet();
        maxSet = c.isMinimumSizeSet();
        minSet = c.isMaximumSizeSet();
        out(name(c) + " sizes; " + msg
            + "\n\t    Size" + sp + name(c.getSize())
            + "\n\tprefSize" + (prfSet?setMsg:sp) + name(c.getPreferredSize())
            + "\n\t minSize" + (maxSet?setMsg:sp) + name(c.getMinimumSize())
            + "\n\t maxSize" + (minSet?setMsg:sp) + name(c.getMaximumSize())
            );

// Pre-Java 1.5
//         boolean prfSet = false, maxSet = false, minSet = false;
//         String setMsg = " ??? ";
//         try {
//             prfSet = ((Boolean)Util.invoke(c, "isPreferredSizeSet")).booleanValue();
//             maxSet = ((Boolean)Util.invoke(c, "isMinimumSizeSet")).booleanValue();
//             minSet = ((Boolean)Util.invoke(c, "isMaximumSizeSet")).booleanValue();
//             setMsg = " SET ";
//         } catch (Throwable t) {}
//         out(name(c) + " sizes; " + msg
//             + "\n\t    Size" + sp + name(c.getSize())
//             + "\n\tprefSize" + (prfSet?setMsg:sp) + name(c.getPreferredSize())
//             + "\n\t minSize" + (maxSet?setMsg:sp) + name(c.getMinimumSize())
//             + "\n\t maxSize" + (minSet?setMsg:sp) + name(c.getMaximumSize())
//             );
    }

    

    public static String name(Object c) {
        return name(c, false);
    }
    
    public static String namex(Object c) {
        return name(c, true);
    }
    
    /** @return a short name for given object, being smart about it if it's a java.awt.Component */
    public static String name(Object c, boolean unique) {

        if (c == null)
            return "null";

        if (c instanceof Boolean)
            return "bool:" + ((Boolean)c).booleanValue();

        if (c instanceof String)
            return c.toString();

        if (c instanceof AWTEvent)
            return eventName((AWTEvent) c);
        
        String title = null;
        String name = null;
        
        if (c instanceof java.awt.Frame) {
            title = ((java.awt.Frame)c).getTitle();
            if ("".equals(title))
                title = null;
        }

        if (title == null && c instanceof Component) {
            title = ((Component)c).getName();
            if (OVERRIDE_REDIRECT.equals(title))
                title = "###";
            if (title == null) {
                if (c instanceof javax.swing.JLabel)
                    title = ((javax.swing.JLabel)c).getText();
                else if (c instanceof javax.swing.AbstractButton)
                    title = ((javax.swing.AbstractButton)c).getText();
            }
        } else if (c instanceof java.awt.MenuComponent) {
            title = ((java.awt.MenuComponent)c).getName();
        } else if (c instanceof java.awt.Dimension) {
            Dimension d = (Dimension) c;
            title = "w="+ d.width + " h=" + d.height;
        }
        

        name = baseObjectName(c);
        if (unique || title == null || title.startsWith("###"))
            name += "@" + Integer.toHexString(c.hashCode());
        if (title != null)
            name += "(" + title + ")";
        
        
        String text = null;
        if (c instanceof javax.swing.text.JTextComponent) {
            text = ((javax.swing.text.JTextComponent)c).getText();
            int ni = text.indexOf('\n');
            if (ni > 1 && !DEBUG.META)
                text = text.substring(0,ni) + "...";
        }
        
        if (text != null)
            name += "[" + text + "]";

        return name;
    }
    
    public static String name(AWTEvent e) {
        return eventName(e);
    }
    
    public static String eventName(AWTEvent e) {
        return String.format("%-40s %-20s %s",
                             name(e.getSource()),
                             e.getClass().getSimpleName(),
                             eventParamString(e)
                             );
//         return ""
//             + VueUtil.pad(37, name(e.getSource()))
//             //+ " " + VueUtil.pad(25, baseClassName(e.getClass().getName() + "@" + Integer.toHexString(e.hashCode())))
//             + " " + VueUtil.pad(20, baseClassName(e.getClass().getName()))
//             + eventParamString(e)
//             ;
        
    }

    public static String eventParamString(AWTEvent e) 
    {
        String s = "[" + e.paramString();
        if (e instanceof InputEvent) {
            if (e instanceof MouseEvent) {
                if (((MouseEvent)e).isPopupTrigger())
                    s += ",POPUP-TRIGGER";
                if (Util.isMacPlatform() && ((InputEvent)e).isControlDown())
                    s += ",MAC-POPUP";
            }
            s += "," + Integer.toBinaryString(((InputEvent)e).getModifiers());
            s += "," + Integer.toBinaryString(((InputEvent)e).getModifiersEx());
        }
        return s + "]";
    }
    

    private static String baseClassName(Class clazz) {
        return clazz.getSimpleName();
        //return baseClassName(clazz.getName());
    }
    private static String baseObjectName(Object o) {
        return o == null ? "null" : baseClassName(o.getClass());
        //return o == null ? "null" : baseClassName(o.getClass().getName());
    }
//     protected static String baseClassName(String className) {
//         return className.substring(className.lastIndexOf('.') + 1);
//     }

    //-----------------------------------------------------------------------------
    // Event testers
    //-----------------------------------------------------------------------------
    
    public static final int ALL_MODIFIER_KEYS_MASK =
          java.awt.event.InputEvent.SHIFT_MASK
        | java.awt.event.InputEvent.CTRL_MASK
        | java.awt.event.InputEvent.META_MASK
        | java.awt.event.InputEvent.ALT_MASK
        ;

    private static boolean isPopupTrigger(MouseEvent e) {
        
        if (Util.isMacPlatform()) {
            
            // will detect mac pop-up trigger in any MouseEvent (not just MOUSE_PRESSED)

            return e.isPopupTrigger() // does NOT detect in MOUSE_RELEASED
                || e.isControlDown(); // will detect mac pop-up trigger in any MouseEvent
            
        } else {
            
            return e.isPopupTrigger();
            
        }
    }

    public static boolean isDoubleClick(MouseEvent e) {
        
        // % 2 detects cascading double clicks (reported as a 4 click, 6 click, etc)

        // clickCount > 0 prevents action with long mouse down (reports as 0 click)

        // we also make sure that BUTTON1_MASK is set -- it's a left click
        
        return e.getClickCount() > 1
            && e.getClickCount() % 2 == 0
            && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
          //&& (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0 // will not detect in MOUSE_RELEASED
            && !isPopupTrigger(e) 
            ;
    }
    
//     public static boolean isUnmodifiedDoubleClick(MouseEvent e) {
//         return isDoubleClick(e)
//             && (e.getModifiers() & GUI.ALL_MODIFIER_KEYS_MASK) == 0
//     }
//     public static boolean isModifiedDoubleClick(MouseEvent e) {
//         return isDoubleClick(e)
//             && (e.getModifiers() & GUI.ALL_MODIFIER_KEYS_MASK) != 0
//     }
    
    
    public static boolean isSingleClick(MouseEvent e) {
        return e.getClickCount() == 1
            && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
          //&& (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0 // will not detect in MOUSE_RELEASED
        ////&& (e.getModifiers() & GUI.ALL_MODIFIER_KEYS_MASK) == 0
            ;
    }
    
    /** @return true if 1 click, button 2 or 3 pressed, button 1 not already down & ctrl not down */
    public static boolean isRightClick(MouseEvent e) {
        return e.getClickCount() == 1
            && (e.getButton() == MouseEvent.BUTTON3 ||
                e.getButton() == MouseEvent.BUTTON2)
            && (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0
            && !e.isControlDown() // is used for context-menu pop-up on Mac OS X
            ;
    }

//     /** single click count and isRightClick is true */
//     public static boolean isSingleRightClick(MouseEvent e) {
//         return e.getClickCount() == 1 && isRightClick(e);
//     }

    public static boolean anyModifierKeysDown(InputEvent e) {
        return (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) != 0;
    }
    
    public static boolean noModifierKeysDown(InputEvent e) {
        return !anyModifierKeysDown(e);
    }
    
    public static boolean isMenuPopup(ComponentEvent ce) {
        if (ce instanceof MouseEvent) {
            MouseEvent e = (MouseEvent) ce;
            return e.isPopupTrigger() || isRightClick(e);
        } else
            return false;
    }
 
    //-----------------------------------------------------------------------------
    
    private static class LocalData extends DataFlavor {
        private Class clazz;
        LocalData(Class clazz) throws ClassNotFoundException {
            super(DataFlavor.javaJVMLocalObjectMimeType
                  + "; class=" + clazz.getName()
                  + "; humanPresentableName=" + baseClassName(clazz));
            //super(DataFlavor.javaJVMLocalObjectMimeType, baseClassName(clazz));
            this.clazz = clazz;
        }

        /*
         * DataFlavor.equals make's all javaJVMLocalObjectMimeType's look the same.  As
         * we don't support cross VM transfer for these yet (serialization), we override
         * equals to do an object compare (otherwise, Transferable's get confused, as
         * they can't distinguish between these types).
         *
         * Okay -- don't need this if we construct above by building up a MimeType
         * specification.
         */
        /*
        public boolean equals(DataFlavor that) {
            return this == that;
        }
        */
        
    }
    
    public static DataFlavor makeDataFlavor(String mimeType) {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(mimeType);
        } catch (Throwable t) {
            Util.printStackTrace(t);
        }
        return flavor;
    }
    
    // probably move this & LocalData to something like LWDataTransfer with MapViewer.LWTransfer
    public static DataFlavor makeDataFlavor(Class clazz) {
        
        // We don't generally support serialization yet, so we need to make sure to
        // tag non-supporting flavors as javaVM local only.

        if (java.io.Serializable.class.isAssignableFrom(clazz)) {
            
            return new DataFlavor(clazz, baseClassName(clazz));

        } else {
            
            try {
                return new LocalData(clazz);
            } catch (ClassNotFoundException e) {
                // Should never happen, as we already have a live Class object
                Util.printStackTrace(e);
            }
            
        }
        return null;
    }


    private static final AlphaComposite DisabledIconAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f);
    
    public static final class ProxyEnabledIcon implements javax.swing.Icon
    {
        private final javax.swing.Icon icon;
        private final java.awt.Component trackingEnabled;
        
        public ProxyEnabledIcon(javax.swing.Icon icon, java.awt.Component c) {
            this.icon = icon;
            this.trackingEnabled = c;
        }
        
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            
            if (!trackingEnabled.isEnabled()) {
                if (DEBUG.TOOL) {
                    System.out.println("DISABLED " + trackingEnabled.getClass().getName()
                                       + " disables " + icon
                                       + "; painting on " + c.getClass().getName());
                }
                if (DEBUG.BOXES) {
                    g.setColor(Color.red);
                    g.drawLine(x,y,x+100,y+100);
                }
                ((Graphics2D)g).setComposite(DisabledIconAlpha);
            }

            
            icon.paintIcon(c, g, x, y);
                
        }
        public int getIconWidth() { return icon.getIconWidth(); }
        public int getIconHeight() { return icon.getIconHeight(); }
    }
    
    private static class IconImpl implements Icon {
        final int width, height;
        public IconImpl(int w, int h) {
            width = w;
            height = h;
        }
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            if (DEBUG.BOXES) {
                g.setColor(Color.red);
                g.fillRect(x, y, width, height);
            }
//             if (DEBUG.BOXES) {
//                 g.setColor(Color.gray);
//                 g.drawRect(x, y, width-1, height-1);
//             }
        }
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        public String toString() { return getClass().getSimpleName() + " " + width + "x" + height; }
    }
    
    
    public static final class EmptyIcon extends IconImpl {
        public EmptyIcon(int w, int h) {
            super(w, h);
        }
        public EmptyIcon(Icon icon) {
            this(icon.getIconWidth(), icon.getIconHeight());
        }
    }
    
    public static final class ResizedIcon extends IconImpl {
        final Icon icon;
        public ResizedIcon(Icon icon, int w, int h) {
            super(w, h);
            this.icon = icon;
        }

        public Icon getOriginal() {
            return icon;
        }
        
        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {

            // attempt to center into the desired icon size region

            final int offx = (width - icon.getIconWidth()) / 2;
            final int offy = (height - icon.getIconHeight()) / 2;
            icon.paintIcon(c, g, x + offx, y + offy);
        }
        
    }

    private static class UnicodeIcon extends IconImpl {
        final TextRow glyph;
        final Color color;
        final int xoff, yoff;

        UnicodeIcon(int code, int pointSize, Color c, int width, int height, int x, int y) {
            super(width, height);
            color = c;
            xoff = x;
            yoff = y;
            
            //glyph = new TextRow(new String(new char[ (char) code ]),
            glyph = new TextRow("" + ((char)code),
                                getUnicodeGlyphFont(pointSize));
        }
        
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(color);
            //Log.debug("drawing " + glyph + " at " + x + "," + y + "; +" + xoff + ",+" + yoff);
            glyph.draw(g, x + xoff, y + yoff);
        }
    }

    public static Icon makeUnicodeIcon(int code, int pointSize, Color c, int width, int height, int xoff, int yoff) {
        return new UnicodeIcon(code, pointSize, c, width, height, xoff, yoff);
    }

    

    public static Icon reframeIcon(Icon i, int w, int h) {
        return new ResizedIcon(i, w, h);
    }

    
    public static final Icon EmptyIcon16 = new EmptyIcon(16,16);
    public static final Icon NO_ICON = new EmptyIcon(0,0);
    public static final Insets EmptyInsets = new Insets(0,0,0,0);
        
    private static boolean anyIcons(Component[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof AbstractButton) {
                if (((AbstractButton)items[i]).getIcon() != null)
                    return true;
            }
        }
        return false;
    }
    
    private static void enforceIcons(Component[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof AbstractButton) {
                AbstractButton menuItem = (AbstractButton) items[i];
                if (menuItem.getIcon() == null)
                    menuItem.setIcon(EmptyIcon16);
            }
        }
    }
    
    public static void adjustMenuIcons(javax.swing.JMenu menu) {
        //out("ADJUSTING " + name(menu));
        if (anyIcons(menu.getMenuComponents()))
            enforceIcons(menu.getMenuComponents());
    }
    
    public static void adjustMenuIcons(javax.swing.JPopupMenu menu) {
        //out("ADJUSTING " + name(menu));
        if (anyIcons(menu.getComponents()))
            enforceIcons(menu.getComponents());
    }

    public static JMenu buildMenu(JMenu menu, Action[] actions) {
        addToMenu(menu, actions);
        return menu;
    }

    public static void addToMenu(JPopupMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        adjustMenuIcons(menu);
    }
    public static void addToMenu(JMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        adjustMenuIcons(menu);
    }
    

    public static JMenu buildMenu(String name, Action[] actions) {
        return buildMenu(new JMenu(name), actions);
    }
    
    public static JPopupMenu buildMenu(Action[] actions) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        adjustMenuIcons(menu);
        return menu;

    }

    /**
     * Given a trigger component (such as a label), when mouse is
     * pressed on it, pop the given menu.  Default location is below
     * the given trigger.
     */
    public static class PopupMenuHandler extends tufts.vue.MouseAdapter
        implements javax.swing.event.PopupMenuListener
    {
        private long mLastHidden;
        private JPopupMenu mMenu;
        
        public PopupMenuHandler(Component trigger, JPopupMenu menu)
        {
            trigger.addMouseListener(this);
            menu.addPopupMenuListener(this);
            mMenu = menu;
        }

        public void mousePressed(MouseEvent e) {
            long now = System.currentTimeMillis();
            if (now - mLastHidden > 100)
                showMenu(e.getComponent());
        }

        /** show the menu relative to the given trigger that activated it */
        public void showMenu(Component trigger) {
            mMenu.show(trigger, getMenuX(trigger), getMenuY(trigger));
        }

        /** get menu X location relative to trigger: default is 0 */
        public int getMenuX(Component trigger) { return 0; }
        /** get menu Y location relative to trigger: default is trigger height (places below trigger) */
        public int getMenuY(Component trigger) { return trigger.getHeight(); }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            mLastHidden = System.currentTimeMillis();
            //out("HIDING");
        }
        
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) { /*out("SHOWING");*/ }
        public void popupMenuCanceled(PopupMenuEvent e) { /*out("CANCELING");*/ }
        
        // One gross thing about a pop-up menu is that there's no way to know that it
        // was just hidden by a click on the component that popped it.  That is, if you
        // click on the menu launcher once, you want to pop it, and if you click again,
        // you want to hide it.  But the AWT system autmatically cancels the pop-up as
        // soon as the mouse-press happens ANYWYERE, and even before we'd get a
        // processMouseEvent, so by the time we get this MOUSE_PRESSED, the menu is
        // already hidden, and it looks like we should show it again!  So we have to use
        // a simple timer.
        
    }

    public static Font getUnicodeGlyphFont(int pointSize) {
        if (Util.isWindowsPlatform())
            return new Font("Lucida Sans Unicode", Font.PLAIN, pointSize+4); // this is included in default WinXP installations
        // setFont(new Font("Arial Unicode MS", Font.PLAIN, pointSize+4)); // this is not
        else if (Util.isMacPlatform())
            return new Font("Symbol Regular", Font.PLAIN, pointSize);
        else
            return new Font("Lucida Grande", Font.PLAIN, pointSize);
    }

    /**
     * A class for a providing a label of fixed size to display the given unicode character.
     * A font is selected per-platform for ensuring good results.
     * Todo: can we get a font that will handle this on Linux?  ("Lucida Grande" is the default).
     * The fixed sizes is useful in the case where you want a changeable icon, such as a
     * right-arrow / down-arrow for an open/close icon, but the glyphs are actually different
     * sizes.  This allows you to fix to the size to something that accomodates both (currently
     * you must tune and select that size manually, and use two IconicLabels with the same size).
     */
    public static class IconicLabel extends JLabel implements Icon {
        
        private final Dimension mSize;
	private char icon;
        public IconicLabel(char iconChar, int pointSize, Color color, int width, int height) {
            super(new String(new char[] { iconChar }), JLabel.CENTER);
	    icon = iconChar;
            setFont(getUnicodeGlyphFont(pointSize));
            
            // todo: this may be a problem for Linux: does it have any decent default fonts that
            // include the fancy extended unicode character sets?
            //setFont(new Font("Arial Unicode MS", Font.PLAIN, (int) (((float)pointSize) * 1.3)));

            // todo: the Mac v.s. PC fonts are too different to get
            // this perfect; will need need per platform glyph
            // selection (the unicode char).  Or: could do something
            // crazy like get the bounding box of the character in the
            // available font, and keep scaling the point size until
            // the bounding box matches what we're looking for...
	    
            mSize = new Dimension(width, height);
            setPreferredSize(mSize);
            setMaximumSize(mSize);
            setMinimumSize(mSize);
            setAlignmentX(0.5f);
            if (color != null)
                setForeground(color);
        }

        public IconicLabel(char iconChar, int width, int height) {
            this(iconChar, 16, null, width, height);
        }

        public int getIconWidth() { return mSize.width; }
        public int getIconHeight() { return mSize.height; }
        /*
        public void addNotify() {
            super.addNotify();
            if (DEBUG.BOXES) System.out.println(name(this) + " prefSize=" + super.getPreferredSize());
        }
        */
        
        @Override
        public void paintComponent(Graphics g) {
        	
            // for debug: fill box with color so can see size
            if (DEBUG.BOXES) {
                g.setColor(Color.red);
                g.fillRect(0, 0, mSize.width, mSize.height);
            }
            if (!Util.isMacPlatform())
                ((java.awt.Graphics2D)g).setRenderingHint
                    (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                     java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
           
            /**
             * On Linux, I couldn't find a single font in font.propeties that had the glyph
             * for the arrows so am drawing them instead, and just not using the characters at all.
             */
            if ((!Util.isMacPlatform() && (!Util.isWindowsPlatform())))
                {
                    Graphics2D g2d = (Graphics2D)g;
                    g2d.setColor(this.getForeground());
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    Shape shape = makeTriangle();
                    if (this.getText() == "\u25B8")		   
                        g2d.rotate(Math.PI+Math.PI/2,this.getWidth()/2.0,this.getHeight()/2.0);
                    g2d.fill(shape);
                }
            else
                {
                    super.paintComponent(g);
                }
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (DEBUG.BOXES) {
                g.setColor(Color.red);
                g.fillRect(0, 0, mSize.width, mSize.height);
            }
            //g.drawString(
        }
    

	private static Shape makeTriangle() {
            GeneralPath p = new GeneralPath();
            p.moveTo( 5f, 5f );
            p.lineTo( 11f, 5f );
            p.lineTo( 7.5f, 11f );
            p.closePath();
            return p;
        }        
	  	 
        
        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, mSize.width, mSize.height);
        }
        
        @Override public Dimension getSize() { return mSize; }
        @Override public Dimension getPreferredSize() { return mSize; }
        @Override public Dimension getMaximumSize() { return mSize; }
        @Override public Dimension getMinimumSize() { return mSize; }
        @Override public Rectangle getBounds() { return new Rectangle(getX(), getY(), mSize.width, mSize.height); }
    }


    // unfinished
    public static class PopupMenuButton extends JLabel {
        private JPopupMenu popMenu;
        private long lastHidden;
        private boolean doRollover = false;

        //static final char chevron = 0xBB; // unicode "right-pointing double angle quotation mark"
        //final Color inactiveColor = isMacAqua ? Color.darkGray : Color.lightGray;

        PopupMenuButton(Icon icon, Action[] actions) {
            super(icon);
            //setForeground(inactiveColor);
            //setName(DockWindow.this.getName());
            setFont(new Font("Arial", Font.PLAIN, 18));
            //Insets borderInsets = new Insets(0,0,1,2); // chevron
            Insets borderInsets = new Insets(1,0,0,2);
            if (DEBUG.BOXES) {
                setBorder(new MatteBorder(borderInsets, Color.orange));
                setBackground(Color.red);
                setOpaque(true);
            } else {
                // using an empty border allows mouse over the border gap
                // to also activate us for rollover
                setBorder(new EmptyBorder(borderInsets));
            }

            setMenuActions(actions);
        }

        /*
        PopupMenuButton(char unicodeCharIcon, Action[] actions) {
            this(new GUI.IconicLabel(unicodeCharIcon, 16, 16), actions);
            doRollover = true;
        }
        */

        public void setMenuActions(Action[] actions)
        {
            clearMenuActions();

            new GUI.PopupMenuHandler(this, GUI.buildMenu(actions)) {
                public void mouseEntered(MouseEvent e) {
                    if (doRollover)
                        setForeground(isMacAqua ? Color.black : Color.white);
                }
                public void mouseExited(MouseEvent e) {
                    if (doRollover)
                        setForeground(isMacAqua ? Color.gray : SystemColor.activeCaption.brighter());
                    //setForeground(inactiveColor);
                }
                
                public int getMenuX(Component c) { return c.getWidth(); }
                public int getMenuY(Component c) { return -getY(); } // 0 in parent
            };

            repaint();
        }

        private void clearMenuActions() {
            MouseListener[] ml = getMouseListeners();
            for (int i = 0; i < ml.length; i++) {
                if (ml[i] instanceof GUI.PopupMenuHandler)
                    removeMouseListener(ml[i]);
            }
        }

    }

    

    // add a color to a Font
    public static class Face extends Font {
        public final Color color;
        public final Color bgColor;
        public Face(Font f, Color c) {
            super(f.getName(), f.getStyle(), f.getSize());
            color = c;
            bgColor = null;
        }
        public Face(Font f) {
            this(f, Color.black);
        }
        public Face(String name, int size, Color c) {
            super(name, Font.PLAIN, size);
            color = c;
            bgColor = null;
        }
        public Face(String name, int style, int size, Color c) {
            super(name, style, size);
            color = c;
            bgColor = null;
        }
        public Face(String name, int style, int size, Color c, Color bg) {
            super(name, style, size);
            color = c;
            bgColor = bg;
        }
    }

    public static Border makeSpace(int inset) {
        return makeSpace(inset, inset, inset, inset);
    }
    
    public static Border makeSpace(Insets i) {
        return makeSpace(i.top, i.left, i.bottom, i.right);
    }

    public static Border makeSpace(int t, int l, int b, int r) {
        if (DEBUG.BOXES)
            return new MatteBorder(t,l,b,r, Color.orange);
        else
            return new EmptyBorder(t,l,b,r);
    }
    
    public static void installBorder(javax.swing.text.JTextComponent c) {
        if (isMacAqua) {
            if (c.isEditable())
                c.setBorder(getAquaTextBorder());
        }
    }

    private static Border AquaTextBorder = null;
    private static Border getAquaTextBorder() {
        if (AquaTextBorder != null)
            return AquaTextBorder;
        
        try {
            Class abc = null;

            try {
                abc = Class.forName("apple.laf.AquaTextFieldBorder");
            } catch (ClassNotFoundException e) {
                // java 6 update as of June 2009 (1.6.0_13) will fail on above, but should find this:
                abc = Class.forName("com.apple.laf.AquaTextFieldBorder");
            }
            AquaTextBorder = (javax.swing.border.Border) abc.newInstance();
            if (DEBUG.Enabled) Log.debug("found Mac Aqua text border: " + abc);
        } catch (Throwable t) {
            Log.error("Mac Aqua GUI init problem:", t);
            AquaTextBorder = new LineBorder(Color.blue); // backup debug
            
        }

        return AquaTextBorder;
    }

    /**
     * Potentially set the foreground/background color & font on the given
     * Component, if there are any resource entries for key ending in .font,
     * .foreground or .background
     */
    public static void init(Component c, String key) {
        Color fg = VueResources.getColor(key+".foreground");
        Color bg = VueResources.getColor(key+".background");
        Font font = VueResources.getFont(key+".font");
        if (fg != null)
            c.setForeground(fg);
        if (bg != null)
            c.setBackground(bg);
        if (font != null)
            c.setFont(font);
    }


    public static void apply(Font f, JComponent c) {
        if (f instanceof Face)
            apply((Face)f, c);
        else if (f.getSize() > 0)
            c.setFont(f);
    }

    public static void apply(Face face, JComponent c) {
        
        if (face.getSize() > 0)
            c.setFont(face);

        if (face.color != null)
            c.setForeground(face.color);
        
        if (face.bgColor != null) {
            c.setBackground(face.bgColor);
            c.setOpaque(true);
        } else {
            c.setOpaque(false);
        }
            
    }

    public static void paintNow(JComponent c) {
        c.paintImmediately(0, 0, c.getWidth(), c.getHeight());
    }
    
    public static void flashBackground(JComponent c, Color color) {
        final Color old = c.getBackground();
        c.setBackground(color);
        paintNow(c);
        c.setBackground(old);
    }
    
    public static void setDocumentFont(javax.swing.JTextPane tp, Font f)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        setFontAttributes(a, f);
        StyledDocument doc = tp.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
    }

    private static void setFontAttributes(javax.swing.text.MutableAttributeSet a, Font f)
    {
        StyleConstants.setFontFamily(a, f.getFamily());
        StyleConstants.setFontSize(a, f.getSize());
        StyleConstants.setItalic(a, f.isItalic());
        StyleConstants.setBold(a, f.isBold());
    }
    
    
//     private void setDocumentColor(Color c)
//     {
//         StyleConstants.setForeground(mAttributeSet, c);
//         javax.swing.text.StyledDocument doc = getStyledDocument();
//         doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), mAttributeSet, true);
//     }


    
    public static void setRootPaneNames(RootPaneContainer r, String name) {
        r.getRootPane().setName(name + ".root");
        r.getContentPane().setName(name + ".content");
        r.getLayeredPane().setName(name + ".layer");
        r.getGlassPane().setName(name + ".glass");
    }

    public static String dropName(int dropAction) {
        String name = "";
        if ((dropAction & DnDConstants.ACTION_COPY) != 0) name += "COPY";
        if ((dropAction & DnDConstants.ACTION_MOVE) != 0) {
            if (name.length() > 0)
                name += "+";
            name += "MOVE";
        }
        if ((dropAction & DnDConstants.ACTION_LINK) != 0) {
            if (name.length() > 0)
                name += "+";
            name += "LINK";
        }
        if (name.length() < 1)
            name = "NONE";
        //name += "(0x" + Integer.toHexString(dropAction) + ")";
        return name;
    }

    public static String dragName(DragSourceDragEvent e) {
        return VueUtil.pad(20, baseObjectName(e))
            + "; dropAction=" + dropName(e.getDropAction())
            + "; userAction=" + dropName(e.getUserAction())
            + "; targetActions=" + dropName(e.getTargetActions());
    }

    public static String dragName(DropTargetDragEvent e) {
        return VueUtil.pad(20, baseObjectName(e))
            + "; dropAction=" + dropName(e.getDropAction())
            + "; sourceActions=" + dropName(e.getSourceActions())
            ;
    }
    
    public static String dropName(DropTargetDropEvent e) {
        return baseObjectName(e)
            + "; dropAction=" + dropName(e.getDropAction())
            + "; sourceActions=" + dropName(e.getSourceActions())
            + "; isLocal=" + e.isLocalTransfer()
            ;

    }
    
    public static String dragName(DragSourceDropEvent e) {
        return VueUtil.pad(20, baseObjectName(e)) + "; drop=" + dropName(e.getDropAction()) + "; success=" + e.getDropSuccess();
    }

    // todo: move to DELAYED-init block for GUI called at end of main during caching runs
    private static final int DragSize = 128;
    private static BufferedImage DragImage = new BufferedImage(DragSize, DragSize, BufferedImage.TYPE_INT_ARGB);
    private static Graphics2D DragGraphicsBuf = (Graphics2D) DragImage.getGraphics();
    private static AlphaComposite DragAlpha = AlphaComposite.getInstance(AlphaComposite.SRC, .667f);
    public static final Color TransparentColor = new Color(0,0,0,0);
    
    public static boolean dragImagesSupported()
    {
        // The windows platform, at least as of XP, doesn't support dragging an image,
        // and doing image creation noticably slows down the start of the drag, so
        // we skip it. (Don't know about Linux)
            
        // 2008-04-14: Windows Vista appears capable of drag images (at least Safari on Vista does it),
        // tho don't know if java 6 is wired up to use it yet (first test was not successful).

        return Util.isMacPlatform();
    }

//     public static void startLWCDrag(Component source,
//                                     MouseEvent mouseEvent,
//                                     tufts.vue.LWComponent c)
//     {
//         // TODO: move LWTransfer (or most of it) out of MapViewer to GUI
//         //startLWCDrag(source, mouseEvent, c, new LWTransfer(c));
//         throw new UnsupportedOperationException();
//     }

    public static void startRecognizedDrag(DragGestureEvent e, tufts.vue.LWComponent c)
    {
//         e.startDrag(DragSource.DefaultCopyDrop,
//                     tufts.vue.MapViewer.getTransferableHelper(c));
        
        e.startDrag(DragSource.DefaultCopyDrop,
                    c.getAsImage(0.5, new Dimension(256,256)),
                    new Point(-(int)c.getWidth()/2, -(int)c.getHeight()/2), // image offset
                    tufts.vue.MapViewer.getTransferableHelper(c),
                    null); // drag source listener

    }
    
    public static void startLWCDrag(Component source,
                                    MouseEvent mouseEvent,
                                    tufts.vue.LWComponent c,
                                    Transferable transfer)
    {

        if (true) {

            Image dragImage = null;

            if (dragImagesSupported()) {
                Dimension maxSize = new Dimension(256,256); // bigger for LW drags
                // This can be slow on Window's, and we can't see it anyway
                dragImage = c.getAsImage(0.5, maxSize);
            }
            
            startDrag(source,
                      mouseEvent,
                      dragImage,
                      null,
                      transfer);

        } else {

            // todo: can optimize with a single buffer for all LW
            // drags, but to fully opt, need to do in conjunction with
            // LWTransfer, which because of Sun's DND code, always
            // generates it's image.
    
            DragGraphicsBuf.setColor(TransparentColor);
            DragGraphicsBuf.fillRect(0,0, DragSize,DragSize);
            
            c.drawImage(DragGraphicsBuf, 0.667, new Dimension(DragSize,DragSize), (Color) null, 1.0);
            
            startSystemDrag(source, mouseEvent, DragImage, transfer);
        }
    }

    /**
     * This allows any Component to initiate a system drag without having to be a drag gesture recognizer -- it
     * can simply be initiated from a MouseMotionListener.mouseDragged
     *
     * @param image, if non-null, is scaled to fit a 128x128 size and rendered with a transparency for dragging
     */
    public static void startSystemDrag(Component source, MouseEvent mouseEvent, Image image, Transferable transfer)
    {
        if (DEBUG.DND) out("startSystemDrag: " + transfer);
        
        Point imageOffset = null;

        if (!dragImagesSupported())
            image = null;

        if (image != null) {
            int w = image.getWidth(null);
            int h = image.getHeight(null);

            int max = 0;

            // Allow zoom of up to 2x in case of a very small icon
            final int maxZoom = (w <= 16 ? 2 : 1);

            if (w > h) {
                if (DragSize > h * maxZoom)
                    max = h * maxZoom;
            } else {
                if (DragSize > w * maxZoom)
                    max = w * maxZoom;
            }

            if (max == 0)
                max = DragSize;
            
            final int nw, nh;
            if (w > h) {
                nw = max;
                nh = h * max / w;
            } else {
                nh = max;
                nw = w * max / h;
            }
            
            // todo opt: could just fill the rect below or to right of what we're about to draw
            // FYI, this is a zillion times faster than use Image.getScaledInstance
            DragGraphicsBuf.setClip(null);
            DragGraphicsBuf.setColor(TransparentColor);
            DragGraphicsBuf.fillRect(0,0, DragSize,DragSize);
            DragGraphicsBuf.setComposite(DragAlpha);
            DragGraphicsBuf.drawImage(image, 0, 0, nw, nh, null);
            image = DragImage;
            
            w = nw;
            h = nh;

            imageOffset = new Point(w / -2, h / -2);
        }

        startDrag(source, mouseEvent, image, imageOffset, transfer);

    }

    public static void startRecognizedDrag(DragGestureEvent e, Resource resource, DragSourceListener dsl)
    {
        final Image dragImage = resource.getDragImage();
        int offX = 0, offY = 0;
        if (dragImage != null) {
            offX = -dragImage.getWidth(null) / 2;
            offY = -dragImage.getHeight(null) / 2;
        }
        e.startDrag(DragSource.DefaultCopyDrop, // cursor
                    dragImage, // drag image
                    new Point(offX,offY), // drag image offset
                    new GUI.ResourceTransfer(resource),
                    dsl);  // drag source listener
    }


    private static void startDrag(Component source,
                                  MouseEvent mouseEvent,
                                  Image rawImage,
                                  Point dragOffset,
                                  Transferable transfer)
    {

        if (dragOffset == null) {
            if (rawImage == null) {
                dragOffset = new Point(0,0);
            } else {
                int w = rawImage.getWidth(null);
                int h = rawImage.getHeight(null);

                dragOffset = new Point(w / -2, h / -2);
            }
        }
        
        // this is a coordinate within the component named in DragStub
        Point dragStart = mouseEvent.getPoint();
            
        // the cursor in the DragStub is determining the actual
        // cursor: the action in the DragGestureEvent and the
        // cursror arg to startDrag: don't know what they're doing...
            
        DragGestureEvent trigger = new DragGestureEvent(new DragStub(mouseEvent, source),
                                                        DnDConstants.ACTION_COPY, // preferred action (1 only)
                                                        dragStart, // start point
                                                        Collections.singletonList(mouseEvent));
        trigger
            .startDrag(DragSource.DefaultCopyDrop, // cursor
                       rawImage,
                       dragOffset,
                       transfer,
                       //null,  // drag source listener
                       //MapViewer.this  // drag source listener
                       new GUI.DragSourceAdapter()
                       // is optional when startDrag from DragGestureEvent, but not dragSource.startDrag
                       );        
    }
    
    
        
    /** A relatively empty DragGestureRecognizer just so we can kick off our
     * own drag event. */
    private static class DragStub extends DragGestureRecognizer {
        public DragStub(InputEvent triggerEvent, Component startFrom) {
            super(DragSource.getDefaultDragSource(),
                  startFrom,                    // component (drag start coordinates local to here)
                  DnDConstants.ACTION_COPY |   // alone, prevents drop while command is down, tho shows w/copy cursor better
                  DnDConstants.ACTION_MOVE |
                  DnDConstants.ACTION_LINK
                  ,
                  null);                        // DragGestureListener (can be null)
                super.events.add(triggerEvent);
                //out("NEW DRAGGER");
        }
        protected void registerListeners() { /*out("DRAGGER REGISTER");*/ }
        protected void unregisterListeners() { /*out("DRAGGER UN-REGISTER");*/ }
    }
        
        

    public static class DragSourceAdapter implements DragSourceListener {
    
        public void dragOver(DragSourceDragEvent dsde) {
            if (DEBUG.DND & DEBUG.META) out("dragOver " + dragName(dsde));
        }
        
        public void dragEnter(DragSourceDragEvent dsde) {
            if (DEBUG.DND) out("        dragEnter " + dragName(dsde));
        }
        public void dropActionChanged(DragSourceDragEvent dsde) {
            if (DEBUG.DND) out("dropActionChanged " + dragName(dsde));
        }
        public void dragExit(DragSourceEvent dse) {
            if (DEBUG.DND) out("         dragExit " + dse);
        }
        public void dragDropEnd(DragSourceDropEvent dsde) {
            if (DEBUG.DND) out("      dragDropEnd " + dragName(dsde));
        }
    }



    public static class ResourceTransfer implements Transferable
    {
        private final java.util.List flavors = new java.util.ArrayList(3);
            
        private final java.util.List content;
    
        public ResourceTransfer(tufts.vue.Resource r) {
            content = Collections.singletonList(r);
            flavors.add(DataFlavor.stringFlavor);
            flavors.add(Resource.DataFlavor);
        }

        public ResourceTransfer(java.io.File file) {
            content = Collections.singletonList(file);
            flavors.add(DataFlavor.stringFlavor);
            flavors.add(DataFlavor.javaFileListFlavor);
        }
        
        /** For a list of Files or URLResource's */
        public ResourceTransfer(java.util.List list)
        {
            flavors.add(DataFlavor.stringFlavor);

            final Object first = list.get(0);

            if (first instanceof Resource) {
                flavors.add(Resource.DataFlavor);
                if (first instanceof tufts.vue.URLResource)
                    flavors.add(DataFlavor.javaFileListFlavor);
            } else if (first instanceof java.io.File) {
                flavors.add(DataFlavor.javaFileListFlavor);
            }

            content = list;
        }
        
                    
        /** Returns the array of flavors in which it can provide the data. */
        public synchronized java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            return (DataFlavor[]) flavors.toArray(new DataFlavor[flavors.size()]);
        }
    
        /** Returns whether the requested flavor is supported by this object. */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (flavor == null)
                return false;

            for (int i = 0; i < flavors.size(); i++)
                if (flavor.equals(flavors.get(i)))
                    return true;
        
            return false;
        }
    
        /**
         * String flavor: return single Resource as text or File as text
         * Resource flavor: return a list of Resource's
         * Java file list flavor: return list of either Resources or Files
         */
        public synchronized Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, java.io.IOException
        {
            if (DEBUG.WORK || (DEBUG.DND && DEBUG.META)) System.out.println("ResourceTransfer: getTransferData, flavor=" + flavor);
        
            Object result = null;
        
            if (DataFlavor.stringFlavor.equals(flavor)) {
            
                // Always support something for the string flavor, or
                // we get an exception thrown (even tho I think that
                // may be against the published API).
                Object o = content.get(0);
                if (o instanceof java.io.File) {
                    result = ((java.io.File)o).toString();
                } else if (o instanceof Resource) {
                    // todo: Resource.getReference?  Returns URL if there is one, local package content otherwise
                    java.io.File file = ((Resource)o).getActiveDataFile();
                    if (file != null)
                        result = file.toString();
                    else
                        result = ((Resource)o).getSpec();
                } else {
                    Log.error("getTransferData: unhandled stringFlavor type: " + Util.tags(o));
                    result = o;
                }
            
            } else if (Resource.DataFlavor.equals(flavor)) {
            
                result = content;
            
            } else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            
                result = content;

            } else {
        
                throw new UnsupportedFlavorException(flavor);
            }
        
            if (DEBUG.WORK || (DEBUG.DND && DEBUG.META)) System.out.println("\treturning " + Util.tags(result));

            return result;
        }
    
    }


    public static class MouseWheelRelay implements MouseWheelListener {
        private final MouseWheelListener head, tail;
    
        public MouseWheelRelay(MouseWheelListener head, MouseWheelListener tail) {
            if (head == null || tail == null)
                throw new NullPointerException("MouseWheelRelay: neither head or tail can be null");
            this.head = head;
            this.tail = tail;
        }

        /**
           
         * Intercept MouseWheelEvents going to the nearest JScrollPane ancestor of
         * Component "overriding" by sending them to "intercept" first.  The intercepting
         * component should consume the event for those it wishes to override, otherwise the event
         * will be passed on to the orignal target overriding.
         *
         * If no JScrollPane is found to intercept events on, the intercepting component will
         * simply be added as a standard MouseWheelListener.
         *
         * @return true if an intercept was installed, false if a standard MouseWheelListener was installed
         *
         * BUG: note that the event delivered to the intercepting listener will still
         * have it's x,y coordinates in the coordinate space of the original target, so
         * the interceptor will currently have to check the event source and adjust for
         * any differing coordinate systems manually.
         */

        public static boolean addScrollPaneIntercept(final MouseWheelListener intercepting, java.awt.Component overriding)
        {
            if (overriding instanceof JScrollPane) {
                ; // overriding is already fully discovered
            } else {
                Component nearestScrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, overriding);
                if (nearestScrollPane != null)
                    overriding = nearestScrollPane;
            }
    
            final MouseWheelListener[] currentListeners = overriding.getMouseWheelListeners();
            
            if (DEBUG.MOUSE || DEBUG.INIT || DEBUG.FOCUS || currentListeners.length > 1)
                out("MouseWheelRelay: " + GUI.name(overriding) + ": currentMouseWheelListeners: "
                    + java.util.Arrays.asList(currentListeners)
                    + "; intercepting=" + GUI.name(intercepting));
            
            if (currentListeners == null || currentListeners.length == 0 || currentListeners[0] == null) {
                overriding.addMouseWheelListener(intercepting);
                return false;
            } else {
                overriding.removeMouseWheelListener(currentListeners[0]);
                overriding.addMouseWheelListener(new MouseWheelRelay(intercepting, currentListeners[0]));
                return true;
            }
                
        }

        public void mouseWheelMoved(MouseWheelEvent e) {


            if (DEBUG.FOCUS) Log.debug("MW-HEAD->" + GUI.eventName(e) +
                                       "\n\t" + getClass().getName() + ": INTERCEPTING dispatch: " + Util.tags(head));
            // first, send to the intercept to see if it wants it
            // TODO: can use SwingUtilities.convertMouseEvent to patch coordinate system of intercepted mouse event
            head.mouseWheelMoved(e);

            // if unconsumed by the intercept, send on to the original target we're overriding (usually, a JScrollPane)
            if (!e.isConsumed()) {
                if (DEBUG.FOCUS) Log.debug("MW-TAIL->" + GUI.eventName(e) +
                                           "\n\t" + getClass().getName() + ":     ORIGINAL dispatch: " + Util.tags(tail));
                tail.mouseWheelMoved(e);
            }
        }
    }

    
    

    private static class DefaultMetalTheme extends javax.swing.plaf.metal.DefaultMetalTheme
    {
        private CommonMetalTheme common;
        
        DefaultMetalTheme(CommonMetalTheme common) { this.common = common; }
        
        public FontUIResource getMenuTextFont() { return common.fontMedium;  }
        public FontUIResource getUserTextFont() { return common.fontSmall; }
        public FontUIResource getControlTextFont() { return common.fontControl; }
        
        protected ColorUIResource getSecondary1() { return common.VueSecondary1; }
        protected ColorUIResource getSecondary2() { return common.VueSecondary2; }
        protected ColorUIResource getSecondary3() { return common.VueSecondary3; }

        public void addCustomEntriesToTable(UIDefaults table) {
            super.addCustomEntriesToTable(table);
            common.addCustomEntriesToTable(table);
        }
        
        public String getName() { return super.getName() + " (VUE)"; }
    }

    static class CommonMetalTheme
    {
        // these are gray in Metal Default Theme
        final ColorUIResource VueSecondary1;
        final ColorUIResource VueSecondary2;
        final ColorUIResource VueSecondary3;

        protected FontUIResource fontSmall  = new FontUIResource("SansSerif", Font.PLAIN, 11);
        protected FontUIResource fontMedium = new FontUIResource("SansSerif", Font.PLAIN, 12);

        // controls: labels, buttons, tabs, tables, etc.
        protected FontUIResource fontControl  = new FontUIResource("SansSerif", Font.PLAIN, 12);
        
        CommonMetalTheme() {
            Color VueColor = getVueColor();
            //Color VueColor = new ColorUIResource(200, 221, 242); // #c8ddf2 from OceanTheme
        
            VueSecondary3 = new ColorUIResource(VueColor);
            VueSecondary2 = new ColorUIResource(VueColor.darker());
            VueSecondary1 = new ColorUIResource(VueColor.darker().darker());

            TabbedPaneUI.toolbarColor = GUI.ToolbarColor;
            TabbedPaneUI.vueSecondary2Color = VueSecondary2;
        }

        public void addCustomEntriesToTable(UIDefaults table)
        {
            //table.put("ComboBox.background", Color.white);
            table.put("Button.font", fontSmall);
            table.put("Label.font", fontSmall);
            table.put("TitledBorder.font", fontMedium.deriveFont(Font.BOLD));
            table.put("TabbedPaneUI", "tufts.vue.gui.GUI$TabbedPaneUI");
        }

    }
        



    /**
     * This tweaks the background color of unselected tabs in the tabbed pane,
     * and completely turns off painting any kind of focus indicator.
     */
    public static class TabbedPaneUI extends javax.swing.plaf.metal.MetalTabbedPaneUI {
        static Color toolbarColor;
        static Color vueSecondary2Color;
        
        public static ComponentUI createUI( JComponent x ) {
            if (DEBUG.INIT) System.out.println("Creating GUI.TabbedPaneUI");
            return new TabbedPaneUI();
            
            /*
            return new TabbedPaneUI(new ColorUIResource(Color.blue),
                                    new ColorUIResource(Color.green));
            */
        }
        
        protected void XinstallDefaults() {
            super.installDefaults();
            
            super.tabAreaBackground = Color.blue;
            super.selectColor = Color.green;
            super.selectHighlight = Color.red;

            super.highlight = Color.green;
            super.lightHighlight = Color.magenta;
            super.shadow = Color.black;
            super.darkShadow = Color.black;
            super.focus = Color.orange;
        }


        protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                           Rectangle[] rects, int tabIndex, 
                                           Rectangle iconRect, Rectangle textRect,
                                           boolean isSelected) {}
        
        protected void paintTabBackground( Graphics g, int tabPlacement,
                                           int tabIndex, int x, int y, int w, int h, boolean isSelected ) {
            int slantWidth = h / 2;
            if (isSelected) {
                // in Ocean Theme, this is a ligher blue, so we've overriden it in defaults.
                g.setColor(selectColor);
            } else {
                //g.setColor( tabPane.getBackgroundAt( tabIndex ) );
                Color c = tabPane.getBackgroundAt( tabIndex );
                // for now, allow toolbar color as optional tabbed bg, but all others override
                if (toolbarColor.equals(c))
                    g.setColor(c);
                else
                    g.setColor(vueSecondary2Color);
            }
            switch ( tabPlacement ) {
            case LEFT:
                g.fillRect( x + 5, y + 1, w - 5, h - 1);
                g.fillRect( x + 2, y + 4, 3, h - 4 );
                break;
            case BOTTOM:
                g.fillRect( x + 2, y, w - 2, h - 4 );
                g.fillRect( x + 5, y + (h - 1) - 3, w - 5, 3 );
                break;
            case RIGHT:
                g.fillRect( x + 1, y + 1, w - 5, h - 1);
                g.fillRect( x + (w - 1) - 3, y + 5, 3, h - 5 );
                break;
            case TOP:
            default:
                g.fillRect( x + 4, y + 2, (w - 1) - 3, (h - 1) - 1 );
                g.fillRect( x + 2, y + 5, 2, h - 5 );
            }
        }
    }
        
    

        
    /*
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        out(e);
    }
    */

    // static { Toolkit.getDefaultToolkit().addPropertyChangeListener(new GUI()); }

    private static void out(String s) {
        Log.info(s);
    }

    /*
    private static PropertyChangeHandler PropertyChangeHandler = new PropertyChangeHandler();
    static java.beans.PropertyChangeListener getPropertyChangeHandler() {
        if (PropertyChangeHandler == null)
            PropertyChangeHandler = new PropertyChangeHandler();
        return PropertyChangeHandler;
    }
    static class PropertyChangeHandler implements java.beans.PropertyChangeListener
    {
        private PropertyChangeHandler() {}

        private boolean mIgnoreActionEvents = false;

        public void propertyChange(java.beans.PropertyChangeEvent e) {
            if (DEBUG.Enabled) out("propertyChange: " + e);
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (mIgnoreActionEvents) {
                if (DEBUG.TOOL) System.out.println(this + " ActionEvent ignored: " + e);
            } else {
                if (DEBUG.TOOL) System.out.println(this + " actionPerformed " + e);
                //fireFontChanged(null, makeFont());
            }
        }
    }
    */


    private GUI() {}


    public static void main(String args[]) {

        // Can't find any property names on Mac OS X -- are there any?
        // Is it possible to get an update when the screen DisplayMode changes?
        
        String propName = null;
        if (args.length > 0)
            propName = args[0];
        if (propName == null)
            propName = "win.propNames";

        //new java.awt.Frame("A Frame").show();

        Object propValue = Toolkit.getDefaultToolkit().getDesktopProperty(propName);

        System.out.println("Property " + propName + " = " + propValue);

        if (propValue != null && propValue instanceof String[]) {
            System.out.println("Supported windows property names:");
            String[] propNames = (String[]) propValue;
            for (int i = 0; i < propNames.length; i++) {
                Object value = Toolkit.getDefaultToolkit().getDesktopProperty(propNames[i]);
                System.out.println(propNames[i] + " = " + value);
            }
        }
    }

    
        
    
    
}