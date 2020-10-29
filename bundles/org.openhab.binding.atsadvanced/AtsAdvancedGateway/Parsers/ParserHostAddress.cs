//namespace AtsAdvancedTest.Parsers
//{
//    using System.Net;
//
//    internal class ParserHostAddress : Parser<IPAddress>
//    {
//        internal override IPAddress Parse(string value)
//        {
//            if (string.IsNullOrEmpty(value))
//            {
//                Program.Warn("The given host name is empty.");
//            }
//
//            return IPAddress.Parse(value);
//        }
//    }
//}
