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
public class PrintAction extends AbstractAction implements Printable {
    
    /** Creates a new instance of PrintAction */
    public PrintAction() {
    }
    
    public PrintAction(String label){
        super(label);
        putValue(Action.SHORT_DESCRIPTION, label);
    }
    public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int param) throws java.awt.print.PrinterException {
        
        if (param == 0)
        {
            Graphics2D g2 = (Graphics2D)graphics;
            MapViewer currentMap = VUE.getActiveViewer();
            Dimension size = currentMap.getSize();
            
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.setClip(0, 0, size.width, size.height);
            currentMap.paintComponent(g2);
         
            g2.setColor(Color.black);
            g2.drawRect(0, 0, size.width - 1, size.height - 1);
            
            return Printable.PAGE_EXISTS;
        }
        
        else 
            return Printable.NO_SUCH_PAGE;
    }
    
    public void actionPerformed(ActionEvent ae)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
     
        job.setPrintable(this);
        
        try
        {
            if (job.printDialog()) {
                try 
                {
                    job.print();
                    System.out.println("done printing");
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
