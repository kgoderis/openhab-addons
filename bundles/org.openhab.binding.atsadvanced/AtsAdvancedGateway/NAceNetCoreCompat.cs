namespace AtsAdvancedGateway;

/// <summary>
/// NAce was built for .NET Framework. <c>EventDispatcher.AsyncFireEvent</c> used
/// <c>Delegate.BeginInvoke</c>, which throws <see cref="PlatformNotSupportedException"/> on .NET 8.
/// <para>
/// Build patches <c>Library/NAce.Utilities.dll</c> from <c>.dll.orig</c> so
/// <c>AsyncFireEvent</c> calls <c>NetCoreAsyncFireLib</c> (ThreadPool + DynamicInvoke).
/// That restores panel listeners for push events (<c>msg.MONITOR</c>, <c>msgCOS.*</c>, …).
/// </para>
/// </summary>
public static class NAceNetCoreCompat
{
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
