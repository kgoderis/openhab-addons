namespace AtsAdvancedTest.Server
{
    using Ace.Communication;

    internal sealed class SerialClient : ClientProgram
    {
        private readonly SerialPortChannel channel;

        internal SerialClient(TestAction testAction, ProgramOptions options)
            : base(testAction, options)
        {
            this.channel = new SerialPortChannel();
            this.channel.Open(options.SerialPort, 115200, System.IO.Ports.Parity.None, 8, System.IO.Ports.StopBits.One);
        }

        protected override Ace.ICommunicationChannel GetChannel()
        {
            return this.channel;
        }
    }
}
