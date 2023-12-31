/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.report.service;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.dataqueries.data.ReportDatabaseTypeEnumData;
import org.apache.fineract.infrastructure.dataqueries.service.ReadReportingService;
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.FileSystemContentRepository;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.config.VfdServiceApi;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelReportUtil;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ReportService(type = "Pentaho")
public class PentahoReportingProcessServiceImpl implements ReportingProcessService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PentahoReportingProcessServiceImpl.class);
	public static final String MIFOS_BASE_DIR = System.getProperty("user.home") + File.separator + ".mifosx";

	private final PlatformSecurityContext context;
	private final boolean noPentaho;
	
	private final BasicAuthTenantDetailsService basicAuthTenantDetailsService;
	
	private final ReadReportingService readReportingService;

	private final VfdServiceApi vfdServiceApi;

	private final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper;

	@Autowired
	private Environment env;

	@Autowired
	public PentahoReportingProcessServiceImpl(final PlatformSecurityContext context,final BasicAuthTenantDetailsService basicAuthTenantDetailsService,
	        final ReadReportingService readReportingService, final VfdServiceApi vfdServiceApi,
											  final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper) {
		ClassicEngineBoot.getInstance().start();
		this.noPentaho = false;
		this.context = context;
		this.basicAuthTenantDetailsService = basicAuthTenantDetailsService;
		this.readReportingService = readReportingService;
		this.vfdServiceApi = vfdServiceApi;
		this.savingAccountRepositoryWrapper = savingAccountRepositoryWrapper;
	}

	@Override
	public Response processRequest(final String reportName, final MultivaluedMap<String, String> queryParams) {

		final String outputTypeParam = queryParams.getFirst("output-type");
		final Map<String, String> reportParams = getReportParams(queryParams);
		final Locale locale = ApiParameterHelper.extractLocale(queryParams);

		String outputType = "HTML";
		if (StringUtils.isNotBlank(outputTypeParam)) {
			outputType = outputTypeParam;
		}

		if (!(outputType.equalsIgnoreCase("HTML") || outputType.equalsIgnoreCase("PDF") || outputType.equalsIgnoreCase("XLS")
				|| outputType.equalsIgnoreCase("XLSX") || outputType.equalsIgnoreCase("CSV"))) {
			throw new PlatformDataIntegrityException(
					"error.msg.invalid.outputType", "No matching Output Type: " + outputType);
		}

		if (this.noPentaho) {
			throw new PlatformDataIntegrityException("error.msg.no.pentaho", "Pentaho is not enabled",
					"Pentaho is not enabled");
		}

		final String reportPath = MIFOS_BASE_DIR + File.separator + "pentahoReports" + File.separator + reportName + ".prpt";
		LOGGER.info("Report path: {}", reportPath);

		// load report definition
		final ResourceManager manager = new ResourceManager();
		manager.registerDefaults();
		Resource res;

		try {
			res = manager.createDirectly(reportPath, MasterReport.class);
			final MasterReport masterReport = (MasterReport) res.getResource();
			final DefaultReportEnvironment reportEnvironment = (DefaultReportEnvironment) masterReport.getReportEnvironment();
			if (locale != null) {
				reportEnvironment.setLocale(locale);
			}
			addParametersToReport(masterReport, reportParams, reportName);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			String userPassword;
			String ownerPassword;
			if ("PDF".equalsIgnoreCase(outputType)) {
				PdfReportUtil.createPDF(masterReport, baos);
				ByteArrayOutputStream signedBaos = baos;

				// sign pdf
				if( ApiParameterHelper.signPdf(queryParams) ){
					signedBaos = this.signPdf(signedBaos,  reportName);
				}

				ByteArrayOutputStream protectedBaos = signedBaos;

				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					ownerPassword = userPassword;
					protectedBaos = this.protectPdf(protectedBaos, userPassword, ownerPassword);
				}

				return Response.ok().entity(protectedBaos.toByteArray()).type("application/pdf").build();
			}

			if ("XLS".equalsIgnoreCase(outputType)) {
				ExcelReportUtil.createXLS(masterReport, baos);
				ByteArrayOutputStream protectedBaos = baos;
				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					protectedBaos = this.protectExcel(baos, userPassword);
				}
				return Response.ok().entity(protectedBaos.toByteArray()).type("application/vnd.ms-excel")
						.header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".xls").build();
			}

			if ("XLSX".equalsIgnoreCase(outputType)) {
				ExcelReportUtil.createXLSX(masterReport, baos);
				ByteArrayOutputStream protectedBaos = baos;
				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					protectedBaos = this.protectExcel(baos, userPassword);
				}
				return Response.ok().entity(protectedBaos.toByteArray()).type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
						.header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".xlsx").build();
			}

			if ("CSV".equalsIgnoreCase(outputType)) {
				CSVReportUtil.createCSV(masterReport, baos, "UTF-8");
				return Response.ok().entity(baos.toByteArray()).type("text/csv")
						.header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".csv").build();
			}

			if ("HTML".equalsIgnoreCase(outputType)) {
				HtmlReportUtil.createStreamHTML(masterReport, baos);
				return Response.ok().entity(baos.toByteArray()).type("text/html").build();
			}
		} catch (final ResourceException | ReportProcessingException | IOException e) {
			throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
		}

		throw new PlatformDataIntegrityException("error.msg.invalid.outputType", "No matching Output Type: " + outputType);
	}

	private void addParametersToReport(final MasterReport report, final Map<String, String> queryParams, String reportName) {

		final AppUser currentUser = this.context.authenticatedUser();

		try {

			final ReportParameterValues rptParamValues = report.getParameterValues();
			final ReportParameterDefinition paramsDefinition = report.getParameterDefinition();

            /*
			 * only allow integer, long, date and string parameter types and
             * assume all mandatory - could go more detailed like Pawel did in
             * Mifos later and could match incoming and pentaho parameters
             * better... currently assuming they come in ok... and if not an
             * error
             */
			for (final ParameterDefinitionEntry paramDefEntry : paramsDefinition.getParameterDefinitions()) {
				final String paramName = paramDefEntry.getName();
				final String pValue = queryParams.get(paramName);
				if (!((paramName.equals("tenantUrl")) || (paramName.equals("userhierarchy") || (paramName.equals("username")) || (paramName
						.equals("password") || (paramName.equals("userid")))) || (StringUtils.isBlank(pValue) && (paramName.equals("startDate") || paramName.equals("endDate"))))) {
					LOGGER.info("paramName:" + paramName);
					if (StringUtils.isBlank(pValue)) {
						throw new PlatformDataIntegrityException("error.msg.reporting.error",
								"Pentaho Parameter: " + paramName + " - not Provided");
					}

					final Class<?> clazz = paramDefEntry.getValueType();
					LOGGER.info("addParametersToReport(" + paramName + " : " + pValue + " : " + clazz.getCanonicalName() + ")");
					if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Integer")) {
						rptParamValues.put(paramName, Integer.parseInt(pValue));
					} else if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Long")) {
						rptParamValues.put(paramName, Long.parseLong(pValue));
					} else if (clazz.getCanonicalName().equalsIgnoreCase("java.sql.Date")) {
						rptParamValues.put(paramName, Date.valueOf(pValue));
					} else {
						rptParamValues.put(paramName, pValue);
					}
				}
			}

			// tenant database name and current user's office hierarchy
			// passed as parameters to allow multitenant penaho reporting
			// and
			// data scoping
			final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
			final FineractPlatformTenantConnection tenantConnection = tenant.getConnection();
			
			String tenantUrl = null;
			ReportDatabaseTypeEnumData databaseType = readReportingService.getReportDatabaseType(reportName);
			if (databaseType != null && databaseType.isMysql()) {
				tenantUrl = "jdbc:mysql://" + tenantConnection.getSchemaServer() + ":" + tenantConnection.getSchemaServerPort() + "/" + tenantConnection.getSchemaName();

				rptParamValues.put("username", tenantConnection.getSchemaUsername());
				rptParamValues.put("password", tenantConnection.getSchemaPassword());

			} else if (databaseType != null && databaseType.isPostgres()) {// postgres, redshfit
				final FineractPlatformTenantConnection postgresConnection = basicAuthTenantDetailsService.getExtraDatabaseReportConnection(tenant.getTenantIdentifier());
				LOGGER.info("redshift db URL: {}", tenant.getTenantIdentifier());

				if (postgresConnection.getSchemaServer() == null ||
						postgresConnection.getSchemaServerPort() == null || postgresConnection.getSchemaName() == null) {
					LOGGER.error("error.msg.reporting.error: Connection properties to extrat report database are empty");
					throw new PlatformDataIntegrityException("error.msg.reporting.error", "Connection properties to extra report database are empty");
				}
				tenantUrl = "jdbc:postgresql://" + postgresConnection.getSchemaServer() + ":" + postgresConnection.getSchemaServerPort() + "/" + postgresConnection.getSchemaName();

				rptParamValues.put("username", postgresConnection.getSchemaUsername());
				rptParamValues.put("password", postgresConnection.getSchemaPassword());
			}
                        
			final String userhierarchy = currentUser.getOffice().getHierarchy();
			LOGGER.info("db URL:" + tenantUrl + "      userhierarchy:" + userhierarchy);
			rptParamValues.put("userhierarchy", userhierarchy);

			final Long userid = currentUser.getId();
			LOGGER.info("db URL:" + tenantUrl + "      userid:" + userid);
			rptParamValues.put("userid", userid);

			rptParamValues.put("tenantUrl", tenantUrl);
			
		} catch (final Exception e) {
			LOGGER.error("error.msg.reporting.error:" + e.getMessage());
			throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
		}
	}

	private Map<String, String> getReportParams(final MultivaluedMap<String, String> queryParams) {

		final Map<String, String> reportParams = new HashMap<>();
		final Set<String> keys = queryParams.keySet();
		String pKey;
		String pValue;
		for (final String k : keys) {

			if (k.startsWith("R_")) {
				pKey = k.substring(2);
				pValue = queryParams.get(k).get(0);
				reportParams.put(pKey, pValue);
			}
		}
		return reportParams;
	}

	private ByteArrayOutputStream protectPdf(ByteArrayOutputStream pdfByteArray, String userPassword, String ownerPassword) throws IOException {

		if(pdfByteArray == null){
			throw new IOException("Password Protecting PDF: " + " Byte array cannot be NULL");
		}
		PDDocument document = PDDocument.load(pdfByteArray.toByteArray());

		AccessPermission accessPermission = new AccessPermission();

		StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword,userPassword,accessPermission);

		//Setting the length of the encryption key
		spp.setEncryptionKeyLength(128);

		//Setting the access permissions
		spp.setPermissions(accessPermission);

		//Protecting the document
		document.protect(spp);

		ByteArrayOutputStream newBaos = new ByteArrayOutputStream();
		document.save(newBaos);
		document.close();
		return newBaos;
	}

	private ByteArrayOutputStream protectExcel(ByteArrayOutputStream pdfByteArray, String userPassword) throws IOException {

		if(pdfByteArray == null){
			throw new IOException("Password Protecting EXCEL: " + " Byte array cannot be NULL");
		}

		//sets the password that will be used for protecting the workbook
		Biff8EncryptionKey.setCurrentUserPassword(userPassword);
		HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(pdfByteArray.toByteArray()), true);

		//call the function that write protects the workbook
		wb.writeProtectWorkbook(Biff8EncryptionKey.getCurrentUserPassword(), "");

		ByteArrayOutputStream outputByteArray = new ByteArrayOutputStream();

		wb.write(outputByteArray);

		// VERY IMPORTANT, RESET THE ENCRYPTION KEY WHEN DONE
		Biff8EncryptionKey.setCurrentUserPassword(null);

		return outputByteArray;
	}

	private ByteArrayOutputStream signPdf(ByteArrayOutputStream pdfByteArray, String documentName) throws IOException {
		if(pdfByteArray == null){
			throw new IOException("Signing PDF Document : " + " Byte array cannot be NULL");
		}

		PDDocument document = PDDocument.load(pdfByteArray.toByteArray());
		String signaturePath = this.env.getProperty("VFD_PDF_SIGNATURE_PATH");

		if(!StringUtils.isNotBlank(signaturePath)){
			return pdfByteArray;
		}
		PDImageXObject pdImage = PDImageXObject.createFromFile(signaturePath, document);

		for (PDPage page: document.getPages()){
			PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
			contentStream.drawImage(pdImage, 80, 270, 100, 50);

			PDFont pdfFont= PDType1Font.HELVETICA_BOLD;
			int fontSize = 14;
			contentStream.setFont(pdfFont, fontSize);
			contentStream.beginText();
			contentStream.newLineAtOffset(200,270);
			contentStream.showText(DateUtils.getLocalDateTimeOfTenant().toString("dd MMMM yyyy"));
			contentStream.endText();

			contentStream.close();
		}

		ByteArrayOutputStream newPdfByteArray = new ByteArrayOutputStream();
		document.save(newPdfByteArray);
		document.close();

		return newPdfByteArray;
	}

	@Override
	public void processAndSendStatement(String reportName, MultivaluedMap<String, String> queryParams) {
		LOGGER.info("Starting processAndSendStatement, report processing started ");

		final String outputTypeParam = queryParams.getFirst("output-type");
		final Map<String, String> reportParams = getReportParams(queryParams);
		final Locale locale = ApiParameterHelper.extractLocale(queryParams);
		final String toAddress = queryParams.getFirst("toAddress");
		final String startDate = reportParams.get("startDate");
		final String endDate = reportParams.get("endDate");
		String savingsAccountIdString = reportParams.get("savingsAccountId");

		if(StringUtils.isBlank(savingsAccountIdString)){
			List<ApiParameterError> errors = new ArrayList<>();
			ApiParameterError error = ApiParameterError.parameterError("validation.msg.validation.errors.exist",
					"Savings Account Id cannot be null","savingsAccountId");
			errors.add(error);

			throw new PlatformApiDataValidationException(errors);
		}

		final SavingsAccount account = this.savingAccountRepositoryWrapper.findByAccountNumberWithNotFoundDetection(savingsAccountIdString);

		String outputType = "HTML";
		if (StringUtils.isNotBlank(outputTypeParam)) {
			outputType = outputTypeParam;
		}

		if (!(outputType.equalsIgnoreCase("HTML") || outputType.equalsIgnoreCase("PDF") || outputType.equalsIgnoreCase("XLS")
				|| outputType.equalsIgnoreCase("XLSX") || outputType.equalsIgnoreCase("CSV"))) {
			throw new PlatformDataIntegrityException(
					"error.msg.invalid.outputType", "No matching Output Type: " + outputType);
		}

		if (this.noPentaho) {
			throw new PlatformDataIntegrityException("error.msg.no.pentaho", "Pentaho is not enabled",
					"Pentaho is not enabled");
		}

		final String reportPath = MIFOS_BASE_DIR + File.separator + "pentahoReports" + File.separator + reportName + ".prpt";
		LOGGER.info("Report path: {}", reportPath);

		// load report definition
		final ResourceManager manager = new ResourceManager();
		manager.registerDefaults();
		Resource res;

		try {
			res = manager.createDirectly(reportPath, MasterReport.class);
			final MasterReport masterReport = (MasterReport) res.getResource();
			final DefaultReportEnvironment reportEnvironment = (DefaultReportEnvironment) masterReport.getReportEnvironment();
			if (locale != null) {
				reportEnvironment.setLocale(locale);
			}
			addParametersToReport(masterReport, reportParams, reportName);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			String userPassword;
			String ownerPassword;
			String statementName = "Vbank_Statement_" + startDate + " - " + endDate;
			if ("PDF".equalsIgnoreCase(outputType)) {
				LOGGER.info("PDF statement processing ");

				PdfReportUtil.createPDF(masterReport, baos);
				ByteArrayOutputStream signedBaos = baos;

				// sign pdf
				if( ApiParameterHelper.signPdf(queryParams) ){
					signedBaos = this.signPdf(signedBaos,  reportName);
				}

				ByteArrayOutputStream protectedBaos = signedBaos;

				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					ownerPassword = userPassword;
					protectedBaos = this.protectPdf(protectedBaos, userPassword, ownerPassword);
				}

				LOGGER.info("Statement processed... sending... to VFD email service");
				File file  = this.generateReportFile(protectedBaos, statementName, "pdf");

				this.vfdServiceApi.sendSavingsAccountStatementEmail(toAddress, account.clientId(),
						statementName, "application/pdf", file);

				LOGGER.info("Statement processing and sending is done: ");
				return;
			}

			if ("XLS".equalsIgnoreCase(outputType)) {
				ExcelReportUtil.createXLS(masterReport, baos);
				ByteArrayOutputStream protectedBaos = baos;
				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					protectedBaos = this.protectExcel(baos, userPassword);
				}
				File file  = this.generateReportFile(protectedBaos, statementName, "xls");

				this.vfdServiceApi.sendSavingsAccountStatementEmail(toAddress, account.clientId(),
						statementName, "application/vnd.ms-excel",file);

				LOGGER.info("Statement processing and sending is done: ");
				return;
			}

			if ("XLSX".equalsIgnoreCase(outputType)) {
				ExcelReportUtil.createXLSX(masterReport, baos);
				ByteArrayOutputStream protectedBaos = baos;
				if(ApiParameterHelper.addPassword(queryParams) && ApiParameterHelper.savingsAccountId(queryParams)){
					userPassword = StringUtils.substring(ApiParameterHelper.getSavingsAccountId(queryParams), -6);
					protectedBaos = this.protectExcel(baos, userPassword);
				}

				File file  = this.generateReportFile(protectedBaos, statementName, "xlsx");
				this.vfdServiceApi.sendSavingsAccountStatementEmail(toAddress, account.clientId(),
						statementName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",file);

				LOGGER.info("Statement processing and sending is done: ");
				return;
			}

			if ("CSV".equalsIgnoreCase(outputType)) {
				CSVReportUtil.createCSV(masterReport, baos, "UTF-8");
			}

			if ("HTML".equalsIgnoreCase(outputType)) {
				HtmlReportUtil.createStreamHTML(masterReport, baos);
			}
		} catch (final ResourceException | ReportProcessingException | IOException e) {
			LOGGER.error("Error processing report: ", e);
			throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
		}

		throw new PlatformDataIntegrityException("error.msg.invalid.outputType", "No matching Output Type: " + outputType);
	}

	private File generateReportFile(final ByteArrayOutputStream byteArrayOutputStream, final String reportName, final String fileFormat) {

		try {

			final String fileLocation = FileSystemContentRepository.FINERACT_BASE_DIR + File.separator + "";
			final String fileNameWithoutExtension = fileLocation + File.separator + reportName;

			// check if file directory exists, if not create directory
			if (!new File(fileLocation).isDirectory()) {
				new File(fileLocation).mkdirs();
			}

			if (byteArrayOutputStream.size() == 0) {
				LOGGER.error("Pentaho report processing failed, empty output stream created");
				throw new PlatformDataIntegrityException("error.msg.reporting.error", "Pentaho report processing failed, empty output stream created");
			} else if ((byteArrayOutputStream.size() > 0)) {
				final String fileName = fileNameWithoutExtension + "." + fileFormat;

				final File file = new File(fileName);
				final FileOutputStream outputStream = new FileOutputStream(file);
				byteArrayOutputStream.writeTo(outputStream);

				return file;
			}

		} catch (IOException | PlatformDataIntegrityException e) {
			LOGGER.error("error.msg.reporting.error:" + " PentahoReportingProcessServiceImpl.generateReportFile threw and Exception " + e.getMessage());
			throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
		}

		return null;
	}

}