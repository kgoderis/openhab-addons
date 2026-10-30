namespace AtsAdvancedGatewayNet8;

/// <summary>
/// Mono-compatible console helpers (<c>INFO:</c> / <c>WARNING:</c> / <c>ERROR:</c>)
/// that also fan out to openHAB via <see cref="LogHub"/>.
/// </summary>
public static class GatewayLog
{
    private static LogHub? _hub;

    public static void Attach(LogHub hub) => _hub = hub;

    public static void Info(string message)
    {
        Console.WriteLine("INFO: {0}", message);
        _hub?.Info(message);
    }

    public static void Warn(string message)
    {
        Console.WriteLine("WARNING: {0}", message);
        _hub?.Warn(message);
    }

    public static void Error(string message)
    {
        Console.Error.WriteLine("ERROR: {0}", message);
        _hub?.Error(message);
    }
}
