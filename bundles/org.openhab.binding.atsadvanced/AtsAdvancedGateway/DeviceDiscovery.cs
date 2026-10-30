// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

using Ace;

namespace AtsAdvancedGateway;

/// <summary>
/// Read-only view of a <c>device.Description</c> NAce response used during
/// <see cref="PanelHost"/> configure / discovery.
/// </summary>
/// <remarks>Author: Karel Goderis - Initial contribution</remarks>
internal sealed class DeviceDiscovery
{
    private readonly IMessage _response;

    /// <summary>
    /// Wraps a discovery response; throws if the message id is not <c>device.Description</c>.
    /// </summary>
    internal DeviceDiscovery(IMessage discoveryResponse)
    {
        _response = discoveryResponse ?? throw new ArgumentNullException(nameof(discoveryResponse));
        if (_response.Info?.Id != "device.Description")
        {
            throw new InvalidOperationException($"Expected device.Description, got {_response.Info?.Id}");
        }
    }

    /// <summary>Panel display name (<c>device.name</c>).</summary>
    internal string? DeviceName => GetString("device.name");

    /// <summary>Product / model string (<c>device.FWID_ProductName</c>).</summary>
    internal string? Model => GetString("device.FWID_ProductName");

    /// <summary>
    /// Firmware version string (e.g. <c>MR_024.024.0128</c>); the middle segment drives
    /// the NAce factory <c>protocol</c> property.
    /// </summary>
    internal string? Version => GetString("device.FWID_FirmwareVersion");

    /// <summary>Hex serial number string (<c>device.FWID_SerialNumber</c>).</summary>
    internal string? SerialNumber => GetString("device.FWID_SerialNumber");

    /// <summary>
    /// Encryption mode from <c>device.FWID_EncMode</c>, or <c>0</c> when absent / unencrypted.
    /// </summary>
    internal int EncryptionMode
    {
        get
        {
            if (_response.GetPropertyStatus("device.FWID_EncMode", 0) != PropertyStatus.Ok)
            {
                return 0;
            }

            var raw = _response.GetProperty("device.FWID_EncMode", 0, null);
            return Convert.ToInt32(raw);
        }
    }

    private string? GetString(string name)
    {
        if (_response.GetPropertyStatus(name, 0) != PropertyStatus.Ok)
        {
            return null;
        }

        return ((string)_response.GetProperty(name, 0, typeof(string))).TrimEnd('\0');
    }
}
