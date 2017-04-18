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
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.UpdateResponse;

/**
 * @author sumitsamson
 *
 */
public class EditWorkItemAction extends AbstractHttpAction {

    private String workSpace;
    private String project;
    private String workItemName;
    private String workItemType;
    private String workItemId;
    private String scheduleState;

    public EditWorkItemAction() {
        addOption("workitemid", true, "Work item id");
        addOption("workitemtype", true, "Work item type you wanted to edit");
        addOption("workitemname", true, "New workt item name");
        addOption("workspacename", false, "Workspace in which project is located");
        addOption("projectname", false, "New project name");
        addOption("descriptionfilepath", false, "Description file path");
        addOption("schedulestate", false, "New schedule state of work item");
        addOption("filepath", false, "Custom fields file path");

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

        workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "ID of the work item(User story,Defect etc.)");

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        String workItemRef = null;

        // checking if given workspace name exists
        workSpace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workSpace)) {
            String workSpaceId = RallyUtil.getWorspaceId(rallyRestTarget, workSpace);
            updateObj.addProperty(Constants.WORKSPACE, "/workspace/" + workSpaceId);

        }

        // checking if given work item exists
        workItemRef = RallyUtil.getUserStoryRef(rallyRestTarget, workItemId, null, workSpace);

        // checking if given project exists
        project = getOptionValue("projectname");
        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectId(rallyRestTarget, project, workSpace);
            updateObj.addProperty(Constants.PROJECT, "/project/" + project);
        }

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
        String temp = getOptionValue("filepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            File file = new File(temp);
            AgileCentralValidator.checkFileExists(file);
            RallyUtil.processCustomFields(temp, updateObj);
        }
        
        // adding new work item description
        temp = getOptionValue("descriptionfilepath");
        if (CommonUtil.checkNotEmpty(temp)) {
            File file = new File(temp);
            AgileCentralValidator.checkFileExists(file);
            try {
                String description = new String(Files.readAllBytes(Paths.get(temp)));
                updateObj.addProperty("Description", description);
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException("Error occured while reading description from temp file" + e.getMessage());
            }

        }

       
        ConsoleWriter.writeln("Request Json Object: " + updateObj);
        return new UpdateRequest(workItemRef, updateObj);

    }

}
