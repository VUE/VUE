package edu.tufts.seasr;
/*
*
* Connector.java
*
* Created on June 11, 2009, 5:00 PM
*
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

/**
 * @author akumar03
 * This class contains methods to connect to instance of seasr specified in the properties
 * file and get flows that can be executed by VUE
 */

import java.net.*;
import java.io.*;

import org.castor.mapping.BindingType;
import org.castor.mapping.MappingUnmarshaller;
import org.exolab.castor.xml.ClassDescriptorResolverFactory;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.XMLClassDescriptorResolver;
import org.exolab.castor.xml.XMLMappingLoader;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.mapping.MappingLoader;
import org.xml.sax.InputSource;
 


import sun.misc.BASE64Encoder;
import tufts.vue.VueResources;
 
public class Connector {
	private static Unmarshaller unmarshaller = null;
	private static URL XML_MAPPING;
	
	private final String username = "admin";
	private final String password = "admin";
	
	public Connector() {
		XML_MAPPING  = this.getClass().getResource("MeandreResponse.xml"); 
		System.out.println("XML_MAPPING:"+XML_MAPPING);
	} 
	public String getFlowsByTag(String tag) throws Exception {
//		String url =  "http://" + username + ":" + password + "@"+VueResources.getString("seasr.address")+":"+VueResources.getString("seasr.port")+"/services/repository/flows_by_tag.xml?tag=vue";
		String url =  "http://localhost:1714/services/repository/flows_by_tag.xml?tag=vue";
		System.out.println(url);
		String login = username+":"+password;
		String encodedLogin =   new BASE64Encoder().encode(login.getBytes());
		System.out.println(login);
		URL seasrUrl = new URL(url);
		  URLConnection connection = seasrUrl.openConnection();
		  connection.setRequestProperty("Authorization", "Basic " + encodedLogin);

          BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
          int bytesRead = 0;
          byte[] buffer = new byte[1024];
          while ((bytesRead = stream.read(buffer)) != -1) {
              
              //Process the chunk of bytes read
              //in this case we just construct a String and print it out
              String chunk = new String(buffer, 0, bytesRead);
              System.out.print(chunk);
          }
          stream.close();

		return "test";
		
	}
	
	public   MeandreResponse parseMeandreResponse(String tag) 
	{
		try {
	      String url =  "http://"+VueResources.getString("seasr.address")+":"+VueResources.getString("seasr.port")+"/services/repository/flows_by_tag.xml?tag="+tag;
			System.out.println(url);
			String login = username+":"+password;
			String encodedLogin =   new BASE64Encoder().encode(login.getBytes());
			System.out.println(login);
			URL seasrUrl = new URL(url);
			URLConnection connection = seasrUrl.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + encodedLogin);

	       BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
	       int bytesRead = 0;
	          byte[] buffer = new byte[1024];
	          String response = "";
	          while ((bytesRead = stream.read(buffer)) != -1) {
	               String chunk = new String(buffer, 0, bytesRead);
	              response += chunk;
	              System.out.print(chunk);
	          }
	          Mapping mapping = new Mapping();
	          mapping.loadMapping(XML_MAPPING);
			Unmarshaller unmarshaller = getUnmarshaller();
			unmarshaller.setValidation(false);
			MeandreResponse r = (MeandreResponse) unmarshaller.unmarshal(new InputSource(new StringReader(response)));
			return r;
		} catch (Exception e) {
			System.out.println("edu.tufts.seasr.Connector.parseMeandreResponse " + e);
			e.printStackTrace();
			return null;
		}
	}
	private static Unmarshaller getUnmarshaller() 
	{
		if (unmarshaller == null) {
			unmarshaller = new Unmarshaller();
			Mapping mapping = new Mapping();
			try {
				mapping.loadMapping(XML_MAPPING);
				unmarshaller.setMapping(mapping);
			} catch (Exception e) {
				System.out.println("edu.tufts.seasr.Connector.getUnmarshaller: " + XML_MAPPING+e);
			}
		}
		return unmarshaller;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Testing Connection to SEASR");
		Connector c = new Connector();
		
		MeandreResponse r = c.parseMeandreResponse(null);
		for(MeandreItem item:r.getMeandreItemList()) {
			System.out.println("uri: "+item.getMeandreUri() );
			System.out.println("Name: "+item.getMeandreUriName());
		}
	}

}
