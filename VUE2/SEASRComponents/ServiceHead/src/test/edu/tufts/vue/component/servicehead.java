/*
 * @(#) NonWebUIFragmentCallback.java @VERSION@
 * 
 * Copyright (c) 2008+ Amit Kumar
 * 
 * The software is released under ASL 2.0, Please
 * read License.txt
 *
 */

package edu.tufts.vue.component;

import java.util.logging.Logger;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

/** This executable component just concatenates the two input strings received
 * pushing it to the output.
 *
 * @author John Doe;
 *
 */
@Component(creator="Joe", description="Sample Component", 
		name="MyComponent",
		tags="test print hello")
public class servicehead implements ExecutableComponent {


	@ComponentInput(description="First String", name="string_one")
	final static String DATA_INPUT_1= "string_one";
	
	
	@ComponentInput(description="Second String", name="string_two")
	final static String DATA_INPUT_2= "string_two";
	

	@ComponentOutput(description="Output String", name="string_out")
	final static String DATA_OUTPUT_1= "string_concatenated";

	
	// log messages are here
	private Logger _logger;
	
	/** This method is invoked when the Meandre Flow is being prepared for 
	 * getting run.
	 *
	 * @param ccp The properties associated to a component context
	 */
	public void initialize ( ComponentContextProperties ccp ) {
		this._logger = ccp.getLogger();
	}

	/** This method just pushes a concatenated version of the entry to the
	 * output.
	 *
	 * @throws ComponentExecutionException If a fatal condition arises during
	 *         the execution of a component, a ComponentExecutionException
	 *         should be thrown to signal termination of execution required.
	 * @throws ComponentContextException A violation of the component context
	 *         access was detected

	 */
	public void execute(ComponentContext cc) throws ComponentExecutionException, ComponentContextException {
		String str1 = cc.getDataComponentFromInput(DATA_INPUT_1).toString();
		_logger.info("Got String1: " + str1);
		String str2 = cc.getDataComponentFromInput(DATA_INPUT_2).toString();
		_logger.info("Got String2: " + str2);
		_logger.info("Pushing out: " + str1+str2);
		cc.pushDataComponentToOutput(DATA_OUTPUT_1,str1+str2);
	}

	/** This method is called when the Menadre Flow execution is completed.
	 *
	 * @param ccp The properties associated to a component context
	 */
	public void dispose ( ComponentContextProperties ccp ) {

	}
}
