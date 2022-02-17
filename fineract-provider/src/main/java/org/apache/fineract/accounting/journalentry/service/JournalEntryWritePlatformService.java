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
package org.apache.fineract.accounting.journalentry.service;

import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.provisioning.domain.ProvisioningEntry;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public interface JournalEntryWritePlatformService {

	CommandProcessingResult createJournalEntry(JsonCommand command, ArrayList<Long> savingsCreditTransactionId, ArrayList<Long> savingsDebitTransactionId);

	CommandProcessingResult revertJournalEntry(JsonCommand command);

	void createJournalEntriesForLoan(Map<String, Object> accountingBridgeData);

	void createJournalEntriesForSavings(Map<String, Object> accountingBridgeData, GLAccount glAccount, String note);

	void createJournalEntriesForClientTransactions(Map<String, Object> accountingBridgeData);

	CommandProcessingResult defineOpeningBalance(JsonCommand command);

	String revertProvisioningJournalEntries(final Date reversalTransactionDate, final Long entityId, final Integer entityType);

	String createProvisioningJournalEntries(ProvisioningEntry entry);

	String createSavingsProductProvisioningJournalEntries(ProvisioningEntry provisioningEntry);

	void createJournalEntriesForShares(Map<String, Object> accountingBridgeData);

	void revertShareAccountJournalEntries(final ArrayList<Long> transactionId, final Date transactionDate);
}
