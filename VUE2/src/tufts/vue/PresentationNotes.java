package tufts.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class PresentationNotes {

	public static void createPresentationSlidesNotes(File file)
	{
        // step 1: creation of a document-object
        Document document = new Document();
        
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
            writer.setStrictImageSequence(true);
            PdfPTable table;
            PdfPCell cell;    
            // step 3: we open the document
            
            document.open();            
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) 
            {
            	
                LWSlide slide = entry.getSlide();
                BufferedImage i = slide.createImage(1.00, new Dimension(700,700));
                Image i2 = Image.getInstance((java.awt.Image)i,VUE.getActivePathway().getMasterSlide().getFillColor());
                i2.setRotationDegrees(90.0f);
                i2.setAbsolutePosition(35,75);                                                                                            
                document.add(i2);
                document.newPage();
            
                
            }
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        
        // step 5: we close the document
        document.close();
    }			
	
	public static void createPresentationNotes(File file)
	{
        // step 1: creation of a document-object
        Document document = new Document();
        
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
            writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            PdfPTable table;
            PdfPCell cell;            
                                                  
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	
                LWSlide slide = entry.getSlide();
                String notes = entry.getNotes();
                String label = entry.getLabel();
                BufferedImage i = slide.createImage(1.00, new Dimension(1000,1000));
              //  document.add(new Paragraph(notes));
                table = new PdfPTable(new float[]{ 1 });
                table.getDefaultCell().setBorder(0);
                table.getDefaultCell().setPaddingTop(80);
                Paragraph p = new Paragraph();
                
                p.setAlignment(Element.ALIGN_CENTER);
                p.add(table);
                Image i2 = Image.getInstance((java.awt.Image)i,VUE.getActivePathway().getMasterSlide().getFillColor());                
                
                cell = new PdfPCell(new Phrase(notes));
                cell.setBorder(0);
                cell.setPaddingTop(80);
                table.addCell(i2);
                table.addCell(cell);
                document.add(p);                 
                document.newPage();
            }
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        
        // step 5: we close the document
        document.close();
    }			
	
	public static void createAudienceNotes(File file)
	{
        // step 1: creation of a document-object
        Document document = new Document();
        
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
            writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            PdfPTable table = new PdfPTable(new float[]{ 1,1 });
            PdfPCell cell;            
            table.getDefaultCell().setBorder(0);
            table.getDefaultCell().setPaddingBottom(30.0f);
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	
                LWSlide slide = entry.getSlide();                
                BufferedImage i = slide.createImage(1.00, new Dimension(600,600));   
                                                
                                
                Image i2 = Image.getInstance((java.awt.Image)i,VUE.getActivePathway().getMasterSlide().getFillColor());                
                                                
                PdfPTable notesTable = new PdfPTable(new float[] {1});                
                notesTable.getDefaultCell().setBorder(0);
                notesTable.getDefaultCell().setBorderWidthBottom(1);
                notesTable.getDefaultCell().setFixedHeight(15.0f);
                for (int k = 0; k < 10; k++)
                	notesTable.addCell(new Phrase());
                                
                
                table.addCell(i2);
                table.addCell(notesTable);                                                              
            }
            document.add(table);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        
        // step 5: we close the document
        document.close();
    }
}


