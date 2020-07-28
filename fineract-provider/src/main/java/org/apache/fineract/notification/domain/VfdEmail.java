package org.apache.fineract.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VfdEmail {

    private ByteArrayOutputStream file;
    private String attachmentType;
    private Date startDate;
    private Date endDate;
    private Integer clientId;
    private String accountNo;

    public VfdEmail(ByteArrayOutputStream file, String attachmentType, Date startDate, Date endDate, Integer clientId, String accountNo) {
        this.file = file;
        this.attachmentType = attachmentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.clientId = clientId;
        this.accountNo = accountNo;
    }

    public ByteArrayOutputStream getFile() {
        return file;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Integer getClientId() {
        return clientId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    @Override
    public String toString() {
        return "VfdEmail{" +
                "file=" + file +
                ", attachmentType='" + attachmentType + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", clientId=" + clientId +
                ", accountNo='" + accountNo + '\'' +
                '}';
    }

    public static MultiValueMap<String, Object> toMultiValueMap(VfdEmail email, String fileName){

        // This nested HttpEntiy is important to create the correct
        // Content-Disposition entry with metadata "name" and "filename"
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<String, String>();
        fileMap.add("Content-Disposition", String.format("form-data; name=\"file\"; filename=\"%s\"", fileName));
        //"attachment; filename=" + "statement"
        //String.format("form-data; name=\"file\"; filename=\"%s\"", theMultipartFile.getName())

        HttpEntity<byte[]> fileEntity = new HttpEntity<>(email.getFile().toByteArray(), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileEntity);
        body.add("attachmentType", email.getAttachmentType());
        body.add("startDate", email.getStartDate());
        body.add("endDate", email.getEndDate());
        body.add("clientId", email.getClientId());
        body.add("accountNo", email.getAccountNo());

        return body;
    }
}
