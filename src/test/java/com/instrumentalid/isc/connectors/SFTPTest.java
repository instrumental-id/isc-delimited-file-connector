package com.instrumentalid.isc.connectors;

import com.instrumentalid.isc.connectors.readers.ConnectorConfig;
import com.instrumentalid.isc.connectors.readers.RemoteFileUtil;
import openconnector.ConnectorException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SFTPTest {
    private static final String CONTENT = "name,email" + System.lineSeparator() + "John Doe,john.doe@example.com";
    private static final String PASSWORD = "abc123";
    private static final int PORT = 22;
    private static final String USER = "test";
    @TempDir
    private static File directory;
    @Container
    private static final GenericContainer sftp = new GenericContainer(
            new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder ->
                            builder
                                    .from("atmoz/sftp:latest")
                                    .run("mkdir -p /home/" + USER + "/upload; chmod -R 777 /home/" + USER)
                                    .build()))
            .withExposedPorts(PORT)
            .withCommand(USER + ":" + PASSWORD + ":1001");
    private final File keyFile = new File("/Users/devin/.ssh/id_rsa_test");

    @BeforeAll
    public static void staticSetup() throws IOException {
        File sftpTestFile = new File(directory, "file.csv");
        sftpTestFile.createNewFile();
        Files.writeString(sftpTestFile.toPath(), CONTENT);

        //copy your files to the sftp
        sftp.withCopyFileToContainer(MountableFile.forHostPath(sftpTestFile.getPath()), "/home/" + USER + "/upload/" + sftpTestFile.getName());
        sftp.withCopyFileToContainer(MountableFile.forClasspathResource("/id_rsa.pub"), "/home/" + USER + "/.ssh/keys/");
        sftp.start();
    }

    @AfterAll
    public static void stopContainer() {
        sftp.stop();
    }

    private Map<String, Object> createConfig() throws IOException {
        String key = "";
        try (InputStream is = this.getClass().getResourceAsStream("/id_rsa")) {
            key = new String(is.readAllBytes(), StandardCharsets.US_ASCII);
        }

        Map<String, Object> config = new HashMap<>();
        config.put("transferType", "sftp");
        config.put("hostname", sftp.getHost());
        config.put("port", String.valueOf(sftp.getMappedPort(PORT)));
        config.put("username", USER);
        config.put("password", "");
        config.put("privateKey", key);
        config.put("privateKeyPassphrase", "P@ssw0rd");
        config.put("path", "/upload/file.csv");
        config.put("header", "true");
        return config;
    }

    @Test
    public void testDiscover() throws ConnectorException, IOException {
        Map<String, Object> config = createConfig();

        try (DelimitedFileIterator iterator = new DelimitedFileIterator(config)) {
            iterator.open();

            if (iterator.hasNext()) {
                Map<String, Object> record = iterator.next();
                if (record != null) {
                    assertThat(record.keySet(), containsInAnyOrder("name", "email"));
                }
            }
        }

    }

    @Test
    public void testSftp() throws IOException {
        Map<String, Object> config = createConfig();

        Path tempDir = Files.createTempDirectory("sftp-test");

        ConnectorConfig parsedConfig = new ConnectorConfig(config);

        Path filePath = RemoteFileUtil.copyFromSFTP(parsedConfig, tempDir.toFile());

        System.out.println(filePath);

        assertThat(filePath, notNullValue());

        String contents = Files.readString(filePath);
        assertThat(contents, containsString("John Doe"));
    }
}
