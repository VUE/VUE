package tufts.vue;

import java.awt.Container;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;

public class ZoomTool extends VueTool
{
    static private final int ZOOM_MANUAL = -1;
    static private final double[] ZoomDefaults = {
        1.0/32, 1.0/24, 1.0/16, 1.0/12, 1.0/8, 1.0/6, 1.0/5, 1.0/4, 1.0/3, 1.0/2, 2.0/3, 0.75,
        1.0,
        1.25, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64
        //, 96, 128, 256, 384, 512
    };
    static private final int ZOOM_FIT_PAD = 16;
    static private final double MaxZoom = ZoomDefaults[ZoomDefaults.length - 1];

    
    public ZoomTool() {
        super();
    }
	
    public JPanel getContextualPanel() {
		return VueToolbarController.getController().getSuggestedContextualPanel();
    }

    public void handleSelection() {
    }
	
    private static final Color SelectorColor = Color.red;
    private static final Color SelectorColorInverted = new Color(0,255,255); // inverse of red
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        /*
        if (VueUtil.isMacPlatform())
            g.setXORMode(SelectorColorInverted);
        else
            g.setXORMode(SelectorColor);
        */
        g.setColor(Color.red);
        super.drawSelector(g, r);
    }

    public boolean usesRightClick()
    {
        return true;
    }

    public boolean isZoomOutMode()
    {
        return getSelectedSubTool().getID().equals("zoomTool.zoomOut");
    }

    public boolean supportsSelection() { return false; }

    public boolean supportsDraggedSelector(MouseEvent e)
    {
        // todo: take a map mouse event, and if zoom level on viewer == MaxZoom, return false
        
        // This is so that if they RIGHT click, the dragged selector doesn't appear --
        // because right click in zoom does a zoom out, and it makes less sense to
        // zoom out on a particular region.
        // Need to recognize button 1 on a drag, where getButton=0, or a release, where modifiers 0 but getButton=1
        return !isZoomOutMode() && (e.getButton() == MouseEvent.BUTTON1 || (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0);
    }
    
    public boolean handleMouseReleased(MapMouseEvent e)
    {
        System.out.println(this + " handleMouseReleased " + e);

        Point p = e.getPoint();
        
        if (e.isShiftDown() || e.getButton() != MouseEvent.BUTTON1
            //|| toolKeyEvent != null && toolKeyEvent.isShiftDown()
            ) {
            if (isZoomOutMode())
                setZoomBigger(p);
            else
                setZoomSmaller(p);
        } else {
            Rectangle box = e.getSelectorBox();
            if (box != null && box.width > 10 && box.height > 10) {
                setZoomFitRegion(e.getMapSelectorBox());
            } else {
                if (isZoomOutMode())
                    setZoomSmaller(p);
                else
                    setZoomBigger(p);
            }
        }
        return true;
    }
    

    /*
        if (e.isShiftDown() || e.getButton() != MouseEvent.BUTTON1
            || toolKeyEvent != null && toolKeyEvent.isShiftDown()
            ) {
            setZoomPoint(e.getPoint());
            if (ZoomTool.isZoomOutMode())
                setZoomBigger();
            else
                setZoomSmaller();
        } else {
            if (draggedSelectorBox != null &&
                draggedSelectorBox.getWidth() > 10 && draggedSelectorBox.getHeight() > 10) {
                setZoomFitRegion(screenToMapRect(draggedSelectorBox));
            } else {
                setZoomPoint(e.getPoint());
                if (ZoomTool.isZoomOutMode())
                    setZoomSmaller();
                else
                    setZoomBigger();
            }
        }
    */
    
    public boolean handleKeyPressed(KeyEvent e){return false;}
    
    public static boolean setZoomBigger(Point focus)
    {
        double curZoom = VUE.getActiveViewer().getZoomFactor();
        for (int i = 0; i < ZoomDefaults.length; i++) {
            if (ZoomDefaults[i] > curZoom) {
                setZoom(ZoomDefaults[i], focus);
                return true;
            }
        }
        return false;
    }
    
    public static boolean setZoomSmaller(Point focus)
    {
        double curZoom = VUE.getActiveViewer().getZoomFactor();
        for (int i = ZoomDefaults.length - 1; i >= 0; i--) {
            if (ZoomDefaults[i] < curZoom) {
                setZoom(ZoomDefaults[i], focus);
                return true;
            }
        }
        return false;
    }

    public static void setZoom(double zoomFactor)
    {
        setZoom(zoomFactor, true, null);
    }
    public static void setZoom(double zoomFactor, Point focus)
    {
        setZoom(zoomFactor, true, focus);
    }
    
    private static void setZoom(double newZoomFactor, boolean adjustViewport, Point focus)
    {
        MapViewer viewer = VUE.getActiveViewer();
        
        if (adjustViewport) {
            if (focus == null) {
                // If no user selected zoom focus point, zoom in to
                // towards the map location at the center of the
                // viewport.
                Container c = viewer;
                focus = new Point(c.getWidth() / 2, c.getHeight() / 2);
            }
            Point2D mapAnchor = viewer.screenToMapPoint(focus);
            double offsetX = (mapAnchor.getX() * newZoomFactor) - focus.getX();
            double offsetY = (mapAnchor.getY() * newZoomFactor) - focus.getY();
            viewer.setMapOriginOffset(offsetX, offsetY);
        }
        
        viewer.setZoomFactor(newZoomFactor);
        
    }
    
    /** fit everything in the current map into the current viewport */
    public static void setZoomFit()
    {
        setZoomFitRegion(VUE.getActiveViewer().getMap().getBounds(),
                         ZOOM_FIT_PAD);
    }
    
    public static void setZoomFitRegion(Rectangle2D mapRegion)
    {
        setZoomFitRegion(mapRegion, 0);
    }
    
    public static void setZoomFitRegion(Rectangle2D mapRegion, int edgePadding)
    {
        Point2D.Double offset = new Point2D.Double();
        MapViewer viewer = VUE.getActiveViewer();
        double newZoom = computeZoomFit(viewer.getSize(),
                                        edgePadding,
                                        mapRegion,
                                        offset);
        if (newZoom > MaxZoom) {
            setZoom(MaxZoom, true, null);
            Point2D mapAnchor = new Point2D.Double(mapRegion.getCenterX(), mapRegion.getCenterY());
            Point focus = new Point(viewer.getWidth()/2, viewer.getHeight()/2);
            double offsetX = (mapAnchor.getX() * MaxZoom) - focus.getX();
            double offsetY = (mapAnchor.getY() * MaxZoom) - focus.getY();
            viewer.setMapOriginOffset(offsetX, offsetY);
        } else {
            setZoom(newZoom, false, null);
            viewer.setMapOriginOffset(offset.getX(), offset.getY());
        }
    }
    
    /*
     * Compute two items: the zoom factor that will fit
     * everything within the given bounds into the given
     * viewport, and put into @param offset the offset
     * to place the viewport at. Used to figure out how
     * to fit everything within a map on the screen and
     * where to pan to so you can see it all.
     */
    public static double computeZoomFit(java.awt.Dimension viewport,
                                        int borderGap,
                                        java.awt.geom.Rectangle2D bounds,
                                        java.awt.geom.Point2D offset)
    {
        int viewWidth = viewport.width - borderGap * 2;
        int viewHeight = viewport.height - borderGap * 2;
        double vertZoom = (double) viewHeight / bounds.getHeight();
        double horzZoom = (double) viewWidth / bounds.getWidth();
        boolean centerVertical;
        double newZoom;
        if (horzZoom < vertZoom) {
            newZoom = horzZoom;
            centerVertical = true;
        } else {
            newZoom = vertZoom;
            centerVertical = false;
        }

        // Now center the components within the dimension
        // that had extra room to scale in.
                    
        double offsetX = bounds.getX() * newZoom - borderGap;
        double offsetY = bounds.getY() * newZoom - borderGap;

        if (centerVertical)
            offsetY -= (viewHeight - bounds.getHeight()*newZoom) / 2;
        else // center horizontal
            offsetX -= (viewWidth - bounds.getWidth()*newZoom) / 2;

        offset.setLocation(offsetX, offsetY);
        return newZoom;
    }
    
}
