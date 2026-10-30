// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using System.Threading.Channels;

namespace AtsAdvancedGateway;

/// <summary>
/// In-process fan-out of panel and gateway <see cref="MessageResponse"/> events to SSE subscribers.
/// </summary>
/// <remarks>
/// Each <c>GET /events</c> client receives an unbounded channel. Panel pushes arrive from
/// <see cref="PanelHost.Received"/>; gateway log lines arrive via <see cref="LogHub"/>.
/// Writers never block on slow readers (<see cref="ChannelWriter{T}.TryWrite"/>).
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public sealed class EventHub
{
    private readonly object _gate = new();
    private readonly List<Channel<MessageResponse>> _subscribers = new();

    /// <summary>
    /// Registers a new subscriber channel cancelled when <paramref name="ct"/> fires
    /// (typically the HTTP request abort token).
    /// </summary>
    /// <param name="ct">Token that completes and removes the subscriber channel.</param>
    /// <returns>Reader consumed by the SSE endpoint.</returns>
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

    /// <summary>
    /// Broadcasts <paramref name="message"/> to every current subscriber (best-effort).
    /// </summary>
    /// <param name="message">Panel event or synthetic gateway log event.</param>
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
