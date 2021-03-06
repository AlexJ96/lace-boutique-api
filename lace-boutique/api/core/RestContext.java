package api.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.endpoint.EndPoint;
import api.services.TokenService;
import api.utils.Utils;
import spark.Service;

public class RestContext {
	
	private static final Logger logger = LoggerFactory.getLogger(RestContext.class);

    private final Service spark;

    private final String basePath;
    
    /**
     * Production API Url
     */
    //private final String url = "https://api.lace-boutique.uk:8080";
    
    /**
     * Test API Url
     */
    private final String url = "http://localhost:8080";
    
    private final String[] unauthorizedEndPoints = { "/token/request-token", "/token/refresh-token", "/shop/getFilters", "/shop/getItems", "/misc/get-page-content" };

    public RestContext(int port, String basePath) {
        this.basePath = basePath;
        spark = Service.ignite().port(port);
        //spark.secure("C:/Users/Administrator/Downloads/selfsigned.jks", "laceboutique", null, null);
    }

    public void addEndpoint(EndPoint endpoint) {
        endpoint.configure(spark, basePath);
        logger.info("REST endpoints registered for {}.", endpoint.getClass().getSimpleName());
    }
    
    public void enableCors() {
    	spark.options("/*", (request,response)->{
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if(accessControlRequestMethod != null){
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });
    	
    	spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type, LBT");
            
            boolean needsAuth = true;
            
//            System.out.println(request.url());
            
            for (String unauthorizedEndPoint : unauthorizedEndPoints) {
            	if (request.url().equalsIgnoreCase(url + basePath + unauthorizedEndPoint))
            		needsAuth = false;
            }
            
            if (needsAuth == true) {
            	if (!request.headers().contains("LBT")) {
            		spark.halt(200, Utils.getJsonBuilder().toJson("Missing LBT Header"));
            	} else {
		            String token = request.headers("LBT").substring(14).trim();
					int responseId = TokenService.validateToken(token);
					
					if (responseId != 0) {
						spark.halt(responseId, Utils.getJsonBuilder().toJson("Unauthorised Access"));
					}
            	}
            }
            System.out.println(request.url());
    	});
        logger.info("CORS support enabled.");
    }

}
