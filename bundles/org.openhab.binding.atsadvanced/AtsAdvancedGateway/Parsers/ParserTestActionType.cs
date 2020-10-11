//namespace AtsAdvancedTest.Parsers
//{
//    using System;
//    using System.Collections.Generic;
//
//    internal class ParserTestActionType : Parser<TestActionType>
//    {
//        private static IDictionary<string, TestActionType> values = new Dictionary<string, TestActionType>(StringComparer.InvariantCultureIgnoreCase)
//        {
//            { "DISCOVERY", TestActionType.Discovery },
//            { "LOGIN", TestActionType.Login },
//            { "NAMES", TestActionType.Names },
//            { "DRYSET", TestActionType.SetPreview },
//        };
//
//        internal override TestActionType Parse(string value)
//        {
//            return Parse(value, values, "The test action type '{0}' is unknown.");
//        }
//    }
//}
