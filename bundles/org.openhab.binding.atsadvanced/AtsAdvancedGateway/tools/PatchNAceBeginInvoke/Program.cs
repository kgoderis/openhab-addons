// Copyright (c) 2010-2026 Contributors to the openHAB project
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License 2.0 which is available at
// http://www.eclipse.org/legal/epl-2.0
//
// SPDX-License-Identifier: EPL-2.0

/*
 * Build-time IL patcher for NAce.Utilities.
 *
 * Rewrites EventDispatcher.AsyncFireEvent so it calls Ace.Utilities.NetCoreAsyncFire.Fire
 * from NetCoreAsyncFireLib.dll (ThreadPool + DynamicInvoke). Framework NAce used
 * Delegate.BeginInvoke / EndInvoke, which are unsupported on modern .NET and break panel
 * push listeners (msg.MONITOR, msgCOS.*, …).
 *
 * Invoked automatically by AtsAdvancedGateway.csproj when Library/NAce.Utilities.dll.orig
 * is present.
 *
 * Usage: PatchNAceBeginInvoke <input-NAce.Utilities.dll> <output.dll> [NetCoreAsyncFireLib.dll]
 *
 * Author: Karel Goderis - Initial contribution
 */

using Mono.Cecil;
using Mono.Cecil.Cil;

if (args.Length < 2)
{
    Console.Error.WriteLine(
        "usage: PatchNAceBeginInvoke <input-NAce.Utilities.dll> <output.dll> [NetCoreAsyncFireLib.dll]");
    return 1;
}

var inputPath = Path.GetFullPath(args[0]);
var outputPath = Path.GetFullPath(args[1]);
var helperPath = ResolveHelper(args.Length >= 3 ? args[2] : null);
if (helperPath == null)
{
    Console.Error.WriteLine("NetCoreAsyncFireLib.dll not found — build tools/NetCoreAsyncFireLib (Release).");
    return 2;
}

using var nace = AssemblyDefinition.ReadAssembly(inputPath, new ReaderParameters { InMemory = true });
using var helperAsm = AssemblyDefinition.ReadAssembly(helperPath, new ReaderParameters { ReadWrite = false });
var naceMod = nace.MainModule;
var helperMod = helperAsm.MainModule;

// Drop any previous in-DLL helper left by older patchers
foreach (var t in naceMod.Types.Where(t => t.Namespace == "Ace.Utilities" && t.Name == "NetCoreAsyncFire").ToList())
{
    naceMod.Types.Remove(t);
}

var fireSrc = helperMod.Types
    .Single(t => t.Namespace == "Ace.Utilities" && t.Name == "NetCoreAsyncFire")
    .Methods.Single(m => m.Name == "Fire" && m.IsStatic && m.Parameters.Count == 3);

var fireRef = naceMod.ImportReference(fireSrc);

var dispatcher = naceMod.Types.Single(t => t.Namespace == "Ace.Utilities" && t.Name == "EventDispatcher");
var asyncFire = dispatcher.Methods.Single(m => m.Name == "AsyncFireEvent" && m.HasGenericParameters);

asyncFire.Body.Instructions.Clear();
asyncFire.Body.ExceptionHandlers.Clear();
asyncFire.Body.Variables.Clear();
asyncFire.Body.InitLocals = false;
{
    var il = asyncFire.Body.GetILProcessor();
    il.Emit(OpCodes.Ldarg_0);
    il.Emit(OpCodes.Ldarg_1);
    il.Emit(OpCodes.Ldarg_2);
    il.Emit(OpCodes.Call, fireRef);
    il.Emit(OpCodes.Ret);
}

Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);
nace.Write(outputPath);

var stillBegin = asyncFire.Body.Instructions.Any(i =>
    i.Operand is MethodReference mr && mr.Name is "BeginInvoke" or "EndInvoke");
Console.WriteLine($"OK: AsyncFireEvent → NetCoreAsyncFire.Fire ({helperPath})");
Console.WriteLine($"Wrote {outputPath}");
Console.WriteLine($"BeginInvoke left? {stillBegin}");
return stillBegin ? 3 : 0;

static string? ResolveHelper(string? explicitPath)
{
    if (!string.IsNullOrEmpty(explicitPath))
    {
        var p = Path.GetFullPath(explicitPath);
        return File.Exists(p) ? p : null;
    }

    var toolsDir = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", ".."));
    foreach (var cfg in new[] { "Release", "Debug" })
    {
        var candidate = Path.Combine(toolsDir, "NetCoreAsyncFireLib", "bin", cfg, "netstandard2.0",
            "NetCoreAsyncFireLib.dll");
        if (File.Exists(candidate))
        {
            return candidate;
        }
    }

    return null;
}
