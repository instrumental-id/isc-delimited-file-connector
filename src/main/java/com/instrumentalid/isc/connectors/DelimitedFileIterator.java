package com.instrumentalid.isc.connectors;

import com.instrumentalid.isc.connectors.readers.ConnectorConfig;
import com.instrumentalid.isc.connectors.readers.RemoteFileUtil;
import de.siegmar.fastcsv.reader.*;
import openconnector.ConnectorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Iterator for reading delimited files from various remote sources.
 */
public class DelimitedFileIterator implements Iterator<Map<String, Object>>, AutoCloseable {
    /**
     * The connector configuration, passed from ISC
     */
    private final ConnectorConfig configuration;

    /**
     * The CSV reader for reading the delimited file
     */
    private CsvReader<NamedCsvRecord> csvReader;

    /**
     * The iterator for reading records from the CSV file
     */
    private CloseableIterator<NamedCsvRecord> iterator;

    /**
     * The local temporary directory for storing the downloaded file
     */
    private final Path localDirectory;

    /**
     * The local temporary file path for the downloaded file
     */
    private Path localFile;

    private final Log log;

    public DelimitedFileIterator(Map<String, Object> config) throws IOException {
        this.configuration = new ConnectorConfig(config);

        this.localDirectory = Files.createTempDirectory("delimited-file-iterator");
        this.log = LogFactory.getLog(DelimitedFileIterator.class);
    }

    /**
     * Closes the iterator and cleans up temporary files.
     *
     * @throws ConnectorException if an error occurs during closing
     */
    public void close() {
        Exception error = null;

        if (this.iterator != null) {
            try {
                this.iterator.close();
            } catch(IOException e) {
                error = e;
                log.error("Caught an error closing the CSV iterator", e);
            }
        }

        if (this.csvReader != null) {
            try {
                this.csvReader.close();
            } catch(IOException e) {
                error = e;
                log.error("Caught an error closing the CSV reader", e);
            }
        }

        try {
            if (this.localDirectory != null) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.localDirectory)) {
                    stream.forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new ConnectorException("Unable to delete temporary file: " + e.getMessage(), e);
                        }
                    });
                }

                Files.delete(this.localDirectory);
            }
        } catch(IOException e) {
            error = e;
            log.error("Caught an error deleting the temporary directory and files " + e.getMessage(), e);
        }

        if (error != null) {
            throw new ConnectorException("Caught an error closing the Delimited File Iterator: " + error.getMessage(), error);
        }
    }

    /**
     * Downloads the remote file to a local temporary directory based on the transfer type.
     * @throws IOException if an I/O error occurs during download
     */
    public void download() throws IOException {
        String transferType = this.configuration.getTransferType();
        switch (transferType.toLowerCase()) {
            case "sftp":
                this.localFile = RemoteFileUtil.copyFromSFTP(this.configuration, this.localDirectory.toFile());
                break;
            case "ftp":
                this.localFile = RemoteFileUtil.copyFromFTP(this.configuration, this.localDirectory.toFile());
                break;
            case "ftps":
                this.localFile = RemoteFileUtil.copyFromFTPS(this.configuration, this.localDirectory.toFile());
                break;
            case "smb":
                this.localFile = RemoteFileUtil.copyFromSMB(this.configuration, this.localDirectory.toFile());
                break;
            case "s3":
                this.localFile = RemoteFileUtil.copyFromS3(this.configuration, this.localDirectory.toFile());
                break;
            case "http":
            case "https":
                this.localFile = RemoteFileUtil.copyFromHTTP(this.configuration, this.localDirectory.toFile());
                break;
            case "gcs":
                this.localFile = RemoteFileUtil.copyFromGoogleCloud(this.configuration, this.localDirectory.toFile());
                break;
            case "azure":
                this.localFile = RemoteFileUtil.copyFromAzureBlob(this.configuration, this.localDirectory.toFile());
                break;
            default:
                throw new ConnectorException("Invalid transfer type. Supported types: sftp, ftp, ftps, smb, s3, webdav, http(s), nfs, gcs, azure");
        }
    }

    /**
     * Checks if there are more records to read from the delimited file.
     * @return true if there are more records, false otherwise
     */
    public boolean hasNext() {
        boolean hasNext = false;
        if (this.iterator != null) {
            hasNext = this.iterator.hasNext();
            if (!hasNext) {
                this.close();
            }
            return hasNext;
        } else {
            throw new ConnectorException("Must call open() before iterating");
        }
    }

    /**
     * Reads the next record from the delimited file and returns it as a map.
     * @return a map representing the next record
     */
    public Map<String, Object> next() {
        NamedCsvRecord record = this.iterator.next();
        Map<String, Object> map = new HashMap<>();
        for (String key : record.getHeader()) {
            String value = record.getField(key);
            if (value != null) {
                map.put(key, value.trim());
            } else {
                map.put(key, "");
            }
        }

        return map;
    }

    /**
     * Opens the delimited file for reading by downloading it and initializing the CSV reader.
     * @throws IOException if an I/O error occurs during file download or reader initialization
     */
    public void open() throws IOException {
        this.download();

        if (configuration.hasHeader()) {
            this.csvReader = CsvReader.builder()
                    .fieldSeparator(configuration.getDelimiter())
                    .skipEmptyLines(true)
                    .commentCharacter('#')
                    .commentStrategy(CommentStrategy.SKIP)
                    .ofNamedCsvRecord(localFile);
        } else {
            List<String> fields = configuration.getColumns();
            CsvCallbackHandler<NamedCsvRecord> callbackHandler =
                    new NamedCsvRecordHandler(fields);

            this.csvReader = CsvReader.builder()
                    .fieldSeparator(configuration.getDelimiter())
                    .skipEmptyLines(true)
                    .commentCharacter('#')
                    .commentStrategy(CommentStrategy.SKIP)
                    .build(callbackHandler, localFile);
        }

        this.iterator = this.csvReader.iterator();
    }
}
