package ourtest;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import de.rki.coronawarnapp.appconfig.internal.InternalConfigData;

import static org.junit.jupiter.api.Assertions.*;

public class InternalConfigDataTest {

    @Test
    public void testEquality() {
        byte[] rawData = new byte[0];
        String etag = "String";
        Instant serverTime = Instant.now();
        Duration localOffset = Duration.ZERO;
        Duration cacheValidity = Duration.ZERO;
        InternalConfigData internalConfigData1 = new InternalConfigData(rawData, etag, serverTime, localOffset, cacheValidity);
        InternalConfigData internalConfigData2 = new InternalConfigData(rawData, etag, serverTime, localOffset, cacheValidity);

        assertTrue(internalConfigData2.equals(internalConfigData1));

        internalConfigData2 = new InternalConfigData(rawData, "AKEF", Instant.parse("1980-04-09T10:15:30.00Z"), localOffset, cacheValidity);

        //assertTrue(internalConfigData2.equals(internalConfigData1));

        //System.out.println(internalConfigData2.hashCode());

        assertEquals(internalConfigData2.hashCode(), 1922165515);
        // 1922165515
    }
}
