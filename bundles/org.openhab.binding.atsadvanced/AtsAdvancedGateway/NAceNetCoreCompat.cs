// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

namespace AtsAdvancedGateway;

/// <summary>
/// Runtime check that the NAce.Utilities BeginInvoke patch helper is loadable.
/// </summary>
/// <remarks>
/// <para>
/// NAce was built for .NET Framework. <c>EventDispatcher.AsyncFireEvent</c> used
/// <c>Delegate.BeginInvoke</c>, which throws <see cref="PlatformNotSupportedException"/> on modern .NET.
/// </para>
/// <para>
/// The gateway build patches <c>Library/NAce.Utilities.dll</c> from <c>.dll.orig</c> so
/// <c>AsyncFireEvent</c> calls <c>NetCoreAsyncFireLib</c> (ThreadPool + DynamicInvoke).
/// That restores panel listeners for push events (<c>msg.MONITOR</c>, <c>msgCOS.*</c>, …).
/// </para>
/// <para>
/// <see cref="Install"/> only verifies the helper assembly is present next to the gateway;
/// the actual IL rewrite happens at build time via <c>tools/PatchNAceBeginInvoke</c>.
/// </para>
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public static class NAceNetCoreCompat
{
    /// <summary>
    /// Loads <c>NetCoreAsyncFireLib</c> early and logs whether <c>NetCoreAsyncFire.Fire</c> is available.
    /// </summary>
    public static void Install()
    {
        // Ensure helper is loadable before NAce fires the first panel event
        try
        {
            var asm = System.Reflection.Assembly.Load("NetCoreAsyncFireLib");
            var fire = asm.GetType("Ace.Utilities.NetCoreAsyncFire")?.GetMethod("Fire");
            Console.WriteLine(fire != null
                ? "NAce compat: NetCoreAsyncFireLib loaded — panel event listeners OK"
                : "NAce compat: WARNING — NetCoreAsyncFire.Fire missing");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"NAce compat: WARNING — NetCoreAsyncFireLib not loadable: {ex.Message}");
        }
    }
}
