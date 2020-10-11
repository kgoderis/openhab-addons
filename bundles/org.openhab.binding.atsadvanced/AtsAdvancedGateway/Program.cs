namespace AtsAdvancedGateway
{
	using System;
	using System.Diagnostics;
	using System.Collections.Generic;
	using System.IO;
	using System.Net;
	using Ace;
	using Ace.Messages;
	using Ace.Ats;
	using ServiceStack;
	using System.Threading;
	using ServiceStack.Text;
	using System.Net.NetworkInformation;
	using System.Security.Permissions;

	public static class Global
	{
		public static ManualResetEvent completed = new ManualResetEvent(false);
		public static Panel panel { get; set; }
		public static ProgramOptions options { get; set; }
		public static AppHost appHost { get; set; }
		public static PanelService panelService { get; set; }
	}

	// Set the UnmanagedCode property.
	[SecurityPermissionAttribute(SecurityAction.Assert, UnmanagedCode = true)]

	class Program
	{
		private static void Main(string[] args)
		{

			try
			{
				Global.options = new ProgramOptions(args);
				if (Global.options.Help)
				{
					Global.options.Usage();
					return;
				}
					
				var listeningOn = "http://*:" + Global.options.Port.ToString() + "/";

				JsConfig<string>.SerializeFn = time =>
				{
					Console.WriteLine("Serialize {0}", time);
					if (time.IsNullOrEmpty())
					{
						return "[Empty]";
					}
					else
					{
						return time;
					}
				};

				Global.appHost = new AppHost();
				Global.appHost.Init();
				Global.appHost.Start(listeningOn);


				Console.WriteLine("AppHost Created at {0}, listening on {1}",
								   DateTime.Now, listeningOn);
			}
			catch (Exception e)
			{
				Console.WriteLine("Exception : {0}", e.Message);
			}

			Global.completed.WaitOne();

		}

		public static void Info(string message)
		{
			Console.WriteLine("INFO: {0}", message);
			Global.panelService.logMessage("msg.info", message);
		}

		public static void Warn(string message)
		{
			Console.WriteLine("WARNING: {0}", message);
			Global.panelService.logMessage("msg.warn", message);
		}

		public static void Error(string message)
		{
			Console.Error.WriteLine("ERROR: {0}", message);
			if ((Environment.ExitCode == 0) || (Environment.ExitCode == -1))
			{
				Environment.ExitCode = -2;
			}

			if (message.Contains("Address already in use") || message.Contains("The socket has been shut down") || message.Contains("Connection reset by peer") || message.Contains("Object reference not set to an instance of an object"))
			{
				Global.panelService.logMessage("msg.error", message);
				Console.Error.WriteLine("Exiting with code: {0}", Environment.ExitCode);
				Environment.Exit(-2);
			}

			Global.panelService.logMessage("msg.error", message);
		}

	}

	[Route("/configurepanel")]
	public class ConfigurePanel : IReturn<ConfigurePanelResponse>
	{
		public string Hostaddress { get; set; }
		public int Port { get; set; }
		public string Password { get; set; }
		public int Hearbeat { get; set; }
		public int Timeout { get; set; }
		public int Retries { get; set; }
	}

	public class ConfigurePanelResponse
	{
		public string Result { get; set; }
	}

	[Route("/message")]
	public class Message : IReturn<MessageResponse>
	{
		public string name { get; set; }
		public List<Property> Properties { get; set; }
	}

	public class Property
	{
		public string id { get; set; }
		public int index { get; set; }
		public object value { get; set; }
	}

	public class MessageResponse
	{
		public string name { get; set; }
		public List<Property> Properties { get; set; }
	}

	public class AppHost : AppSelfHostBase
	{
		public AppHost()
				: base("HttpListener Self-Host", typeof(PanelService).Assembly)
		{
		}

		public override void Configure(Funq.Container container)
		{
			Plugins.Add(new ServerEventsFeature());
			Plugins.Add(new RequestLogsFeature());
			SetConfig(new HostConfig { DebugMode = true });
			//JsConfig.IncludeTypeInfo = true;
			//JsConfig.IncludePublicFields = true;x	
			JsConfig.IncludeNullValues = true;
			JsConfig.DateHandler = DateHandler.ISO8601;
		}
	}

	public class PanelService : Service, IEventListener
	{
		public IMessage response;
		public IServerEvents serverEvents { get; set; }
		public IMessageDriver driver { get; set; }

		public PanelService()
		{
			Global.panelService = this;
		}

		public object Any(ConfigurePanel request)
		{
			Console.WriteLine("Receiving a request to configure the panel: {0}", request.Hostaddress);

			if (driver == null)
			{
				Console.WriteLine("Loading the message driver");
				driver = LoadMessageDriver();
			}

			Global.panel = new Panel(IPAddress.Parse(request.Hostaddress), request.Port, "0000000000000000000000000000000000000000000000000000", request.Hearbeat, request.Timeout, request.Retries, driver);
			Console.WriteLine("Starting discovery of the panel");
			var result = Global.panel.StartDiscover();

			if (Global.panel != null)
			{
				Global.panel.AddListener(this);
				Console.WriteLine("Panel event listener registered at {0}",
					DateTime.Now);
			}

			Console.WriteLine("Sending response of ConfigurePanel to openHAB: {0}", result);

			return new ConfigurePanelResponse { Result = result };

		}

		public object Any(Message request)
		{
			var messageResponse = new MessageResponse();
			messageResponse.Properties = new List<Property>();

			try
			{
				Console.WriteLine("Received message from openHAB: {0}", request.name);
				var msg = Global.panel.CreateMessage(request.name);

				Console.WriteLine("\t {0} properties", request.Properties.Count);
				foreach (Property property in request.Properties)
				{
					Console.WriteLine("\t setting properties {0}, index {1} with value {2} and type {3}", property.id, property.index, property.value, property.value.GetType());
					if (property.value.Equals("true")) {
						msg.SetProperty(property.id, property.index, true);
					}
					else if (property.value.Equals("false")) {
						msg.SetProperty(property.id, property.index, false);
					}
					else
					{
						msg.SetProperty(property.id, property.index, property.value);
					}
				}

				Console.WriteLine("Sending message to Panel: {0}", request.name);
				var sendResult = Global.panel.BeginSend(msg, null, null);
				sendResult.AsyncWaitHandle.WaitOne();
				var finalResponse = Global.panel.EndSend(sendResult);
				sendResult.AsyncWaitHandle.Close();

				Console.WriteLine("Received response from Panel: {0}", finalResponse.Info.Id);
				IMessageInfo finalInfo = finalResponse.Info;
				IPropertyInfoCollection finalProperties = finalInfo.Properties;

				messageResponse.name = finalResponse.Info.Id;

				foreach (IPropertyInfo someInfo in finalProperties)
				{
					IAttributeCollection propertyAttributes = someInfo.Attributes;

					var multiplicity = propertyAttributes["multiplicity"];
					if (multiplicity == "dynamic")
					{
						for (int i = 0; i < finalResponse.Count; i++)
						{
							Property property = new Property();
							property.id = someInfo.Id;
							property.index = i;
							property.value = finalResponse.GetProperty(someInfo.Id, i + 1, null);

							if (someInfo.Id == "name" && ((string)property.value).IsNullOrEmpty())
							{
								property.value = "[Empty]";
							}

							Console.WriteLine("\t respond property {0}, index {1} with value '{2}'", property.id, property.index, property.value);
							messageResponse.Properties.Add(property);

						}
					}
					else
					{

						if (finalResponse.GetPropertyStatus(someInfo.Id, 0) == PropertyStatus.Ok)
						{
							Property property = new Property();
							property.id = someInfo.Id;
							property.index = 0;

							if (someInfo.Id == "device.FWID_MAC")
							{
								var address = (byte[])finalResponse.GetProperty("device.FWID_MAC", 0, typeof(byte[]));
								PhysicalAddress someAddress = new PhysicalAddress(address);
								property.value = someAddress;
								if (Array.TrueForAll(address, (b) => b == 0))
								{
									property.value = null;
								}
							}
							else if (someInfo.Id == "userCARD")
							{
								var address = (byte[])finalResponse.GetProperty("userCARD", 0, typeof(byte[]));
								property.value = "test";
							}
							else if (someInfo.Id == "bitSet")
							{
								byte[] bitSet = (byte[])finalResponse.GetProperty("bitSet", 0, typeof(byte[]));
								property.value = bitSet;
							}
							else
							{
								property.value = finalResponse.GetProperty(someInfo.Id, 0, null);
							}

							Console.WriteLine("\t respond property {0}, index {1} with value '{2}'", property.id, property.index, property.value);
							messageResponse.Properties.Add(property);
						}
					}
				}
			}
			catch (AtsFaultException e)
			{
				messageResponse.name = "return.error";
				Property property = new Property();
				property.id = "result";
				property.index = 0;
				property.value = string.Format("Fault response {0}, {1}", e.Code, e.Message);
				messageResponse.Properties.Add(property);
			}
			catch (Exception e)
			{
				messageResponse.name = "return.error";
				Property property = new Property();
				property.id = "result";
				property.index = 0;
				property.value = e.Message;
				messageResponse.Properties.Add(property); ;
			}

			Console.WriteLine("Sending response to openHAB: {0}", messageResponse.name);
			return messageResponse;
		}

		public void SendMessageCompleted(IAsyncResult ar)
		{
			try
			{
				response = Global.panel.EndSend(ar);
			}
			catch (Exception e)
			{
				Program.Error(e.Message);
				IMessage faultResponse = Global.panel.CreateMessage("return.bool");
				faultResponse.SetProperty("result", 0, 1);
				response = faultResponse;
			}

		}

		public bool Received(ISender device, IMessage message)
		{
			Console.WriteLine("Received event from Panel : {0}: {1}", message.ToString(), message.Info.Id);

			var logEntry = new MessageResponse();
			logEntry.Properties = new List<Property>();
			logEntry.name = message.Info.Id;

			IMessageInfo finalInfo = message.Info;
			IPropertyInfoCollection finalProperties = finalInfo.Properties;
			int propertyCounter = 0;

			foreach (IPropertyInfo someInfo in finalProperties)
			{
				Console.WriteLine("Property: {0}", someInfo.Id);
				IAttributeCollection propertyAttributes = someInfo.Attributes;

				var multiplicity = propertyAttributes["multiplicity"];
				if (multiplicity == "dynamic")
				{
					for (int i = 0; i < message.Count; i++)
					{
						Console.WriteLine("Property added: {0} : {1} : {2}", someInfo.Id, i + 1, message.GetProperty(someInfo.Id, i + 1, null));
						var logProperty = new Property();
						logProperty.id = someInfo.Id;
						logProperty.index = i + 1;
						logProperty.value = message.GetProperty(someInfo.Id, i + 1, null);
						logEntry.Properties.Add(logProperty);
						propertyCounter++;
					}
				}
				else
				{
					if (message.GetPropertyStatus(someInfo.Id, 0) == PropertyStatus.Ok)
					{
						Console.WriteLine("Property added: {0} : {1}", someInfo.Id, message.GetProperty(someInfo.Id, 0, null));
						var logProperty = new Property();
						logProperty.id = someInfo.Id;
						logProperty.index = 0;
						logProperty.value = message.GetProperty(someInfo.Id, 0, null);
						logEntry.Properties.Add(logProperty);
						propertyCounter++;
					}
					else
					{
						Console.WriteLine("Property not OK: {0}", someInfo.Id);
					}
				}
			}

			Console.WriteLine("{0} properties will be send to openHAB", propertyCounter);
			if (serverEvents != null)
			{
				serverEvents.NotifyAll("cmd.panelevent", logEntry);
			}

			return true;
		}

		internal void logMessage(String name, String message)
		{
			var logEntry = new MessageResponse();
			logEntry.Properties = new List<Property>();
			logEntry.name = name;

			var logProperty = new Property();
			logProperty.id = "result";
			logProperty.index = 0;
			logProperty.value = message;

			logEntry.Properties.Add(logProperty);
			if (serverEvents != null)
			{
				serverEvents.NotifyAll("cmd.panelevent", logEntry);
			}

		}

		public IMessageDriver LoadMessageDriver()
		{
			string driverFileName = "ats.advanced.drv";
			Console.WriteLine("Driver path is {0}", Path.Combine(Path.GetDirectoryName(Process.GetCurrentProcess().MainModule.ModuleName), driverFileName));

			IMessageDriver result = LoadMessageDriverFrom(Path.GetFullPath(Global.options.DriverPath))
											  ?? LoadMessageDriverFrom(Path.Combine(Path.GetDirectoryName(Process.GetCurrentProcess().MainModule.ModuleName), driverFileName))
											  ?? LoadBuildInDriver();

			return result;
		}

		private IMessageDriver LoadMessageDriverFrom(string path)
		{
			if (!File.Exists(path))
			{
				return null;
			}

			Stream input = null;
			try
			{
				input = File.OpenRead(path);
				IMessageDriver result = MessageDriverLoader.LoadDriver(input);
				if (result != null)
				{
					Program.Info(string.Format("Using message driver from '{0}'.", path));
				}

				return result;
			}
			catch (Exception e)
			{
				Program.Warn(string.Format("Cannot load message driver file from '{0}': {1}", path, e));
				return null;
			}
			finally
			{
				if (input != null)
				{
					input.Dispose();
				}
			}
		}

		private IMessageDriver LoadBuildInDriver()
		{
			using (Stream input = System.Reflection.Assembly.GetExecutingAssembly().GetManifestResourceStream("AtsAdvancedGateway.ats.advanced.drv"))
			{
				Program.Info("INFO: Using build-in message driver.");
				return MessageDriverLoader.LoadDriver(input);
			}
		}
	}
}

