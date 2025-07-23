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
 * $Log: CustomerCertLinkExcelFileSwingWorker.java,v $
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CustomerCertLinkExcelFileSwingWorker extends SwingWorker<Integer, String> {

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
    public CustomerCertLinkExcelFileSwingWorker(File excelFile, String companyName,
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

                    if (StringUtils.isEmpty(customerNumber)) {
                        publish(".... Customer Number is blank.  Skipping row.");
                        log.info(".... Customer Number is blank.  Skipping row.");
                        continue;
                    }
                    
                    if (StringUtils.isEmpty(certificateId)) {
                        publish(".... Certificate ID is blank.  Skipping row.");
                        log.info(".... Certificate ID is blank.  Skipping row.");
                        continue;
                    }
 
                    // - Look up customer.  If it doesn't exist, add it
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
                    
                    try {
                        publish(".... Looking up Certificate Id=" + certificateId);
                        log.info    (".... Looking up Certificate Id=" + certificateId);
                        certificate = ccCertificate.getCertificate(certCaptureClientId, certificateId);
                        if (certificate == null) {
                           publish(".... Certificate does not exist in CertCapture...skipping row");
                            log.info(".... Certificate does not exist in CertCapture...skipping row");
                            continue;

                        }
                    }    
                    catch (Exception e) {
                        if (e.getMessage().contains("Unable to find")) {
                            publish(".... Certificate does not exist in CertCapture...skipping row");
                            log.info(".... Certificate does not exist in CertCapture...skipping row");
                            continue;

                        }
                    }
                    
                    // Link Certificate/Customer
                    // Associate customer and certificate
                    publish(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                    log.info(".... Adding customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                    ccCertificate.addCertificateCustomer(certCaptureClientId, certificate.getId(), customer.getId());
                    publish(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");
                    log.info(".... Added customer(" + customer.getId() + ")/certificate(" + certificate.getId() + ") association");

                  
                    
                    
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
            if (cell.getCellType().equals(CellType.STRING) && "Billing Customer Number".equalsIgnoreCase(cell.getStringCellValue())) {
                customerNumberColumn = cell.getColumnIndex();
            } else if (cell.getCellType().equals(CellType.STRING) && "Certificate ID".equalsIgnoreCase(cell.getStringCellValue())) {
                certificateIdColumn = cell.getColumnIndex();
            }
        }

        // Validate headers
        int numMissingHeaders = 0;
        String errorMessage = "The following column headers are missing: ";
        if (customerNumberColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Billing Customer Number\"";
            numMissingHeaders++;
        }
        if (certificateIdColumn == -1) {
            errorMessage += (numMissingHeaders > 0 ? "," : "") + "\"Certificate ID\"";
            numMissingHeaders++;
        }


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
