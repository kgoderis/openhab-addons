//namespace AtsAdvancedTest.Actions
//{
//    using System;
//    using Ace;
//    using Ace.Ats;
//    using System.Diagnostics;
//    using System.Threading;
//
//    internal class ActionNames : ActionLogin
//    {
//        private Action completed;
//        private NameReader runner;
//
//        internal ActionNames(Panel panel, LoginOptions options, Action completed)
//            : base(panel, options, completed)
//        {
//        }
//
//        protected override void ExecuteAsync(Action completed)
//        {
//            Console.WriteLine("NAMES: begin");
//            Debug.Assert(completed != null, "Missing completed action.");
//            Debug.Assert(this.completed == null, "Unexpected this.completed action.");
//            this.completed = completed;
//            this.StartAreas();
//        }
//
//        private void StartAreas()
//        {
//            this.Start("select.AreaNames", 1, this.Panel.GetMaximumAreaIndex(), this.StartTriggers);
//        }
//
//        private void StartTriggers()
//        {
//            this.Start("select.TriggerNames", 1, 255, this.StartSystem);
//        }
//
//        private void StartSystem()
//        {
//            this.Start("select.SYSNames", 1, 1, this.StartDgps);
//        }
//
//        private void StartDgps()
//        {
//            this.Start("select.DGPNames", 1, 7, this.StartRases);
//        }
//
//        private void StartRases()
//        {
//            this.Start("select.RASNames", 1, 8, this.StartZones);
//        }
//
//        private void StartZones()
//        {
//            this.Start("select.ZoneNames", 1, 368, this.StartFilters);
//        }
//
//        private void StartFilters()
//        {
//            this.Start("select.CEvFilterNames", 1, 64, this.StartOutputs);
//        }
//
//        private void StartOutputs()
//        {
//            this.Start("select.OutputNames", 1, 200, this.StartCS);
//        }
//
//        private void StartCS()
//        {
//            this.Start("select.CSNames", 1, 16, this.StartPCC);
//        }
//
//        private void StartPCC()
//        {
//            this.Start("select.PCCNames", 1, 16, this.StartDL);
//        }
//
//        private void StartDL()
//        {
//            this.Start("select.DLNames", 1, 6, this.StartSchedAct);
//        }
//
//        private void StartSchedAct()
//        {
//            this.Start("select.SchedActNames", 1, 64, this.StartSchedActLst);
//        }
//
//        private void StartSchedActLst()
//        {
//            this.Start("select.SchedActLstNames", 1, 32, this.StartSchedExc);
//        }
//
//        private void StartSchedExc()
//        {
//            this.Start("select.SchedExcNames", 1, 64, this.StartSchedules);
//        }
//
//        private void StartSchedules()
//        {
//            this.Start("select.ScheduleNames", 1, 4, this.StartFobs);
//        }
//
//        private void StartFobs()
//        {
//            this.Start("select.FobNames", 1, 112, this.StartUserGroups);
//        }
//
//        private void StartUserGroups()
//        {
//            this.Start("select.UserGroupNames", 1, 16, this.StartUsers);
//        }
//
//        private void StartUsers()
//        {
//            this.Start("select.UserNames", 1, 50, this.Completed);
//        }
//
//        private void Start(string message, int minimum, int maximum, Action continuation)
//        {
//            this.runner = new NameReader(this.Panel, message, minimum, maximum, continuation);
//            this.runner.Start();
//        }
//
//        private void Completed()
//        {
//            this.runner = null;
//            Action completed = System.Threading.Interlocked.Exchange(ref this.completed, null);
//            Debug.Assert(completed != null, "Missing completed action.");
//            completed();
//        }
//
//        private class NameReader
//        {
//            private readonly Panel panel;
//            private readonly string request;
//            private readonly int limit;
//            private readonly Action completed;
//            private int current;
//
//            internal NameReader(Panel panel, string request, int minimum, int maximum, Action completed)
//            {
//                Debug.Assert(panel != null, "Missing panel proxy.");
//                Debug.Assert(!string.IsNullOrEmpty(request), "Missing request message name.");
//                Debug.Assert(minimum <= maximum, "Minimum cannot be greater than maximum.");
//                Debug.Assert(completed != null, "Missing action to call when the operation is completed.");
//                this.panel = panel;
//                this.request = request;
//                this.current = minimum;
//                this.limit = maximum;
//                this.completed = completed;
//            }
//
//            internal void Start()
//            {
//                this.RequestChunk();
//            }
//
//            private void RequestChunk()
//            {
//                Console.WriteLine("NAMES: {0} from {1}", this.request, this.current);
//                var request = this.panel.CreateMessage(this.request);
//                IPropertyInfo info;
//                try
//                {
//                    info = request.Info.Properties["index"];
//                }
//                catch (ArgumentException)
//                {
//                    info = null;
//                }
//                if (info != null)
//                {
//                    request.SetProperty("index", 0, this.current);
//                }
//                else
//                {
//                    Debug.Assert(this.current == this.limit, "Expected only element.");
//                }
//
//                this.panel.BeginSend(request, this.RequestCompleted, null);
//            }
//
//            private void RequestCompleted(IAsyncResult ar)
//            {
//                Console.WriteLine("NAMES: response");
//                try
//                {
//                    var result = this.panel.EndSend(ar);
//                    ShowNames(result);
//                    this.current += result.Count;
//                    if (this.current <= this.limit)
//                    {
//                        this.RequestChunk();
//                    }
//                    else
//                    {
//                        this.completed();
//                    }
//                }
//                catch (AtsFaultException e)
//                {
//                    string message = string.Format("Fault response {0}, {1}", e.Code, e.Message);
//                    Program.Error(message);
//                    this.completed();
//                }
//                catch (Exception e)
//                {
//                    Debug.Print("Names completed error {0}: {1}", e, e.StackTrace);
//                    Program.Error(e.Message);
//                    this.completed();
//                }
//            }
//
//            private static void ShowNames(IMessage response)
//            {
//                Debug.Assert(response != null, "Missing response message with object names.");
//                if (response.Info == null)
//                {
//                    throw new Exception("Unrecognized response message, but expected response with object names.");
//                }
//
//                Console.WriteLine("\t{0} with {1} items", response.Info.Id, response.Count);
//                var first = (int)response.GetProperty("index", 0, typeof(int));
//                for (int i = 0; i < response.Count; i++)
//                {
//                    Console.WriteLine("\t#{0}: {1}", i + first, response.GetProperty("name", i + 1, null));
//                }
//            }
//        }
//    }
//}
