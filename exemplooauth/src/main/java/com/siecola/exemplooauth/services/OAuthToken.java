package com.siecola.exemplooauth.services;

import javax.annotation.security.PermitAll;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.siecola.exemplooauth.TokenCacheManager;

@Path("/token")
public class OAuthToken {

	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	@PermitAll
	public Response authorize(@Context HttpServletRequest request, MultivaluedMap<String, String> form)
			throws OAuthSystemException {
		try {
			OAuthTokenRequest oauthRequest = new OAuthTokenRequest(new OAuthRequestWrapper(request, form));
			OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(
					new MD5Generator());

			Entity userEntity = findUserByEmail(oauthRequest.getUsername());
			if (userEntity == null) {
				return buildInvalidUserPassResponse();				
			}

			if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
					GrantType.AUTHORIZATION_CODE.toString())) {
				if (!checkAuthCode(oauthRequest.getParam(OAuth.OAUTH_CODE))) {
					return buildBadAuthCodeResponse();
				}
			} else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
					GrantType.PASSWORD.toString())) {
				if (!userEntity.getProperty(UserManager.PROP_PASSWORD).equals(oauthRequest.getPassword())) {
					return buildInvalidUserPassResponse();
				}
			}
			
			final String accessToken = oauthIssuerImpl.accessToken();
			
			if (saveToken(oauthRequest.getUsername(), accessToken)) {
				OAuthResponse response = OAuthASResponse
						.tokenResponse(HttpServletResponse.SC_OK)
						.setAccessToken(accessToken)
						.setExpiresIn("3600")
						.setTokenType("bearer")								
						.buildJSONMessage();
				return Response.status(response.getResponseStatus())
						.entity(response.getBody()).build();				
			} else {
				return buildInternalServerError();
			}

		} catch (OAuthProblemException e) {
			OAuthResponse res = OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
					.buildJSONMessage();
			return Response.status(res.getResponseStatus())
					.entity(res.getBody()).build();
		}
	}

	private Response buildBadAuthCodeResponse() throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse
				.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
				.setError(OAuthError.TokenResponse.INVALID_GRANT)
				.setErrorDescription("invalid authorization code")
				.buildJSONMessage();
		return Response.status(response.getResponseStatus())
				.entity(response.getBody()).build();
	}

	private Response buildInvalidUserPassResponse() throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse
				.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
				.setError(OAuthError.TokenResponse.INVALID_GRANT)
				.setErrorDescription("invalid username or password")
				.buildJSONMessage();
		return Response.status(response.getResponseStatus())
				.entity(response.getBody()).build();
	}

	private Response buildInternalServerError() throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse
				.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
				.setError(OAuthError.TokenResponse.INVALID_REQUEST)
				.setErrorDescription("internal server error")
				.buildJSONMessage();
		return Response.status(response.getResponseStatus())
				.entity(response.getBody()).build();
	}

	private boolean checkAuthCode(String authCode) {
		return authCode.equals(OAuth.OAUTH_BEARER_TOKEN);
	}
	
	private Entity findUserByEmail(String email) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Filter emailFilter = new FilterPredicate(UserManager.PROP_EMAIL, FilterOperator.EQUAL, email);
			
		Query query = new Query(UserManager.USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();

		return userEntity;
	}
	
	@SuppressWarnings({"unchecked"})
	private boolean saveToken (String email, String token) {
        try {
    		Cache cache = TokenCacheManager.getInstance().getCache();
        	
            cache.put(token, email);
        } catch (CacheException e) {
        	return false;
        }		
		
		return true;
	}
}

class OAuthRequestWrapper extends HttpServletRequestWrapper {
	private MultivaluedMap<String, String> form;

	public OAuthRequestWrapper(HttpServletRequest request,
			MultivaluedMap<String, String> form) {
		super(request);
		this.form = form;
	}

	@Override
	public String getParameter(String name) {
		String value = super.getParameter(name);
		if (value == null) {
			value = form.getFirst(name);
		}
		return value;
	}
	
	@Override
	public String[] getParameterValues(String name) {
	    String[] values = super.getParameterValues(name);
	    if(values == null && form.get(name) != null){
	        values = new String[form.get(name).size()];
	        values = form.get(name).toArray(values);
	    }
	    return values;
	}
}