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
            MapViewer currentMap = (MapViewer)VUE.tabbedPane.getSelectedComponent();
            Dimension size = currentMap.getSize();
            
            graphics.setClip(0, 0, size.width, size.height);
            currentMap.paintComponent(graphics);
         
            graphics.setColor(Color.black);
            graphics.drawRect(0, 0, size.width - 1, size.height - 1);
            
            return Printable.PAGE_EXISTS;
        }
        
        else 
            return Printable.NO_SUCH_PAGE;
    }
    
    public void actionPerformed(ActionEvent ae)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        
        PageFormat format = job.defaultPage();
        Paper paper = format.getPaper();
        
        int offset = 15;
        double x, y, width, height;
        x = paper.getImageableX() + offset;
        y = paper.getImageableY() + offset;
        width = paper.getWidth() - offset;
        height = paper.getHeight() - offset;
        
        paper.setImageableArea(x, y, width, height);
        
        format.setPaper(paper);
        job.setPrintable(this, format);
        
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
