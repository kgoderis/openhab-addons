using System.Threading.Channels;

namespace AtsAdvancedGatewayNet8;

/// <summary>Fan-out of panel / gateway events to SSE subscribers.</summary>
public sealed class EventHub
{
    private readonly object _gate = new();
    private readonly List<Channel<MessageResponse>> _subscribers = new();

    public ChannelReader<MessageResponse> Subscribe(CancellationToken ct)
    {
        var channel = Channel.CreateUnbounded<MessageResponse>(new UnboundedChannelOptions
        {
            SingleReader = true,
            SingleWriter = false
        });

        lock (_gate)
        {
            _subscribers.Add(channel);
        }

        ct.Register(() =>
        {
            lock (_gate)
            {
                _subscribers.Remove(channel);
            }

            channel.Writer.TryComplete();
        });

        return channel.Reader;
    }

    public void Publish(MessageResponse message)
    {
        List<Channel<MessageResponse>> snapshot;
        lock (_gate)
        {
            snapshot = _subscribers.ToList();
        }

        foreach (var ch in snapshot)
        {
            ch.Writer.TryWrite(message);
        }
    }
}
