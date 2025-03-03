namespace AtsAdvancedTest.Server
{
    using System;
    using System.Diagnostics;
    using System.Threading;
    using Ace;

    internal abstract class ClientProgram
    {
        /// <summary>
        /// The test action to be perfomed on established connection.
        /// </summary>
        private readonly TestAction testAction;

        private readonly ManualResetEvent completed = new ManualResetEvent(false);

        /// <summary>
        /// Initializes a new instance of the <see cref="Server"/> class.
        /// </summary>
        /// <param name="testAction">The test action to be perfomed on established connection.</param>
        /// <param name="options">Program options.</param>
        internal ClientProgram(TestAction testAction, ProgramOptions options)
        {
            Debug.Assert(testAction != null, "Missing test action.");
            Debug.Assert(options != null, "Missing program options.");
            this.testAction = testAction;
        }

        internal void Run()
        {
            var channel = GetChannel();
            Debug.Assert(this.testAction != null, "Missing test action.");
//            this.testAction(new Panel(), this.ActionCompleted, channel);

            // Wait until the action is completed.
            Debug.Print("Client is waiting for the end of test action.");
            this.completed.WaitOne();
        }

        protected abstract ICommunicationChannel GetChannel();

        private void ActionCompleted(object state)
        {
            Debug.Print("The action is completed.");
            try
            {
                var disposable = state as IDisposable;
                if (disposable != null)
                {
                    disposable.Dispose();
                }
            }
            finally
            {
                this.completed.Set();
            }
        }
    }
}
