/**
 * CertCaptureCompanyBean.java
 *
 * Description:  This class handles the CertCapture company information.
 *
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CertCaptureCompanyBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
package com.ttg.certificate_upload.beans;


public class CertCaptureCompanyBean {
    
    private String name;
    private String sandboxClientId;
    private String productionClientId;
    
    public CertCaptureCompanyBean(String name, String sandboxClientId, String productionClientId) {
        this.name = name;
        this.sandboxClientId = sandboxClientId;
        this.productionClientId = productionClientId;
        
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSandboxClientId() {
        return sandboxClientId;
    }

    public void setSandboxClientId(String sandboxClientId) {
        this.sandboxClientId = sandboxClientId;
    }

    public String getProductionClientId() {
        return productionClientId;
    }

    public void setProductionClientId(String productionClientId) {
        this.productionClientId = productionClientId;
    }
    
    
}
