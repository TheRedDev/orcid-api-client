package edu.cornell.mannlib.orcidclient.actions.version_2_0;

import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.model.ExternalIdentifier;
import edu.cornell.mannlib.orcidclient.model.OrcidBio;
import edu.cornell.mannlib.orcidclient.model.OrcidId;
import edu.cornell.mannlib.orcidclient.model.OrcidProfile;
import edu.cornell.mannlib.orcidclient.responses.message_2_0.OrcidExternalIdentifier;
import edu.cornell.mannlib.orcidclient.responses.message_2_0.OrcidPerson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import javax.naming.Name;
import javax.swing.text.html.parser.Entity;

import static edu.cornell.mannlib.orcidclient.context.OrcidClientContext.Setting.API_ENVIRONMENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Util {
    private static PoolingClientConnectionManager cm;
    private static final HttpClient httpClient;

    static {
        cm = new PoolingClientConnectionManager();
        cm.setDefaultMaxPerRoute(50);
        cm.setMaxTotal(300);
        httpClient = new DefaultHttpClient(cm);
    }

    /**
     * Read JSON from the URL
     * @param url
     * @return
     */
    public static String readJSON(String url, List<NameValuePair> headers) {
        try {
            HttpGet request = new HttpGet(url);
//            if (nvps != null) {
//                request.setEntity(new UrlEncodedFormEntity(nvps));
//            }

            // Content negotiate for csl / citeproc JSON
            request.setHeader("Accept", "application/json");
            if (headers != null) {
                for (NameValuePair header : headers) {
                    request.setHeader(header.getName(), header.getValue());
                }
            }

            HttpResponse response = httpClient.execute(request, new BasicHttpContext());
            try {
                switch (response.getStatusLine().getStatusCode()) {
                    case 200:
                        try (InputStream in = response.getEntity().getContent()) {
                            StringWriter writer = new StringWriter();
                            IOUtils.copy(in, writer, "UTF-8");
                            return writer.toString();
                        }
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (IOException e) {
        }

        return null;
    }

    public static OrcidProfile toModel(OrcidPerson om) {
        if (om != null) {
            OrcidProfile profile = new OrcidProfile();
            OrcidBio bio = new OrcidBio();

            if (om.getExternalIdentifiers() != null) {
                List<ExternalIdentifier> idList = new ArrayList<ExternalIdentifier>();

                for (OrcidExternalIdentifier oldId : om.getExternalIdentifiers().getExternalIdentifiers()) {
                    ExternalIdentifier newId = new ExternalIdentifier();

                    if (oldId.getExtCommonName() != null) {
                        newId.setExternalIdCommonName(oldId.getExtCommonName());
                    }
                    if (oldId.getExtReference() != null) {
                        newId.setExternalIdReference(oldId.getExtReference());
                    }
                    if (oldId.getExtUrl() != null) {
                        newId.setExternalIdUrl(oldId.getExtUrl().getValue());
                    }

                    idList.add(newId);
                }

                bio.setExternalIdentifiers(idList);
            }
            profile.setOrcidBio(bio);

            if (om.getName() != null) {
                OrcidId oid = new OrcidId();

                oid.setPath(om.getName().getPath());
                String prefix = "http://";
                if ("sandbox".equalsIgnoreCase(OrcidClientContext.getInstance().getSetting(API_ENVIRONMENT))) {
                  prefix += "sandbox.";
                }
                prefix += "orcid.org/";
                oid.setUri(prefix + om.getName().getPath());
                profile.setOrcidIdentifier(oid);
            }

            return profile;
        }

        return null;
    }
}
