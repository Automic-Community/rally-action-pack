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
     * Formatted Id of the user story to be deleted
     */
    private String workItemRef;

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
            DeleteRequest deleteRequest = new DeleteRequest(workItemRef);
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
        // Workspace name where the user story is located
        String workSpaceRef = "";
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        }

        // Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        String temp = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(temp, "Work Item ID");
        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, temp, workSpaceRef, workItemType);

    }

}
