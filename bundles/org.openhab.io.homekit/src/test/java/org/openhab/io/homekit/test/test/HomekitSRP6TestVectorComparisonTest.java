package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openhab.io.homekit.test.helper.HomekitSRP6TestVectors;
import org.openhab.io.homekit.test.helper.SRP63072TestVectors;

/**
 * Comprehensive test class that compares all hex strings between HomekitSRP6TestVectors
 * and SRP63072TestVectors to identify matches and differences.
 * 
 * This test helps understand the relationship between the two test vector sets
 * and identifies which values are consistent vs. different.
 */
public class HomekitSRP6TestVectorComparisonTest {

    /**
     * Test class to hold comparison results
     */
    private static class ComparisonResult {
        final String fieldName;
        final String homekitValue;
        final String srpValue;
        final boolean isMatch;
        final String homekitLength;
        final String srpLength;
        final List<String> differences;

        ComparisonResult(String fieldName, String homekitValue, String srpValue) {
            this.fieldName = fieldName;
            this.homekitValue = homekitValue;
            this.srpValue = srpValue;
            this.isMatch = homekitValue.equals(srpValue);
            this.homekitLength = homekitValue.isEmpty() ? "EMPTY" : String.valueOf(homekitValue.length());
            this.srpLength = srpValue.isEmpty() ? "EMPTY" : String.valueOf(srpValue.length());
            this.differences = findDifferences(homekitValue, srpValue);
        }

        private List<String> findDifferences(String homekit, String srp) {
            List<String> diffs = new ArrayList<>();

            if (homekit.isEmpty() && srp.isEmpty()) {
                return diffs;
            }

            if (homekit.isEmpty() || srp.isEmpty()) {
                diffs.add("One value is empty while the other has content");
                return diffs;
            }

            if (homekit.length() != srp.length()) {
                diffs.add(String.format("Length mismatch: Homekit=%d, SRP=%d", homekit.length(), srp.length()));
            }

            // Find byte-level differences
            int minLength = Math.min(homekit.length(), srp.length());
            for (int i = 0; i < minLength; i += 8) {
                int end = Math.min(i + 8, minLength);
                String homekitChunk = homekit.substring(i, end);
                String srpChunk = srp.substring(i, end);

                if (!homekitChunk.equals(srpChunk)) {
                    diffs.add(String.format("Position %d-%d: Homekit='%s' vs SRP='%s'", i, end - 1, homekitChunk,
                            srpChunk));
                }
            }

            return diffs;
        }
    }

