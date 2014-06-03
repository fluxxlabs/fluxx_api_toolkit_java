package io.fluxx.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FluxxInvoker {

    // needed for JDK 1.7 to avoid "unrecognized_name for SNI" errors
    static {
        System.setProperty("jsse.enableSNIExtension", "false");
    }


    protected String applicationId;
    protected String applicationSecret;
    protected String endpoint;
    protected String token;

    protected static Log log = LogFactory.getLog(FluxxInvoker.class);

    Gson gson = new Gson();


    public FluxxInvoker(String endpoint, String applicationId, String applicationSecret) {
        this.endpoint = endpoint;
        this.applicationId = applicationId;
        this.applicationSecret = applicationSecret;
    }


    public void refreshToken() {
        token = getToken();
    }


    protected String getToken() {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost method = new HttpPost(endpoint + "/oauth/token");
            Map params = ImmutableMap.of("grant_type", "client_credentials", "client_id", applicationId, "client_secret", applicationSecret);
            method.setEntity(mapToFormEntity(params));

            HttpResponse response = httpClient.execute(method);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = read(response.getEntity().getContent());

            if (responseCode != HttpStatus.SC_OK) {
                String message = "unexpected status code: " + responseCode + ", body: " + responseBody;
                System.err.println(message);
                throw new RuntimeException(message);
            }
            log.debug("response: " + responseBody);
            HashMap parsed = gson.fromJson(responseBody, HashMap.class);
            return (String) parsed.get("access_token");
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Map get(String uri, Map<String,String> params) {
        return invoke( "GET", uri, params);
    }

    public Map put(String uri, Map<String,String> params) {
        return invoke( "PUT", uri, params);
    }

    public Map post(String uri, Map<String,String> params) {
        return invoke( "POST", uri, params);
    }

    public Map delete(String uri, Map<String,String> params) {
        return invoke( "DELETE", uri, params);
    }

    public Map invoke(String uri, Map<String,String> params) {
        return post(uri, params);
    }

    public Map invoke(String methodType, String uri, Map<String,String> params) {
        if (token == null)
            refreshToken();

        String rawResponse = null;
        try {
            rawResponse = rawInvoke(methodType, uri, params);
        }
        catch (UnauthorizedStatus e) {
            log.debug("'unauthorized' response - resetting bearer token");
            refreshToken();
            rawResponse = rawInvoke(uri, methodType, params);
        }

        Map parsed = gson.fromJson(rawResponse, HashMap.class);
        log.debug("parsed: " + parsed);
        return parsed;
    }

    public String rawInvoke(String methodType, String uri, Map<String,String> params) {
        try {
            String fullUri = endpoint + "/api/rest/v1/" + uri;
            HttpPost method = new HttpPost(fullUri);
            method.addHeader("Authorization", "Bearer " + token);
            method.addHeader("X-HTTP-Method-Override", methodType);
            method.setEntity(mapToFormEntity(params));

            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(method);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = read(response.getEntity().getContent());
            log.debug("response code: " + responseCode);
            if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                log.warn("unauthorized response: " + responseBody);
                throw new UnauthorizedStatus("unauthorized: " + responseBody);
            } else if (responseCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                throw new RuntimeException("The Post method is not implemented by this URI: " + fullUri + ", resp: " + responseBody);
            } else if (responseCode != HttpStatus.SC_OK) {
                String message = "unexpected status code: " + responseCode + ", body: " + responseBody;
                log.warn(message);
                throw new RuntimeException(message);
            }
            return responseBody;
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected HttpEntity mapToFormEntity(Map<String,String> params) throws UnsupportedEncodingException {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (Map.Entry<String,String> entry : params.entrySet()) {
            pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return new UrlEncodedFormEntity(pairs);
    }


    class UnauthorizedStatus extends RuntimeException {
        UnauthorizedStatus(String message) {
            super(message);
        }
    }


    protected static final int BUFFER_SIZE = 8192;

    public static String read(InputStream inputStream) {
        return read( new InputStreamReader(inputStream) );
    }

    public static String read(Reader reader) {
        try {
            BufferedReader in;
            if (reader instanceof BufferedReader)
                in = (BufferedReader) reader;
            else
                in = new BufferedReader(reader);

            CharArrayWriter data = new CharArrayWriter();
            char buf[] = new char[BUFFER_SIZE];
            int ret;
            while ((ret = in.read(buf, 0, BUFFER_SIZE)) != -1)
                data.write(buf, 0, ret);
            String result = data.toString();
            return result;
        }
        catch (IOException e) {
            log.error(e,e);
            throw new RuntimeException(e);
        }
    }

}
