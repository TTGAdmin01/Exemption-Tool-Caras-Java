package com.ttg.certificate_upload.beans;

import java.util.ArrayList;
import java.util.Date;

/**
 * Title: CertCaptureCertificateBean
 * 
 * Description: This class holds data for a cert capture certificate.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CertCaptureCertificateBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
public class CertCaptureCertificateBean {

    private Integer id; // certificate ID
    private String client_id; // company ID
    private Date signed_date;
    private Date expiration_date;
    private String filename;
    private Boolean never_renew;
    private Boolean renewable;
    private String replacement;
    private String created;
    private String modified;

    private Boolean valid;
    private Boolean verified;
    private Boolean submit_to_stack;
    private String certificate_number;
    private String verification_number;
    private Boolean unused_multi_cert;
    private String exmpt_percent;
    private Boolean barcode_read;
    private String tax_number;
    private Boolean is_single;
    private String legacy_certificate_id;
    private String calc_id;
    private String tax_number_type;
    private String business_number;
    private String business_number_type;
    private String exempt_reason_description;
    // private String sst_metadata; // We don't use it and sometimes comes in as array of name/value
    private Integer page_count;
    private String communication_id;
    private String location_id;
    private String anonymous_customer_id;
    private Boolean s3_document_exists;
    private CertCaptureStatusBean status = new CertCaptureStatusBean();
    private CertCaptureExposureZoneBean exposure_zone = new CertCaptureExposureZoneBean();
    private ArrayList<String> attributes = new ArrayList();
    private CertCaptureTaxCodeBean expected_tax_code = new CertCaptureTaxCodeBean();
    private CertCaptureTaxCodeBean actual_tax_code;
    private CertCaptureDocumentTypeBean document_type;


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

    public Date getSigned_date() {
        return signed_date;
    }

    public void setSigned_date(Date signed_date) {
        this.signed_date = signed_date;
    }

    public Date getExpiration_date() {
        return expiration_date;
    }

    public void setExpiration_date(Date expiration_date) {
        this.expiration_date = expiration_date;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean isNever_renew() {
        return never_renew;
    }

    public void setNever_renew(Boolean never_renew) {
        this.never_renew = never_renew;
    }

    public Boolean isRenewable() {
        return renewable;
    }

    public void setRenewable(Boolean renewable) {
        this.renewable = renewable;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
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

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Boolean isVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getCertificate_number() {
        return certificate_number;
    }

    public void setCertificate_number(String certificate_number) {
        this.certificate_number = certificate_number;
    }

    public String getVerification_number() {
        return verification_number;
    }

    public void setVerification_number(String verification_number) {
        this.verification_number = verification_number;
    }

    public Boolean isUnused_multi_cert() {
        return unused_multi_cert;
    }

    public void setUnused_multi_cert(Boolean unused_multi_cert) {
        this.unused_multi_cert = unused_multi_cert;
    }

    public String getExmpt_percent() {
        return exmpt_percent;
    }

    public void setExmpt_percent(String exmpt_percent) {
        this.exmpt_percent = exmpt_percent;
    }

    public Boolean isBarcode_read() {
        return barcode_read;
    }

    public void setBarcode_read(Boolean barcode_read) {
        this.barcode_read = barcode_read;
    }

    public String getTax_number() {
        return tax_number;
    }

    public void setTax_number(String tax_number) {
        this.tax_number = tax_number;
    }

    public Boolean isIs_single() {
        return is_single;
    }

    public void setIs_single(Boolean is_single) {
        this.is_single = is_single;
    }

    public String getLegacy_certificate_id() {
        return legacy_certificate_id;
    }

    public void setLegacy_certificate_id(String legacy_certificate_id) {
        this.legacy_certificate_id = legacy_certificate_id;
    }

    public String getCalc_id() {
        return calc_id;
    }

    public void setCalc_id(String calc_id) {
        this.calc_id = calc_id;
    }

    public String getTax_number_type() {
        return tax_number_type;
    }

    public void setTax_number_type(String tax_number_type) {
        this.tax_number_type = tax_number_type;
    }

    public String getBusiness_number() {
        return business_number;
    }

    public void setBusiness_number(String business_number) {
        this.business_number = business_number;
    }

    public String getBusiness_number_type() {
        return business_number_type;
    }

    public void setBusiness_number_type(String business_number_type) {
        this.business_number_type = business_number_type;
    }

    public String getExempt_reason_description() {
        return exempt_reason_description;
    }

    public void setExempt_reason_description(String exempt_reason_description) {
        this.exempt_reason_description = exempt_reason_description;
    }

    /*
	public String getSst_metadata() {
		return sst_metadata;
	}
	public void setSst_metadata(String sst_metadata) {
		this.sst_metadata = sst_metadata;
	}
     */
    public Integer getPage_count() {
        return page_count;
    }

    public void setPage_count(Integer page_count) {
        this.page_count = page_count;
    }

    public String getCommunication_id() {
        return communication_id;
    }

    public void setCommunication_id(String communication_id) {
        this.communication_id = communication_id;
    }

    public String getLocation_id() {
        return location_id;
    }

    public void setLocation_id(String location_id) {
        this.location_id = location_id;
    }

    public String getAnonymous_customer_id() {
        return anonymous_customer_id;
    }

    public void setAnonymous_customer_id(String anonymous_customer_id) {
        this.anonymous_customer_id = anonymous_customer_id;
    }

    public Boolean isS3_document_exists() {
        return s3_document_exists;
    }

    public void setS3_document_exists(Boolean s3_document_exists) {
        this.s3_document_exists = s3_document_exists;
    }

    public CertCaptureStatusBean getStatus() {
        return status;
    }

    public void setStatus(CertCaptureStatusBean status) {
        this.status = status;
    }

    public CertCaptureExposureZoneBean getExposure_zone() {
        return exposure_zone;
    }

    public void setExposure_zone(CertCaptureExposureZoneBean exposure_zone) {
        this.exposure_zone = exposure_zone;
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<String> attributes) {
        this.attributes = attributes;
    }

    public CertCaptureTaxCodeBean getExpected_tax_code() {
        return expected_tax_code;
    }

    public void setExpected_tax_code(CertCaptureTaxCodeBean expected_tax_code) {
        this.expected_tax_code = expected_tax_code;
    }

    public CertCaptureTaxCodeBean getActual_tax_code() {
        return actual_tax_code;
    }

    public void setActual_tax_code(CertCaptureTaxCodeBean actual_tax_code) {
        this.actual_tax_code = actual_tax_code;
    }

    public CertCaptureDocumentTypeBean getDocument_type() {
        return document_type;
    }

    public void setDocument_type(CertCaptureDocumentTypeBean document_type) {
        this.document_type = document_type;
    }

    public Boolean getSubmit_to_stack() {
        return submit_to_stack;
    }

    public void setSubmit_to_stack(Boolean submit_to_stack) {
        this.submit_to_stack = submit_to_stack;
    }


}
