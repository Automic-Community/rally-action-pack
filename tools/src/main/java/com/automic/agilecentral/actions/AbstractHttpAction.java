package com.automic.agilecentral.actions;

import java.net.URI;
import java.net.URISyntaxException;

import com.automic.agilecentral.config.InsecureRallyRestApi;
import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.constants.ExceptionConstants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.rallydev.rest.RallyRestApi;

/**
 * This class defines the execution of any action.It provides some initializations and validations on common inputs .The
 * child actions will implement its executeSpecific() method as per their own need.
 */
public abstract class AbstractHttpAction extends AbstractAction {

    /**
     * apiKey to make the request for all the actions
     */
    protected RallyRestApi rallyRestTarget;

    /**
     * Service end point
     */
    private URI baseUrl;

    /**
     * apikey to connect to agile central
     */
    private String apiKey;

    /**
     * Username for Login into CA Agile Central
     */
    private String username;

    /**
     * Password to the username
     */
    private String password;

    /**
     * Api version of agile central
     */
    private String apiVersion;

    public AbstractHttpAction() {
        addOption(Constants.BASE_URL, true, "CA Agile Central URL");
        addOption(Constants.USERNAME, false, "Username for Login into CA Agile Central");
        addOption(Constants.SKIP_CERT_VALIDATION, true, "Skip SSL Validation");
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
        this.username = getOptionValue(Constants.USERNAME);
        this.password = System.getenv(Constants.ENV_PASSWORD);
        this.apiKey = System.getenv(Constants.ENV_API_TOKEN);

        // check if login parameters are provided
        if (!CommonUtil.checkNotEmpty(username) && !CommonUtil.checkNotEmpty(apiKey)) {
            throw new AutomicException("Provide either username and password or the api key");
        }

        String temp = getOptionValue(Constants.BASE_URL);
        try {
            this.baseUrl = new URI(temp);
        } catch (URISyntaxException e) {
            ConsoleWriter.writeln(e);
            String msg = String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, "URL", temp);
            throw new AutomicException(msg);
        }

        // if cert validation needs to be skipped
        if (CommonUtil.convert2Bool(getOptionValue(Constants.SKIP_CERT_VALIDATION))) {
            if (CommonUtil.checkNotEmpty(apiKey)) {
                new InsecureRallyRestApi(baseUrl, apiKey);
            } else {
                new InsecureRallyRestApi(baseUrl, username, password);
            }
        }

        // for performing all the CRUD and query operations.
        if (CommonUtil.checkNotEmpty(apiKey)) {
            rallyRestTarget = new RallyRestApi(baseUrl, apiKey);
        } else {
            rallyRestTarget = new RallyRestApi(baseUrl, username, password);
        }
        
        //setting the api version
        this.apiVersion = CommonUtil.getEnvParameter(Constants.ENV_API_VERSION, Constants.AC_API_VERSION);
        rallyRestTarget.setWsapiVersion(apiVersion);
    }

    /**
     * Method to execute the action.
     *
     * @throws AutomicException
     */
    protected abstract void executeSpecific() throws AutomicException;

}