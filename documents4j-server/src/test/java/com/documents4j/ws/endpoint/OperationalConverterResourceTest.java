package com.documents4j.ws.endpoint;

import com.documents4j.job.MockConversion;
import com.documents4j.ws.ConverterNetworkProtocol;
import com.documents4j.ws.ConverterServerInformation;
import com.documents4j.ws.application.WebConverterApplication;
import com.documents4j.ws.application.WebConverterTestConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OperationalConverterResourceTest extends AbstractEncodingJerseyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationalConverterResourceTest.class);

    private static final String MESSAGE = "Hello converter!";
    private static final boolean CONVERTER_IS_OPERATIONAL = true;
    private static final long DEFAULT_TIMEOUT = 3000L;
    private static final long ADDITIONAL_TIMEOUT = 5000L;

    @Override
    protected Application configure() {
        return ResourceConfig.forApplication(new WebConverterApplication(
                new WebConverterTestConfiguration(CONVERTER_IS_OPERATIONAL, DEFAULT_TIMEOUT, SOURCE_FORMAT, TARGET_FORMAT)));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testFetchConverterServerInformation() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(MediaType.APPLICATION_XML_TYPE)
                .get();
        assertEquals(ConverterNetworkProtocol.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_XML, response.getMediaType().toString());
        ConverterServerInformation converterServerInformation = response.readEntity(ConverterServerInformation.class);
        assertEquals(DEFAULT_TIMEOUT, converterServerInformation.getTimeout());
        assertEquals(CONVERTER_IS_OPERATIONAL, converterServerInformation.isOperational());
        assertEquals(ConverterNetworkProtocol.CURRENT_PROTOCOL_VERSION, converterServerInformation.getProtocolVersion());
        assertEquals(Collections.singletonMap(SOURCE_FORMAT, Collections.singleton(TARGET_FORMAT)), converterServerInformation.getSupportedConversions());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionSuccess() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.OK.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(TARGET_FORMAT.toString(), response.getMediaType().toString());
        assertEquals(MockConversion.OK.asReply(MESSAGE), response.readEntity(String.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionInputError() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.INPUT_ERROR.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.INPUT_ERROR.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionConverterError() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.CONVERTER_ERROR.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.CONVERTER_ERROR.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionCancel() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.CANCEL.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.CANCEL.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT + ADDITIONAL_TIMEOUT)
    public void testConversionTimeout() throws Exception {
        LOGGER.info("Testing web request timeout handling: waiting for maximal {} milliseconds",
                DEFAULT_TIMEOUT + ADDITIONAL_TIMEOUT);
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.TIMEOUT.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.TIMEOUT.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionIllegalSourceFormat() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(TARGET_FORMAT.toString())
                .post(Entity.entity(MockConversion.OK.toInputStream(MESSAGE), TARGET_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.FORMAT_ERROR.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void testConversionIllegalTargetFormat() throws Exception {
        Response response = target(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(SOURCE_FORMAT.toString())
                .post(Entity.entity(MockConversion.OK.toInputStream(MESSAGE), SOURCE_FORMAT.toString()));
        assertEquals(ConverterNetworkProtocol.Status.FORMAT_ERROR.getStatusCode(), response.getStatus());
        assertNull(response.getMediaType());
        assertNull(response.readEntity(Object.class));
    }
}
