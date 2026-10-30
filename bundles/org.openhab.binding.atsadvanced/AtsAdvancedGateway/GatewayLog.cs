// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

namespace AtsAdvancedGateway;

/// <summary>
/// Preferred console logging API for gateway call sites.
/// Writes <c>INFO:</c> / <c>WARNING:</c> / <c>ERROR:</c> prefixed lines and mirrors them
/// to openHAB through an attached <see cref="LogHub"/>.
/// </summary>
/// <remarks>
/// Prefer this over raw <see cref="Console"/> writes so SSE fans out consistently and
/// <see cref="GatewayLogBridge"/> can skip duplicate tee of already-prefixed lines.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public static class GatewayLog
{
    private static LogHub? _hub;

    /// <summary>Attaches the SSE log hub (called once from <see cref="GatewayLogBridge.Install"/>).</summary>
    public static void Attach(LogHub hub) => _hub = hub;

    /// <summary>Logs an informational message to stdout and SSE.</summary>
    public static void Info(string message)
    {
        Console.WriteLine("INFO: {0}", message);
        _hub?.Info(message);
    }

    /// <summary>Logs a warning to stdout and SSE.</summary>
    public static void Warn(string message)
    {
        Console.WriteLine("WARNING: {0}", message);
        _hub?.Warn(message);
    }

    /// <summary>Logs an error to stderr and SSE.</summary>
    public static void Error(string message)
    {
        Console.Error.WriteLine("ERROR: {0}", message);
        _hub?.Error(message);
    }
}
