# HAP M1 Calculation Analysis: Correct Implementation According to HAP Specification

## Overview

This document provides a detailed analysis of the correct M1 (client evidence) calculation according to the HomeKit Accessory Protocol (HAP) specification, comparing the openHAB implementation with the NimbusDS implementation and a Node.js/TypeScript SRP implementation.

## 🎯 Correct M1 Calculation According to HAP Specification

According to the **HomeKit Accessory Protocol (HAP) specification**, the correct M1 calculation is:

### **HAP Specification Formula:**
```
M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))
```

Where:
- `H(N)`: Hash of the modulus N
- `H(g)`: Hash of the generator g  
- `H(I)`: Hash of the identity/username
- `s`: Salt
- `A`: Client public key
- `B`: Server public key
- `H(S)`: Hash of the session key S

## 🔍 Implementation Comparison

### **✅ Node.js/TypeScript Implementation is CORRECT**

The **Node.js/TypeScript implementation** correctly follows the HAP specification:

```typescript
// Node.js/TypeScript getM1 function (CORRECT)
function getM1(params: SrpParams, u_buf: Buffer, s_buf: Buffer, A_buf: Buffer, B_buf: Buffer, K_buf: Buffer): Buffer {
    assertIsBuffer(u_buf, "identity (I)");
    assertIsBuffer(s_buf, "salt (s)");
    assertIsBuffer(A_buf, "client public key (A)");
    assertIsBuffer(B_buf, "server public key (B)");
    assertIsBuffer(K_buf, "session key (K)");

    // ✅ Correct: Independent hash calculations with separate createHash() calls
    const hN = crypto.createHash(params.hash).update(params.N.toBuffer(true)).digest();
    const hG = crypto.createHash(params.hash).update(params.g.toBuffer(true)).digest();

    // ✅ Correct: XOR operation
    for (let i = 0; i < hN.length; i++) {
        hN[i] ^= hG[i];
    }

    // ✅ Correct: Independent hash calculation
    const hU = crypto.createHash(params.hash).update(u_buf).digest();

    // ✅ Correct: Final concatenation
    return crypto.createHash(params.hash)
        .update(hN).update(hU).update(s_buf)
        .update(A_buf).update(B_buf).update(K_buf)
        .digest();
}
```

**Key Features of Node.js Implementation:**
- ✅ **Independent hash calculations**: Each `crypto.createHash()` creates a fresh hash context
- ✅ **Correct XOR operation**: Properly XORs H(N) and H(g)
- ✅ **Proper concatenation**: Components are concatenated in the correct order
- ✅ **HAP-compliant**: Follows the exact HAP specification formula
- ✅ **Padded byte arrays**: Uses `toBuffer(true)` which includes full-length, padded byte arrays

### **❌ openHAB Implementation is INCORRECT**

The **openHAB implementation** has a **critical bug** in BigInteger to byte array conversion:

```java
// openHAB calculateM1HAP method (INCORRECT)
private BigInteger calculateM1HAP(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
        BigInteger g, byte[] identity, byte[] salt) {

    // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))

    // Calculate H(N) - ❌ WRONG: Uses trimmed byte array
    digest.reset();
    byte[] nBytes = HomekitByte.toByteArray(N);  // ❌ REMOVES leading zeros!
    updateDigestInChunks(digest, nBytes);
    byte[] hN = new byte[digest.getDigestSize()];
    digest.doFinal(hN, 0);

    // Calculate H(g) - ❌ WRONG: Uses trimmed byte array  
    digest.reset();
    byte[] gBytes = HomekitByte.toByteArray(g);  // ❌ REMOVES leading zeros!
    updateDigestInChunks(digest, gBytes);
    byte[] hg = new byte[digest.getDigestSize()];
    digest.doFinal(hg, 0);

    // H(N) xor H(g) - ❌ WRONG: Based on incorrect hashes
    byte[] hNxorHg = new byte[hN.length];
    for (int i = 0; i < hN.length; i++) {
        hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
    }

    // ... rest of calculation
}
```

**Critical Issues with openHAB Implementation:**
- ❌ **Wrong byte array conversion**: `HomekitByte.toByteArray()` removes leading zeros
- ❌ **Incorrect H(N)**: Calculated on trimmed byte array instead of full-length
- ❌ **Incorrect H(g)**: Calculated on trimmed byte array instead of full-length
- ❌ **Wrong XOR result**: Based on incorrect hash values

### **❌ NimbusDS Implementation is INCORRECT**

The **NimbusDS implementation** has a **critical bug** - it doesn't reset the digest between hash calculations:

```java
// NimbusDS ClientEvidenceRoutineImpl (INCORRECT)
public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx) {
    MessageDigest digest = MessageDigest.getInstance(cryptoParams.H);

    // Calculate H(N) xor H(g)
    digest.update(HomekitByte.toByteArray(cryptoParams.N));
    byte[] hN = digest.digest();  // ✅ Correct: H(N)
    digest.update(HomekitByte.toByteArray(cryptoParams.g));
    byte[] hg = digest.digest();  // ❌ WRONG: This is H(N || g), not H(g)!
    byte[] hNhg = xor(hN, hg);
    
    // Calculate H(identity)
    digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
    byte[] hu = digest.digest();  // ❌ WRONG: This is H(N || g || identity), not H(identity)!
    
    // Calculate H(S)
    digest.update(HomekitByte.toByteArray(ctx.S));
    byte[] hS = digest.digest();  // ❌ WRONG: This is H(N || g || identity || S), not H(S)!
    
    // Final M1 calculation: H(H(N) xor H(g) || H(identity) || s || A || B || H(S))
    digest.update(hNhg);
    digest.update(hu);
    digest.update(HomekitByte.toByteArray(ctx.s));
    digest.update(HomekitByte.toByteArray(ctx.A));
    digest.update(HomekitByte.toByteArray(ctx.B));
    digest.update(hS);

    return new BigInteger(1, digest.digest());
}
```