    @Test
    public void testCompareAllHexStrings() {
        List<ComparisonResult> results = new ArrayList<>();

        // Compare all hex string fields
        results.add(new ComparisonResult("MODULUS_N", HomekitSRP6TestVectors.MODULUS_N, SRP63072TestVectors.MODULUS_N));
        results.add(new ComparisonResult("GENERATOR_G", HomekitSRP6TestVectors.GENERATOR_G,
                SRP63072TestVectors.GENERATOR_G));
        results.add(new ComparisonResult("SALT_HEX", HomekitSRP6TestVectors.SALT_HEX, SRP63072TestVectors.SALT_HEX));
        results.add(new ComparisonResult("K_HEX", HomekitSRP6TestVectors.K_HEX, SRP63072TestVectors.K_HEX));
        results.add(new ComparisonResult("X_HEX", HomekitSRP6TestVectors.X_HEX, SRP63072TestVectors.X_HEX));
        results.add(new ComparisonResult("EXPECTED_VERIFIER_HEX", HomekitSRP6TestVectors.EXPECTED_VERIFIER_HEX,
                SRP63072TestVectors.EXPECTED_VERIFIER_HEX));
        results.add(new ComparisonResult("A_PRIVATE_HEX", HomekitSRP6TestVectors.A_PRIVATE_HEX,
                SRP63072TestVectors.A_PRIVATE_HEX));
        results.add(new ComparisonResult("B_PRIVATE_HEX", HomekitSRP6TestVectors.B_PRIVATE_HEX,
                SRP63072TestVectors.B_PRIVATE_HEX));
        results.add(new ComparisonResult("EXPECTED_A_PUBLIC_HEX", HomekitSRP6TestVectors.EXPECTED_A_PUBLIC_HEX,
                SRP63072TestVectors.EXPECTED_A_PUBLIC_HEX));
        results.add(new ComparisonResult("EXPECTED_B_PUBLIC_HEX", HomekitSRP6TestVectors.EXPECTED_B_PUBLIC_HEX,
                SRP63072TestVectors.EXPECTED_B_PUBLIC_HEX));
        results.add(new ComparisonResult("U_HEX", HomekitSRP6TestVectors.U_HEX, SRP63072TestVectors.U_HEX));
        results.add(new ComparisonResult("EXPECTED_SESSION_KEY_HEX", HomekitSRP6TestVectors.EXPECTED_SESSION_KEY_HEX,
                SRP63072TestVectors.EXPECTED_SESSION_KEY_HEX));
        // PREMASTER_SECRET_HEX is empty in HomekitSRP6TestVectors, so skip this comparison
        // results.add(new ComparisonResult("PREMASTER_SECRET_HEX", HomekitSRP6TestVectors.PREMASTER_SECRET_HEX,
        // SRP63072TestVectors.PREMASTER_SECRET_HEX));
        results.add(new ComparisonResult("M1_HEX", HomekitSRP6TestVectors.M1_HEX, SRP63072TestVectors.M1_HEX));
        results.add(new ComparisonResult("M2_HEX", HomekitSRP6TestVectors.M2_HEX, SRP63072TestVectors.M2_HEX));

        // Print comprehensive comparison report
        printComparisonReport(results);

        // Assert that we have the expected number of comparisons
        assertEquals(14, results.size(), "Should compare 14 hex string fields");
    }

    private void printComparisonReport(List<ComparisonResult> results) {
        System.out.println("=".repeat(80));
        System.out.println("COMPREHENSIVE HEX STRING COMPARISON REPORT");
        System.out.println("HomekitSRP6TestVectors vs SRP63072TestVectors");
        System.out.println("=".repeat(80));

        // Count matches and differences
        int matches = 0;
        int differences = 0;
        int homekitEmpty = 0;
        int srpEmpty = 0;

        for (ComparisonResult result : results) {
            if (result.isMatch) {
                matches++;
            } else {
                differences++;
            }

            if (result.homekitValue.isEmpty()) {
                homekitEmpty++;
            }
            if (result.srpValue.isEmpty()) {
                srpEmpty++;
            }
        }

        // Print summary
        System.out.println("\nSUMMARY:");
        System.out.println("-".repeat(40));
        System.out.printf("Total fields compared: %d%n", results.size());
        System.out.printf("Exact matches: %d%n", matches);
        System.out.printf("Differences: %d%n", differences);
        System.out.printf("HomekitSRP6TestVectors empty fields: %d%n", homekitEmpty);
        System.out.printf("SRP63072TestVectors empty fields: %d%n", srpEmpty);

        // Print detailed results
        System.out.println("\nDETAILED COMPARISON:");
        System.out.println("=".repeat(80));

        for (ComparisonResult result : results) {
            System.out.printf("\n%-25s | ", result.fieldName);

            if (result.isMatch) {
                System.out.print("✅ MATCH");
                if (!result.homekitValue.isEmpty()) {
                    System.out.printf(" (Length: %s)", result.homekitLength);
                } else {
                    System.out.print(" (Both empty)");
                }
            } else {
                System.out.print("❌ DIFFERENT");
                System.out.printf(" | Homekit: %s chars | SRP: %s chars", result.homekitLength, result.srpLength);
            }

            System.out.println();

            // Print differences if any
            if (!result.differences.isEmpty()) {
                System.out.println("  Differences:");
                for (String diff : result.differences) {
                    System.out.println("    - " + diff);
                }
            }
        }

        // Print field categories
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FIELD CATEGORIES:");
        System.out.println("-".repeat(40));

        System.out.println("\n✅ IDENTICAL FIELDS:");
        results.stream().filter(r -> r.isMatch).forEach(r -> System.out.printf("  - %s%n", r.fieldName));

        System.out.println("\n❌ DIFFERENT FIELDS:");
        results.stream().filter(r -> !r.isMatch).forEach(
                r -> System.out.printf("  - %s (Homekit: %s, SRP: %s)%n", r.fieldName, r.homekitLength, r.srpLength));

        System.out.println("\n📋 MISSING IN HOMEKITSRP6TESTVECTORS:");
        results.stream().filter(r -> r.homekitValue.isEmpty() && !r.srpValue.isEmpty())
                .forEach(r -> System.out.printf("  - %s%n", r.fieldName));

        System.out.println("\n📋 MISSING IN SRP63072TESTVECTORS:");
        results.stream().filter(r -> !r.homekitValue.isEmpty() && r.srpValue.isEmpty())
                .forEach(r -> System.out.printf("  - %s%n", r.fieldName));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ANALYSIS:");
        System.out.println("-".repeat(40));

        if (matches > differences) {
            System.out.println("✅ More fields match than differ, suggesting similar test vector sets");
        } else {
            System.out.println("❌ More fields differ than match, suggesting different test vector sets");
        }

        if (homekitEmpty > 0) {
            System.out.printf("⚠️  HomekitSRP6TestVectors has %d empty fields that SRP63072TestVectors provides%n",
                    homekitEmpty);
        }

        if (srpEmpty > 0) {
            System.out.printf("⚠️  SRP63072TestVectors has %d empty fields that HomekitSRP6TestVectors provides%n",
                    srpEmpty);
        }

        System.out.println("\n" + "=".repeat(80));
    }

