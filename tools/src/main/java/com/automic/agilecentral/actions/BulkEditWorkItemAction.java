/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;

/**
 * This class is used to perform update on more than one work item i.e user story,defects etc. It will throw an error
 * and quits as soon as an error occurred while updating the fields of a particular work item.This might result in
 * partial update of the work items.Following fields can be updated for more than one work items using this action
 * :Project Name,Description,Schedule State and Custom fields.
 * 
 */
public class BulkEditWorkItemAction extends AbstractHttpAction {

    private static final int REQUEST_SIZE = 15;

    public BulkEditWorkItemAction() {
        addOption("workitemids", true, "Work item ids");
        addOption("workitemtype", true, "Work item type you wanted to edit");
        addOption("workspacename", false, "Workspace in which stories are located");
        addOption("projectname", false, "New project name");
        addOption("descriptionfilepath", false, "Description file path");
        addOption("schedulestate", false, "New schedule state of work item");
        addOption("customfilepath", false, "Custom fields file path");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        JsonObject updateObj = prepareJsonObject();
        List<String> workItemRefArray = prepareWorkItemReferences();

        for (String ref : workItemRefArray) {
            try {
                UpdateRequest req = new UpdateRequest(ref, updateObj);
                UpdateResponse updateResponse = rallyRestTarget.update(req);
                if (!updateResponse.wasSuccessful()) {
                    ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
                    throw new AutomicException(Arrays.toString(updateResponse.getErrors()));
                }
                ConsoleWriter.writeln(String.format("Work Item %s updated",
                        updateResponse.getObject().get("FormattedID")));
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(String.format("Error occured while updating work item ids: %s",
                        e.getMessage()));
            }
        }
    }

    private JsonObject prepareJsonObject() throws AutomicException {
        // checking if required inputs are provided
        String project = getOptionValue("projectname");
        String scheduleState = getOptionValue("schedulestate");
        String customFilePath = getOptionValue("customfilepath");
        String descFilePath = getOptionValue("descriptionfilepath");

        if (CommonUtil.checkNotEmpty(project) && CommonUtil.checkNotEmpty(scheduleState)
                && CommonUtil.checkNotEmpty(customFilePath) && CommonUtil.checkNotEmpty(descFilePath)) {
            throw new AutomicException("Please provide atleast one of the following: Project Name,Description,"
                    + "Schedule State or Custom Fields");
        }
        JsonObject updateObj = new JsonObject();

        // checking if given project exists
        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectRef(rallyRestTarget, project, null);
            updateObj.addProperty(Constants.PROJECT, project);
        }

        // adding new work item scheduled state
        if (CommonUtil.checkNotEmpty(scheduleState)) {
            updateObj.addProperty("ScheduleState", scheduleState);
        }

        // Custom fields addition
        if (CommonUtil.checkNotEmpty(customFilePath)) {
            File file = new File(customFilePath);
            AgileCentralValidator.checkFileExists(file);
            RallyUtil.processCustomFields(customFilePath, updateObj);
        }

        // adding new work item description
        if (CommonUtil.checkNotEmpty(descFilePath)) {
            String description = CommonUtil.readFileIntoString(descFilePath);
            updateObj.addProperty("Description", description);
        }
        return updateObj;
    }

    private List<String> prepareWorkItemReferences() throws AutomicException {
        // checking if given work item exists
        String workItemIds = getOptionValue("workitemids");
        AgileCentralValidator.checkNotEmpty(workItemIds, "Work item ids");

        String[] workItemsArray = workItemIds.split(",");
        Set<String> uniqueWorkItemSet = new HashSet<>(workItemsArray.length);
        for (String s : workItemsArray) {
            String tmp = s.trim();
            if (!tmp.isEmpty()) {
                uniqueWorkItemSet.add(tmp);
            }
        }

        // check the size 0
        if (uniqueWorkItemSet.size() == 0) {
            throw new AutomicException(String.format("Please provide valid work items %s", workItemIds));
        }

        List<String> workItemList = new ArrayList<String>(uniqueWorkItemSet);
        return retrieveWorkItemRefs(workItemList);
    }

    private List<String> retrieveWorkItemRefs(List<String> workItemList) throws AutomicException {
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        String workSpace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workSpace)) {
            workSpace = RallyUtil.getWorspaceRef(rallyRestTarget, workSpace);
        }
        
        List<String> workItemRefArray = new ArrayList<>(workItemList.size());
        Map<String, List<String>> queryFilter = new HashMap<>();
        List<String> fetch = new ArrayList<>(Arrays.asList(new String[] { "_ref" }));

        int totalRequestSize = workItemList.size();
        int start = 0;
        int end;
        while (start < totalRequestSize) {
            end = start + REQUEST_SIZE;
            if (end > totalRequestSize) {
                end = totalRequestSize;
            }
            List<String> requestList = workItemList.subList(start, end);
            queryFilter.put("FormattedID", requestList);

            QueryResponse queryResponse = null;
            try {
                queryResponse = RallyUtil.queryOR(rallyRestTarget, workItemType, workSpace, queryFilter, fetch, null);
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(String.format("Error occured while fetching details of work items : %s",
                        requestList));
            }

            int totalResults = queryResponse.getTotalResultCount();
            if (totalResults != requestList.size()) {
                ConsoleWriter.writeln(queryResponse.getResults());
                ConsoleWriter.writeln("Expected Count: " + requestList.size() + " Actual Count: " + totalResults);
                ConsoleWriter.writeln("Some of the Work items does not exist " + requestList);
                throw new AutomicException("Invalid work items have been provided.");
            }

            for (JsonElement elem : queryResponse.getResults()) {
                workItemRefArray.add(elem.getAsJsonObject().get("_ref").getAsString());
            }
            start = end;
        }
        return workItemRefArray;
    }

}
