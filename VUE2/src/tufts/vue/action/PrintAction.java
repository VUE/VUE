/*
 * PrintAction.java
 *
 * Created on June 6, 2003, 12:12 PM
 */

package tufts.vue.action;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.print.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import tufts.vue.*;
/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class which handles the printing task of the currently active map*/
public class PrintAction extends AbstractAction implements Printable {
    
    /** Creates a new instance of PrintAction */
    public PrintAction() {
        super();
    }
    
    public PrintAction(String label){
        super(label);
        putValue(Action.SHORT_DESCRIPTION, label);
    }
    
    /**A method which actualy takes care of what to print*/
    public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int param) throws java.awt.print.PrinterException {
        
        MapViewer currentMap = VUE.getActiveViewer();
        Rectangle2D bounds = currentMap.getAllComponentBounds();
        int xLocation = (int)bounds.getX() + 5, yLocation = (int)bounds.getY() + 5;
        Dimension size = new Dimension((int)bounds.getWidth() + xLocation, (int)bounds.getHeight() + yLocation);
        int totalPages;
        
        //calcuates the total page number using the component size and the paper size
        if (pageFormat.getOrientation() == PageFormat.PORTRAIT)
          totalPages = (int)Math.ceil((size.getHeight() / pageFormat.getImageableHeight()));
          
        else
          totalPages = (int)Math.ceil((size.getWidth() / pageFormat.getImageableWidth()));
         
        //if the page number is less than one, make it one
        //maybe not necessary?
        if (totalPages <= 1)
            totalPages = 1;
        
        System.out.println("total page number is " + totalPages);
        
        //printing pages
        if(param < totalPages)
        {
            Graphics2D g2 = (Graphics2D)graphics;
            int xClip = 0, yClip = 0;
            double xTranslation, yTranslation;
            
            if (pageFormat.getOrientation() == PageFormat.LANDSCAPE)
            {
                xClip = (int)pageFormat.getImageableWidth() * param;
                xTranslation = pageFormat.getImageableX() - xClip;
                
                System.out.println("xClip is " + xClip);
                System.out.println("xTranslation is " + xTranslation);
                
                //translate it to the printable section of the paper
                g2.translate(xTranslation, pageFormat.getImageableY());
                g2.setClip(xClip, 0, (int)pageFormat.getImageableWidth() - 1, (int)pageFormat.getImageableHeight() - 1);
            }
            
            else
            {
                //translate it to the printable section of the paper
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                g2.setClip(0, 0, (int)pageFormat.getImageableWidth() - 1, (int)pageFormat.getImageableHeight() - 1);
            }
            
            //printing the currently selected map using the given graphics object
            currentMap.paintComponent(g2);
          
            //border outline of the map
            g2.setColor(Color.black);
            g2.drawRect(xClip, yClip, (int)pageFormat.getImageableWidth() - 1, (int)pageFormat.getImageableHeight() - 1);
            
            return Printable.PAGE_EXISTS;
        }
        
        //if it is an out of bound page number then the page doesn't exist
        else 
            return Printable.NO_SUCH_PAGE;
    }
    
    /**Reacts to the action dispatched by the class*/
    public void actionPerformed(ActionEvent ae)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
         
        try
        {
            //job.setPrintable(this);
            
            if (job.printDialog()) {
                try 
                {
                    PageFormat format = job.pageDialog(job.defaultPage());
                    job.setPrintable(this, format);
                    job.print();
                } 
                catch (Exception ex) 
                {
                    System.err.println("couldn't print:" + ex);
                    ex.printStackTrace();
                }
            }
        }
        
        catch (Exception e)
        {
            System.err.println("can't open the dialog box:" + e);
        }
    }
}
