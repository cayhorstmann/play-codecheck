package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

    public Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public Result preflight() {
        response().setHeader("Access-Control-Allow-Origin", "*");
        return ok();
    }
}
