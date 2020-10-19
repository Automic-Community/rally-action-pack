package com.automic.agilecentral.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.automic.agilecentral.constants.ExceptionConstants;
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

    private static final int BATCH_SIZE = 200;
    private String filePath;
    private int limit;

    private List<String> userInputFields;
    private Set<String> uniqueFields;
    private List<String> resultFields;

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
            QueryResponse queryResponse = runQuery(queryRequest, 1, limit);

            int totalResultCount = queryResponse.getTotalResultCount();
            if (totalResultCount != 0) {
                JsonArray results = queryResponse.getResults();
                JsonObject jsonObj = results.get(0).getAsJsonObject();
                prepareUserFieldMapping(jsonObj);
                createCSV(queryResponse, queryRequest);
                ConsoleWriter.writeln("UC4RB_AC_TOTAL_RESULT_COUNT ::=" + totalResultCount);
                ConsoleWriter.writeln("UC4RB_AC_EXPORT_FILE_PATH ::=" + filePath);
            } else {
                ConsoleWriter.writeln("UC4RB_AC_TOTAL_RESULT_COUNT ::=" + totalResultCount);
            }
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Unable to export work items");
        }
    }

    private QueryResponse runQuery(QueryRequest qReq, int start, int noofrecords) throws AutomicException, IOException {
        qReq.setStart(start);
        if (noofrecords < BATCH_SIZE) {
            qReq.setPageSize(noofrecords);
            qReq.setLimit(noofrecords);
        } else {
            qReq.setPageSize(BATCH_SIZE);
            qReq.setLimit(BATCH_SIZE);
        }
        QueryResponse response = rallyRestTarget.query(qReq);
        if (!response.wasSuccessful()) {
            ConsoleWriter.writeln(Arrays.toString(response.getErrors()));
            throw new AutomicException("Unable to export work items");
        }
        return response;
    }

    private void createCSV(QueryResponse queryResponse, QueryRequest queryRequest) throws AutomicException {
        int maxExport = queryResponse.getTotalResultCount();

        if (limit != 0 && limit < maxExport) {
            maxExport = limit;
        }

        int remainingItems = maxExport;

        int start = 0;
        try (CSVWriter writer = new CSVWriter(filePath, userInputFields)) {
            QueryResponse response = queryResponse;
            do {
                if (start != 0) {
                    int remainingRecords = maxExport - start;
                    response = runQuery(queryRequest, start + 1, remainingRecords);
                }
                JsonArray results = response.getResults();
                int goTill = results.size();
                remainingItems = remainingItems - results.size();
                if (remainingItems < 0) {
                    goTill = results.size() + remainingItems;
                }
                ConsoleWriter.writeln("Remaining Work items to be exported " + remainingItems);
                for (int i = 0; i < goTill; i++) {
                    writer.writeLine(readRecord(results.get(i).getAsJsonObject()));
                }
                writer.flush();
                start = start + results.size();
            } while (start < maxExport);
        } catch (IOException e) {
            throw new AutomicException(" Error in writing csv file" + e.getMessage());
        }
    }

    // Validating and preparing input parameters
    private QueryRequest prepareAndValidateInputs() throws AutomicException {
        // Validate Work item type
        String workItemType = getOptionValue("workitemtype");
        AgileCentralValidator.checkNotEmpty(workItemType, "Work Item type");

        // Validate export file path check for just file name.
        String temp = getOptionValue("exportfilepath");
        AgileCentralValidator.checkNotEmpty(temp, "Export file path");
        File file = new File(temp);
        AgileCentralValidator.checkFileWritable(file);
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException e) {
            throw new AutomicException(" Error in getting unique absolute path " + e.getMessage());
        }

        QueryRequest queryRequest = new QueryRequest(workItemType);
        String workSpaceName = getOptionValue("workspace");
        if (CommonUtil.checkNotEmpty(workSpaceName)) {
            String workSpaceRef = RallyUtil.getWorspaceRef(rallyRestTarget, workSpaceName);
            queryRequest.setWorkspace(workSpaceRef);
        }

        String filters = getOptionValue("filters");
        if (CommonUtil.checkNotEmpty(filters)) {
            queryRequest.addParam("query", filters);
        }

        String fieldInput = getOptionValue("fields");
        if (CommonUtil.checkNotEmpty(fieldInput)) {
            prepareUserFields(fieldInput);
            Fetch fetchList = new Fetch();
            fetchList.addAll(uniqueFields);
            queryRequest.setFetch(fetchList);
        }

        // Validate count
        String rowLimit = getOptionValue("limit");
        limit = CommonUtil.parseStringValue(rowLimit, -1);
        if (limit < 0) {
            throw new AutomicException(String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, "Maximum Items",
                    rowLimit));
        }
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        return queryRequest;
    }

    // Read the record from json object into the form which can be written in CSV format.
    private List<String> readRecord(JsonObject workItem) throws AutomicException {
        List<String> record = new ArrayList<String>(resultFields.size());
        for (String field : resultFields) {
            JsonElement jeObj = workItem.get(field);
            String value = null;
            if (jeObj != null && jeObj.isJsonPrimitive()) {
                value = jeObj.getAsString();
            } else if (jeObj != null && jeObj.isJsonObject()) {
                JsonObject jObj = (JsonObject) jeObj;
                if (jObj.has("_refObjectName")) {
                    value = jObj.get("_refObjectName").getAsString();
                }
            }
            record.add(value);
        }
        return record;
    }

    // Prepare field mapping by comparing user provided fields and actual fields in case insensitive manner.
    private void prepareUserFieldMapping(JsonObject jsonObj) throws AutomicException {
        Set<Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        if (userInputFields == null) {
            resultFields = new ArrayList<String>(entrySet.size());
            List<String> excludeHeaders = getExcludedFields();
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                String key = entry.getKey();
                JsonElement value = jsonObj.get(key);
                if (!excludeHeaders.contains(key)) {
                    if (value.isJsonPrimitive()) {
                        resultFields.add(key);
                    } else if (value.isJsonObject()) {
                        JsonObject jObj = (JsonObject) value;
                        if (jObj.has("_refObjectName")) {
                            resultFields.add(key);
                        }
                    }
                }
            }
            userInputFields = resultFields;
        } else {
            resultFields = new ArrayList<String>();
            Map<String, String> tempMap = new HashMap<String, String>(entrySet.size());
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                String key = entry.getKey();
                tempMap.put(key.toLowerCase(), key);
            }
            StringBuilder invalidFields = new StringBuilder();
            for (String userInput : userInputFields) {
                String resultKey = tempMap.get(userInput.toLowerCase());
                if (resultKey != null) {
                    resultFields.add(resultKey);
                } else {
                    invalidFields.append(userInput).append(" ");
                }
            }
            if (invalidFields.length() != 0) {
                throw new AutomicException("Invalid export fields have been specified " + invalidFields.toString());
            }
        }
    }

    // Validate and Translate user provided field input parameters
    private void prepareUserFields(String fieldInput) throws AutomicException {
        String[] fields = fieldInput.split(",");
        userInputFields = new ArrayList<String>(fields.length);
        uniqueFields = new HashSet<String>(fields.length);
        for (String field : fields) {
            String temp = field.trim();
            if (CommonUtil.checkNotEmpty(temp)) {
                userInputFields.add(temp);
                uniqueFields.add(temp.toLowerCase());
            }
        }
        if (uniqueFields.isEmpty()) {
            throw new AutomicException(String.format("Invalid Fields have been provided [%s] ", fieldInput));
        }
    }

    // Retrieve the list of excluded fields
    private List<String> getExcludedFields() {
        List<String> temp = new ArrayList<>();
        temp.add("_rallyAPIMajor");
        temp.add("_rallyAPIMinor");
        temp.add("_ref");
        temp.add("_refObjectUUID");
        temp.add("_objectVersion");
        temp.add("_refObjectName");
        temp.add("ObjectUUID");
        temp.add("VersionId");
        temp.add("_type");
        return temp;
    }

}