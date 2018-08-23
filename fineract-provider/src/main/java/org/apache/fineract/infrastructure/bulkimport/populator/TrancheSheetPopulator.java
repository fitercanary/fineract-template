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
package org.apache.fineract.infrastructure.bulkimport.populator;

import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class TrancheSheetPopulator extends AbstractWorkbookPopulator {

	private static final int CLIENT_COL = 0;
	private static final int LOAN_EXTERNAL_ID_COL = 1;
	private static final int EXPECTED_DISBURSEMENT_DATE_COL = 2;
	private static final int DISBURSEMENT_DATE_COL = 3;
	private static final int PRINCIPAL_COL = 4;

	public TrancheSheetPopulator() {
	}

	@Override
	public void populate(final Workbook workbook, String dateFormat) {
		Sheet trancheSheet = workbook.createSheet(TemplatePopulateImportConstants.TRANCH_SHEET_NAME);
		setLayout(trancheSheet);
	}

	private void setLayout(Sheet worksheet) {
		worksheet.setColumnWidth(CLIENT_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
		worksheet.setColumnWidth(LOAN_EXTERNAL_ID_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
		worksheet.setColumnWidth(EXPECTED_DISBURSEMENT_DATE_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
		worksheet.setColumnWidth(DISBURSEMENT_DATE_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
		worksheet.setColumnWidth(PRINCIPAL_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
		Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
		rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
		writeString(CLIENT_COL, rowHeader, "Client");
		writeString(LOAN_EXTERNAL_ID_COL, rowHeader, "Loan External ID");
		writeString(EXPECTED_DISBURSEMENT_DATE_COL, rowHeader, "Expected Disbursement Date");
		writeString(DISBURSEMENT_DATE_COL, rowHeader, "Disbursement Date");
		writeString(PRINCIPAL_COL, rowHeader, "Principal");

	}

}