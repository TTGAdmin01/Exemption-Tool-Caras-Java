package com.ttg.certificate_upload.beans;

import java.util.ArrayList;

/**
 * Title: CertCaptureCertificatePONumberBean
 * 
 * Description: This class holds data for a cert capture certificate PO Number.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 * 
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureCertificatePONumberBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureCertificatePONumberBean {

	private int id; // PO Number ID
	private int certificate_id; // certificate ID
	private String po_number;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getCertificate_id() {
		return certificate_id;
	}
	public void setCertificate_id(int certificate_id) {
		this.certificate_id = certificate_id;
	}
	public String getPo_number() {
		return po_number;
	}
	public void setPo_number(String po_number) {
		this.po_number = po_number;
	}
	

	
}
