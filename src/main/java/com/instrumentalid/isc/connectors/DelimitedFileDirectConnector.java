package com.instrumentalid.isc.connectors;

import com.instrumentalid.isc.connectors.readers.ConnectorConfig;
import openconnector.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DelimitedFileDirectConnector extends AbstractConnector {
    @Override
    public Schema discoverSchema() throws ConnectorException, UnsupportedOperationException {
        try {
            ConnectorConfig configuration = new ConnectorConfig(config.getConfig());
            if (configuration.hasHeader()) {

                try (DelimitedFileIterator iterator = new DelimitedFileIterator(config.getConfig())) {
                    iterator.open();

                    if (iterator.hasNext()) {
                        Map<String, Object> record = iterator.next();
                        Schema schema = new Schema();
                        schema.setIncludePermissions(false);
                        for (String key : record.keySet()) {
                            schema.addAttribute(key, Schema.Type.STRING);
                        }
                        return schema;
                    }
                }

                throw new ConnectorException("No records found");
            } else {
                List<String> fields = configuration.getColumns();
                Schema schema = new Schema();
                for (String field : fields) {
                    schema.addAttribute(field, Schema.Type.STRING);
                }
                return schema;
            }
        } catch(IOException e) {
            throw new ConnectorException("Unable to derive schema: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Feature> getSupportedFeatures(String objectType) {
        return Arrays.asList(Feature.ITERATE, Feature.DISCOVER_SCHEMA);
    }

    /**
     * Iterate over records in the delimited file, returning them one at a time
     * as a map of attribute names to values.
     *
     * TODO implement filtering support
     *
     * @param filter the filter to apply (not supported)
     * @return an iterator over the records
     * @throws ConnectorException if an error occurs during iteration
     * @throws UnsupportedOperationException if filtering is not supported
     */
    @Override
    public Iterator<Map<String, Object>> iterate(Filter filter) throws ConnectorException, UnsupportedOperationException {
        try {
            DelimitedFileIterator iterator = new DelimitedFileIterator(config.getConfig());
            iterator.open();
            return iterator;
        } catch(IOException e) {
            throw new ConnectorException("Unable to create iterator: " + e.getMessage(), e);
        }
    }

    /**
     * Read a single record by its unique identifier. This is not supported for
     * a delimited file connector at this time.
     *
     * TODO: implement read by ID support by scanning the file for the record with the given ID
     *
     * @param s the unique identifier of the record
     * @return a map representing the record
     * @throws ConnectorException if an error occurs during the read
     * @throws ObjectNotFoundException if the record is not found
     * @throws UnsupportedOperationException if this operation is not supported
     */
    @Override
    public Map<String, Object> read(String s) throws ConnectorException, ObjectNotFoundException, UnsupportedOperationException {
        return Map.of();
    }

    /**
     * Tests the connection to the delimited file by attempting to download and read it.
     * @throws ConnectorException if the connection test fails
     */
    @Override
    public void testConnection() throws ConnectorException {
        try {
            try (DelimitedFileIterator iterator = new DelimitedFileIterator(config.getConfig())) {
                iterator.download();
            }
        } catch(IOException e) {
            throw new ConnectorException("Test connection failed: " + e.getMessage(), e);
        }
    }
}
