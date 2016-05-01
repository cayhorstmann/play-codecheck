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
