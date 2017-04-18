/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.QueryResponse;
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
    private String description;
    private String scheduleState;

    public EditWorkItemAction() {
        addOption("workitemid", true, "Work item id");
        addOption("workitemtype", true, "Work item type you wanted to edit");
        addOption("workitemname", true, "New workt item name");
        addOption("workspacename", false, "Workspace in which project is located");
        addOption("projectname", false, "New project name");
        addOption("description", false, "New desciption of work item");
        addOption("schedulestate", false, "New schedule state of work item");
        addOption("filepath", false, "Custom fields file path");

    }

    @Override
    protected void executeSpecific() throws AutomicException {

        UpdateRequest updateRequest = checkInputsAndPrepareRequest();
        try {
            UpdateResponse updateResponse = rallyRestTarget.update(updateRequest);
            ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
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

        Map<String, String> queryFilter = null;
        Map<String, String> queryParam = null;
        List<String> fetch = null;
        String workItemRef = null;
        try {

            // checking if given workspace name exists
            workSpace = getOptionValue("workspacename");
            if (CommonUtil.checkNotEmpty(workSpace)) {
                // setting query filter to get object id
                queryFilter = new HashMap<>();
                queryFilter.put("Name", workSpace);

                // setting query param to be used while querying user story
                queryParam = new HashMap<>();
                queryParam.put("Workspace", workSpace);

                workSpace = RallyUtil.getObjectId(rallyRestTarget, Constants.WORKSPACE, queryFilter, fetch, null);

            }

            // checking if given work item exists
            queryFilter = new HashMap<>();
            queryFilter.put("FormattedID", workItemId);

            QueryResponse queryResponse = RallyUtil.query(rallyRestTarget, workItemType, queryFilter, fetch,
                    queryParam);
            if (queryResponse.getTotalResultCount() > 0) {
                JsonObject workspaceJsonObject = queryResponse.getResults().get(0).getAsJsonObject();
                workItemRef = workspaceJsonObject.get("_ref").getAsString();

            } else {
                throw new AutomicException(String.format("Work item %s might not exists", workItemId));
            }

            // checking if given project exists
            project = getOptionValue("projectname");
            if (CommonUtil.checkNotEmpty(project)) {
                // querying and getting project id

                queryFilter = new HashMap<>();
                queryFilter.put("Name", project);
                queryParam = new HashMap<>();
                queryParam.put("Workspace", workSpace);

                project = RallyUtil.getObjectId(rallyRestTarget, Constants.PROJECT, queryFilter, fetch, queryParam);
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

            // adding new work item description
            description = getOptionValue("description");
            if (CommonUtil.checkNotEmpty(description)) {
                updateObj.addProperty("Description", description);
            }

            // Custom fields addition
            String temp = getOptionValue("filepath");
            if (CommonUtil.checkNotEmpty(temp)) {
                File file = new File(temp);
                AgileCentralValidator.checkFileExists(file);
                RallyUtil.processCustomFields(temp, updateObj);
            }
            ConsoleWriter.writeln("Request Json Object: " + updateObj);
            return new UpdateRequest(workItemRef, updateObj);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

}
