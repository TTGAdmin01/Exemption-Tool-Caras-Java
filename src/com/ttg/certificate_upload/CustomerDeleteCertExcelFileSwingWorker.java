/**
 * Title: ExcelFileSwingWorker
 *
 * Description: This class does the work of creating a link between customers/certificates
 *
 * Copyright: Copyright (c) 2022
 *
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 *
 * $Log: CustomerDeleteCertExcelFileSwingWorker.java,v $
 * Revision 1.4  2023/01/09 22:50:04  carab
 * Add start date to criteria for COMPLETE certs
 *
 * Revision 1.3  2022/09/20 22:23:03  carab
 * Add ability to remove local exposure zones
 *
 * Revision 1.2  2022/08/11 22:40:04  carab
 * Add initials to exposure zone, encode customer-number for search
 *
 * Revision 1.1  2022/08/05 23:37:36  carab
 * Delete Certificate, ShipToStates
 *
 * Revision 1.1  2022/05/09 23:54:29  carab
 * Add program to link existing customers with certificates
 *
 *
 *
 */
package com.ttg.certificate_upload;

import com.ttg.certificate_upload.beans.CertCaptureCertificateBean;
import com.ttg.certificate_upload.beans.CertCaptureCompanyBean;
import com.ttg.certificate_upload.beans.CertCaptureCustomerBean;
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CustomerDeleteCertExcelFileSwingWorker extends SwingWorker<Integer, String> {

    private Logger log = LogManager.getLogger(getClass());

    private File excelFile;
    private String companyName;
    private HashMap<String, CertCaptureCompanyBean> companies;
    private String targetEnvironment;
    private Properties properties;
    private JTextArea textarea;
    private XSSFWorkbook workbook;
    private List<String> messages = new ArrayList();

    private int customerNumberColumn = -1;
    private int certificateIdColumn = -1;
    private int stateColumn = -1;
    private int localExposureZoneColumn = -1;
     

    /**
     * Initialize data used by the worker
     *
     * @param excelFile
     * @param companyName
     * @param companies
     * @param targetEnvironment
     * @param properties
     * @param textarea
     */
    public CustomerDeleteCertExcelFileSwingWorker(File excelFile, String companyName,
            HashMap<String, CertCaptureCompanyBean> companies, String targetEnvironment, 
            Properties properties, JTextArea textarea) {
        this.excelFile = excelFile;
        this.companyName = companyName;
        this.companies = companies;
        this.targetEnvironment = targetEnvironment;
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
                    String certificateId = getStringFromRow(row, this.certificateIdColumn);
                    String state = getStringFromRow(row, this.stateColumn);
                    String localExposureZone = null;
                    if (localExposureZoneColumn != -1) {
                        localExposureZone = getStringFromRow(row, this.localExposureZoneColumn);
                    }

                    if (StringUtils.isEmpty(customerNumber)) {
                        publish(".... Customer Number is blank.  Skipping row.");
                        log.info(".... Customer Number is blank.  Skipping row.");
                        continue;
                    }
                    
                    
 
                     // - Look up customer.  
                    publish(".... Looking up Customer. customerNumber=" + customerNumber);
                    log.info(".... Looking up Customer. customerNumber=" + customerNumber);

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
                            publish(".... Customer does not exist in CertCapture...skipping row");
                            log.info(".... Customer does not exist in CertCapture...skipping row");
                            continue;
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

                    //  Find Certificate
                    CertCaptureCertificateBean certificate = null;

                    // If certificate id is not blank, find and delete the certificate
                    if (!StringUtils.isEmpty(certificateId)) {
                        try {
                            publish(".... Looking up Certificate Id=" + certificateId);
                            log.info(".... Looking up Certificate Id=" + certificateId);
                            certificate = ccCertificate.getCertificate(certCaptureClientId, certificateId);
                            if (certificate == null) {
                                publish(".... Certificate does not exist in CertCapture...skipping row");
                                log.info(".... Certificate does not exist in CertCapture...skipping row");
                                continue;

                            }
                        } catch (Exception e) {
                            if (e.getMessage().contains("Unable to find") || e.getMessage().contains("Not Found")) {
                                publish(".... Certificate does not exist in CertCapture...skipping row");
                                log.info(".... Certificate does not exist in CertCapture...skipping row");

                            }
                        }

                        // Unlink Certificate/Customer
                        // Delete Certificate
                        if (certificate != null) {
                            publish(".... Unlinking customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            log.info(".... Unlinking customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            try {
                                ccCertificate.deleteCertificateCustomer(certCaptureClientId, certificate.getId(), customer.getId());
                                publish(".... Unlinked customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                                log.info(".... Unlinked customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                            } catch (Exception e) {
                                if (e.getMessage().contains("code=400 reason=Bad Request")) {
                                publish(".... customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association not found");
                                log.info(".... customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association not found");
                                }
                                else {
                                    throw e;
                                }
                            }

                            // Get certificate customers.  Are there more customers?
                            List<CertCaptureCustomerBean> certCustomers = ccCertificate.getCertificateCustomers(certCaptureClientId, certificate.getId());
                            if (certCustomers != null && certCustomers.size() > 0) {
                                publish(".... Certificate " + certificate.getId() + " still has " + certCustomers.size() + " customers.  Not deleting.");
                                log.info(".... Certificate " + certificate.getId() + " still has " + certCustomers.size() + " customers.  Not deleting.");

                            } // No more customers, delete certificate
                            else {
                                publish(".... Deleting certificate(" + certificate.getId() + ")");
                                log.info(".... Deleting certificate(" + certificate.getId() + ")");
                                ccCertificate.deleteCertificate(certCaptureClientId, certificate.getId());
                                publish(".... Deleted certificate(" + certificate.getId() + ")");
                                log.info(".... Deleted certificate(" + certificate.getId() + ")");
                            }
                        }
                    }
                    
                    // If state is not blank, remove the shipto
                    if (!StringUtils.isEmpty(state)) {
                        publish(".... Removing Customer Exposure Zone(" + state + ")");
                        log.info(".... Removing Customer Exposure Zone(" + state + ")");
                        List<CertCaptureCustomerExposureZoneBean> exposureZones = new ArrayList();
                        CertCaptureCustomerExposureZoneBean exposureZone = new CertCaptureCustomerExposureZoneBean();
                        
                        // Assume initials if state is 2 characters
                        if (state.length() ==2) {
                            exposureZone.setInitials(state);
                        } else {
                            exposureZone.setName(state);
                        }
                        exposureZones.add(exposureZone);
                        try {
                            ccCustomer.removeCustomerExposureZones(certCaptureClientId, customer.getId(), exposureZones);
                            publish(".... Removed Customer Ship-to state(" + state + ")");
                            log.info(".... Removed Customer Ship-to state(" + state + ")");
                        } catch (Exception e) {
                            if (e.getMessage().contains("code=400 reason=Bad Request")) {
                                publish(".... Ship-to State(" + state + ") does not exist");
                                log.info(".... Ship-to State(" + state + ") does not exist");
                            }
                            else {
                                throw e;
                            }
                        }
                    }
                  
                                        
                    // If local exposure zone is not blank, remove the exposure zone
                    if (!StringUtils.isEmpty(localExposureZone)) {
                        publish(".... Removing Customer Local Exposure Zone(" + localExposureZone + ")");
                        log.info(".... Removing Customer Local Exposure Zone(" + localExposureZone + ")");
                        List<CertCaptureCustomerExposureZoneBean> exposureZones = new ArrayList();
                        CertCaptureCustomerExposureZoneBean exposureZone = new CertCaptureCustomerExposureZoneBean();
                        
                        // Assume initials if state is 2 characters
                        if (localExposureZone.length() ==2) {
                            exposureZone.setInitials(localExposureZone);
                        } else {
                            exposureZone.setName(localExposureZone);
                        }
                        exposureZones.add(exposureZone);
                        try {
                            ccCustomer.removeCustomerLocalExposureZones(certCaptureClientId, customer.getId(), exposureZones);
                            publish(".... Removed Customer Local Exposure Zone(" + localExposureZone + ")");
                            log.info(".... Removed Customer Local Exposure Zone(" + localExposureZone + ")");
                        } catch (Exception e) {
                            if (e.getMessage().contains("code=400 reason=Bad Request")) {
                                publish(".... Local Exposure Zone(" + localExposureZone + ") does not exist");
                                log.info(".... Local Exposure Zone(" + localExposureZone + ") does not exist");
                            }
                            else {
                                throw e;
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
            if (cell.getCellType().equals(CellType.STRING) && "Customer Number".equalsIgnoreCase(cell.getStringCellValue())) {
                customerNumberColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate ID".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateIdColumn = cell.getColumnIndex();
           } else if (cell.getCellType().equals(CellType.STRING) && 
                   ("State".equalsIgnoreCase(cell.getStringCellValue()) ||
                   "Certificate Exposure Zone".equalsIgnoreCase(cell.getStringCellValue()))  ) {
                stateColumn = cell.getColumnIndex();
           } else if (cell.getCellType().equals(CellType.STRING) && "Local Exposure Zone".equalsIgnoreCase(cell.getStringCellValue())) {
               localExposureZoneColumn = cell.getColumnIndex();
           }
        }

        // Validate headers
        int numMissingHeaders = 0;
        String errorMessage = "The following column headers are missing: ";
        if (customerNumberColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Customer Number\"";
            numMissingHeaders++;
        }
        if (certificateIdColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate ID\"";
            numMissingHeaders++;
        }

        if (stateColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"State\"";
            numMissingHeaders++;
        }

        // local exposure zone not required.
        
        // Sold To Customer Number, Sold To Customer Name, Exclude From Mailing Campaign optional
        
        if (numMissingHeaders > 0) {
            return errorMessage;
        }

        return null;
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
