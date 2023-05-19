//namespace AtsAdvancedTest.Actions
//{
//    using System;
//    using System.Diagnostics;
//using Ace;
//    using Ace.Ats;
//    using System.Collections;
//    using System.Text;
//using System.Collections.Generic;
//
//    internal class ActionSetPreview : ActionLogin
//    {
//        private Action completed;
//        private int sessionId;
//
//        internal ActionSetPreview(Panel panel, LoginOptions options, Action completed)
//            : base(panel, options, completed)
//        {
//        }
//
//        protected override void ExecuteAsync(Action completed)
//        {
//            Debug.Assert(completed != null, "Missing action to be called when the process is completed.");
//            this.completed = completed;
//            this.BeginGetValidAreas();
//        }
//
//        private static void PrintSysEvent(IMessage sysEvent)
//        {
//            Debug.Assert(sysEvent != null, "Missing return.sysevent message.");
//            Debug.Assert(sysEvent.Info != null, "Expected return.sysevent message.");
//            Debug.Assert(StringComparer.Ordinal.Equals("return.sysevent", sysEvent.Info.Id), "Expected return.sysevent message.");
//            StringBuilder sb = new StringBuilder();
//            for(int i=1; i <= 8; i++)
//            {
//                bool active = (bool)sysEvent.GetProperty(string.Format("area.{0}", i), 0, typeof(bool));
//                if (active)
//                {
//                    if (sb.Length > 0)
//                    {
//                        sb.Append(", ");
//                    }
//
//                    sb.Append(i);
//                }
//            }
//
//            var areas = sb.ToString();
//            var categories = AtsUtils.ReadEventCategories(sysEvent);
//            var id = sysEvent.GetProperty("eventUniqueID", 0, typeof(string));
//            var clazz = sysEvent.GetProperty("classID", 0, typeof(string));
//            var obj = sysEvent.GetProperty("objNum", 0, typeof(string));
//            var type = sysEvent.GetProperty("eventTypeID", 0, typeof(string));
//            Console.WriteLine("\tevent: id={0}, class={1}, obj={2}, type={3}, areas=[{4}], categories=[{5}]", id, clazz, obj, type, areas, categories);
//        }
//
//        private void BeginGetValidAreas()
//        {
//            Console.WriteLine("BEGIN READ VALID AREAS");
//            IMessage request = this.Panel.CreateMessage("getValid.Areas");
//            this.Begin(request, this.EndGetValidAreas);
//        }
//
//        private void EndGetValidAreas(IMessage response)
//        {
//            Console.WriteLine("END READ VALID AREAS");
//            if (!StringComparer.Ordinal.Equals("return.validAreas", response.Info.Id))
//            {
//                throw new AtsFaultException(string.Format("Unexpected response message: {0}", response.Info.Id));
//            }
//
//            byte[] areaList = (byte[])response.GetProperty("bitset", 0, typeof(byte[]));
//            this.BeginCreateCC(new BitArray(areaList));
//        }
//
//        private void BeginCreateCC(BitArray areas)
//        {
//            Console.WriteLine("BEGIN CREATE CC A_STATE");
//            var request = this.Panel.CreateMessage("createCC.A_STATE");
//            var sb = new StringBuilder();
//            for (int i = 1; i <= this.Panel.GetMaximumAreaIndex(); i++)
//            {
//                bool enable = areas[i - 1];
//                string prop = string.Format("area.{0}", i);
//                request.SetProperty(prop, 0, enable);
//                if (enable)
//                {
//                    if (sb.Length > 0)
//                    {
//                        sb.Append(", ");
//                    }
//
//                    sb.Append(i);
//                }
//            }
//
//            Console.WriteLine("\tareas = {0}", sb.ToString());
//            for (int i = this.Panel.GetMaximumAreaIndex() + 1; i <= 8; i++)
//            {
//                string prop = string.Format("area.{0}", i);
//                request.SetProperty(prop, 0, false);
//            }
//
//            this.Begin(request, this.EndCreateCC);
//        }
//
//        private void EndCreateCC(IMessage response)
//        {
//            Console.WriteLine("BEGIN CREATE CC A_STATE");
//            if (!StringComparer.Ordinal.Equals("return.short", response.Info.Id))
//            {
//                throw new AtsFaultException(string.Format("Unexpected response message: {0}", response.Info.Id));
//            }
//
//            this.sessionId = (int)response.GetProperty("result", 0, typeof(int));
//            Console.WriteLine("\tsession ID = {0}", this.sessionId);
//            this.BeginReadUninhibitable(true);
//        }
//
//        private void BeginReadUninhibitable(bool first)
//        {
//            Console.WriteLine("BEGIN FN CC A_STATE_GET_UNINH {0}", first ? "first" : "next");
//            var request = this.Panel.CreateMessage("fnCC.A_STATE_GET_UNINH");
//            request.SetProperty("sessionID", 0, this.sessionId);
//            request.SetProperty("next", 0, !first);
//            this.Begin(request, this.EndReadUninhibitable);
//        }
//
//        private void EndReadUninhibitable(IMessage response)
//        {
//            Console.WriteLine("END FN CC A_STATE_GET_UNINH");
//            if (StringComparer.Ordinal.Equals("return.void", response.Info.Id))
//            {
//                this.BeginReadInhibitable(true);
//                return;
//            }
//
//            if (StringComparer.Ordinal.Equals("return.sysevent", response.Info.Id))
//            {
//                PrintSysEvent(response);
//                this.BeginReadUninhibitable(false);
//                return;
//            }
//
//            throw new AtsFaultException(string.Format("Unexpected response message: {0}", response.Info.Id));
//        }
//
//        private void BeginReadInhibitable(bool first)
//        {
//            Console.WriteLine("BEGIN FN CC A_STATE_GET_INH {0}", first ? "first" : "next");
//            var request = this.Panel.CreateMessage("fnCC.A_STATE_GET_INH");
//            request.SetProperty("sessionID", 0, this.sessionId);
//            request.SetProperty("next", 0, !first);
//            this.Begin(request, this.EndReadInhibitable);
//        }
//
//        private void EndReadInhibitable(IMessage response)
//        {
//            Console.WriteLine("END FN CC A_STATE_GET_INH");
//            if (StringComparer.Ordinal.Equals("return.void", response.Info.Id))
//            {
//                this.BeginDestroyCC();
//                return;
//            }
//
//            if (StringComparer.Ordinal.Equals("return.sysevent", response.Info.Id))
//            {
//                PrintSysEvent(response);
//                this.BeginReadInhibitable(false);
//                return;
//            }
//
//            throw new AtsFaultException(string.Format("Unexpected response message: {0}", response.Info.Id));
//        }
//
//        private void BeginDestroyCC()
//        {
//            Console.WriteLine("BEGIN DESTROY CC");
//            var request = this.Panel.CreateMessage("destroyCC.SESSION");
//            request.SetProperty("sessionID", 0, this.sessionId);
//            this.Begin(request, this.EndDestroyCC);
//        }
//
//        private void EndDestroyCC(IMessage response)
//        {
//            Console.WriteLine("END DESTROY CC");
//            this.Completed();
//        }
//
//        private void Begin(IMessage request, Action<IMessage> continuation)
//        {
//            Debug.Assert(continuation != null, "Missing action to process response.");
//            this.Panel.BeginSend(request, this.End, continuation);
//        }
//
//        private void End(IAsyncResult ar)
//        {
//            Action<IMessage> continuation = (Action<IMessage>)ar.AsyncState;
//            Debug.Assert(continuation != null, "Missing action to process response.");
//            try
//            {
//                IMessage response = this.Panel.EndSend(ar);
//                if (response.Info == null)
//                {
//                    Program.Error("The response message is unrecognized.");
//                    this.Completed();
//                }
//                else
//                {
//                    continuation(response);
//                }
//            }
//            catch (AtsFaultException e)
//            {
//                string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
//                Program.Error(message);
//                this.Completed();
//            }
//            catch (Exception e)
//            {
//                Debug.Print("Names completed error {0}: {1}", e, e.StackTrace);
//                Program.Error(e.Message);
//                this.Completed();
//            }
//        }
//
//        private void Completed()
//        {
//            Debug.Print("SET PREVIEW Completed");
//            Action completed = System.Threading.Interlocked.Exchange(ref this.completed, null);
//            Debug.Assert(completed != null, "Missing completed action.");
//            completed();
//        }
//    }
//}
