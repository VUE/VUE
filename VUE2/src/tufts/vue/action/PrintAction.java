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
        
        //page number 1
        if (param == 0)
        {
            //draws the map content using the given graphics object
            Graphics2D g2 = (Graphics2D)graphics;
            
            //border outline of the map
            
            MapViewer currentMap = VUE.getActiveViewer();
            Dimension size = currentMap.getSize();
            
            //translate it to the printable section of the paper
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            //g2.setClip(0, 0, size.width, size.height);
            g2.setClip(0, 0, (int)pageFormat.getImageableWidth() - 1, (int)pageFormat.getImageableHeight() - 1);
            currentMap.paintComponent(g2);
          
            g2.setColor(Color.black);
            g2.drawRect(0, 0, (int)pageFormat.getImageableWidth() - 1, (int)pageFormat.getImageableHeight() - 1);
            
            return Printable.PAGE_EXISTS;
        }
        
        //more than page 1 then don't print
        //could change in the future
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
