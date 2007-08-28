package tufts.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

public class PresentationNotes {

	public static void createMapAsPDF(File file)
	{
        // step 1: creation of a document-object
        Document document = new Document(PageSize.LETTER.rotate());
        
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            // step 3: we open the document
            
            document.open();            
            

            PdfContentByte cb = writer.getDirectContent();

            PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-70, document.getPageSize().height()-70);

            Graphics2D g2d = tp.createGraphics(document.getPageSize().width()-70, document.getPageSize().height()-70, new DefaultFontMapper(),false,100.0f);                                   
            
            Dimension page = new Dimension((int)document.getPageSize().width()-70,(int)document.getPageSize().getHeight()-70);
            // compute zoom & offset for visible map components
            Point2D.Float offset = new Point2D.Float();
            // center vertically only if landscape mode
            //if (format.getOrientation() == PageFormat.LANDSCAPE)
            //TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
            Rectangle2D bounds = VUE.getActiveMap().getBounds();
            double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
            System.out.println(scale  + " zoom factor...");
            // set up the DrawContext
            DrawContext dc = new DrawContext(g2d,
                      scale,
                      -offset.x,
                      -offset.y,
                      null, // frame would be the PageFormat offset & size rectangle
                      VUE.getActiveMap(),
                      false); // todo: absolute links shouldn't be spec'd here

        //    dc.setAntiAlias(true);
         //   dc.setMapDrawing();
            dc.setClipOptimized(false);

            VUE.getActiveMap().drawZero(dc);
            g2d.dispose();
            
            cb.addTemplate(tp,35,35);
            document.newPage();
                                                          
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
			
    public static void createPresentationSlidesNotes(File file)
    {
        // step 1: creation of a document-object
        Document document = new Document(PageSize.LETTER.rotate());
        
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
            
            final float pageWidth = document.getPageSize().width();
            final float pageHeight = document.getPageSize().height();
            final float fillWidth = pageWidth - 70;
            final float fillHeight = pageHeight - 70;

            if (DEBUG.Enabled) {
                System.out.println("\n---------------------------------");
                System.out.println("PDF DOCUMENT: pageSize " + document.getPageSize());
                System.out.println("fillWidth=" + fillWidth + " fillHeight=" + fillHeight);
            }
            
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
                if (DEBUG.Enabled) {
                    System.out.println("\nHANDLING ENTRY " + entry);
                }
            	
                final LWSlide slide = entry.getSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                //final float scale = fillWidth / toDraw.getWidth(); // assumes wide aspect
                
                //final PdfContentByte cb = writer.getDirectContent();
                //cb.cr
                final PdfTemplate template = PdfTemplate.createTemplate(writer, fillWidth, fillHeight);
                final Graphics2D graphics = template.createGraphics(fillWidth, fillHeight, new DefaultFontMapper(), false, 100.0f);
                final DrawContext dc = new DrawContext(graphics, 1.0);
                //final DrawContext dc = new DrawContext(graphics, scale);
                //final DrawContext dc = new DrawContext(graphics, toDraw);
                dc.setClipOptimized(false);
                dc.setInteractive(false);
                
                if (DEBUG.Enabled) {
                    System.out.println("  DRAWING INTO " + dc + " g=" + graphics + " clip=" + tufts.Util.fmt(graphics.getClip()));
                    dc.g.setColor(Color.green);
                    dc.g.fillRect(-Short.MAX_VALUE/2, -Short.MAX_VALUE/2, Short.MAX_VALUE, Short.MAX_VALUE);
                }

                //final java.awt.geom.AffineTransform tx = dc.g.getTransform();
                //toDraw.drawZero(dc);
                toDraw.drawFit(dc, 0);
                if (DEBUG.PDF) {
                    //dc.g.setTransform(tx);
                    final String dcDesc = dc.toString() + String.format(" scale=%.1f%%", dc.g.getTransform().getScaleX() * 100);
                    dc.setRawDrawing();
                    dc.g.setColor(Color.red);
                    dc.g.setFont(VueConstants.FixedSmallFont);
                    dc.g.drawString(dcDesc, 10, fillHeight - 27);
                    dc.g.drawString(entry.toString(), 10, fillHeight - 16);
                    dc.g.drawString(toDraw.toString(), 10, fillHeight - 5);
                }
                
                graphics.dispose();
                document.add(Image.getInstance(template));
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
    
// 	public static void createPresentationSlidesNotes(File file)
// 	{
//         // step 1: creation of a document-object
//         Document document = new Document(PageSize.LETTER.rotate());
        
//         try {
//             // step 2:
//             // we create a writer that listens to the document
//             // and directs a PDF-stream to a file            
//             PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
//             writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
//             writer.setStrictImageSequence(true);
                
//             // step 3: we open the document
            
//             document.open();            
            
//             for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) 
//             {
            	
//                 LWSlide slide = entry.getSlide();
//                 PdfContentByte cb = writer.getDirectContent();
//                 //cb.cr
//                 PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-70, document.getPageSize().height()-70);
//                 //PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-80, document.getPageSize().height()-80);
//                 Graphics2D g2d = tp.createGraphics(document.getPageSize().width()-70, document.getPageSize().height()-70, new DefaultFontMapper(),false,100.0f);
//                 DrawContext dc = new DrawContext(g2d,0.90); 
//                 dc.setClipOptimized(false); 
//                 slide.drawZero(dc);                                                                                            
//                 g2d.dispose();                                                                                                         
//                 //document.add(Image.getInstance(tp));
//                 cb.addTemplate(tp, 35, 35);
//                 //document.newPage();
//                 document.newPage();
            
                
//             }
//         }
//         catch(DocumentException de) {
//             System.err.println(de.getMessage());
//         }
//         catch(IOException ioe) {
//             System.err.println(ioe.getMessage());
//         }
        
//         // step 5: we close the document
//         document.close();
//     }			
	/*
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
                //String label = entry.getLabel();
                
                
                PdfContentByte cb = writer.getDirectContent();
                PdfTemplate tp = cb.createTemplate(200, 200);
                Graphics2D g2d = tp.createGraphics(200, 200, new DefaultFontMapper());
                
                
                table = new PdfPTable(new float[]{ 1 });
                table.getDefaultCell().setBorder(0);
                //table.getDefaultCell().setPaddingTop(30);
                Paragraph p = new Paragraph();
                
                p.setAlignment(Element.ALIGN_CENTER);
                                                                
                cell = new PdfPCell(new Phrase(notes));
                cell.setBorder(0);
              //  cell.setPaddingTop(30);
                slide.draw(new DrawContext(g2d,0.25));                                                                                            
                
                table.addCell(Image.getInstance(tp));
                table.addCell(cell);
                p.add(table);
                document.add(p);                 
                document.newPage();
                g2d.dispose();
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
	*/
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


