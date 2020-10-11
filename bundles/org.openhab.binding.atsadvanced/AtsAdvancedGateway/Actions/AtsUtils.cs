namespace AtsAdvancedGateway.Actions
{
    using Ace;
    using System.Text;
    using System.Security.Cryptography;

    internal static class AtsUtils
    {
        private static RandomNumberGenerator random = RandomNumberGenerator.Create();

        private static readonly string[] Categories = new string[]
        {
            "FAULT",
            "MAINS",
            "ACTZN",
            "ACT24H",
            "ACTLCD",
            "ACTDEV",
            "ALARMS_NCNF",
            "ALARMS_CNF",
            "FAULTS_NCNF",
            "FAULTS_CNF",
            "WALK_REQ",
            "WALK_OK",
            "SYSTEM",
        };

        internal static int GetEncryptionKeySize(int encryptionMode)
        {
            switch (encryptionMode)
            {
                case 1:
                case 2:
                case 3:
                    return 16 * encryptionMode;

                default:
                    return -1;
            }
        }

        internal static void FillRandomBytes(byte[] data)
        {
            random.GetBytes(data);
        }

        internal static string ReadEventCategories(IMessage message)
        {
            var result = new StringBuilder();
            for (int i = 0; i < Categories.Length; i++)
            {
                var propname = "evCat" + Categories[i];
                var status = (bool)message.GetProperty(propname, 0, typeof(bool));
                if (status)
                {
                    if (result.Length > 0)
                    {
                        result.Append(", ");
                    }

                    result.Append(propname);
                }
            }

            return result.ToString();
        }
    }
}
