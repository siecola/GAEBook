package com.siecola.exemplo1.services;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.siecola.exemplo1.models.Product;


@Path("/products")
public class ProductManager {

	private static final Logger log = Logger.getLogger("ProductManager");
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	@PermitAll
	public Product getProduct(@PathParam("code") int code) {		
			
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Filter codeFilter = new FilterPredicate("Code", FilterOperator.EQUAL, code);
			
		Query query = new Query("Products").setFilter(codeFilter);
		Entity productEntity = datastore.prepare(query).asSingleEntity();
		
		if (productEntity != null) {
			Product product = entityToProduct(productEntity);
			
			return product;
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public List<Product> getProducts() {

		List<Product> products = new ArrayList<>();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Query query;
		query = new Query("Products").addSort("Code", SortDirection.ASCENDING);

		List<Entity> productsEntities = datastore.prepare(query).asList(
				FetchOptions.Builder.withDefaults());

		for (Entity productEntity : productsEntities) {
			Product product = entityToProduct(productEntity);	
			
			products.add(product);
		}

		return products;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ADMIN", "USER"})
	public Product saveProduct(@Valid Product product) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		if (!checkIfCodeExist (product)) {
			Key productKey = KeyFactory.createKey("Products", "productKey");
			Entity productEntity = new Entity("Products", productKey);

			productToEntity (product, productEntity);
			
			datastore.put(productEntity);

			product.setId(productEntity.getKey().getId());			
		} else {
			throw new WebApplicationException("Já existe um produto cadastrado com o mesmo código", Status.BAD_REQUEST);
		}
		
		return product;
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	@RolesAllowed({"ADMIN", "USER"})
	public Product alterProduct(@PathParam("code") int code, @Valid Product product) {
		if (product.getId() != 0) {
			if (!checkIfCodeExist (product)) {
				DatastoreService datastore = DatastoreServiceFactory
						.getDatastoreService();

				Filter codeFilter = new FilterPredicate("Code",
						FilterOperator.EQUAL, code);

				Query query = new Query("Products").setFilter(codeFilter);

				Entity productEntity = datastore.prepare(query).asSingleEntity();				

				if (productEntity != null) {			
					productToEntity (product, productEntity);
					
					datastore.put(productEntity);
					
					product.setId(productEntity.getKey().getId());
					return product;
				} else {
					throw new WebApplicationException(Status.NOT_FOUND);
				}							
			} else {
				throw new WebApplicationException("Já existe um produto cadastrado com o mesmo código", Status.BAD_REQUEST);
			}
		} else {
			throw new WebApplicationException("O ID do produto deve ser informado para ser alterado", Status.BAD_REQUEST);
		}
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	@RolesAllowed("ADMIN")
	public Product deleteProduct(@PathParam("code") int code) {

		//Mensagem 1 - DEBUG
		log.finest("Tentando apagar produto com código=[" + String.valueOf(code) + "]");
		
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
	
		Filter codeFilter = new FilterPredicate("Code",
				FilterOperator.EQUAL, code);
	
		Query query = new Query("Products").setFilter(codeFilter);
	
		Entity productEntity = datastore.prepare(query).asSingleEntity();				
	
		if (productEntity != null) {
			datastore.delete(productEntity.getKey());
			
			//Mensagem 2 - INFO
			log.info("Produto com código=[" + String.valueOf(code) + "] apagado com sucesso");
					
			Product product = entityToProduct(productEntity);
			
			return product;
		} else {	
			//Mensagem 3 - ERROR
			log.severe ("Erro ao apagar produto com código=[" + String.valueOf(code) + "]. Produto não encontrado!");
			
			throw new WebApplicationException(Status.NOT_FOUND);			
		}
	}
	
	private boolean checkIfCodeExist (Product product) {		
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Filter codeFilter = new FilterPredicate("Code", FilterOperator.EQUAL, product.getCode());
		
		Query query = new Query("Products").setFilter(codeFilter);
		Entity productEntity = datastore.prepare(query).asSingleEntity();
	
		if (productEntity == null) {
			return false;
		} else {
			if (productEntity.getKey().getId() == product.getId()) {
				//está alterando o mesmo produto
				return false;
			} else {				
				return true;
			}				
		}
	}
	
	public static Product entityToProduct (Entity productEntity) {
		Product product = new Product();
		product.setId(productEntity.getKey().getId());
		product.setProductID((String) productEntity.getProperty("ProductID"));
		product.setName((String) productEntity.getProperty("Name"));
		product.setCode(Integer.parseInt(productEntity.getProperty("Code")
				.toString()));
		product.setModel((String) productEntity.getProperty("Model"));
		product.setPrice(Float.parseFloat(productEntity.getProperty("Price")
				.toString()));		
		return product;
	}
	
	private void productToEntity (Product product, Entity productEntity) {
		productEntity.setProperty("ProductID", product.getProductID());
		productEntity.setProperty("Name", product.getName());
		productEntity.setProperty("Code", product.getCode());
		productEntity.setProperty("Model", product.getModel());
		productEntity.setProperty("Price", product.getPrice());
	}
	
}