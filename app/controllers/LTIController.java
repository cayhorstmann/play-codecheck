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
	    
public class LTIController extends Controller {
    public Result config() throws UnknownHostException {
        String host = request().host();
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        return ok(views.xml.lti_config.render(host)).as("application/xml");
    } 
    
}