    @Test
    public void testValidateHexStringFormats() {
        // Test that all non-empty hex strings are valid
        String[] homekitFields = { HomekitSRP6TestVectors.MODULUS_N, HomekitSRP6TestVectors.GENERATOR_G,
                HomekitSRP6TestVectors.SALT_HEX, HomekitSRP6TestVectors.EXPECTED_VERIFIER_HEX,
                HomekitSRP6TestVectors.A_PRIVATE_HEX, HomekitSRP6TestVectors.B_PRIVATE_HEX,
                HomekitSRP6TestVectors.EXPECTED_A_PUBLIC_HEX, HomekitSRP6TestVectors.EXPECTED_B_PUBLIC_HEX,
                HomekitSRP6TestVectors.U_HEX, HomekitSRP6TestVectors.EXPECTED_SESSION_KEY_HEX };

        String[] srpFields = { SRP63072TestVectors.MODULUS_N, SRP63072TestVectors.GENERATOR_G,
                SRP63072TestVectors.SALT_HEX, SRP63072TestVectors.K_HEX, SRP63072TestVectors.X_HEX,
                SRP63072TestVectors.EXPECTED_VERIFIER_HEX, SRP63072TestVectors.A_PRIVATE_HEX,
                SRP63072TestVectors.B_PRIVATE_HEX, SRP63072TestVectors.EXPECTED_A_PUBLIC_HEX,
                SRP63072TestVectors.EXPECTED_B_PUBLIC_HEX, SRP63072TestVectors.U_HEX,
                SRP63072TestVectors.EXPECTED_SESSION_KEY_HEX, SRP63072TestVectors.M1_HEX, SRP63072TestVectors.M2_HEX };

        // Validate HomekitSRP6TestVectors
        for (int i = 0; i < homekitFields.length; i++) {
            String field = homekitFields[i];
            if (!field.isEmpty()) {
                assertTrue(field.matches("^[0-9A-Fa-f]+$"),
                        "HomekitSRP6TestVectors field " + i + " should be valid hex: " + field);
            }
        }

        // Validate SRP63072TestVectors
        for (int i = 0; i < srpFields.length; i++) {
            String field = srpFields[i];
            if (!field.isEmpty()) {
                assertTrue(field.matches("^[0-9A-Fa-f]+$"),
                        "SRP63072TestVectors field " + i + " should be valid hex: " + field);
            }
        }
    }
}
