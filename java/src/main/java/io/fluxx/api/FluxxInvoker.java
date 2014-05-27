package io.fluxx.api;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.HashMap;
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
            HttpClient httpClient = new HttpClient();
            PostMethod method = new PostMethod(endpoint + "/oauth/token");
            method.addParameter("grant_type", "client_credentials");
            method.addParameter("client_id", applicationId);
            method.addParameter("client_secret", applicationSecret);

            int responseCode = httpClient.executeMethod(method);
            String responseBody = read(method.getResponseBodyAsStream());

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

    public Map invoke(String uri, Map<String,String> params) {
        if (token == null)
            refreshToken();

        String rawResponse = null;
        try {
            rawResponse = rawInvoke(uri, params);
        }
        catch (UnauthorizedStatus e) {
            log.debug("'unauthorized' response - resetting bearer token");
            refreshToken();
            rawResponse = rawInvoke(uri, params);
        }

        Map parsed = gson.fromJson(rawResponse, HashMap.class);
        log.debug("parsed: " + parsed);
        return parsed;
    }

    public String rawInvoke(String uri, Map<String,String> params) {
        try {
            String fullUri = endpoint + "/api/rest/v1/" + uri;
            PostMethod method = new PostMethod(fullUri);
            method.addRequestHeader("Authorization", "Bearer " + token);
            for (Map.Entry<String,String> entry : params.entrySet()) {
                method.addParameter( entry.getKey(), entry.getValue() );
            }

            HttpClient httpClient = new HttpClient();
            int responseCode  = httpClient.executeMethod(method);
            String responseBody = read(method.getResponseBodyAsStream());
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
