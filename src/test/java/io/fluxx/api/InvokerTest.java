package io.fluxx.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.List;
import java.util.Map;


public class InvokerTest {


    protected static Log log = LogFactory.getLog(InvokerTest.class);

    String applicationId = "cf797376871e6fb4f6b1f0b8723c23b57e01c94677b61f2925a92348f088ade1";
    String applicationSecret = "ae70700c6e1f7903bd70f85b8df554f43f2418f81b90ce373e54fd7b48ab21f5";
    String endpoint = "http://ef.local:3000";  // note, generally https except for a local development fluxx server

    @Test
    public void simpleUserFilter() {

        Gson gson = new Gson();

        // example credentials.  credentials can be generated from the /oauth/applications page of your target Fluxx instance

        FluxxInvoker invoker = new FluxxInvoker(endpoint, applicationId, applicationSecret);

        // note, for complex api parameters like the filter criteria, you can either hand craft a properly escaped json string,
        // or use the Google Guava Gson library as demonstrated below

        // String filterJson = "{\"group_type\":\"and\",\"conditions\":[[\"granted\",\"eq\",\"1\"],[\"program_organization_id\",\"filter\",{\"group_type\":\"and\",\"conditions\":[[\"geo_state_id\",\"eq\",\"4014\"],[\"geo_country_id\",\"eq\",\"250\"]]}]]}";

        Map filter = ImmutableMap.of("group_type", "and", "conditions", ImmutableList.of(
                ImmutableList.of("last_name", "eq", "Abe")));

        Map<String,String> params = ImmutableMap.of("style", "full", "filter", gson.toJson(filter));

        // result will be generically parsed by Gson into untyped maps and lists
        // in the future we plan to support generation of strongly typed model classes and automatic marshalling into those objects
        Map result = invoker.invoke("user/list", params);

        log.info("full result: " + result);
//        log.info("total entries: " + result.get("total_entries"));
//        List<Map> grants = (List<Map>)((Map)result.get("records")).get("grant_request");
//        if (grants != null && grants.size() > 0) {
//            Map grant = grants.get(0);
//            log.info("first grant: " + grant);
//            log.info("project summary: " + grant.get("project_summary"));
//        }
    }


    @Test
    public void simpleUserRawFilter() {
        FluxxInvoker invoker = new FluxxInvoker(endpoint, applicationId, applicationSecret);
        String filter = "{\"group_type\":\"and\",\"conditions\":[[\"last_name\",\"eq\",\"Warning\"]]}";
        Map<String,String> params = ImmutableMap.of("style", "full", "filter", "{\"group_type\":\"and\",\"conditions\":[[\"last_name\",\"eq\",\"Warning\"]]}");
        Map result = invoker.get("user/list", params);
        log.info("full result: " + result);
    }

    @Test
    public void createUser() {
        FluxxInvoker invoker = new FluxxInvoker(endpoint, applicationId, applicationSecret);
        Gson gson = new Gson();
        Map<String,String> data = new ImmutableMap.Builder()
                .put("email", "john2@doe.com")
                .put("first_name", "John")
                .put("last_name", "Doe")
                .put("login", "johndoe2")
                .put("password", "jd")
                .put("password_confirmation", "jd")
                .build();
        Map result = invoker.post("user", ImmutableMap.of("data", gson.toJson(data)));
        log.info("full result: " + result);
        Map user = (Map) result.get("user");
        if (user != null) {
            int id = ((Double)user.get("id")).intValue();
            log.info("new user id: " + id);
        } else {
            Map error = (Map) result.get("error");
            if (error != null)
                log.info("user creation failed: " + error.get("message"));
            else
                log.warn("unexpected result: " + result);
        }
    }



    @Test
    public void updateUser() {
        FluxxInvoker invoker = new FluxxInvoker(endpoint, applicationId, applicationSecret);
        Gson gson = new Gson();

        Map filter = ImmutableMap.of("group_type", "and", "conditions", ImmutableList.of(
                ImmutableList.of("email", "eq", "john@doe.com")));
        Map listResult = invoker.get("user/list", ImmutableMap.of("style", "full", "filter", gson.toJson(filter)));

        List<Map> users = (List<Map>)((Map)listResult.get("records")).get("user");
        if (users != null && users.size() > 0) {
            Map first = users.get(0);
            log.info("first user: " + first);
            int id = ((Double)first.get("id")).intValue();

            Map<String,String> data = new ImmutableMap.Builder()
                    .put("first_name", "Johnny")
                    .put("last_name", "Doe")
                    .build();
            Map result = invoker.put("user/"+id, ImmutableMap.of("data", gson.toJson(data)));
            log.info("full result: " + result);
        } else {
            log.info("user not found");
        }

    }



    //    @Test
    public void filteredGrants() {

        Gson gson = new Gson();

        FluxxInvoker invoker = new FluxxInvoker(endpoint, applicationId, applicationSecret);

        // note, for complex api parameters like the filter criteria, you can either hand craft a properly escaped json string,
        // or use the Google Guava Gson library as demonstrated below

        // String filterJson = "{\"group_type\":\"and\",\"conditions\":[[\"granted\",\"eq\",\"1\"],[\"program_organization_id\",\"filter\",{\"group_type\":\"and\",\"conditions\":[[\"geo_state_id\",\"eq\",\"4014\"],[\"geo_country_id\",\"eq\",\"250\"]]}]]}";

        Map filter = ImmutableMap.of("group_type", "and", "conditions", ImmutableList.of(
                        ImmutableList.of("granted", "eq", "1"),
                        ImmutableList.of("program_organization_id", "filter",
                                ImmutableMap.of("group_type", "and", "conditions", ImmutableList.of(
                                        ImmutableList.of("geo_state_id", "eq", "4014"),
                                        ImmutableList.of("geo_country_id", "eq", "250"))))));

        Map<String,String> params = ImmutableMap.of("style", "full", "filter", gson.toJson(filter));

        // result will be generically parsed by Gson into untyped maps and lists
        // in the future we plan to support generation of strongly typed model classes and automatic marshalling into those objects
        Map result = invoker.get("grant_request/list", params);

        log.info("full result: " + result);
        log.info("total entries: " + result.get("total_entries"));
        List<Map> grants = (List<Map>)((Map)result.get("records")).get("grant_request");
        if (grants != null && grants.size() > 0) {
            Map grant = grants.get(0);
            log.info("first grant: " + grant);
            log.info("project summary: " + grant.get("project_summary"));
        }

	}


}
