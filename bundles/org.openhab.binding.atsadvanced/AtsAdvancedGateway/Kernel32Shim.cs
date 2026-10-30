// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using System.Reflection;
using System.Runtime.InteropServices;

namespace AtsAdvancedGateway;

/// <summary>
/// Registers a native library resolver so NAce.Utilities P/Invokes to
/// <c>kernel32!OutputDebugString</c> resolve on macOS/Linux.
/// </summary>
/// <remarks>
/// On Windows the real kernel32 is used and this shim is a no-op. Elsewhere the gateway
/// ships a tiny <c>kernel32.dll</c> / <c>libkernel32.dll</c> stub next to the executable.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
internal static class Kernel32Shim
{
    /// <summary>
    /// Installs <see cref="NativeLibrary.SetDllImportResolver"/> for the NAce assemblies
    /// when not running on Windows.
    /// </summary>
    public static void Install()
    {
        if (OperatingSystem.IsWindows())
        {
            return;
        }

        var path = Path.Combine(AppContext.BaseDirectory, "kernel32.dll");
        if (!File.Exists(path))
        {
            Console.Error.WriteLine($"WARN: kernel32 shim missing at {path}");
            return;
        }

        IntPtr Resolver(string libraryName, Assembly assembly, DllImportSearchPath? searchPath)
        {
            if (!libraryName.Contains("kernel32", StringComparison.OrdinalIgnoreCase))
            {
                return IntPtr.Zero;
            }

            return NativeLibrary.TryLoad(path, out var handle) ? handle : IntPtr.Zero;
        }

        foreach (var name in new[] { "NAce.Utilities", "NAce.Ats", "NAce.Api", "NAce.Message", "NAce.Communication" })
        {
            NativeLibrary.SetDllImportResolver(Assembly.Load(name), Resolver);
        }

        Console.WriteLine($"kernel32 shim installed → {path}");
    }
}
