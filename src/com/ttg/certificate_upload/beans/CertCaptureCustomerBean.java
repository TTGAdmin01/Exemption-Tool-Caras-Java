package com.ttg.certificate_upload.beans;

/**
 * Title: CertCaptureCustomerBean
 * 
 * Description: This class holds data for a cert capture customer.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 * 
 * @author Cara Brauner
 * 
 * $Log: CertCaptureCustomerBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureCustomerBean {
	
	private Integer id; // customer ID
	private String client_id;  // CertCapture company code
	private String customer_number; // CDH party number
	private String alternate_id; // ERP customer numbers, comma delimited
	private String name;
	private String attn_name;
	private String address_line1;
	private String address_line2;
	private String city;
	private String zip;
	private String phone_number;
	private String fax_number;
	private String email_address;
	private String contact_name;
	private String last_transaction;
	private String created;
	private String modified;
	private boolean is_bill;
	private boolean is_ship;
	private String fein_number;
	private String location_id;
	private String welcome_letter_status;
	private boolean explicit;
	private boolean direct;
	private CertCaptureCountryBean country = new CertCaptureCountryBean();
	private CertCaptureStateBean state = new CertCaptureStateBean();
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getClient_id() {
		return client_id;
	}
	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}
	public String getCustomer_number() {
		return customer_number;
	}
	public void setCustomer_number(String customer_number) {
		this.customer_number = customer_number;
	}
	public String getAlternate_id() {
		return alternate_id;
	}
	public void setAlternate_id(String alternate_id) {
		this.alternate_id = alternate_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAttn_name() {
		return attn_name;
	}
	public void setAttn_name(String attn_name) {
		this.attn_name = attn_name;
	}
	public String getAddress_line1() {
		return address_line1;
	}
	public void setAddress_line1(String address_line1) {
		this.address_line1 = address_line1;
	}
	public String getAddress_line2() {
		return address_line2;
	}
	public void setAddress_line2(String address_line2) {
		this.address_line2 = address_line2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public String getPhone_number() {
		return phone_number;
	}
	public void setPhone_number(String phone_number) {
		this.phone_number = phone_number;
	}
	public String getFax_number() {
		return fax_number;
	}
	public void setFax_number(String fax_number) {
		this.fax_number = fax_number;
	}
	public String getEmail_address() {
		return email_address;
	}
	public void setEmail_address(String email_address) {
		this.email_address = email_address;
	}
	public String getContact_name() {
		return contact_name;
	}
	public void setContact_name(String contact_name) {
		this.contact_name = contact_name;
	}
	public String getLast_transaction() {
		return last_transaction;
	}
	public void setLast_transaction(String last_transaction) {
		this.last_transaction = last_transaction;
	}
	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getModified() {
		return modified;
	}
	public void setModified(String modified) {
		this.modified = modified;
	}
	public boolean isIs_bill() {
		return is_bill;
	}
	public void setIs_bill(boolean is_bill) {
		this.is_bill = is_bill;
	}
	public boolean isIs_ship() {
		return is_ship;
	}
	public void setIs_ship(boolean is_ship) {
		this.is_ship = is_ship;
	}
	public String getFein_number() {
		return fein_number;
	}
	public void setFein_number(String fein_number) {
		this.fein_number = fein_number;
	}
	public String getLocation_id() {
		return location_id;
	}
	public void setLocation_id(String location_id) {
		this.location_id = location_id;
	}
	public String getWelcome_letter_status() {
		return welcome_letter_status;
	}
	public void setWelcome_letter_status(String welcome_letter_status) {
		this.welcome_letter_status = welcome_letter_status;
	}
	public boolean isExplicit() {
		return explicit;
	}
	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}
	public boolean isDirect() {
		return direct;
	}
	public void setDirect(boolean direct) {
		this.direct = direct;
	}
	public CertCaptureCountryBean getCountry() {
		return country;
	}
	public void setCountry(CertCaptureCountryBean country) {
		this.country = country;
	}
	public CertCaptureStateBean getState() {
		return state;
	}
	public void setState(CertCaptureStateBean state) {
		this.state = state;
	}
	

	


}
