package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.MathHelper;
import static org.junit.jupiter.api.Assertions.*;

class MathCompatibilityTest {
    @Test void negativeBlockHitsUseMathematicalFloorAndPositiveFraction() {
        assertEquals(-18, MathHelper.floor(-17.25));
        assertEquals(0.75, MathHelper.fractionalPart(-17.25));
        assertEquals(0.25, MathHelper.fractionalPart(17.25));
        assertEquals(0, MathHelper.fractionalPart(-17));
        assertEquals(3, MathHelper.clamp(7, 1, 3));
        assertEquals(1F, MathHelper.clamp(-2F, 1F, 3F));
        assertEquals(2.5, MathHelper.clamp(2.5, 1, 3));
        assertTrue(Double.isNaN(MathHelper.clamp(Double.NaN, 0, 1)));
        assertEquals(Math.sqrt(2), MathHelper.getSquareRootOfTwoMapped(), 1E-7);
    }
}
