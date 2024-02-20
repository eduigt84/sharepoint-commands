package sharepoint.igt.com.connection;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SharepointHttpContext {

    private static final Logger LOG = LoggerFactory.getLogger(SharepointHttpContext.class);

    private SharepointAuth sharepointAuth;

    public SharepointHttpContext(String clientID, String secretID, String domain, String site) {
        sharepointAuth = new SharepointAuth(clientID, secretID, domain, site, String.format("https://%s/sites/%s", domain, site));
    }

    protected File downloadFile(HttpRequestBase request, String fileName) {
        try {
            HttpResponse response = sharepointAuth.getHttpClient().execute(request);
            InputStream is = response.getEntity().getContent();

            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            int inByte;
            while((inByte = is.read()) != -1) {
                fos.write(inByte);
            }
            is.close();
            fos.close();

            return file;
        } catch (IOException  ex) {
            LOG.error("Some unexpected IO error happened!");
            ex.printStackTrace();
        }

        return null;
    }

    protected JSONObject executeQuery(HttpRequestBase request) {
        try {
            HttpResponse response = sharepointAuth.getHttpClient().execute(request);
            if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 201) {
                String methodReference = "";
                String path = "";
                try {
                    methodReference = Thread.currentThread().getStackTrace()[2].getMethodName();
                    path = request.getURI().getPath();
                } catch(Exception ex) {
                    //noop it's okay just for debugging
                }

                LOG.error("Some error happened, please check transaction, method: {}, API: {}, error={}", methodReference, path,  response.getStatusLine().getStatusCode());
                return null;
            } else {
                String bodyJson = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                return new JSONObject(bodyJson);
            }
        } catch (IOException  ex) {
            LOG.error("Some unexpected IO error happened!");
            ex.printStackTrace();
        }

        return null;
    }

    protected Header requireContext() throws Exception {
        HttpPost contextRequest = generateRequest(HttpPost.class, "_api/contextinfo");
        HttpResponse contextResponse = sharepointAuth.getHttpClient().execute(contextRequest);
        String bodyJson = IOUtils.toString(contextResponse.getEntity().getContent(), Charset.defaultCharset());

        JSONObject body = new JSONObject(bodyJson);
        String formDigestValue = body.getJSONObject("d").getJSONObject("GetContextWebInformation").getString("FormDigestValue");

        return new BasicHeader("X-RequestDigest", formDigestValue);
    }

    protected <T> T generateRequest(Class<T> clazz, String suffix) {
        return generateRequest(clazz, suffix, false);
    }

    protected <T> T generateRequest(Class<T> clazz, String suffix, boolean requiresContext) {
        // httpClient = HttpClients.createDefault(); // renew client

        if(clazz.getTypeName().contains("HttpGet")) {
            HttpGet getRequest = new HttpGet(String.format("%s/%s", this.sharepointAuth.getFullSiteURL(), suffix));
            getRequest.setHeaders(getHeaders(requiresContext));

            return (T) getRequest;
        } else if(clazz.getTypeName().contains("HttpPost")){
            HttpPost postRequest = new HttpPost(String.format("%s/%s", this.sharepointAuth.getFullSiteURL(), suffix));
            postRequest.setHeaders(getHeaders(requiresContext));

            return (T) postRequest;
        } else {
            return null; // see the world burn :)
        }
    }

    protected Header[] getHeaders(boolean contextRequired) {
        ArrayList<Header> headers = new ArrayList();

        headers.add(new BasicHeader("Authorization", this.sharepointAuth.getAuthToken().getLeft() + " " + this.sharepointAuth.getAuthToken().getRight()));
        headers.add(new BasicHeader("Accept", "application/json;odata=verbose"));
        headers.add(new BasicHeader("content-type", "application/json;odata=verbose"));

        if (contextRequired) {
            LOG.debug("Context required for transaction...");

            try {
                Header context = requireContext();
                headers.add(context);
                LOG.debug("Context acquired OK");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return headers.toArray(new Header[0]);
    }

    protected StringEntity stringiFyPayload(JSONObject payload) {
        String payloadStr = payload.toString();

        try {
            StringEntity ent = new StringEntity(payloadStr, "application/json", "UTF-8");
            return ent;
        } catch (UnsupportedEncodingException ex){
            LOG.error("Something went wrong transforming json object to string");
            ex.printStackTrace();
        }

        return null;
    }

    protected String encode(String value) {
        String encodedPageName = value;

        try {
            // ahhh sharepoint is so picky...
            encodedPageName = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replaceAll("%2F", "/").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Can't encode page name {}", value);
        }

        return encodedPageName;
    }


    public SharepointAuth getSharepointAuth() {
        return sharepointAuth;
    }
}