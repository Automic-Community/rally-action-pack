/**
 * 
 */
package com.automic.agilecentral.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

/**
 * @author sumitsamson
 *
 */
public class RallyUtil {

    private static final String OBJECT_ID = "ObjectID";

    public static QueryResponse query(RallyRestApi restApi, String type, Map<String, String> queryFilter,
            List<String> fetch, Map<String, String> queryParam) throws IOException, AutomicException {

        QueryRequest queryRequest = new QueryRequest(type);

        if (null != fetch && fetch.size() > 0) {
            queryRequest.setFetch(new Fetch(fetch.toArray(new String[fetch.size()])));
        }

        if (null != queryFilter && queryFilter.size() > 0) {
            List<QueryFilter> queryFilterList = new ArrayList<QueryFilter>();
            for (Map.Entry<String, String> filter : queryFilter.entrySet()) {
                queryFilterList.add(new QueryFilter(filter.getKey(), "=", filter.getValue()));
            }
            queryRequest
                    .setQueryFilter(QueryFilter.and(queryFilterList.toArray(new QueryFilter[queryFilterList.size()])));
        }

        if (queryParam != null && queryParam.size() > 0) {
            for (Map.Entry<String, String> param : queryParam.entrySet()) {
                queryRequest.addParam(param.getKey(), param.getValue());
            }
        }

        QueryResponse queryResponse = restApi.query(queryRequest);

        if (!queryResponse.wasSuccessful()) {
            throw new AutomicException(queryResponse.getErrors()[0]);
        }
        return queryResponse;

    }

    public static String getObjectId(RallyRestApi restApi, String type, Map<String, String> queryFilter,
            List<String> fetch, Map<String, String> queryParam) throws IOException, AutomicException {

        QueryResponse queryResponse = query(restApi, type, queryFilter, fetch, queryParam);
        int totalResultCount = queryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            type = ("HIERARCHICALREQUIREMENT".equalsIgnoreCase(type) ? "User Story " : type);
            throw new AutomicException(String.format("[%s] named [%s] found [%s] , expected 1", type,
                    queryFilter.get("Name"), totalResultCount));
        }
        return queryResponse.getResults().get(0).getAsJsonObject().get(OBJECT_ID).getAsString();

    }

    public static void processCustomFields(String fileName, JsonObject jsonObj) throws AutomicException {
        List<String> list = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            list = stream.filter(s -> !s.isEmpty())// trim them and filter out
                                                   // all empty lines
                    .map(line -> line.trim()).collect(Collectors.toList());

            for (String fields : list) {
                String[] field = fields.split("=");
                if (field.length == 2) {
                    Arrays.stream(field).map(String::trim).toArray(unused -> field);// Trim
                    jsonObj.addProperty(field[0], field[1]);
                } else {
                    String errorMsg = String.format(
                            "Error in the given custom field [%s] ,please provide the valid input e.g Key1=Val1 ",
                            fields.toString());
                    throw new AutomicException(errorMsg);
                }
            }

        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while processing custom fields :: " + e.getMessage());
        }
    }

    public static String getWorspaceRef(RallyRestApi restApi, String workspaceName) throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(Constants.WORKSPACE);
        queryRequest.setQueryFilter(new QueryFilter("Name", "=", workspaceName));

        QueryResponse workspaceQueryResponse;
        try {
            workspaceQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceName);
        }

        if (!workspaceQueryResponse.wasSuccessful()) {
            throw new AutomicException(workspaceQueryResponse.getErrors()[0]);
        }

        int totalResultCount = workspaceQueryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            throw new AutomicException(String.format("Workspace named %s found %s ,expected 1", workspaceName,
                    totalResultCount));
        }
        return workspaceQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

    public static String getProjectRef(RallyRestApi restApi, String projectName, String workspaceRef)
            throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(Constants.PROJECT);
        queryRequest.setQueryFilter(new QueryFilter("Name", "=", projectName));

        if (CommonUtil.checkNotEmpty(workspaceRef)) {
            queryRequest.setWorkspace(workspaceRef);
        }

        QueryResponse projectQueryResponse;
        try {
            projectQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceRef);
        }

        if (!projectQueryResponse.wasSuccessful()) {
            throw new AutomicException(projectQueryResponse.getErrors()[0]);
        }

        int totalResultCount = projectQueryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            throw new AutomicException(String.format("Project named %s found %s ,expected 1", projectName,
                    totalResultCount));
        }
        return projectQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

    /**
     * Get the ref of the WSAPI object.
     * 
     * @param restApi
     *            rally
     * @param formattedId
     * @param workspaceRef
     * @param type
     * @return
     * @throws AutomicException
     */
    public static String getWorkItemRef(RallyRestApi restApi, String formattedId, String workspaceRef, String type)
            throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(type);
        queryRequest.setQueryFilter(new QueryFilter("FormattedID", "=", formattedId));
        if (CommonUtil.checkNotEmpty(workspaceRef)) {
            queryRequest.setWorkspace(workspaceRef);
        }

        QueryResponse storyQueryResponse;
        try {
            storyQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceRef);
        }

        if (!storyQueryResponse.wasSuccessful()) {
            throw new AutomicException(storyQueryResponse.getErrors()[0]);
        }

        int totalResultCount = storyQueryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            throw new AutomicException(String.format("User story id %s found %s ,expected 1", formattedId,
                    totalResultCount));
        }
        return storyQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

}
