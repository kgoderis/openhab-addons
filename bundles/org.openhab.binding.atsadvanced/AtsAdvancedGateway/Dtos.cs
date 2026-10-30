// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

namespace AtsAdvancedGateway;

/// <summary>
/// JSON body for <c>POST /configurepanel</c>. Mirrors the Java binding
/// <c>PanelClient.ConfigurePanel</c> DTO (camelCase on the wire).
/// </summary>
/// <remarks>
/// Opens the panel TCP channel, loads <c>ats.advanced.drv</c>, discovers the device,
/// and negotiates the encryption session key when required. No ATS domain interpretation.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public sealed class ConfigurePanelRequest
{
    /// <summary>Panel host name or IPv4 address.</summary>
    public string? Hostaddress { get; set; }

    /// <summary>Panel TCP port (typically the ATS Advanced IP port).</summary>
    public int Port { get; set; }

    /// <summary>
    /// Encryption password used to derive the master key. When null or empty the gateway
    /// substitutes fifty-two ASCII zeros (legacy default accepted by many panels).
    /// </summary>
    public string? Password { get; set; }

    /// <summary>
    /// Panel ping / heartbeat interval in milliseconds (default 10000).
    /// Spelling <c>Hearbeat</c> is intentional wire-compat with the Java DTO.
    /// </summary>
    public int Hearbeat { get; set; } = 10000;

    /// <summary>Per-attempt send timeout in milliseconds (default 5000).</summary>
    public int Timeout { get; set; } = 5000;

    /// <summary>NAce send retry count (default 2).</summary>
    public int Retries { get; set; } = 2;
}

/// <summary>
/// JSON body returned by <c>POST /configurepanel</c>.
/// </summary>
/// <remarks>Author: Karel Goderis - Initial contribution</remarks>
public sealed class ConfigurePanelResponse
{
    /// <summary>
    /// Configuration outcome. Success is the string <c>"1"</c>; otherwise a human-readable
    /// fault or exception message suitable for the bridge status detail.
    /// </summary>
    public string? Result { get; set; }
}

/// <summary>
/// Single NAce message property as exposed on the JSON wire.
/// </summary>
/// <remarks>Author: Karel Goderis - Initial contribution</remarks>
public sealed class MessageProperty
{
    /// <summary>NAce property id (e.g. <c>bitSet</c>, <c>result</c>, <c>name</c>).</summary>
    public string? Id { get; set; }

    /// <summary>Zero-based index for multi-value / dynamic properties.</summary>
    public int Index { get; set; }

    /// <summary>
    /// JSON-safe value. Binary payloads (e.g. COS bit sets) are Base64 strings;
    /// timestamps are XML/ISO round-trip strings for Java parsers.
    /// </summary>
    public object? Value { get; set; }
}

/// <summary>
/// JSON body for <c>POST /message</c>: opaque NAce request identified by message name.
/// </summary>
/// <remarks>Author: Karel Goderis - Initial contribution</remarks>
public sealed class MessageRequest
{
    /// <summary>NAce message id (e.g. <c>getSTAT.ZONE</c>, <c>createCC.AREA</c>).</summary>
    public string? Name { get; set; }

    /// <summary>Optional property bag applied via <c>IMessage.SetProperty</c> before send.</summary>
    public List<MessageProperty>? Properties { get; set; }
}

/// <summary>
/// JSON body for message replies and SSE <c>panelevent</c> payloads.
/// </summary>
/// <remarks>
/// Produced by mapping an <c>Ace.Messages.IMessage</c> to JSON-safe property values.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public sealed class MessageResponse
{
    /// <summary>NAce response / event message id (e.g. <c>returnSTAT.ZONE</c>, <c>msg.MONITOR</c>).</summary>
    public string? Name { get; set; }

    /// <summary>Flattened property list (dynamic multiplicity expanded to multiple entries).</summary>
    public List<MessageProperty> Properties { get; set; } = new();
}
