package com.automic.agilecentral.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CSVUtils;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.util.RallyUtil;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;

/**
 * This used to import data from Agile Central and saved in csv format
 * 
 * @author Anurag Upadhyay
 *
 */
public class ExportWorkItemsAction extends AbstractHttpAction {
    private static final String REF_OBJ_NAME = "_refObjectName";

    private String[] fields;
    private String filePath;
    private int lineCount;

    public ExportWorkItemsAction() {
        addOption("workspace", false, "Workspace name");
        addOption("workitemtype", true, "Work item type");
        addOption("filters", false, "Result filter");
        addOption("fields", false, "Fields to export");
        addOption("exportfilepath", true, "Export file path");
        addOption("limit", true, "Maximum result to fetch");
    }

    @Override
    protected void executeSpecific() throws AutomicException {
        QueryRequest queryRequest = prepareAndValidateInputs();

        // Preparing add comment request the work item

        try {
            QueryResponse queryResponse = this.rallyRestTarget.query(queryRequest);

            if (queryResponse.wasSuccessful()) {
                JsonArray results = queryResponse.getResults();

                if (results != null && results.size() > 0) {
                    if (fields == null) {
                        fields = getFields(queryResponse.getResults());
                    } else {
                        // Validate fields exist in json data
                        checkFieldExist(results, fields);
                    }
                    csvWriter(queryResponse.getResults(), fields, filePath);
                }
                filePath = lineCount > 0 ? filePath : "";
                ConsoleWriter.writeln("UC4RB_AC_EXPORT_FILE_PATH ::=" + filePath);
                ConsoleWriter.writeln("UC4RB_AC_TOTAL_RESULT_COUNT ::=" + lineCount);
                ConsoleWriter.writeln("Total available record count : " + queryResponse.getTotalResultCount());
            } else {
                ConsoleWriter.writeln(Arrays.toString(queryResponse.getErrors()));
                throw new AutomicException("Unable to export the work item.");
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private QueryRequest prepareAndValidateInputs() throws AutomicException {

        String workSpaceName = getOptionValue("workspace");
        // Validate Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        String filters = getOptionValue("filters");
        String field = getOptionValue("fields");

        // Validate export file path
        String temp = getOptionValue("exportfilepath");
        AgileCentralValidator.checkNotEmpty(temp, "Export file path");
        File file = new File(temp);
        AgileCentralValidator.checkFileWritable(file);
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException e) {
            throw new AutomicException(" Error in getting unique absolute path " + e.getMessage());
        }
        // Validate count
        String limit = getOptionValue("limit");
        AgileCentralValidator.checkNotEmpty(limit, "Maximum Items");

        QueryRequest queryRequest = new QueryRequest(workItemType);

        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            String workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
            queryRequest.setWorkspace(workSpaceRef);
        }

        if (CommonUtil.checkNotEmpty(field)) {
            fields = field.split(",");
            fields = validateAndPreapreFields(field.split(","));
            queryRequest.setFetch(new Fetch(fields));
        }

        queryRequest.addParam("query", filters);
        int recordLimit = CommonUtil.parseStringValue(limit, 0);
        if (recordLimit != 0) {
            queryRequest.setPageSize(recordLimit);
            queryRequest.setLimit(recordLimit);
        }

        return queryRequest;
    }

    private void csvWriter(JsonArray results, String[] fields, String filePath) throws AutomicException {
        if (fields != null && fields.length > 0) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                List<String> fieldsData = new ArrayList<String>(fields.length);
                // Add Headers
                CSVUtils.writeLine(bw, Arrays.asList(fields));
                for (JsonElement result : results) {
                    JsonObject workItem = result.getAsJsonObject();
                    for (String field : fields) {
                        if (workItem.get(field) != null && workItem.get(field).isJsonPrimitive()) {
                            fieldsData.add(workItem.get(field).getAsString());
                        } else if (workItem.get(field) != null && workItem.get(field).isJsonObject()) {
                            JsonObject jObj = (JsonObject) workItem.get(field);
                            if (jObj.has(REF_OBJ_NAME)) {
                                fieldsData.add(jObj.get(REF_OBJ_NAME).getAsString());
                            } else {
                                fieldsData.add("");
                            }
                        } else {
                            fieldsData.add("");
                        }
                    }
                    CSVUtils.writeLine(bw, fieldsData);
                    fieldsData.clear();
                    lineCount++;
                }

            } catch (IOException e) {
                throw new AutomicException(" Error in writing csv file" + e.getMessage());
            }
        }

    }

    private String[] getFields(JsonArray results) throws IOException {
        // Prepare exclude headers
        String[] excludeHeaderArr = new String[] { "_rallyAPIMajor", "_rallyAPIMinor", "_ref", "_refObjectUUID",
                "_objectVersion", "ObjectUUID", "VersionId", "HasParent", "_type" };
        Set<String> excludeHeaders = new HashSet<String>(Arrays.asList(excludeHeaderArr));
        JsonElement jsonEle = results.get(0);
        JsonObject jsonObj = jsonEle.getAsJsonObject();
        Set<Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        List<String> fields = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            if (!excludeHeaders.contains(key)) {
                if (jsonObj.get(key).isJsonPrimitive()) {
                    fields.add(key);
                } else if (jsonObj.get(key).isJsonObject()) {
                    JsonObject jObj = (JsonObject) jsonObj.get(key);
                    if (jObj.has(REF_OBJ_NAME)) {
                        fields.add(key);
                    }
                }
            }
        }
        return fields.toArray(new String[0]);
    }

    private void checkFieldExist(JsonArray results, String[] fields) throws AutomicException {
        JsonElement jsonEle = results.get(0);
        JsonObject jsonObj = jsonEle.getAsJsonObject();
        for (String field : fields) {
            if (!jsonObj.has(field)) {
                throw new AutomicException(String.format("Provided Field [%s] does not exist.", field));
            }
        }
    }

    private String[] validateAndPreapreFields(String[] array) throws AutomicException {
        List<String> fields = new ArrayList<String>();
        if (array != null && array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                String value = array[i].trim();
                if (CommonUtil.checkNotEmpty(value)) {
                    fields.add(value);
                }
            }
        }
        if (fields.isEmpty()) {
            throw new AutomicException("Invalid Fields value.");
        }
        return fields.toArray(new String[0]);
    }

}
