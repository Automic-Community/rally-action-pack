package com.automic.agilecentral.cli;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.automic.agilecentral.constants.ExceptionConstants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.ConsoleWriter;

/**
 * This class is used to parse the arguments against provided options using apache cli library. Further this class
 * provides method to retrieve the argument value.
 */
public class Cli {

    private CommandLine cmd = null;

    public Cli(CliOptions options, String[] args) throws AutomicException {
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options.getOptions(), args, true);
        } catch (ParseException e) {
            ConsoleWriter.writeln(e);
            ConsoleWriter.writeln("Error parsing the command line options");
            printHelp(options.getOptions());
            throw new AutomicException(String.format(ExceptionConstants.INVALID_ARGS, e.getMessage()));
        }
    }

    public String getOptionValue(String arg) {
        String value = cmd.getOptionValue(arg);
        return (value != null ? value.trim() : value);
    }

    public void log(List<String> ignoreOptions) throws AutomicException {
        ConsoleWriter.newLine();
        ConsoleWriter.writeln("******     Input Parameters     ******");
        ConsoleWriter.newLine();
        for (Option o : cmd.getOptions()) {
            if (!ignoreOptions.contains(o.getOpt())) {
                ConsoleWriter.writeln("Parameter [" + o.getDescription() + "] = " + o.getValue());
            }

        }
        ConsoleWriter.newLine();
    }

    private void printHelp(Options options) {
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp("Usage ", options);
    }
}
