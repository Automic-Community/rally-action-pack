/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

/**
 * @author sumitsamson
 *
 */
public class CreateWorkItemAction extends AbstractHttpAction {

    private String workspace;
    private String project;
    private String workItemName;
    private String workItemType;
    private String scheduleState;

    public CreateWorkItemAction() {

        addOption("workitemname", true, "Name of the work item");
        addOption("workitemtype", true, "Work item type you wanted to create");
        addOption("workspacename", false, "Workspace in which project is located");
        addOption("projectname", false, "Project Name where work item needs to be created");
        addOption("descriptionfilepath", false, "Description file path");
        addOption("schedulestate", false, "Schedule state of work");
        addOption("customfilepath", false, "Custom fields file path");

    }

    @Override
    protected void executeSpecific() throws AutomicException {

        JsonObject newObj = new JsonObject();
        checkandPrepareInputs(newObj);

        ConsoleWriter.writeln("Request Json Object: " + newObj);
        CreateRequest createRequest = new CreateRequest(workItemType, newObj);
        try {
            CreateResponse createResponse = rallyRestTarget.create(createRequest);
            ConsoleWriter.writeln("Response Json Object: " + createResponse.getObject());
            if (createResponse.wasSuccessful()) {
                ConsoleWriter.writeln(
                        "UC4RB_AC_WORK_ITEM_ID ::=" + createResponse.getObject().get("FormattedID").getAsString());
                ConsoleWriter.writeln("UC4RB_AC_WORK_ITEM_OBJ_ID ::=" + createResponse.getObject().get("ObjectID"));

            } else {
                throw new AutomicException(Arrays.toString(createResponse.getErrors()));
            }

        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private void checkandPrepareInputs(JsonObject newObj) throws AutomicException {
        workItemName = getOptionValue("workitemname");
        AgileCentralValidator.checkNotEmpty(workItemName, "Name of user story");
        newObj.addProperty("Name", workItemName);

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        workspace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workspace)) {
            workspace = RallyUtil.getWorspaceRef(rallyRestTarget, workspace);
            newObj.addProperty(Constants.WORKSPACE, workspace);
        }

        project = getOptionValue("projectname");
        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectRef(rallyRestTarget, project, workspace);
            newObj.addProperty(Constants.PROJECT, project);
        }

        scheduleState = getOptionValue("schedulestate");
        if (CommonUtil.checkNotEmpty(scheduleState)) {
            newObj.addProperty("ScheduleState", scheduleState);
        }

        // Custom fields addition
        String temp = getOptionValue("customfilepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            File file = new File(temp);
            AgileCentralValidator.checkFileExists(file);
            RallyUtil.processCustomFields(temp, newObj);
        }

        // description addition
        temp = getOptionValue("descriptionfilepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            File file = new File(temp);
            AgileCentralValidator.checkFileExists(file);
            try {
                String description = new String(Files.readAllBytes(Paths.get(temp)));
                newObj.addProperty("Description", description);
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException("Error occured while reading description from temp file" + e.getMessage());
            }

        }

    }

}