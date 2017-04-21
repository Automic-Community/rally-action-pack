package com.automic.agilecentral.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ExportUserStories extends AbstractHttpAction {

    private String[] fields;
    private String filePath;

    public ExportUserStories() {
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
                if (fields == null) {
                    fields = getFields(queryResponse.getResults());
                }
                csvWriter(queryResponse.getResults(), fields, filePath);
                ConsoleWriter.writeln("UC4RB_AC_EXPORT_FILE_PATH ::=" + filePath);
                ConsoleWriter.writeln("UC4RB_AC_TOTAL_RESULT_COUNT ::=" + queryResponse.getResults().size());
                ConsoleWriter.writeln("Total available record count : " + queryResponse.getTotalResultCount());
            } else {
                throw new AutomicException(Arrays.toString(queryResponse.getErrors()));
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
        filePath = getOptionValue("exportfilepath");
        AgileCentralValidator.checkNotEmpty(filePath, "Export file path");
        File file = new File(filePath);
        AgileCentralValidator.checkFileWritable(file);
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
            for (int i = 0; i < fields.length; i++)
                fields[i] = fields[i].trim();
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

    public void csvWriter(JsonArray results, String[] fields, String filePath) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        List<String> fieldsData = new ArrayList<String>(fields.length);
        // Add Headers
        CSVUtils.writeLine(writer, Arrays.asList(fields));
        for (JsonElement result : results) {
            JsonObject defect = result.getAsJsonObject();
            for (String field : fields) {
                if (defect.get(field) != null && defect.get(field).isJsonPrimitive()) {
                    fieldsData.add(defect.get(field).getAsString());
                } else if (defect.get(field) != null && defect.get(field).isJsonObject()) {
                    JsonObject jObj = (JsonObject) defect.get(field);
                    if (jObj.has("_refObjectName")) {
                        fieldsData.add(jObj.get("_refObjectName").getAsString());
                    }else{
                        fieldsData.add("");
                    }
                }else{
                    fieldsData.add("");
                }
            }
            CSVUtils.writeLine(writer, fieldsData);
            fieldsData.clear();
        }

        writer.flush();
        writer.close();

    }

    private String[] getFields(JsonArray results) throws IOException {
        JsonElement jsonEle = results.get(0);
        JsonObject jsonObj = jsonEle.getAsJsonObject();
        Set<Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        List<String> fields = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            if (jsonObj.get(entry.getKey()).isJsonPrimitive()) {
                fields.add(entry.getKey());
            } else if (jsonObj.get(entry.getKey()).isJsonObject()) {
                JsonObject jObj = (JsonObject) jsonObj.get(entry.getKey());
                if (jObj.has("_refObjectName")) {
                    fields.add(entry.getKey());
                }
            }
        }
        return fields.toArray(new String[0]);
    }

}
