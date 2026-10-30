using Ace;

namespace AtsAdvancedGatewayNet8;

internal sealed class DeviceDiscovery
{
    private readonly IMessage _response;

    internal DeviceDiscovery(IMessage discoveryResponse)
    {
        _response = discoveryResponse ?? throw new ArgumentNullException(nameof(discoveryResponse));
        if (_response.Info?.Id != "device.Description")
        {
            throw new InvalidOperationException($"Expected device.Description, got {_response.Info?.Id}");
        }
    }

    internal string? DeviceName => GetString("device.name");
    internal string? Model => GetString("device.FWID_ProductName");
    internal string? Version => GetString("device.FWID_FirmwareVersion");
    internal string? SerialNumber => GetString("device.FWID_SerialNumber");

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
