namespace AtsAdvancedGateway.Parsers
{
    using System.Text;

	internal class ParserDriverFile : Parser<string>
    {
        internal override string Parse(string value)
        {
            if (string.IsNullOrEmpty(value))
            {
				Program.Warn("The given path is empty.");
            }

            return value;
        }
    }
}
