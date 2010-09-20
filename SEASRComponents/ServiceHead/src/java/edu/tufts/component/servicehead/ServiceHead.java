/*
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

/*
 * ServiceHead.java
 *
 *  
 */

package edu.tufts.component.servicehead;


import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;
import org.meandre.webui.ConfigurableWebUIFragmentCallback;
import org.meandre.webui.WebUIException;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;



// ------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/demo/", 
		creator = "Xavier Llor&agrave, modified by Anoop", 
		description = "Service head for a service that gets data via posts v1.1.2 ", 
		name = "Service head", tags = "WebUI, process request", 
		mode = Mode.webui, firingPolicy = Component.FiringPolicy.all
)
// -------------------------------------------------------------------------

/**
 *  This is a component to take url requests for seasr components.
 *  This is based on code provided by Xavier
 * 
 * @author Xavier Lor&agrave;
 * @ Modified by Anoop Kumar for VUE
 */
public class ServiceHead extends  AbstractExecutableComponent implements ConfigurableWebUIFragmentCallback {

	// -------------------------------------------------------------------------

	@ComponentProperty(
			description = "The URL path that the component will respond to",
			name = "url_path",
			defaultValue = "/service/ping"
	)
	public final static String PROP_URL_PATH = "url_path";
	
	@ComponentOutput(
			description = "A map object containing the key elements on the request and the assiciated values", 
			name =  Names.PORT_REQUEST_DATA
	)
	public final static String OUTPUT_VALUEMAP = Names.PORT_REQUEST_DATA;


	
	@ComponentOutput(
			description = "The response to be sent to the Service Tail Post.", 
			name = Names.PORT_RESPONSE_HANDLER
	)
	public final static String OUTPUT_RESPONSE = Names.PORT_RESPONSE_HANDLER;

	
	@ComponentOutput(
			description = "The semaphore to signal the response was sent.", 
			name =Names.PORT_SEMAPHORE
	)
	public final static String OUTPUT_SEMAPHORE = Names.PORT_SEMAPHORE;


	
	// -------------------------------------------------------------------------

	private PrintStream console;
	private ComponentContext ccHandle;

	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------

	public String getContextPath() {
		return ccHandle.getProperty(PROP_URL_PATH);
	}
	
	public void emptyRequest(HttpServletResponse response)
			throws WebUIException {
//		Log.warn("Empty request should have never been called! for "+ccHandle.getProperty(PROP_URL_PATH));
	}

	@SuppressWarnings("unchecked")
	public void handle(HttpServletRequest request, HttpServletResponse response)
	throws WebUIException {
		Map<String,byte[]> map = new Hashtable<String,byte[]>();
		Enumeration mapRequest = request.getParameterNames();
		while ( mapRequest.hasMoreElements() ) {
			String sName = mapRequest.nextElement().toString();
			String [] sa = request.getParameterValues(sName);
			String sAcc = "";
			for ( String s:sa ) sAcc+=s;
			try {
                map.put(sName, sAcc.getBytes("UTF-8"));
			}
            catch (UnsupportedEncodingException e) {
                throw new WebUIException(e);
            }
		}
		try {
			Semaphore sem = new Semaphore(1, true);
			sem.acquire();
			ccHandle.pushDataComponentToOutput(OUTPUT_VALUEMAP,  BasicDataTypesTools.mapToByteMap(map));
			ccHandle.pushDataComponentToOutput(OUTPUT_RESPONSE, response);
			ccHandle.pushDataComponentToOutput(OUTPUT_SEMAPHORE, sem);
			sem.acquire();
			sem.release();
		} catch (InterruptedException e) {
			throw new WebUIException(e);
		} catch (ComponentContextException e) {
			throw new WebUIException(e);
		}
		
	}

	@Override
	public void disposeCallBack(ComponentContextProperties ccp)
			throws Exception {
		console.println("[INFO] Disposing service head for " + ccp.getProperty(PROP_URL_PATH));

	}

	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
		try {
			this.ccHandle = cc;
			cc.startWebUIFragment((ConfigurableWebUIFragmentCallback)this);
			console.println("[INFO] Starting service head for " + cc.getProperty(PROP_URL_PATH));
			while (!cc.isFlowAborting()) {
				Thread.sleep(1000);
			}
			console.println("[INFO] Aborting for service head for" + cc.getProperty(PROP_URL_PATH));
			cc.stopWebUIFragment(this);
		} catch (Exception e) {
			throw new ComponentExecutionException(e);
		}
		
	}

	@Override
	public void initializeCallBack(ComponentContextProperties ccp)
			throws Exception {
		console = ccp.getOutputConsole();
		
		console.println("[INFO] Initializing service head for " + ccp.getProperty(PROP_URL_PATH));
			
	}

}
