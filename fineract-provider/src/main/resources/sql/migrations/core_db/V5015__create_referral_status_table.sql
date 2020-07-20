--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

CREATE TABLE referral_status (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    client_id bigint(20) NOT NULL,
    status VARCHAR(100) NOT NULL,
    phone_no VARCHAR(100),
    email VARCHAR(250),
    device_id VARCHAR(250),
    last_saved date,
    PRIMARY KEY (id),
    CONSTRAINT referral_status_client FOREIGN KEY (client_id) REFERENCES m_client (id)
)
