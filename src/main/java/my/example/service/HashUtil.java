package my.example.service;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 */
public class HashUtil {

    public static int genHash(short code1, short code2) {
        if (code1 <= 0 || code2 <= 0) {
            throw new IllegalArgumentException("Code index <= 0");
        }

        if (isNaturalOrder(code1, code2)) {
            return ((code1 & 0xffff) << 16) | (code2 & 0xffff);
        } else {
            return ((code2 & 0xffff) << 16) | (code1 & 0xffff);
        }

    }

    public static boolean isNaturalOrder(short code1, short code2) {
        if (code1 < code2) {
            return true;
        } else if (code1 > code2) {
            return false;
        } else {
            throw new IllegalArgumentException("equal codes");
        }
    }

    public static short[] extractCodes(int hash) {
        if (hash < 0xffff) {
            throw new IllegalArgumentException("Invalid hash");
        }
        short[] codes = {(short) (hash >> 16), (short) (hash & 0x0000ffff)};

        if (codes[0] <= 0 || codes[1] <= 0 || codes[0] >= codes[1]) {
            throw new IllegalArgumentException("Invalid hash, wrong code values: " + codes[0] + "/" + codes[1]);
        }
        return codes;


    }
}
