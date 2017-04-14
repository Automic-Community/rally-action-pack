/**
 * 
 */
package com.automic.agilecentral.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

}
