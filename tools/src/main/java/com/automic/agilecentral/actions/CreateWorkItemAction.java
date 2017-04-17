/**
 * 
 */
package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class CreateWorkItemAction extends AbstractHttpAction {

	private String workSpace;
	private String project;
	private String workItemName;
	private String workItemType;
	private String description;
	private String scheduleState;
	private List<String> customFields = Collections.emptyList();

	public CreateWorkItemAction() {

		addOption("workitemname", true, "Name of the work item");
		addOption("workitemtype", true, "Work item type you wanted to create");
		addOption("workspacename", false, "Workspace in which project is located");
		addOption("projectname", false, "Project Name where work item needs to be created");
		addOption("description", false, "Desciption of work item");
		addOption("schedulestate", false, "Schedule state of work");
		addOption("filepath", false, "Custom fields file path");

	}

	@Override
	protected void executeSpecific() throws AutomicException {

		JsonObject newObj = new JsonObject();
		checkandPrepareInputs(newObj);

		if (null != customFields && !customFields.isEmpty()) {
			addCustomfileds(newObj);
		}
		ConsoleWriter.writeln("Request Json Object: " + newObj);
		CreateRequest createRequest = new CreateRequest(workItemType, newObj);
		try {
			CreateResponse createResponse = rallyRestTarget.create(createRequest);
			ConsoleWriter.writeln("Response Json Object: " + createResponse.getObject());
			if (createResponse.wasSuccessful()) {
				ConsoleWriter.writeln(
						"UC4RB_AC_WORK_ITEM_ID ::=" + createResponse.getObject().get("FormattedID").getAsString());
				ConsoleWriter.writeln("UC4RB_AC_WORK_ITEM_OBJ_ID ::=" + createResponse.getObject().get("ObjectID"));

			} else {
				throw new AutomicException(Arrays.toString(createResponse.getErrors()));
			}

		} catch (IOException e) {
			ConsoleWriter.writeln(e);
			throw new AutomicException(e.getMessage());
		}

	}

	private void checkandPrepareInputs(JsonObject newObj) throws AutomicException {
		workItemName = getOptionValue("workitemname");
		AgileCentralValidator.checkNotEmpty(workItemName, "Name of user story");
		newObj.addProperty("Name", workItemName);

		workItemType = getOptionValue("workitemtype");
		AgileCentralValidator.checkNotEmpty(workItemType, "Type of work item e.g HIERARCHICALREQUIREMENT ,DEFECT etc");

		scheduleState = getOptionValue("schedulestate");
		if (CommonUtil.checkNotEmpty(scheduleState)) {
			newObj.addProperty("ScheduleState", scheduleState);
		}

		description = getOptionValue("description");
		if (CommonUtil.checkNotEmpty(description)) {
			newObj.addProperty("Description", description);
		}

		String temp = getOptionValue("filepath");
		if (CommonUtil.checkNotEmpty(temp)) {
			File file = new File(temp);
			AgileCentralValidator.checkFileExists(file);
			customFields = getCustomFields(temp);
		}

		Map<String, String> queryFilter;
		List<String> fetch;
		try {
			workSpace = getOptionValue("workspacename");
			if (CommonUtil.checkNotEmpty(workSpace)) {
				// querying and getting workspace id
				queryFilter = new HashMap<>();
				fetch = new ArrayList<>();

				queryFilter.put("Name", workSpace);
				fetch.add("ObjectID");

				workSpace = RallyUtil.getObjectId(rallyRestTarget, Constants.WORKSPACE, queryFilter, fetch);
				newObj.addProperty(Constants.WORKSPACE, "/workspace/" + workSpace);
			}

			project = getOptionValue("projectname");
			if (CommonUtil.checkNotEmpty(project)) {
				// querying and getting project id

				queryFilter = new HashMap<>();
				fetch = new ArrayList<>();

				queryFilter.put("Name", project);
				project = RallyUtil.getObjectId(rallyRestTarget, Constants.PROJECT, queryFilter, fetch);
				newObj.addProperty(Constants.PROJECT, "/project/" + project);
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

			for (String fields : customFields) {
				String[] field = fields.split("=");
				if (field.length == 2) {
					Arrays.stream(field).map(String::trim).toArray(unused -> field);// Trim
					newObj.addProperty(field[0], field[1]);
				} else {
					String errorMsg = String.format(
							"Error in the given custom field [%s] ,please provide the valid input e.g Key1=Val1 ",
							fields.toString());
					throw new AutomicException(errorMsg);
				}
			}
		} catch (AutomicException e) {
			throw e;
		}

		catch (Exception e) {
			ConsoleWriter.writeln(e);
			throw new AutomicException(
					"Error in the given custom fields ,please provide the valid input e.g Key1=Val1 \\n Key2=Val2 :: "
							+ e.getMessage());
		}
	}

	private List<String> getCustomFields(String fileName) throws AutomicException {
		List<String> list = new ArrayList<>();

		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
			list = stream.filter(s -> !s.isEmpty())// trim them and filter out
													// all empty lines
					.map(line -> line.trim()).collect(Collectors.toList());
		} catch (IOException e) {
			ConsoleWriter.writeln(e);
			throw new AutomicException("Error occured while processing custom fields :: " + e.getMessage());
		}

		return list;

	}

}