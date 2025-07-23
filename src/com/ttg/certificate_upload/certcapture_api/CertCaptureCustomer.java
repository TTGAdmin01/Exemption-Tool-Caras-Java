package com.ttg.certificate_upload.certcapture_api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ttg.certificate_upload.beans.CertCaptureCustomerBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerCustomFieldsBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerExposureZoneBean;
import com.ttg.certificate_upload.utils.StringUtils;
import java.net.URLEncoder;

import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;

/**
 *
 * Title: CertCaptureCustomer
 *
 * Description: This class gets customer information from CertCapture
 *
 * Copyright: Copyright (c) 2021
 *
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CertCaptureCustomer.java,v $
 * Revision 1.7  2022/09/20 22:23:03  carab
 * Add ability to remove local exposure zones
 *
 * Revision 1.6  2022/08/11 22:40:04  carab
 * Add initials to exposure zone, encode customer-number for search
 *
 * Revision 1.5  2022/08/05 23:37:36  carab
 * Delete Certificate, ShipToStates
 *
 * Revision 1.4  2022/05/09 23:54:29  carab
 * Add program to link existing customers with certificates
 *
 * Revision 1.3  2022/02/09 18:02:59  carab
 * Add customer attribute
 *
 * Revision 1.2  2022/01/24 23:49:36  carab
 * Add Sold To Customer custom field
 *
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 *
 */
public class CertCaptureCustomer extends CertCaptureAuthenticate {

    private Logger log = LogManager.getLogger(getClass());

    private String certCaptureUrl;

    /*
	 * Sets up authentication token
     */
    public CertCaptureCustomer(String certCaptureUrl, String username, String password) {
        super(username, password);
        setCertCaptureUrl(certCaptureUrl);
    }

