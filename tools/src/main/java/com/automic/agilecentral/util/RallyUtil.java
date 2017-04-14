/**
 * 
 */
package com.automic.agilecentral.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.automic.agilecentral.exception.AutomicException;
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

	public static QueryResponse query(RallyRestApi restApi, String type, Map<String, String> queryFilter,
			List<String> fetch) throws IOException {

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

		return restApi.query(queryRequest);

	}

	public static String getObjectId(RallyRestApi restApi, String type, Map<String, String> queryFilter,
			List<String> fetch) throws IOException, AutomicException {

		QueryResponse queryResponse = query(restApi, type, queryFilter, fetch);
		int totalResultCount = queryResponse.getTotalResultCount();

		if (totalResultCount != 1) {
			type = (type.equalsIgnoreCase("HIERARCHICALREQUIREMENT") ? "User Story " : type);
			throw new AutomicException(String.format("[%s] named [%s] found [%s] , expected 1", type,
					queryFilter.get("Name"), totalResultCount));
		}
		return queryResponse.getResults().get(0).getAsJsonObject().get("ObjectID").getAsString();

	}

}
