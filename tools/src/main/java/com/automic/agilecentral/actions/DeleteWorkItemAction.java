package com.automic.agilecentral.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.QueryResponse;

/**
 * This action deletes the given work item with the given ID.
 * 
 * @author shrutinambiar
 *
 */
public class DeleteWorkItemAction extends AbstractHttpAction {

    /**
     * Workspace name where the user story is located
     */
    private String workSpace;

    /**
     * Formatted Id of the user story to be deleted
     */
    private String workItemId;

    /**
     * Work item type
     */
    private String workItemType;

    public DeleteWorkItemAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work Item ID");
        addOption("workitemtype", true, "Work item type");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        prepareInputs();
        try {
            JsonArray userStoryResult = queryRes();

            String ref = "";
            for (JsonElement result : userStoryResult) {
                ref = result.getAsJsonObject().get("_ref").getAsString();
            }

            // deleting the work item
            DeleteRequest deleteRequest = new DeleteRequest(ref);
            DeleteResponse deleteResponse;
            deleteResponse = rallyRestTarget.delete(deleteRequest);
            if (!deleteResponse.wasSuccessful()) {
                throw new AutomicException(deleteResponse.getErrors()[0]);
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private void prepareInputs() throws AutomicException {
        workSpace = getOptionValue("workspace");

        workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "Work Item ID");

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");
    }

    private JsonArray queryRes() throws AutomicException, IOException {

        // filter criteria
        Map<String, String> queryFilter = new HashMap<>();
        queryFilter.put("FormattedID", workItemId);

        // query parameter
        Map<String, String> queryParam = null;
        if (CommonUtil.checkNotEmpty(workSpace)) {
            queryParam = new HashMap<>();
            queryParam.put("Workspace", workSpace);
        }

        // result to be fetched
        List<String> fetch = new ArrayList<>();
        fetch.add("Results");

        QueryResponse queryResponse = RallyUtil.query(rallyRestTarget, workItemType, queryFilter, fetch, queryParam);

        if (!queryResponse.wasSuccessful()) {
            throw new AutomicException(queryResponse.getErrors()[0]);
        } else if (queryResponse.getTotalResultCount() != 1) {
            throw new AutomicException("Multiple or no result found.");
        }

        return queryResponse.getResults();
    }

}
