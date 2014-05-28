import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import io.fluxx.api.FluxxInvoker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;


public class FluxxApiExample {


    protected static Log log = LogFactory.getLog(FluxxApiExample.class);

    public static void main(String[] args) {

        Gson gson = new Gson();

        // example credentials.  credentials can be generated from the /oauth/applications page of your target Fluxx instance
        String applicationId = "cf797376871e6fb4f6b1f0b8723c23b57e01c94677b61f2925a92348f088ade1";
        String applicationSecret = "ae70700c6e1f7903bd70f85b8df554f43f2418f81b90ce373e54fd7b48ab21f5";
        String endpoint = "http://ef.local:3000";  // note, generally https except for a local development fluxx server

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
        Map result = invoker.invoke("grant_request/list", params);

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
