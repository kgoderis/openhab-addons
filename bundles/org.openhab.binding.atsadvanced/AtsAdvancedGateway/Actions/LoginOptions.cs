namespace AtsAdvancedTest.Actions
{
    using System.Diagnostics;
    using Ace.Ats;

    internal struct LoginOptions
    {
        private readonly LoginPurpose purpose;
        private readonly int timeout;
        private readonly int retries;
        private readonly string pin;
        private readonly string password;
        private readonly int heartbeat;
        private readonly int duetime;

        internal LoginOptions(ProgramOptions options)
        {
            Debug.Assert(options != null, "Missing program options.");
            this.purpose = LoadPurpose(options);
            this.timeout = options.Timeout;
            this.retries = options.Retries;
            this.pin = options.Pin;
            this.password = options.Password;
            this.heartbeat = options.HeartBeat;
            this.duetime = options.DueTime;
        }

        internal LoginPurpose Purpose
        {
            get
            {
                return this.purpose;
            }
        }

        internal int Timeout
        {
            get
            {
                return this.timeout;
            }
        }

        internal int Retries
        {
            get
            {
                return this.retries;
            }
        }

        internal string Pin
        {
            get
            {
                return this.pin;
            }
        }

        internal string Password
        {
            get
            {
                return this.password;
            }
        }

        internal int HeartBeat
        {
            get
            {
                return this.heartbeat;
            }
        }

        internal int DueTime
        {
            get
            {
                return this.duetime;
            }
        }

        private static LoginPurpose LoadPurpose(ProgramOptions options)
        {
            var result = LoginPurpose.None;
            if (options.Upload)
            {
                result |= LoginPurpose.Upload;
            }

            if (options.Download)
            {
                result |= LoginPurpose.Download;
            }

            if (options.Control)
            {
                result |= LoginPurpose.Control;
            }

            if (options.Monitor)
            {
                result |= LoginPurpose.Monitor;
            }

            if (options.Diagnostics)
            {
                result |= LoginPurpose.Diagnostics;
            }

            if (options.LogView)
            {
                result |= LoginPurpose.LogView;
            }

            return result;
        }
    }
}
