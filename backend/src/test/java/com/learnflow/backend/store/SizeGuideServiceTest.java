package com.learnflow.backend.store;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class SizeGuideServiceTest {

    private final SizeGuideService guide = new SizeGuideService(
        new ObjectMapper()
    );

    SizeGuideServiceTest() throws Exception {}

    private Map<String, BigDecimal> measures(
        String bust,
        String waist,
        String hip,
        String shoulder
    ) {
        Map<String, BigDecimal> m = new HashMap<>();
        if (bust != null) m.put("bustCm", new BigDecimal(bust));
        if (waist != null) m.put("waistCm", new BigDecimal(waist));
        if (hip != null) m.put("hipCm", new BigDecimal(hip));
        if (shoulder != null) m.put("shoulderCm", new BigDecimal(shoulder));
        return m;
    }

    @Test
    void followsAllFourChartsAndConservativeSharedBoundaries() {
        assertEquals(
            "S",
            guide
                .advise("SIZE_AO_NU", measures("82", "64", null, "35.5"), null)
                .size()
        );
        assertEquals(
            "M",
            guide
                .advise("SIZE_QUAN_NU", measures(null, "66", "90", null), null)
                .size()
        );
        assertEquals(
            "L",
            guide
                .advise("SIZE_CHAN_VAY", measures(null, "72", "96", null), null)
                .size()
        );
        assertEquals(
            "XXL",
            guide
                .advise("SIZE_DAM", measures("102", "84", "108", null), null)
                .size()
        );
    }

    @Test
    void missingOutOfRangeAndUnavailableSizesAreNeverInvented() {
        assertNull(
            guide
                .advise("SIZE_AO_NU", measures("82", "64", null, null), null)
                .size()
        );
        assertNull(
            guide
                .advise("SIZE_DAM", measures("103", "84", "108", null), null)
                .size()
        );
        assertNull(
            guide
                .advise(
                    "SIZE_QUAN_NU",
                    measures(null, "67", "92", null),
                    Set.of("S", "XL")
                )
                .size()
        );
        assertNull(guide.advise(null, Map.of(), null).size());
    }

    @Test
    void mixedMeasurementsExplainRoomierFit() {
        var result = guide.advise(
            "SIZE_DAM",
            measures("82", "72", "96", null),
            null
        );
        assertEquals("L", result.size());
        assertTrue(result.explanation().contains("có thể rộng"));
    }

    @Test
    void policiesAndContactsMatchStoreInformation() throws Exception {
        var store = new StoreInfoService(new ObjectMapper());
        assertEquals("maihunw@gmail.com", store.info().contact().email());
        assertTrue(store.answer("đổi trả").orElseThrow().contains("7 ngày"));
        assertTrue(
            store.answer("phí vận chuyển").orElseThrow().contains("3–7")
        );
        assertTrue(
            store.answer("liên hệ").orElseThrow().contains("0982655491")
        );
    }
}
