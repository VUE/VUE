/*
 * Export.java
 *
 * Created on Feb 27, 2009
 *
 * Copyright 2003-2009 Tufts University  Licensed under the
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

import java.io.File;

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
		
	}
	/** A method that creates a VPK from map file. T 
    *
    * @param mapFile is pointer to vue map file 
    * @param outputFile VPK output file
    * @throws java.lang.Exception
    */
	public void createVPK(String  mapFile, String outputFile) throws Exception { 
		
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
		
	}
	public static void main(String[] args) throws Exception {
        String inputFile = args[0];
        String outputFile = args[1];
        Export exporter = new Export();
        int option = 0;
        if(args.length == 3 && args[2] != null) {
        	 option = Integer.parseInt(args[2]);
         
        }  
        	exporter.export(inputFile, outputFile,option);
       
       }
	
}
