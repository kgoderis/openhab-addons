using System;
using System.Reflection;
using System.Threading;

namespace Ace.Utilities;

/// <summary>
/// Replacement for NAce EventDispatcher.AsyncFireEvent on .NET Core / .NET 8
/// (Delegate.BeginInvoke is unsupported). Compiled then merged into NAce.Utilities.dll.
/// </summary>
public static class NetCoreAsyncFire
{
    public static void Fire(Delegate notify, Array listeners, Delegate errorHandler)
    {
        if (listeners == null)
        {
            return;
        }

        for (var i = 0; i < listeners.Length; i++)
        {
            var listener = listeners.GetValue(i);
            var worker = new Worker
            {
                Notify = notify,
                Listener = listener,
                ErrorHandler = errorHandler
            };
            ThreadPool.QueueUserWorkItem(Worker.Run, worker);
        }
    }

    private sealed class Worker
    {
        public Delegate Notify;
        public object Listener;
        public Delegate ErrorHandler;

        public static void Run(object state)
        {
            var w = (Worker)state;
            try
            {
                w.Notify.DynamicInvoke(w.Listener);
            }
            catch (Exception ex)
            {
                if (w.ErrorHandler == null)
                {
                    return;
                }

                try
                {
                    var inner = ex is TargetInvocationException tie && tie.InnerException != null
                        ? tie.InnerException
                        : ex;
                    w.ErrorHandler.DynamicInvoke(inner);
                }
                catch
                {
                    // swallow — same as NAce error-handler path
                }
            }
        }
    }
}
