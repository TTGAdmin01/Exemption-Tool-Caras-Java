package com.ttg.certificate_upload.beans;

import java.util.ArrayList;

/**
 * Title: CertCaptureExposureZoneBean
 * 
 * Description: This class holds data for a cert capture certificate exposure zone.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureExposureZoneBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureExposureZoneBean {
	
	private Integer id; // zone ID
	private String name;
	private String description;
	private Boolean auto_ship_to;
	private String tag;
	private CertCaptureStateBean state;
	private CertCaptureCountryBean country;
	private CertCaptureDocumentTypeBean document_type;
	private ArrayList<String> attributes;
	
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Boolean isAuto_ship_to() {
		return auto_ship_to;
	}
	public void setAuto_ship_to(Boolean auto_ship_to) {
		this.auto_ship_to = auto_ship_to;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public CertCaptureStateBean getState() {
		return state;
	}
	public void setState(CertCaptureStateBean state) {
		this.state = state;
	}
	public CertCaptureCountryBean getCountry() {
		return country;
	}
	public void setCountry(CertCaptureCountryBean country) {
		this.country = country;
	}
	public CertCaptureDocumentTypeBean getDocument_type() {
		return document_type;
	}
	public void setDocument_type(CertCaptureDocumentTypeBean document_type) {
		this.document_type = document_type;
	}
	public ArrayList<String> getAttributes() {
		return attributes;
	}
	public void setAttributes(ArrayList<String> attributes) {
		this.attributes = attributes;
	}

	
}
