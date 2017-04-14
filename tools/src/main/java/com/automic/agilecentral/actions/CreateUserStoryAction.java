/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

/**
 * @author sumitsamson
 *
 */
public class CreateUserStoryAction extends AbstractHttpAction {

	private String workSpace;
	private String project;
	private String workItemName;
	private String description;
	private String scheduleState;
	private String customFields;

	public CreateUserStoryAction() {

		addOption("workspacename", false, "Workspace in which project is located");
		addOption("projectname", false, "Project Name where work item needs to be created");
		addOption("usrstoryname", true, "Name of user story");
		addOption("description", false, "Desciption of work item");
		addOption("schedulestate", false, "Schedule state of work");
		addOption("customfields", false, "Custom fields");

	}

	@Override
	protected void executeSpecific() throws AutomicException {

		JsonObject newObj = new JsonObject();
		checkandPrepareInputs(newObj);

		if (CommonUtil.checkNotEmpty(customFields)) {
			addCustomfileds(newObj);
		}

		CreateRequest createRequest = new CreateRequest("hierarchicalrequirement", newObj);
		try {
			CreateResponse createResponse = rallyRestTarget.create(createRequest);

			if (createResponse.wasSuccessful()) {
				ConsoleWriter.writeln(
						"UC4RB_AC_STORY_FMT_ID ::=" + createResponse.getObject().get("FormattedID").getAsString());
				ConsoleWriter.writeln("UC4RB_AC_STORY_OBJ_ID ::=" + createResponse.getObject().get("ObjectID"));

			} else {
				throw new AutomicException(Arrays.toString(createResponse.getErrors()));
			}

		} catch (IOException e) {
			ConsoleWriter.writeln(e);
			throw new AutomicException(e.getMessage());
		}

	}

	/**
	 * @param newObj
	 * @throws AutomicException
	 */
	private void addCustomfileds(JsonObject newObj) throws AutomicException {
		try {
			String regex = System.getProperty("line.separator");
			String[] listOfFields = customFields.split(regex);
			for (String fields : listOfFields) {
				String[] field = fields.split("=");
				newObj.addProperty(field[0], field[1]);
			}
		} catch (Exception e) {
			throw new AutomicException(
					"Error in the given custom fields ,please provide the valid input e.g Key1=Val1 \\n Key2=Val2");
		}
	}

	private void checkandPrepareInputs(JsonObject newObj) throws AutomicException {
		workItemName = getOptionValue("usrstoryname");
		AgileCentralValidator.checkNotEmpty(workItemName, "Name of user story");
		newObj.addProperty("Name", workItemName);

		workSpace = getOptionValue("workspacename");

		project = getOptionValue("projectname");

		scheduleState = getOptionValue("schedulestate");

		description = getOptionValue("description");

		customFields = getOptionValue("customfields");

		newObj.addProperty("Description", description);
		newObj.addProperty("ScheduleState", scheduleState);

		Map<String, String> queryFilter;
		List<String> fetch;
		try {

			if (null != workSpace && !workSpace.isEmpty()) {
				// querying and getting workspace id
				queryFilter = new HashMap<>();
				fetch = new ArrayList<>();

				queryFilter.put("Name", workSpace);
				fetch.add("ObjectID");

				workSpace = RallyUtil.getObjectId(rallyRestTarget, Constants.WORKSPACE, queryFilter, fetch);
				newObj.addProperty(Constants.WORKSPACE, "/workspace/" + workSpace);
			}

			if (null != project && !project.isEmpty()) {
				// querying and getting project id

				queryFilter = new HashMap<>();
				fetch = new ArrayList<>();

				queryFilter.put("Name", project);
				project = RallyUtil.getObjectId(rallyRestTarget, Constants.PROJECT, queryFilter, fetch);
			}

		} catch (IOException e) {
			ConsoleWriter.writeln(e);
			throw new AutomicException(e.getMessage());
		}

	}

}