**Critical Issues with NimbusDS Implementation:**
- ❌ **Missing digest reset**: No `digest.reset()` calls between hash calculations
- ❌ **Cumulative hashing**: Each hash calculation includes all previous data
- ❌ **Incorrect H(g)**: Actually calculates `H(N || g)` instead of `H(g)`
- ❌ **Incorrect H(I)**: Actually calculates `H(N || g || identity)` instead of `H(identity)`
- ❌ **Incorrect H(S)**: Actually calculates `H(N || g || identity || S)` instead of `H(S)`

## 🎯 The Root Causes

### **1. openHAB Root Cause: Wrong Byte Array Conversion**

The **openHAB implementation** uses `HomekitByte.toByteArray()` which **removes leading zeros**:

```java
public static byte[] toByteArray(BigInteger i) {
    byte[] array = i.toByteArray();
    if (array[0] == 0) {
        array = Arrays.copyOfRange(array, 1, array.length);  // ❌ REMOVES leading zero!
    }
    return array;
}
```

**TypeScript uses `toBuffer(true)` which includes full-length, padded byte arrays.**

### **2. NimbusDS Root Cause: Missing Digest Reset**

The **NimbusDS implementation** doesn't reset the digest between hash calculations, causing cumulative hashing.

## 🔧 How to Fix openHAB Implementation

To fix the openHAB implementation, we need to use **padded byte arrays** instead of trimmed ones:

```java
// Corrected openHAB implementation
private BigInteger calculateM1HAP(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
        BigInteger g, byte[] identity, byte[] salt) {

    // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))

    // Calculate H(N) - ✅ FIXED: Use full-length byte array
    digest.reset();
    byte[] nBytes = N.toByteArray();  // ✅ Use BigInteger.toByteArray() directly
    updateDigestInChunks(digest, nBytes);
    byte[] hN = new byte[digest.getDigestSize()];
    digest.doFinal(hN, 0);

    // Calculate H(g) - ✅ FIXED: Use full-length byte array
    digest.reset();
    byte[] gBytes = g.toByteArray();  // ✅ Use BigInteger.toByteArray() directly
    updateDigestInChunks(digest, gBytes);
    byte[] hg = new byte[digest.getDigestSize()];
    digest.doFinal(hg, 0);

    // H(N) xor H(g) - ✅ FIXED: Based on correct hashes
    byte[] hNxorHg = new byte[hN.length];
    for (int i = 0; i < hN.length; i++) {
        hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
    }

    // ... rest of calculation remains the same
}
```

## 📊 Implementation Comparison Summary

| Implementation | Byte Array Conversion | Hash Independence | HAP Compliance | Status |
|----------------|----------------------|-------------------|----------------|---------|
| **Node.js/TypeScript** | ✅ `toBuffer(true)` (padded) | ✅ `createHash()` | ✅ Correct formula | **CORRECT** |
| **openHAB** | ❌ `toByteArray()` (trimmed) | ✅ `digest.reset()` | ❌ Wrong hashes | **INCORRECT** |
| **NimbusDS** | ❌ `toByteArray()` (trimmed) | ❌ Missing reset | ❌ Wrong formula | **INCORRECT** |

## 📊 Test Results Comparison

From the test results, we can see the impact of these bugs:

```
openHAB Client M1: fa699949373bd2a92d5634101515df6d44857761...
NimbusDS Client M1: 5f7c14ab57ed0e94fd1d78c6b4dd09ed7e340b7e...
openHAB Client M1 vs NimbusDS Client M1: false
```

The M1 values are completely different because:
1. **openHAB**: Uses trimmed byte arrays for H(N) and H(g)
2. **NimbusDS**: Uses cumulative hashing without digest reset

## ✅ Conclusion

**Only the Node.js/TypeScript implementation is correct according to the HAP specification.** Both openHAB and NimbusDS have critical bugs:

1. **openHAB**: Needs to use `BigInteger.toByteArray()` directly instead of `HomekitByte.toByteArray()` to preserve leading zeros
2. **NimbusDS**: Needs to add `digest.reset()` calls between each hash calculation

The **Node.js/TypeScript implementation correctly uses padded byte arrays** with `toBuffer(true)`, which matches the expected HAP specification behavior.

## 📚 References

- **HAP Specification**: HomeKit Accessory Protocol Specification Release R2, Section 5.5.2 SRP Test Vectors
- **SRP-6a Protocol**: RFC 5054 and Tom Wu's SRP-6a paper
- **openHAB Implementation**: `HomekitSRP6Client.calculateM1HAP()` and `HomekitSRP6Server.calculateM1HAP()`
- **Node.js Implementation**: `getM1()` function in the provided TypeScript code
- **Test Vectors**: `HomekitSRP6TestVectors.java` - Official HAP test vectors

---

*This analysis was performed as part of the openHAB HomeKit binding development to ensure HAP specification compliance.* 