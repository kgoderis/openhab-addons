//namespace AtsAdvancedTest.Actions
//{
//    using System;
//    using System.Collections.Generic;
//    using System.Linq;
//    using System.Text;
//    using System.Diagnostics;
//
//    internal struct DiscoveryOptions
//    {
//        private readonly int timeout;
//        private readonly int retries;
//
//        internal DiscoveryOptions(ProgramOptions options)
//        {
//            Debug.Assert(options != null, "Missing program options.");
//            this.timeout = options.Timeout;
//            this.retries = options.Retries;
//        }
//
//        internal int Timeout
//        {
//            get
//            {
//                return this.timeout;
//            }
//        }
//
//        internal int Retries
//        {
//            get
//            {
//                return this.retries;
//            }
//        }
//    }
//}
