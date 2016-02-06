package com.siecola.exemplo1.models;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class Product implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private long id;
	
	@NotNull
	private String productID;
	
	@NotNull
	private String name;
	
	@NotNull
	private String model;
	
	private int code;
	private float price;

	//getters and setters

	public String getProductID() {
		return productID;
	}
	public void setProductID(String productID) {
		this.productID = productID;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		this.price = price;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
