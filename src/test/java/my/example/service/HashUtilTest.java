package my.example.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class HashUtilTest {

    private final static Random RANDOM = new Random();

    @Test
    public void testComplex() {
        for (short index1 = 1; index1 < Short.MAX_VALUE / 2; index1++) {
            short index2 = (short) (index1 + RANDOM.nextInt(Short.MAX_VALUE / 4) + 1);

            int hash1 = HashUtil.genHash(index1, index2);
            int hash2 = HashUtil.genHash(index2, index1);
            Assertions.assertEquals(hash1, hash2);

            short[] codes = HashUtil.extractCodes(hash1);
            Assertions.assertEquals(index1, codes[0]);
            Assertions.assertEquals(index2, codes[1]);
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            HashUtil.genHash((short) 0, (short) 10);
            HashUtil.genHash((short) 10, (short) 0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            HashUtil.genHash((short) 10, (short) 10);
        });


    }


}
