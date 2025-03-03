//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//
//    internal class ParserSerialPortName : Parser<string>
//    {
//        internal override string Parse(string value)
//        {
//            if (string.IsNullOrEmpty(value))
//            {
//                Program.Warn("The given serial port name is empty.");
//            }
//            else if (!value.StartsWith("COM", StringComparison.OrdinalIgnoreCase))
//            {
//                Program.Warn(string.Format("The given serial port name '{0}' does not start with 'COM'.", value));
//            }
//
//            if (value != null)
//            {
//                var index = value.IndexOfAny(System.IO.Path.GetInvalidFileNameChars());
//                if (index >= 0)
//                {
//                    Program.Warn(string.Format("The given serial port name '{0}' consists invalid character at position {1}.", value, index));
//                }
//            }
//
//            return value;
//        }
//    }
//}
