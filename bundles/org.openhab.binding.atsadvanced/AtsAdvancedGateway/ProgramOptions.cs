namespace AtsAdvancedGateway
{
	using System;
	using System.Diagnostics;
	using System.Net;
	using Parsers;
	using System.Text;
	using System.IO;

	public class ProgramOptions
	{
		private readonly bool help;
		private int port = -1;
		private string driverpath;

		internal ProgramOptions(params string[] args)
		{
			var optionsReader = new OptionsReader(0, args);
			if (optionsReader.IsHelp())
			{
				help = true;
				return;
			}

			var optionParser = new ParserOptionType();
			while (!optionsReader.IsParsed())
			{
				var optionName = optionsReader.Next();
				var option = optionParser.Parse(optionName);
				switch (option)
				{
					case OptionType.Port:
						optionsReader.ReadOption(optionName, -1, new ParserPort(), ref port);
						break;

					case OptionType.Driver:
						optionsReader.ReadOption(optionName, null, new ParserDriverFile(), ref driverpath);
						break;

					default:
						throw new NotImplementedException(string.Format("Option '{0}' is not implemented yet.", optionName));
				}
			}
		}

		internal bool Help
		{
			get
			{
				return help;
			}
		}

		internal int Port
		{
			get
			{
				Debug.Assert(!help, "Unexpected access to the property while help option is specified.");
				if (port == -1)
				{
					throw new ArgumentException("Missing option '--port' in which the server is being started.");
				}

				Debug.Assert((port >= IPEndPoint.MinPort) && (port <= IPEndPoint.MaxPort), "Invalid port number.");
				return port;
			}
		}

		internal string DriverPath
		{
			get
			{
				Debug.Assert(!help, "Unexpected access to the property while help option is specified.");

				return driverpath;
			}
		}

		internal void Usage()
		{
			Usage(
				"[options]",
				"-h, --help, -?           This help string.",
				"-p, --port <port>        The port number on which the web services are listening on.");
		}

		internal void Usage(string options, params string[] details)
		{
			var syntax = new StringBuilder()
					.AppendFormat("Syntax: {0} {1}", Path.GetFileNameWithoutExtension(Process.GetCurrentProcess().MainModule.ModuleName), options)
					.AppendLine();
			if (details.Length > 0)
			{
				syntax
						.AppendLine()
						.AppendLine("Options:")
						.AppendLine();
				foreach (var detail in details)
				{
					syntax.AppendLine(detail);
				}
			}

			Console.WriteLine(syntax);
		}
	}
}
