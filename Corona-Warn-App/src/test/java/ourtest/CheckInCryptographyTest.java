package ourtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.joda.time.Instant;

import de.rki.coronawarnapp.presencetracing.checkins.CheckIn;
import de.rki.coronawarnapp.server.protocols.internal.pt.CheckInOuterClass;
import de.rki.coronawarnapp.server.protocols.internal.pt.TraceWarning;
import kotlin.random.Random;
import de.rki.coronawarnapp.covidcertificate.common.cryptography.AesCryptography;
import de.rki.coronawarnapp.presencetracing.checkins.cryptography.CheckInCryptography;
import okio.ByteString;

public class CheckInCryptographyTest {

    Random secureRandom;
    AesCryptography aesCryptography;

    CheckInCryptography checkInCryptography;

    @Before
    public void setup() {
        secureRandom = Random.Default;
        aesCryptography = new AesCryptography();
        checkInCryptography = new CheckInCryptography(secureRandom, aesCryptography);
    }

    public CheckIn getMockCheckIn() {
        long id = 0L;
        ByteString traceLocationId = ByteString.EMPTY;
        int version = 0;
        int type = 1;
        String description = "what ever";
        String address = "duhil";
        Instant traceLocationStart =Instant.parse("2021-04-09T10:15:30.00Z");
        Instant traceLocationEnd = Instant.parse("2021-04-09T10:25:30.00Z");
        int defaultCheckInLengthInMinutes = 5;
        ByteString cryptographicSeed = ByteString.EMPTY;
        String cnPublicKey = "";
        Instant checkInStart = Instant.parse("2021-04-09T10:10:30.00Z");
        Instant checkInEnd = Instant.parse("2021-04-09T10:40:30.00Z");
        boolean completed = true;
        boolean createJournalEntry = false;
        boolean isSubmitted = false;
        boolean hasSubmissionConsent = false;

        return new CheckIn(id, traceLocationId, version, type, description,
                address, traceLocationStart, traceLocationEnd,
                defaultCheckInLengthInMinutes, cryptographicSeed, cnPublicKey,
                checkInStart, checkInEnd, completed, createJournalEntry, isSubmitted,
                hasSubmissionConsent
        );
    }

    @Test
    public void testEncrypt() {
        int transmissionRiskLevel = 6;
        CheckInOuterClass.CheckInProtectedReport encryptedData =
                checkInCryptography.encrypt(getMockCheckIn(), transmissionRiskLevel);
        TraceWarning.TraceTimeIntervalWarning decryptedData =
                checkInCryptography.decrypt(encryptedData, ByteString.EMPTY);

        assertEquals(decryptedData.getTransmissionRiskLevel(), 6);
    }
}
