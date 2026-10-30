using Ace.Ats;

namespace AtsAdvancedGatewayNet8;

internal readonly struct MasterKeyProvider
{
    private const int PasswordLength = 12;
    private readonly string _password;

    internal MasterKeyProvider(string password) => _password = password ?? throw new ArgumentNullException(nameof(password));

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
