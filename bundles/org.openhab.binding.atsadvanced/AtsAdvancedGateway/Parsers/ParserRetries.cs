//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//
//    internal class ParserRetries : Parser<int>
//    {
//        internal override int Parse(string value)
//        {
//            int result;
//            if (!int.TryParse(value, out result))
//            {
//                throw new ArgumentException(string.Format("Invalid retries value '{0}'.", value));
//            }
//
//            if ((result < 0) || (result > int.MaxValue))
//            {
//                throw new ArgumentException(string.Format("Invalid retries value '{0}'.", value));
//            }
//
//            if (result > 5)
//            {
//                Program.Warn(string.Format("The retries value {0} is unreasonable.", result));
//            }
//
//            return result;
//        }
//    }
//}
