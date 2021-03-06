package com.automic.agilecentral.actions;

import java.io.IOException;
import java.util.Arrays;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.UpdateResponse;

/**
 * This class changes the status of the given work item.
 * 
 * @author shrutinambiar
 *
 */
public class ChangeWorkItemStatusAction extends AbstractHttpAction {

    private static final String BLOCKED = "BLOCKED";
    private static final String READY = "READY";
    private static final String NONE = "NONE";

    /**
     * Json containing the status to be set.
     */
    private JsonObject updatedWorkItem;

    /**
     * reference of the work item
     */
    private String workItemRef;

    public ChangeWorkItemStatusAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work item ID");
        addOption("workitemstatus", true, "Work item status");
        addOption("workitemtype", true, "Work item type");
        addOption("blockedreason", false, "Blocked reason");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        prepareInputs();
        try {
            // Updating the status for the given work item
            UpdateRequest updateRequest = new UpdateRequest(workItemRef, updatedWorkItem);
            UpdateResponse updateResponse = rallyRestTarget.update(updateRequest);
            if (!updateResponse.wasSuccessful()) {
                ConsoleWriter.writeln(Arrays.toString(updateResponse.getErrors()));
                throw new AutomicException("Unable to change the status of work item.");
            }

            ConsoleWriter.writeln("Response Json Object: " + updateResponse.getObject());
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }
    }

    private void prepareInputs() throws AutomicException {

        // Work item status
        String workItemStatus = getOptionValue("workitemstatus");
        AgileCentralValidator.checkNotEmpty(workItemStatus, "Work item status");

        // Json to update the status
        updatedWorkItem = new JsonObject();

        switch (workItemStatus.toUpperCase()) {
            case NONE:
                updatedWorkItem.addProperty(READY, false);
                updatedWorkItem.addProperty(BLOCKED, false);
                break;

            case READY:
                updatedWorkItem.addProperty(READY, true);
                updatedWorkItem.addProperty(BLOCKED, false);
                break;

            case BLOCKED:
                updatedWorkItem.addProperty(BLOCKED, true);
                updatedWorkItem.addProperty(READY, false);
                String blockReason = getOptionValue("blockedreason");
                if (CommonUtil.checkNotEmpty(blockReason)) {
                    updatedWorkItem.addProperty("BlockedReason", blockReason);
                }
                break;
            default:
                throw new AutomicException(String.format("Invalid status[%s]. Specify None, Ready or Blocked.",
                        workItemStatus));
        }

        // Work item type : hierarchicalrequirement/Defect/Task
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        // Work item ID
        String workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "Work item ID");

        // Workspace name where the user story is located
        String workSpaceRef = null;
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        }

        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpaceRef, workItemType);
    }

}
