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
public final class RallyUtil {

    private static final String OBJECT_ID = "ObjectID";

    public static QueryResponse query(RallyRestApi restApi, String type, Map<String, String> queryFilter,
            List<String> fetch, Map<String, String> queryParam) throws IOException {

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

        return queryResponse;

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

    public static String getWorspaceId(RallyRestApi restApi, String workspaceName) throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(Constants.WORKSPACE);
        queryRequest.setQueryFilter(new QueryFilter("Name", "=", workspaceName));

        QueryResponse workspaceQueryResponse;
        try {
            workspaceQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceName);
        }

        int totalResultCount = workspaceQueryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            throw new AutomicException(
                    String.format("Workspace named %s found %s ,expected 1", workspaceName, totalResultCount));
        }
        return workspaceQueryResponse.getResults().get(0).getAsJsonObject().get(OBJECT_ID).getAsString();
    }

    public static String getProjectId(RallyRestApi restApi, String projectName, String workspaceName)
            throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(Constants.PROJECT);
        queryRequest.setQueryFilter(new QueryFilter("Name", "=", projectName));
        if (null != workspaceName && !workspaceName.isEmpty()) {
            queryRequest.addParam(Constants.WORKSPACE, workspaceName);
        }

        QueryResponse projectQueryResponse;
        try {
            projectQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceName);
        }

        int totalResultCount = projectQueryResponse.getTotalResultCount();
        projectQueryResponse.wasSuccessful();

        if (totalResultCount != 1) {
            throw new AutomicException(
                    String.format("Project named %s found %s ,expected 1", projectName, totalResultCount));
        }
        return projectQueryResponse.getResults().get(0).getAsJsonObject().get(OBJECT_ID).getAsString();
    }

    public static String getWorkItemRef(RallyRestApi restApi, String type ,String formattedId, String projectName,
            String workspaceName) throws AutomicException {
        QueryRequest queryRequest = new QueryRequest(type);
        queryRequest.setQueryFilter(new QueryFilter("FormattedID", "=", formattedId));
        if (null != workspaceName && !workspaceName.isEmpty()) {
            queryRequest.addParam(Constants.WORKSPACE, workspaceName);
        }

        if (null != projectName && !projectName.isEmpty()) {
            queryRequest.addParam(Constants.PROJECT, projectName);
        }

        QueryResponse storyQueryResponse;
        try {
            storyQueryResponse = restApi.query(queryRequest);
        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while getting workspace id for workspace name =" + workspaceName);
        }

        int totalResultCount = storyQueryResponse.getTotalResultCount();

        if (totalResultCount != 1) {
            throw new AutomicException(
                    String.format("User story id %s found %s ,expected 1", formattedId, totalResultCount));
        }
        return storyQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

}
