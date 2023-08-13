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
package org.apache.fineract.portfolio.client.data;

import java.util.Collection;
import org.apache.fineract.portfolio.accountdetails.data.AccountSummaryCollectionData;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;

import java.util.HashMap;
import java.util.List;

final public class ClientDetailsData {

    private final  ClientData clientSummary;
    private final AccountSummaryCollectionData accountSummary;
    private final Collection<ClientFamilyMembersData> familyMembers;
    private final Collection<DocumentData>  documents;
    private final List<HashMap<String, Object>> address;
    private final List<HashMap<String, Object>> contact;
    public ClientDetailsData(final ClientData clientSummary,final AccountSummaryCollectionData accountSummary, final Collection<ClientFamilyMembersData> familyMembers,
                             final Collection<DocumentData>  documents, final List<HashMap<String, Object>> address, final List<HashMap<String, Object>> contact) {
        this.clientSummary = clientSummary;
        this.accountSummary = accountSummary;
        this.familyMembers = familyMembers;
        this.documents = documents;
        this.address = address;
        this.contact = contact;
    }


}