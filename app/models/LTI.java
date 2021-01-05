package models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.Assignment;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
import play.Logger;
import play.mvc.Http;

@Singleton
public class LTI {
	@Inject private S3Connection s3conn;
	private static Logger.ALogger logger = Logger.of("com.horstmann.codecheck");
	
    public boolean validate(Http.Request request) {
    	final String OAUTH_KEY_PARAMETER = "oauth_consumer_key";
    	
    	Map<String, String[]> postParams = request.body().asFormUrlEncoded();
    	if (postParams == null) return false;
    	Set<Map.Entry<String, String>> entries = new HashSet<>();
	 	for (Map.Entry<String, String[]> entry : postParams.entrySet()) 
	 		for (String s : entry.getValue())
	 			entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), s));
	 	String url = Util.prefix(request) + request.uri();
	 	
	 	String key = Util.getParam(postParams, OAUTH_KEY_PARAMETER);
	 	for (Map.Entry<String, String> entry : Util.getParams(url).entrySet())
	 		entries.add(entry);
	 	int n = url.lastIndexOf("?"); 
	 	if (n >= 0) url = url.substring(0, n);
	 	OAuthMessage oam = new OAuthMessage("POST", url, entries);
        OAuthConsumer cons = new OAuthConsumer(null, key, getSharedSecret(key), null); 
        OAuthValidator oav = new SimpleOAuthValidator();
        OAuthAccessor acc = new OAuthAccessor(cons);
        
        try {
	      oav.validateMessage(oam, acc);
          return true;
        } catch (Exception e) {
        	logger.error("Did not validate: " + e.getLocalizedMessage() + "\nurl: " + url + "\nentries: " + entries);
            return false;
        }
    }
            
	public String getSharedSecret(String oauthConsumerKey) {
		String sharedSecret = "";
		try {
			ObjectNode result = s3conn.readJsonObjectFromDynamoDB("CodeCheckLTICredentials", "oauth_consumer_key", oauthConsumerKey);
			if (result != null) sharedSecret = result.get("shared_secret").asText();
			else logger.warn("No shared secret for consumer key " + oauthConsumerKey);
		} catch (IOException e) {
			logger.warn("Could not read CodeCheckLTICredentials");
			// Return empty string
		}		
		return sharedSecret;
	}	    

	public void passbackGradeToLMS(String gradePassbackURL,
			String sourcedID, double score, String oauthConsumerKey)
			throws URISyntaxException, IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, NoSuchAlgorithmException {
		String oauthSecret = getSharedSecret(oauthConsumerKey);
		String xmlString1 = "<?xml version = \"1.0\" encoding = \"UTF-8\"?> <imsx_POXEnvelopeRequest xmlns = \"http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0\"> <imsx_POXHeader> <imsx_POXRequestHeaderInfo> <imsx_version>V1.0</imsx_version> <imsx_messageIdentifier>" 
	            + System.currentTimeMillis() + "</imsx_messageIdentifier> </imsx_POXRequestHeaderInfo> </imsx_POXHeader> <imsx_POXBody> <replaceResultRequest> <resultRecord> <sourcedGUID> <sourcedId>";
		String xmlString2 = "</sourcedId> </sourcedGUID> <result> <resultScore> <language>en</language> <textString>";
		String xmlString3 = "</textString> </resultScore> </result> </resultRecord> </replaceResultRequest> </imsx_POXBody> </imsx_POXEnvelopeRequest>";        	
		String xml = xmlString1 + sourcedID + xmlString2 + score + xmlString3;        	
		
		URL url = new URL(gradePassbackURL);
		HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
		request.setRequestMethod("POST");
		request.setRequestProperty("Content-Type", "application/xml");
		//request.setRequestProperty("Authorization", "OAuth"); // Needed for Moodle???
		
		byte[] xmlBytes = xml.getBytes("UTF-8"); 
		request.setRequestProperty("Content-Length", Integer.toString(xmlBytes.length));

		// https://stackoverflow.com/questions/28204736/how-can-i-send-oauth-body-hash-using-signpost
		DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(oauthConsumerKey, oauthSecret);
		consumer.setTokenWithSecret(null, null);
		MessageDigest md = MessageDigest.getInstance("SHA1");
		String bodyHash = Base64.getEncoder().encodeToString(md.digest(xmlBytes));
		HttpParameters params = new HttpParameters();
        params.put("oauth_body_hash", URLEncoder.encode(bodyHash, "UTF-8"));
        //params.put("realm", gradePassbackURL); // http://zewaren.net/site/?q=node/123
        consumer.setAdditionalParameters(params);        
		consumer.sign(request); 		

		//logger.info("Request after signing: {}", consumer.getRequestParameters());
		//logger.info("XML: {}", xml);

		// POST the xml to the grade passback url
		request.setDoOutput(true);
		OutputStream out = request.getOutputStream();
		out.write(xmlBytes);
		out.close();
		// TODO: Eliminate this log? Or look for error only?
		// request.connect();
		logger.info(request.getResponseCode() + " " + request.getResponseMessage());
		try {
			InputStream in = request.getInputStream();
			String body = new String(Util.readAllBytes(in), "UTF-8");
			logger.info("Response body received from LMS: " + body);
		} catch (Exception e) {			
			InputStream in = request.getErrorStream();
			String body = new String(Util.readAllBytes(in), "UTF-8");
			logger.info("Response error received from LMS: " + body);
		}
	}			
}