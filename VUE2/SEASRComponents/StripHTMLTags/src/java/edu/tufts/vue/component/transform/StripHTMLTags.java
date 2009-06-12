package edu.tufts.vue.component.transform;


import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;


@Component(baseURL = "meandre://seasr.org/components/demo/", creator = "Anoop Kumar", description = "This component removes HTML tags "
	+ " Version 1.1 ", name = "Strip HTML Tags", tags = "tags, strip html", mode = Mode.compute, firingPolicy = Component.FiringPolicy.all)
public class StripHTMLTags implements ExecutableComponent {
	@ComponentInput(description = "Input String" 
			+ "<br>TYPE:String", name = "String")
	public final static String DATA_INPUT = "String";

	@ComponentOutput(description = "Output String"
			+ "<br>TYPE: String", name = "String")
	public final static String DATA_OUTPUT = "String";

	public void dispose(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub

	}

	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {
		String inputString = (String) cc.getDataComponentFromInput(DATA_INPUT);
		String outputString = inputString.replaceAll("\\<.*?>","");
		cc.pushDataComponentToOutput(DATA_OUTPUT, outputString);
		// TODO Auto-generated method stub

	}

	public void initialize(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub

	}

}
