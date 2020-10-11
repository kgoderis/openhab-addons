namespace AtsAdvancedTest.Actions
{
    using System;
    using System.Diagnostics;
    using System.Text;
    using Ace;
    using Ace.Ats;

    internal class ActionDiscovery
    {
        private readonly DiscoveryOptions options;
        private readonly Action completed;
        private readonly Panel panel;

        internal ActionDiscovery(Panel panel, DiscoveryOptions options, Action completed)
        {
            Debug.Assert(panel != null, "Missing panel.");
            Debug.Assert(completed != null, "Missing completed action.");
            this.panel = panel;
            this.options = options;
            this.completed = completed;
            panel.Timeout = options.Timeout;
            panel.Retries = options.Retries;
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

        internal bool Start()
        {
            IMessage request = this.panel.CreateMessage("device.getDescription");
            this.panel.BeginSend(request, this.DiscoverCompleted, null);
            return false;
        }

        private void DiscoverCompleted(IAsyncResult ar)
        {
            try
            {
                var result = new DeviceDiscovery(this.panel.EndSend(ar));
                Console.WriteLine(GetDeviceDetails(result));
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
