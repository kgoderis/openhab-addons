//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//    using System.Collections.Generic;
//
//    internal class ParserProgramMode : Parser<ProgramMode>
//    {
//        private static IDictionary<string, ProgramMode> values = new Dictionary<string, ProgramMode>(StringComparer.InvariantCultureIgnoreCase)
//        {
//            { "tcp-server", ProgramMode.TcpServer },
//            { "com-client", ProgramMode.SerialClient },
//            { "tcp-client", ProgramMode.TcpClient },
//        };
//
//        internal override ProgramMode Parse(string value)
//        {
//            return Parse(value, values, "The program mode '{0}' is unknown.");
//        }
//    }
//}
