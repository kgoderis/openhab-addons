// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

namespace AtsAdvancedGateway;

/// <summary>
/// Publishes gateway log lines onto the SSE bus as synthetic panel events
/// <c>msg.info</c>, <c>msg.warn</c>, or <c>msg.error</c> with a single <c>result</c> property.
/// </summary>
/// <remarks>
/// The Java bridge maps those names onto panel status / log channels so operators see
/// gateway diagnostics next to panel traffic without a separate log socket.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public sealed class LogHub
{
    private readonly EventHub _events;

    /// <summary>Creates a hub that publishes through <paramref name="events"/>.</summary>
    public LogHub(EventHub events) => _events = events;

    /// <summary>Publishes an informational line as <c>msg.info</c>.</summary>
    public void Info(string message) => Publish("msg.info", message);

    /// <summary>Publishes a warning line as <c>msg.warn</c>.</summary>
    public void Warn(string message) => Publish("msg.warn", message);

    /// <summary>Publishes an error line as <c>msg.error</c>.</summary>
    public void Error(string message) => Publish("msg.error", message);

    /// <summary>
    /// Maps a structured level/category pair onto the wire event name and publishes
    /// <paramref name="message"/> in the <c>result</c> property.
    /// </summary>
    /// <param name="level">Log level string (e.g. Warning, Error, Critical).</param>
    /// <param name="category">Unused on the wire; retained for call-site convenience.</param>
    /// <param name="message">Human-readable text delivered to openHAB.</param>
    public void Publish(string level, string category, string message)
    {
        // category unused on the wire — only the text is sent in "result"
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
