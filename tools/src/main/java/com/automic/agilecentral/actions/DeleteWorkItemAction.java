package com.automic.agilecentral.actions;

import java.io.IOException;
import java.util.Arrays;

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

    public DeleteWorkItemAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work Item ID");
        addOption("workitemtype", true, "Work item type");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        String workItemRef = getWorkItemRef();
        try {
            // deleting the work item
            DeleteRequest deleteRequest = new DeleteRequest(workItemRef);
            DeleteResponse deleteResponse;
            deleteResponse = rallyRestTarget.delete(deleteRequest);
            if (!deleteResponse.wasSuccessful()) {
                throw new AutomicException(Arrays.toString(deleteResponse.getErrors()));
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private String getWorkItemRef() throws AutomicException {
        // Workspace name where the user story is located
        String workSpaceRef = null;
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        }

        // Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        String temp = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(temp, "Work Item ID");
        return RallyUtil.getWorkItemRef(rallyRestTarget, temp, workSpaceRef, workItemType);
    }

}
