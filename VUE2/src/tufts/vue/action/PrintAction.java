/*
 * PrintAction.java
 *
 * Created on June 6, 2003, 12:12 PM
 */

package tufts.vue.action;

import tufts.vue.*;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.*;
import java.awt.print.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;


/**
 * @author Daisuke Fujiwara
 * @author Scott Fraize
 * @version March 2004
 */

/**A class which handles the printing task of the currently active map*/
public class PrintAction extends Actions$VueAction implements Printable, Runnable
{
    private static PrintAction singleton;
    public static PrintAction getPrintAction() {
        if (singleton == null)
            singleton = new PrintAction();
        return singleton;
    }

    private boolean isPrintUnderway;
    private boolean isPrintingView;
    private String jobName;
    
    private PrintAction() {
        super("Print...");
    }

    private boolean isPrintingView() {
        return isPrintingView;
    }

    public int print(Graphics graphics, PageFormat format, int pageIndex)
        throws java.awt.print.PrinterException
    {
        if (pageIndex > 0) {
            out("page " + pageIndex + " requested, ending print job.");
            return Printable.NO_SUCH_PAGE;
        }

        out("asked to render page " + pageIndex + " in " + out(format));
        final MapViewer viewer = VUE.getActiveViewer();
        final LWMap map = viewer.getMap();
        final Rectangle2D bounds;

        if (isPrintingView())
            bounds = viewer.getVisibleMapBounds();
        else
            bounds = map.getBounds(); // todo: need get visible component bounds

        if (bounds.isEmpty()) {
            out("Empty page: skipping print.");
            return Printable.NO_SUCH_PAGE;
        }
            
        Dimension page = new Dimension((int) format.getImageableWidth() - 1,
                                       (int) format.getImageableHeight() - 1);

        out("requested map bounds: " + bounds);
        
        Graphics2D g2 = (Graphics2D) graphics;
        
        g2.translate(format.getImageableX(), format.getImageableY());

        // draw border outline of page
        g2.setColor(Color.gray);
        g2.setStroke(VueConstants.STROKE_ONE);
        g2.drawRect(0, 0, page.width, page.height);
            
        // compute zoom & offset for visible map components
        Point2D offset = new Point2D.Double();
        double scale = ZoomTool.computeZoomFit(page, 0, bounds, offset);
        g2.translate(-offset.getX(), -offset.getY());
        g2.scale(scale,scale);
        // set up the DrawContext
        DrawContext dc = new DrawContext(g2, scale);
        dc.setPrinting(true);
        dc.setAntiAlias(true);
        // render the map
        map.draw(dc);
          
        out("page " + pageIndex + " rendered.");
        return Printable.PAGE_EXISTS;
    }


    private PrinterJob printerJob;
    private PrinterJob getPrinterJob()
    {
        //if (printerJob == null)
            printerJob = PrinterJob.getPrinterJob();
        return printerJob;
    }
    
    private PageFormat pageFormat;
    private PageFormat getPageFormat(PrinterJob job)
    {
        if (pageFormat == null) {
            pageFormat = job.defaultPage();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
        }
        return pageFormat;
    }
    /**
     * Run the system page format dialog using the last used
     * PageFormat as the default setup.
     */
    private PageFormat getPageFormatInteractive(PrinterJob job)
    {
        return pageFormat = job.pageDialog(getPageFormat(job));
    }
    
    /** If there's anything to print, initiate a print job */
    public synchronized void actionPerformed(ActionEvent ae)
    {
        if (isPrintUnderway) {
            out("ignoring [" + ae.getActionCommand() + "]; print already underway.");
            return;
        }
        this.jobName = VUE.getActiveMap().getDisplayLabel();
        if (VUE.getActiveMap().isEmpty()) {
            out("nothing to print");
            return;
        }
        isPrintUnderway = true;
        setEnabled(false);
        isPrintingView = ae.getActionCommand().indexOf("View") >= 0;
        new Thread(this).start();
    }

