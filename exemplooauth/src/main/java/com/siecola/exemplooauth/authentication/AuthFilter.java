package com.siecola.exemplooauth.authentication;
		
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.siecola.exemplooauth.TokenCacheManager;
import com.siecola.exemplooauth.models.User;
import com.siecola.exemplooauth.services.UserManager;

public class AuthFilter implements ContainerRequestFilter {

	private static final String ACCESS_UNAUTHORIZED = "Você não tem permissão para acessar esse recurso";
	
    @Context
    private ResourceInfo resourceInfo;
        
	@Override
	public void filter(ContainerRequestContext requestContext) {
		
		Method method = resourceInfo.getResourceMethod();
		
		if (method.isAnnotationPresent(PermitAll.class)) {
			return;
		}
			
		String auth = requestContext.getHeaderString("Authorization");
		
		if (auth == null) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				    .entity(ACCESS_UNAUTHORIZED).build());
			return;
		}

        String accessToken = auth.substring("Bearer ".length());        
		
		RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
	    Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAllowed.value()));

	    if (checkCredentialsAndRoles (accessToken, rolesSet, requestContext) == false) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				    .entity(ACCESS_UNAUTHORIZED).build());
            return;	    	
	    } 
	}
	
	private boolean checkCredentialsAndRoles (String accessToken, Set<String> roles, ContainerRequestContext requestContext) {
		boolean isUserAllowed = false;
		String email;
		
        try {
        	Cache cache = TokenCacheManager.getInstance().getCache();
        	
	    	if (cache.containsKey(accessToken)) {
	    		email = (String) cache.get(accessToken);
	    
	    		DatastoreService datastore = DatastoreServiceFactory
	    				.getDatastoreService();

	    		Filter emailFilter = new FilterPredicate(UserManager.PROP_EMAIL, FilterOperator.EQUAL, email);
	    			
	    		Query query = new Query(UserManager.USER_KIND).setFilter(emailFilter);
	    		Entity userEntity = datastore.prepare(query).asSingleEntity();
	    		
	    		if (userEntity != null) {			
	    			if (roles.contains(userEntity.getProperty(UserManager.PROP_ROLE))) {
	    				
	    				final User user = updateUserLogin(datastore, userEntity);
	    				
	    			    requestContext.setSecurityContext(new SecurityContext() {
	    					
	    					@Override
	    					public boolean isUserInRole(String role) {
	    						return role.equals(user.getRole());
	    					}
	    					
	    					@Override
	    					public boolean isSecure() {
	    						return true;
	    					}
	    					
	    					@Override
	    					public Principal getUserPrincipal() {
	    						return user;
	    					}
	    					
	    					@Override
	    					public String getAuthenticationScheme() {
	    						return SecurityContext.BASIC_AUTH;
	    					}
	    				});
	    				
	    				isUserAllowed = true;
	    			}
	    		}	    		
	    	}
        } catch (CacheException e) {
        }       
		
		return isUserAllowed;
	}
		
	@SuppressWarnings("unchecked")
	private User updateUserLogin (DatastoreService datastore, Entity userEntity) {
		User user = new User();
		boolean canUseCache = true;
		boolean saveOnCache = true;
		
		String email = (String) userEntity.getProperty(UserManager.PROP_EMAIL);
		
        try {
        	Cache cache = TokenCacheManager.getInstance().getCache();
        	
	    	if (cache.containsKey(email)) {
	    		Date lastLogin = (Date)cache.get(email);
	    		if ((Calendar.getInstance().getTime().getTime() - lastLogin.getTime()) < 30000) {
	    			saveOnCache = false;
	    		}
	    	} 
	    	
	    	if (saveOnCache == true) {
				cache.put(email, (Date)Calendar.getInstance().getTime());            		
				canUseCache = false;            		
	    	}
        } catch (CacheException e) {
        	canUseCache = false;        	
        }		
        
        if (canUseCache == false) {
    		user.setEmail((String) userEntity.getProperty(UserManager.PROP_EMAIL));
    		user.setPassword((String) userEntity.getProperty(UserManager.PROP_PASSWORD));
    		user.setId(userEntity.getKey().getId());
    		user.setGcmRegId((String) userEntity.getProperty(UserManager.PROP_GCM_REG_ID));
    		user.setLastLogin((Date) Calendar.getInstance().getTime());
    		user.setLastGCMRegister((Date) userEntity.getProperty(UserManager.PROP_LAST_GCM_REGISTER));
    		user.setRole((String) userEntity.getProperty(UserManager.PROP_ROLE));			    
    	    
    		userEntity.setProperty(UserManager.PROP_LAST_LOGIN, user.getLastLogin());
    		datastore.put(userEntity);        	
        }
		
		return user;
	}
}

