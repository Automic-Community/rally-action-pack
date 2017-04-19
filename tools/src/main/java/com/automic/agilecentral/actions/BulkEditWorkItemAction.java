/**
 * 
 */
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
 * @author sumitsamson
 *
 */
public class BulkEditWorkItemAction extends AbstractHttpAction{
    
    private String workSpace;
    private String project;
    private String workItemName;
    private String workItemType;
    private String workItemId;
    private String scheduleState;

    public BulkEditWorkItemAction() {
        addOption("workitemids", true, "Work item ids");
        addOption("workitemtype", true, "Work item type you wanted to edit");
        addOption("workitemname", false, "New workt item name");
        addOption("workspacename", false, "Workspace in which story is located");
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
                throw new AutomicException(Arrays.toString(updateResponse.getErrors()));
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(String.format("Error occured while updating work item id: %s", workItemId));
        }

    }

    private UpdateRequest checkInputsAndPrepareRequest() throws AutomicException {
        JsonObject updateObj = new JsonObject();

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        String workItemRef = null;

        workSpace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workSpace)) {
            workSpace = RallyUtil.getWorspaceRef(rallyRestTarget, workSpace);
            updateObj.addProperty(Constants.WORKSPACE, workSpace);
        }

        // checking if given project exists
        project = getOptionValue("projectname");
        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectRef(rallyRestTarget, project, workSpace);
            updateObj.addProperty(Constants.PROJECT, project);
        }

        // checking if given work item exists
        workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "ID of the work item(User story,Defect etc.)");
        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpace, workItemType);

        // adding new work item name
        workItemName = getOptionValue("workitemname");
        if (CommonUtil.checkNotEmpty(workItemName)) {
            updateObj.addProperty("Name", workItemName);
        }

        // adding new work item scheduled state
        scheduleState = getOptionValue("schedulestate");
        if (CommonUtil.checkNotEmpty(scheduleState)) {
            updateObj.addProperty("ScheduleState", scheduleState);
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
            String description = /*CommonUtil.readFileIntoString(temp)*/null;
            updateObj.addProperty("Description", description);

        }

        ConsoleWriter.writeln("Request Json Object: " + updateObj);
        return new UpdateRequest(workItemRef, updateObj);

    }

}
