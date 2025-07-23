/**
 * Title: ExcelFileSwingWorker
 *
 * Description: This class does the work of uploading certificates.
 *
 * Copyright: Copyright (c) 2021
 *
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: ExcelFileSwingWorker.java,v $
 * Revision 1.7  2023/02/01 22:49:07  carab
 * Add additional messaging to certificate import by date
 *
 * Revision 1.6  2023/01/09 22:50:04  carab
 * Add start date to criteria for COMPLETE certs
 *
 * Revision 1.5  2022/02/09 18:02:59  carab
 * Add customer attribute
 *
 * Revision 1.4  2022/02/02 22:46:59  carab
 * Add Sold To Customer Number, Sold To Customer Name custom fields
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
 */
package com.ttg.certificate_upload;

import com.ttg.certificate_upload.beans.CertCaptureCertificateBean;
import com.ttg.certificate_upload.beans.CertCaptureCompanyBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerCustomFieldsBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerExposureZoneBean;
import com.ttg.certificate_upload.certcapture_api.CertCaptureCertificate;
import com.ttg.certificate_upload.utils.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;

import com.ttg.certificate_upload.certcapture_api.CertCaptureCustomer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelFileSwingWorker extends SwingWorker<Integer, String> {

    private Logger log = LogManager.getLogger(getClass());

    private File excelFile;
    private String companyName;
    private HashMap<String, CertCaptureCompanyBean> companies;
    private String targetEnvironment;
    private File certificateImageDirectory;
    private String targetCertificateStatus;
    private Properties properties;
    private JTextArea textarea;
    private XSSFWorkbook workbook;
    private List<String> messages = new ArrayList();

    private int customerNumberColumn = -1;
    private int customerAltIdColumn = -1;
    private int customerFeinColumn = -1;
    private int customerNameColumn = -1;
    private int customerAttnNameColumn = -1;
    private int customerAddress1Column = -1;
    private int customerAddress2Column = -1;
    private int customerCityColumn = -1;
    private int customerStateColumn = -1;
    private int customerZipColumn = -1;
    private int customerCountryColumn = -1;
    private int customerPhoneColumn = -1;
    private int customerFaxColumn = -1;
    private int customerEmailColumn = -1;
    private int certificateStateColumn = -1;
    private int certificateImageColumn = -1;
    private int certificateEffectiveDateColumn = -1;
    private int certificateExpirationDateColumn = -1;
    private int certificateNumberColumn = -1;
    private int exemptReasonColumn = -1;
    private int soldToCustomerNumberColumn = -1;
    private int soldToCustomerNameColumn = -1;
    private int excludeFromMailingCampaignColumn = -1;
    
    private String[] usSalesTaxStates = {
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "FL", "GA", "HI",
        "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA",
        "MI", "MN", "MS", "MO", "NE", "NV", "NJ", "NM", "NY", "NC", 
	"ND", "OH", "OK", "PA", "RI", "SC", "SD", "TN", "TX", "UT", 
	"VT", "VA", "WA", "WV", "WI", "WY", "DC"
    };

    /**
     * Initialize data used by the worker
     *
     * @param excelFile
     * @param certificateImageDirectory
     * @param companyName
     * @param companies
     * @param targetEnvironment
     * @param properties
     * @param textarea
     */
    public ExcelFileSwingWorker(File excelFile, File certificateImageDirectory, String companyName,
            HashMap<String, CertCaptureCompanyBean> companies, String targetEnvironment, String targetCertificateStatus,
            Properties properties, JTextArea textarea) {
        this.excelFile = excelFile;
        this.certificateImageDirectory = certificateImageDirectory;
        this.companyName = companyName;
        this.companies = companies;
        this.targetEnvironment = targetEnvironment;
        this.targetCertificateStatus = targetCertificateStatus;
        this.properties = properties;
        this.textarea = textarea;
        this.workbook = null;
    }

    @Override
    protected Integer doInBackground() throws Exception {

        int rowNum = 0;
        int headerRow = -1;

        String certCaptureClientId = null;
        String certCaptureUsername = null;
        String certCapturePassword = null;
        String certCaptureUrl = null;

        // Get company ID for selected company.
        CertCaptureCompanyBean companyBean = companies.get(companyName);
        if (companyBean == null) {
            publish("Company [" + companyName + "] not found in list.  This is a programming error. ");
            throw new Exception("Company [" + companyName + "] not found in list.  This is a programming error. ");
        }

        // Set values based on target environment
        if ("production".equals(targetEnvironment)) {
            certCaptureClientId = companyBean.getProductionClientId();
            certCaptureUsername = properties.getProperty("CERTCAPTURE_PRODUCTION_USERNAME");
            certCapturePassword = properties.getProperty("CERTCAPTURE_PRODUCTION_PASSWORD");
            certCaptureUrl = properties.getProperty("CERTCAPTURE_PRODUCTION_URL");
        } else {
            certCaptureClientId = companyBean.getSandboxClientId();
            certCaptureUsername = properties.getProperty("CERTCAPTURE_SANDBOX_USERNAME");
            certCapturePassword = properties.getProperty("CERTCAPTURE_SANDBOX_PASSWORD");
            certCaptureUrl = properties.getProperty("CERTCAPTURE_SANDBOX_URL");
        }

        publish(".... targetCertificateStatus=" + this.targetCertificateStatus);
        log.debug(".... targetCertificateStatus=" + this.targetCertificateStatus);

        try {
            
            // Open Excel file
            publish("Opening Excel File");
            log.info("Opening Excel File");
            workbook = new XSSFWorkbook(new FileInputStream(excelFile));

        } catch (FileNotFoundException ex) {
            publish(ex.getClass().getName() + " exception occurred opening Excel file");
            throw ex;
        } catch (IOException ex) {
            publish(ex.getClass().getName() + " exception occurred opening Excel file");
            throw ex;
        }

        // Assume first sheet
        publish("Getting first worksheet");
        log.info("Getting first worksheet");
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            try {
                rowNum++;

                // Find header row
                if (headerRow == -1) {
                    publish("Row " + rowNum + " looking for headers..");
                    log.info("Row " + rowNum + " looking for headers..");
                    Cell firstCell = row.getCell(0);
                    if (firstCell == null || StringUtils.isEmpty(firstCell.getStringCellValue())) {
                        // Skip row
                        continue;
                    } else {
                        publish("Row " + rowNum + " Found header Row.");
                        log.info("Row " + rowNum + " Found header Row.");
                        headerRow = row.getRowNum();

                        String errorMessage = processAndValidateHeaderRow(row);
                        if (!StringUtils.isEmpty(errorMessage)) {
                            log.error(errorMessage);
                            throw new Exception(errorMessage);
                        }

                    }
                } // Data Rows
                else {
                    publish("Row " + rowNum + " Processing data row.");
                    log.info("Row " + rowNum + " Processing data row.");
                    
                    // Per Ricky Ortiz, for multi-state, create exposure zones for all states,
                    // Create certificate for first state.
                    
                    CertCaptureCertificate ccCertificate = new CertCaptureCertificate(certCaptureUrl, certCaptureUsername, certCapturePassword);
                    CertCaptureCustomer ccCustomer = new CertCaptureCustomer(certCaptureUrl, certCaptureUsername, certCapturePassword);

                    String customerNumber = getStringFromRow(row, this.customerNumberColumn);
                    String altId = getStringFromRow(row, this.customerAltIdColumn);
                    String customerFein = getStringFromRow(row, this.customerFeinColumn);
                    String customerName = getStringFromRow(row, this.customerNameColumn);
                    String customerAttnName = getStringFromRow(row, this.customerAttnNameColumn);
                    String customerAddress1 = getStringFromRow(row, this.customerAddress1Column);
                    String customerAddress2 = getStringFromRow(row, this.customerAddress2Column);
                    String customerCity = getStringFromRow(row, this.customerCityColumn);
                    String customerState = getStringFromRow(row, this.customerStateColumn);
                    String customerZipCode = getStringFromRow(row, this.customerZipColumn);
                    String customerCountry = getStringFromRow(row, this.customerCountryColumn);
                    String customerPhone = getStringFromRow(row, this.customerPhoneColumn);
                    String customerFax = getStringFromRow(row, this.customerFaxColumn);
                    String customerEmail = getStringFromRow(row, this.customerEmailColumn);
                    String[] certificateStates = getStringsFromRow(row, this.certificateStateColumn);
                    String certificateImage = getStringFromRow(row, this.certificateImageColumn);
                    String exemptReason = getStringFromRow(row, this.exemptReasonColumn);
                    
                    String soldToCustomerNumber = null;
                    String soldToCustomerName = null;
                    String excludeFromMailingCampaign = null;
                    if (this.soldToCustomerNumberColumn != -1) {
                        soldToCustomerNumber = getStringFromRow(row, this.soldToCustomerNumberColumn);
                    }
                    if (this.soldToCustomerNameColumn != -1) {
                        soldToCustomerName = getStringFromRow(row, this.soldToCustomerNameColumn);
                    }
                    if (this.excludeFromMailingCampaignColumn != -1) {
                        excludeFromMailingCampaign = getStringFromRow(row, this.excludeFromMailingCampaignColumn);
                    }
                    
                    
                    String certificateNumber = null;
                    Date effectiveDate = null;
                    Date expirationDate = null;
                    if ("COMPLETE".equals(this.targetCertificateStatus)) {
                        certificateNumber = getStringFromRow(row, this.certificateNumberColumn);
                        effectiveDate = getDateFromRow(row, this.certificateEffectiveDateColumn);
                        expirationDate = getDateFromRow(row, this.certificateExpirationDateColumn);
                    }

                    // If customer number is blank, we can't use the row
                    if (StringUtils.isEmpty(customerNumber)) {
                        publish(".... Billing Customer Numberr is empty.  Skipping to next row");
                        log.info(".... Billing Customer Number is empty.  Skipping to next row");
                        continue;
                    }
                    
 
                    // - Look up customer.  If it doesn't exist, add it
                    publish(".... Looking up Customer. customerNumber=" + customerNumber + " name=" + customerName);
                    log.info(".... Looking up Customer. customerNumber=" + customerNumber + " name=" + customerName);

                    // Look up customer in CertCapture.
                    CertCaptureCustomerBean customer = null;
                    boolean customerExists = false;
                    try {
                        customer = ccCustomer.getCustomerByCustomerNumber(certCaptureClientId, customerNumber);
                        if (customer != null) {
                            customerExists = true;
                            publish(".... customer found: id=" + customer.getId() + " customerNumber=" + customer.getCustomer_number() + " alternateId=" + customer.getAlternate_id() + " name=" + customer.getName());
                            log.info(".... customer found: id=" + customer.getId() + " customerNumber=" + customer.getCustomer_number() + " alternateId=" + customer.getAlternate_id() + " name=" + customer.getName());
                        } else {
                            publish(".... Customer does not exist in CertCapture");
                        }

                    } catch (IOException ioe) {
                        // already logged
                        publish("Row " + rowNum + " UNEXPECTED " + ioe.getMessage());
                        publish("Row " + rowNum + " Skipping to next row.");
                    } catch (Exception e) {
                        // already logged
                        publish("Row " + rowNum + " UNEXPECTED " + e.getMessage());
                        publish("Row " + rowNum + " Skipping to next row.");
                        continue;
                    }

                    // Add customer if it doesn't exist
                    if (!customerExists) {
                        // Set values in customer bean.
                        customer = new CertCaptureCustomerBean();
                        customer.setClient_id(certCaptureClientId);
                        customer.setCustomer_number(customerNumber);
                        customer.setAlternate_id(altId);
                        customer.setFein_number(customerFein);
                        customer.setName(customerName);
                        customer.setAttn_name(customerAttnName);
                        customer.setAddress_line1(customerAddress1);
                        customer.setAddress_line2(customerAddress2);
                        customer.setCity(customerCity);

                        if (!StringUtils.isEmpty(customerState) && customerState.length() == 2) {
                            customer.getState().setInitials(customerState);
                        } else {
                            customer.getState().setName(customerState);
                        }

                        // CertCapture seems to NOT like empty state
                        if (StringUtils.isEmpty(customerState)) {
                            customer.setState(null);
                        }                        

                        customer.setZip(customerZipCode);

                        if (!StringUtils.isEmpty(customerCountry) && (customerCountry.length() == 2 || customerCountry.length() == 3)) {
                            customer.getCountry().setInitials(customerCountry);
                        } else {
                            customer.getCountry().setName(customerCountry);
                        }

                        customer.setPhone_number(customerPhone);
                        customer.setFax_number(customerFax);
                        customer.setEmail_address(customerEmail);

                        publish(".... Calling CertCapture to add customer");
                        CertCaptureCustomerBean newCustomer = ccCustomer.addCustomer(customer);
                        publish(".... New CertCapture customer ID=" + newCustomer.getId() + " customerNumber=" + newCustomer.getCustomer_number());
                        customer = newCustomer;
                        
                    } 
                    
                    // If they have a custom field add it
                    boolean foundNumber = false;
                    boolean foundName = false;
                    if (!StringUtils.isEmpty(soldToCustomerNumber) || !StringUtils.isEmpty(soldToCustomerName)) {
                        publish(".... Checking Sold To Customer custom field");
                        List<CertCaptureCustomerCustomFieldsBean> customerCustomFieldsList = ccCustomer.getCustomFields(certCaptureClientId, customer.getId());
                        if (customerCustomFieldsList != null) {
                            for (CertCaptureCustomerCustomFieldsBean customField : customerCustomFieldsList) {
                                if ("Sold To Customer Number".equalsIgnoreCase(customField.getField_name())) {
                                    customField.setValue(soldToCustomerNumber);
                                    foundNumber = true;
                                }
                                if ("Sold To Customer Name".equalsIgnoreCase(customField.getField_name())) {
                                    customField.setValue(soldToCustomerName);
                                    foundName = true;
                                }
                            }
                            if (!foundNumber) {
                                publish(".... didn't find Sold To Customer Number in customer custom fields list.  THIS IS A SETUP ERROR FOR THE COMPANY.");
                                log.info(".... didn't find Sold To Customer Number in customer custom fields list.  This is a set up error for the company.");
                            }
                            if (!foundName) {
                                publish(".... didn't find Sold To Customer Name in customer custom fields list.  THIS IS A SETUP ERROR FOR THE COMPANY.");
                                log.info(".... didn't find Sold To Customer Name in customer custom fields list.  This is a set up error for the company.");
                            }                        
                        } else {
                            // No Sold To Customer custom fields
                            publish(".... didn't find any customer custom fields list.  THIS IS A SETUP ERROR FOR THE COMPANY.");
                            log.info(".... didn't find any customer custom fields list.  This is a set up error for the company.");
                        }

                        // Found the field.  Send update
                        if (foundNumber && foundName) {
                            log.info("Calling addCustomFields");
                            ccCustomer.addCustomFields(customer.getClient_id(), customer.getId(), customerCustomFieldsList);
                            publish(".... Updated Customer custom fields.");
                            log.info("Customer custom fields added");

                        }

                    }

                    // If they have Exclude From Mailing Campaign, add a customer attribute
                    if (!StringUtils.isEmpty(excludeFromMailingCampaign)) {
                        log.info(".... Calling CertCapture to add Attribute [" + excludeFromMailingCampaign + "] toc customer.");
                        ccCustomer.addCustomerAttribute(customer.getClient_id(), customer.getId(), excludeFromMailingCampaign.toUpperCase());
                        publish(".... Customer Attribute [" + excludeFromMailingCampaign + "] added.");
                        log.info(".... Customer Attribute [" + excludeFromMailingCampaign + "] added.");
                    }
                    

                    // if Certificate states is empty, we are done
                    if (certificateStates == null || certificateStates.length == 0) {
                        publish(".... State is empty.  Skipping to next row");
                        log.info(".... State is empty.  Skipping to next row");
                        continue;
                        
                    }

                    // Check exposure zones
                    // Add certificate exposure zone for all states
                    publish(".... Calling CertCapture to add customer exposure zone(s)=" + StringUtils.arrayToCommaDelimitedString(certificateStates));
                    log.debug(".... Calling CertCapture to add customer exposure zone(s)=" + StringUtils.arrayToCommaDelimitedString(certificateStates));
                    List<CertCaptureCustomerExposureZoneBean> exposureZones = new ArrayList();
                    for (int i = 0; i < certificateStates.length; i++) {
                        CertCaptureCustomerExposureZoneBean exposureZone = new CertCaptureCustomerExposureZoneBean();
                        exposureZone.setName(ccCertificate.convertStateName(certificateStates[i]));
                        exposureZones.add(exposureZone);
                    }
                    ccCustomer.addCustomerExposureZones(certCaptureClientId, customer.getId(), exposureZones);
                    publish(".... Added customer exposure zone=" + StringUtils.arrayToCommaDelimitedString(certificateStates));
                    log.info(".... Added customer exposure zone=" + StringUtils.arrayToCommaDelimitedString(certificateStates));

                    
                    List<CertCaptureCertificateBean> certificates = null;
                    CertCaptureCertificateBean certificate = null;
                    
                    // For PENDING Look up certificate for first state.  If it doesn't exist, add it.
                    if ("PENDING".equals(this.targetCertificateStatus)) {
                        publish(".... Looking up Certificate");
                        certificates = ccCertificate.getCertificates(certCaptureClientId, customer.getId(), exemptReason, certificateStates[0], null);
                        
                        // Not foundNumber, add certificate
                        if (certificates == null || certificates.size() == 0) {

                            publish(".... found no matching certificate, adding new certificate for state=" + certificateStates[0] + " exemptReason=" + exemptReason);
                            log.info(".... found no matching certificate, adding new certificate for state=" + certificateStates[0] + " exemptReason=" + exemptReason);

                            certificate = new CertCaptureCertificateBean();
                            certificate.setClient_id(certCaptureClientId);
                            certificate.getExposure_zone().setName(ccCertificate.convertStateName(certificateStates[0]));
                            certificate.getExpected_tax_code().setName(exemptReason.toUpperCase());

                            // Special fields for PENDING
                            certificate.getStatus().setName("PENDING");
                            certificate.setSubmit_to_stack(Boolean.TRUE);

                            String base64pdf = null;
                            try {
                                base64pdf = readAndEncodePDF(certificateImage);

                            } catch (IOException ioe) {
                                // already logged
                                publish(".... Got IO exception reading certificate image file " + certificateImage + " see log.");
                                publish(".... SKIPPING CERTIFICATE!!");
                                continue;
                            }

                            CertCaptureCertificateBean newCertificate = ccCertificate.addCertificate(certificate, base64pdf);
                            publish(".... certificate saved, id=" + newCertificate.getId());
                            log.info(".... certificate saved, id=" + newCertificate.getId());
                            certificate = newCertificate;

                            // get customers for certificate.  If none-foundNumber, create a link
                            List<CertCaptureCustomerBean> customers = null;

                            // Associate customer and certificate
                            publish(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            log.info(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            ccCertificate.addCertificateCustomer(certCaptureClientId, certificate.getId(), customer.getId());
                            publish(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            log.info(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                        }
                    
                    
                        else {
                            publish(".... found " + certificates.size() + " certificate(s) for customer for state=" + certificateStates[0] + " exemptReason=" + exemptReason);
                            log.info(".... found " + certificates.size() + " certificate(s) for customer for state=" + certificateStates[0] + " exemptReason=" + exemptReason);

                        }
                    }

                   // For COMPLETE Look up certificate for each state.  If it doesn't exist, add it.
                    if ("COMPLETE".equals(this.targetCertificateStatus)) {
                        String base64pdf = null;
                        boolean base64Attempted = false;
                        boolean base64Fail = false;
                        for (int stateNo = 0; stateNo < certificateStates.length && base64Fail == false; stateNo++) {
                            publish(".... Looking up Certificate for " + certificateStates[stateNo] + " effectiveDate=" + DateUtils.formatDate(effectiveDate,"yyyy-MM-dd"));
                            certificates = ccCertificate.getCertificates(certCaptureClientId, customer.getId(), exemptReason, certificateStates[stateNo], effectiveDate);

                            
                            // Not foundNumber, add certificate
                            if (certificates == null || certificates.size() == 0) {

                                publish(".... found no matching certificate, adding new certificate for state=" + certificateStates[stateNo] + " exemptReason=" + exemptReason + " effectiveDate=" + DateUtils.formatDate(effectiveDate,"yyyy-MM-dd"));
                                log.info(".... found no matching certificate, adding new certificate for state=" + certificateStates[stateNo] + " exemptReason=" + exemptReason + " effectiveDate=" + DateUtils.formatDate(effectiveDate,"yyyy-MM-dd"));

                                certificate = new CertCaptureCertificateBean();
                                certificate.setClient_id(certCaptureClientId);
                                certificate.getExposure_zone().setName(ccCertificate.convertStateName(certificateStates[stateNo]));
                                certificate.getExpected_tax_code().setName(exemptReason.toUpperCase());

                                // Special fields for COMPLETE certs
                                certificate.setTax_number(certificateNumber);
                                certificate.setSigned_date(effectiveDate);
                                certificate.setExpiration_date(expirationDate);
                                certificate.setValid(true);
                                certificate.getStatus().setName("COMPLETE");

                                try {
                                    base64Attempted = true;
                                    base64pdf = readAndEncodePDF(certificateImage);

                                } catch (IOException ioe) {
                                    base64Fail = true;

                                    // already logged
                                    publish(".... Got IO exception reading certificate image file " + certificateImage + " see log.");
                                    publish(".... SKIPPING CERTIFICATE!!");

                                    continue;
                                }

                                CertCaptureCertificateBean newCertificate = ccCertificate.addCertificate(certificate, base64pdf);
                                publish(".... certificate saved, id=" + newCertificate.getId());
                                log.info(".... certificate saved, id=" + newCertificate.getId());
                                certificate = newCertificate;

                                // get customers for certificate.  If none-foundNumber, create a link
                                List<CertCaptureCustomerBean> customers = null;

                                // Associate customer and certificate
                                publish(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                                log.info(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                                ccCertificate.addCertificateCustomer(certCaptureClientId, certificate.getId(), customer.getId());
                                publish(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                                log.info(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");

                            } else {
                                publish(".... found " + certificates.size() + " certificate(s) for customer for state=" + certificateStates[stateNo] + " exemptReason=" + exemptReason);
                                log.info(".... found " + certificates.size() + " certificate(s) for customer for state=" + certificateStates[stateNo] + " exemptReason=" + exemptReason);

                            }


                        } 
                    }
                    
                    
                    publish(".... Complete");
                    log.info(".... Complete");
                }
            } catch (Exception e) {
                publish("Exception " + e.getClass().getName());
                publish("Message: " + e.getMessage());
                StringWriter swriter = new StringWriter();
                PrintWriter pwriter = new PrintWriter(swriter);
                e.printStackTrace(pwriter);
                publish("Stack Trace: ");
                publish(swriter.toString());
                throw e;
            }
        }

        publish("Done processing Excel file");
        publish("===========================================");
        publish("All Done!");

        workbook.close();
        workbook = null;

        // Data Rows:
        return textarea.getText().length();
    }

    /**
     * Get a String from a data row. Data can sometimes be a number as far as
     * Excel is concerned but we want the string value.
     *
     * @param row
     * @return
     */
    private String getStringFromRow(Row row, int columnNumber) {
        String stringValue = null;

        // if column not set, return null
        if (columnNumber < 0) {
            return stringValue;
        }
        
        // Some data we need is numeric but we want the string value.
        if (row.getCell(columnNumber) == null || row.getCell(columnNumber).getCellType().equals(CellType.STRING)) {
            stringValue = (row.getCell(columnNumber) == null ? null : row.getCell(columnNumber).getStringCellValue());
        } else if (row.getCell(columnNumber).getCellType().equals(CellType.NUMERIC)) {
            double doubleValue = row.getCell(columnNumber).getNumericCellValue();
            stringValue = "" + (int) doubleValue;
        }
        
        if (!StringUtils.isEmpty(stringValue)) {
            return stringValue.trim();
        }
        return stringValue;
    }

    private String[] getStringsFromRow(Row row, int columnNumber) {
        String[] stringValues = null;
        
        // Get a list of strings and parse into trimmed array
        String stringValue = getStringFromRow(row, columnNumber);

        if (!StringUtils.isEmpty(stringValue)) {
            String[] strings = stringValue.split(",");
            stringValues = new String[strings.length];
            for (int i = 0; i < strings.length; i++) {
                stringValues[i] = strings[i].trim();
            }
        }
        
        if ("USA".equals(stringValue)) {
            stringValues = usSalesTaxStates;
        }

        return stringValues;
    }
    /**
     * Get a date cell from a row.
     *
     * @param row
     * @param columnNumber
     * @return
     */
    private Date getDateFromRow(Row row, int columnNumber) {
        Date dateValue = null;

        dateValue = (row.getCell(columnNumber) == null ? null : row.getCell(columnNumber).getDateCellValue());
        return dateValue;

    }

    /**
     * Determine column locations and validate if all required columns are
     * present
     *
     * @param row
     * @return
     */
    private String processAndValidateHeaderRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Number".equalsIgnoreCase(cell.getStringCellValue())) {
                customerNumberColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Alternate ID".equalsIgnoreCase(cell.getStringCellValue())) {
                customerAltIdColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer FEIN".equalsIgnoreCase(cell.getStringCellValue())) {
                customerFeinColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Name".equalsIgnoreCase(cell.getStringCellValue())) {
                customerNameColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Attn Name".equalsIgnoreCase(cell.getStringCellValue())) {
                customerAttnNameColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Address 1".equalsIgnoreCase(cell.getStringCellValue())) {
                customerAddress1Column = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Address 2".equalsIgnoreCase(cell.getStringCellValue())) {
                customerAddress2Column = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer City".equalsIgnoreCase(cell.getStringCellValue())) {
                customerCityColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer State".equalsIgnoreCase(cell.getStringCellValue())) {
                customerStateColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Zip".equalsIgnoreCase(cell.getStringCellValue())) {
                customerZipColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Country".equalsIgnoreCase(cell.getStringCellValue())) {
                customerCountryColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Phone".equalsIgnoreCase(cell.getStringCellValue())) {
                customerPhoneColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Fax".equalsIgnoreCase(cell.getStringCellValue())) {
                customerFaxColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Email".equalsIgnoreCase(cell.getStringCellValue())) {
                customerEmailColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate State".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateStateColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate Image".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateImageColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate Effective Date".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateEffectiveDateColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate Expiration Date".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateExpirationDateColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate Number".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateNumberColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Exempt Reason".equalsIgnoreCase(cell.getStringCellValue())) {
                exemptReasonColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Sold To Customer Number".equalsIgnoreCase(cell.getStringCellValue())) {
                soldToCustomerNumberColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Sold To Customer Name".equalsIgnoreCase(cell.getStringCellValue())) {
                soldToCustomerNameColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Exclude from Mailing Campaign".equalsIgnoreCase(cell.getStringCellValue())) {
                excludeFromMailingCampaignColumn = cell.getColumnIndex();
            }
        }

        // Validate headers
        int numMissingHeaders = 0;
        String errorMessage = "The following column headers are missing: ";
        if (customerNumberColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Number\"";
            numMissingHeaders++;
        }
        if (customerAltIdColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Alternate ID\"";
            numMissingHeaders++;
        }
        if (customerFeinColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer FEIN\"";
            numMissingHeaders++;
        }
        if (customerNameColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Name\"";
            numMissingHeaders++;
        }
        if (customerAttnNameColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Attn Name\"";
            numMissingHeaders++;
        }
        if (customerAddress1Column == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Address 1\"";
            numMissingHeaders++;
        }
        if (customerAddress2Column == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Address 2\"";
            numMissingHeaders++;
        }
        if (customerCityColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer City\"";
            numMissingHeaders++;
        }
        if (customerStateColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer State\"";
            numMissingHeaders++;
        }
        if (customerZipColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Zip\"";
            numMissingHeaders++;
        }
        if (customerCountryColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Country\"";
            numMissingHeaders++;
        }
        if (customerPhoneColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Phone\"";
            numMissingHeaders++;
        }
        if (customerFaxColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Fax\"";
            numMissingHeaders++;
        }
        if (customerEmailColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Email\"";
            numMissingHeaders++;
        }

        if (certificateStateColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate State\"";
            numMissingHeaders++;
        }

        if (exemptReasonColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Exempt Reason\"";
            numMissingHeaders++;
        }

        if (certificateImageColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate Image\"";
            numMissingHeaders++;
        }

        if ("COMPLETE".equals(this.targetCertificateStatus)) {
            if (certificateEffectiveDateColumn == -1) {
                errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate Effective Date\"";
                numMissingHeaders++;
            }
            if (certificateExpirationDateColumn == -1) {
                errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate Expiration Date\"";
                numMissingHeaders++;
            }                
            if (certificateNumberColumn == -1) {
                errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate Number\"";
                numMissingHeaders++;
            }
        }

        // Sold To Customer Number, Sold To Customer Name, Exclude From Mailing Campaign optional
        
        if (numMissingHeaders > 0) {
            return errorMessage;
        }

        return null;
    }

    /**
     * Read the certificate pdf file and encode to base64.
     *
     * @param certificateImage
     * @return
     * @throws IOException
     */
    private String readAndEncodePDF(String certificateImage) throws IOException {
        String fullPath = this.certificateImageDirectory.getAbsolutePath() + File.separator + certificateImage;
        File certificateImageFile = new File(fullPath);
        byte[] byteFileArray = new byte[0];
        String base64String = "";

        try {
            byteFileArray = FileUtils.readFileToByteArray(certificateImageFile);
            if (byteFileArray.length > 0) {

                base64String = Base64.encodeBase64String(byteFileArray);
                //log.info("encoded base64");
                //log.info(base64String);

            }
        } catch (IOException ex) {
            log.error("IOException reading " + certificateImage + " and convverting to base 64");
            throw ex;
        }

        return base64String;
    }

    @Override
    protected void process(List<String> chunks) {
        // Get Info
        if (chunks == null) return;
        
        for (String message : chunks) {
            textarea.append(message + "\n");
            textarea.setCaretPosition(textarea.getText().length() - 1);
        }
    }

    public int getCaretPosition() {
        return textarea.getCaretPosition();
    }
}
