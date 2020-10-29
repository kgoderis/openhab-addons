namespace AtsAdvancedGateway.Parsers
{
    using System;
    using System.Net;

    internal class ParserPort : Parser<int>
    {
        internal override int Parse(string value)
        {
            int result;
            if (!int.TryParse(value, out result))
            {
                throw new ArgumentException(string.Format("Invalid port number '{0}'.", value));
            }

            if ((result < IPEndPoint.MinPort) || (result > IPEndPoint.MaxPort))
            {
                throw new ArgumentException(string.Format("Invalid port number value '{0}'.", value));
            }

            if (result < 1024)
            {
                Program.Warn("The port number between 0 and 1023 is reserved for system purposes.");
            }

            return result;
        }
    }
}
