using System.Net;
using System.Net.NetworkInformation;
using Ace;
using Ace.Ats;
using Ace.Communication;
using Ace.Messages;

namespace AtsAdvancedGateway;

/// <summary>Thin NAce host: connect, encrypt, send/receive, forward events. No ATS domain logic.</summary>
public sealed class PanelHost : IEventListener, IDisposable
{
    private readonly EventHub _events;
    private readonly object _gate = new();
    private IMessageDriver? _driver;
    private IMessageFactory? _factory;
    private AtsAdvancedPanel? _panel;
    private int _timeout = 5000;
    private int _retries = 2;
    private bool _disposed;

    public PanelHost(EventHub events) => _events = events;

    public bool IsConfigured
    {
        get
        {
            lock (_gate)
            {
                return _panel != null && _factory != null;
            }
        }
    }

    public string Configure(ConfigurePanelRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Hostaddress))
        {
            return "Missing Hostaddress";
        }

        if (request.Port <= 0)
        {
            return "Missing Port";
        }

        var password = string.IsNullOrEmpty(request.Password)
            ? new string('0', 52)
            : request.Password;

                var heartbeat = request.Hearbeat > 0 ? request.Hearbeat : 10000;
        var timeout = request.Timeout > 0 ? request.Timeout : 5000;
        var retries = request.Retries > 0 ? request.Retries : 2;

        try
        {
            lock (_gate)
            {
                DisposePanel_NoLock();
                _timeout = timeout;
                _retries = retries;

                _driver = LoadDriver();
                _factory = _driver.CreateFactory();
                _panel = new AtsAdvancedPanel
                {
                    EventMessageFactory = _factory,
                    PingSettings = new PingSettings(heartbeat, timeout, retries)
                };

                if (!IPAddress.TryParse(request.Hostaddress, out var ip))
                {
                    ip = Dns.GetHostAddresses(request.Hostaddress)
                        .First(a => a.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork);
                }

                GatewayLog.Info($"Opening panel TCP {ip}:{request.Port}");
                _panel.Open(new TcpCommunicationChannel(ip, request.Port), true);
                _panel.AddListener(this);
                GatewayLog.Info("Panel event listener registered (panelevents → openHAB)");

                var result = StartDiscover_NoLock(password, timeout, retries);
                return result;
            }
        }
        catch (AtsFaultException ex)
        {
            var msg = $"ATS Fault {ex.Code}, {ex.Message}";
            PublishError(msg);
            return msg;
        }
        catch (Exception ex)
        {
            PublishError(ex.Message);
            return ex.Message;
        }
    }

    public MessageResponse Send(MessageRequest request)
    {
        var response = new MessageResponse { Properties = new List<MessageProperty>() };

        try
        {
            // Serialize Begin/End/ToDto with event ToDto, but never hold the lock across WaitOne
            // (NAce may deliver Received on the completion path → deadlock).
            IMessageFactory factory;
            AtsAdvancedPanel panel;
            int timeout;
            int retries;
            IAsyncResult ar;

            lock (_gate)
            {
                if (_panel == null || _factory == null)
                {
                    response.Name = "return.error";
                    response.Properties.Add(new MessageProperty
                    {
                        Id = "result",
                        Index = 0,
                        Value = "Panel not configured. Call /configurepanel first."
                    });
                    return response;
                }

                factory = _factory;
                panel = _panel;
                timeout = _timeout;
                retries = _retries;

                var msg = factory.CreateMessage(request.Name ?? throw new ArgumentException("Missing name"));
                if (request.Properties != null)
                {
                    foreach (var p in request.Properties)
                    {
                        if (p.Id == null)
                        {
                            continue;
                        }

                        msg.SetProperty(p.Id, p.Index, Coerce(p.Value));
                    }
                }

                ar = panel.BeginSend(msg, timeout, retries, null, null);
            }

            if (!ar.AsyncWaitHandle.WaitOne(timeout * Math.Max(1, retries) + 10000))
            {
                throw new TimeoutException($"Timeout waiting for {request.Name}");
            }

            lock (_gate)
            {
                var finalResponse = panel.EndSend(ar);
                return ToDto(finalResponse);
            }
        }
        catch (AtsFaultException ex)
        {
            response.Name = "return.error";
            response.Properties.Add(new MessageProperty
            {
                Id = "result",
                Index = 0,
                Value = $"Fault response {ex.Code}, {ex.Message}"
            });
            return response;
        }
        catch (Exception ex)
        {
            GatewayLog.Error($"Send error [{request.Name}]: {ex}");
            response.Name = "return.error";
            response.Properties.Add(new MessageProperty
            {
                Id = "result",
                Index = 0,
                Value = $"{ex.GetType().Name}: {ex.Message}"
            });
            return response;
        }
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        lock (_gate)
        {
            DisposePanel_NoLock();
        }
    }

    /// <summary>NAce panel push → SSE panelevent.</summary>
    public bool Received(ISender device, IMessage message)
    {
        try
        {
            MessageResponse dto;
            lock (_gate)
            {
                dto = ToDto(message);
            }

            GatewayLog.Info($"Received event from Panel: {dto.Name}");
            _events.Publish(dto);
        }
        catch (Exception ex)
        {
            GatewayLog.Error($"Event fan-out error: {ex.Message}");
        }

        return true;
    }

    private void DisposePanel_NoLock()
    {
        try
        {
            _panel?.RemoveListener(this);
        }
        catch
        {
            // ignore
        }

        try
        {
            _panel?.Close();
        }
        catch (Exception ex)
        {
            GatewayLog.Warn($"Panel Close: {ex.GetType().Name}: {ex.Message}");
        }

        try
        {
            (_panel as IDisposable)?.Dispose();
        }
        catch
        {
            // ignore
        }

        _panel = null;
        _factory = null;
        _driver = null;
    }

    private string StartDiscover_NoLock(string password, int timeout, int retries)
    {
        var panel = _panel!;
        var factory = _factory!;

        var desc = SendRaw_NoLock("device.getDescription", timeout, retries);
        var info = new DeviceDiscovery(desc);
        GatewayLog.Info(
            $"DISCOVERY name={info.DeviceName} model={info.Model} ver={info.Version} enc={info.EncryptionMode} serial={info.SerialNumber}");

        // Prefer numeric model from FWID if present; fall back to product name string (legacy).
        try
        {
            if (desc.GetPropertyStatus("device.FWID_ProductName", 0) == PropertyStatus.Ok)
            {
                factory.SetProperty("model", 0, info.Model);
            }
        }
        catch (Exception ex)
        {
            GatewayLog.Warn($"model context warn: {ex.Message}");
        }

        if (!string.IsNullOrEmpty(info.Version) && info.Version.Contains('.'))
        {
            // MR_024.024.0128 → protocol 24, then walk down until factory accepts it
            var parts = info.Version.Split('.');
            var protocol = int.Parse(parts[1]);
            for (; protocol > 0; protocol--)
            {
                try
                {
                    factory.SetProperty("protocol", 0, protocol);
                    GatewayLog.Info($"factory protocol={protocol}");
                    break;
                }
                catch
                {
                    // try lower protocol
                }
            }
        }

        if (!string.IsNullOrEmpty(info.SerialNumber))
        {
            panel.SerialNumber = Convert.ToInt64(info.SerialNumber, 16);
        }

        var encMode = info.EncryptionMode;
        GatewayLog.Info($"encryption mode resolved={encMode}");
        if (encMode != 0)
        {
            panel.SetEncryptionKey(new MasterKeyProvider(password).GetKeyData(encMode));
            var sessionKey = new byte[16];
            AtsUtils.FillRandomBytes(sessionKey);

            var sessionResponse = SendRaw_NoLock("begin.changeSessionKey", timeout, retries,
                m => m.SetProperty("data", 0, sessionKey));
            var length = AtsUtils.GetEncryptionKeySize(encMode);
            var newKey = new byte[length];
            Buffer.BlockCopy(sessionKey, 0, newKey, 0, length / 2);
            sessionKey = (byte[])sessionResponse.GetProperty("data", 0, typeof(byte[]));
            Buffer.BlockCopy(sessionKey, 0, newKey, length / 2, length / 2);
            SendRaw_NoLock("end.changeSessionKey", timeout, retries);
            panel.SetEncryptionKey(newKey);
            GatewayLog.Info("Session key negotiated");
        }

        return "1";
    }

    private IMessage SendRaw_NoLock(string name, int timeout, int retries, Action<IMessage>? setup = null)
    {
        var msg = _factory!.CreateMessage(name);
        setup?.Invoke(msg);
        var ar = _panel!.BeginSend(msg, timeout, retries, null, null);
        if (!ar.AsyncWaitHandle.WaitOne(timeout * Math.Max(1, retries) + 10000))
        {
            throw new TimeoutException($"Timeout waiting for {name}");
        }

        return _panel.EndSend(ar);
    }

    private static IMessageDriver LoadDriver()
    {
        var path = Path.Combine(AppContext.BaseDirectory, "ats.advanced.drv");
        if (!File.Exists(path))
        {
            throw new FileNotFoundException("ats.advanced.drv not found", path);
        }

        using var input = File.OpenRead(path);
        var driver = MessageDriverLoader.LoadDriver(input);
        if (driver == null)
        {
            throw new InvalidOperationException("Failed to load message driver");
        }

        GatewayLog.Info($"Loaded driver from {path}");
        return driver;
    }

    private static object? Coerce(object? value)
    {
        if (value is null)
        {
            return null;
        }

        if (value is System.Text.Json.JsonElement je)
        {
            return je.ValueKind switch
            {
                System.Text.Json.JsonValueKind.True => true,
                System.Text.Json.JsonValueKind.False => false,
                System.Text.Json.JsonValueKind.Number => je.TryGetInt64(out var l) ? l : je.GetDouble(),
                System.Text.Json.JsonValueKind.String => je.GetString(),
                System.Text.Json.JsonValueKind.Null => null,
                _ => je.ToString()
            };
        }

        if (value is string s)
        {
            if (s.Equals("true", StringComparison.OrdinalIgnoreCase))
            {
                return true;
            }

            if (s.Equals("false", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }
        }

        return value;
    }

    private static MessageResponse ToDto(IMessage message)
    {
        var dto = new MessageResponse
        {
            Name = message.Info.Id,
            Properties = new List<MessageProperty>()
        };

        foreach (IPropertyInfo someInfo in message.Info.Properties)
        {
            try
            {
                var multiplicity = someInfo.Attributes["multiplicity"];
                if (multiplicity == "dynamic")
                {
                    for (var i = 0; i < message.Count; i++)
                    {
                        object? dynVal;
                        try
                        {
                            dynVal = JsonSafe(
                                NormalizeValue(someInfo.Id, message.GetProperty(someInfo.Id, i + 1, null)));
                        }
                        catch (Exception ex)
                        {
                            dynVal = $"<{ex.GetType().Name}: {ex.Message}>";
                        }

                        dto.Properties.Add(new MessageProperty
                        {
                            Id = someInfo.Id,
                            Index = i,
                            Value = dynVal
                        });
                    }
                }
                else if (message.GetPropertyStatus(someInfo.Id, 0) == PropertyStatus.Ok)
                {
                    object? value;
                    if (someInfo.Id == "device.FWID_MAC")
                    {
                        var address = (byte[])message.GetProperty("device.FWID_MAC", 0, typeof(byte[]));
                        value = Array.TrueForAll(address, b => b == 0) ? null : new PhysicalAddress(address).ToString();
                    }
                    else if (someInfo.Id == "bitSet")
                    {
                        var bitSet = (byte[])message.GetProperty("bitSet", 0, typeof(byte[]));
                        value = Convert.ToBase64String(bitSet);
                    }
                    else
                    {
                        try
                        {
                            value = NormalizeValue(someInfo.Id, message.GetProperty(someInfo.Id, 0, null));
                        }
                        catch (Exception ex)
                        {
                            value = $"<{ex.GetType().Name}: {ex.Message}>";
                        }
                    }

                    // System.Text.Json cannot serialize arbitrary NAce types (enums, IPAddress, …)
                    value = JsonSafe(value);
                    dto.Properties.Add(new MessageProperty { Id = someInfo.Id, Index = 0, Value = value });
                }
            }
            catch (Exception ex)
            {
                dto.Properties.Add(new MessageProperty
                {
                    Id = someInfo.Id,
                    Index = 0,
                    Value = $"<{ex.GetType().Name}: {ex.Message}>"
                });
            }
        }

        return dto;
    }

    private static object? NormalizeValue(string id, object? value)
    {
        if (value is string s && string.IsNullOrEmpty(s) && id == "name")
        {
            return "[Empty]";
        }

        // msg.MONITOR timeStamp must be XML/ISO for Java DatatypeConverter.parseDateTime
        if (id == "timeStamp")
        {
            return FormatTimeStamp(value);
        }

        return value;
    }

    private static object? FormatTimeStamp(object? value) => value switch
    {
        DateTime dt => System.Xml.XmlConvert.ToString(dt, System.Xml.XmlDateTimeSerializationMode.RoundtripKind),
        DateTimeOffset dto => System.Xml.XmlConvert.ToString(dto),
        _ => value
    };

    private static object? JsonSafe(object? value)
    {
        if (value is null || value is string or bool or byte or sbyte or short or ushort or int or uint or long
            or ulong or float or double or decimal)
        {
            return value;
        }

        // Catch DateTime on any property (incl. if NormalizeValue missed the id)
        if (value is DateTime or DateTimeOffset)
        {
            return FormatTimeStamp(value);
        }

        if (value is Enum)
        {
            return Convert.ToInt32(value);
        }

        if (value is byte[] bytes)
        {
            return Convert.ToBase64String(bytes);
        }

        return Convert.ToString(value, System.Globalization.CultureInfo.InvariantCulture);
    }

    private void PublishError(string message) => GatewayLog.Error(message);
}
