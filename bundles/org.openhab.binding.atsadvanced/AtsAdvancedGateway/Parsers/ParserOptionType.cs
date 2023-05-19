namespace AtsAdvancedGateway.Parsers
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;

    internal class ParserOptionType : Parser<OptionType>
    {
        private static IDictionary<string, OptionType> values = CreateDecoder();

        internal override OptionType Parse(string value)
        {
            return Parse(value, values, "The program option '{0}' is unknown.");
        }

        private static IDictionary<string, OptionType> CreateDecoder()
        {
            var result = new Dictionary<string, OptionType>(StringComparer.InvariantCultureIgnoreCase);
            AddOption(result, OptionType.Port, "p", "port");
			AddOption(result, OptionType.Driver, "d", "driver");

            return result;
        }

        private static void AddOption(IDictionary<string, OptionType> result, OptionType option, params string[] switches)
        {
            foreach(var item in switches)
            {
                Debug.Assert(!string.IsNullOrEmpty(item), "Missing switch name.");
                result.Add((item.Length == 1 ? "-" : "--") + item, option);
                result.Add("/" + item, option);
            }
        }
    }
}
