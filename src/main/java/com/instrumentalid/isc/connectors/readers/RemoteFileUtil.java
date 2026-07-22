package com.instrumentalid.isc.connectors.readers;

import com.jcraft.jsch.*;
import openconnector.ConnectorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTP;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.azure.storage.blob.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;

/**
 * Utility class for copying files from various remote storage systems to a local directory.
 * TODO migrate this into one reader class per protocol
 * TODO support path patterns (e.g., dated files)
 */
public class RemoteFileUtil {

    private static final int CHANNEL_TIMEOUT = 5000;
    private static final int SESSION_TIMEOUT = 10000;
    private static final Log log = LogFactory.getLog(RemoteFileUtil.class);

    public static Path copyFromAzureBlob(ConnectorConfig config, File localDirectory) throws IOException {
        File localFile = new File(localDirectory, "file-" + UUID.randomUUID());

        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(config.getAzureConnectionString())
                .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(config.getAzureContainerName());
            BlobClient blobClient = containerClient.getBlobClient(config.getPath());

            if (!blobClient.exists()) {
                throw new ConnectorException("File not found in Azure Blob Storage");
            }

            blobClient.downloadToFile(localFile.getPath());
            return localFile.toPath();
        } catch (Exception e) {
            throw new ConnectorException("Could not retrieve file from Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    public static Path copyFromFTP(ConnectorConfig configuration, File localTemporaryDirectory) {
        try {
            File localFile = new File(localTemporaryDirectory, "file-" + UUID.randomUUID());
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(configuration.getHostname(), configuration.getPort());

                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    ftpClient.retrieveFile(configuration.getPath(), fos);
                }
            } finally {
                ftpClient.logout();
            }

            return localFile.toPath();
        } catch(IOException e) {
            throw new ConnectorException("Could not retrieve a file via FTP: " + e.getMessage(), e);
        }
    }

    public static Path copyFromFTPS(ConnectorConfig configuration, File localTemporaryDirectory) {
        try {
            File localFile = new File(localTemporaryDirectory, "file-" + UUID.randomUUID());
            FTPSClient ftpsClient = new FTPSClient(true);
            try {
                ftpsClient.connect(configuration.getHostname(), configuration.getPort());
                ftpsClient.login(configuration.getUsername(), configuration.getPassword());
                ftpsClient.enterLocalPassiveMode();
                ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    if (!ftpsClient.retrieveFile(configuration.getPath(), fos)) {
                        throw new ConnectorException("Failed to download file: " + ftpsClient.getReplyString());
                    }
                }
                return localFile.toPath();
            } finally {
                if (ftpsClient.isConnected()) {
                    ftpsClient.logout();
                    ftpsClient.disconnect();
                }
            }
        } catch (IOException e) {
            throw new ConnectorException("Could not retrieve file via FTPS: " + e.getMessage(), e);
        }
    }

    public static Path copyFromGoogleCloud(ConnectorConfig config, File localDirectory) throws IOException {
        File localFile = new File(localDirectory, "file-" + UUID.randomUUID());

        try {
            Storage storage = StorageOptions.newBuilder()
                .setProjectId(config.getGcpProjectId())
                .setCredentials(GoogleCredentials.fromStream(
                    new ByteArrayInputStream(config.getGcpCredentialsJson().getBytes(StandardCharsets.UTF_8))))
                .build()
                .getService();

            Blob blob = storage.get(BlobId.of(config.getGcsBucket(), config.getPath()));
            if (blob == null) {
                throw new ConnectorException("File not found in GCS bucket");
            }

            blob.downloadTo(localFile.toPath());
            return localFile.toPath();
        } catch (Exception e) {
            throw new ConnectorException("Could not retrieve file from Google Cloud Storage: " + e.getMessage(), e);
        }
    }

    public static Path copyFromHTTP(ConnectorConfig config, File localDirectory) throws IOException {
        // Implementation using Apache HttpClient
        return downloadFile(config, localDirectory);
    }

    public static Path copyFromS3(ConnectorConfig configuration, File localTemporaryDirectory) {
        try {
            S3Client s3Client;
            if (configuration.hasAwsCredentials()) {
                AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                    configuration.getAwsAccessKeyId(),
                    configuration.getAwsSecretKey()
                );
                s3Client = S3Client.builder()
                    .region(Region.of(configuration.getS3Region()))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            } else {
                // Use default credential provider chain (environment variables, instance profile, etc)
                s3Client = S3Client.builder()
                    .region(Region.of(configuration.getS3Region()))
                    .build();
            }

            log.debug("S3 URL: " + configuration.getPath());
            log.debug("S3 Bucket: " + configuration.getBucket());
            log.debug("S3 Region: " + configuration.getS3Region());

            File localFile = new File(localTemporaryDirectory, "file-" + UUID.randomUUID());
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(configuration.getBucket())
                    .key(configuration.getPath())
                    .build();

                s3Client.getObject(getObjectRequest, localFile.toPath());
                return localFile.toPath();
            } finally {
                s3Client.close();
            }
        } catch (Exception e) {
            log.error("Could not retrieve file from S3", e);
            throw new ConnectorException("Could not retrieve file from S3: " + e.getMessage(), e);
        }
    }

    public static Path copyFromSFTP(ConnectorConfig configuration, File localTemporaryDirectory) throws ConnectorException {
        try {
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession(configuration.getUsername(), configuration.getHostname(), configuration.getPort());

            jschSession.setConfig("StrictHostKeyChecking", configuration.getStrictHostKeyChecking() ? "yes" : "no");

            if (configuration.hasPasswordAuthentication()) {
                jschSession.setPassword(configuration.getPassword());
            } else {
                // Write the private key to a temp file
                Path privateKeyPath = Paths.get(localTemporaryDirectory.getAbsolutePath(), UUID.randomUUID().toString());
                Files.writeString(privateKeyPath, configuration.getPrivateKey());

                jsch.addIdentity(privateKeyPath.toString(), configuration.getPrivateKeyPassphrase());
            }

            jschSession.connect(SESSION_TIMEOUT);

            try {
                Channel sftp = jschSession.openChannel("sftp");
                sftp.connect(CHANNEL_TIMEOUT);
                try {
                    // Local filename that matches the remote
                    File localFile = new File(localTemporaryDirectory, "file-" + UUID.randomUUID());

                    ChannelSftp channelSftp = (ChannelSftp) sftp;
                    channelSftp.get(configuration.getPath(), localFile.getAbsolutePath());
                    channelSftp.exit();

                    return localFile.toPath();
                } finally {
                    if (sftp.isConnected()) {
                        sftp.disconnect();
                    }
                }
            } finally {
                if (jschSession.isConnected()){
                    jschSession.disconnect();
                }
            }
        } catch(JSchException | IOException | SftpException e) {
            log.error("Unable to create JSch session", e);
            throw new ConnectorException("Unable to create JSch session: " + e, e);
        }
    }

    public static Path copyFromSMB(ConnectorConfig configuration, File localTemporaryDirectory) throws ConnectorException {
        try {
            jcifs.Config.setProperty("jcifs.smb.client.domain", configuration.getDomain());
            jcifs.Config.setProperty("jcifs.netbios.wins", configuration.getHostname());
            jcifs.Config.setProperty("jcifs.smb.client.soTimeout", String.valueOf(SESSION_TIMEOUT));

            // Create authentication credentials
            jcifs.smb.NtlmPasswordAuthentication auth = new jcifs.smb.NtlmPasswordAuthentication(
                configuration.getDomain(),
                configuration.getUsername(),
                configuration.getPassword()
            );

            String url = String.format("smb://%s/%s/%s",
                configuration.getHostname(),
                configuration.getShare(),
                configuration.getPath());

            log.info("SMB URL: " + url);
            log.info("SMB Authentication: " + auth.getDomain() + "\\" + auth.getUsername());

            File localFile = new File(localTemporaryDirectory, "file-" + UUID.randomUUID());
            jcifs.smb.SmbFile smbFile = new jcifs.smb.SmbFile(url, auth);

            try (FileOutputStream fos = new FileOutputStream(localFile);
                 java.io.InputStream in = smbFile.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            log.info("File downloaded from SMB: " + localFile.getAbsolutePath());

            return localFile.toPath();
        } catch(IOException e) {
            log.error("Could not retrieve file via SMB", e);
            throw new ConnectorException("Could not retrieve file via SMB: " + e.getMessage(), e);
        }
    }

    private static Path downloadFile(ConnectorConfig config, File localDirectory) throws IOException {
        File localFile = new File(localDirectory, "file-" + UUID.randomUUID());
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequestBase request;
            String url = config.getTransferType().equalsIgnoreCase("http") ? 
                "http://" + config.getHostname() + config.getPath() :
                config.getUrl();
                
            request = new HttpGet(url);

            // Add bearer token if provided
            String bearerToken = config.getBearerToken();
            if (bearerToken != null && !bearerToken.isEmpty()) {
                request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }
            // Add Basic Auth if username and password are provided and no bearer token
            else if (config.hasPasswordAuthentication()) {
                String auth = config.getUsername() + ":" + config.getPassword();
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            }

            // Add any additional headers
            Map<String, String> headers = config.getHttpHeaders();
            if (headers != null) {
                headers.forEach(request::setHeader);
            }

            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new ConnectorException("HTTP request failed with status: " + statusCode);
                }

                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    response.getEntity().writeTo(fos);
                }
                return null;
            });
        }
        
        return localFile.toPath();
    }
}
