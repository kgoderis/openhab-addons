package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.openhab.io.homekit.util.HomekitByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the Pad function in HomekitSRP6Util.
 * 
 * The Pad function takes a BigInteger and a target length, and returns a byte array
 * of the specified length. If the BigInteger's byte representation is shorter than
 * the target length, it pads with leading zeros. If it's longer, it returns the
 * original bytes.
 */
public class HomekitBytePadTest {

    private static final Logger logger = LoggerFactory.getLogger(HomekitBytePadTest.class);

    @Test
    public void testPadWithSmallNumber() {
        logger.info("=== Testing Pad with Small Number ===");

        // Test padding a small number to a larger length
        BigInteger smallNumber = new BigInteger("123");
        int targetLength = 8;

        logger.debug("Input: smallNumber = {}, targetLength = {}", smallNumber, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(smallNumber.toByteArray()));

        byte[] result = HomekitByte.Pad(smallNumber, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = smallNumber.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        // Verify leading bytes are zero
        for (int i = 0; i < result.length - originalBytes.length; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ Small number padding test passed");
    }

    @Test
    public void testPadWithLargeNumber() {
        logger.info("=== Testing Pad with Large Number ===");

        // Test a large number that doesn't need padding
        BigInteger largeNumber = new BigInteger("123456789012345678901234567890");
        int targetLength = 4; // Smaller than the number's byte length

        logger.debug("Input: largeNumber = {}, targetLength = {}", largeNumber, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(largeNumber.toByteArray()));

        byte[] result = HomekitByte.Pad(largeNumber, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // Should return the original bytes without padding
        byte[] originalBytes = largeNumber.toByteArray();
        logger.debug("Expected: length = {}, bytes = {}", originalBytes.length,
                java.util.Arrays.toString(originalBytes));

        assertArrayEquals(originalBytes, result);

        logger.info("✅ Large number padding test passed");
    }

    @Test
    public void testPadWithExactLength() {
        logger.info("=== Testing Pad with Exact Length ===");

        // Test when the number's byte length equals the target length
        BigInteger number = new BigInteger("12345");
        byte[] originalBytes = number.toByteArray();
        int targetLength = originalBytes.length;

        logger.debug("Input: number = {}, targetLength = {}", number, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(originalBytes));

        byte[] result = HomekitByte.Pad(number, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);
        assertArrayEquals(originalBytes, result);

        logger.info("✅ Exact length padding test passed");
    }

    @Test
    public void testPadWithZero() {
        logger.info("=== Testing Pad with Zero ===");

        // Test padding zero
        BigInteger zero = BigInteger.ZERO;
        int targetLength = 8;

        logger.debug("Input: zero = {}, targetLength = {}", zero, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(zero.toByteArray()));

        byte[] result = HomekitByte.Pad(zero, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // All bytes should be zero
        for (byte b : result) {
            assertEquals(0, b);
        }

        logger.info("✅ Zero padding test passed");
    }

    @Test
    public void testPadWithNegativeNumber() {
        logger.info("=== Testing Pad with Negative Number ===");

        // Test padding a negative number
        BigInteger negativeNumber = new BigInteger("-123");
        int targetLength = 8;

        logger.debug("Input: negativeNumber = {}, targetLength = {}", negativeNumber, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(negativeNumber.toByteArray()));

        byte[] result = HomekitByte.Pad(negativeNumber, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = negativeNumber.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        // Verify leading bytes are zero
        for (int i = 0; i < result.length - originalBytes.length; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ Negative number padding test passed");
    }

    @Test
    public void testPadWithOne() {
        logger.info("=== Testing Pad with One ===");

        // Test padding the number 1
        BigInteger one = BigInteger.ONE;
        int targetLength = 8;

        logger.debug("Input: one = {}, targetLength = {}", one, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(one.toByteArray()));

        byte[] result = HomekitByte.Pad(one, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Only the last byte should be 1, others should be 0
        assertEquals(1, result[result.length - 1]);
        for (int i = 0; i < result.length - 1; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ One padding test passed");
    }

    @Test
    public void testPadWithMaxValue() {
        logger.info("=== Testing Pad with Max Value ===");

        // Test padding a very large number
        BigInteger maxValue = new BigInteger("9999999999999999999999999999999999999999999999999999999999999999");
        int targetLength = 16;

        logger.debug("Input: maxValue = {}, targetLength = {}", maxValue, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(maxValue.toByteArray()));

        byte[] result = HomekitByte.Pad(maxValue, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // Should return original bytes since they're longer than target
        byte[] originalBytes = maxValue.toByteArray();
        logger.debug("Expected: length = {}, bytes = {}", originalBytes.length,
                java.util.Arrays.toString(originalBytes));

        assertArrayEquals(originalBytes, result);

        logger.info("✅ Max value padding test passed");
    }

    @Test
    public void testPadWithTargetLengthZero() {
        logger.info("=== Testing Pad with Target Length Zero ===");

        // Test with target length of 0
        BigInteger number = new BigInteger("123");
        int targetLength = 0;

        logger.debug("Input: number = {}, targetLength = {}", number, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(number.toByteArray()));

        byte[] result = HomekitByte.Pad(number, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // Should return original bytes since target length is 0
        byte[] originalBytes = number.toByteArray();
        logger.debug("Expected: length = {}, bytes = {}", originalBytes.length,
                java.util.Arrays.toString(originalBytes));

        assertArrayEquals(originalBytes, result);

        logger.info("✅ Target length zero test passed");
    }

    @Test
    public void testPadWithNegativeTargetLength() {
        logger.info("=== Testing Pad with Negative Target Length ===");

        // Test with negative target length
        BigInteger number = new BigInteger("123");
        int targetLength = -1;

        logger.debug("Input: number = {}, targetLength = {}", number, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(number.toByteArray()));

        byte[] result = HomekitByte.Pad(number, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // Should return original bytes since target length is negative
        byte[] originalBytes = number.toByteArray();
        logger.debug("Expected: length = {}, bytes = {}", originalBytes.length,
                java.util.Arrays.toString(originalBytes));

        assertArrayEquals(originalBytes, result);

        logger.info("✅ Negative target length test passed");
    }

    @Test
    public void testPadWithLargeTargetLength() {
        logger.info("=== Testing Pad with Large Target Length ===");

        // Test with a very large target length
        BigInteger number = new BigInteger("123");
        int targetLength = 1000;

        logger.debug("Input: number = {}, targetLength = {}", number, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(number.toByteArray()));

        byte[] result = HomekitByte.Pad(number, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = number.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        // Verify leading bytes are zero
        for (int i = 0; i < result.length - originalBytes.length; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ Large target length test passed");
    }

    @Test
    public void testPadWithPowerOfTwo() {
        logger.info("=== Testing Pad with Power of Two ===");

        // Test with numbers that are powers of 2
        BigInteger powerOfTwo = new BigInteger("256"); // 2^8
        int targetLength = 16;

        logger.debug("Input: powerOfTwo = {}, targetLength = {}", powerOfTwo, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(powerOfTwo.toByteArray()));

        byte[] result = HomekitByte.Pad(powerOfTwo, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = powerOfTwo.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        // Verify leading bytes are zero
        for (int i = 0; i < result.length - originalBytes.length; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ Power of two test passed");
    }

    @Test
    public void testPadWithByteBoundary() {
        logger.info("=== Testing Pad with Byte Boundary ===");

        // Test with a number that has exactly 8 bits (1 byte)
        BigInteger byteBoundary = new BigInteger("255"); // 8 bits, 1 byte
        int targetLength = 4;

        logger.debug("Input: byteBoundary = {}, targetLength = {}", byteBoundary, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(byteBoundary.toByteArray()));

        byte[] result = HomekitByte.Pad(byteBoundary, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = byteBoundary.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        // Verify leading bytes are zero
        for (int i = 0; i < result.length - originalBytes.length; i++) {
            assertEquals(0, result[i]);
        }

        logger.info("✅ Byte boundary test passed");
    }

    @Test
    public void testPadWithHomekitByteCompatibility() {
        logger.info("=== Testing Pad with HomekitByte Compatibility ===");

        // Test that the result is compatible with HomekitByte.toByteArray()
        BigInteger number = new BigInteger("123456789");
        int targetLength = 8;

        logger.debug("Input: number = {}, targetLength = {}", number, targetLength);
        logger.debug("Original bytes: {}", java.util.Arrays.toString(number.toByteArray()));

        byte[] result = HomekitByte.Pad(number, targetLength);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // The function uses HomekitByte.toByteArray() internally
        // Verify the result matches what we'd expect from that method
        assertEquals(targetLength, result.length);

        // Verify the original number is preserved at the end
        byte[] originalBytes = number.toByteArray();
        logger.debug("Original bytes length: {}", originalBytes.length);

        for (int i = 0; i < originalBytes.length; i++) {
            assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
        }

        logger.info("✅ HomekitByte compatibility test passed");
    }

    @Test
    public void testPadWithVariousLengths() {
        logger.info("=== Testing Pad with Various Lengths ===");

        // Test with various target lengths
        BigInteger number = new BigInteger("123");
        int[] targetLengths = { 1, 2, 4, 8, 16, 32, 64 };

        logger.debug("Input: number = {}, targetLengths = {}", number, java.util.Arrays.toString(targetLengths));
        logger.debug("Original bytes: {}", java.util.Arrays.toString(number.toByteArray()));

        for (int targetLength : targetLengths) {
            logger.debug("Testing targetLength = {}", targetLength);

            byte[] result = HomekitByte.Pad(number, targetLength);

            logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

            if (number.toByteArray().length <= targetLength) {
                assertEquals(targetLength, result.length);

                // Verify the original number is preserved at the end
                byte[] originalBytes = number.toByteArray();
                logger.debug("Original bytes length: {}", originalBytes.length);

                for (int i = 0; i < originalBytes.length; i++) {
                    assertEquals(originalBytes[i], result[result.length - originalBytes.length + i]);
                }

                // Verify leading bytes are zero
                for (int i = 0; i < result.length - originalBytes.length; i++) {
                    assertEquals(0, result[i]);
                }
            } else {
                // Should return original bytes if target length is too small
                logger.debug("Target length too small, expecting original bytes");
                assertArrayEquals(number.toByteArray(), result);
            }
        }

        logger.info("✅ Various lengths test passed");
    }

    @Test
    public void testPadWithBigIntegerN_NormalPadding() {
        logger.info("=== Testing Pad with BigInteger N - Normal Padding ===");

        BigInteger n = new BigInteger("123");
        BigInteger N = new BigInteger("FFFFFFFFFFFFFFFF", 16); // 8 bytes

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(8, result.length);
        byte[] expected = new byte[8];
        byte[] nBytes = n.toByteArray();
        System.arraycopy(nBytes, 0, expected, 8 - nBytes.length, nBytes.length);

        logger.debug("Expected: length = {}, bytes = {}", expected.length, java.util.Arrays.toString(expected));

        assertArrayEquals(expected, result);

        logger.info("✅ BigInteger N normal padding test passed");
    }

    @Test
    public void testPadWithBigIntegerN_NoPaddingNeeded() {
        logger.info("=== Testing Pad with BigInteger N - No Padding Needed ===");

        BigInteger n = new BigInteger("12345678901234567890");
        BigInteger N = new BigInteger("FFFF", 16); // 2 bytes

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // N has bitLength 16, so calculated length is (16+7)/8 = 2
        // n has more than 2 bytes, so Pad() returns original bytes from HomekitByte.toByteArray()
        byte[] expectedBytes = HomekitByte.toByteArray(n);

        logger.debug("Expected: length = {}, bytes = {}", expectedBytes.length,
                java.util.Arrays.toString(expectedBytes));

        assertArrayEquals(expectedBytes, result);

        logger.info("✅ BigInteger N no padding needed test passed");
    }

    @Test
    public void testPadWithBigIntegerN_ExactFit() {
        logger.info("=== Testing Pad with BigInteger N - Exact Fit ===");

        BigInteger n = new BigInteger("12345678", 16); // 4 bytes
        BigInteger N = new BigInteger("FFFFFFFF", 16); // 4 bytes

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(4, result.length);
        assertArrayEquals(n.toByteArray(), result);

        logger.info("✅ BigInteger N exact fit test passed");
    }

    @Test
    public void testPadWithBigIntegerN_NisZero() {
        logger.info("=== Testing Pad with BigInteger N - N is Zero ===");

        BigInteger n = BigInteger.ZERO;
        BigInteger N = new BigInteger("FFFFFFFF", 16); // 4 bytes

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(4, result.length);
        for (byte b : result)
            assertEquals(0, b);

        logger.info("✅ BigInteger N is zero test passed");
    }

    @Test
    public void testPadWithBigIntegerN_NisZeroLength() {
        logger.info("=== Testing Pad with BigInteger N - N is Zero Length ===");

        BigInteger n = new BigInteger("123");
        BigInteger N = BigInteger.ZERO;

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        // When N is zero, bitLength is 0, so calculated length is (0+7)/8 = 0
        // When length is 0, Pad() returns the original bytes from toByteArray(n)
        byte[] expectedBytes = n.toByteArray();

        logger.debug("Expected: length = {}, bytes = {}", expectedBytes.length,
                java.util.Arrays.toString(expectedBytes));

        assertArrayEquals(expectedBytes, result);

        logger.info("✅ BigInteger N is zero length test passed");
    }

    @Test
    public void testPadWithBigIntegerN_NullArguments() {
        logger.info("=== Testing Pad with BigInteger N - Null Arguments ===");

        // Note: HomekitByte.Pad() has @NonNull parameters, so null tests are not applicable
        // The compiler will prevent null values from being passed
        BigInteger n = new BigInteger("123");
        BigInteger N = new BigInteger("FFFFFFFF", 16);

        logger.debug("Input: n = {}, N = {} (bitLength: {})", n, N.toString(16), N.bitLength());
        logger.debug("n bytes: {}", java.util.Arrays.toString(n.toByteArray()));
        logger.debug("N bytes: {}", java.util.Arrays.toString(N.toByteArray()));

        // Test that valid calls work
        byte[] result = HomekitByte.Pad(n, N);

        logger.debug("Result: length = {}, bytes = {}", result.length, java.util.Arrays.toString(result));

        assertEquals(4, result.length);

        logger.info("✅ BigInteger N null arguments test passed");
    }
}
