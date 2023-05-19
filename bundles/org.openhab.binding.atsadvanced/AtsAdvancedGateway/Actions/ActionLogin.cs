namespace AtsAdvancedTest.Actions
{
    using System;
    using System.Diagnostics;
    using System.Threading;
    using Ace;
    using Ace.Ats;
	using System.Text;

    internal abstract class ActionLogin
    {
        private readonly Panel panel;
        private readonly LoginOptions options;
        private readonly Action completed;
        private Timer dueTimer;

        internal ActionLogin(Panel panel, LoginOptions options, Action completed)
        {
            Debug.Assert(panel != null, "Missing panel.");
            Debug.Assert(completed != null, "Missing completed action.");
            this.panel = panel;
            this.options = options;
            this.completed = completed;
            panel.Timeout = options.Timeout;
            panel.Retries = options.Retries;
            if (options.HeartBeat != -1)
            {
                panel.EnablePingSettings(options.HeartBeat, options.Timeout, options.Retries);
            }
        }

        internal Panel Panel
        {
            get
            {
                return this.panel;
            }
        }

        internal bool Start()
        {
            this.StartDiscover();
            return false;
        }

        private void StartDiscover()
        {
            Console.WriteLine("LOGIN: discovering");
            IMessage request = this.panel.CreateMessage("device.getDescription");
            this.panel.BeginSend(request, this.DiscoverCompleted, null);
        }

        private void DiscoverCompleted(IAsyncResult ar)
        {
            try
            {
                var result = new DeviceDiscovery(this.panel.EndSend(ar));
                Console.WriteLine(ActionDiscovery.GetDeviceDetails(result));
                this.panel.AdjustFactory(result);
                this.panel.SetSerialNumber(Convert.ToInt64(result.SerialNumber, 16));
                if (result.EncryptionMode != 0)
                {
                    this.StartEncryption(result.EncryptionMode);
                }
                else
                {
                    this.StartLogin();
                }
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        private void StartEncryption(int encryptionMode)
        {
            Console.WriteLine("LOGIN: initializing encrypted connection");
            this.Panel.SetEncryptionKey(new MasterKeyProvider(this.options.Password).GetKeyData(encryptionMode));
            byte[] sessionKey = new byte[16];
            AtsUtils.FillRandomBytes(sessionKey);
            var request = this.Panel.CreateMessage("begin.changeSessionKey");
            request.SetProperty("data", 0, sessionKey);
            Action<IMessage> confirm = (r) => BeginConfirmEncryption(r, encryptionMode, sessionKey);
            this.Panel.BeginSend(request, this.EndEncryption, confirm);
        }

        private void EndEncryption(IAsyncResult ar)
        {
            Action<IMessage> confirm = ar.AsyncState as Action<IMessage>;
            try
            {
                IMessage response = this.Panel.EndSend(ar);
                confirm(response);
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        private void BeginConfirmEncryption(IMessage response, int encryptionMode, byte[] sessionKey)
        {
            Console.WriteLine("LOGIN: confirm new session key");
            int length = AtsUtils.GetEncryptionKeySize(encryptionMode);
            byte[] newKey = new byte[length];
            Buffer.BlockCopy(sessionKey, 0, newKey, 0, length / 2);
            sessionKey = (byte[])response.GetProperty("data", 0, typeof(byte[]));
            Buffer.BlockCopy(sessionKey, 0, newKey, length / 2, length / 2);
            var request = this.Panel.CreateMessage("end.changeSessionKey");
            this.Panel.BeginSend(request, this.EndConfirmEncryption, newKey);
        }

        private void EndConfirmEncryption(IAsyncResult ar)
        {
            byte[] sessionKey = ar.AsyncState as byte[];
            try
            {
                IMessage response = this.Panel.EndSend(ar);
                this.panel.SetEncryptionKey(sessionKey);
                this.StartLogin();
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        internal static IMessage CreateLoginRequest(Panel panel, string pin, LoginPurpose options)
        {
			//IMessage result = panel.CreateMessage("device.disconnect");

			IMessage result = panel.CreateMessage("device.getConnect");
			result.SetProperty("userPIN", 0, pin);
			result.SetProperty("userAction_UPLOAD", 0, (options & LoginPurpose.Upload) != LoginPurpose.None);
			result.SetProperty("userAction_DOWNLOAD", 0, (options & LoginPurpose.Download) != LoginPurpose.None);
			result.SetProperty("userAction_CTRL", 0, (options & LoginPurpose.Control) != LoginPurpose.None);
			result.SetProperty("userAction_MONITOR", 0, (options & LoginPurpose.Monitor) != LoginPurpose.None);
			result.SetProperty("userAction_DIAG", 0, (options & LoginPurpose.Diagnostics) != LoginPurpose.None);
			result.SetProperty("userAction_LOGREAD", 0, (options & LoginPurpose.LogView) != LoginPurpose.None);
            return result;
        }

        private void StartLogin()
        {
            Console.WriteLine("LOGIN: login user for '{0}'", this.options.Purpose);
            IMessage request = CreateLoginRequest(this.panel, this.options.Pin, this.options.Purpose);
            this.panel.BeginSend(request, this.LoginCompleted, null);
        }

        protected abstract void ExecuteAsync(Action completed);

		private void StartGetUser()
		{
			Console.WriteLine("LOGIN: get user info for '{0}'", this.options.Purpose);
			IMessage request = panel.CreateMessage("get.UserInfo");
			this.panel.BeginSend(request, this.GetUserCompleted, null);
		}

		private void GetUserCompleted(IAsyncResult ar)
		{
			Console.WriteLine("LOGIN: user info", this.options.Purpose);
			try
			{
				IMessage response = this.panel.EndSend(ar);
				String resp = new StringBuilder()
					.AppendFormat("\tuserID       = {0}", (string)response.GetProperty("userID", 0, typeof(string)))
					.AppendLine()
					.AppendFormat("\tuserName     = {0}", (string)response.GetProperty("userName", 0, typeof(string)))
					.AppendLine()
					.AppendFormat("\tSESSM_UPLOAD     = {0}", response.GetProperty("SESSM_UPLOAD", 0, typeof(Boolean)))
					.AppendLine()
					.AppendFormat("\tSESSM_DOWNLOAD     = {0}", response.GetProperty("SESSM_DOWNLOAD", 0, typeof(Boolean)))
					.AppendLine()
					.AppendFormat("\tSESSM_CTRL     = {0}", response.GetProperty("SESSM_CTRL", 0, typeof(Boolean)))
					.AppendLine()
					.AppendFormat("\tSESSM_MONITOR     = {0}", response.GetProperty("SESSM_MONITOR", 0, typeof(Boolean)))
					.AppendLine()
					.AppendFormat("\tSESSM_DIAG     = {0}", response.GetProperty("SESSM_DIAG", 0, typeof(Boolean)))
					.AppendLine()
					.AppendFormat("\tSESSM_LOGREAD     = {0}", response.GetProperty("SESSM_LOGREAD", 0, typeof(Boolean)))
					.AppendLine()
					.ToString();
				Console.WriteLine("LOGIN: user get result '{0}'", resp);
					

			}
			catch (AtsFaultException e)
			{
				string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
				Program.Error(message);
				Debug.Assert(this.completed != null, "Missing completed action.");
				this.completed();
			}
			catch (Exception e)
			{
				Program.Error(e.Message);
				Debug.Assert(this.completed != null, "Missing completed action.");
				this.completed();
			}
		}

        private void LoginCompleted(IAsyncResult ar)
        {
            Console.WriteLine("LOGIN: run", this.options.Purpose);
            try
            {
                IMessage response = this.panel.EndSend(ar);
				this.StartGetUser();
                this.ExecuteAsync(this.ExecuteCompleted);
            }
            catch (AtsFaultException e)
            {
				string message = string.Format("Fault user login response {0}, {1}", e.Code, e.Message);

				IMessage result = panel.CreateMessage("device.disconnect");
				this.panel.BeginSend(result, this.LoginCompleted, null);

                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        private void ExecuteCompleted()
        {
            Console.WriteLine("LOGIN: ran", this.options.Purpose);
            try
            {
                if (this.options.DueTime != -1)
                {
                    this.StartDueTime();
                }
                else
                {
                    this.StartLogout();
                }
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        private void StartDueTime()
        {
            Console.WriteLine("LOGIN: waiting");
            this.dueTimer = new Timer(this.DueTimeCompleted, null, this.options.DueTime, Timeout.Infinite);
        }

        private void DueTimeCompleted(object state)
        {
            try
            {
                this.StartLogout();
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }

        private void StartLogout()
        {
            Console.WriteLine("LOGIN: logoff user");
            IMessage request = this.panel.CreateMessage("device.disconnect");
            this.panel.BeginSend(request, this.LogoutCompleted, null);
        }

        private void LogoutCompleted(IAsyncResult ar)
        {
            try
            {
                this.panel.EndSend(ar);
            }
            catch (AtsFaultException e)
            {
                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
                Program.Error(message);
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
            }
            finally
            {
                Debug.Assert(this.completed != null, "Missing completed action.");
                this.completed();
            }
        }
    }
}
