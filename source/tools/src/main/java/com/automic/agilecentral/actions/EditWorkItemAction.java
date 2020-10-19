package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.UpdateResponse;

/**
 * @author sumitsamson This class is used to edit the work item (user story,defect etc.).
 *
 */
public class EditWorkItemAction extends AbstractHttpAction {

    private String workItemId;

    public EditWorkItemAction() {
        addOption("workitemid", true, "Work item id");
        addOption("workitemtype", true, "Work item type you wanted to edit");
        addOption("workitemname", false, "New work item name");
        addOption("workspacename", false, "Workspace in which workitem is located");
        addOption("projectname", false, "New project name");
        addOption("descriptionfilepath", false, "Description file path");
        addOption("schedulestate", false, "New schedule state of work item");
        addOption("customfilepath", false, "Custom fields file path");

    }

    @Override
    protected void executeSpecific() throws AutomicException {

        UpdateRequest updateRequest = checkInputsAndPrepareRequest();
        try {
            UpdateResponse updateResponse = rallyRestTarget.update(updateRequest);
            ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
            if (!updateResponse.wasSuccessful()) {
                ConsoleWriter.writeln(Arrays.toString(updateResponse.getErrors()));
                throw new AutomicException("Unable to update the work item.");
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(String.format("Error occured while updating work item id: %s", workItemId));
        }

    }

    private UpdateRequest checkInputsAndPrepareRequest() throws AutomicException {
        JsonObject updateObj = new JsonObject();

        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        String workItemRef = null;

        String workSpace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workSpace)) {
            workSpace = RallyUtil.getWorspaceRef(rallyRestTarget, workSpace);
            updateObj.addProperty(Constants.WORKSPACE, workSpace);
        }

        // checking if given project exists
        String project = getOptionValue("projectname");
        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectRef(rallyRestTarget, project, null);
            updateObj.addProperty(Constants.PROJECT, project);
        }

        // checking if given work item exists
        workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "ID of the work item(User story,Defect etc.)");
        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpace, workItemType);

        // adding new work item name
        String workItemName = getOptionValue("workitemname");
        if (CommonUtil.checkNotEmpty(workItemName)) {
            updateObj.addProperty(Constants.NAME, workItemName);
        }

        // adding new work item scheduled state
        String scheduleState = getOptionValue("schedulestate");
        if (CommonUtil.checkNotEmpty(scheduleState)) {
            updateObj.addProperty(Constants.SCHEDULE_STATE, scheduleState);
        }

        // Custom fields addition
        String temp = getOptionValue("customfilepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            File file = new File(temp);
            AgileCentralValidator.checkFileExists(file);
            RallyUtil.processCustomFields(temp, updateObj);
        }

        // adding new work item description
        temp = getOptionValue("descriptionfilepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            String description = CommonUtil.readFileIntoString(temp);
            updateObj.addProperty(Constants.DESCRIPTION, description);

        }

        ConsoleWriter.writeln("Request Json Object: " + updateObj);
        return new UpdateRequest(workItemRef, updateObj);
    }
}
