package sharepoint.igt.com.connection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import sharepoint.igt.com.utils.SharepointUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * Basically a token helper, but actually takes care of Auth stuff
 */
public class SharepointAuth {
    private String clientID;
    private String secretID;
    private String domain;
    private String site;
    private String fullSiteURL = "";
    private String baseURL = "";
    private Pair authToken = null;

    public SharepointAuth(String clientID, String secretID, String domain, String site, String fullSiteURL) {
        this.clientID = clientID;
        this.secretID = secretID;
        this.domain = domain;
        this.site = site;
        this.fullSiteURL = fullSiteURL;

        this.baseURL = "https://" + this.domain;
        this.authenticate();
    }

    public Pair getAuthToken() {
        if(authToken == null){
            throw new RuntimeException("Auth token not stablished yet! - Quit!");
        }

        return authToken;
    }

    private String authenticate() {
        Pair<String, String> bearerRealmAndRessourceId = getBearerRealmAndResourceId();
        String bearerRealm = bearerRealmAndRessourceId.getLeft();
        String ressourceId = bearerRealmAndRessourceId.getRight();

        String bearerToken = getBearerToken(bearerRealm, ressourceId);

        this.authToken = Pair.of("Bearer", bearerToken);

        return bearerToken; // in case token might be needed?
    }

    private String getBearerToken(String bearerRealm, String ressourceId) {
        // static URL for authentication
        String url = String.format("https://accounts.accesscontrol.windows.net/%s/tokens/OAuth/2", bearerRealm);

        HttpPost postRequest = new HttpPost(url);
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        String clientId = String.format("%s@%s", clientID, bearerRealm);
        String resource = String.format("%s/%s@%s", ressourceId, domain, bearerRealm);

        List<BasicNameValuePair> params =  Arrays.asList(new BasicNameValuePair[] {
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("client_id", clientId),
                new BasicNameValuePair("client_secret", secretID),
                new BasicNameValuePair("scope", resource),
                new BasicNameValuePair("resource", resource)
        });

        try  {
            postRequest.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = getHttpClient().execute(postRequest);

            String bodyJson = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            JSONObject body = new JSONObject(bodyJson);
            String bearerToken = body.getString("access_token");

            return bearerToken;
        } catch (IOException e) {
            throw new RuntimeException("Error processing bearer token", e);
        }
    }

    private Pair<String, String> getBearerRealmAndResourceId() {
        HttpHead headRequest = new HttpHead(fullSiteURL);
        headRequest.setHeader("Authorization", "Bearer");

        try {
            HttpResponse response = getHttpClient().execute(headRequest);
            Header[] headers = response.getHeaders("www-authenticate");

            String bearerRealm = SharepointUtils.extractHeaderElement(headers, "Bearer realm");
            String ressourceId = SharepointUtils.extractHeaderElement(headers, "client_id");

            return Pair.of(bearerRealm, ressourceId);
        } catch (IOException e) {
            throw new RuntimeException("Error getting bearer realm and res. id", e);
        }
    }

    protected CloseableHttpClient getHttpClient() {
        try {
            CloseableHttpClient httpClient;

            TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            SSLContext sslContext = null;
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                    NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory> create()
                            .register("https", sslsf)
                            .register("http", new PlainConnectionSocketFactory())
                            .build();

            BasicHttpClientConnectionManager connectionManager =
                    new BasicHttpClientConnectionManager(socketFactoryRegistry);

            httpClient = HttpClients.custom()
                    .setHostnameVerifier(new AllowAllHostnameVerifier())
                    .setSSLSocketFactory(sslsf)
                    .setConnectionManager(connectionManager).build();

            return httpClient;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFullSiteURL() {
        return fullSiteURL;
    }

    public String getDomain() {
        return domain;
    }

    public String getSite() {
        return site;
    }

    public String getBaseURL() {
        return baseURL;
    }
}
