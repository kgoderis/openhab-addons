//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//    using System.Diagnostics;
//
//    internal class ParserTimeout : Parser<int>
//    {
//        private readonly int min;
//        private readonly int max;
//
//        internal ParserTimeout(int min, int max)
//        {
//            Debug.Assert(min > 0, "Invalid minimum reasonable timeout value.");
//            Debug.Assert(max > 0, "Invalid maximum reasonable timeout value.");
//            Debug.Assert(min <= max, "Minimum must not be greater than maximum.");
//            this.min = min;
//            this.max = max;
//        }
//
//        internal override int Parse(string value)
//        {
//            int result;
//            if (!int.TryParse(value, out result))
//            {
//                throw new ArgumentException(string.Format("Invalid timeout value '{0}'.", value));
//            }
//
//            if ((result <= 0) || (result > int.MaxValue))
//            {
//                throw new ArgumentException(string.Format("Invalid timeout value '{0}'.", value));
//            }
//
//            if ((result < this.min) || (result > this.max))
//            {
//                Program.Warn(string.Format("The timeout value {0} is unreasonable.", result));
//            }
//
//            return result;
//        }
//    }
//}
