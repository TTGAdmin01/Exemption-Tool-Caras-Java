package com.ttg.certificate_upload.beans;

import java.util.ArrayList;

/**
 * Title: CertCaptureCustomerExposureZoneBean
 *
 * Description: This class holds data an exposure zone to be added to a customer
 *
 * Copyright: Copyright (c) 2021
 *
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CertCaptureCustomerExposureZoneBean.java,v $
 * Revision 1.2  2022/08/11 22:40:04  carab
 * Add initials to exposure zone, encode customer-number for search
 * Revision 1.1.1.1
 * 2021/10/20 22:24:42 carab Imported sources
 *
 *
 */
public class CertCaptureCustomerExposureZoneBean {

    private String name;
    private String initials;

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
