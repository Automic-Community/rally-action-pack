package com.automic.agilecentral.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.automic.agilecentral.constants.Constants;

/**
 * This class is used to instantiate the Options for CA Agile Central.This class only initializes the action and help
 * options.Further action specific options should be added to the same object (i.e CliOptions object) instantiated by
 * this class
 */

public class CliOptions {
    private static final boolean ISREQUIRED = true;
    private final Options osOpts;

    public CliOptions() {
        osOpts = new Options();
        osOpts.addOption(Option.builder(Constants.ACTION).required(ISREQUIRED).hasArg().desc("Action Name").build());
        osOpts.addOption(Option.builder(Constants.HELP).required(!ISREQUIRED).desc("Usage").build());
    }

    public void addOption(String optionName, boolean isRequired, String description) {
        osOpts.addOption(Option.builder(optionName).required(isRequired).hasArg().desc(description).build());
    }

    Options getOptions() {
        return osOpts;
    }
}
