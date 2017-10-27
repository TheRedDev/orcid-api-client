/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.orcidclient.actions.version_2_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.auth.AccessToken;
import edu.cornell.mannlib.orcidclient.beans.ExternalId;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.model.OrcidProfile;
import edu.cornell.mannlib.orcidclient.responses.message_2_0.OrcidExternalIdentifier;
import edu.cornell.mannlib.orcidclient.responses.message_2_0.OrcidString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 */
public class AddExternalIdAction implements edu.cornell.mannlib.orcidclient.actions.AddExternalIdAction {
	private static final Log log = LogFactory.getLog(AddExternalIdAction.class);

	private final OrcidClientContext occ;

	public AddExternalIdAction() {
		this.occ = OrcidClientContext.getInstance();
	}
	
	private static String httpEntityAsString(HttpEntity httpEntity) throws IOException {
    Charset charset = ContentType.getOrDefault(httpEntity).getCharset();
    if (charset == null) {
        charset = HTTP.DEF_CONTENT_CHARSET;
    }
    try {
        return new String(EntityUtils.toByteArray(httpEntity), charset.name());
    } catch (UnsupportedEncodingException ex) {
        return new String(EntityUtils.toByteArray(httpEntity));
    }
}

	@Override
	public OrcidProfile execute(ExternalId externalId, AccessToken accessToken)
			throws OrcidClientException {

		try {
			URI baseUri = new URI(occ.getApiMemberUrl());
			log.debug("authorizedApiBaseUrl: <"+baseUri+">");
			String requestUrl = URIUtils.resolve(baseUri, accessToken.getOrcid() + "/external-identifiers").toString();
			log.debug("Request URL: " + requestUrl);

			OrcidExternalIdentifier id = new OrcidExternalIdentifier();

			id.setExtCommonName(externalId.getCommonName());
			id.setExtReference(externalId.getReference());
			OrcidString url = new OrcidString();
			url.setValue(externalId.getUrl());
			id.setExtUrl(url);
			id.setExtRelationship("SELF");
			id.setVisibility(externalId.getVisibility().toString());

			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(id);

			log.debug("Outgoing string: " + json);

			Request request = Request
					.Post(requestUrl)
					.addHeader("Content-Type", "application/vnd.orcid+json")
					.addHeader(
							"Authorization",
							accessToken.getTokenType() + " "
									+ accessToken.getAccessToken())
					.bodyString(json, ContentType.APPLICATION_FORM_URLENCODED);
			Response response = request.execute();
			HttpResponse httpResponse = response.returnResponse();
      HttpEntity entity = httpResponse.getEntity();
			String string = httpEntityAsString(entity);
      StatusLine statusLine = httpResponse.getStatusLine();
			log.debug("Content from AddExternalID was: " + string);
      if (statusLine.getStatusCode() == 409 && string.contains("of type 'external-identifier' is duplicated")) {
        log.debug(externalId.getUrl() + " was already linked on orcid profile");
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(),
                statusLine.getReasonPhrase());
      }

			ReadProfileAction readAction = new ReadProfileAction();
			return readAction.execute(accessToken);
		} catch (URISyntaxException e) {
			throw new OrcidClientException(
					"API_BASE_URL is not syntactically valid.", e);
		} catch (HttpResponseException e) {
			// Bad status code? Something funky.
			log.error("HttpResponse status code: " + e.getStatusCode());
			throw new OrcidClientException(
					"Failed to add external ID. HTTP status code="
							+ e.getStatusCode() + " message="+e.getMessage(), e);
		} catch (IOException e) {
			throw new OrcidClientException("Failed to add external ID", e);
		}
	}
}
