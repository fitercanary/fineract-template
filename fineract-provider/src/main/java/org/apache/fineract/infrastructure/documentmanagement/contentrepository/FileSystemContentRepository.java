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
package org.apache.fineract.infrastructure.documentmanagement.contentrepository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.fineract.infrastructure.core.domain.Base64EncodedImage;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.data.FileData;
import org.apache.fineract.infrastructure.documentmanagement.data.ImageData;
import org.apache.fineract.infrastructure.documentmanagement.domain.StorageType;
import org.apache.fineract.infrastructure.documentmanagement.exception.ContentManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.pdf.codec.Base64;

public class FileSystemContentRepository implements ContentRepository {

    private final static Logger logger = LoggerFactory.getLogger(FileSystemContentRepository.class);

    public static final String FINERACT_BASE_DIR = System.getProperty("user.home") + File.separator + ".fineract";

    private final FileSystemContentPathSanitizer pathSanitizer;

    public FileSystemContentRepository(final FileSystemContentPathSanitizer pathSanitizer) {
        this.pathSanitizer = pathSanitizer;
    }

    @Override
    public String saveFile(final InputStream uploadedInputStream, final DocumentCommand documentCommand) {
        final String fileName = documentCommand.getFileName();
        final String uploadDocumentLocation = generateFileParentDirectory(documentCommand.getParentEntityType(),
                documentCommand.getParentEntityId());

        ContentRepositoryUtils.validateFileSizeWithinPermissibleRange(documentCommand.getSize(), fileName);

        writeFileToFileSystem(fileName, uploadedInputStream, uploadDocumentLocation);

        return uploadDocumentLocation + File.separator + fileName;
    }

    @Override
    public String saveImage(final InputStream uploadedInputStream, final Long resourceId, final String imageName, final Long fileSize) {
        final String uploadImageLocation = generateClientImageParentDirectory(resourceId);

        ContentRepositoryUtils.validateFileSizeWithinPermissibleRange(fileSize, imageName);
        makeDirectories(uploadImageLocation);

        writeFileToFileSystem(imageName, uploadedInputStream, uploadImageLocation);

        return uploadImageLocation + File.separator + imageName;
    }

    @Override
    public String saveImage(final Base64EncodedImage base64EncodedImage, final Long resourceId, final String imageName) {
        final String uploadImageLocation = generateClientImageParentDirectory(resourceId);

        makeDirectories(uploadImageLocation);

        final String fileLocation = uploadImageLocation + File.separator + imageName + base64EncodedImage.getFileExtension();
        try {
            final OutputStream out = new FileOutputStream(new File(fileLocation));
            final byte[] imgBytes = Base64.decode(base64EncodedImage.getBase64EncodedString());
            out.write(imgBytes);
            out.flush();
            out.close();
        } catch (final IOException ioe) {
            throw new ContentManagementException(imageName, ioe.getMessage());
        }
        return fileLocation;
    }

    @Override
    public void deleteImage(final Long resourceId, final String location) {
        final boolean fileDeleted = deleteFile(location);
        if (!fileDeleted) {
            // no need to throw an Error, simply log a warning
            logger.warn("Unable to delete image associated with clients with Id " + resourceId);
        }
    }

    @Override
    public void deleteFile(final String fileName, final String documentPath) {
        final boolean fileDeleted = deleteFile(documentPath);
        if (!fileDeleted) { throw new ContentManagementException(fileName, null); }
    }

    private boolean deleteFile(final String documentPath) {
        String path = pathSanitizer.sanitize(documentPath);

        final File fileToBeDeleted = new File(path);
        return fileToBeDeleted.delete();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILE_SYSTEM;
    }

    @Override
    public FileData fetchFile(final DocumentData documentData) {
        String path = pathSanitizer.sanitize(documentData.fileLocation());

        final File file = new File(path);
        return new FileData(file, documentData.fileName(), documentData.contentType());
    }

    @Override
    public ImageData fetchImage(final ImageData imageData) {
        String path = pathSanitizer.sanitize(imageData.location());

        final File file = new File(path);
        imageData.updateContent(file);
        return imageData;
    }

    /**
     * Generate the directory path for storing the new document
     * 
     * @param entityType
     * @param entityId
     * @return
     */
    private String generateFileParentDirectory(final String entityType, final Long entityId) {
        return FileSystemContentRepository.FINERACT_BASE_DIR + File.separator
                + ThreadLocalContextUtil.getTenant().getName().replaceAll(" ", "").trim() + File.separator + "documents" + File.separator
                + entityType + File.separator + entityId + File.separator + ContentRepositoryUtils.generateRandomString();
    }

    /**
     * Generate directory path for storing new Image
     */
    private String generateClientImageParentDirectory(final Long resourceId) {
        return FileSystemContentRepository.FINERACT_BASE_DIR + File.separator
                + ThreadLocalContextUtil.getTenant().getName().replaceAll(" ", "").trim() + File.separator + "images" + File.separator
                + "clients" + File.separator + resourceId;
    }

    /**
     * Recursively create the directory if it does not exist *
     */
    private void makeDirectories(final String directoryPath){
        Path path = Paths.get(directoryPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ContentManagementException(directoryPath, e.getMessage());
        }
    }

    private void writeFileToFileSystem(final String fileName, final InputStream uploadedInputStream, final String fileLocation) {
        try{
            String sanitizedPath = pathSanitizer.sanitize(fileLocation + File.separator + fileName);
            makeDirectories(fileLocation);
            FileUtils.copyInputStreamToFile(uploadedInputStream, new File(sanitizedPath));// NOSONAR
        } catch (final IOException ioException) {
            throw new ContentManagementException(fileName, ioException.getMessage());
        }
    }
}