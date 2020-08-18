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
package org.apache.fineract.notification.config;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.apache.fineract.notification.domain.VfdTransferNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VfdServiceApi {
    
    private final String NOTIFICATION_SERVICE_DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook?alertType=both";
    private final String NOTIFICATION_SERVICE_AUTH_TOKEN_FIELD = "VFDBankAuth";
    private final String NOTIFICATION_SERVICE_DEFAULT_AUTH_TOKEN = "Bearer 73d64eb6-15fd-35df-a543-4a5ae672c455";

    private final String EMAIL_SERVICE_DEFAULT_URL = "http://172.31.11.10:9092/notification/attachment";

    @Autowired
    private Environment env;

    public ResponseEntity<String> sendNotification(VfdTransferNotification notification) {

            String auth_token = this.env.getProperty("VFD_NOTIFICATION_SERVICE_AUTH_TOKEN");
            auth_token = auth_token == null ? NOTIFICATION_SERVICE_DEFAULT_AUTH_TOKEN : auth_token;

            String url = this.env.getProperty("VFD_NOTIFICATION_SERVICE_URL");
            url = url == null ? NOTIFICATION_SERVICE_DEFAULT_URL : url;

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.add(NOTIFICATION_SERVICE_AUTH_TOKEN_FIELD, NOTIFICATION_SERVICE_DEFAULT_AUTH_TOKEN);
            HttpEntity<VfdTransferNotification> request = new HttpEntity<>(notification, headers);

        return restTemplate.postForEntity(NOTIFICATION_SERVICE_DEFAULT_URL, request, String.class);
    }

    public ResponseEntity<String> sendSavingsAccountStatementEmail( String toAddress, Long clientId, String attachmentName, File file){

        RestTemplate restTemplate = new RestTemplate();

        LinkedMultiValueMap<String,Object> requestEntity = new LinkedMultiValueMap<>();
        requestEntity.add("clientId", clientId);
        requestEntity.add("toAddress", toAddress);
        requestEntity.add("attachmentName", attachmentName);
        requestEntity.add("file",new FileSystemResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<LinkedMultiValueMap<String,Object>> request = new HttpEntity(requestEntity, headers);

        String url = this.env.getProperty("VFD_EMAIL_SERVICE_URL");
        url = url != null ? url : EMAIL_SERVICE_DEFAULT_URL;

        final ResponseEntity<String> stringResponseEntity =
                restTemplate.postForEntity(url,  request, String.class);

        return stringResponseEntity;
    }
}
