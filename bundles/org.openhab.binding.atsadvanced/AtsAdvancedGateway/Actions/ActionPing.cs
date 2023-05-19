namespace AtsAdvancedTest.Actions
{
    using System;
    using Ace;
    using Ace.Ats;

    internal class ActionPing : ActionLogin
    {
        internal ActionPing(Panel panel, LoginOptions options, Action completed)
            : base(panel, options, completed)
        {
        }

        protected override void ExecuteAsync(Action completed)
        {
            Console.WriteLine("PING: begin");
            IMessage request = this.Panel.CreateMessage("is.Alive");
            this.Panel.BeginSend(request, this.Completed, completed);
        }

        private void Completed(IAsyncResult ar)
        {
            Console.WriteLine("PING: end");
            Action completed = ar.AsyncState as Action;
            try
            {
                this.Panel.EndSend(ar);
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
                if (completed != null)
                {
                    completed();
                }
            }
        }
    }
}
