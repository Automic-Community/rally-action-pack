package com.automic.agilecentral.actions;

import java.net.URI;
import java.net.URISyntaxException;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.constants.ExceptionConstants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.rallydev.rest.RallyRestApi;

/**
 * This class defines the execution of any action.It provides some
 * initializations and validations on common inputs .The child actions will
 * implement its executeSpecific() method as per their own need.
 */
public abstract class AbstractHttpAction extends AbstractAction {

	protected String apiKey;

	/**
	 * Service end point
	 */
	private URI baseUrl;

	/**
	 * Username for Login into CA Agile Central
	 */
	private String username;

	private String password;
	
	protected RallyRestApi rallyRestTarget;

	public AbstractHttpAction() {
		addOption(Constants.BASE_URL, true, "CA Agile Central URL");
		addOption(Constants.USERNAME, true, "Username for Login into CA Agile Central");
		addOption(Constants.API_KEY, false, "Rally Rest API Key");
	}

	/**
	 * This method initializes the arguments and calls the execute method.
	 *
	 * @throws AutomicException
	 *             exception while executing an action
	 */
	public final void execute() throws AutomicException {
		prepareCommonInputs();
		executeSpecific();
	}

	@SuppressWarnings("deprecation")
	private void prepareCommonInputs() throws AutomicException {
		String temp = getOptionValue(Constants.BASE_URL);
		this.username = getOptionValue("username");
		AgileCentralValidator.checkNotEmpty(username, "Username for Login into CA Agile Central");

		this.password = System.getenv(Constants.ENV_PASSWORD);

		try {
			this.baseUrl = new URI(temp);
		} catch (URISyntaxException e) {
			ConsoleWriter.writeln(e);
			String msg = String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, "URL", temp);
			throw new AutomicException(msg);
		}
		rallyRestTarget = new RallyRestApi(baseUrl, username, password);

	}

	/**
	 * Method to execute the action.
	 *
	 * @throws AutomicException
	 */
	protected abstract void executeSpecific() throws AutomicException;

	/**
	 * Method to initialize Client instance.
	 *
	 * @throws AutomicException
	 *
	 */

}