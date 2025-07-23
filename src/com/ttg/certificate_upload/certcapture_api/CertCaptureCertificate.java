package com.ttg.certificate_upload.certcapture_api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ttg.certificate_upload.beans.CertCaptureCertificateArrayBean;
import com.ttg.certificate_upload.beans.CertCaptureCertificateBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerBean;
import com.ttg.certificate_upload.beans.CertCaptureCertificateCustomFieldsBean;
import com.ttg.certificate_upload.beans.CertCaptureCertificatePONumberBean;
import com.ttg.certificate_upload.beans.CertCaptureTaxCodeArrayBean;
import com.ttg.certificate_upload.beans.CertCaptureTaxCodeBean;
import com.ttg.certificate_upload.utils.StringUtils;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;

/**
 * Title: CertCaptureCertificate
 *
 * Description: This class gets certificate information from CertCapture
 *
 * Copyright: Copyright (c) 2021
 *
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CertCaptureCertificate.java,v $
 * Revision 1.4  2022/08/05 23:37:36  carab
 * Delete Certificate, ShipToStates
 *
 * Revision 1.3  2022/01/24 23:49:36  carab
 * Add Sold To Customer custom field
 *
 * Revision 1.2  2021/12/28 22:16:24  carab
 * Log4j vulnerability fix, add COMPLETE and PENDING certs, enable USA as cert states
 *
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 *
 */
public class CertCaptureCertificate extends CertCaptureAuthenticate {

    private Logger log = LogManager.getLogger(getClass());

    private String certCaptureUrl;

    private HashMap<String, String> usStates = new HashMap();
    private HashMap<String, String> canadaProvinces = new HashMap();

    /*
	 * Sets up authentication token
     */
    public CertCaptureCertificate(String certCaptureUrl, String username, String password) {
        super(username, password);
        setCertCaptureUrl(certCaptureUrl);

        initHashmaps();
    }

