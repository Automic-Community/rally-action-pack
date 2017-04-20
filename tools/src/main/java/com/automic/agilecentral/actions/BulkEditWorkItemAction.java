/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
 * @author sumitsamson This class is used to perform updation on more than one work item i.e user story,defects etc. It
 *         will throw an error and quit as soon as an error occurred while updating the fields of a particular work
 *         item.This might result in partial updation of the work items.Following fields can be updated for more than
 *         one work items using this action :Project Name,Description,Schedule State and Custom fields.
 * 
 */
public class BulkEditWorkItemAction extends AbstractHttpAction {

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

        List<UpdateRequest> updateRequest = checkInputsAndPrepareRequest();

        for (UpdateRequest req : updateRequest) {
            try {
                UpdateResponse updateResponse = rallyRestTarget.update(req);

                if (!updateResponse.wasSuccessful()) {
                    ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
                    throw new AutomicException(Arrays.toString(updateResponse.getErrors()));
                }
                ConsoleWriter
                        .writeln(String.format("Work Item %s updated", updateResponse.getObject().get("FormattedID")));
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(String.format("Error occured while updating work item id: %s", workItemIds));
            }
        }

    }

    private List<UpdateRequest> checkInputsAndPrepareRequest() throws AutomicException {

        // checking if required inputs are provided
        project = getOptionValue("projectname");
        scheduleState = getOptionValue("schedulestate");
        String customFilePath = getOptionValue("customfilepath");
        String descFilePath = getOptionValue("descriptionfilepath");

        if ((project == null || project.isEmpty()) && (scheduleState == null || scheduleState.isEmpty())
                && (customFilePath == null || customFilePath.isEmpty())
                || (descFilePath == null || descFilePath.isEmpty())) {
            throw new AutomicException("Please provide atleast one of the following: Project Name,Description,"
                    + "Schedule State or Custom Fields");
        }

        JsonObject updateObj = new JsonObject();

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

        // checking if given work item exists
        workItemIds = getOptionValue("workitemids");
        if (!Pattern.matches("[a-zA-z0-9,]+[^,]", workItemIds)) {
            throw new AutomicException(String.format("User Story input is not proper %s", workItemIds));
        }

        if (CommonUtil.checkNotEmpty(workSpace)) {
            workSpace = RallyUtil.getWorspaceRef(rallyRestTarget, workSpace);
            updateObj.addProperty(Constants.WORKSPACE, workSpace);
        }

        // getting work item references

        List<String> workItemRefArray = getWorkItemRefList();

        // checking if given project exists

        if (CommonUtil.checkNotEmpty(project)) {
            project = RallyUtil.getProjectRef(rallyRestTarget, project, workSpace);
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
        List<UpdateRequest> bulkUpdateRequest = new ArrayList<>();
        for (String ref : workItemRefArray) {
            bulkUpdateRequest.add(new UpdateRequest(ref, updateObj));
        }
        return bulkUpdateRequest;

    }

    /**
     * @return
     * @throws AutomicException
     */
   /* private List<String> getWorkItemRefList() throws AutomicException {
        List<String> fetch = new ArrayList<>(Arrays.asList(new String[] { "_ref" }));

        List<String> workItemIdList = new ArrayList<>(Arrays.asList(workItemIds.split(",")));
        Map<String, List<String>> queryFilter = new HashMap<>();
        queryFilter.put("FormattedID", workItemIdList);
        QueryResponse queryResponse = null;
        try {
            queryResponse = RallyUtil.queryOR(rallyRestTarget, workItemType, workSpace, queryFilter, fetch, null);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(String
                    .format("Error occured while fetching reference type for work items of type %s", workItemType));
        }

        int totalResults = queryResponse.getTotalResultCount();
        if (totalResults == 0 || totalResults != workItemIdList.size()) {
            ConsoleWriter.writeln(queryResponse.getResults());
            throw new AutomicException(String.format("Error while retrieving work items expected %s, recieved %s",
                    workItemIdList.size(), totalResults));
        }

        List<String> workItemRefArray = new ArrayList<>();
        for (JsonElement elem : queryResponse.getResults()) {
            workItemRefArray.add(elem.getAsJsonObject().get("_ref").getAsString());

        }

        return workItemRefArray;
    }*/
    
    private List<String> getWorkItemRefList() throws AutomicException {
        List<String> fetch = new ArrayList<>(Arrays.asList(new String[] { "_ref" }));
        List<String> workItemIdList = new ArrayList<>(Arrays.asList(workItemIds.split(",")));
        List<String> workItemRefArray = new ArrayList<>();
        Map<String, List<String>> queryFilter = new HashMap<>();
        QueryResponse queryResponse = null;
        
        StringBuffer str= new StringBuffer();
        for(String id:workItemIdList){
            queryFilter.put("FormattedID", Arrays.asList(new String[]{id}));
           
            try {
                queryResponse = RallyUtil.queryOR(rallyRestTarget, workItemType, workSpace, queryFilter, fetch, null);
                int totalResults = queryResponse.getTotalResultCount();
                if(totalResults == 0){
                    throw new AutomicException(String.format("Work item %s not found",id));
                   
                }else{
                    str.append(id);
                    str.append(",");
                    workItemRefArray.add(queryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString()); 
                }
                
            } catch (IOException e) {
                ConsoleWriter.writeln(e);
                throw new AutomicException(String
                        .format("Error occured while fetching reference type for work items of type %s", workItemType));
            }
            
        }
        str.deleteCharAt(str.length()-1);
        ConsoleWriter.writeln(str);
        return workItemRefArray;
        
    }

}
