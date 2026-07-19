import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const [webPath, wasmPath, hostPath] = process.argv.slice(2);
if (!webPath || !wasmPath || !hostPath) throw new Error("missing conformance paths");
const cases = [
  [[5n,3n,2n,4n,4n,2n,2n,3n,5n,5n,4n,1n,3n,3n,4n],0n],
  [[4n,3n,3n,4n,3n,3n,4n,3n,3n,4n,3n,3n,4n,3n,3n],1n],
  [[10n,0n,0n,10n,0n,0n,10n,0n,0n,10n,0n,0n,10n,0n,0n],2n],
  [[5n,3n,2n],3n],
  [[],4n],
];
const rejected = [[5n], [5n,3n], [-1n,5n,6n], [5n,3n,3n], Array(18).fill(0n)];

const web = await import(pathToFileURL(path.resolve(webPath)));
if (web.kotobaArtifact.requiredCapabilities.length !== 0)
  throw new Error("Family Web graph requested a capability");
if (web.instantiateKotoba().main() !== 42n) throw new Error("Family Web main mismatch");
for (const args of cases)
  if (web.instantiateKotoba()["summary-check"](...args) !== 42n)
    throw new Error("Family Web summary mismatch");
for (const values of rejected)
  if (web.instantiateKotoba()["reject-check"](values) !== 42n)
    throw new Error("Family Web accepted invalid allocation");

const host = await import(pathToFileURL(path.resolve(hostPath)));
const wasmBytes = fs.readFileSync(path.resolve(wasmPath));
for (const [values, caseId] of cases) {
  const wasm = await host.instantiateKotoba(wasmBytes);
  if (wasm.instance.exports["summary-check"](
      wasm.typedValues.vectorI64(values), caseId) !== 42n)
    throw new Error("Family Wasm summary mismatch");
}
for (const values of rejected) {
  const wasm = await host.instantiateKotoba(wasmBytes);
  if (wasm.instance.exports["reject-check"](
      wasm.typedValues.vectorI64(values)) !== 42n)
    throw new Error("Family Wasm accepted invalid allocation");
}
const wasmMain = await host.instantiateKotoba(wasmBytes);
if (wasmMain.instance.exports.main() !== 42n) throw new Error("Family Wasm main mismatch");
console.log("ghosthacker-family: bounded opening-week Web/Wasm conformance passed");
