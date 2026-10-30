// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using System.Text;

namespace AtsAdvancedGateway;

/// <summary>
/// Tees raw <see cref="Console"/> output into <see cref="LogHub"/> for SSE delivery to openHAB.
/// </summary>
/// <remarks>
/// Also attaches <see cref="GatewayLog"/> so structured call sites publish once.
/// Lines already prefixed with <c>INFO:</c> / <c>WARNING:</c> / <c>ERROR:</c> are skipped by the
/// tee to avoid duplicate SSE events when <see cref="GatewayLog"/> wrote them.
/// Prefer <see cref="GatewayLog"/> for new call sites.
/// Author: Karel Goderis - Initial contribution
/// </remarks>
public static class GatewayLogBridge
{
    /// <summary>
    /// Installs console tees and attaches <paramref name="hub"/> to <see cref="GatewayLog"/>.
    /// </summary>
    /// <param name="hub">Hub that publishes synthetic <c>msg.*</c> SSE events.</param>
    public static void Install(LogHub hub)
    {
        GatewayLog.Attach(hub);
        Console.SetOut(new TeeLineWriter(Console.Out, hub, stdout: true));
        Console.SetError(new TeeLineWriter(Console.Error, hub, stdout: false));
    }

    /// <summary>
    /// Line-buffered <see cref="TextWriter"/> that mirrors completed lines to <see cref="LogHub"/>.
    /// </summary>
    private sealed class TeeLineWriter : TextWriter
    {
        private readonly TextWriter _inner;
        private readonly LogHub _hub;
        private readonly bool _stdout;
        private readonly StringBuilder _buffer = new();
        private readonly object _gate = new();

        public TeeLineWriter(TextWriter inner, LogHub hub, bool stdout)
        {
            _inner = inner;
            _hub = hub;
            _stdout = stdout;
        }

        public override Encoding Encoding => _inner.Encoding;

        public override void Write(char value)
        {
            _inner.Write(value);
            lock (_gate)
            {
                if (value == '\n')
                {
                    FlushBuffer();
                }
                else if (value != '\r')
                {
                    _buffer.Append(value);
                }
            }
        }

        public override void Write(string? value)
        {
            if (value == null)
            {
                return;
            }

            _inner.Write(value);
            lock (_gate)
            {
                foreach (var ch in value)
                {
                    if (ch == '\n')
                    {
                        FlushBuffer();
                    }
                    else if (ch != '\r')
                    {
                        _buffer.Append(ch);
                    }
                }
            }
        }

        public override void WriteLine(string? value)
        {
            _inner.WriteLine(value);
            lock (_gate)
            {
                if (!string.IsNullOrEmpty(value))
                {
                    _buffer.Append(value);
                }

                FlushBuffer();
            }
        }

        public override void Flush()
        {
            _inner.Flush();
            lock (_gate)
            {
                if (_buffer.Length > 0)
                {
                    FlushBuffer();
                }
            }
        }

        private void FlushBuffer()
        {
            if (_buffer.Length == 0)
            {
                return;
            }

            var line = _buffer.ToString();
            _buffer.Clear();

            // GatewayLog.Info/Warn/Error already publish — skip duplicates from the tee.
            if (line.StartsWith("INFO: ", StringComparison.Ordinal)
                || line.StartsWith("WARNING: ", StringComparison.Ordinal)
                || line.StartsWith("ERROR: ", StringComparison.Ordinal))
            {
                return;
            }

            if (_stdout)
            {
                _hub.Info(line);
            }
            else
            {
                _hub.Error(line);
            }
        }
    }
}
