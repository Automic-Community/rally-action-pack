package com.automic.agilecentral.actions;

import java.util.Arrays;

import com.automic.agilecentral.cli.Cli;
import com.automic.agilecentral.cli.CliOptions;
import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;

/**
 * An abstract action which parses the command line parameters using apache cli and further calls the execute method.
 * The implementation of execute method will be provided by the subclass of this class. This class also provides the
 * method to retrieve the arguments which can be used inside execute method.
 */
public abstract class AbstractAction {

    private CliOptions actionOptions;
    private Cli cli;

    public AbstractAction() {
        actionOptions = new CliOptions();
    }

    /**
     * This method is used to add the argument.
     * 
     * @param optionName
     *            argument key used to identify the argument.
     * @param isRequired
     *            true/false. True means argument is mandatory otherwise it is optional.
     * @param description
     *            represents argument description.
     */
    public final void addOption(String optionName, boolean isRequired, String description) {
        actionOptions.addOption(optionName, isRequired, description);
    }

    /**
     * This method is used to retrieve the value of specified argument.
     * 
     * @param arg
     *            argument key for which you want to get the value.
     * @return argument value for the specified argument key.
     */
    public final String getOptionValue(String arg) {
        return cli.getOptionValue(arg);
    }

    public final void executeAction(String[] args) throws AutomicException {
        cli = new Cli(actionOptions, args);
        cli.log(Arrays.asList(new String[] { Constants.ACTION }));
        execute();
    }

    /**
     * Method to execute the action.
     * 
     * @throws AutomicException
     */
    protected abstract void execute() throws AutomicException;

}
