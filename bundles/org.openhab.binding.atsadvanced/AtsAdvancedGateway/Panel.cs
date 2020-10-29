namespace AtsAdvancedGateway
{
    using System;
	using System.Net;
    using System.Text;
    using Ace.Ats;
    using Ace;
    using System.Diagnostics;
	using Ace.Communication;
	using Actions;

    public class Panel
    {
		private IMessageFactory factory;
        private AtsAdvancedPanel panel = new AtsAdvancedPanel();
		private TcpCommunicationChannel channel;

		internal Panel(IPAddress address, int port, string password, int heartbeat, int timeout, int retries,IMessageDriver driver)
		{
			factory = driver.CreateFactory();
			panel.EventMessageFactory = factory;

			Debug.Assert (address != null, "Missing host name of the panel.");
			Debug.Assert (port != 0, "Missing port number of the panel.");
			Debug.Assert (heartbeat != 0, "Missing heartbeat period of the panel.");
			Debug.Assert (timeout != 0, "Missing timeout period of the panel.");
			Debug.Assert (retries != 0, "Missing number of retries of the panel.");

			Hostaddress = address;
			Port = port;
			Password = password;
			Heartbeat = heartbeat;
			Timeout = timeout;
			Retries = retries;

			if (Heartbeat != -1)
			{
				EnablePingSettings(Heartbeat, Timeout, Retries);
			}

			Console.WriteLine ("Channel with host: {0}",Hostaddress);
			Console.WriteLine ("Channel with port: {0}",Port);
			channel = new TcpCommunicationChannel(Hostaddress, Port);
			if (channel != null) {
				Console.WriteLine ("Channel is not null");
			}
			panel.Open(channel, true);

        }
			
		public IPAddress Hostaddress
		{
			get;
			set;
		}

		public int Port
		{
			get;
			set;
		}
		public string Password
		{
			get;
			set;
		}

		public int Heartbeat
		{
			get;
			set;
		}

		public int Timeout
        {
            get;
            set;
        }

		public int Retries
        {
            get;
            set;
        }

		public void AddListener(IEventListener aListener) {
			panel.AddListener (aListener);
		}

        internal void SetSerialNumber(long serialNumber)
        {
            panel.SerialNumber = serialNumber;
        }

        internal void SetEncryptionKey(byte[] key)
        {
            panel.SetEncryptionKey(key);
        }

        internal void EnablePingSettings(int period, int timeout, int retries)
        {
            panel.PingSettings = new PingSettings(period, timeout, retries);
        }

        internal IMessage CreateMessage(string name)
        {
            return factory.CreateMessage(name);
        }

        internal IAsyncResult BeginSend(IMessage message, AsyncCallback callback, object state)
        {
            return panel.BeginSend(message, Timeout, Retries, callback, state);
        }

        internal IMessage EndSend(IAsyncResult ar)
        {
            return panel.EndSend(ar);
        }

        internal void AdjustFactory(DeviceDiscovery info)
        {
            try
            {
                factory.SetProperty("model", 0, info.Model);
				}
            catch (Exception e)
            {
				throw new Exception (string.Format("Unable to setup model context in the factory: '{0}'. {1}", info.Model, e));
            }

            try
            {
                var protocol = int.Parse(info.Version.Split('.')[1]);
                do
                {
                    try
                    {
						factory.SetProperty("protocol", 0, protocol);
						break;
                    }
                    catch (Exception ex)
                    {
                        protocol--;
                    }
                } while (protocol > 0);
            }
            catch (Exception e)
            {
				throw new Exception (string.Format ("Unable to setup protocol context in the factory: '{0}'. {1}", info.Version, e));
            }
        }

        internal int GetMaximumAreaIndex()
        {
            return (int)factory.GetProperty("maximum-area-index", 0, typeof(int));
        }

		public string StartDiscover()
		{
			string feedbackMessage = "1";

			try
			{
				var request = CreateMessage("device.getDescription");

				var sendResult = BeginSend(request, null,null);
				sendResult.AsyncWaitHandle.WaitOne ();
				var result = new DeviceDiscovery(panel.EndSend(sendResult));
				sendResult.AsyncWaitHandle.Close ();

				AdjustFactory(result);
				SetSerialNumber(Convert.ToInt64(result.SerialNumber, 16));

				if (result.EncryptionMode != 0)
				{
					SetEncryptionKey(new MasterKeyProvider(Password).GetKeyData(result.EncryptionMode));
					byte[] sessionKey = new byte[16];

					AtsUtils.FillRandomBytes(sessionKey);

					var sessionRequest = CreateMessage("begin.changeSessionKey");
					sessionRequest.SetProperty("data", 0, sessionKey);

					sendResult = BeginSend(sessionRequest, null, null);
					sendResult.AsyncWaitHandle.WaitOne ();
					var sessionResponse = EndSend(sendResult);
					sendResult.AsyncWaitHandle.Close();

					var length = AtsUtils.GetEncryptionKeySize(result.EncryptionMode);
					byte[] newKey = new byte[length];
					Buffer.BlockCopy(sessionKey, 0, newKey, 0, length / 2);
					sessionKey = (byte[])sessionResponse.GetProperty("data", 0, typeof(byte[]));
					Buffer.BlockCopy(sessionKey, 0, newKey, length / 2, length / 2);

					var sessionEndRequest = CreateMessage("end.changeSessionKey");
					sendResult = BeginSend(sessionEndRequest, null, null);
					sendResult.AsyncWaitHandle.WaitOne ();
					var sessionEndresponse = EndSend(sendResult);
					sendResult.AsyncWaitHandle.Close();

					panel.SetEncryptionKey(newKey);
				}
			}
			catch (AtsFaultException e)
			{
				feedbackMessage = string.Format("ATS Fault response {0}, {1}", e.Code, e.Message);
				Program.Error(feedbackMessage);

			}
			catch (Exception e)
			{
				feedbackMessage = string.Format("General Fault response {0}", e.Message);
				Program.Error(feedbackMessage);

			}

			return feedbackMessage;
		}

		internal static string GetDeviceDetails(DeviceDiscovery info)
		{
			return new StringBuilder()
				.AppendLine("DISCOVERY")
				.AppendFormat("\tname       = {0}", info.DeviceName)
				.AppendLine()
				.AppendFormat("\tmodel      = {0}", info.Model)
				.AppendLine()
				.AppendFormat("\tversion    = {0}", info.Version)
				.AppendLine()
				.AppendFormat("\tserial     = {0}", info.SerialNumber)
				.AppendLine()
				.AppendFormat("\tMAC        = {0}", info.Mac)
				.AppendLine()
				.AppendFormat("\tencryption = {0}", info.EncryptionMode)
				.AppendLine()
				.ToString();
		}

    }
}
