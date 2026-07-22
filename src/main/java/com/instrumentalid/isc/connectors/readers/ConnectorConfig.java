package com.instrumentalid.isc.connectors.readers;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConnectorConfig class representing configuration for various connector types.
 * Supports SFTP, SMB, S3, GCS, Azure, WebDAV, HTTP, and HTTPS connectors.
 *
 * TODO make transfer type into an enum
 */
@SuppressWarnings("unchecked")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ConnectorConfig {

    /**
     * Additional properties not explicitly defined in the class
     */
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * AWS access key ID
     */
    @JsonProperty("awsAccessKeyId")
    private String awsAccessKeyId;

    /**
     * AWS secret key
     */
    @JsonProperty("awsSecretKey")
    private String awsSecretKey;

    /**
     * Azure connection string
     */
    @JsonProperty("azureConnectionString")
    private String azureConnectionString;

    /**
     * Azure container name
     */
    @JsonProperty("azureContainerName")
    private String azureContainerName;

    /**
     * Bearer token for HTTP/WebDAV authentication
     */
    @JsonProperty("bearerToken")
    private String bearerToken;

    /**
     * Bucket name for S3 or other object storage
     */
    @JsonProperty("bucket")
    private String bucket;

    /**
     * List of column names for delimited files
     */
    @JsonProperty("columns")
    private List<String> columns;

    /**
     * Delimiter character for delimited files
     */
    @JsonProperty("delimiter")
    private Character delimiter;

    /**
     * Domain for SMB authentication
     */
    @JsonProperty("domain")
    private String domain;

    /**
     * GCP credentials in JSON format
     */
    @JsonProperty("gcpCredentialsJson")
    private String gcpCredentialsJson;

    /**
     * GCP project ID
     */
    @JsonProperty("gcpProjectId")
    private String gcpProjectId;

    /**
     * GCS bucket name
     */
    @JsonProperty("gcsBucket")
    private String gcsBucket;

    /**
     * Indicates if the first row is a header row
     */
    @JsonProperty("header")
    @JsonDeserialize(using = FlexibleBooleanDeserializer.class)
    private Boolean header;

    /**
     * Hostname or IP address for the connection
     */
    @JsonProperty("hostname")
    private String hostname;

    /**
     * HTTP headers for HTTP/WebDAV connections
     */
    @JsonProperty("httpHeaders")
    private Map<String, String> httpHeaders;

    /**
     * Password for authentication (for SFTP, SMB, etc.)
     */
    @JsonProperty("password")
    private String password;

    /**
     * File path or object key; usage depends on transfer type
     */
    @JsonProperty("path")
    private String path;

    /**
     * Port number for the connection (default 22 for SFTP)
     */
    @JsonProperty("port")
    @JsonDeserialize(using = FlexibleIntegerDeserializer.class)
    private Integer port;

    /**
     * Private key for SSH authentication
     */
    @JsonProperty("privateKey")
    private String privateKey;

    /**
     * Passphrase for the private key
     */
    @JsonProperty("privateKeyPassphrase")
    private String privateKeyPassphrase;

    /**
     * AWS S3 region name
     */
    @JsonProperty("region")
    private String region;

    /**
     * SMB share name
     */
    @JsonProperty("share")
    private String share;

    /**
     * Enable strict host key checking for SSH connections
     */
    @JsonProperty("strictHostKeyChecking")
    @JsonDeserialize(using = FlexibleBooleanDeserializer.class)
    private Boolean strictHostKeyChecking;

    /**
     * Transfer type (e.g., "sftp", "smb", "s3", "gcs", "azure", "webdav", "http", "https")
     */
    @JsonProperty("transferType")
    private String transferType;

    /**
     * URL for HTTP/WebDAV connections
     */
    @JsonProperty("url")
    private String url;

    /**
     * Username for authentication (for SFTP, SMB, etc.)
     */
    @JsonProperty("username")
    private String username;

    /**
     * WebDAV authentication token
     */
    @JsonProperty("webdavToken")
    private String webdavToken;

    /**
     * WebDAV server URL
     */
    @JsonProperty("webdavUrl")
    private String webdavUrl;

    /**
     * SMB workgroup name
     */
    @JsonProperty("workgroup")
    private String workgroup;

    /**
     * Default constructor; used by Jackson for deserialization.
     */
    public ConnectorConfig() {
    }

    /**
     * Map constructor to initialize ConnectorConfig from a map of properties.
     * This will be the constructor invoked when the configuration is being
     * passed from ISC to the connector server.
     *
     * @param config Map of configuration properties
     */
    public ConnectorConfig(Map<String, Object> config) {
        if (config != null) {
            config.forEach((key, value) -> {
                switch (key) {
                    case "delimiter":
                        this.delimiter = value instanceof String ? ((String) value).charAt(0) : (Character) value;
                        break;
                    case "header":
                        this.header = parseBoolean(value);
                        break;
                    case "columns":
                        this.columns = (List<String>) value;
                        break;
                    case "transferType":
                        this.transferType = (String) value;
                        break;
                    case "url":
                        this.url = (String) value;
                        break;
                    case "hostname":
                        this.hostname = (String) value;
                        break;
                    case "port":
                        this.port = parseInteger(value, 22);
                        break;
                    case "username":
                        this.username = (String) value;
                        break;
                    case "password":
                        this.password = (String) value;
                        break;
                    case "domain":
                        this.domain = (String) value;
                        break;
                    case "workgroup":
                        this.workgroup = (String) value;
                        break;
                    case "share":
                        this.share = (String) value;
                        break;
                    case "path":
                        this.path = (String) value;
                        break;
                    case "privateKey":
                        this.privateKey = (String) value;
                        break;
                    case "privateKeyPassphrase":
                        this.privateKeyPassphrase = (String) value;
                        break;
                    case "strictHostKeyChecking":
                        this.strictHostKeyChecking = parseBoolean(value);
                        break;
                    case "bucket":
                        this.bucket = (String) value;
                        break;
                    case "region":
                        this.region = (String) value;
                        break;
                    case "awsAccessKeyId":
                        this.awsAccessKeyId = (String) value;
                        break;
                    case "awsSecretKey":
                        this.awsSecretKey = (String) value;
                        break;
                    case "webdavUrl":
                        this.webdavUrl = (String) value;
                        break;
                    case "webdavToken":
                        this.webdavToken = (String) value;
                        break;
                    case "httpHeaders":
                        this.httpHeaders = (Map<String, String>) value;
                        break;
                    case "bearerToken":
                        this.bearerToken = (String) value;
                        break;
                    case "gcpProjectId":
                        this.gcpProjectId = (String) value;
                        break;
                    case "gcpCredentialsJson":
                        this.gcpCredentialsJson = (String) value;
                        break;
                    case "gcsBucket":
                        this.gcsBucket = (String) value;
                        break;
                    case "azureConnectionString":
                        this.azureConnectionString = (String) value;
                        break;
                    case "azureContainerName":
                        this.azureContainerName = (String) value;
                        break;
                    default:
                        additionalProperties.put(key, value);
                }
            });
        }
    }

    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Parse Boolean from Object. If the value is a String, it checks for "true" (case-insensitive).
     * @param value Object value
     * @return Boolean value
     */
    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return ((String) value).equalsIgnoreCase("true");
        }
        return false;
    }

    /**
     * Parse Integer from Object. If the value is a String, it attempts to parse it.
     * @param value Object value
     * @param defaultValue Default value if parsing fails
     * @return Integer value
     */
    private static Integer parseInteger(Object value, int defaultValue) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public String getAzureConnectionString() {
        return azureConnectionString;
    }

    public String getAzureContainerName() {
        return azureContainerName;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getBucket() {
        return bucket;
    }

    public List<String> getColumns() {
        return columns;
    }

    public Character getDelimiter() {
        return delimiter != null ? delimiter : ',';
    }

    public String getDomain() {
        return domain;
    }

    public String getGcpCredentialsJson() {
        return gcpCredentialsJson;
    }

    public String getGcpProjectId() {
        return gcpProjectId;
    }

    public String getGcsBucket() {
        return gcsBucket;
    }

    public Boolean getHeader() {
        return header != null ? header : false;
    }

    public String getHostname() {
        return hostname;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders != null ? httpHeaders : new HashMap<>();
    }

    public String getPassword() {
        return password;
    }

    public String getPath() {
        return path;
    }

    public Integer getPort() {
        return port != null ? port : 22;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public String getRegion() {
        return region != null ? region : "us-east-1";
    }

    public String getS3Region() {
        return getRegion();
    }

    public String getShare() {
        return share;
    }

    public Boolean getStrictHostKeyChecking() {
        return strictHostKeyChecking != null ? strictHostKeyChecking : false;
    }

    public String getTransferType() {
        return transferType;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getWebdavToken() {
        return webdavToken;
    }

    public String getWebdavUrl() {
        return webdavUrl;
    }

    public String getWorkgroup() {
        return workgroup != null ? workgroup : "WORKGROUP";
    }

    public boolean hasAwsCredentials() {
        return isNotNullOrEmpty(awsAccessKeyId) && isNotNullOrEmpty(awsSecretKey);
    }

    public boolean hasHeader() {
        return header != null && header;
    }

    public boolean hasPasswordAuthentication() {
        return isNotNullOrEmpty(username) && isNotNullOrEmpty(password);
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalProperties.put(name, value);
    }

    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public void setAzureConnectionString(String azureConnectionString) {
        this.azureConnectionString = azureConnectionString;
    }

    public void setAzureContainerName(String azureContainerName) {
        this.azureContainerName = azureContainerName;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public void setDelimiter(Character delimiter) {
        this.delimiter = delimiter;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setGcpCredentialsJson(String gcpCredentialsJson) {
        this.gcpCredentialsJson = gcpCredentialsJson;
    }

    public void setGcpProjectId(String gcpProjectId) {
        this.gcpProjectId = gcpProjectId;
    }

    public void setGcsBucket(String gcsBucket) {
        this.gcsBucket = gcsBucket;
    }

    public void setHeader(Boolean header) {
        this.header = header;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setShare(String share) {
        this.share = share;
    }

    public void setStrictHostKeyChecking(Boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWebdavToken(String webdavToken) {
        this.webdavToken = webdavToken;
    }

    public void setWebdavUrl(String webdavUrl) {
        this.webdavUrl = webdavUrl;
    }

    public void setWorkgroup(String workgroup) {
        this.workgroup = workgroup;
    }

    public void validate() {
        if (transferType == null) {
            throw new IllegalArgumentException("transferType is required");
        }
        String type = transferType.toLowerCase();
        switch (type) {
            case "webdav":
                requireNotNull(webdavUrl, "WebDAV URL is required");
                break;
            case "http":
            case "https":
                requireNotNull(url, "HTTP URL is required");
                break;
            case "gcs":
                requireNotNull(gcpProjectId, "GCP Project ID is required");
                requireNotNull(gcsBucket, "GCS bucket name is required");
                break;
            case "azure":
                requireNotNull(azureConnectionString, "Azure connection string is required");
                requireNotNull(azureContainerName, "Azure container name is required");
                break;
        }
    }
}

