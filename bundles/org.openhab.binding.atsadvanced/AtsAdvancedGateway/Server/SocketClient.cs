namespace AtsAdvancedTest.Server
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;
using Ace.Communication;
    using System.Net.Sockets;

    internal sealed class SocketClient : ClientProgram
    {
        private readonly TcpCommunicationChannel channel;

        internal SocketClient(TestAction testAction, ProgramOptions options)
            : base(testAction, options)
        {
            this.channel = new TcpCommunicationChannel(options.HostAddress, options.Port);
        }

        protected override Ace.ICommunicationChannel GetChannel()
        {
            return this.channel;
        }

    }
}
