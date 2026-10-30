// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using System;
using System.Reflection;
using System.Threading;

namespace Ace.Utilities;

/// <summary>
/// Drop-in replacement for NAce <c>EventDispatcher.AsyncFireEvent</c> on modern .NET.
/// </summary>
/// <remarks>
/// <para>
/// Framework NAce used <c>Delegate.BeginInvoke</c>, which throws
/// <see cref="PlatformNotSupportedException"/> on .NET Core / .NET 8+. This helper queues
/// each listener notification on the <see cref="ThreadPool"/> and invokes via
/// <see cref="Delegate.DynamicInvoke"/>.
/// </para>
/// <para>
/// Built as <c>NetCoreAsyncFireLib.dll</c> and either referenced at runtime or IL-merged into
/// <c>NAce.Utilities.dll</c> by <c>tools/PatchNAceBeginInvoke</c> so panel push events
/// (<c>msg.MONITOR</c>, <c>msgCOS.*</c>, …) reach the gateway panel host listener.
/// </para>
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public static class NetCoreAsyncFire
{
    /// <summary>
    /// Asynchronously notifies each entry in <paramref name="listeners"/> via <paramref name="notify"/>.
    /// Failures are forwarded to <paramref name="errorHandler"/> when present.
    /// </summary>
    /// <param name="notify">Delegate invoked with each listener as its sole argument.</param>
    /// <param name="listeners">Array of listener objects (may be null — then a no-op).</param>
    /// <param name="errorHandler">Optional handler receiving the unwrapped exception.</param>
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

    /// <summary>Thread-pool work item state for one listener notification.</summary>
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
