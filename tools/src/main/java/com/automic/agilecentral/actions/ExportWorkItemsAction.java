package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CSVWriter;
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

    /**
     * Exclude fields
     */
    private static final String[] EXCLUDEHEADERS = new String[] { "_rallyAPIMajor", "_rallyAPIMinor", "_ref",
            "_refObjectUUID", "_objectVersion", "ObjectUUID", "VersionId", "HasParent", "_type" };
    private static final int PAGE_SIZE_THRESHOLD = 200;
    private static final int LIMIT_THRESHHOLD = 200;
    private String[] fields;
    private String filePath;
    private int lineCount;
    private String field;
    private int limit;

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
        try {
            QueryResponse queryResponse = this.rallyRestTarget.query(queryRequest);
            if (queryResponse.wasSuccessful()) {
                JsonArray results = queryResponse.getResults();
                int totalResultCount = queryResponse.getTotalResultCount();
                if (results != null && results.size() > 0) {
                    if (fields == null) {
                        fields = getFields(results);
                    } else {
                        // Validate fields exist in json data
                        checkFieldExist(results, fields);
                    }
                    createCSV(totalResultCount, queryRequest, fields);
                }
                filePath = lineCount > 0 ? filePath : "";
                ConsoleWriter.writeln("UC4RB_AC_EXPORT_FILE_PATH ::=" + filePath);
                ConsoleWriter.writeln("UC4RB_AC_TOTAL_RESULT_COUNT ::=" + totalResultCount);
            } else {
                ConsoleWriter.writeln(Arrays.toString(queryResponse.getErrors()));
                throw new AutomicException("Unable to export work items");
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }

    }

    private void createCSV(final int totalRecords, QueryRequest queryRequest, final String[] headers)
            throws AutomicException {

        int totalRequestSize = 0;
        if (limit == 0) {
            totalRequestSize = totalRecords;
        } else {
            totalRequestSize = totalRecords > limit ? limit : totalRecords;
        }
        int start = 0;
        try (CSVWriter writer = new CSVWriter(filePath, headers)) {
            while (start < totalRequestSize) {
                queryRequest.setStart(start);
                QueryResponse queryResponse = this.rallyRestTarget.query(queryRequest);
                if (queryResponse.wasSuccessful()) {
                    ConsoleWriter.newLine();
                    ConsoleWriter.writeln("Start with index : " + start);
                    ConsoleWriter.writeln("Fetched Record size : " + queryResponse.getResults().size());
                    csvWriter(queryResponse.getResults(), fields, filePath, writer);
                }

                start = start + LIMIT_THRESHHOLD + 1;
                if (start > totalRequestSize) {
                    start = totalRequestSize;
                }
            }
            ConsoleWriter.newLine();
        } catch (IOException e) {
            throw new AutomicException(" Error in writing csv file" + e.getMessage());
        }
    }

    private QueryRequest prepareAndValidateInputs() throws AutomicException {

        String workSpaceName = getOptionValue("workspace");
        // Validate Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        String filters = getOptionValue("filters");
        field = getOptionValue("fields");

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
        String rowLimit = getOptionValue("limit");
        AgileCentralValidator.checkNotEmpty(rowLimit, "Maximum Items");

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

        queryRequest.setPageSize(PAGE_SIZE_THRESHOLD);
        queryRequest.setLimit(LIMIT_THRESHHOLD);
        limit = CommonUtil.parseStringValue(rowLimit, -1);

        return queryRequest;
    }

    private void csvWriter(JsonArray results, String[] fields, String filePath, CSVWriter writer)
            throws AutomicException {
        if (fields != null && fields.length > 0) {
            try {
                List<String> fieldsData = new ArrayList<String>(fields.length);
                for (JsonElement result : results) {
                    JsonObject workItem = result.getAsJsonObject();
                    for (String field : fields) {
                        JsonElement jeObj = workItem.get(field);
                        if (jeObj != null && jeObj.isJsonPrimitive()) {
                            fieldsData.add(jeObj.getAsString());
                        } else if (jeObj != null && jeObj.isJsonObject()) {
                            JsonObject jObj = (JsonObject) jeObj;
                            if (jObj.has("_refObjectName")) {
                                fieldsData.add(jObj.get("_refObjectName").getAsString());
                            } else {
                                fieldsData.add("");
                            }
                        } else {
                            fieldsData.add("");
                        }
                    }
                    writer.writeLine(fieldsData);
                    fieldsData.clear();
                    lineCount++;
                }
            } catch (IOException e) {
                throw new AutomicException(" Error in writing csv file" + e.getMessage());
            }
        }

    }

    private String[] getFields(JsonArray results) throws IOException {
        Set<String> excludeHeaders = new HashSet<String>(Arrays.asList(EXCLUDEHEADERS));
        JsonElement jsonEle = results.get(0);
        JsonObject jsonObj = jsonEle.getAsJsonObject();
        Set<Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        List<String> fields = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            JsonElement value = jsonObj.get(key);
            if (!excludeHeaders.contains(key)) {
                if (value.isJsonPrimitive()) {
                    fields.add(key);
                } else if (value.isJsonObject()) {
                    JsonObject jObj = (JsonObject) value;
                    if (jObj.has("_refObjectName")) {
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
        Set<Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        for (int i = 0; i < fields.length; i++) {
            if (!jsonObj.has(field)) {
                boolean flag = true;
                for (Map.Entry<String, JsonElement> entry : entrySet) {
                    String key = entry.getKey();
                    if (CommonUtil.checkNotEmpty(key) && fields[i].equalsIgnoreCase(key)) {
                        fields[i] = key;
                        flag = false;
                    }
                }
                if (flag) {
                    throw new AutomicException(String.format("Provided Field [%s] does not exist.", field));
                }
            }
        }
    }

    private String[] validateAndPreapreFields(String[] array) throws AutomicException {
        Set<String> uniqueFields = new HashSet<String>();
        if (array != null && array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                String value = array[i].trim();
                if (CommonUtil.checkNotEmpty(value)) {
                    uniqueFields.add(value);
                }
            }
        }
        if (uniqueFields.isEmpty()) {
            throw new AutomicException(String.format("Invalid Fields have been provided [%s] ", field));
        }
        return uniqueFields.toArray(new String[0]);
    }

}
