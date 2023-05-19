namespace AtsAdvancedGateway.Actions
{
    using System;
    using System.Diagnostics;
    using Ace.Ats;

    internal struct MasterKeyProvider
    {
        private const int PasswordLength = 12;

        private readonly string password;

        internal MasterKeyProvider(string password)
        {
            Debug.Assert(password != null, "Missing password.");
            this.password = password;
        }

        internal byte[] GetKeyData(int encryptionMode)
        {
//            var password1 = password.Substring(0, Math.Min(12, password.Length));
//            var password2 = password.Substring(12, 12);
//            var password3 = password.Substring(24, 12);
//            var password4 = password.Substring(36, 12);

            switch (encryptionMode)
            {
                case 1:
                    return AtsAdvancedPanel.MakeEncryptionKey(this.Sub(1), this.Sub(2));

                case 2:
                    return AtsAdvancedPanel.MakeEncryptionKey(this.Sub(1), this.Sub(2), this.Sub(3));

                case 3:
                    return AtsAdvancedPanel.MakeEncryptionKey(this.Sub(1), this.Sub(2), this.Sub(3), this.Sub(4));

                default:
                    throw new NotImplementedException(string.Format("The encryption mode '{0}' is not imlemented yet.", encryptionMode));
            }


        }

        private string Sub(int index)
        {
            Debug.Assert(this.password != null, "Missing text.");
            var startIndex = index * PasswordLength;
            var available = this.password.Length - startIndex;
            return available <= 0 ? string.Empty : available <= PasswordLength ? this.password.Substring(startIndex) : this.password.Substring(startIndex, PasswordLength);
        }
    }
}
