# SRP6 Migration Plan: NimbusDS to BouncyCastle

## Overview

This document outlines the plan to migrate from `com.nimbusds.srp6` to BouncyCastle's SRP6Client and SRP6Server implementations, extending them for HomeKit Accessory Protocol (HAP) compatibility.

## Current State Analysis

### Current Implementation
- Uses `com.nimbusds.srp6` library (version 1.5.2)
- Custom session classes: `HomekitClientSRP6Session` and `HomekitServerSRP6Session`
- Custom evidence calculation routines for HAP compatibility
- Supports both standard SRP6 and SRP-6a protocols
- Flexible state management and error handling

### BouncyCastle SRP6 Implementation
- Implements standard SRP6 protocol (not SRP-6a)
- `SRP6Client` and `SRP6Server` classes
- Built-in evidence calculation (M1, M2)
- Different API and state management approach
- More mature and widely-used cryptographic library

## Migration Strategy

### Phase 1: Dependency Management
1. **Remove NimbusDS dependency**
   - Remove `org.openhab.osgiify:com.nimbusds.srp6a:1.5.2` from `pom.xml`
   - Ensure BouncyCastle dependency is already present (should be)

2. **Update imports**
   - Replace all `com.nimbusds.srp6.*` imports with `org.bouncycastle.crypto.agreement.srp6.*`
   - Update any related utility imports

### Phase 2: Create HAP-Compatible Extensions

#### 2.1 HAP-Specific Client Class
```java
public class HAPSRP6Client extends SRP6Client {
    // Override evidence calculation for HAP compatibility
    // Implement custom M1 calculation using raw session key S
    // Maintain compatibility with HAP specification
}
```

#### 2.2 HAP-Specific Server Class
```java
public class HAPSRP6Server extends SRP6Server {
    // Override evidence calculation for HAP compatibility
    // Implement custom M2 calculation using raw session key S
    // Maintain compatibility with HAP specification
}
```

### Phase 3: Session Class Migration

#### 3.1 Replace HomekitClientSRP6Session
- **Option A**: Direct replacement with HAPSRP6Client
  - Pros: Simpler, uses BouncyCastle directly
  - Cons: May require API compatibility layer
- **Option B**: Wrapper approach
  - Keep existing session class, delegate to HAPSRP6Client
  - Pros: Maintains existing API
  - Cons: Additional abstraction layer

#### 3.2 Replace HomekitServerSRP6Session
- Similar approach as client session
- Ensure server-side state management compatibility

### Phase 4: Evidence Calculation Override

#### 4.1 HAP Evidence Requirements
- **M1 Calculation**: `H(H(N) XOR H(g) | H(I) | s | A | B | K)`
- **M2 Calculation**: `H(A | M1 | K)`
- **Session Key**: Use raw session key S, not H(S)

#### 4.2 Implementation Strategy
```java
@Override
protected BigInteger calculateM1(BigInteger A, BigInteger B, BigInteger K) {
    // Implement HAP-specific M1 calculation
    // Use raw session key S instead of H(S)
}

@Override
protected BigInteger calculateM2(BigInteger A, BigInteger M1, BigInteger K) {
    // Implement HAP-specific M2 calculation
    // Use raw session key S instead of H(S)
}
```

### Phase 5: Helper Class Updates

#### 5.1 Update HomekitEncryptionEngine
- Replace NimbusDS imports with BouncyCastle
- Update evidence calculation routines
- Ensure compatibility with new client/server classes

#### 5.2 Update HomekitPairSetupServlet
- Replace NimbusDS imports with BouncyCastle
- Update session creation and management
- Ensure proper error handling

### Phase 6: Test Updates

#### 6.1 Update Existing Tests
- Replace NimbusDS imports in test classes
- Update test setup to use new HAP-compatible classes
- Ensure test vectors still work correctly

#### 6.2 Add Migration Tests
- Test compatibility with existing HAP test vectors
- Verify evidence calculation correctness
- Test error handling and edge cases

## Technical Considerations

### API Compatibility
- **State Management**: BouncyCastle uses different state management
- **Error Handling**: Adapt error handling to BouncyCastle's approach
- **Method Signatures**: May need adapter methods for compatibility

### Performance Impact
- BouncyCastle is generally more performant
- Reduced dependency overhead
- Better memory management

### Security Considerations
- BouncyCastle is more mature and widely audited
- Maintains HAP security requirements
- Proper random number generation

## Implementation Order

1. **Create HAP-compatible BouncyCastle extensions**
   - Implement HAPSRP6Client
   - Implement HAPSRP6Server
   - Test evidence calculations

2. **Update session classes**
   - Replace HomekitClientSRP6Session
   - Replace HomekitServerSRP6Session
   - Ensure API compatibility

3. **Update helper classes**
   - Update HomekitEncryptionEngine
   - Update HomekitPairSetupServlet
   - Update any utility classes

4. **Update tests**
   - Replace imports in test classes
   - Verify test vector compatibility
   - Add migration-specific tests

5. **Remove NimbusDS dependency**
   - Remove from pom.xml
   - Clean up any remaining imports
   - Verify no compilation errors

## Risk Assessment

### High Risk
- **Evidence calculation differences**: HAP requires specific evidence calculation
- **API compatibility**: Different state management approaches
- **Test failures**: Existing tests may need significant updates

### Medium Risk
- **Performance regression**: Unlikely but possible
- **Memory usage changes**: Different internal implementations

### Low Risk
- **Security**: BouncyCastle is more mature
- **Dependency conflicts**: BouncyCastle is already used

## Success Criteria

1. **Compilation**: All code compiles without errors
2. **Tests Pass**: All existing tests pass with new implementation
3. **HAP Compatibility**: Evidence calculations match HAP specification
4. **Performance**: No significant performance regression
5. **Functionality**: All existing functionality works correctly

## Rollback Plan

If issues arise during migration:
1. Keep NimbusDS dependency temporarily
2. Implement feature flag to switch between implementations
3. Gradual migration with A/B testing
4. Complete rollback to NimbusDS if necessary

## Timeline Estimate

- **Phase 1-2**: 1-2 days (dependency and extension creation)
- **Phase 3**: 2-3 days (session class migration)
- **Phase 4-5**: 1-2 days (helper class updates)
- **Phase 6**: 1-2 days (test updates)
- **Testing and validation**: 2-3 days
- **Total**: 7-12 days

## Next Steps

1. Review and approve this migration plan
2. Create HAP-compatible BouncyCastle extensions
3. Begin session class migration
4. Update tests and validate functionality
5. Remove NimbusDS dependency
6. Document final implementation 