package com.ttg.certificate_upload.beans;

import java.util.ArrayList;

/**
 * Title: CertCaptureCertificateCustomFieldsBean
 * 
 * Description: This class holds data for a cert capture certificate custom fields.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureCertificateCustomFieldsBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureCertificateCustomFieldsBean {

	private Integer id; // customer ID
	private Integer client_id; // company ID
	private String field_name;
	private String type;
	private String[] possible_values;
	private boolean uses_data_entry;
	private boolean required_in_data_entry;
	private String legacy_field;
	private String legacy_field_internal_name;
	private Integer document_type_id;
	private boolean uses_gencert;
	private String value;
	
	public Integer getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Integer getClient_id() {
		return client_id;
	}
	public void setClient_id(int client_id) {
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
	public String[] getPossible_values() {
		return possible_values;
	}
	public void setPossible_values(String[] possible_values) {
		this.possible_values = possible_values;
	}
	public boolean isUses_data_entry() {
		return uses_data_entry;
	}
	public void setUses_data_entry(boolean uses_data_entry) {
		this.uses_data_entry = uses_data_entry;
	}
	public boolean isRequired_in_data_entry() {
		return required_in_data_entry;
	}
	public void setRequired_in_data_entry(boolean required_in_data_entry) {
		this.required_in_data_entry = required_in_data_entry;
	}
	public String getLegacy_field() {
		return legacy_field;
	}
	public void setLegacy_field(String legacy_field) {
		this.legacy_field = legacy_field;
	}
	public String getLegacy_field_internal_name() {
		return legacy_field_internal_name;
	}
	public void setLegacy_field_internal_name(String legacy_field_internal_name) {
		this.legacy_field_internal_name = legacy_field_internal_name;
	}
	public Integer getDocument_type_id() {
		return document_type_id;
	}
	public void setDocument_type_id(int document_type_id) {
		this.document_type_id = document_type_id;
	}
	public boolean isUses_gencert() {
		return uses_gencert;
	}
	public void setUses_gencert(boolean uses_gencert) {
		this.uses_gencert = uses_gencert;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