    /**
     * Get CertCaptureCustomer information from CertCapture
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public CertCaptureCustomerBean getCustomer(String companyId, String customerId) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCustomerBean customer = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        log.debug("getCustomer(): looking up customer for companyId=" + companyId + " and customerId=" + customerId);
        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId;
            log.debug("getCustomer(): fullUrl=" + fullUrl);
            log.debug("getCustomer(): x-client-id=" + companyId);
            log.debug("getCustomer(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .execute().returnContent().asString();

            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("getCustomer(): CertCapture response=" + response);

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
                throw new Exception("getCustomer(): CertCapture returned " + writer.toString());
            } else {
                customer = objectMapper.readValue(response, CertCaptureCustomerBean.class);
            }
        } catch (IOException ioe) {
            if (ioe.getMessage() != null && ioe.getMessage().contains("status code: 404, reason phrase: Not Found")) {
                // Customer not found.  This is OK
            } else {
                // Errors related to calling CertCapture
                log.error("getCustomer(): IOException calling CertCapture to get customer information. " + ioe.getMessage(), ioe);
                throw new IOException("getCustomer(): IOException calling CertCapture to get customer information. " + ioe.getMessage(), ioe);
            }

        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCustomer(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customer;

    }

    /**
     * Alternate method to get customer. Customer ID is int coming with Vertex
     * certificate
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public CertCaptureCustomerBean getCustomer(String companyId, int customerId) throws Exception {
        return getCustomer(companyId, "" + customerId);
    }

    /**
     * Get the customer record using the customer number. Same as getCustomer
     * with an additional header to tell CertCapture what to search for.
     *
     * @param companyId
     * @param customerNumber
     * @return
     * @throws Exception
     */
    public CertCaptureCustomerBean getCustomerByCustomerNumber(String companyId, String customerNumber) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCustomerBean customer = null;
        

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        log.debug("getCustomer(): looking up customer for companyId=" + companyId + " and customerId=" + customerNumber);
        try {
            // Encode space, slash, and ampersand
             String customerNumberEncoded = customerNumber.replaceAll(" ", "%20").replaceAll("/", "%252F").replaceAll("&", "%26");
            
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerNumberEncoded;
            log.debug("getCustomer(): fullUrl=" + fullUrl);
            log.debug("getCustomer(): x-client-id=" + companyId);
            log.debug("getCustomer(): Authorization=Basic " + getToken());

            // Call CertCapture to get the customer information
            response = Request.get(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("x-customer-primary-key", "customer_number")
                    .execute().returnContent().asString();

            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("getCustomer(): CertCapture response=" + response);

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
                throw new Exception("getCustomer(): CertCapture returned " + writer.toString());
            } else {
                customer = objectMapper.readValue(response, CertCaptureCustomerBean.class);
            }
        } catch (IOException ioe) {
            if (ioe.getMessage() != null && ioe.getMessage().contains("status code: 404, reason phrase: Not Found")) {
                // Customer not found.  This is OK
            } else {
                // Errors related to calling CertCapture
                log.error("getCustomer(): IOException calling CertCapture to get customer information. " + ioe.getMessage(), ioe);
                throw new IOException("getCustomer(): IOException calling CertCapture to get customer information. " + ioe.getMessage(), ioe);
            }

        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCustomer(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customer;

    }
    

    /**
     * Alternate method using customerNumber as a number
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public CertCaptureCustomerBean getCustomerByCustomerNumber(String companyId, int customerNumber) throws Exception {
        return getCustomerByCustomerNumber(companyId, "" + customerNumber);
    }

    /**
     * Get custom fields for the customer record
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerCustomFieldsBean> getCustomFields(String companyId, String customerId) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCustomerCustomFieldsBean> customFields = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/custom-fields";
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
                customFields = objectMapper.readValue(response, new TypeReference<List<CertCaptureCustomerCustomFieldsBean>>() {
                });
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("getCustomFields(): IOException calling CertCapture to get customer custom-fields information. " + ioe.getMessage(), ioe);
            throw new IOException("getCustomFields(): IOException calling CertCapture to get customer custom-fields information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("getCustomFields(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customFields;

    }

    /**
     * Alternate method to get customer custom fields. Customer ID is int coming
     * with Vertex certificate
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerCustomFieldsBean> getCustomFields(String companyId, int customerId) throws Exception {
        return getCustomFields(companyId, "" + customerId);
    }

    /**
     * Add a customer to CertCapture for the requested company.
     */
    public CertCaptureCustomerBean addCustomer(CertCaptureCustomerBean customer) throws Exception {
        String response = null;
        StringWriter writer = null;
        CertCaptureCustomerBean customerOut = null;

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {

            /* format is URL form encoded
                ** POST https://api.certcapture.com/v2/customers
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** {
                **   "customer_number": "caraparty1",
                **   "name": "Cara's Hardware Emporium",
                **   "address_line1": "1164 NW Weybridge Way",
                **   "city": "Beaverton",
                **   "zip": "97006",
                **   "phone_number": "503-806-2494",
                **   "fax_number": "503-617-7537",
                **   "email_address": "cara@taxtechnologygroup.com",
                **   "contact_name": "Cara Brauner",
                **   "state": {
                **     "initials": "OR"
                **   },
                **   "country": {
                **     "name": "United States"
                **   }
                ** }
             */
            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers";
            log.debug("addCustomer(): fullUrl=" + fullUrl);
            log.debug("addCustomer(): x-client-id=" + customer.getClient_id());
            log.debug("addCustomer(): Authorization=Basic " + getToken());

            // Create the JSON string of the customer
            writer = new StringWriter();
            objectMapper.writeValue(writer, customer);
            log.debug("addCustomer(): body of call");
            log.debug(writer.toString());

            //bodyForm(Form.form().add("username", "vip").add("password", "secret").build())
            // Call CertCapture to add the customer information
            response = Request.post(fullUrl)
                    .addHeader("Authorization", "Basic " + getToken())
                    .addHeader("x-client-id", customer.getClient_id())
                    .addHeader("Content-Type", "application/json")
                    .bodyString(writer.toString(), ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();

            writer = new StringWriter();
            JsonNode responseObject = objectMapper.readTree(response);
            objectMapper.writeValue(writer, responseObject);

            log.debug("addCustomer(): CertCapture response=" + response);

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
                throw new Exception("addCustomer(): CertCapture returned " + writer.toString());
            } else {
                customerOut = objectMapper.readValue(response, CertCaptureCustomerBean.class);
            }

        } catch (JsonProcessingException ex) {
            // Errors related to converting country or state to Json object
            log.error("addCustomer(): Exception " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw ex;
        } catch (IOException ex) {
            // Errors related to adding the customer
            log.error("addCustomer(): Exception " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw ex;
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCustomer(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;
        }

        return customerOut;
    }

    /**
     * Add custom fields for the customer record
     *
     * @param companyId
     * @param customerId
     * @return
     * @throws Exception
     */
    public List<CertCaptureCustomerCustomFieldsBean> addCustomFields(String companyId, Integer customerId, List<CertCaptureCustomerCustomFieldsBean> customerCustomFields) throws Exception {
        String response = null;
        StringWriter writer = null;
        List<CertCaptureCustomerCustomFieldsBean> customFields = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* format is URL form encoded
                ** PUT https://api.certcapture.com/v2/customers/id/custom-fields
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** custom_fields=        [
                **     {
                **         "id": 1,
                **         "value": "illo"
                **     },
                **     {
                **         "id": 5,
                **         "value": "doloribus"
                **     }
                ** ]
             */

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/custom-fields";
            log.debug("addCustomFields(): fullUrl=" + fullUrl);
            log.debug("addCustomFields(): x-client-id=" + companyId);
            log.debug("addCustomFields(): Authorization=Basic " + getToken());

            // Create the JSON string of the custom fields
            writer = new StringWriter();
            objectMapper.writeValue(writer, customerCustomFields);
            log.debug("addCustomFields(): body of call");
            log.debug("custom_fields=" + writer.toString());

            // Call CertCapture to get the customer information
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("custom_fields", writer.toString()).build())
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("addCustomFields(): CertCapture response=" + response);

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
                    throw new Exception("addCustomFields(): CertCapture returned " + writer.toString());
                }
            } else {
                customFields = objectMapper.readValue(response, new TypeReference<List<CertCaptureCustomerCustomFieldsBean>>() {
                });
            }

        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCustomFields(): IOException calling CertCapture to add customer custom-fields information. " + ioe.getMessage(), ioe);
            throw new IOException("addCustomFields(): IOException calling CertCapture to add customer custom-fields information. " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCustomFields(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return customFields;

    }

    /**
     * Associate an exposure zone (state) with a customer
     *
     * @return
     */
    public String addCustomerExposureZones(String companyId, Integer customerId, List<CertCaptureCustomerExposureZoneBean> exposureZones) throws IOException, Exception {
        String response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* format is URL form encoded
                ** PUT https://api.certcapture.com/v2/customers/id/shiptostates
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** { ship_to_states=[
                **    {"name":"North Carolina"},
                **    {"name":"Washington"}
                **    ]
                ** }
             */

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/shiptostates";
            log.debug("addCustomerExposureZones(): fullUrl=" + fullUrl);
            log.debug("addCustomerExposureZones(): x-client-id=" + companyId);
            log.debug("addCustomerExposureZones(): Authorization=Basic " + getToken());

            // Create the JSON string of the custom fields
            writer = new StringWriter();
            objectMapper.writeValue(writer, exposureZones);
            log.debug("addCustomFields(): body of call");
            log.debug("ship_to_states=" + writer.toString());

            // Call CertCapture to add the exposure zones 
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("ship_to_states", writer.toString()).build())
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("addCustomerExposureZones(): CertCapture response=" + response);

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
                    throw new Exception("addCustomerExposureZones(): CertCapture returned " + writer.toString());
                }

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCustomerExposureZones(): IOException calling CertCapture to add customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
            throw new IOException("addCustomerExposureZones(): IOException calling CertCapture to add customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCustomerExposureZones(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;

    }

    /**
     * Remove an exposure zone (state) with a customer
     *
     * @return
     */
    public String removeCustomerExposureZones(String companyId, int customerId, List<CertCaptureCustomerExposureZoneBean> exposureZones) throws IOException, Exception {
        HttpResponse response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* format is URL form encoded
                ** DELETE https://api.certcapture.com/v2/customers/id/shiptostates
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** { ship_to_states=[
                **    {"name":"North Carolina"},
                **    {"name":"Washington"}
                **    ]
                ** }
             */

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/shiptostates";
            log.debug("removeCustomerExposureZones(): fullUrl=" + fullUrl);
            log.debug("removeCustomerExposureZones(): x-client-id=" + companyId);
            log.debug("removeCustomerExposureZones(): Authorization=Basic " + getToken());

            // Create the JSON string of the custom fields
            writer = new StringWriter();
            objectMapper.writeValue(writer, exposureZones);
            log.debug("removeCustomerExposureZones(): body of call");
            log.debug("ship_to_states=" + writer.toString());

            // Call CertCapture to add the exposure zones 
            response = Request.delete(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("ship_to_states", writer.toString()).build())
                    .execute().returnResponse();

            // Not reading into JsonNode this time as this is an array.
            log.debug("removeCustomerExposureZones(): CertCapture response=" + response.getCode());

            // Returns 204 (OK)  400(error)
            // 
            if (response.getCode() != 204) {
                throw new Exception("removeCustomerExposureZones(): CertCapture returned code=" + response.getCode() + " reason=" + response.getReasonPhrase());

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("removeCustomerExposureZones(): IOException calling CertCapture to remove customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
            throw new IOException("removeCustomerExposureZones(): IOException calling CertCapture to remove customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("removeCustomerExposureZones(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;

    }    
    
       /**
     * Remove an exposure zone (local) with a customer
     *
     * @return
     */
    public String removeCustomerLocalExposureZones(String companyId, int customerId, List<CertCaptureCustomerExposureZoneBean> exposureZones) throws IOException, Exception {
        HttpResponse response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* format is URL form encoded
                ** DELETE https://api.certcapture.com/v2/customers/id/exposure-zones
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** { exposure_zones=[
                **    {"name":"North Carolina"},
                **    {"name":"Washington"}
                **    ]
                ** }
             */

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/exposure-zones";
            log.debug("removeCustomerExposureZones(): fullUrl=" + fullUrl);
            log.debug("removeCustomerExposureZones(): x-client-id=" + companyId);
            log.debug("removeCustomerExposureZones(): Authorization=Basic " + getToken());

            // Create the JSON string of the custom fields
            writer = new StringWriter();
            objectMapper.writeValue(writer, exposureZones);
            log.debug("removeCustomerExposureZones(): body of call");
            log.debug("exposure_zones=" + writer.toString());

            // Call CertCapture to add the exposure zones 
            response = Request.delete(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("exposure_zones", writer.toString()).build())
                    .execute().returnResponse();

            // Not reading into JsonNode this time as this is an array.
            log.debug("removeCustomerLocalExposureZones(): CertCapture response=" + response.getCode());

            // Returns 204 (OK)  400(error)
            // 
            if (response.getCode() != 204) {
                throw new Exception("removeCustomerLocalExposureZones(): CertCapture returned code=" + response.getCode() + " reason=" + response.getReasonPhrase());

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("removeCustomerLocalExposureZones(): IOException calling CertCapture to remove customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
            throw new IOException("removeCustomerLocalExposureZones(): IOException calling CertCapture to remove customer Exposure Zones customer=(" + customerId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("removeCustomerLocalExposureZones(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;

    }    
    
    /**
     * Associate an attribute (DO NOT USE) with a customer
     *
     * @return
     */
    public String addCustomerAttribute(String companyId, Integer customerId, String attributeName) throws IOException, Exception {
        String response = null;
        StringWriter writer = null;
        String success = null;

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            /* format is URL form encoded
                ** PUT https://api.certcapture.com/v2/customers/id/attributes
                ** HEADERS:
                ** x-client-id:12345
                ** Authorization:Basic MYAUTHSTRING=
                ** Content-Type:application/x-www-form-urlencoded
                **
                ** BODY:
                ** attributes=[{"name":"DO NOT USE"}]
                ** 
             */

            String fullUrl = getCertCaptureUrl() + (getCertCaptureUrl().endsWith("/") ? "" : "/") + "v2/customers/" + customerId + "/attributes";
            log.debug("addCustomerAttribute(): fullUrl=" + fullUrl);
            log.debug("addCustomerAttribute(): x-client-id=" + companyId);
            log.debug("addCustomerAttribute(): Authorization=Basic " + getToken());

            // Create the JSON string of the attribute fields.  
            String attributeData = "[{\"name\":\"" + attributeName + "\"}]";
            log.debug("addCustomerAttribute(): body of call");
            log.debug("attributes=" + attributeData);

            // Call CertCapture to add the exposure zones 
            response = Request.put(fullUrl)
                    .addHeader("Authorization", "basic " + getToken())
                    .addHeader("x-client-id", companyId)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .bodyForm(Form.form().add("attributes", attributeData).build())
                    .execute().returnContent().asString();

            // Not reading into JsonNode this time as this is an array.
            log.debug("addCustomerAttribute(): CertCapture response=" + response);

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
                    throw new Exception("addCustomerAttribute(): CertCapture returned " + writer.toString());
                }

            }
        } catch (IOException ioe) {
            // Errors related to calling CertCapture
            log.error("addCustomerAttribute(): IOException calling CertCapture to add customer Attribute customer=(" + customerId + "). " + ioe.getMessage(), ioe);
            throw new IOException("addCustomerAttribute(): IOException calling CertCapture to add customer Attribute customer=(" + customerId + "). " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            // Errors related to reading response into object
            log.error("addCustomerAttribute(): Exception " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;

        }

        return success;

    }

    public String getCertCaptureUrl() {
        return certCaptureUrl;
    }

    public void setCertCaptureUrl(String certCaptureUrl) {
        this.certCaptureUrl = certCaptureUrl;
    }

}
