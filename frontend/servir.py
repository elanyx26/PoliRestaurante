"""Servidor estático de desarrollo para el frontend.

Uso:  python servir.py [puerto]     (por defecto 5173, el origen que el backend permite en CORS)

A diferencia de `python -m http.server`, desactiva la caché del navegador para que
cada cambio en HTML, CSS o JS se vea al recargar.
"""
import sys
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class SinCache(SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()


if __name__ == "__main__":
    puerto = int(sys.argv[1]) if len(sys.argv) > 1 else 5173
    carpeta = str(Path(__file__).resolve().parent)
    servidor = ThreadingHTTPServer(("127.0.0.1", puerto), partial(SinCache, directory=carpeta))
    print(f"Frontend en http://localhost:{puerto}  (Ctrl+C para detener)")
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido")
