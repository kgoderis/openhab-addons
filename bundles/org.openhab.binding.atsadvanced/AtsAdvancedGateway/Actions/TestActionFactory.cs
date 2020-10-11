namespace AtsAdvancedTest.Actions
{
    using System;
    using System.Diagnostics;

    internal class TestActionFactory
    {
        private readonly Func<Panel, Action, Func<bool>> actionActivator;

        internal TestActionFactory(ProgramOptions options)
        {
            if (options.ActionType == TestActionType.Discovery)
            {
                var discoveryOptions = new DiscoveryOptions(options);
                this.actionActivator = (panel, c) => new ActionDiscovery(panel, discoveryOptions, c).Start;
                return;
            }

            LoginOptions loginOptions = new LoginOptions(options);
            switch (options.ActionType)
            {
                case TestActionType.Login:
                    this.actionActivator = (panel, c) => new ActionPing(panel, loginOptions, c).Start;
                    return;

                case TestActionType.Names:
                    this.actionActivator = (panel, c) => new ActionNames(panel, loginOptions, c).Start;
                    return;

                case TestActionType.SetPreview:
                    this.actionActivator = (panel, c) => new ActionSetPreview(panel, loginOptions, c).Start;
                    return;
            }

            throw new NotImplementedException(string.Format("The test action '{0}' is not implemented yet.", options.ActionType));
        }

        internal bool Run(Panel panel, Action<object> completed, object state)
        {
            Action safeCompleted = () => Completed(panel, state, completed);
            try
            {
                return this.actionActivator(panel, safeCompleted)();
            }
            catch (Exception e)
            {
                Debug.Print("PERFORM TEST ERROR {0}", e);
                Program.Error(e.Message);
                safeCompleted();
                return true;
            }
        }

        private static void Completed(Panel panel, object state, Action<object> completed)
        {
            try
            {
                if (completed != null)
                {
                    completed(state);
                }
            }
            catch (Exception e)
            {
                Debug.Print("COMPLETED ERROR {0}", e);
                Program.Error(e.Message);
            }
            finally
            {
                IDisposable dispose = panel as IDisposable;
                if (dispose != null)
                {
                    dispose.Dispose();
                }
            }
        }
    }
}
