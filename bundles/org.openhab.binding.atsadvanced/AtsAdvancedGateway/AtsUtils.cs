using System.Security.Cryptography;
using Ace;

namespace AtsAdvancedGateway;

internal static class AtsUtils
{
    private static readonly RandomNumberGenerator Random = RandomNumberGenerator.Create();

    internal static int GetEncryptionKeySize(int encryptionMode) =>
        encryptionMode is >= 1 and <= 3 ? 16 * encryptionMode : -1;

    internal static void FillRandomBytes(byte[] data) => Random.GetBytes(data);
}
