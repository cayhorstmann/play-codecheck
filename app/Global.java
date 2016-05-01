import models.PlayConfig;
import models.Util;
import play.Application;
import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

public class Global extends GlobalSettings {

	// http://stackoverflow.com/questions/25152277/play-framework-2-3-cors-headers/26604110#26604110
	// For CORS
	private class ActionWrapper extends Action.Simple {
		public ActionWrapper(Action<?> action) {
			this.delegate = action;
		}

		@Override
		public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
			Promise<Result> result = this.delegate.call(ctx);
			Http.Response response = ctx.response();
			response.setHeader("Access-Control-Allow-Origin", "*");
			return result;
		}
	}

	@Override
	public Action<?> onRequest(Http.Request request, java.lang.reflect.Method actionMethod) {
		return new ActionWrapper(super.onRequest(request, actionMethod));
	}

	@Override
	public void onStart(Application app) {
		try {
			Util.loadS3Credentials(PlayConfig.INSTANCE);
		} catch (IOException ex) {
			RuntimeException wrapper = new RuntimeException();
			wrapper.initCause(ex);
			throw wrapper;
		}
	}
}
