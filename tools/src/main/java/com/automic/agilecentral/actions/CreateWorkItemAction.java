/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.IOException;
import java.util.Arrays;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

/**
 * @author sumitsamson
 *
 */
public class CreateWorkItemAction extends AbstractHttpAction {

	private String workspaceName;
	private String projectName;
	private String workItemName;
	private String description;
	private String scheduleState;
	private String customFields;

	public CreateWorkItemAction() {

		addOption("workspaceName", true, "Workspace in which project is located");
		addOption("projectName", true, "Project Name where work item needs to be created");
		addOption("workItemName", true, "Name of workitem ");
		addOption("description", false, "Desciption of work item");
		addOption("scheduleState", true, "Schedule state of work");
		addOption("customFields", false, "Custom fields");

	}

	@Override
	protected void executeSpecific() throws AutomicException {

		checkandPrepareInputs();

		JsonObject newObj = new JsonObject();
		newObj.addProperty("Workspace", "/workspace/" + workspaceName);
		newObj.addProperty("Project", "/project/" + projectName);
		newObj.addProperty("Name", workItemName);
		newObj.addProperty("Description", description);
		newObj.addProperty("ScheduleState", scheduleState);

		if (!customFields.isEmpty()) {
			addCustomfileds(newObj);
		}

		CreateRequest createRequest = new CreateRequest("hierarchicalrequirement", newObj);
		try {
			CreateResponse createResponse = rallyRestTarget.create(createRequest);

			if (createResponse.wasSuccessful()) {
				ConsoleWriter.writeln("UC4RB_AC_WORK_ITEM_ID ::=" + createResponse.getObject().get("FormattedID"));

			} else {
				throw new AutomicException(Arrays.toString(createResponse.getErrors()));
			}

		} catch (IOException e) {
			ConsoleWriter.writeln(e);
			new AutomicException(e.getMessage());
		}

	}

	/**
	 * @param newObj
	 * @throws AutomicException
	 */
	private void addCustomfileds(JsonObject newObj) throws AutomicException {
		try {
			String regex = "[)][,][(]";
			customFields = customFields.substring(1, customFields.lastIndexOf(")"));
			String[] listOfFields = customFields.split(regex);
			for (String fields : listOfFields) {
				String[] field = fields.split(",");
				newObj.addProperty(field[0], field[1]);
			}
		} catch (Exception e) {
			throw new AutomicException(
					"Error in the given custom fields ,please provide the valid input e.g (\"Name1\", \"Value1\"),(\"Name2\",\"Value2\")");
		}
	}

	private void checkandPrepareInputs() throws AutomicException {
		workspaceName = getOptionValue("workspaceName");
		AgileCentralValidator.checkNotEmpty(workspaceName, "Workspace");

		projectName = getOptionValue("projectName");
		AgileCentralValidator.checkNotEmpty(projectName, "Project Name");

		workItemName = getOptionValue("workItemName");
		AgileCentralValidator.checkNotEmpty(workItemName, "Work item Name");

		scheduleState = getOptionValue("scheduleState");
		AgileCentralValidator.checkNotEmpty(scheduleState, "Schedule state of work");

		description = getOptionValue("description");

		customFields = getOptionValue("customFields");

	}

}
