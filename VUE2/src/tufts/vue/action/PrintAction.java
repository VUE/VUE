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
 * PrintAction and inner class PrintJob.  PrintAction handles figuring
 * out the print style we want (whole map or just visible view) and
 * creating a PrintJob to handle it, as well as caching the last used
 * PageFormat.  PrintJob handles the actual printing.  PrintJob can be
 * threaded, tho this is currently disabled due to java bugs that can
 * leave the VUE app raised over the print dialogs sometimes.  This
 * means that VUE can't repaint itself while the print dialogs are
 * active (not true on Mac OS X, but true at least on W2K/JVM1.4.2).
 * 
 * @version $Revision: 1.46 $ / $Date: 2009-08-28 17:13:05 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class PrintAction extends tufts.vue.VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PrintAction.class);

    private static PrintAction singleton;
    public static PrintAction getPrintAction() {
        if (singleton == null)
            singleton = new PrintAction(VueResources.getString("menu.file.printdot"));
        return singleton;
    }

    private boolean isPrintUnderway;
    
    private PrintAction(String label) {
        super(label, null, ":general/Print");
    }
    // either make private or get rid of getPrintAction singleton
    public PrintAction() {
        this("Print");
    }

    private PrinterJob printerJob;
    private PrinterJob getPrinterJob()
    {
        //if (printerJob == null)
            printerJob = PrinterJob.getPrinterJob();
        return printerJob;
    }
    
    private PageFormat pageFormat;
    private PageFormat getPageFormat(PrinterJob job, Rectangle2D bounds)
    {
        if (pageFormat == null) {
            pageFormat = job.defaultPage();
            // todo: keep a list of print formats for each map?
            if (bounds.getWidth() > bounds.getHeight())
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
            else
                pageFormat.setOrientation(PageFormat.PORTRAIT);
        }
        return pageFormat;
    }
    /**
     * Run the system page format dialog using the last used
     * PageFormat as the default setup.  If user hits cancel
     * on the dialog, null is returned.
     */
    private PageFormat getPageFormatInteractive(PrinterJob job, Rectangle2D bounds)
    {
        PageFormat initial = getPageFormat(job, bounds);
        PageFormat result = job.pageDialog(initial);
        if (result != initial)
            return pageFormat = result;
        else
            return null; // dialog was canceled
    }
    
    /** If there's anything to print, initiate a print job */
    public synchronized void actionPerformed(ActionEvent ae)
    {
        if (isPrintUnderway) {
            out("ignoring [" + ae.getActionCommand() + "]; print already underway.");
            return;
        }
        LWMap map = VUE.getActiveMap();
        Rectangle2D bounds = map.getBounds();
        if (bounds.isEmpty()) {
            out("nothing to print in " + map);
            return;
        }
        boolean viewerPrint = false;
        if (ae.getActionCommand() != null &&
            ae.getActionCommand().indexOf("Visible") >= 0)
            viewerPrint = true;

        // if any tool windows open in W2K/1.4.2 when start this thread,
        // the print dialog get's obscured!
        
        isPrintUnderway = true;
        setEnabled(false);
        try {
            new PrintJob(VUE.getActiveViewer(), viewerPrint).start();
//             if (DEBUG.Enabled)
//                 new PrintJob(VUE.getActiveViewer(), viewerPrint).start();
//             else
//                 new PrintJob(VUE.getActiveViewer(), viewerPrint).runPrint();
        } catch (Throwable t) {
            out("exception creating or running PrintJob: " + t);
            t.printStackTrace();
            isPrintUnderway = false;
            setEnabled(true);
        }
    }

    private static volatile int JobCount = 1;

    private class PrintJob extends Thread implements Printable
    {
        private LWMap map;
        private LWComponent focal;
        private String jobName;
        private boolean isPrintingView;
        private Rectangle2D bounds; // map bounds of print job
        
        private PrintJob(MapViewer viewer, boolean viewerPrint) {
            super("PrintJob#" + JobCount++);
            this.map = viewer.getMap();
            this.focal = viewerPrint ? viewer.getFocal() : map;
            this.jobName = map.getDisplayLabel();
            this.isPrintingView = viewerPrint;

            // be sure to grab bounds info now -- sometimes it's possible
            // that the viewer bounds go negative once the model print dialog
            // boxes go active and VUE is hung without being able to reshape
            // or repaint itself.  (That's not a problem when we run this
            // in a thread, but there's a java bug with that right now, tho
            // it's safer to do it this way anyway).
            if (isPrintingView()) {
                if (focal == map)
                    this.bounds = viewer.getVisibleMapBounds();
                else
                    this.bounds = focal.getBorderBounds();
            } else
                this.bounds = map.getBounds();
            out(viewerPrint ? "printing: viewer contents" : "printing: whole map");
            out("requested map bounds: " + bounds);
        }

        private boolean isPrintingView() {
            return isPrintingView;
        }
        
        public void run() {
            out("print thread active");
            runPrint();
            out("print thread complete");
        }
        
        private void runPrint() {
            try {
                runDialogsAndPrint();
            } catch (Exception e) {
                out("print exception: " + e);
                e.printStackTrace();
            } finally {
                isPrintUnderway = false;
            }
            PrintAction.this.setEnabled(true);
        }

        private void runDialogsAndPrint() 
            throws java.awt.print.PrinterException
        {
            out("job starting for " + this.map);
            PrinterJob job = getPrinterJob();
            out("got OS job: " + tufts.Util.tags(job));
            if (job.printDialog()) {
                out("printDialog ran, waiting for format...");
                //PageFormat format = getPageFormat(job, bounds);
                PageFormat format = getPageFormatInteractive(job, bounds);
                out("format: " + outpf(format));
                if (format != null) {
                    job.setJobName(jobName);
                    job.setPrintable(this, format);
                    // try setting pageable to see if it then
                    // skips system dialog (we'd like a no-dialog option)
                    //job.setPrintService(job.getPrintService());

                    // this only *sometimes* works as a workaround
                    // for the vue ap mysteriously being raised above
                    // the system print dialog...
                    //VUE.frame.toBack();
                    
                    out("printing...");
                    job.print();
                }
            }
            out("job complete.");
        }

        public int print(Graphics gc, PageFormat format, int pageIndex)
            throws java.awt.print.PrinterException
        {
            if (pageIndex > 0) {
                out("page " + pageIndex + " requested, ending print job.");
                return Printable.NO_SUCH_PAGE;
            }

            out("asked to render page " + pageIndex + " in " + outpf(format));

            Dimension page = new Dimension((int) format.getImageableWidth() - 1,
                                           (int) format.getImageableHeight() - 1);

            Graphics2D g = (Graphics2D) gc;

            if (DEBUG.CONTAINMENT) {
                g.setColor(Color.lightGray);
                g.fillRect(0,0, 9999,9999);
            }
        
            g.translate(format.getImageableX(), format.getImageableY());

            // Don't need to clip if printing whole map, as computed zoom
            // should have made sure everything is within page size
            //if (!isPrintingView())
            //g.clipRect(0, 0, page.width, page.height);
                        
            if (DEBUG.CONTAINMENT) {
                //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                // draw border outline of page
                g.setColor(Color.gray);
                g.setStroke(VueConstants.STROKE_TWO);
                g.drawRect(0, 0, page.width, page.height);
                //g.setComposite(AlphaComposite.Src);
            }
        
            // compute zoom & offset for visible map components
            Point2D.Float offset = new Point2D.Float();
            // center vertically only if landscape mode
            //if (format.getOrientation() == PageFormat.LANDSCAPE)
            // TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
            double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
            out("rendering at scale " + scale);
            // set up the DrawContext
            DrawContext dc = new DrawContext(g,
                                             scale,
                                             -offset.x,
                                             -offset.y,
                                             null, // frame would be the PageFormat offset & size rectangle
                                             focal,
                                             false); // todo: absolute links shouldn't be spec'd here

            dc.setMapDrawing();
            dc.setPrintQuality();

            if (isPrintingView() && map == focal)
                g.clipRect((int) Math.floor(bounds.getX()),
                           (int) Math.floor(bounds.getY()),
                           (int) Math.ceil(bounds.getWidth()),
                           (int) Math.ceil(bounds.getHeight()));
        
            if (DEBUG.CONTAINMENT) {
                g.setColor(Color.red);
                g.setStroke(VueConstants.STROKE_TWO);
                g.draw(bounds);
            }
            
            // render the map
            if (map == focal)
                map.draw(dc);
            else
                focal.draw(dc);
          
            out("page " + pageIndex + " rendered.");
            return Printable.PAGE_EXISTS;
        }

        private void out(String s) {
            Log.info(String.format("PrintJob@%x[%s] %s", hashCode(), jobName, s));
            //System.out.println("PrintJob@" + Integer.toHexString(hashCode()) + "[" + jobName + "] " + s);
        }
    }


    private static String outpf(PageFormat p) {
        if (p == null) return null;
        final String[] o = {"LANDSCAPE", "PORTRAIT", "REVERSE LANDSCAPE"};
        return
            String.format("PageFormat[%s xoff=%.1fpt (%.2fin); yoff=%.1fpt (%.2fin); imageable-area: %.1fpt x %.1fpt (%.2f x %.2f inches)]",
                          o[p.getOrientation()],
                          p.getImageableX(),
                          p.getImageableX() / 72.0,
                          p.getImageableY(),
                          p.getImageableY() / 72.0,
                          p.getImageableWidth(),
                          p.getImageableHeight(),
                          p.getImageableWidth() / 72.0,
                          p.getImageableHeight() / 72.0
                          );
    }
}
