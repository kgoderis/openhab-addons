//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//    using System.Collections.Generic;
//    using System.Linq;
//    using System.Text;
//    using System.Text.RegularExpressions;
//
//    internal class ParserPin : Parser<string>
//    {
//        internal override string Parse(string value)
//        {
//            if (string.IsNullOrEmpty(value))
//            {
//                Program.Warn("The given PIN is empty.");
//            }
//            else if (Encoding.UTF8.GetByteCount(value) > 10)
//            {
//                Program.Warn(string.Format("The given PIN '{0}' is too long.", value));
//            }
//
//            if (!Regex.IsMatch(value, "^[0-9]*$", RegexOptions.Singleline | RegexOptions.CultureInvariant))
//            {
//                Program.Warn(string.Format("The given PIN '{0}' consists of unsupported characters.", value));
//            }
//
//            return value;
//        }
//    }
//}
