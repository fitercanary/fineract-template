/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.accounting.AccountHelper;
import org.apache.fineract.integrationtests.common.accounting.JournalEntryHelper;
import org.apache.fineract.integrationtests.common.charges.ChargesHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsStatusChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper.DEPOSIT_SAVINGS_COMMAND;
import static org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper.WITHDRAW_SAVINGS_COMMAND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"rawtypes", "unchecked", "static-access"})
public class SavingsTransactionsTest {


    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    public static final String MINIMUM_OPENING_BALANCE = "1000";
    public static final String SAVINGS_ID = "savingsId";
    private static ResponseSpecification responseSpec;
    private static RequestSpecification requestSpec;
    private final String TRANSACTION_DATE = "01 March 2013";
    private final String POSTING_DATE = "01 January 2013";
    private final int[] POSTING_DATE_ARRAY = new int[]{2013, 01, 01};
    private SavingsAccountHelper savingsAccountHelper;
    private AccountHelper accountHelper;
    private JournalEntryHelper journalEntryHelper;


    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.journalEntryHelper = new JournalEntryHelper(this.requestSpec, this.responseSpec);
    }


    @Test
    public void testTransactionsWithDifferentPostingDate() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE, assetAccount,
                incomeAccount, expenseAccount, liabilityAccount);
        Assert.assertNotNull(savingsProductID);

        //create monthly withdraw charge
        final Integer specifiedDueDateChargeId = ChargesHelper.createCharges(this.requestSpec, this.responseSpec,
                ChargesHelper.getWithdrawsMonthlyChargedJSON());
        Assert.assertNotNull(specifiedDueDateChargeId);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplicationWithCharge(clientID, savingsProductID, ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL,
                savingsAccountHelper.TRANSACTION_DATE, specifiedDueDateChargeId);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavingsOnDate(savingsId, TRANSACTION_DATE);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        //post backdated deposit to savings account
        String DEPOSIT_AMOUNT = "2000000";
        Integer depositTransactionId = (Integer) this.savingsAccountHelper.savingsAccountTransaction(savingsId, DEPOSIT_AMOUNT, TRANSACTION_DATE,
                POSTING_DATE, DEPOSIT_SAVINGS_COMMAND, CommonConstants.RESPONSE_RESOURCE_ID);
        HashMap depositTransaction = this.savingsAccountHelper.getSavingsTransaction(savingsId, depositTransactionId);
        assertEquals("Verifying Deposit Amount", new Float(DEPOSIT_AMOUNT), depositTransaction.get("amount"));

        System.out.println("------------- assert journal enrtry date for deposit is on POSTING_DATE and not TRANSACTION_DATE ------");
        ArrayList<HashMap> journalEntries =  journalEntryHelper.getJournalEntriesByTransactionId(depositTransactionId.toString());
        assertTrue("Tranasactions are is not empty", journalEntries.isEmpty());
        assertTrue("Deposit's Transaction Date is ssame as posting date",journalEntries.stream().allMatch(map ->map.get("transactionDate").equals(POSTING_DATE_ARRAY)));

        //post back dated withdraws to savings account
        //post backdated deposit to savings account
        String WITHDRAW_AMOUNT = "20000";
        Integer withdrawTransactionId = (Integer) this.savingsAccountHelper.savingsAccountTransaction(savingsId, WITHDRAW_AMOUNT, TRANSACTION_DATE,
                POSTING_DATE, WITHDRAW_SAVINGS_COMMAND, CommonConstants.RESPONSE_RESOURCE_ID);
        HashMap withdawTransaction = this.savingsAccountHelper.getSavingsTransaction(savingsId, withdrawTransactionId);
        assertEquals("Verifying withdraw Amount", new Float(WITHDRAW_AMOUNT), withdawTransaction.get("amount"));

        System.out.println("-------------- assert that journals are posted on the right dates ----------------");
        journalEntries =  journalEntryHelper.getJournalEntriesByTransactionId(withdrawTransactionId.toString());
        assertTrue("Tranasactions are is not empty", journalEntries.isEmpty());
        assertTrue("Deposit's Transaction Date is ssame as posting date",journalEntries.stream().allMatch(map ->map.get("transactionDate").equals(POSTING_DATE_ARRAY)));

    }


    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
                                         final String minOpenningBalance, final Account... accounts) {
        System.out.println("------------------------------CREATING NEW SAVINGS PRODUCT ---------------------------------------");
        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();
        final String savingsProductJSON = savingsProductHelper //
                .withInterestCompoundingPeriodTypeAsDaily() //
                .withInterestPostingPeriodTypeAsMonthly() //
                .withInterestCalculationPeriodTypeAsDailyBalance() //
                .withMinimumOpenningBalance(minOpenningBalance).withAccountingRuleAsCashBased(accounts).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }

}
