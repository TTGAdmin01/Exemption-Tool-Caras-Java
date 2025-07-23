package com.ttg.certificate_upload.beans;

import java.util.ArrayList;

/**
 * Title: CertCaptureCustomerCustomFieldsBean
 * 
 * Description: This class holds data for a cert capture customer custom fields.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureCustomerCustomFieldsBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureCustomerCustomFieldsBean {

	private Integer id; // customer ID
	private String client_id; // company ID
	private String field_name;
	private String type;
	private Boolean uses_gencert;
	private Boolean system_code;
	private Boolean gencert_lock;
	private Boolean is_editable;
	private String value;
	
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
	public String getField_name() {
		return field_name;
	}
	public void setField_name(String field_name) {
		this.field_name = field_name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean isUses_gencert() {
		return uses_gencert;
	}
	public void setUses_gencert(Boolean uses_gencert) {
		this.uses_gencert = uses_gencert;
	}
	public Boolean isSystem_code() {
		return system_code;
	}
	public void setSystem_code(Boolean system_code) {
		this.system_code = system_code;
	}
	public Boolean isGencert_lock() {
		return gencert_lock;
	}
	public void setGencert_lock(Boolean gencert_lock) {
		this.gencert_lock = gencert_lock;
	}
	public Boolean isIs_editable() {
		return is_editable;
	}
	public void setIs_editable(Boolean is_editable) {
		this.is_editable = is_editable;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	

	


}
