namespace AtsAdvancedGateway.Parsers
{
    using System;
    using System.Collections.Generic;

    internal abstract class Parser<T>
    {
        internal abstract T Parse(string value);

        internal static T Parse(string value, IDictionary<string,T> values, string errorFormat)
        {
            T result;
            if (values.TryGetValue(value, out result))
            {
                return result;
            }

            throw new ArgumentException(string.Format(errorFormat, value));
        }
    }
}
