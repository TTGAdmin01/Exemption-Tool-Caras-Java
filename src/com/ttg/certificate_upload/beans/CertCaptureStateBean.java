package com.ttg.certificate_upload.beans;

/**
 * Title: CertCaptureStateBean
 * 
 * Description: This class holds data for a cert capture US state.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureStateBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureStateBean {
	
	private Integer id;
	private String name;
	private String initials;
	private Integer geocode;
	
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
	public String getInitials() {
		return initials;
	}
	public void setInitials(String initials) {
		this.initials = initials;
	}
	public Integer getGeocode() {
		return geocode;
	}
	public void setGeocode(Integer geocode) {
		this.geocode = geocode;
	}
	
	

}
