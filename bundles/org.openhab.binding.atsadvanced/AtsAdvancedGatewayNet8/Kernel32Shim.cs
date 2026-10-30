using System.Reflection;
using System.Runtime.InteropServices;

namespace AtsAdvancedGatewayNet8;

/// <summary>
/// NAce.Utilities P/Invokes kernel32!OutputDebugString. Mono shims that; .NET on macOS/Linux does not.
/// </summary>
internal static class Kernel32Shim
{
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
