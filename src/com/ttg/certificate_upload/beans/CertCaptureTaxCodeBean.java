package com.ttg.certificate_upload.beans;

/**
 * Title: CertCaptureTaxCodeBean
 * 
 * Description: This class holds data for a cert capture certificate tax code.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureTaxCodeBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureTaxCodeBean {
	
	private Integer id; // document type ID
	private String name;
	private String tag;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String description) {
		this.tag = description;
	}
	
	
}
