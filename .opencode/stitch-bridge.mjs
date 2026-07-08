// Nunca hardcodear la key aquí (el repo es público desde HIM-018). Se toma del
// entorno — exportala en tu shell o `source ~/.bastion-secrets.env` antes de correr
// opencode. Ver AGENTS.md sección "Secretos" para dónde vive.
const API_KEY = process.env.STITCH_API_KEY;
if (!API_KEY) {
  process.stderr.write("E:STITCH_API_KEY no está definida en el entorno\n");
}
const BASE_URL = "https://stitch.googleapis.com/mcp";

let buf = "";
process.stdin.on("data", (chunk) => {
  buf += chunk.toString();
  const lines = buf.split("\n").filter(Boolean);
  buf = "";
  for (const line of lines) {
    try {
      const req = JSON.parse(line);
      if (req.method === "tools/list") {
        fetch(BASE_URL, {
          method: "POST",
          headers: { "Content-Type": "application/json", "X-Goog-Api-Key": API_KEY },
          body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }),
        })
          .then((r) => r.json())
          .then((d) =>
            process.stdout.write(
              JSON.stringify({ jsonrpc: "2.0", id: req.id, result: d.result }) + "\n"
            )
          );
      } else if (req.method === "tools/call") {
        fetch(BASE_URL, {
          method: "POST",
          headers: { "Content-Type": "application/json", "X-Goog-Api-Key": API_KEY },
          body: JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "tools/call",
            params: { name: req.params.name, arguments: req.params.arguments },
          }),
        })
          .then((r) => r.json())
          .then((d) => {
            const t = d.result?.content?.[0]?.text;
            const p = t ? JSON.parse(t) : d.result;
            process.stdout.write(
              JSON.stringify({
                jsonrpc: "2.0",
                id: req.id,
                result: {
                  content: [
                    {
                      type: "text",
                      text: typeof p === "string" ? p : JSON.stringify(p, null, 2),
                    },
                  ],
                },
              }) + "\n"
            );
          });
      }
    } catch (e) {
      process.stderr.write("E:" + e.message + "\n");
    }
  }
});
setTimeout(() => {}, 60000);
