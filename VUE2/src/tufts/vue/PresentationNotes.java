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
package tufts.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import tufts.Util;
import tufts.vue.gui.GUI;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGraphics2D;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import edu.tufts.vue.pdf.VueFontMapper;

public class PresentationNotes {
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PresentationNotes.class);
    
	private static FontMapper dfm = null;
	
	private static FontMapper getFontMapper()
	{
		if (dfm != null)
			return dfm;
		else
		{
			 dfm = new VueFontMapper();					
	            	
	            if (Util.isWindowsPlatform())
	            {
	            	String drive = System.getenv("HOMEDRIVE");
	            	String path = drive + "\\Windows\\Fonts\\";
	            	((VueFontMapper) dfm).insertDirectory(path);
	            	FontFactory.registerDirectory(path);	            
	            }
	            else if (Util.isMacPlatform())
	            {
	            	((VueFontMapper) dfm).insertDirectory("/Library/Fonts/");
	            	FontFactory.registerDirectory("/Library/Fonts/");	            
	            	((VueFontMapper) dfm).insertDirectory("/System/Library/Fonts/");
	            	FontFactory.registerDirectory("/SystemLibrary/Fonts/");	  	
	            }
            		           	                
	         return dfm;
		}
	}
	
	public static void createMapAsPDF (File file)
	{
		createMapAsPDF(file,VUE.getActiveMap());
	}
	public static void createMapAsPDF(File file, LWMap map)
	{
        // step 1: creation of a document-object
        Document document = new Document(PageSize.LETTER.rotate());
        
        try {
        	GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            // step 3: we open the document
            
            document.open();            
            
            
            PdfContentByte cb = writer.getDirectContent();
          //  cb.setFontAndSize(arg0, arg1)
            PdfTemplate tp = cb.createTemplate(document.getPageSize().getWidth()-70, document.getPageSize().getHeight()-70);
          // tp.createGraphicsShapes(arg0, arg1) 
            
           
            PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(document.getPageSize().getWidth()-70, document.getPageSize().getHeight()-70, getFontMapper(),false,60.0f);                                   

            Dimension page = new Dimension((int)document.getPageSize().getWidth()-70,(int)document.getPageSize().getHeight()-70);
            // compute zoom & offset for visible map components
            Point2D.Float offset = new Point2D.Float();
            offset.x=35;
            offset.y=35;
            // center vertically only if landscape mode
            //if (format.getOrientation() == PageFormat.LANDSCAPE)
            //TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
            Rectangle2D bounds = map.getBounds();
            double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
          //  System.out.println(scale  + " zoom factor...");
            // set up the DrawContext
            DrawContext dc = new DrawContext(g2d,
                      scale,
                      -offset.x,
                      -offset.y,
                      null, // frame would be the PageFormat offset & size rectangle
                      map,
                      false); // todo: absolute links shouldn't be spec'd here

        //    dc.setAntiAlias(true);
            dc.setMapDrawing();
         //   dc.setPDFRender(true);
            //dc.setPrioritizeQuality(false); // why was this low quality?
            dc.setPrintQuality();
            //dc.setAntiAlias(false); // why was this turned off?  was it redundant?
            
            dc.setClipOptimized(true);	
         //   dc.setDraftQuality(true);
          //  dc.setRawDrawing();
            //dc.setClipOptimized(false);
            
            dc.setInteractive(false);
            dc.setDrawPathways(false);

            // VUE.getActiveMap().draw(dc);
            LWPathway.setShowSlides(false);
            map.drawZero(dc);
            LWPathway.setShowSlides(true);
            g2d.dispose();
          //  document.add(new Paragraph(new Chunk().setAnchor("http://www.cnn.com")));
            cb.addTemplate(tp,0,0);
            document.newPage();
                                                          
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	   GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
    }
			

	public static void createPresentationSlidesDeck(File file)
	{
        // step 1: creation of a document-object
        final Document document = new Document(PageSize.LETTER.rotate());
        
        try {
        	GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
            writer.setStrictImageSequence(true);
            // step 3: we open the document
            
            document.open();
            
            final float pageWidth = document.getPageSize().getWidth();
            final float pageHeight = document.getPageSize().getHeight();
            final float fillWidth = pageWidth - 70;
            final float fillHeight = pageHeight - 70;

            if (DEBUG.Enabled) {
                System.out.println("\n---------------------------------");
                System.out.println("PDF DOCUMENT: pageSize " + document.getPageSize());
                System.out.println("fillWidth=" + fillWidth + " fillHeight=" + fillHeight);
            }
            int currentIndex = VUE.getActivePathway().getIndex();
            VUE.getActivePathway().setIndex(-1);
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {

                if (DEBUG.Enabled) Log.debug("\n\nHANDLING DECK ENTRY " + entry);
                final LWSlide slide = entry.produceSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                
                final PdfTemplate template = PdfTemplate.createTemplate(writer, fillWidth, fillHeight);
                final PdfGraphics2D graphics = (PdfGraphics2D) template.createGraphics(fillWidth, fillHeight, getFontMapper(), false, 60.0f);
                final DrawContext dc = new DrawContext(graphics, 1.0);
//                 //final DrawContext dc = new DrawContext(graphics, scale);
                //final DrawContext dc = new DrawContext(graphics, toDraw); // ideally, should use this
                dc.setClipOptimized(false);
                dc.setInteractive(false); // should be un-needed
                dc.setPrintQuality();

                // PROBLEM TOFIX: portals, when rendered as a map-slide, are not showing what's below them
                
                if (DEBUG.Enabled) {
                    Log.debug("DRAWING INTO " + dc + " g=" + graphics + " clip=" + tufts.Util.fmt(graphics.getClip()));
                    if (DEBUG.PDF) {
                        dc.g.setColor(Color.green);
                        dc.g.fillRect(-Short.MAX_VALUE/2, -Short.MAX_VALUE/2, Short.MAX_VALUE, Short.MAX_VALUE);
                    }
                }
                
                try {
                    if (DEBUG.Enabled) dc.clearDebug();
                    toDraw.drawFit(dc, 0);
                } catch (Throwable t) {
                    Log.error("exception drawing " + toDraw, t);
                }
                
                try {
                
                    if (DEBUG.Enabled) Log.debug("painted " + DrawContext.getDebug() + " to " + dc);

                    if (DEBUG.PDF) {
                        final String dcDesc = dc.toString() + String.format(" scale=%.1f%%", dc.g.getTransform().getScaleX() * 100);
                        dc.setRawDrawing();
                        dc.g.setColor(Color.red);
                        dc.g.setFont(VueConstants.FixedSmallFont);
                        dc.g.drawString(dcDesc, 10, fillHeight - 27);
                        dc.g.drawString(entry.toString(), 10, fillHeight - 16);
                        dc.g.drawString(toDraw.toString(), 10, fillHeight - 5);
                    }
                    
                    // the graphics dispose appears to be very important -- we've seen completely intermittant
                    // problems with generating many page PDF documents, which would be well explained by
                    // java or internal itext buffers running out of memory.
                    graphics.dispose();
                    
                    document.add(Image.getInstance(template));
                    document.newPage();
                } catch (Throwable t) {
                    Log.error("exception finishing " + toDraw + " in " + dc, t);
                }
            }
            VUE.getActivePathway().setIndex(currentIndex);
            if (DEBUG.Enabled) Log.debug("PROCESSED ALL ENTRIES");
            
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	   GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
    }			
	
	private static final int SlideSizeX = 230;
	private static final int SlideSizeY = 172;
	public static void createPresentationNotes8PerPage(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object
        Document document = new Document(PageSize.LETTER);
        
        try {
        	GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

           // PdfPTable table;
           // PdfPCell cell;            
            int entryCount = 0;
            int entryOnPage =0;
            int currentIndex = VUE.getActivePathway().getIndex();
            
            VUE.getActivePathway().setIndex(-1);
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	
                final LWSlide slide = entry.produceSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                
                entryCount++;
                //String label = entry.getLabel();
                PdfContentByte cb = writer.getDirectContent();
                //cb.cr
                PdfTemplate tp = cb.createTemplate(SlideSizeX,SlideSizeY);
                Point2D.Float offset = new Point2D.Float();
                
                Rectangle2D bounds = null;
         
               	bounds = slide.getBounds();
                
                Dimension page = null;
                
                
                page = new Dimension(SlideSizeX,172);
                
                //PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-80, document.getPageSize().height()-80);
                double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
                PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(SlideSizeX,SlideSizeY, getFontMapper(),false,60.0f);
                DrawContext dc = new DrawContext(g2d,
                        scale,
                        -offset.x,
                        -offset.y,
                        null, // frame would be the PageFormat offset & size rectangle
                        entry.isMapView() ? entry.getFocal() : slide,
                        false); // todo: absolute links shouldn't be spec'd here
 
                dc.setClipOptimized(false);                        	
                dc.setPrintQuality();
                //slide.drawZero(dc);
                toDraw.drawFit(dc, 0);
                                                                                            
                g2d.dispose();                                                                                                         
                //document.add(Image.getInstance(tp));
                
                if (entryOnPage == 0)
                {
                	drawSequenceNumber(writer,36,739,entryCount);
                	cb.addTemplate(tp,56, 583);                	                                                        	
                }
                if (entryOnPage == 1)
                {
                	drawSequenceNumber(writer,296,739,entryCount);
                	cb.addTemplate(tp,306, 583);                	                                                        	
                }
                if (entryOnPage == 2)
                {
                	drawSequenceNumber(writer,36,559,entryCount);
                	cb.addTemplate(tp,56, 403);                	
                }
                if (entryOnPage == 3)
                {
                	drawSequenceNumber(writer,296,559,entryCount);
                	cb.addTemplate(tp,306, 403);                	                                                        	
                }
                if (entryOnPage == 4)
                {
                	drawSequenceNumber(writer,36,375,entryCount);
                	cb.addTemplate(tp,56, 219);                	
                }
                if (entryOnPage == 5)
                {
                	drawSequenceNumber(writer,296,375,entryCount);
                	cb.addTemplate(tp,306, 219);                	                                                        	
                }
                if (entryOnPage == 6)
                {
                	drawSequenceNumber(writer,36,192,entryCount);
                	cb.addTemplate(tp,56, 36);
                	//cb.addTemplate(drawLines(writer),296,18);
                }
                if (entryOnPage == 7)
                {
                	drawSequenceNumber(writer,296,192,entryCount);
                	cb.addTemplate(tp,306, 36);                	                                                        	
                }
                						   
                	
                entryOnPage++;
               if (entryCount % 8 == 0)
               {
            	   document.newPage();
            	   entryOnPage =0;
               }
            }
            VUE.getActivePathway().setIndex(currentIndex);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	 GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
    }			
	
	private static PdfTemplate drawLines(PdfWriter writer)
	{
		PdfContentByte cb2 = writer.getDirectContent();
        //cb.cr
        PdfTemplate tp2 = cb2.createTemplate(SlideSizeX+40,SlideSizeY);
        tp2.setColorStroke(Color.gray);
        //tp2.setColorFill(Color.gray);
        int x=30;
        for (int i=1; i < 9; i++)
        {
        	tp2.moveTo(0, x);
        	tp2.lineTo(SlideSizeX+40, x);
        	tp2.stroke();
        	x+=20;
        }
        
        return tp2;
	}
	
	private static void drawSequenceNumber(PdfWriter writer, float x, float y, int seq)
	{
		 PdfContentByte cb = writer.getDirectContent();
	     try {
			cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 16);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     cb.beginText();
	     cb.showTextAligned(Element.ALIGN_CENTER, new Integer(seq).toString()+".", x, y, 0f);
	     cb.endText();
	     cb.stroke();
        
        //return tp2;
	}
	
    
	
	public static void createAudienceNotes(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object
        Document document = new Document(PageSize.LETTER);
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

           // PdfPTable table;
           // PdfPCell cell;            
            int entryCount = 0;
            int entryOnPage =0;
            int currentIndex = VUE.getActivePathway().getIndex();
            
            VUE.getActivePathway().setIndex(-1);
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	
                final LWSlide slide = entry.produceSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                
                entryCount++;
                //String label = entry.getLabel();
                PdfContentByte cb = writer.getDirectContent();
                //cb.cr
                PdfTemplate tp = cb.createTemplate(SlideSizeX,SlideSizeY);
                Point2D.Float offset = new Point2D.Float();
                // center vertically only if landscape mode
                //if (format.getOrientation() == PageFormat.LANDSCAPE)
                //TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
                
                Rectangle2D bounds = null;
               
                bounds = slide.getBounds();
                
                Dimension page = null;
                
                
                page = new Dimension(SlideSizeX,172);
                
                //PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-80, document.getPageSize().height()-80);
                double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
                PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(SlideSizeX,SlideSizeY,getFontMapper(),false,60.0f);
                DrawContext dc = new DrawContext(g2d,
                        scale,
                        -offset.x,
                        -offset.y,
                        null, // frame would be the PageFormat offset & size rectangle
                        entry.isMapView() ? entry.getFocal() : slide,
                        false); // todo: absolute links shouldn't be spec'd here
 
                dc.setClipOptimized(false);
                dc.setPrintQuality();
                
                toDraw.drawFit(dc,0);

                                                                                            
                g2d.dispose();                                                                                                         
                //document.add(Image.getInstance(tp));
                
                if (entryOnPage == 0)
                {
                	drawSequenceNumber(writer,36,739,entryCount);
                	cb.addTemplate(tp,56, 583);                	                    
                    cb.addTemplate(drawLines(writer),296,565);
                	
                }
                if (entryOnPage == 1)
                {
                	drawSequenceNumber(writer,36,559,entryCount);
                	cb.addTemplate(tp,56, 403);
                	cb.addTemplate(drawLines(writer),296,385);
                }
                if (entryOnPage == 2)
                {
                	drawSequenceNumber(writer,36,375,entryCount);
                	cb.addTemplate(tp,56, 219);
                	cb.addTemplate(drawLines(writer),296,201);
                }
                if (entryOnPage == 3)
                {
                	drawSequenceNumber(writer,36,192,entryCount);
                	cb.addTemplate(tp,56, 36);
                	cb.addTemplate(drawLines(writer),296,18);
                }
                						   
                	
                entryOnPage++;
               if (entryCount % 4 == 0)
               {
            	   document.newPage();
            	   entryOnPage =0;
               }
            }
            VUE.getActivePathway().setIndex(currentIndex);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();     
    }
	
	public static void createSpeakerNotes1PerPage(File file)
	{
		
		 // step 1: creation of a document-object
		
		//This is a bit of a mess but because of hte bugs with drawing the slides
		//the easy way, we have no other choice but to render them directly onto the pdf
		//which makes it hard to use tables for stuff like formatting text...so we'll render
		// a blank table cell then render the image into it.
        Document document = new Document();
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            
            // step 3: we open the document
            document.open();
            
            PdfPTable table;
            PdfPCell cell;            
            int currentIndex = VUE.getActivePathway().getIndex();
            
            VUE.getActivePathway().setIndex(-1);
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	
                final LWSlide slide = entry.produceSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                final String notes = entry.getNotes();
                //String label = entry.getLabel();
                
                PdfContentByte cb = writer.getDirectContent();

                Point2D.Float offset = new Point2D.Float();
            
                Rectangle2D bounds = null;
                
                //if (!entry.isMapView())
                	bounds = slide.getBounds();
                //else 
                	//bounds = entry.getFocal().getBounds();
            
                Dimension page = null;
            
            
                page = new Dimension(432,324);
            
                PdfTemplate tp = cb.createTemplate(432,324);                
                double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
                PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(432,324, getFontMapper(),false,60.0f);
                DrawContext dc = new DrawContext(g2d,
                    scale,
                    -offset.x,
                    -offset.y,
                    null, // frame would be the PageFormat offset & size rectangle
                    entry.isMapView() ? entry.getFocal() : slide,
                    false); // todo: absolute links shouldn't be spec'd here

                dc.setClipOptimized(false);
                dc.setPrintQuality();
                /*if (!entry.isMapView())                	
                	slide.drawZero(dc);
                else
                {                
                	entry.getFocal().draw(dc);
                }*/
                toDraw.drawFit(dc,0);

                                                                                        
                g2d.dispose();                                                                                                         

                cb.addTemplate(tp,80, 482);

                
                //Paragraph p = new Paragraph();
                //p.setExtraParagraphSpace(330);
               // p.setSpacingBefore(330f);
              //  p.setAlignment(Element.ALIGN_CENTER);
                
                Paragraph phrase = new Paragraph(notes);
                //phrase.setExtraParagraphSpace(340f);
                phrase.setSpacingBefore(320f);
                phrase.setKeepTogether(true);
               //cell = new PdfPCell(phrase);
                //cell.setBorder(0);
                   //         table = new PdfPTable(new float[]{ 1 });
                  //        table.setWidthPercentage(100.0f);
                  //        table.getDefaultCell().setBorder(0);
                          //table.getDefaultCell().setPaddingTop(30);
            
                                                                                                            
                
                //PdfPCell c2 = new PdfPCell();
                //c2.setFixedHeight(340); //slides are 540x405
                //c2.setBorder(0);
                //table.addCell(c2);                
                //table.addCell(cell);
                //table.setKeepTogether(false);
                //cell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
                
                //p.add(table);
                //System.out.println("CELL HEIGHT : " + cell.getHeight());
                //Section s1 = new Section();
                //ColumnText chunk2 = new ColumnText(cb);
                //chunk2.setText(phrase);
                //chunk2.setSi
                //chunk2.setSimpleColumn(phrase,70, 330, document.getPageSize().width()-70,document.getPageSize().height()-70,15, Element.ALIGN_LEFT);
               // chunk2.go();
                //PdfChunk chunk2 = new PdfChunk);
                Paragraph p2 = new Paragraph(" ");
                p2.setKeepTogether(false);
                phrase.setKeepTogether(false);
               // p2.setExtraParagraphSpace(230f);
                document.add(p2);
                document.add(phrase);
                document.newPage();               
            }

            VUE.getActivePathway().setIndex(currentIndex);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
    }

	public static void createSpeakerNotes4PerPage(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object
		
		
		
        Document document = new Document(PageSize.LETTER);
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            PdfPTable table;
            PdfPCell cell;            
            int entryCount = 0;
            int entryOnPage =0;
            int currentIndex = VUE.getActivePathway().getIndex();

            VUE.getActivePathway().setIndex(-1);
            
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	

                final LWSlide slide = entry.produceSlide();
                final LWComponent toDraw = (slide == null ? entry.node : slide);
                final String notes = entry.getNotes();
                entryCount++;
                
                
                table = new PdfPTable(new float[]{ 1,1 });
                table.getDefaultCell().setBorder(0);
                //table.getDefaultCell().setPaddingBottom(50.0f);
                table.setSpacingAfter(20.0f);
                Paragraph p = new Paragraph();
                
                p.setAlignment(Element.ALIGN_CENTER);
                                                
                Phrase phrase = new Phrase(notes);
                Font f = phrase.getFont();
                f.setSize(8.0f);
                p.setFont(f);
                cell = new PdfPCell(phrase);
                cell.setBorder(0);         
                
                PdfPCell i2 = new PdfPCell();
                i2.setFixedHeight(172);
                i2.setBorder(0);
                
                
                
                
                                 
                
                //Render the table then throw the images on
	            PdfContentByte cb = writer.getDirectContent();	            
	            PdfTemplate tp = cb.createTemplate(SlideSizeX,SlideSizeY);
	            
	            Point2D.Float offset = new Point2D.Float();
                // center vertically only if landscape mode
                //if (format.getOrientation() == PageFormat.LANDSCAPE)
                //TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
                
                Rectangle2D bounds = null;
                //if (!entry.isMapView())
                	bounds = slide.getBounds();
                //else 
                	//bounds = entry.getFocal().getBounds();
                
                Dimension page = null;
                
                
                page = new Dimension(SlideSizeX,172);
                
                //PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-80, document.getPageSize().height()-80);
                double scale = ZoomTool.computeZoomFit(page, 5, bounds, offset, true);
                PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(SlideSizeX,SlideSizeY, getFontMapper(),false,60.0f);
                DrawContext dc = new DrawContext(g2d,
                        scale,
                        -offset.x,
                        -offset.y,
                        null, // frame would be the PageFormat offset & size rectangle
                        entry.isMapView() ? entry.getFocal() : slide,
                        false); // todo: absolute links shouldn't be spec'd here
 
                dc.setClipOptimized(false);
                dc.setPrintQuality();
                /*if (!entry.isMapView())                	
                	slide.drawZero(dc);
                else
                {                
                	entry.getFocal().draw(dc);
                }*/
                toDraw.drawFit(dc,0);

                                                                                            
                g2d.dispose();                                                                                                         
                //document.add(Image.getInstance(tp));
                
              //  int position = cell.
           //     drawSequenceNumber(writer,36,position+203,entryCount);
                
           //     cb.addTemplate(tp,56, position);                	                                        
                						   
                              
                Image img = Image.getInstance(tp);
	            table.addCell(img);
	            table.addCell(cell);
	            p.add(table);
	            document.add(p);
            }
            VUE.getActivePathway().setIndex(currentIndex);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
        
	}

	public static void createNodeNotes4PerPage(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object
		
		
		
        Document document = new Document(PageSize.LETTER);
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            PdfPTable table;
            PdfPCell cell;            
            int entryCount = 0;
            int entryOnPage =0;
                       
            Iterator i = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            while (i.hasNext())
            {
                LWComponent component = (LWComponent) i.next();
                if (component instanceof LWNode)
                {
                	final LWNode node= (LWNode)component;
                	
                	final String notes = node.getNotes();                                
                
                	entryCount++;                
                
                	table = new PdfPTable(new float[]{ 1,1 });
                	table.getDefaultCell().setBorder(0);

                	table.setSpacingAfter(20.0f);
                	Paragraph p = new Paragraph();
                
                	p.setAlignment(Element.ALIGN_CENTER);
                                                
                	Phrase phrase = new Phrase(notes);
                	
                	Font f = phrase.getFont();
                	f.setSize(8.0f);
                	p.setFont(f);
                	cell = new PdfPCell(phrase);
                	cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                	cell.setBorder(0);         
                
                	PdfPCell i2 = new PdfPCell();
                	i2.setFixedHeight(172);
                	i2.setBorder(0);                                                                                             
                
                	//Render the table then throw the images on
                	PdfContentByte cb = writer.getDirectContent();	            
                	PdfTemplate tp = cb.createTemplate(SlideSizeX,SlideSizeY);
	            
                	Point2D.Float offset = new Point2D.Float();
                	//center vertically only if landscape mode
                	//if (format.getOrientation() == PageFormat.LANDSCAPE)
                	//TODO: allow horizontal centering, but not vertical centering (handle in computeZoomFit)
                
                	Rectangle2D bounds = null;
                
                	bounds = node.getBounds();
                
                	Dimension page = null;
                
                
                	page = new Dimension(SlideSizeX,172);
                
                //	PdfTemplate tp = cb.createTemplate(document.getPageSize().width()-80, document.getPageSize().height()-80);
                	double scale = ZoomTool.computeZoomFit(page, 15, bounds, offset, true);
                	PdfGraphics2D g2d = (PdfGraphics2D)tp.createGraphics(SlideSizeX,SlideSizeY, getFontMapper(),false,60.0f);
                	DrawContext dc = new DrawContext(g2d,
                        scale,
                        -offset.x,
                        -offset.y,
                        null, // frame would be the PageFormat offset & size rectangle
                        node,
                        false); // todo: absolute links shouldn't be spec'd here
 
                	dc.setClipOptimized(false);
                        dc.setPrintQuality();
                    node.drawFit(dc,15);
                    

                                                                                            
                    g2d.dispose();                                                                                                         
                              
                    Image img = Image.getInstance(tp);
                    table.addCell(img);
                    table.addCell(cell);
                    p.add(table);
                    document.add(p);
                }
            }
            
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        
        // step 5: we close the document
        document.close();
        
	}

	public static void createOutline(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object					
        Document document = new Document(PageSize.LETTER);
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            Paragraph p1 = new Paragraph(VUE.getActivePathway().getLabel());
            p1.setSpacingAfter(15.0f);
            Font f = p1.getFont();
            f.setStyle(Font.BOLD);
            p1.setFont(f);
			document.add(p1);
			
			/*p1.add("The leading of this paragraph is calculated automagically. ");
			p1.add("The default leading is 1.5 times the fontsize. ");
			p1.add(new Chunk("You can add chunks "));
			p1.add(new Phrase("or you can add phrases. "));
			p1.add(new Phrase(
            */
            int entryCount=1;
            int currentIndex = VUE.getActivePathway().getIndex();
            
            VUE.getActivePathway().setIndex(-1);
            for (LWPathway.Entry entry : VUE.getActivePathway().getEntries()) {
            	                
                String notes = entry.getNotes();
                Paragraph p = new Paragraph(entryCount + ".  " + entry.getLabel());
                f = p.getFont();
                f.setStyle(Font.BOLD);
                p.setFont(f);
                Paragraph notesP = new Paragraph(notes);
                notesP.setIndentationLeft(15.0f);                
                notesP.setSpacingAfter(15.0f);
                document.add(p);
                document.add(notesP);
                						   
                entryCount++;
            }
            VUE.getActivePathway().setIndex(currentIndex);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        // step 5: we close the document
        document.close();
        
    }

	public static void createNodeOutline(File file)
	{
		//page size notes:
		//martin-top,left,right,bottom = 36
		//widht :612
		//height : 792
		//usable space 540 x 720
        // step 1: creation of a document-object					
        Document document = new Document(PageSize.LETTER);
        
        try {
        	   GUI.activateWaitCursor();
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file            
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
          //  writer.setDefaultColorspace(PdfName.DEFAULTRGB, null);
           // writer.setStrictImageSequence(true);
            
            // step 3: we open the document
            document.open();

            Paragraph p1 = new Paragraph(VUE.getActiveMap().getLabel());
            
            p1.setSpacingAfter(15.0f);
            Font f = p1.getFont();
            f.setStyle(Font.BOLD);
            f.setSize(18f);
            p1.setFont(f);
			document.add(p1);
			
			String n2 =VUE.getActiveMap().getNotes();
			if (n2 != null && n2.length() > 0)
			{
				Paragraph p2 = new Paragraph(n2);
				p2.setIndentationLeft(30.0f);
				p2.setSpacingAfter(15.0f);
			//	f = p2.getFont();
				//f.setSize(f.getSize()-2);
				//p2.setFont(f);
				document.add(p2);
			}
            
			int entryCount=1;
			float indentation = 0.0f;
			Iterator it = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
			
            while (it.hasNext())            
            {
            	LWComponent c = (LWComponent)it.next();
            	if (c instanceof LWNode)
            	{
            		LWNode n = (LWNode)c;
            		outlineChildNode(document, indentation, n, entryCount);
            		entryCount++;
            		iterateChildren(document,indentation+10,n,1);
            	}
                
            }
            
            it = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            while (it.hasNext())            
            {
            	LWComponent c = (LWComponent)it.next();
            	if (c instanceof LWLink)
            	{
	            	LWLink l = (LWLink)c;
	                String notes = l.getNotes();
	                String linkLabel = l.getLabel();
	                
	                if ((notes == null || notes.length() == 0) && (linkLabel == null || linkLabel.length() ==0))
	                	continue;
	                
	                if (linkLabel == null || linkLabel.length()==0)
	                	linkLabel = "Link";
	                
	                Paragraph p = new Paragraph(entryCount + ".  " + linkLabel.replaceAll("\\n",""));
	                f = p.getFont();
	                f.setStyle(Font.BOLD);
	                f.setSize(14f);
	                p.setFont(f);
	                Paragraph notesP = new Paragraph(notes);
	                
	                
	               // f = notesP.getFont();
				//	f.setSize(f.getSize()-2);
	                notesP.setIndentationLeft(30.0f);                
	                notesP.setSpacingAfter(15.0f);
	                document.add(p);
	                document.add(notesP);
	                
	                
	                entryCount++;
            	}
            }
            
            
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        finally
        {
        	GUI.clearWaitCursor();
        }
        // step 5: we close the document
        document.close();
        
    }
	
	private static void iterateChildren(Document d, float indentation,LWNode n, int entryCount)
	{
		if (n.hasChildren())
    	{
    		Iterator i = n.getChildIterator();
    		
    		while (i.hasNext())
    		{
    			
    			Object o = i.next();
    		//	System.out.println("child : " + o);
    			if (o instanceof LWNode)
    			{
    				//System.out.println("outlineChildNode");
    				outlineChildNode(d,indentation,(LWNode)o,entryCount);
    				entryCount++;
    				iterateChildren(d,indentation+10,(LWNode)o,1);
    			}
    			else
    				continue;
    		}
    	}
	}
	private static void outlineChildNode(Document d, float indentation,LWNode n, int entryCount)
	{
		String notes = n.getNotes();
        String nodeLabel = n.getLabel();
        
        if (nodeLabel == null || nodeLabel.length()==0)
        	nodeLabel = "Node";
        
        Paragraph p = new Paragraph(entryCount + ".  " + n.getLabel().replaceAll("\\n",""));
        Font f = p.getFont();
        f.setStyle(Font.BOLD);
        f.setSize(14f);
        p.setFont(f);
        p.setIndentationLeft(indentation);
        Paragraph notesP = new Paragraph(notes);

        notesP.setIndentationLeft(indentation+20);                
        notesP.setSpacingAfter(15.0f);
        try {
			d.add(p);
			d.add(notesP);
		} catch (DocumentException e) {
			e.printStackTrace();
		}        		
			
	}
}


