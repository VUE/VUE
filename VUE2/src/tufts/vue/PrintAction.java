/*
 * PrintAction.java
 *
 * Created on June 6, 2003, 12:12 PM
 */

package tufts.vue;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.print.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
            MapViewer currentMap = (MapViewer)VUE.tabbedPane.getSelectedComponent();
            currentMap.paintComponent(graphics);
         
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
