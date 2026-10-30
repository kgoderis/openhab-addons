// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using System.Security.Cryptography;

namespace AtsAdvancedGateway;

/// <summary>
/// Small helpers used during panel encryption session negotiation.
/// </summary>
/// <remarks>Author: Karel Goderis - Initial contribution</remarks>
internal static class AtsUtils
{
    private static readonly RandomNumberGenerator Random = RandomNumberGenerator.Create();

    /// <summary>
    /// Returns the session-key byte length for ATS encryption modes 1–3 (16 × mode),
    /// or <c>-1</c> when the mode is unsupported.
    /// </summary>
    /// <param name="encryptionMode">Panel <c>device.FWID_EncMode</c> value.</param>
    internal static int GetEncryptionKeySize(int encryptionMode) =>
        encryptionMode is >= 1 and <= 3 ? 16 * encryptionMode : -1;

    /// <summary>Fills <paramref name="data"/> with cryptographically strong random bytes.</summary>
    internal static void FillRandomBytes(byte[] data) => Random.GetBytes(data);
}
