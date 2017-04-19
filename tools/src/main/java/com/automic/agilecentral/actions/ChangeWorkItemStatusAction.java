package com.automic.agilecentral.actions;

import java.io.IOException;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.UpdateResponse;

/**
 * This class changes the schedule state of the given work item.
 * 
 * @author shrutinambiar
 *
 */
public class ChangeWorkItemStatusAction extends AbstractHttpAction {

    /**
     * Work item type : hierarchicalrequirement/Defect/Task
     */
    private String workItemType;

    /**
     * Schedule status of the work item
     */
    private String workItemStatus;

    /**
     * Formatted Id of the work item to be deleted
     */
    private String workItemRef;

    public ChangeWorkItemStatusAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work item ID");
        addOption("workitemstatus", true, "Work item status");
        addOption("workitemtype", true, "Work item type");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        prepareInputs();
        try {
            // Json to update the status
            JsonObject updatedWorkItem = new JsonObject();
            String[] statusList = workItemStatus.split(",");
            for (String status : statusList) {
                String[] stat = status.split("=");
                if (stat.length != 2) {
                    throw new AutomicException(String.format(
                            "Invalid value[%s] for status. Provide the value in a key-value comma separated manner.",
                            workItemStatus));
                }
                updatedWorkItem.addProperty(stat[0], stat[1]);
            }

            // Updating the status for the given work item
            UpdateRequest updateRequest = new UpdateRequest(workItemRef, updatedWorkItem);
            UpdateResponse updateResponse = rallyRestTarget.update(updateRequest);
            if (!updateResponse.wasSuccessful()) {
                throw new AutomicException(updateResponse.getErrors()[0]);
            }
            ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private void prepareInputs() throws AutomicException {

        // Work item status
        workItemStatus = getOptionValue("workitemstatus");
        AgileCentralValidator.checkNotEmpty(workItemStatus, "Work item status");

        // Work item type
        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        // Work item ID
        String workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "Work item ID");

        // Workspace name where the user story is located
        String workSpaceRef = "";
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        }

        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpaceRef, workItemType);
    }

}
