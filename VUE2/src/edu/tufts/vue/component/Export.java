/*
 * Export.java
 *
 * Created on Feb 27, 2009
 *
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
package edu.tufts.vue.component;

import tufts.vue.action.OpenAction;
import tufts.vue.action.ImageConversion;
import tufts.vue.action.SVGConversion;
import tufts.vue.action.ImageMap;
import tufts.vue.action.Archive;
import edu.tufts.vue.rdf.RDFIndex;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import tufts.vue.*;
/**
 * @author akumar03
 *
 */



public class Export {
	/** A method that creates a pdf from map file. T 
     *
     * @param mapFile is pointer to vue map file 
     * @param outputFile pdf output file
     * @throws java.lang.Exception
     */
	public void createPdf(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile);
		PresentationNotes.createMapAsPDF(new File(outputFile), map);
	}
	
	/** A method that creates a JPEG from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile JPEG output file
    * @throws java.lang.Exception
    */
	public void createJPEG(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile);
		ImageConversion.convert(map.getAsImage(), new File(outputFile), ImageConversion.JPEG);	
	}
	/** A method that creates a HTML from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile HTML output file
    * @throws java.lang.Exception
    */
	public void createHTML(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile); 
		new ImageMap().createImageMap(new File(outputFile), map,1.0);
	}
	/** A method that creates a PNG from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile PNG output file
    * @throws java.lang.Exception
    */
	public void createPNG(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile);
		ImageConversion.convert(map.getAsImage(), new File(outputFile), ImageConversion.PNG);
	}
	
	/** A method that creates a SVG from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile SVG output file
    * @throws java.lang.Exception
    */
	public void createSVG(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile);
		SVGConversion.createSVG(new File(outputFile), map);
	}
	/** A method that creates a RDF from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile RDF output file
    * @throws java.lang.Exception
    */
	public void createRDF(String  mapFile, String outputFile) throws Exception {
	    LWMap map = OpenAction.loadMap(mapFile);
		RDFIndex index = new  RDFIndex();
		index.indexMap(map);
		FileWriter writer = new FileWriter(new File(outputFile));
        index.write(writer);
        writer.close();
	}
	/** A method that creates a VPK from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile VPK output file
    * @throws java.lang.Exception
    */
	public void createVPK(String  mapFile, String outputFile) throws Exception { 
		LWMap map = OpenAction.loadMap(mapFile);
		Archive.writeArchive(map, new File(outputFile));
	}
	public void export(String inputFile,String outputFile,String option) throws Exception {
		HashMap<String,Integer> optionsMap = new HashMap<String,Integer>();
		optionsMap.put("jpeg",0);
		optionsMap.put("png",1);
		optionsMap.put("pdf",2);
		optionsMap.put("html",3);
		optionsMap.put("htm",3);
		optionsMap.put("svg",4);
		optionsMap.put("rdf",5);
		optionsMap.put("vpk",6);
		export(inputFile,outputFile,optionsMap.get(option.toLowerCase()));
	}
	public void export(String inputFile,String outputFile,int option) throws Exception {
		switch (option)  {
			case 0:
				createJPEG(inputFile, outputFile);
				break;
			case 1:
				createPNG(inputFile,outputFile);
				break;
			case 2:
				createPdf(inputFile,outputFile);
				break;
			case 3:
				createHTML(inputFile,outputFile);
				break;
			case 4:
				createSVG(inputFile,outputFile);
				 break;
			case 5:
				createRDF(inputFile,outputFile);
				 break;
			case 6:
				createVPK(inputFile,outputFile);
				break;
			default:
				printHelp();
				break;
		}
	}
	public void printHelp() {
		System.out.println("Usage: java -jar VUEExport.jar <input file(vue map)> <output file> [option]");
		System.out.println();
		System.out.println("The arguments  are:");
		System.out.println("-h or --help  : prints this informaion");
		System.out.println("<input file>  : this is a vue map of the type .vue or .vpk extention" );
		System.out.println("<output file> : location to output file" );
		System.out.println("[option]      : a number or format of the output from the following list");
		System.out.println("   			     0, jpeg or no option - saves to jpeg format" );
		System.out.println("   			     1, png - saves to png format" );
		System.out.println("   			     2, pdf - saves to pdf format" );
		System.out.println("   			     3, html - saves to html format" );
		System.out.println("   			     4, svg - saves to svg format" );
		System.out.println("   			     5, rdf - saves to rdf format" );
		System.out.println("   			     6, vpk - saves to vpk format" );
		
	}
	public static void main(String[] args) throws Exception {
		Export exporter = new Export();    
		if(args.length < 2) {
			exporter.printHelp();
			System.exit(0);
		}
		if(args[0] != null ) {
			if(args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")) {
				exporter.printHelp();
				System.exit(0);
			}
		}
        String inputFile = args[0];
        String outputFile = args[1];
        int option = 0;
        if(args.length == 3 && args[2] != null) {
        	try {
        		option = Integer.parseInt(args[2]);
        		exporter.export(inputFile, outputFile,option);
        	} catch(Exception ex) {
        		exporter.export(inputFile,outputFile,args[2]);
        	}
        } else {  
        	exporter.export(inputFile, outputFile,0);
        }
	}
}
