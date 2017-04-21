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
 * @author sumitsamson This class is used to perform updation on more than one work item i.e user story,defects etc. It
 *         will throw an error and quit as soon as an error occurred while updating the fields of a particular work
 *         item.This might result in partial updation of the work items.Following fields can be updated for more than
 *         one work items using this action :Project Name,Description,Schedule State and Custom fields.
 * 
 */
public class BulkEditWorkItemAction extends AbstractHttpAction {

    private static final int REQUEST_SIZE = 15;
    private String workSpace;
    private String project;
    private String workItemType;
    private String workItemIds;
    private String scheduleState;

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

        JsonObject updateObj = new JsonObject();

        List<String> workItemRefArray = checkInputsAndPrepareRequest(updateObj);
        UpdateRequest req;
        for (String ref : workItemRefArray) {
            try {
                req = new UpdateRequest(ref, updateObj);

                UpdateResponse updateResponse = rallyRestTarget.update(req);
                if (!updateResponse.wasSuccessful()) {
                    ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
                    throw new AutomicException(Arrays.toString(updateResponse.getErrors()));
                }
                ConsoleWriter
                        .writeln(String.format("Work Item %s updated", updateResponse.getObject().get("FormattedID")));
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(
                        String.format("Error occured while updating work item ids: %s", e.getMessage()));
            }

        }

    }

    private List<String> checkInputsAndPrepareRequest(JsonObject updateObj) throws AutomicException {

        // checking if required inputs are provided
        project = getOptionValue("projectname");
        scheduleState = getOptionValue("schedulestate");
        String customFilePath = getOptionValue("customfilepath");
        String descFilePath = getOptionValue("descriptionfilepath");

        if (CommonUtil.checkNotEmpty(project) && CommonUtil.checkNotEmpty(scheduleState)
                && CommonUtil.checkNotEmpty(customFilePath) && CommonUtil.checkNotEmpty(descFilePath)) {
            throw new AutomicException("Please provide atleast one of the following: Project Name,Description,"
                    + "Schedule State or Custom Fields");
        }

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        // checking if given work item exists
        workItemIds = getOptionValue("workitemids");
        AgileCentralValidator.checkNotEmpty(workItemIds, "Work item ids");

        workSpace = getOptionValue("workspacename");
        if (CommonUtil.checkNotEmpty(workSpace)) {
            workSpace = RallyUtil.getWorspaceRef(rallyRestTarget, workSpace);
            updateObj.addProperty(Constants.WORKSPACE, workSpace);
        }

        // getting work item references

        List<String> workItemRefArray = getWorkItemRefList();

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

        return workItemRefArray;

    }

    /**
     * @return
     * @throws AutomicException
     */

    private List<String> getWorkItemRefList() throws AutomicException {
        List<String> fetch = new ArrayList<>(Arrays.asList(new String[] { "_ref" }));

        String[] workItemsArray = workItemIds.split(",");
        Set<String> uniqueWorkItemSet = new HashSet<>(workItemsArray.length);
        for (String s : workItemsArray) {
            String tmp = s.trim();
            if (!tmp.isEmpty()) {
                uniqueWorkItemSet.add(tmp);
            }
        }

        // check the size 0
        if(uniqueWorkItemSet.size()<1){
            throw new AutomicException(String.format("Please provide valid work items %s",workItemIds));
        }

        Map<String, List<String>> queryFilter = new HashMap<>();
        List<String> workItemRefArray = new ArrayList<>(uniqueWorkItemSet.size());

        // break the list into the sets of 15

        int start = 0;
        int end;
        int totalRequestSize = uniqueWorkItemSet.size();

        QueryResponse queryResponse = null;
        List<String> workItemList = new ArrayList<String>(uniqueWorkItemSet);

        while (start < totalRequestSize) {

            end = start + REQUEST_SIZE;
            if (end > totalRequestSize) {
                end = totalRequestSize;
            }
            // start and end should give the data.

            List<String> requestList = workItemList.subList(start, end);
            queryFilter.put("FormattedID", requestList);

            try {
                queryResponse = RallyUtil.queryOR(rallyRestTarget, workItemType, workSpace, queryFilter, fetch, null);
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(
                        String.format("Error occured while fetching details of work items : %s", e.getMessage()));
            }

            int totalResults = queryResponse.getTotalResultCount();
            if (totalResults == 0 || totalResults != requestList.size()) {
                ConsoleWriter.writeln(queryResponse.getResults());
                throw new AutomicException(
                        "Error occured while fetching details of work items ,some work items might not exists");
            }

            for (JsonElement elem : queryResponse.getResults()) {
                workItemRefArray.add(elem.getAsJsonObject().get("_ref").getAsString());

            }

            start = end;
        }

        return workItemRefArray;
    }

}
