/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.orcidclient.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.auth.AccessToken;
import edu.cornell.mannlib.orcidclient.beans.ExternalId;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext.Setting;
import edu.cornell.mannlib.orcidclient.orcidmessage.ExternalIdCommonName;
import edu.cornell.mannlib.orcidclient.orcidmessage.ExternalIdReference;
import edu.cornell.mannlib.orcidclient.orcidmessage.ExternalIdUrl;
import edu.cornell.mannlib.orcidclient.orcidmessage.ExternalIdentifier;
import edu.cornell.mannlib.orcidclient.orcidmessage.ExternalIdentifiers;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidBio;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidMessage;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidProfile;

/**
 * Example using Curl:
 * 
 * <pre>
 * curl -H 'Content-Type: application/orcid+xml' 
 *      -H 'Authorization: Bearer 5be2dfe5-b834-436e-8c10-7b3fcf65e1a2' 
 *      -d '@/Documents/XML/add_id.xml' 
 *      -X POST 'http://api.sandbox-1.orcid.org/v1.0.23/0000-0001-6259-6530/orcid-bio/external-identifiers'
 * </pre>
 * 
 * Data file to send:
 * 
 * <pre>
 * <orcid-message 
 *         xsi:schemaLocation="http://www.orcid.org/ns/orcid https://raw.github.com/ORCID/ORCID-Source/master/orcid-model/src/main/resources/orcid-message-1.0.23.xsd" 
 *         xmlns="http://www.orcid.org/ns/orcid">
 *     <message-version>1.0.23</message-version>
 *     <orcid-profile>
 *         <orcid-bio>
 *             <external-identifiers visibility="public">
 *                 <external-identifier>
 *                     <external-id-common-name>My ID system</external-id-common-name>
 *                     <external-id-reference>#1234-567-8</external-id-reference>
 *                     <external-id-url>http://www.myid.com/1234-567-8</external-id-url>
 *                 </external-identifier>
 *             </external-identifiers>
 *         </orcid-bio>
 *     </orcid-profile>
 * </orcid-message>
 * </pre>
 * 
 * @see http://support.orcid.org/knowledgebase/articles/186983-add-external-ids-
 *      technical-developer
 */
public class AddExternalIdAction {
	private static final Log log = LogFactory.getLog(AddExternalIdAction.class);

	private final OrcidClientContext occ;

	public AddExternalIdAction() {
		this.occ = OrcidClientContext.getInstance();
	}

	public OrcidMessage execute(ExternalId externalId, AccessToken accessToken)
			throws OrcidClientException {

		try {
			URI baseUri = new URI(occ.getSetting(Setting.AUTHORIZED_API_BASE_URL));
			log.debug("authorizedApiBaseUrl: <"+baseUri+">");
			String requestUrl = URIUtils.resolve(baseUri,
					accessToken.getOrcid() + "/orcid-bio/external-identifiers")
					.toString();
			
			log.debug("Request URL: " + requestUrl);

			OrcidMessage outMessage = buildOutgoingMessage(externalId);
			String outString = occ.marshall(outMessage);
			log.debug("Outgoing string: " + outString);

			Request request = Request
					.Post(requestUrl)
					.addHeader("Content-Type", "application/orcid+xml")
					.addHeader(
							"Authorization",
							accessToken.getTokenType() + " "
									+ accessToken.getAccessToken())
					.bodyString(outString,
							ContentType.APPLICATION_FORM_URLENCODED);
			log.debug("Request Info: "+request);
			Response response = request.execute();
			Content content = response.returnContent();
			String string = content.asString();
			log.debug("Content from AddExternalID was: " + string);

			return occ.unmarshall(string);
		} catch (URISyntaxException e) {
			throw new OrcidClientException(
					"API_BASE_URL is not syntactically valid.", e);
		} catch (HttpResponseException e) {
			// Bad status code? Something funky.
			log.error("HttpResponse status code: " + e.getStatusCode());
			log.error("HttpResponse message: " + e.getMessage());
			throw new OrcidClientException(
					"Failed to add external ID. HTTP status code="
							+ e.getStatusCode(), e);
		} catch (IOException e) {
			throw new OrcidClientException("Failed to add external ID", e);
		}
	}

	private OrcidMessage buildOutgoingMessage(ExternalId externalId) {
		ExternalIdCommonName eicn = new ExternalIdCommonName();
		eicn.setContent(externalId.getCommonName());

		ExternalIdReference eir = new ExternalIdReference();
		eir.setContent(externalId.getReference());

		ExternalIdUrl eiu = new ExternalIdUrl();
		eiu.setValue(externalId.getUrl());

		ExternalIdentifier ei = new ExternalIdentifier();
		ei.setExternalIdCommonName(eicn);
		ei.setExternalIdReference(eir);
		ei.setExternalIdUrl(eiu);

		ExternalIdentifiers eis = new ExternalIdentifiers();
		eis.getExternalIdentifier().add(ei);
		eis.setVisibility(externalId.getVisibility());

		OrcidBio ob = new OrcidBio();
		ob.setExternalIdentifiers(eis);

		OrcidProfile op = new OrcidProfile();
		op.setOrcidBio(ob);

		OrcidMessage om = new OrcidMessage();
		om.setOrcidProfile(op);
		om.setMessageVersion(occ.getSetting(Setting.MESSAGE_VERSION));

		return om;
	}

}
