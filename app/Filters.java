import play.api.mvc.EssentialFilter;
import play.filters.cors.CORSFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

/**
 * Global HTTP filters.
 *
 * Copied from CORS documentation for Play:
 * https://www.playframework.com/documentation/2.5.x/CorsFilter#Cross-Origin-Resource-Sharing
 */
public class Filters implements HttpFilters {

    @Inject
    CORSFilter corsFilter;

    public EssentialFilter[] filters() {
        return new EssentialFilter[] { corsFilter };
    }
}