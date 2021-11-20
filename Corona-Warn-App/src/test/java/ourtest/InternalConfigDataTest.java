package ourtest;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;


import de.rki.coronawarnapp.appconfig.internal.InternalConfigData;

import static org.junit.jupiter.api.Assertions.*;

public class InternalConfigDataTest {

    public InternalConfigData getMockInternalConfigData(String etag) {
        byte[] rawData = new byte[0];
        Instant serverTime = Instant.parse("1980-04-09T10:15:30.00Z");
        Duration localOffset = Duration.ZERO;
        Duration cacheValidity = Duration.ZERO;

        return new InternalConfigData(rawData, etag, serverTime, localOffset, cacheValidity);
    }

    @Test
    public void testEquality() {
        String etag1 = "1";
        String etag2 = "2";
        InternalConfigData internalConfigData1 = getMockInternalConfigData(etag1);
        InternalConfigData internalConfigData2 = getMockInternalConfigData(etag1);

        assertTrue(internalConfigData2.equals(internalConfigData1));
    }

    @Test
    public void testInEquality() {

        byte[] rawData = new byte[0];
        String etag1 = "1";
        String etag2 = "2";
        Instant serverTime = Instant.now();
        Duration localOffset = Duration.ZERO;
        Duration cacheValidity = Duration.ZERO;

        InternalConfigData internalConfigData3 = new InternalConfigData(rawData, etag1, serverTime, localOffset, cacheValidity);
        InternalConfigData internalConfigData4 = new InternalConfigData(rawData, etag2, serverTime, localOffset, cacheValidity);

        assertTrue(internalConfigData3.equals(internalConfigData4));

    }

    @Test
    public void testHash() {
        String etag = "AKEF";
        // 1922165515
        assertEquals(getMockInternalConfigData(etag).hashCode(), 1922165515);
    }
}