    public void run()
    {
        try {
            out("print thread active");
            PrinterJob job = getPrinterJob();
            if (job.printDialog()) {
                try {
                    PageFormat format = getPageFormatInteractive(job);
                    out("format: " + out(format));
                    job.setJobName(this.jobName);
                    job.setPrintable(this, format);
                    job.print();
                } catch (Exception ex) {
                    out("print exception:" + ex);
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            out("print dialog exception: " + e);
        } finally {
            isPrintUnderway = false;
        }
        setEnabled(true);
        out("print thread complete");
    }

    private void out(String s) {
        System.out.println("PrintAction[" + jobName + "] " + s);
    }

    private static String out(PageFormat p) {
        if (p == null) return null;
        final String[] o = {"LANDSCAPE", "PORTRAIT", "REVERSE LANDSCAPE"};
        return
            "PageFormat[" + o[p.getOrientation()]
            + " " + p.getImageableX() + "," + p.getImageableY()
            + " " + p.getImageableWidth() + "x" + p.getImageableHeight();
    }




    
    public int OLD_print(Graphics graphics, PageFormat page, int pageIndex)
        throws java.awt.print.PrinterException
    {
        MapViewer viewer = VUE.getActiveViewer();
        LWMap map = VUE.getActiveMap();
        // will need to translate GC to origin offset of map: printing starts at coords 0,0 (of course)
        Rectangle2D bounds = map.getBounds();
        int xLocation = (int)bounds.getX() + 5, yLocation = (int)bounds.getY() + 5;
        Dimension view = new Dimension((int)bounds.getWidth() + xLocation, (int)bounds.getHeight() + yLocation);
        int totalPages;
        
        //calcuates the total page number using the component view and the paper view
        if (page.getOrientation() == PageFormat.PORTRAIT)
            totalPages = (int)Math.ceil((view.getHeight() / page.getImageableHeight()));
          
        else
            totalPages = (int)Math.ceil((view.getWidth() / page.getImageableWidth()));
         
        //if the page number is less than one, make it one
        //maybe not necessary?
        if (totalPages <= 1)
            totalPages = 1;
        
        System.out.println("total page number is " + totalPages);
        
        //printing pages
        if(pageIndex < totalPages)
            {
                Graphics2D g2 = (Graphics2D)graphics;
                int xClip = 0, yClip = 0;
                double xTranslation, yTranslation;
            
                if (page.getOrientation() == PageFormat.LANDSCAPE)
                    {
                        xClip = (int)page.getImageableWidth() * pageIndex;
                        xTranslation = page.getImageableX() - xClip;
                
                        System.out.println("xClip is " + xClip);
                        System.out.println("xTranslation is " + xTranslation);
                
                        //translate it to the printable section of the paper
                        g2.translate(xTranslation, page.getImageableY());
                        g2.setClip(xClip, 0, (int)page.getImageableWidth() - 1, (int)page.getImageableHeight() - 1);
                    }
            
                else
                    {
                        //translate it to the printable section of the paper
                        g2.translate(page.getImageableX(), page.getImageableY());
                        g2.setClip(0, 0, (int)page.getImageableWidth() - 1, (int)page.getImageableHeight() - 1);
                    }
                final double scale = 0.5;
                g2.scale(scale,scale);
                //printing the currently selected map using the given graphics object
                //viewer.paintComponent(g2);
                DrawContext dc = new DrawContext(g2);
                dc.setPrinting(true);
                dc.setAntiAlias(true);
                map.draw(dc);
                g2.scale(1/scale,1/scale);
          
                //border outline of the map
                g2.setColor(Color.black);
                g2.drawRect(xClip, yClip,
                            (int)page.getImageableWidth() - 2,
                            (int)page.getImageableHeight() - 2);
            
                return Printable.PAGE_EXISTS;
            }
        
        //if it is an out of bound page number then the page doesn't exist
        else 
            return Printable.NO_SUCH_PAGE;
    }
    
    
    
}
