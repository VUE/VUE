/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

/*
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;


import javax.swing.*;

import java.awt.Component;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import tufts.vue.*;
import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.vue.gui.HtmlOutputDialog;
import tufts.vue.gui.VueFrame;

import static tufts.vue.Resource.*;
    
/**
 * Save the currently active map.
 *
 * @author akumar03
 * @author Scott Fraize
 * @author  Helen Oliver, Imperial College London revisions added & initialled 2010-2011
 */
public class SaveAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SaveAction.class);

    private boolean saveAs = true;
    private boolean export = true;
    
    public SaveAction(String label, boolean saveType, boolean export){
        super(label, null, ":general/Save");
        setSaveAs(saveType);
        this.export = export;
    }
    
    public SaveAction(String label, boolean saveType)
    {
    	this(label,saveType,false);
    }
    public SaveAction(String label) {
        this(label, true,false);
    }
    
    public SaveAction() {
        this("Save", false,false);
    }
    
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    
    /*    
    public void setFileName(String fileName) {
        file = new File(fileName);
    }
    
    public String getFileName() {
        return file.getAbsolutePath();
    }
    */
    
    private boolean inSave = false;
    public void actionPerformed(ActionEvent e)
    {
        if (inSave) // otherwise rapid Ctrl-S's will trigger multiple dialog boxes
            return;            	
        
        try {
            inSave = true;
            Log.info("Action["+e.getActionCommand()+"] invoked...");
            if (saveMap(tufts.vue.VUE.getActiveMap(), isSaveAs(),export))
                Log.info("Action["+e.getActionCommand()+"] completed.");
            else
                Log.info("Action["+e.getActionCommand()+"] aborted.");
        } finally {
            inSave = false;
        }
    }

    /**
     * @return true if success, false if not
     */
      
    public static boolean saveMap(LWMap map, boolean saveAs, boolean export)
    {
        Log.info("saveMap: " + map);        
        
        GUI.activateWaitCursor();         
       
        
        if (map == null)
            return false;
        
        File file = map.getFile();
        
        // HO 12/01/2012 BEGIN ******
        boolean bWritable = true;
        boolean bAlreadyNotified = false;
        // HO 12/01/2012 END *******

        int response = -1;
        if (map.getSaveFileModelVersion() == 0) {

        	final Object[] defaultOrderButtons = { VueResources.getString("saveaction.saveacopy"),VueResources.getString("saveaction.save")};
            Object[] messageObject = {map.getLabel()};
        	response = VueUtil.option
            (VUE.getDialogParent(),
        	 VueResources.getFormatMessage(messageObject, "dialog.saveaction.message"),         
        	 VueResources.getFormatMessage(messageObject, "dialog.saveaction.title"),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             defaultOrderButtons,             
             VueResources.getString("saveaction.saveacopy")
             );
        }
        
        if (response == 0) {
            saveAs=true;
        } if ((saveAs || file == null) && !export) {
            //file = ActionUtil.selectFile("Save Map", "vue");
            file = ActionUtil.selectFile("Save Map", null);
            // HO 12/01/2012 BEGIN ******
            if (file != null) {
    	        File lockFile = FileLockAction.isFileLockedByOtherUser(file);
    	        if (lockFile != null) {
	        		// HO 19/01/2012 BEGIN *****
	        		//LWMap openMap = VUE.isThisMapAlreadyOpen(file);
	        		//if (openMap == null)
	        		// HO 19/01/2012 END ******
	        			// HO 18/01/2012 BEGIN ********
	        			FileLockAction.notifyThatFileIsLocked(file, lockFile);
				    	// HO 19/01/2012 BEGIN *******
				    	FileLockAction.notifyTargetFilesThatAreLocked(map);
				    	// HO 19/01/2012 END *********
    		    		// HO 18/01/2012 END ********
			    	
			    	return false;
    	        } else {
			    	// HO 19/01/2012 BEGIN *******
    	        	if (!VUE.bConstructingWormholes) {
    	        		if (!bAlreadyNotified) {
    	        			FileLockAction.notifyTargetFilesThatAreLocked(map);
    	        			bAlreadyNotified = true;
    	        		}
    	        	}
			    	// HO 19/01/2012 END *********
    	        }

            } 
            // HO 12/01/2012 END ********
        } else if (export) {
            file = ActionUtil.selectFile("Export Map", "export");
        }
            
        if (file == null) {
            try {
                return false;
            } finally {
                GUI.clearWaitCursor();
            }
        }

        try {
        	         	
            Log.info("saveMap: target[" + file + "]");
            
            // HO 12/01/2012 BEGIN ******
            if (file != null) {
    	        File lockFile = FileLockAction.isFileLockedByOtherUser(file);
    	        if (lockFile != null) {
    	        	// we don't want to notify while we're in the middle of wormholes
    	        	if (!VUE.bConstructingWormholes)  {
	        			FileLockAction.notifyThatFileIsLocked(file, lockFile);
				    	// HO 19/01/2012 BEGIN *******
				    	FileLockAction.notifyTargetFilesThatAreLocked(map);
				    	// HO 19/01/2012 END *********
    	        	} 
			    	
			    	return false;
    	        } else {
    	        	if (!VUE.bConstructingWormholes) {
    	        		if (!bAlreadyNotified) {
    	        			// HO 19/01/2012 BEGIN *******
    	        			FileLockAction.notifyTargetFilesThatAreLocked(map);
    	        			bAlreadyNotified = true;
    	        			// HO 19/01/2012 END *********
    	        		}
    	        	}
    	        }

            } 
            // HO 12/01/2012 END ********
            
            final String name = file.getName().toLowerCase();
            // HO 06/01/2012 BEGIN ******
            final File newFile = file;
            // HO 06/01/2012 END ********

            if (name.endsWith(".rli.xml")) {
                new IMSResourceList().convert(map,file);
            }
            // HO 05/01/2012 BEGIN *******
            // else if (name.endsWith(".xml") || name.endsWith(".vue")) {
            else if (name.endsWith(".xml")) {
            	// HO 05/01/2012 END *******
                ActionUtil.marshallMap(file, map);
            }
            // HO 05/01/2012 BEGIN *******
            else if (name.endsWith(".vue")) {
            	// HO 05/01/2012 BEGIN *******
                FileLockAction.deletePreviousLockFile(map, newFile);
                // HO 05/01/2012 END ********* 
            	ActionUtil.marshallMap(file, map);                
                // HO 05/01/2012 BEGIN *******
                FileLockAction.createLockFile(file, map, false, false);
                // HO 05/01/2012 END *********              
            }
            // HO 05/01/2012 END *********
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                ImageConversion.createActiveMapJpeg(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".png"))
                ImageConversion.createActiveMapPng(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".svg"))
                SVGConversion.createSVG(file);
            
            else if (name.endsWith(".pdf"))
            {
            	PresentationNotes.createMapAsPDF(file);
                //new PDFTransform().convert(file);
            }
            else if (name.endsWith(".zip"))
            {   Vector resourceVector = new Vector();
            	Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            	while(i.hasNext()) {	
            		LWComponent component = (LWComponent) i.next();
            		System.out.println("Component:"+component+" has resource:"+component.hasResource());
            		if(component.hasResource() && (component.getResource() instanceof URLResource)){
                    
            			URLResource resource = (URLResource) component.getResource();                    
            			//   	if(resource.getType() == Resource.URL) {
            			try {
                        // File file = new File(new URL(resource.getSpec()).getFile());
                        if(resource.isLocalFile()) {
                        	String spec = resource.getSpec();                        	                        
                        	System.out.println(resource.getSpec());
                            Vector row = new Vector();
                            row.add(new Boolean(true));
                            row.add(resource);
                            row.add(new Long(file.length()));
                            row.add("Ready");
                            resourceVector.add(row);
                        }
            			}catch (Exception ex) {
            				System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
            				ex.printStackTrace();
            			}                    
            		}                
            	}
            	File savedCMap =PublishUtil.createZip(map, resourceVector);
            	 InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                int fileLength = (int)savedCMap.length();
                byte bytes[] = new  byte[fileLength];
                try
                {
                	while (istream.read(bytes,0,fileLength) != -1)
                		ostream.write(bytes,0,fileLength);
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                }
                finally
                {
                	istream.close();
                	ostream.close();
                }
            }
            //else if (name.endsWith(".html"))
              //  new HTMLConversion().convert(file);
            
            //else if (name.endsWith(".imap"))
            else if (name.endsWith(".html")) 
            {            	
            	HtmlOutputDialog hod = new HtmlOutputDialog();
            	hod.setVisible(true);   
            	if (hod.getReturnVal() > 0)
            		// HO 28/11/2011 BEGIN **********
            		// new ImageMap().createImageMap(file, hod.getScale());
            		// merging with current VUE release
            		new ImageMap().createImageMap(file, hod.getScale(), hod.getFormat());
            		// HO 28/11/2011 END **********
            }
