// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using Ace.Ats;

namespace AtsAdvancedGateway;

/// <summary>
/// Derives the NAce master encryption key from the panel password for modes 1–3.
/// </summary>
/// <remarks>
/// The password is sliced into 12-character chunks starting at index 12, 24, 36, 48
/// and passed to <see cref="AtsAdvancedPanel.MakeEncryptionKey"/>. Chunk 0 (first 12 chars)
/// is unused by this legacy ATS Advanced scheme.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
internal readonly struct MasterKeyProvider
{
    private const int PasswordLength = 12;
    private readonly string _password;

    /// <summary>Creates a provider for the given panel encryption password.</summary>
    internal MasterKeyProvider(string password) => _password = password ?? throw new ArgumentNullException(nameof(password));

    /// <summary>
    /// Builds the master key byte array for <paramref name="encryptionMode"/> (1, 2, or 3).
    /// </summary>
    /// <exception cref="NotImplementedException">Thrown for unsupported modes.</exception>
    internal byte[] GetKeyData(int encryptionMode) =>
        encryptionMode switch
        {
            1 => AtsAdvancedPanel.MakeEncryptionKey(Sub(1), Sub(2)),
            2 => AtsAdvancedPanel.MakeEncryptionKey(Sub(1), Sub(2), Sub(3)),
            3 => AtsAdvancedPanel.MakeEncryptionKey(Sub(1), Sub(2), Sub(3), Sub(4)),
            _ => throw new NotImplementedException($"Encryption mode '{encryptionMode}' is not implemented.")
        };

    private string Sub(int index)
    {
        var startIndex = index * PasswordLength;
        var available = _password.Length - startIndex;
        if (available <= 0)
        {
            return string.Empty;
        }

        return available <= PasswordLength
            ? _password[startIndex..]
            : _password.Substring(startIndex, PasswordLength);
    }
}
