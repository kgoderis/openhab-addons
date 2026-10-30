namespace AtsAdvancedGatewayNet8;

/// <summary>
/// Same idea as Mono <c>PanelService.logMessage</c>: push gateway log lines to openHAB
/// as panel SSE events named <c>msg.info</c> / <c>msg.warn</c> / <c>msg.error</c>
/// with a <c>result</c> property.
/// </summary>
public sealed class LogHub
{
    private readonly EventHub _events;

    public LogHub(EventHub events) => _events = events;

    public void Info(string message) => Publish("msg.info", message);

    public void Warn(string message) => Publish("msg.warn", message);

    public void Error(string message) => Publish("msg.error", message);

    public void Publish(string level, string category, string message)
    {
        // category unused on the wire — Mono only sent the text in "result"
        _ = category;
        var name = level switch
        {
            "Error" or "Critical" => "msg.error",
            "Warning" => "msg.warn",
            _ => "msg.info"
        };
        Publish(name, message);
    }

    private void Publish(string name, string message)
    {
        if (string.IsNullOrWhiteSpace(message))
        {
            return;
        }

        _events.Publish(new MessageResponse
        {
            Name = name,
            Properties =
            {
                new MessageProperty { Id = "result", Index = 0, Value = message.TrimEnd() }
            }
        });
    }
}
