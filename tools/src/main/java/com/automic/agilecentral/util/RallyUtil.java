/**
 * 
 */
package com.automic.agilecentral.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    /**
     * this method is used to read the custom input fields from file and add them to the json.
     * 
     * @param fileName
     * @param jsonObj
     * @throws AutomicException
     */
    public static void processCustomFields(String fileName, JsonObject jsonObj) throws AutomicException {

        Properties prop = new Properties();

        try (FileInputStream input = new FileInputStream(fileName)) {
            prop.load(input);
            Set<String> keys = prop.stringPropertyNames();
            for (String key : keys) {
                if (CommonUtil.checkNotEmpty(key)) {
                    jsonObj.addProperty(key, prop.getProperty(key));
                }
            }

        } catch (IOException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException("Error occured while processing custom fields :: " + e.getMessage());
        }

    }

    /**
     * Get the workspace ref of the WSAPI object.
     * 
     * @param restApi
     * @param workspaceName
     *            Name of the workspace
     * @return
     * @throws AutomicException
     */
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
            throw new AutomicException(
                    String.format("Workspace named %s found %s ,expected 1", workspaceName, totalResultCount));
        }
        return workspaceQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

    /**
     * Get the project ref of the WSAPI object.
     * 
     * @param restApi
     * @param projectName
     *            Name of the project
     * @param workspaceRef
     *            Reference of the workspace in which the project is located. May be absolute or relative.
     * @return
     * @throws AutomicException
     */
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
            throw new AutomicException(
                    String.format("Project named %s found %s ,expected 1", projectName, totalResultCount));
        }
        return projectQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

    /**
     * Get the work item ref of the WSAPI object.
     * 
     * @param restApi
     * @param formattedId
     *            Formatted Id of the work item. E.g US12/TS20 etc
     * @param workspaceRef
     *            Reference of the workspace in which the work item is located
     * @param type
     *            Type of workitem - hierarchicalrequirement/defect/task etc
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
            throw new AutomicException(
                    String.format("User story id %s found %s ,expected 1", formattedId, totalResultCount));
        }
        return storyQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
    }

    public static QueryResponse queryOR(RallyRestApi restApi, String type, String workspaceRef,
            Map<String, List<String>> queryFilter, List<String> fetch, Map<String, String> queryParam)
            throws IOException, AutomicException {
        QueryFilter qf = null;
        if (null != queryFilter && queryFilter.size() > 0) {

            for (Map.Entry<String, List<String>> filter : queryFilter.entrySet()) {
                List<String> values = filter.getValue();
                for (String val : values) {
                    qf = qf == null ? new QueryFilter(filter.getKey(), "=", val)
                            : qf.or(new QueryFilter(filter.getKey(), "=", val));
                }

            }

        }

        return query(restApi, type, workspaceRef, qf, fetch, queryParam);

    }

    public static QueryResponse queryAND(RallyRestApi restApi, String type, String workspaceRef,
            Map<String, List<String>> queryFilter, List<String> fetch, Map<String, String> queryParam)
            throws IOException, AutomicException {

        QueryFilter qf = null;
        if (null != queryFilter && queryFilter.size() > 0) {

            for (Map.Entry<String, List<String>> filter : queryFilter.entrySet()) {
                List<String> values = filter.getValue();
                for (String val : values) {
                    qf = qf == null ? new QueryFilter(filter.getKey(), "=", val)
                            : qf.and(new QueryFilter(filter.getKey(), "=", val));
                }

            }

        }

        return query(restApi, type, workspaceRef, qf, fetch, queryParam);

    }

    public static QueryResponse query(RallyRestApi restApi, String type, String workspaceRef, QueryFilter queryFilter,
            List<String> fetch, Map<String, String> queryParam) throws IOException, AutomicException {
        QueryRequest queryRequest = new QueryRequest(type);
        if (null != workspaceRef) {
            queryRequest.setWorkspace(workspaceRef);
        }

        if (null != fetch && fetch.size() > 0) {
            queryRequest.setFetch(new Fetch(fetch.toArray(new String[fetch.size()])));
        }

        if (null != queryFilter) {
            queryRequest.setQueryFilter(queryFilter);
        }

        if (queryParam != null && queryParam.size() > 0) {
            for (Map.Entry<String, String> param : queryParam.entrySet()) {
                queryRequest.addParam(param.getKey(), param.getValue());
            }
        }

        QueryResponse queryResponse = restApi.query(queryRequest);

        if (!queryResponse.wasSuccessful()) {
            throw new AutomicException(Arrays.toString(queryResponse.getErrors()));
        }
        return queryResponse;

    }

}
