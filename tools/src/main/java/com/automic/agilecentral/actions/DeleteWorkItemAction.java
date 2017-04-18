package com.automic.agilecentral.actions;

import java.io.IOException;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.response.DeleteResponse;

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
            // deleting the work item
            DeleteRequest deleteRequest = new DeleteRequest(workItemId);
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
        if (CommonUtil.checkNotEmpty(workSpace)) {
            RallyUtil.getWorspaceId(rallyRestTarget, workSpace);
        }

        workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        String temp = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(temp, "Work Item ID");
        workItemId = RallyUtil.getWorkItemRef(rallyRestTarget, temp, null, workSpace, workItemType);

    }

}