    /**
     * Get CertCaptureCertificate information from CertCapture
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public CertCaptureCertificateBean getCertificate(String companyId, String certificateId) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCertificateBean certificate = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        log.debug("getCertificate(): looking up certificate for companyId=" + companyId + " and certificateId=" + certificateId);
        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId;
            log.debug("getCertificate(): fullUrl=" + fullUrl);
            log.debug("getCertificate(): x-client-id=" + companyId);
            log.debug("getCertificate(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("getCertificate(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a customer or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                /* not-found response looks like this:
				 * {
					"success": false,
					"code": 40,
					"error": "Unable to find any results for CertCaptureCustomer"
					}
                 */
                throw new Exception("getCertificate(): CertCapture returned " + writer.toString());
            } else {
                certificate = objectMapper.readValue(response, CertCaptureCertificateBean.class);
            }
        } catch (IOException ioe) {
             log.error("getCertificate(): IOException calling CertCapture to get certificate information. " + ioe.getMessage(), ioe);
            throw new IOException("getCertificate(): IOException calling CertCapture to get certificate information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCertificate(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return certificate;

    }

    /**
     * Get a list of certificates for the given customer and certificate number
     *
     * @param companyId
     * @param customerId
     * @param exempt Reason
     * @param state
     * @param startDate
     * @return
     */
    public List<CertCaptureCertificateBean> getCertificates(String companyId, Integer customerId, String exemptReason, String state, Date startDate) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCertificateBean> certificates = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String filter = "[[\"customers.id\",\"=\",\"" + customerId + "\"]";

            // This parameter doesn't seem to work
            /*
            if (!StringUtils.isEmpty(taxNumber)) {

                filter += ",[\"certificate_number\",\"=\",\"" + taxNumber + "\"]";
            }
             */
            if (!StringUtils.isEmpty(state)) {
                filter += ",[\"exposure_zone.name\",\"=\",\"" + convertStateName(state) + "\"]";
            }
            if (startDate != null) {
                filter += ",[\"signed_date\",\"=\",\"" + DateUtils.formatDate(startDate, "yyyy-MM-dd") + "\"]";
            }
            if (!StringUtils.isEmpty(exemptReason)) {
                filter += ",[\"expected_tax_code.name\",\"=\",\"" + exemptReason.toUpperCase()+ "\"]";
            }
            filter += "]";

            log.debug("getCertificates(): filter=" + filter);

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/?filter="
                    + URLEncoder.encode(filter, "UTF-8");
            log.debug("getCertificates(): fullUrl=" + fullUrl);
            log.debug("getCertificates(): x-client-id=" + companyId);
            log.debug("getCertificates(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("getCertificates(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a custom field or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("getCertificates(): CertCapture returned " + writer.toString());
                }
            } else {
                CertCaptureCertificateArrayBean certificateArrayBean = objectMapper.readValue(response, CertCaptureCertificateArrayBean.class);
                certificates = certificateArrayBean.getData();
                log.debug("getCertificates(): found " + (certificates == null ? "null" : certificates.size()) + " certificates.");
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("getCertificates(): IOException calling CertCapture to get certificates for a customer. " + ioe.getMessage(), ioe);
            throw new IOException("getCertificates(): IOException calling CertCapture to get certificates for a customer. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCertificates(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return certificates;
    }

    /**
     * Get the custom fields for a certificate
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCertificateCustomFieldsBean> getCustomFields(String companyId, String certificateId) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCertificateCustomFieldsBean> customFields = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId + "/custom-fields";
            log.debug("getCustomFields(): fullUrl=" + fullUrl);
            log.debug("getCustomFields(): x-client-id=" + companyId);
            log.debug("getCustomFields(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("getCustomFields(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a custom field or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("getCustomFields(): CertCapture returned " + writer.toString());
                }
            } else {
                customFields = objectMapper.readValue(response, new TypeReference<List<CertCaptureCertificateCustomFieldsBean>>() {
                });
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("getCustomFields(): IOException calling CertCapture to get certificate custom-fields information. " + ioe.getMessage(), ioe);
            throw new IOException("getCustomFields(): IOException calling CertCapture to get certificate custom-fields information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCustomFields(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customFields;

    }

    /**
     * Convenience method
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCertificateCustomFieldsBean> getCustomFields(String companyId, Integer certificateId) throws Exception {
        return getCustomFields(companyId, certificateId.toString());
    }

    /**
     * Get the customers that are associated with a certificate
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerBean> getCertificateCustomers(String companyId, String certificateId) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCustomerBean> customers = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId + "/customers";
            log.debug("getCertificateCustomers(): fullUrl=" + fullUrl);
            log.debug("getCertificateCustomers(): x-client-id=" + companyId);
            log.debug("getCertificateCustomers(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("getCertificateCustomers(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a custom field or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("getCertificateCustomers(): CertCapture returned " + writer.toString());
                }
            } else {
                customers = objectMapper.readValue(response, new TypeReference<List<CertCaptureCustomerBean>>() {
                });
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("getCertificateCustomers(): IOException calling CertCapture to get certificate customers information. " + ioe.getMessage(), ioe);
            throw new IOException("getCertificateCustomers(): IOException calling CertCapture to get certificate customers information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCertificateCustomers(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customers;
    }

    /**
     * Convenience function for getCertificateCustomers using Integer instead of
     * String
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerBean> getCertificateCustomers(Integer companyId, Integer certificateId) throws Exception {
        return getCertificateCustomers(companyId.toString(), certificateId.toString());
    }

    /**
     * Convenience function for getCertificateCustomers using Integer instead of
     * String
     *
     * @param companyId
     * @param certificateId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerBean> getCertificateCustomers(String companyId, Integer certificateId) throws Exception {
        return getCertificateCustomers(companyId, certificateId.toString());
    }

    public List<CertCaptureCertificatePONumberBean> getCertificatePONumbers(String companyId, String certificateId) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCertificatePONumberBean> poNumbers = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId + "/po-numbers";
            log.debug("getCertificatePONumbers(): fullUrl=" + fullUrl);
            log.debug("getCertificatePONumbers(): x-client-id=" + companyId);
            log.debug("getCertificatePONumbers(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("getCertificatePONumbers(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a custom field or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("getCertificatePONumbers(): CertCapture returned " + writer.toString());
                }
            } else {
                poNumbers = objectMapper.readValue(response, new TypeReference<List<CertCaptureCertificatePONumberBean>>() {
                });
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("getCertificatePONumbers(): IOException calling CertCapture to get certificate customers information. " + ioe.getMessage(), ioe);
            throw new IOException("getCertificatePONumbers(): IOException calling CertCapture to get certificate customers information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCertificatePONumbers(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return poNumbers;
    }

    public CertCaptureCertificateBean addCertificate(CertCaptureCertificateBean certificate, String base64Image) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCertificateBean certificateOut = null;

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        try {
            /* format is URL form encoded
            ** POST https://api.certcapture.com/v2/certificates
            ** HEADERS:
            ** x-client-id:12345
            ** Authorization:Basic MYAUTHSTRING=
            ** Content-Type:application/x-www-form-urlencoded
            **
            ** BODY:
            ** signed_date=1989-01-09
            ** &expiration_date=2020-08-01
            ** &valid=true
            ** &verified=true
            ** &never_renew=true
            ** &renewable=true
            ** &unused_multi_cert=true
            ** &exmpt_percent=1
            ** &verification_number=347236
            ** &tax_number=84761363
            ** &expected_tax_code={"id":0,"name":"RESALE","tag":"enim"}
            ** &exposureZone={"id":8,"name":"New York","tag":"natus"}
            ** &status={"name":"COMPLETE"}
            ** &pdf=NzdmMWNhOGEtNDcxMC0zZGM5LTkxNTMtYTMxMTgzYWFkNjU1LmpwZw==
             */
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates";
            log.debug("addCertificate(): fullUrl=" + fullUrl);
            log.debug("addCertificate(): x-client-id=" + certificate.getClient_id());
            log.debug("addCertificate(): Authorization=Basic " + getToken());

            // Add form data.  
            /*
            List<NameValuePair> formData = Form.form()
                    .add("expected_tax_code", "{\"name\":\"" + certificate.getExpected_tax_code().getName() + "\"}")
                    .add("exposure_zone", "{\"name\":\"" + certificate.getExposure_zone().getName() + "\"}")
                    .add("status", "{\"name\":\"" + certificate.getStatus().getName() + "\"}")
                    .add("pdf", base64Image)
                    .build();
            */


            List<NameValuePair> formData = new ArrayList();
            if (certificate.getSigned_date() != null) {
                formData.add(new BasicNameValuePair("signed_date", DateUtils.formatDate(certificate.getSigned_date(), "yyyy-MM-dd")));
            }
            if (certificate.getExpiration_date() != null) {
                formData.add(new BasicNameValuePair("expiration_date", DateUtils.formatDate(certificate.getExpiration_date(), "yyyy-MM-dd")));
            }
            if (Boolean.TRUE.equals((certificate.isValid()))) {
                formData.add(new BasicNameValuePair("valid", "true"));
            }
            if (Boolean.FALSE.equals((certificate.isValid()))) {
                formData.add(new BasicNameValuePair("valid", "false"));
            }
            if (!StringUtils.isEmpty(certificate.getExpected_tax_code().getName())) {
                formData.add(new BasicNameValuePair("expected_tax_code", "{\"name\":\"" + certificate.getExpected_tax_code().getName() + "\"}"));
            }
            formData.add(new BasicNameValuePair("exposure_zone", "{\"name\":\"" + certificate.getExposure_zone().getName() + "\"}"));
            if (!StringUtils.isEmpty(certificate.getStatus().getName())) {
                formData.add(new BasicNameValuePair("status", "{\"name\":\"" + certificate.getStatus().getName() + "\"}"));
            }
            if (Boolean.TRUE.equals(certificate.getSubmit_to_stack())) {
                formData.add(new BasicNameValuePair("submit_to_stack", "\"true\""));
            }
            if (!StringUtils.isEmpty(certificate.getTax_number())) {
                formData.add(new BasicNameValuePair("tax_number", certificate.getTax_number()));
            }
            formData.add(new BasicNameValuePair("pdf", base64Image));
            
            
            for (NameValuePair nvPair : formData) {
                log.debug("addCertificate(): body of call name=[" + nvPair.getName() + "] value=[" + nvPair.getValue() + "]");
            }

            //bodyForm(Form.form().add("username", "vip").add("password", "secret").build())
            // Call CertCapture to add the customer information
            response = Request.post(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", certificate.getClient_id())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(formData)
                    .execute().returnContent().asString();

            /*
            // Create the JSON string of the certificate
            writer = new StringWriter();
            objectMapper.writeValue(writer, certificate);
            log.debug("addCertificate(): body of call");
            log.debug(writer.toString());

            response = Request.post(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", certificate.getClient_id())
                    .addHeader("Content-Type", "application/json")
                    .bodyString(writer.toString(), ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();
             */
            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("addCertificate(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a customer or it throws some kind of API error.  
            // 
            if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                /* not-found response looks like this:
				 * {
					"success": false,
					"code": 40,
					"error": "Unable to find any results for CertCaptureCustomer"
					}
                 */
                throw new Exception("addCertificate(): CertCapture returned " + writer.toString());
            } else {
                certificateOut = objectMapper.readValue(response, CertCaptureCertificateBean.class);
            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCertificate(): IOException calling CertCapture to add certificate. " + ioe.getMessage(), ioe);
            throw new IOException("addCertificate(): IOException calling CertCapture to add certificate. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCertificate(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return certificateOut;
    }

    /**
     * Update a certificate
     *
     * @param certificate
     * @return
     * @throws Exception
     */
    public String updateCertificate(CertCaptureCertificateBean certificate) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCertificateBean certificateOut = null;

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        try {
            /* format is URL form encoded
            ** PUT https://api.certcapture.com/v2/certificates/id
            ** HEADERS:
            ** x-client-id:12345
            ** Authorization:Basic MYAUTHSTRING=
            ** Content-Type:application/x-www-form-urlencoded
            **
            ** BODY:
            ** signed_date=1992-03-13
            ** &expiration_date=2007-03-23
            ** &valid=false
            ** &verified=false
            ** &never_renew=false
            ** &renewable=false
            ** &unused_multi_cert=true
            ** &exmpt_percent=1
            ** &verification_number=477188
            ** &tax_number=44244388
            ** &expected_tax_code={"id":5,"name":"RESALE","tag":"voluptatem"}
            ** &exposureZone={"id":2,"name":"West Virginia","tag":"incidunt"}
            ** &status={"name":"PENDING"}
            ** &pdf=YTIwNzViZWQtODMwMC0zZjE1LThhODYtMWI4NDg1MjljYmRhLnR4dA==
             */
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificate.getId();
            log.debug("updateCertificate(): fullUrl=" + fullUrl);
            log.debug("updateCertificate(): x-client-id=" + certificate.getClient_id());
            log.debug("updateCertificate(): Authorization=Basic " + getToken());

            // Add form data.  Default expiration date if one isn't present
            List<NameValuePair> formData = Form.form()
                    .add("signed_date", DateUtils.formatDate(certificate.getSigned_date(), "yyyy-MM-dd"))
                    .add("expiration_date", (certificate.getExpiration_date() == null ? "9999-12-31" : DateUtils.formatDate(certificate.getExpiration_date(), "yyyy-MM-dd")))
                    .add("valid", "true")
                    .add("tax_number", certificate.getTax_number())
                    .add("expected_tax_code", "{\"name\":\"" + certificate.getExpected_tax_code().getName() + "\"}")
                    .add("exposure_zone", "{\"name\":\"" + certificate.getExposure_zone().getName() + "\"}")
                    .add("status", "{\"name\":\"COMPLETE\"}")
                    //.add("pdf", base64Image)
                    .build();

            for (NameValuePair nvPair : formData) {
                log.debug("updateCertificate(): body of call name=[" + nvPair.getName() + "] value=[" + nvPair.getValue() + "]");
            }

            //bodyForm(Form.form().add("username", "vip").add("password", "secret").build())
            // Call CertCapture to add the customer information
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", certificate.getClient_id())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(formData)
                    .execute().returnContent().asString();

            /*
            // Create the JSON string of the certificate
            writer = new StringWriter();
            objectMapper.writeValue(writer, certificate);
            log.debug("addCertificate(): body of call");
            log.debug(writer.toString());

            response = Request.post(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", certificate.getClient_id())
                    .addHeader("Content-Type", "application/json")
                    .bodyString(writer.toString(), ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();
             */
            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("updateCertificate(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a customer or it throws some kind of API error.  
            // 
            if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                /* not-found response looks like this:
				 * {
					"success": false,
					"code": 40,
					"error": "Unable to find any results for CertCaptureCustomer"
					}
                 */
                throw new Exception("updateCertificate(): CertCapture returned " + writer.toString());
            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("updateCertificate(): IOException calling CertCapture to update certificate. " + ioe.getMessage(), ioe);
            throw new IOException("updateCertificate(): IOException calling CertCapture to update certificate. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("updateCertificate(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return response;
    }

    public String addCertificateCustomFields(String companyId, Integer certificateId, List<CertCaptureCertificateCustomFieldsBean> customFields) throws Exception {
        String response = null;
        StringWriter writer = null;

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        try {
            /* format is URL form encoded
            ** POST https://api.certcapture.com/v2/certificates/id/custom-fields
            ** HEADERS:
            ** x-client-id:12345
            ** Authorization:Basic MYAUTHSTRING=
            ** Content-Type:application/x-www-form-urlencoded
            **
            ** BODY:
BODY:
                ** custom_fields=        [
                **     {
                **         "field_name": "DPC Specific Exemption",
                **         "value": "12345"
                **     },
                **     {
                **         "field_name": "WESNET Ship-to Code",
                **         "value": "01"
                **     }
                ** ]custom_fields=[{ "field_name": "DPC Specific Exemption", "value": "12345" },{field_name}: "WESNET Ship-to Code", "value": "xx"}]
             */
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + +certificateId + "/custom-fields";
            log.debug("addCertificateCustomFields(): fullUrl=" + fullUrl);
            log.debug("addCertificateCustomFields(): x-client-id=" + companyId);
            log.debug("addCertificateCustomFields(): Authorization=Basic " + getToken());

            // Create the JSON string of the custom fields
            writer = new StringWriter();
            objectMapper.writeValue(writer, customFields);
            log.debug("addCertificateCustomFields(): body of call");
            log.debug("custom_fields=" + writer.toString());

            // Call CertCapture to add the certificate custom field information
            // Call CertCapture to get the customer information
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("custom_fields", writer.toString()).build())
                    .execute().returnContent().asString();

            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("addCertificate(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a customer or it throws some kind of API error.  
            // 
            if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                /* not-found response looks like this:
				 * {
					"success": false,
					"code": 40,
					"error": "Unable to find any results for CertCaptureCustomer"
					}
                 */
                throw new Exception("addCertificateCustomFields(): CertCapture returned " + writer.toString());
            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCertificateCustomFields(): IOException calling CertCapture to add certificate custom fields. " + ioe.getMessage(), ioe);
            throw new IOException("addCertificateCustomFields(): IOException calling CertCapture to add certificate custom fields. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCertificateCustomFields(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return response;
    }

    /**
     * Associate a customer with a certificate
     *
     * @param certificateId
     * @param customerId
     * @return
     * @throws Exception
     */
    public String addCertificateCustomer(String companyId, Integer certificateId, Integer customerId) throws Exception {
        String response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId + "/customers";
            log.debug("addCertificateCustomer(): fullUrl=" + fullUrl);
            log.debug("addCertificateCustomer(): x-client-id=" + companyId);
            log.debug("addCertificateCustomer(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form()
                            .add("customers", "[{\"id\":" + customerId + "}]")
                            .build())
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("addCertificateCustomer(): CertCapture response=" + response);

            
            // Convert the response to an object
            // Response is success: true or false
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null) {
                    success = responseObject.get("success").asText();
                }

                // Throw an error if we didn't get True back.
                if (!"true".equals(success)) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("addCertificateCustomer(): CertCapture returned " + writer.toString());
                }

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCertificateCustomer(): IOException calling CertCapture to add customer(" + customerId + ") to certificate(" + certificateId + "). " + ioe.getMessage(), ioe);
            throw new IOException("addCertificateCustomer(): IOException calling CertCapture to add customer(" + customerId + ") to certificate(" + certificateId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCertificateCustomer(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;
    }
    
     /**
     * Delete a customer from a certificate
     *
     * @param certificateId
     * @param customerId
     * @return
     * @throws Exception
     */
    public String deleteCertificateCustomer(String companyId, Integer certificateId, Integer customerId) throws Exception {
        HttpResponse response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId + "/customers";
            log.debug("deleteCertificateCustomer(): fullUrl=" + fullUrl);
            log.debug("deleteCertificateCustomer(): x-client-id=" + companyId);
            log.debug("deleteCertificateCustomer(): Authorization=Basic " + getToken());

            // Call CertCapture to delete the customer information
            response = Request.delete(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form()
                            .add("customers", "[{\"id\":" + customerId + "}]")
                            .build())
                    .execute().returnResponse();

            // Only returns a code 204=OK, 400=Not found
            log.debug("deleteCertificateCustomer(): CertCapture response=" + response.getCode());

            // Convert the response to an object
            // Response is success: true or false
            // 
            if (response.getCode() != 204) {
                    throw new Exception("deleteCertificateCustomer(): CertCapture returned code=" + response.getCode() + " reason=" + response.getReasonPhrase());

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("deleteCertificateCustomer(): IOException calling CertCapture to delete customer(" + customerId + ") from certificate(" + certificateId + "). " + ioe.getMessage(), ioe);
            throw new IOException("deleteCertificateCustomer(): IOException calling CertCapture to delete customer(" + customerId + ") from certificate(" + certificateId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("deleteCertificateCustomer(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;
    }
    
         /**
     * Delete a certificate
     *
     * @param certificateId
     * @param customerId
     * @return
     * @throws Exception
     */
    public String deleteCertificate(String companyId, Integer certificateId) throws Exception {
        String response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/certificates/" + certificateId;
            log.debug("deleteCertificate(): fullUrl=" + fullUrl);
            log.debug("deleteCertificate(): x-client-id=" + companyId);
            log.debug("deleteCertificate(): Authorization=Basic " + getToken());

            // Call CertCapture to delete the customer information
            response = Request.delete(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Only returns a code 204=OK, 400=Not found
            log.debug("deleteCertificate(): CertCapture response=" + response);

            // Not reading into JsonNode this time as this is an array.
            log.debug("deleteCertificate(): CertCapture response=" + response);

            
            // Convert the response to an object
            // Response is success: true or false
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null) {
                    success = responseObject.get("success").asText();
                }

                // Throw an error if we didn't get True back.
                if (!"true".equals(success)) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("deleteCertificate(): CertCapture returned " + writer.toString());
                }

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("deleteCertificate(): IOException calling CertCapture to delete certificate(" + certificateId + "). " + ioe.getMessage(), ioe);
            throw new IOException("deleteCertificate(): IOException calling CertCapture to delete (" + certificateId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("deleteCertificate(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;
    }

    /**
     * Get the official exempt reason information
     *
     * @param exemptReasonCode
     * @return
     */
    public List<CertCaptureTaxCodeBean> getExemptReasons(String companyId, String exemptReasonCode) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureTaxCodeBean> taxCodes = null;

        String exemptReason = exemptReasonCode;
        if (exemptReason != null) {
            exemptReason = exemptReason.trim();
        }

        if (StringUtils.isEmpty(exemptReason)) {
            return taxCodes;
        }

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* GET https://api.certcapture.com/v2/exempt-reasons?
        ** filter=["name","ilike","<reasoncode>"
        ** filter by the exempt reason we have to find the one to put on the certificate
             */
            String filter = "[[\"name\",\"ilike\",\"" + exemptReason + "\"]]";
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/exempt-reasons?filter="
                    + URLEncoder.encode(filter, "UTF-8");
            log.debug("getExemptReasons(): fullUrl=" + fullUrl);
            log.debug("getExemptReasons(): x-client-id=" + companyId);
            log.debug("getExemptReasons(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("getExemptReasons(): CertCapture response=" + response);

            // Convert the response to an object
            // We should always get a custom field or it throws some kind of API error.  We should not have a not-found response.
            // 
            if (!StringUtils.isEmpty(response) && response.contains("success")) {
                writer = new StringWriter();
                JsonNode responseObject = objectMapper.readTree(response);
                objectMapper.writeValue(writer, responseObject);

                if (responseObject.get("success") != null && "false".equals(responseObject.get("success").asText())) {
                    /* not-found response looks like this:
					 * {
						"success": false,
						"code": 40,
						"error": "Unable to find any results for CertCaptureCustomer"
						}
                     */
                    throw new Exception("getExemptReasons(): CertCapture returned " + writer.toString());
                }
            } else {
                CertCaptureTaxCodeArrayBean taxCodeArray = objectMapper.readValue(response, CertCaptureTaxCodeArrayBean.class);
                taxCodes = taxCodeArray.getData();
                log.debug("getExemptReasons(): found " + (taxCodeArray == null ? "null" : taxCodes.size()) + " taxCodes (exempt reasons).");
            }

        } catch (IOException ioe) {
            // Not found
            if (ioe.getMessage() != null && ioe.getMessage().contains("reason phrase: Not Found")) {
                // Ignore.  Not found.
            } else {
                // Errors related to calling CertCapture
                log.error("getExemptReasons(): IOException calling CertCapture to get exempt reason [" + exemptReasonCode + "] " + ioe.getMessage(), ioe);
                throw new IOException("getCertificates(): IOException calling CertCapture to get exempt reason [" + exemptReasonCode + "] " + ioe.getMessage(), ioe);
            }
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getExemptReasons(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }
        return taxCodes;
    }

    public String getCertCaptureUrl() {
        return certCaptureUrl;
    }

    public void setCertCaptureUrl(String certCaptureUrl) {
        this.certCaptureUrl = certCaptureUrl;
    }

    /**
     * Find the fully spelled state in the list.
     *
     * @param inState
     * @return
     */
    public String convertStateName(String inState) {

        // Assuming US/Canada here
        if (!StringUtils.isEmpty(inState)) {
            if (usStates.containsKey(inState)) {
                return usStates.get(inState);
            }
            if (canadaProvinces.containsKey(inState)) {
                return canadaProvinces.get(inState);
            }
        }

        return inState;
    }

    /**
     * Convenience functions for setting exposure zones.
     *
     * @param inState
     * @return
     */
    public boolean isCanadaProvince(String inState) {
        return canadaProvinces.containsKey(inState);
    }

    /**
     * Convenience functions for setting exposure zones.
     *
     * @param inState
     * @return
     */
    public boolean isUsState(String inState) {
        return usStates.containsKey(inState);
    }

    /**
     * Initialize USA/Canada state-abbr to state-name maps
     */
    private void initHashmaps() {
        usStates.put("AL", "Alabama");
        usStates.put("AK", "Alaska");
        usStates.put("AZ", "Arizona");
        usStates.put("AR", "Arkansas");
        usStates.put("CA", "California");
        usStates.put("CO", "Colorado");
        usStates.put("CT", "Connecticut");
        usStates.put("DE", "Delaware");
        usStates.put("FL", "Florida");
        usStates.put("GA", "Georgia");
        usStates.put("HI", "Hawaii");
        usStates.put("ID", "Idaho");
        usStates.put("IL", "Illinois");
        usStates.put("IN", "Indiana");
        usStates.put("IA", "Iowa");
        usStates.put("KS", "Kansas");
        usStates.put("KY", "Kentucky");
        usStates.put("LA", "Louisiana");
        usStates.put("ME", "Maine");
        usStates.put("MD", "Maryland");
        usStates.put("MA", "Massachusetts");
        usStates.put("MI", "Michigan");
        usStates.put("MN", "Minnesota");
        usStates.put("MS", "Mississippi");
        usStates.put("MO", "Missouri");
        usStates.put("MT", "Montana");
        usStates.put("NE", "Nebraska");
        usStates.put("NV", "Nevada");
        usStates.put("NH", "New Hampshire");
        usStates.put("NJ", "New Jersey");
        usStates.put("NM", "New Mexico");
        usStates.put("NY", "New York");
        usStates.put("NC", "North Carolina");
        usStates.put("ND", "North Dakota");
        usStates.put("OH", "Ohio");
        usStates.put("OK", "Oklahoma");
        usStates.put("OR", "Oregon");
        usStates.put("PA", "Pennsylvania");
        usStates.put("PR", "Puerto Rico");
        usStates.put("RI", "Rhode Island");
        usStates.put("SC", "South Carolina");
        usStates.put("SD", "South Dakota");
        usStates.put("TN", "Tennessee");
        usStates.put("TX", "Texas");
        usStates.put("UT", "Utah");
        usStates.put("VT", "Vermont");
        usStates.put("VA", "Virginia");
        usStates.put("WA", "Washington");
        usStates.put("WV", "West Virginia");
        usStates.put("WI", "Wisconsin");
        usStates.put("WY", "Wyoming");
        usStates.put("DC", "District Of Columbia");
        usStates.put("PR", "Puerto Rico");
        usStates.put("VI", "Virgin Islands");
        
        canadaProvinces.put("AB", "Alberta");
        canadaProvinces.put("BC", "British Columbia");
        canadaProvinces.put("MB", "Manitoba");
        canadaProvinces.put("NB", "New Brunswick");
        canadaProvinces.put("NF", "Newfoundland & Labrador");
        canadaProvinces.put("NT", "Northwest Territories");
        canadaProvinces.put("NS", "Nova Scotia");
        canadaProvinces.put("NU", "Nunavut");
        canadaProvinces.put("ON", "Ontario");
        canadaProvinces.put("PE", "Prince Edward Island");
        canadaProvinces.put("QC", "Quebec");
        canadaProvinces.put("SK", "Saskatchewan");
        canadaProvinces.put("YT", "Yukon");

    }
    
    
}
