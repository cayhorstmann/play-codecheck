package controllers;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import models.Problem;
import models.Util;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.createAssignment;
import views.html.showAssignment;
	    
public class LTIController extends Controller {
    public Result config() throws UnknownHostException {
        String host = request().host();
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        return ok(views.xml.lti_config.render(host)).as("application/xml");
    } 
    
    /*
     * Called from Canvas and potentially other LMS with a "resource selection" interface
     */
    public Result createAssignment() throws UnsupportedEncodingException {    
	 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	Logger.info("LTIController.createAssignment: " + Util.paramsToString(postParams));
	 	/*
	 	if (!Util.validate(request())) {
	 		session().clear();
	 		return badRequest("Failed OAuth validation");
	 	}	 	
	 	*/
		String role = Util.getParam(postParams, "roles");
		if (!Util.isInstructor(role)) 
			return badRequest("Instructor role is required to create an assignment.");
    	String userId = Util.getParam(postParams, "user_id");
		if (Util.isEmpty(userId)) 
			return badRequest("No user id");
		session().put("user", userId);

		String contextId = Util.getParam(postParams, "context_id");
		String resourceLinkId = Util.getParam(postParams, "resource_link_id");
		String toolConsumerId = Util.getParam(postParams, "tool_consumer_instance_guid");
		String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
		return ok(createAssignment.render(contextId, resourceLinkId, 
			toolConsumerId, launchPresentationReturnURL));			
 	}
        
	// This method gets called when an assignment has been created with createAssignment.scala.html.
	public Result addAssignment() {
		Map<String, String[]> postParams = request().body().asFormUrlEncoded();

		/*Assignment assignment = new Assignment();
		assignment.contextId = Util.getParam(postParams, "context_id");
		assignment.resourceLinkId = Util.getParam(postParams, "resource_link_id");
		assignment.toolConsumerId = Util.getParam(postParams, "tool_consumer_id");
       
		String duration = Util.getParam(postParams, "duration");
		if(duration.equals(""))
    	   assignment.duration = 0;
		else
    	   assignment.duration = Integer.parseInt(duration);
		assignment.save();
		try {
			addNewProblemsFromFormSubmission(problemlist, assignment);
		} catch (Exception ex) {
			return badRequest(ex.getMessage());
		}*/

        String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
        String problemURL = Util.getParam(postParams, "url");
        String host = request().host();
        int ltiInsertLoc = problemURL.indexOf(host) + host.length();
        String assignmentURL = problemURL.substring(0, ltiInsertLoc) + "/lti" + problemURL.substring(ltiInsertLoc);
        return ok(showAssignment.render(launchPresentationReturnURL, 
    		   Util.getParams(launchPresentationReturnURL), assignmentURL));
    }
    
}