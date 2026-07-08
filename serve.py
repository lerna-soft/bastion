#!/usr/bin/env python3
"""Server that serves APK and receives logs from Bastion app."""

import json
import os
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime

APK_DIR = "/home/lerna/apk-share"
LOG_FILE = "/home/lerna/apk-share/bastion-logs.ndjson"
CHUNK = 64 * 1024


class Handler(BaseHTTPRequestHandler):

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (datetime.now().strftime("%H:%M:%S"), fmt % args))
        sys.stderr.flush()

    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_GET(self):
        # Find newest versioned APK
        latest_apk = None
        if os.path.exists(APK_DIR):
            versioned = [f for f in os.listdir(APK_DIR) if f.startswith("bastion-v") and f.endswith(".apk")]
            if versioned:
                latest_apk = sorted(versioned, reverse=True)[0]

        if self.path == "/apk-share/bastion-debug.apk" or self.path == "/latest":
            # Serve the newest versioned APK, fallback to bastion-debug.apk
            target_name = latest_apk or "bastion-debug.apk"
            apk_path = os.path.join(APK_DIR, target_name)
            if not os.path.exists(apk_path):
                self.send_response(404)
                self._cors()
                self.end_headers()
                self.wfile.write(b"APK not found")
                return
            size = os.path.getsize(apk_path)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", f"attachment; filename={target_name}")
            self.send_header("Content-Length", str(size))
            self._cors()
            self.end_headers()
            with open(apk_path, "rb") as f:
                while True:
                    buf = f.read(CHUNK)
                    if not buf:
                        break
                    self.wfile.write(buf)
            return
        # Direct file access for specific APK
        if self.path.startswith("/apk-share/bastion-v") and self.path.endswith(".apk"):
            target_name = os.path.basename(self.path)
            apk_path = os.path.join(APK_DIR, target_name)
            if not os.path.exists(apk_path):
                self.send_response(404)
                self._cors()
                self.end_headers()
                self.wfile.write(b"APK not found")
                return
            size = os.path.getsize(apk_path)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", f"attachment; filename={target_name}")
            self.send_header("Content-Length", str(size))
            self._cors()
            self.end_headers()
            with open(apk_path, "rb") as f:
                while True:
                    buf = f.read(CHUNK)
                    if not buf:
                        break
                    self.wfile.write(buf)
            return
        if self.path == "/update":
            update_file = os.path.join(APK_DIR, "latest.json")
            if os.path.exists(update_file):
                with open(update_file) as f:
                    data = f.read()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Cache-Control", "no-cache")
                self._cors()
                self.end_headers()
                self.wfile.write(data.encode())
            else:
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self._cors()
                self.end_headers()
                self.wfile.write(b'{"update":false}')
            return
        if self.path == "/logs":
            logs = []
            log_path = LOG_FILE
            if os.path.exists(log_path):
                with open(log_path) as f:
                    logs = [json.loads(line) for line in f if line.strip()]
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self._cors()
            self.end_headers()
            self.wfile.write(json.dumps(logs[-500:]).encode())
            return
        # index / help
        apk_files = []
        if os.path.exists(APK_DIR):
            for f in sorted(os.listdir(APK_DIR), reverse=True):
                if f.endswith(".apk") and not f.startswith("budget"):
                    apk_files.append(f)
        apk_links = "".join(
            '<li><a href="/apk-share/%s" style="color:#4FC3F7">%s</a> (%.1f MB)</li>' % (
                f, f, os.path.getsize(os.path.join(APK_DIR, f)) / 1024 / 1024
            )
            for f in apk_files if os.path.exists(os.path.join(APK_DIR, f))
        )
        latest_link = "/apk-share/" + latest_apk if latest_apk else "/apk-share/bastion-debug.apk"
        html = """<html><body style="background:#1e1e1e;color:#ccc;font-family:sans-serif;padding:2em">
<h1>Bastion Server</h1>
<h2>APKs</h2>
<ul>
<li><a href="%s" style="color:#4FC3F7;font-weight:bold;font-size:1.2em">⬇ Download latest APK</a> (%s)</li>
%s
</ul>
<h2>Logs</h2>
<ul>
<li><a href="/logs" style="color:#4FC3F7">View logs</a></li>
</ul>
<p>POST /logs to send log entries</p>
</body></html>""" % (latest_link, latest_apk or "none", apk_links)
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self._cors()
        self.end_headers()
        self.wfile.write(html.encode())

    def do_POST(self):
        if self.path == "/logs":
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len) if content_len else b"[]"
            try:
                entries = json.loads(body)
                if not isinstance(entries, list):
                    entries = [entries]
            except json.JSONDecodeError:
                entries = [{"raw": body.decode("utf-8", "replace")}]

            ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            with open(LOG_FILE, "a") as f:
                for entry in entries:
                    entry["_received"] = ts
                    f.write(json.dumps(entry, ensure_ascii=False) + "\n")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self._cors()
            self.end_headers()
            self.wfile.write(b'{"ok":true,"count":' + str(len(entries)).encode() + b'}')
        else:
            self.send_response(404)
            self._cors()
            self.end_headers()
            self.wfile.write(b"Not found")


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8765))
    server = HTTPServer(("0.0.0.0", port), Handler)
    print(f"Bastion server on http://0.0.0.0:{port}")
    print(f"  APK:   http://<host>:{port}/apk-share/bastion-debug.apk")
    print(f"  Logs:  POST http://<host>:{port}/logs  (app sends here)")
    print(f"  View:  GET  http://<host>:{port}/logs")
    print(f"  Index: http://<host>:{port}/")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("shutting down")
        server.server_close()
