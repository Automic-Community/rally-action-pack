package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

/**
 * Attache a file to the given work item
 * 
 * @author shrutinambiar
 *
 */
public class AddAttachmentAction extends AbstractHttpAction {

    /**
     * ObjectId of the work item
     */
    private String workItemRef;

    /**
     * File to be attached
     */
    private File filePath;

    /**
     * Description of the attachment
     */
    private String description;

    public AddAttachmentAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemid", true, "Work item ID");
        addOption("filepath", true, "File path");
        addOption("workitemtype", true, "Work item type");
        addOption("description", false, "Description");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        prepareInputs();
        try {

            // Read file
            byte[] fileBytes = Files.readAllBytes(filePath.toPath());
            String imageBase64 = Base64.encodeBase64String(fileBytes);

            // create AttachmentContent from imageBase64String
            JsonObject attachmentContent = new JsonObject();
            attachmentContent.addProperty("Content", imageBase64);

            CreateRequest attachmentContentCreateRequest = new CreateRequest("AttachmentContent", attachmentContent);
            CreateResponse attachmentContentResponse = rallyRestTarget.create(attachmentContentCreateRequest);
            if (!attachmentContentResponse.wasSuccessful()) {
                throw new AutomicException(Arrays.toString(attachmentContentResponse.getErrors()));
            }

            // Attach the file uploaded to the story
            JsonObject myAttachment = new JsonObject();
            myAttachment.addProperty("Artifact", workItemRef);
            myAttachment.addProperty("Content", attachmentContentResponse.getObject().getAsJsonObject().get("_ref")
                    .getAsString());

            String contentType = Files.probeContentType(filePath.toPath());
            if (contentType == null) {
                contentType = "";
            }
            ConsoleWriter.writeln(String.format("Content Type of the file[%s] : [%s]", filePath, contentType));
            myAttachment.addProperty("ContentType", contentType);
            myAttachment.addProperty("Name", filePath.getName());

            if (CommonUtil.checkNotEmpty(description)) {
                myAttachment.addProperty("Description", description);
            }

            CreateRequest attachmentCreateRequest = new CreateRequest("Attachment", myAttachment);
            CreateResponse attachmentResponse = rallyRestTarget.create(attachmentCreateRequest);
            if (!attachmentResponse.wasSuccessful()) {
                throw new AutomicException(Arrays.toString(attachmentResponse.getErrors()));
            }
            ConsoleWriter.writeln("Response Json Object: " + attachmentResponse.getObject());
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private void prepareInputs() throws AutomicException {

        // attachment
        String tempFile = getOptionValue("filepath");
        filePath = new File(tempFile);
        AgileCentralValidator.checkFileExists(filePath);
        if (filePath.length() == 0) {
            throw new AutomicException(String.format("The file[%s] is empty.", filePath));
        }

        // Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        // Work item ID
        String workItemId = getOptionValue("workitemid");
        AgileCentralValidator.checkNotEmpty(workItemId, "Work item ID");

        // description
        description = getOptionValue("description");

        // Workspace name where the user story is located
        String workSpaceRef = null;
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
        }

        workItemRef = RallyUtil.getWorkItemRef(rallyRestTarget, workItemId, workSpaceRef, workItemType);
    }

}
