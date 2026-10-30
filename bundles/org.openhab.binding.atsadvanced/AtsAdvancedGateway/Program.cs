using System.Text.Json;
using System.Text.Json.Serialization;
using AtsAdvancedGateway;

Kernel32Shim.Install();
NAceNetCoreCompat.Install();

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls(builder.Configuration["urls"] ?? "http://127.0.0.1:18137");

var eventHub = new EventHub();
var logHub = new LogHub(eventHub);
builder.Services.AddSingleton(eventHub);
builder.Services.AddSingleton(logHub);
GatewayLogBridge.Install(logHub);

builder.Services.ConfigureHttpJsonOptions(o =>
{
    o.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
    o.SerializerOptions.PropertyNameCaseInsensitive = true;
    o.SerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
});
builder.Services.AddSingleton<PanelHost>();

var app = builder.Build();
var jsonOptions = new JsonSerializerOptions
{
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    PropertyNameCaseInsensitive = true,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
};

app.MapGet("/health", (PanelHost host) => Results.Json(new
{
    status = "ok",
    configured = host.IsConfigured,
    runtime = System.Runtime.InteropServices.RuntimeInformation.FrameworkDescription
}));

app.MapPost("/configurepanel", (ConfigurePanelRequest request, PanelHost host) =>
{
    GatewayLog.Info($"configurepanel {request.Hostaddress}:{request.Port}");
    var result = host.Configure(request);
    return Results.Json(new ConfigurePanelResponse { Result = result }, jsonOptions);
});

app.MapPost("/message", (MessageRequest request, PanelHost host) =>
{
    GatewayLog.Info($"message {request.Name} props={request.Properties?.Count ?? 0}");
    var response = host.Send(request);
    return Results.Json(response, jsonOptions);
});

app.MapGet("/events", async (HttpContext http, EventHub hub, CancellationToken ct) =>
{
    http.Response.Headers.ContentType = "text/event-stream";
    http.Response.Headers.CacheControl = "no-cache";
    http.Response.Headers.Connection = "keep-alive";

    await http.Response.WriteAsync(": connected\n\n", ct);
    await http.Response.Body.FlushAsync(ct);

    var reader = hub.Subscribe(ct);
    await foreach (var evt in reader.ReadAllAsync(ct))
    {
        var payload = JsonSerializer.Serialize(evt, jsonOptions);
        await http.Response.WriteAsync($"event: panelevent\ndata: {payload}\n\n", ct);
        await http.Response.Body.FlushAsync(ct);
    }
});

GatewayLog.Info("ATS Advanced Gateway (.NET 8) listening; health at /health");
app.Run();
