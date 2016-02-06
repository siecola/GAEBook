package com.siecola.exemplo1.services;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;
import com.siecola.exemplo1.models.Product;
import com.siecola.exemplo1.models.User;

@Path("/message")
public class MessageManager {

	private static final Logger log = Logger.getLogger("MessageManager");

	@Context
	SecurityContext securityContext;

	@POST
	@Path("/sendproduct/{product_code}/{email}")
	@RolesAllowed({"ADMIN"})
	public String sendProduct (@PathParam("product_code") int productCode, @PathParam("email") String email) {
		
		Product product;		
		if ((product = findProduct(productCode)) == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		User user;
		if ((user = findUser(email)) == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		Sender sender = new Sender("AIzaSyB1dPp609IGN0kP7xoLiL27QO63HYjd6GU");	
		Gson gson = new Gson();
		Message message = new Message.Builder().addData("product", gson.toJson(product)).build();			
		Result result;
		
		try {
			result = sender.send(message, user.getGcmRegId(), 5);	
			if (result.getMessageId() != null) {
				String canonicalRegId = result.getCanonicalRegistrationId();
				if (canonicalRegId != null) {
					log.severe("Usuário [" + user.getEmail() + "] com mais de um registro");
				}
			} else {	
				String error = result.getErrorCodeName();
				log.severe("Usuário [" + user.getEmail() + "] não registrado");
				log.severe(error);
				throw new WebApplicationException(Status.NOT_FOUND);					
			}
		} catch (IOException e) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
		return "Produto " + product.getName() + " enviado com sucesso para usuário " + user.getEmail();
	}
	
	private User findUser(String email) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Filter emailFilter = new FilterPredicate(UserManager.PROP_EMAIL, FilterOperator.EQUAL, email);
			
		Query query = new Query(UserManager.USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		
		if (userEntity != null) {
			return UserManager.entityToUser(userEntity);
		} else {
			return null;
		}			
	}

	private Product findProduct (int code) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Filter codeFilter = new FilterPredicate("Code", FilterOperator.EQUAL, code);
			
		Query query = new Query("Products").setFilter(codeFilter);
		Entity productEntity = datastore.prepare(query).asSingleEntity();
		
		if (productEntity != null) {		
			return ProductManager.entityToProduct(productEntity);
		} else {
			return null;
		}			
	}	
}
