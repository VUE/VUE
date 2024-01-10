package tufts.vue.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.beans.*;
import javax.activation.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class DnDTabbedPane extends JTabbedPane {
    private static final int LINEWIDTH = 3;
    private static final String NAME = "test";
    private final GhostGlassPane glassPane = new GhostGlassPane();
    private final Rectangle lineRect  = new Rectangle();
    private final Color     lineColor = new Color(0, 100, 255);
    private int dragTabIndex = -1;

    private void clickArrowButton(String actionKey) {
        ActionMap map = getActionMap();
        if(map != null) {
            Action action = map.get(actionKey);
            if (action != null && action.isEnabled()) {
                action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
            }
        }
    }
    private static Rectangle rBackward = new Rectangle();
    private static Rectangle rForward  = new Rectangle();
    private static int rwh = 20;
    private static int buttonsize = 30; //xxx magic number of scroll button size
    private void autoScrollTest(Point glassPt) {
        Rectangle r = getTabAreaBounds();
        int tabPlacement = getTabPlacement();
        if(tabPlacement==TOP || tabPlacement==BOTTOM) {
            rBackward.setBounds(r.x, r.y, rwh, r.height);
            rForward.setBounds(r.x+r.width-rwh-buttonsize, r.y, rwh+buttonsize, r.height);
        }else if(tabPlacement==LEFT || tabPlacement==RIGHT) {
            rBackward.setBounds(r.x, r.y, r.width, rwh);
            rForward.setBounds(r.x, r.y+r.height-rwh-buttonsize, r.width, rwh+buttonsize);
        }
        if(rBackward.contains(glassPt)) {
            //System.out.println(new java.util.Date() + "Backward");
            clickArrowButton("scrollTabsBackwardAction");
        }else if(rForward.contains(glassPt)) {
            //System.out.println(new java.util.Date() + "Forward");
            clickArrowButton("scrollTabsForwardAction");
        }
    }
    public DnDTabbedPane() {
        super();
        final DragSourceListener dsl = new DragSourceListener() {
            public void dragEnter(DragSourceDragEvent e) {
                e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
            }
            public void dragExit(DragSourceEvent e) {
                e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
                lineRect.setRect(0,0,0,0);
                glassPane.setPoint(new Point(-1000,-1000));
                glassPane.repaint();
            }
            public void dragOver(DragSourceDragEvent e) {
                Point glassPt = e.getLocation();
                SwingUtilities.convertPointFromScreen(glassPt, glassPane);
                int targetIdx = getTargetTabIndex(glassPt);
                //if(getTabAreaBounds().contains(tabPt) && targetIdx>=0 &&
                if(getTabAreaBounds().contains(glassPt) && targetIdx>=0 &&
                   targetIdx!=dragTabIndex && targetIdx!=dragTabIndex+1) {
                    e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
                    glassPane.setCursor(DragSource.DefaultMoveDrop);
                }else{
                    e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
                    glassPane.setCursor(DragSource.DefaultMoveNoDrop);
                }
            }
            public void dragDropEnd(DragSourceDropEvent e) {
                lineRect.setRect(0,0,0,0);
                dragTabIndex = -1;
                glassPane.setVisible(false);
                if(hasGhost()) {
                    glassPane.setVisible(false);
                    glassPane.setImage(null);
                }
            }
            public void dropActionChanged(DragSourceDragEvent e) {}
        };
        final Transferable t = new Transferable() {
            private final DataFlavor FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME);
            public Object getTransferData(DataFlavor flavor) {
                return DnDTabbedPane.this;
            }
            public DataFlavor[] getTransferDataFlavors() {
                DataFlavor[] f = new DataFlavor[1];
                f[0] = this.FLAVOR;
                return f;
            }
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.getHumanPresentableName().equals(NAME);
            }
        };
        final DragGestureListener dgl = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent e) {
                if(getTabCount()<=1) return;
                Point tabPt = e.getDragOrigin();
                dragTabIndex = indexAtLocation(tabPt.x, tabPt.y);
                //"disabled tab problem".
                if(dragTabIndex<0 || !isEnabledAt(dragTabIndex)) return;
                initGlassPane(e.getComponent(), e.getDragOrigin());
                try{
                    e.startDrag(DragSource.DefaultMoveDrop, t, dsl);
                }catch(InvalidDnDOperationException idoe) {
                    idoe.printStackTrace();
                }
            }
        };
        new DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, new CDropTargetListener(), true);
        new DragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
    }

    class CDropTargetListener implements DropTargetListener{
        public void dragEnter(DropTargetDragEvent e) {
            if(isDragAcceptable(e)) e.acceptDrag(e.getDropAction());
            else e.rejectDrag();
        }
        public void dragExit(DropTargetEvent e) {}
        public void dropActionChanged(DropTargetDragEvent e) {}

        private Point pt_ = new Point();
        public void dragOver(final DropTargetDragEvent e) {
            Point pt = e.getLocation();
            if(getTabPlacement()==JTabbedPane.TOP || getTabPlacement()==JTabbedPane.BOTTOM) {
                initTargetLeftRightLine(getTargetTabIndex(pt));
            }else{
                initTargetTopBottomLine(getTargetTabIndex(pt));
            }
            if(hasGhost()) {
                glassPane.setPoint(pt);
            }
            if(!pt_.equals(pt)) glassPane.repaint();
            pt_ = pt;
            autoScrollTest(pt);
        }

        public void drop(DropTargetDropEvent e) {
            if(isDropAcceptable(e)) {
                convertTab(dragTabIndex, getTargetTabIndex(e.getLocation()));
                e.dropComplete(true);
            }else{
                e.dropComplete(false);
            }
            repaint();
        }
        public boolean isDragAcceptable(DropTargetDragEvent e) {
            Transferable t = e.getTransferable();
            if(t==null) return false;
            DataFlavor[] f = e.getCurrentDataFlavors();
            if(t.isDataFlavorSupported(f[0]) && dragTabIndex>=0) {
                return true;
            }
            return false;
        }
        public boolean isDropAcceptable(DropTargetDropEvent e) {
            Transferable t = e.getTransferable();
            if(t==null) return false;
            DataFlavor[] f = t.getTransferDataFlavors();
            if(t.isDataFlavorSupported(f[0]) && dragTabIndex>=0) {
                return true;
            }
            return false;
        }
    }

    private boolean hasGhost = true;
    public void setPaintGhost(boolean flag) {
        hasGhost = flag;
    }
    public boolean hasGhost() {
        return hasGhost;
    }
    private boolean isPaintScrollArea = true;
    public void setPaintScrollArea(boolean flag) {
        isPaintScrollArea = flag;
    }
    public boolean isPaintScrollArea() {
        return isPaintScrollArea;
    }

    private int getTargetTabIndex(Point glassPt) {
        Point tabPt = SwingUtilities.convertPoint(glassPane, glassPt, DnDTabbedPane.this);
        boolean isTB = getTabPlacement()==JTabbedPane.TOP || getTabPlacement()==JTabbedPane.BOTTOM;
        for(int i=0;i<getTabCount();i++) {
            Rectangle r = getBoundsAt(i);
            if(isTB) r.setRect(r.x-r.width/2, r.y,  r.width, r.height);
            else     r.setRect(r.x, r.y-r.height/2, r.width, r.height);
            if(r.contains(tabPt)) return i;
        }
        Rectangle r = getBoundsAt(getTabCount()-1);
        if(isTB) r.setRect(r.x+r.width/2, r.y,  r.width, r.height);
        else     r.setRect(r.x, r.y+r.height/2, r.width, r.height);
        return   r.contains(tabPt)?getTabCount():-1;
    }
    private void convertTab(int prev, int next) {
        if(next<0 || prev==next) {
            return;
        }
        Component cmp = getComponentAt(prev);
      //  Component tab = getTabComponentAt(prev);
        String str    = getTitleAt(prev);
        Icon icon     = getIconAt(prev);
        String tip    = getToolTipTextAt(prev);
        boolean flg   = isEnabledAt(prev);
        int tgtindex  = prev>next ? next : next-1;
        remove(prev);
        insertTab(str, icon, cmp, tip, tgtindex);
        setEnabledAt(tgtindex, flg);
        //When you drag'n'drop a disabled tab, it finishes enabled and selected.
        //pointed out by dlorde
        if(flg) setSelectedIndex(tgtindex);

        //I have a component in all tabs (jlabel with an X to close the tab) and when i move a tab the component disappear.
        //pointed out by Daniel Dario Morales Salas
   //     setTabComponentAt(tgtindex, tab);
    }

    private void initTargetLeftRightLine(int next) {
        if(next<0 || dragTabIndex==next || next-dragTabIndex==1) {
            lineRect.setRect(0,0,0,0);
        }else if(next==0) {
            Rectangle r = SwingUtilities.convertRectangle(this, getBoundsAt(0), glassPane);
            lineRect.setRect(r.x-LINEWIDTH/2,r.y,LINEWIDTH,r.height);
        }else{
            Rectangle r = SwingUtilities.convertRectangle(this, getBoundsAt(next-1), glassPane);
            lineRect.setRect(r.x+r.width-LINEWIDTH/2,r.y,LINEWIDTH,r.height);
        }
    }
    private void initTargetTopBottomLine(int next) {
        if(next<0 || dragTabIndex==next || next-dragTabIndex==1) {
            lineRect.setRect(0,0,0,0);
        }else if(next==0) {
            Rectangle r = SwingUtilities.convertRectangle(this, getBoundsAt(0), glassPane);
            lineRect.setRect(r.x,r.y-LINEWIDTH/2,r.width,LINEWIDTH);
        }else{
            Rectangle r = SwingUtilities.convertRectangle(this, getBoundsAt(next-1), glassPane);
            lineRect.setRect(r.x,r.y+r.height-LINEWIDTH/2,r.width,LINEWIDTH);
        }
    }

    private void initGlassPane(Component c, Point tabPt) {
        getRootPane().setGlassPane(glassPane);
        if(hasGhost()) {
            Rectangle rect = getBoundsAt(dragTabIndex);
            BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            c.paint(g);
            rect.x = rect.x<0?0:rect.x;
            rect.y = rect.y<0?0:rect.y;
            image = image.getSubimage(rect.x,rect.y,rect.width,rect.height);
            glassPane.setImage(image);
        }
        Point glassPt = SwingUtilities.convertPoint(c, tabPt, glassPane);
        glassPane.setPoint(glassPt);
        glassPane.setVisible(true);
    }

    private Rectangle getTabAreaBounds() {
        Rectangle tabbedRect = getBounds();
        //pointed out by daryl. NullPointerException: i.e. addTab("Tab",null)
        //Rectangle compRect   = getSelectedComponent().getBounds();
        Component comp = getSelectedComponent();
        int idx = 0;
        while(comp==null && idx<getTabCount()) comp = getComponentAt(idx++);
        Rectangle compRect = (comp==null)?new Rectangle():comp.getBounds();
        int tabPlacement = getTabPlacement();
        if(tabPlacement==TOP) {
            tabbedRect.height = tabbedRect.height - compRect.height;
        }else if(tabPlacement==BOTTOM) {
            tabbedRect.y = tabbedRect.y + compRect.y + compRect.height;
            tabbedRect.height = tabbedRect.height - compRect.height;
        }else if(tabPlacement==LEFT) {
            tabbedRect.width = tabbedRect.width - compRect.width;
        }else if(tabPlacement==RIGHT) {
            tabbedRect.x = tabbedRect.x + compRect.x + compRect.width;
            tabbedRect.width = tabbedRect.width - compRect.width;
        }
        tabbedRect.grow(2, 2);
        return tabbedRect;
    }
    class GhostGlassPane extends JPanel {
        private final AlphaComposite composite;
        private Point location = new Point(0, 0);
        private BufferedImage draggingGhost = null;
        public GhostGlassPane() {
            setOpaque(false);
            composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
            //http://bugs.sun.com/view_bug.do?bug_id=6700748
            //setCursor(null);
        }
        public void setImage(BufferedImage draggingGhost) {
            this.draggingGhost = draggingGhost;
        }
        public void setPoint(Point location) {
            this.location = location;
        }
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(composite);
            if(isPaintScrollArea() && getTabLayoutPolicy()==SCROLL_TAB_LAYOUT) {
                g2.setPaint(Color.RED);
                g2.fill(rBackward);
                g2.fill(rForward);
            }
            if(draggingGhost != null) {
                double xx = location.getX() - (draggingGhost.getWidth(this) /2d);
                double yy = location.getY() - (draggingGhost.getHeight(this)/2d);
                g2.drawImage(draggingGhost, (int)xx, (int)yy , null);
            }
            if(dragTabIndex>=0) {
                g2.setPaint(lineColor);
                g2.fill(lineRect);
            }
        }
    }
}