//             else if (name.endsWith(".htm")) {
//                 writeHTMLOutline(map, file);
//             }
            else if(name.endsWith(".rdf"))
            {
               edu.tufts.vue.rdf.RDFIndex index = new edu.tufts.vue.rdf.RDFIndex();
               
               String selectionType = VueResources.getString("rdf.export.selection");
               
               if(selectionType.equals("ALL"))
               {
                 Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
                 while(maps.hasNext())
                 {
                     index.index(maps.next());
                 }
               }
               else if(selectionType.equals("ACTIVE"))
               {
                 index.index(VUE.getActiveMap());  
               }    
               else
               {    
                 index.index(VUE.getActiveMap());
               }  
               FileWriter writer = new FileWriter(file);
               index.write(writer);
               writer.close();
            }
            else if (name.endsWith(VueUtil.VueArchiveExtension))
            {
            	// HO 05/01/2012 BEGIN *******
                FileLockAction.deletePreviousLockFile(map, newFile);
                // HO 05/01/2012 END ********* 
            		
            	Archive.writeArchive(map, file);
            	
                FileLockAction.createLockFile(file, map, false, false);
                // HO 05/01/2012 END *********
                
            } else {
                Log.warn("Unknown save type for filename extension: " + name);
                return false;
            }

			// don't know this as not all the above stuff is passing
            // exceptions on to us!
            Log.debug("Save completed for " + file);
            if (!VUE.isApplet())
            {
            	VueFrame frame = (VueFrame)VUE.getMainWindow();
            	String title = VUE.getName() + ": " + name;                      
            	frame.setTitle(title);
            }
            // HO 11/03/2011 BEGIN *****************
            // make sure that newly saved archive files also get added to the Recent Files list
            //if (name.endsWith(".vue"))
            if ((name.endsWith(".vue")) || (name.endsWith(VueUtil.VueArchiveExtension)))
        	// HO 11/03/2011 END *****************
            {
            	RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
            	rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
            }
            return true;

        } catch (Throwable t) {
            Log.error("Exception attempting to save file " + file + ": " + t);
            Throwable e = t;
            if (t.getCause() != null)
                e = t.getCause();
            if (e instanceof java.io.FileNotFoundException) {
                Log.error("Save Failed: " + e);
            } else {
                Log.error("Save failed for \"" + file + "\"; ", e);
                //tufts.Util.printStackTrace(e);
            }
            if (e != t)
                Log.error("Exception attempting to save file " + file + ": " + e);
            VueUtil.alert(String.format(Locale.getDefault(),VueResources.getString("saveaction.savemap.error")+ "\"%s\";\n"+VueResources.getString("saveaction.targetfiel")+"\n\n"+VueResources.getString("saveaction.problem"),
                                        map.getLabel(), file, Util.formatLines(e.toString(), 80)),
                          "Problem Saving Map");
        } finally {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                GUI.clearWaitCursor();
            }});
        }

        return false;
    }
    

    
    
    

    
    // HO 15/02/2011 BEGIN ****************
    /**
     * A function to auto-open a map immediately after saving.
     * Previously, when you saved in .vpk format, the original
     * .vue map would remain the active map and the new .vpk would
     * not open. It behaved like an export. Since making the .vpk
     * format the default, this is unintuitive behaviour.
     * This function auto-opens the new .vpk file and closes
     * the original .vue, if there is one.
     * @param map, the LWMap being saved
     * @param saveAs, true if the map may need to be saved under a new name,
     * false if using the original name
     * @param export, true if we are exporting the map, false otherwise
     * @return the newly-saved LWMap object
     * @author Helen Oliver
     */ 
    // HO 22/08/2011 BEGIN ***************
    // public static LWMap saveMapSpecial(LWMap map, boolean saveAs, boolean export)
    public static LWMap saveMapSpecial(LWMap map, boolean saveAs, boolean export, String caption)
    // HO 22/08/2011 END ***************
    {
        Log.info("saveMapSpecial: " + map);        
        
        GUI.activateWaitCursor();         
       
        // input validation
        if (map == null)
            return null;
        
        File file = map.getFile();
        // HO 21/12/2011 BEGIN *****
        boolean bWritable = FileLockAction.checkIfFileIsWritable(file, map, false, false);
        if (bWritable == false)
        	return null;
        // HO 21/12/2011 END ********
        
        // HO 22/02/2011 BEGIN *****************
        // get the name of the map we started with
        String strInitMapLabel = map.getLabel();
        // the file we started with
        File initFile = file;
        // HO 22/02/2011 END *****************
        int response = -1;
        
        // show the Save dialog
        if (map.getSaveFileModelVersion() == 0) {
        	final Object[] defaultOrderButtons = { VueResources.getString("saveaction.saveacopy"),VueResources.getString("saveaction.save")};
            Object[] messageObject = {map.getLabel()};
        	response = VueUtil.option
            (VUE.getDialogParent(),
        	 VueResources.getFormatMessage(messageObject, "dialog.saveaction.message"),         
        	 VueResources.getFormatMessage(messageObject, "dialog.saveaction.title"),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             defaultOrderButtons,             
             VueResources.getString("saveaction.saveacopy")
             );
        }
        
        // respond to user input
        if (response == 0) {
            saveAs=true;
        } if ((saveAs || file == null) && !export) {
        	// HO 22/08/2011 BEGIN ***************
            // file = ActionUtil.selectFile("Save Map", null);
        	file = ActionUtil.selectFile(caption, null);
            // HO 22/08/2011 END ***************
        } else if (export) {
            file = ActionUtil.selectFile("Export Map", "export");
        }
            
        if (file == null) {
            try {
                return null;
            } finally {
                GUI.clearWaitCursor();
            }
        }

        try {        	         	
            Log.info("saveMapSpecial: target[" + file + "]");
            
            final String name = file.getName().toLowerCase();

            if (name.endsWith(".rli.xml")) {
                new IMSResourceList().convert(map,file);
            }
            else if (name.endsWith(".xml") || name.endsWith(".vue")) {
                ActionUtil.marshallMap(file, map);

                // HO 18/02/2011 BEGIN **********
                // and then open the new map, if it's not
                // already in the process of being opened
                if (!bAutoOpeningMap) {
                	autoOpenMap(file);  
                }
                // HO 18/02/2011 END ************
                
                // HO 22/02/2011 BEGIN **********  
                // close the old map, if there is one
                if ((initFile != null) && (!initFile.equals(file))) {
                	// get all the open maps
                	Collection<LWMap> coll = VUE.getAllMaps();
                	// if there's more than one map open
                	if (coll.size() > 1) {
                	// cycle through them
	            		for (LWMap aMap: coll) {
	            				// if this open map hasn't got the same label
	            				// it must be the original map
	            				if(aMap.getLabel().equals(strInitMapLabel)) {
	            					// so close it without prompting
	    	    					VUE.closeMapSilently(map, false);
	            					// nullify this because it was causing memory leaks
	            					initFile = null;
	            				}
            				// nullify this because it was causing memory leaks
	            			aMap = null;
	            		}
                	}
                	// nullify this because it was causing memory leaks
                	coll = null;
                	
                }
                // HO 22/02/2011 END ********** 

            }
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                ImageConversion.createActiveMapJpeg(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".png"))
                ImageConversion.createActiveMapPng(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".svg"))
                SVGConversion.createSVG(file);
            
            else if (name.endsWith(".pdf"))
            {
            	PresentationNotes.createMapAsPDF(file);
            }
            else if (name.endsWith(".zip"))
            {   Vector resourceVector = new Vector();
            	Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            	while(i.hasNext()) {	
            		LWComponent component = (LWComponent) i.next();
            		System.out.println("Component:"+component+" has resource:"+component.hasResource());
            		if(component.hasResource() && (component.getResource() instanceof URLResource)){
                    
            			URLResource resource = (URLResource) component.getResource();                    
                
            			try {
                        if(resource.isLocalFile()) {
                        	String spec = resource.getSpec();                        	                        
                        	System.out.println(resource.getSpec());
                            Vector row = new Vector();
                            row.add(new Boolean(true));
                            row.add(resource);
                            row.add(new Long(file.length()));
                            row.add("Ready");
                            resourceVector.add(row);
                        }
            			}catch (Exception ex) {
            				System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
            				ex.printStackTrace();
            			}                    
            		}                
            	}
            	File savedCMap =PublishUtil.createZip(map, resourceVector);
            	InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                int fileLength = (int)savedCMap.length();
                byte bytes[] = new  byte[fileLength];
                try
                {
                	while (istream.read(bytes,0,fileLength) != -1)
                		ostream.write(bytes,0,fileLength);
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                }
                finally
                {
                	istream.close();
                	ostream.close();
                }
            }

            else if (name.endsWith(".html")) 
            {            	
            	HtmlOutputDialog hod = new HtmlOutputDialog();
            	hod.setVisible(true);   
            	if (hod.getReturnVal() > 0)
            		new ImageMap().createImageMap(file, hod.getScale(), hod.getFormat());
            }

            else if(name.endsWith(".rdf"))
            {
               edu.tufts.vue.rdf.RDFIndex index = new edu.tufts.vue.rdf.RDFIndex();
               
               String selectionType = VueResources.getString("rdf.export.selection");
               
               if(selectionType.equals("ALL"))
               {
                 Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
                 while(maps.hasNext())
                 {
                     index.index(maps.next());
                 }
               }
               else if(selectionType.equals("ACTIVE"))
               {
                 index.index(VUE.getActiveMap());  
               }    
               else
               {    
                 index.index(VUE.getActiveMap());
               }  
               FileWriter writer = new FileWriter(file);
               index.write(writer);
               writer.close();
            }
            else if (name.endsWith(VueUtil.VueArchiveExtension))
            {
                // HO 20/02/2011 BEGIN **********
            	// if it's a .vpk file we need to make sure we close and auto-open
            	// appropriately
            	Archive.writeArchiveSpecial(map, file);
            	// HO 20/02/2011 END **********
                
            } else {
                Log.warn("Unknown save type for filename extension: " + name);
                return null;
            }

			// don't know this as not all the above stuff is passing
            // exceptions on to us!
            Log.debug("Save completed for " + file);
            if (!VUE.isApplet())
            {
            	VueFrame frame = (VueFrame)VUE.getMainWindow();
            	String title = VUE.getName() + ": " + name;                      
            	frame.setTitle(title);
            }
            // HO 11/03/2011 BEGIN *****************
            // make sure new .vpk files appear in the Recent Files list
            //if (name.endsWith(".vue"))
            if ((name.endsWith(".vue")) || (name.endsWith(VueUtil.VueArchiveExtension)))
        	// HO 11/03/2011 END *****************
            {
            	RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
            	rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
            }
            // HO 16/02/2011 BEGIN **********
            if (map.getFile() == null) {
            	map.setFile(file);
            }
            // HO 16/02/2011 END **********
            return map;

        } catch (Throwable t) {
            Log.error("Exception attempting to save file " + file + ": " + t);
            Throwable e = t;
            if (t.getCause() != null)
                e = t.getCause();
            if (e instanceof java.io.FileNotFoundException) {
                Log.error("Save Failed: " + e);
            } else {
                Log.error("Save failed for \"" + file + "\"; ", e);
            }
            if (e != t)
                Log.error("Exception attempting to save file " + file + ": " + e);
            VueUtil.alert(String.format(Locale.getDefault(),VueResources.getString("saveaction.savemap.error")+ "\"%s\";\n"+VueResources.getString("saveaction.targetfiel")+"\n\n"+VueResources.getString("saveaction.problem"),
                                        map.getLabel(), file, Util.formatLines(e.toString(), 80)),
                          "Problem Saving Map");
        } finally {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                GUI.clearWaitCursor();
            }});
        }

        return null;
    }            
    // HO 15/02/2011 END ******************
    
    // HO 07/02/2011 BEGIN **********
    // flags whether or not we are already in the process of
    // opening the map
    public static boolean bAutoOpeningMap = false;
    
    /**
     * A routine to automatically open a map
     * @param theFile, the file to open
     * @author Helen Oliver
     */
    private static void autoOpenMap(File theFile) {
    	// flag start of the process of opening the map
    	bAutoOpeningMap = true;
    	
    	// open the map even if it is not the active map
    	VUE.displayMapSpecial(theFile);
    	
    	// flag end of the process of opening the map
    	bAutoOpeningMap = false;
    }
    // HO 07/02/2011 END **********


    private static void writeHTMLOutline(LWMap map, File file)
        throws IOException
    {
        PrintWriter out;
        if (file == null) {
            System.err.println("Writing to stdout");
            out = new PrintWriter(System.out, true);
        } else {
            System.out.println("Writing: " + file);
            out = new PrintWriter(file);
        }
            
        writeHTMLOutline(map, out);
        out.flush();
        out.close();
    }
    
    private static void writeHTMLOutline(LWMap map, PrintWriter out)
    {
        String title = map.getLabel();
        if (title.endsWith(".vue") || title.endsWith(".htm"))
            title = title.substring(0, title.length()-4);

        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + title + "</title>");
        out.println("<!-- generated by " + tufts.vue.Version.WhatString + " -->");
        out.println("<!-- generated on " + new java.util.Date() + " from " + map.getFile() + " -->");
        out.println("</head>");
        out.println("<body>");
        //out.println("<h1>" + title + "</h1>");

        writeUnorderedNode(map, out);
        
        out.println("</body>");
        out.println("</html>");
    }

    // TODO: make successfully recursive to depth, with deeper ordered
    // lists, tracking level so we can use <h2> or whatever at top level,
    // shrinking as we go down.

    private static void writeUnorderedNode(LWComponent node, PrintWriter out)
    {
        final java.util.List<LWComponent> topChildren = node.getChildList();
        final LWComponent[] ordered = topChildren.toArray(new LWComponent[topChildren.size()]);
        java.util.Arrays.sort(ordered, LWComponent.GridSorter);

        for (LWComponent c : ordered) {
            if (c.hasLabel()) {
                out.print("<h2>");
                writeLabel(c, out);
                out.print("</h2>");
            }
            writeNode(c, out);
        }
    }
    
    private static void writeNode(LWComponent c, PrintWriter out) {
        if (c instanceof LWGroup || c instanceof LWSlide)
            writeUnorderedNode(c, out);
        else if (c instanceof LWLink)
            ;
        else
            writeOrderedChildren(c, out);
    }
    
    private static void writeOrderedChildren(LWComponent node, PrintWriter out)
    {
        
        if (node.hasChildren()) {
            out.println("<ul>");
            for (LWComponent c : node.getChildList()) {
                out.print("<li>");
                writeLabel(c, out);
                if (c.hasChildren())
                    writeNode(c, out);
                //writeNode(node, out);
                out.println();
            }
            out.println("</ul>");
        }
        
    }

    private static void writeLabel(LWComponent node, PrintWriter out)
    {

        if (node.hasResource())
            out.println("<a href=\"" + node.getResource() + "\">");

        java.awt.Font f = node.getFont();
        if (f.isBold()) out.print("<b>");
        if (f.isItalic()) out.print("<i>");

        boolean didFontColor = false;
        if (node.getTextColor() != null && !java.awt.Color.black.equals(node.getTextColor())) {
            out.print("<font color=\"" + node.getXMLtextColor() + "\">");
            didFontColor = true;
        }
        
        out.print(node.getLabel());
        if (f.isBold()) out.print("</b>");
        if (f.isItalic()) out.print("</i>");

        if (didFontColor)
            out.print("</font>");

        if (node.hasResource())
            out.print("</a>");
        

    }
    
    public static boolean saveMap(LWMap map) {
        return saveMap(map, false,false);
    }
    
    // HO 22/02/2011 BEGIN ************
    /**
     * A routine to save a map that may not be the active map
     * @param map, the LWMap to save
     * @return the LWMap that has been saved
     * @author Helen Oliver
     */
    public static LWMap saveMapSpecial(LWMap map) {
    	// HO 22/02/2011 BEGIN ************
        // return saveMapSpecial(map, false,false);
    	return saveMapSpecial(map, false,false, "Save Map");
        // HO 22/02/2011 END ************
    }
    // HO 22/02/2011 END *************
    
    public static boolean PACKAGE_DEBUG = false;

    public static void main(String args[])
        throws IOException
    {
        //VUE.parseArgs(args);
        DEBUG.Enabled=true;
        DEBUG.IMAGE=true;
        PACKAGE_DEBUG = true;
        VUE.debugInit(false);
        Images.loadDiskCache();

        final LWMap map = ActionUtil.unmarshallMap(new File(args[0]));
        Log.debug("MAIN: Unmarshalled map: " + map);

        Archive.writeArchive(map, new File(map.getLabel().substring(0, map.getLabel().length() - 4) + VueUtil.VueArchiveExtension));
        //writeArchive(map, new File("test" + VueUtil.VueArchiveExtension));

        
//         if (args.length > 1)
//             writeHTMLOutline(map, new File(args[1]));
//         else
//             writeHTMLOutline(map, (File) null);

        
//         LWMap map = new LWMap("test");
//         map.setFile(new File("test.xml"));
//         map.addNode(new LWNode("Test Node"));
//         System.err.println("Attempting to save test map " + map);
//         DEBUG.Enabled = DEBUG.INIT = true;
//         new SaveAction().saveMap(map, false);
    }

    
    /*
    public void actionPerformed_writes_over_other_saved_maps(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
         
        boolean saveCondition = true;
        
        if (isSaveAs() || file == null)
        {
          file = ActionUtil.selectFile("Save Map", "xml");
          
          if (file == null)
              saveCondition = false;
        }
        
        if (saveCondition == true)
        {
            if (file.getName().endsWith(".xml")) {
                LWMap map = tufts.vue.VUE.getActiveMap();
                map.setLabel(file.getName());
                ActionUtil.marshallMap(file, map);
            }
          else if (file.getName().endsWith(".jpeg"))
            new ImageConversion().createJpeg(file);
          
          else if (file.getName().endsWith(".svg"))
            new SVGConversion().createSVG(file);
          
          else if (file.getName().endsWith(".pdf"))
            new PDFTransform().convert(file);
          
          else if (file.getName().endsWith(".html"))
            new HTMLConversion().convert(file);
          
          System.out.println("Saved " + getFileName());
        }
            
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    */

    
}






