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
package org.apache.fineract.portfolio.savings.classification.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.classification.data.TransactionClassificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class TransactionClassificationReadPlatformServiceJPAImpl implements TransactionClassificationReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionClassificationMapper transactionClassificationMapper = new TransactionClassificationMapper();;

    @Autowired
    public TransactionClassificationReadPlatformServiceJPAImpl(PlatformSecurityContext context, final RoutingDataSource dataSource) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Collection<TransactionClassificationData> getTransactionClassification() {
        this.context.authenticatedUser();

        return this.jdbcTemplate.query(this.transactionClassificationMapper.schema(), this.transactionClassificationMapper,
                new Object[] {});
    }

    private static final class TransactionClassificationMapper implements RowMapper<TransactionClassificationData> {

        private final String schemaSql = "select mts.id classificationId,mts.classification_name as classification,mts.operator_name as operator from m_transaction_classification mts";

        public String schema() {
            return this.schemaSql;
        }

        @SuppressWarnings("unused")
        @Override
        public TransactionClassificationData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long classificationId = rs.getLong("classificationId");
            final String classification = rs.getString("classification");
            final String operator = rs.getString("operator");
            return new TransactionClassificationData(classificationId, classification, operator);
        }

    }

}
