package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

/**
 * This action used to add comment to provided work Item.
 * 
 * @author Anurag Upadhyay
 *
 */
public class AddCommentAction extends AbstractHttpAction {

    public AddCommentAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work Item ID");
        addOption("workitemtype", true, "Work item type");
        addOption("commentfilepath", true, "Comment to work item ");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        JsonObject newComment = prepareAndValidateInputs();
        ConsoleWriter.writeln("Request Json Object: " + newComment);
        // Preparing add comment request the work item
        CreateRequest createRequest = new CreateRequest(Constants.CONVERSATION_POST, newComment);
        try {
            CreateResponse createResponse = this.rallyRestTarget.create(createRequest);
            ConsoleWriter.writeln("Response Json Object: " + createResponse.getObject());
            if (!createResponse.wasSuccessful()) {
                throw new AutomicException(Arrays.toString(createResponse.getErrors()));
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private JsonObject prepareAndValidateInputs() throws AutomicException {
        // Validate Workspace name
        String workSpaceName = getOptionValue("workspace");
        AgileCentralValidator.checkNotEmpty(workSpaceName, "Workspace name");
        // Validate Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");
        // Validate Work item Id
        String workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "Work Item ID");
        // Validate Comment file path
        String temp = getOptionValue("commentfilepath");
        AgileCentralValidator.checkNotEmpty(workItemId, "Comment to work item ");
        File file = new File(temp);
        AgileCentralValidator.checkFileExists(file);

        // Reading Comment file
        String comment = null;
        try {
            comment = new String(Files.readAllBytes(Paths.get(temp)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while reading comment from temp file" + e.getMessage());
        }

        // Resolving Workspace & work Item reference
        String workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        String workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpaceRef, workItemType);

        // Preparing comment json Object
        JsonObject newComment = new JsonObject();
        newComment.addProperty("Type", Constants.CONVERSATION_POST);
        newComment.addProperty("Text", comment);
        newComment.addProperty("Artifact", workItemRef);

        return newComment;
    }

}
