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

CREATE TABLE `m_transaction_classification` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`classification_name` VARCHAR(50) NOT NULL,
	`operator_name` VARCHAR(50) NOT NULL,
	PRIMARY KEY (`id`)
);

INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Airtime and Data', 'AIRTEL');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Airtime and Data', 'MTN');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Airtime and Data', 'GLO');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Airtime and Data', '9MOBILE');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'PHCN Postpaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'PHCN Prepaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Enugu State Water Co');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'KEDCO PostPaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Benin Electriicty Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ikeja Electricity Prepaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ibadan Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Enugu Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'KEDCO Prepaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ikeja Electricity Postpaid');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'PortHarcourt Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Eko Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Yola Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ogun State Water Corporation');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Lagos Water Corporation');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Kaduna Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Jos Electricity Distribution Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ikeja Electric Non-Energy Payments');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'ICE Commercial Power');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'FCT Water Board');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'DSTV Subscription');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'MyTV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Startimes Payments');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Infinity TV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Play Subscription');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'TrendTV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'GoTV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Cable Africa Network TV(CanTV)');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'DSTV Box Office Wallet Topup');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Montage Cable TV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'iROKOtv');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'African Cable Television');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Montage Decoder Starter Pack');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Kwese TV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'TSTV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Platinum Plus TV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Linda Ikeji TV');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'iPNX Subscription');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Swift 4G Subscription');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Smile Bundle');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Spectranet Limited');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Coollink');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Airtel MyPlan');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Wakanow');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'AERO Book-On-Hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Medview Airlines');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Hak Air Book-On-Hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Dana Air');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Discovery Air Book-on-hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Arik Air Book-On-Hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Asky Air Mobile Book on Hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'RwandAir Book on Hold');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Africa World Airlines');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Air Peace');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'AZMAN AIRLINE');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Emirates');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'South African Airlines');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Lekki Concession Company');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'LCC Agent Payment');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'DHL shipping payments');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Fuel Voucher');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Total Cards');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Animal Care');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Lafarge Africa Plc');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'United Cement Company of Nigeria');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ashaka Cement PLC');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'PayIT Wallet');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Rivers State Collections');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Lagos State Polytechnic');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Ajayi Crowther University');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Corona Schools');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Lagos Business School');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Loyola Jesuit College');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Adeleke University Ede');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Crescent University, Abeokuta');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'MYBETCITY');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Bet9ja');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'PLUSBET-PIN');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'UBC365');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Rukkabet');
INSERT INTO `m_transaction_classification` (`classification_name`, `operator_name`) VALUES ('Bill Payment', 'Bet9ja Wallet');

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'DELETE_TRANSACTIONCLASSIFICATION', 'TRANSACTIONCLASSIFICATION', 'DELETE');
INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'CREATE_TRANSACTIONCLASSIFICATION', 'TRANSACTIONCLASSIFICATION', 'CREATE');
INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'READ_TRANSACTIONCLASSIFICATION', 'TRANSACTIONCLASSIFICATION', 'READ');