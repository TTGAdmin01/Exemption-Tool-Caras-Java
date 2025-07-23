/**
 * 
 */
package com.ttg.certificate_upload.certcapture_api;

import java.util.Base64;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


/**
 * Title: CertCaptureAuthenticate
 * 
 * Description: This class creates the authentication token for CertCapture
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureAuthenticate.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 *
 */

public class CertCaptureAuthenticate {
	private Logger log = LogManager.getLogger(getClass());;

	protected String authenticationToken;
	
	public CertCaptureAuthenticate(String username, String password) {
		setToken(username, password);
	}
	
	/*
	 * Set the base64 token for authentication with CertCapture.
	 * Basic base64(username + ':' + password)
	 */
	public void  setToken(String username, String password) {
		
		String unencoded = username + ":" + password;
		authenticationToken =  Base64.getEncoder().encodeToString(unencoded.getBytes());

	}

	public String getToken() {
		return authenticationToken;
	}

}
