// Подключение телефонов к игре по QR-коду: QR с адресами страниц агента и капитана.
window.Join = (function () {
  "use strict";

  function svg(text) {
    const qr = qrcode(0, "M");
    qr.addData(text);
    qr.make();
    return qr.createSvgTag({ cellSize: 4, margin: 2, scalable: true });
  }

  // Публичный адрес сайта: его и кладём в QR (по умолчанию — адрес, с которого открыта страница).
  function lanBase() {
    if (location.protocol.indexOf("http") !== 0) return Promise.resolve(null);
    // Папка, где лежит сайт (работает и в корне домена, и в подпапке).
    const fallback = new URL(".", location.href).href;
    return fetch("api/info", { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (info) { return (info.urls && info.urls[0]) || fallback; })
      .catch(function () { return fallback; });
  }

  return { svg: svg, lanBase: lanBase };
})();
