// Dirección del backend.
//  - En tu computador (localhost) usa el backend local.
//  - Publicado en internet usa la dirección de abajo: cámbiala por la URL de tu servicio en Render
//    (la ves en su panel, algo como https://poli-restaurante-api.onrender.com), sin barra al final.
const BACKEND_PUBLICO = "https://poli-restaurante-api.onrender.com";

const local = ["localhost", "127.0.0.1"].includes(location.hostname);

window.APP_CONFIG = {
  API_URL: local ? "http://localhost:8080" : BACKEND_PUBLICO,
};
