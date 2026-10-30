namespace AtsAdvancedGatewayNet8;

public sealed class ConfigurePanelRequest
{
    public string? Hostaddress { get; set; }
    public int Port { get; set; }
    public string? Password { get; set; }
    public int Hearbeat { get; set; } = 10000;
    public int Timeout { get; set; } = 5000;
    public int Retries { get; set; } = 2;
}

public sealed class ConfigurePanelResponse
{
    public string? Result { get; set; }
}

public sealed class MessageProperty
{
    public string? Id { get; set; }
    public int Index { get; set; }
    public object? Value { get; set; }
}

public sealed class MessageRequest
{
    public string? Name { get; set; }
    public List<MessageProperty>? Properties { get; set; }
}

public sealed class MessageResponse
{
    public string? Name { get; set; }
    public List<MessageProperty> Properties { get; set; } = new();
}
