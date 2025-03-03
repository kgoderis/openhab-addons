namespace AtsAdvancedTest
{
    using System;
    using Ace;

    internal delegate bool TestAction(Panel panel, Action<object> completed, object state);
}
