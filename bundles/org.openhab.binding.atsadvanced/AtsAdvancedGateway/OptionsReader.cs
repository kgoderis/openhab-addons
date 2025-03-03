namespace AtsAdvancedGateway
{
    using System;
    using System.Diagnostics;
    using Parsers;

    public class OptionsReader
    {
        private readonly int shift;
        private readonly string[] args;
        private int currentIndex;

        internal OptionsReader(int shift, string[] args)
        {
            Debug.Assert(shift >= 0, "Invalid shift argument.");
            Debug.Assert(args != null, "Missing args.");
            this.shift = shift;
            this.args = args;
            this.currentIndex = shift;
        }

        internal int Count
        {
            get
            {
                return Math.Max(this.args.Length - this.shift, 0);
            }
        }

        internal bool IsParsed()
        {
            return this.currentIndex >= this.args.Length;
        }

        internal bool IsHelp()
        {
            if (this.shift >= this.args.Length)
            {
                return true;
            }

            for (var i = this.shift; i < this.args.Length; i++)
            {
                if (IsOneOf(this.args[i], "-?", "-h", "--help", "/?", "/h", "/help"))
                {
                    return true;
                }
            }

            return false;
        }

        internal void Reset()
        {
            this.currentIndex = this.shift;
        }

        internal string Next()
        {
            if (this.currentIndex > this.args.Length)
            {
                return null;
            }

            return this.currentIndex < this.args.Length ? this.args[this.currentIndex++] : null;
        }

        internal void ReadOption<T>(string optionName, T emptyValue, Parser<T> parser, ref T currentValue)
        {
            if (emptyValue == null ? currentValue != null : !emptyValue.Equals(currentValue))
            {
                throw new ArgumentException("Too many values for option.", optionName);
            }

            if (this.IsParsed())
            {
                throw new ArgumentException("Missing vaue for option.", optionName);
            }

            currentValue = parser.Parse(this.Next());
        }

        internal void ReadSwitch(string optionName, ref bool currentValue)
        {
            if (currentValue)
            {
                throw new ArgumentException("Too many switch instances.", optionName);
            }

            currentValue = true;
        }

        private static bool IsOneOf(string value, params string[] tokens)
        {
            return Array.Exists(tokens, (token) => StringComparer.InvariantCultureIgnoreCase.Equals(value, token));
        }
    }
}
