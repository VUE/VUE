package edu.tufts.component.servicetail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;
import java.util.Map;
import java.beans.XMLEncoder;


import javax.servlet.http.HttpServletResponse;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

@Component(
		baseURL = "meandre://seasr.org/components/demo/", 
		creator = "Anoop Kuamr", 
		description = "Service tail that works with VUE", 
		name = "Service Tail", tags = "WebUI, process request", 
		mode = Mode.webui, firingPolicy = Component.FiringPolicy.all
)

public class ServiceTailVUE implements ExecutableComponent {

	@ComponentInput(description = "A Map containing the output to response", name = "object")
	public final static String INPUT_OBJECT= "object";

	@ComponentInput(description = "The response sent by the Service Head.", name = "response")
	public final static String INPUT_RESPONSE = "response";

	@ComponentInput(description = "The semaphore to signal the response was sent.", name = "semaphore")
	public final static String INPUT_SEMAPHORE = "semaphore";

	// -------------------------------------------------------------------------

	public void initialize(ComponentContextProperties ccp)
			throws ComponentExecutionException, ComponentContextException {
	}

	public void dispose(ComponentContextProperties ccp)
			throws ComponentExecutionException, ComponentContextException {
	}

	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {

		 Object object =   cc.getDataComponentFromInput(INPUT_OBJECT);
				
		Semaphore sem = (Semaphore) cc
				.getDataComponentFromInput(INPUT_SEMAPHORE);
		HttpServletResponse response = (HttpServletResponse) cc
				.getDataComponentFromInput(INPUT_RESPONSE);
		PrintStream ccHandle = cc.getOutputConsole();

		ccHandle.println("[INFO] Sending requested results");
		try {
			PrintWriter pw = response.getWriter();
			response.setContentType("text/xml");
			pw.println(mapToXML(object));
			response.getWriter().flush();
			sem.release();
		} catch (IOException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);
			ccHandle.println(baos.toString());
		}
	}

	private String mapToXML(Object object) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLEncoder e = new XMLEncoder(os);
		e.writeObject(object);
		e.close();
		return os.toString();
	}
}
