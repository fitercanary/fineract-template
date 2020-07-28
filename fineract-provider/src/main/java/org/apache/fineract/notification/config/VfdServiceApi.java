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

import java.util.Arrays;

import org.apache.fineract.notification.domain.VfdTransferNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class VfdServiceApi {
    
    private final String NOTIFICATION_SERVICE_DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook?alertType=both";
    private final String NOTIFICATION_SERVICE_AUTH_TOKEN_FIELD = "VFDBankAuth";
    private final String NOTIFICATION_SERVICE_DEFAULT_AUTH_TOKEN = "Bearer 73d64eb6-15fd-35df-a543-4a5ae672c455";

    private final String EMAIL_SERVICE_DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook?alertType=both";

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

    public ResponseEntity<String> sendEmail(MultiValueMap<String, Object> body){

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        //headers.add(AUTH_TOKEN_FIELD, DEFAULT_AUTH_TOKEN);
        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(EMAIL_SERVICE_DEFAULT_URL, requestEntity, String.class);
    }
}
