// Lógica pura (sin DOM ni xterm) de la selección de terminal — HIM-020.
// Se aísla aquí para poder testearla con Node (ver selection-logic.test.js). index.html la carga
// como <script src> y delega en SelLogic.* para que el comportamiento testeado sea el mismo que
// corre en el WebView.
(function (root) {
    var SelLogic = {};

    // Orden total de celdas {col, row}: negativo si a<b, 0 si igual, positivo si a>b.
    SelLogic.cmpCell = function (a, b) { return (a.row - b.row) || (a.col - b.col); };

    // Normaliza un par de celdas a {start, end} con start <= end.
    SelLogic.normalize = function (a, b) {
        return SelLogic.cmpCell(b, a) < 0 ? { start: b, end: a } : { start: a, end: b };
    };

    // Número de celdas (lineal, con wrap por filas) entre start y end INCLUSIVE. Mínimo 1.
    SelLogic.selLength = function (start, end, cols) {
        var len = (end.row - start.row) * cols + (end.col - start.col) + 1;
        return len < 1 ? 1 : len;
    };

    // Límites de la palabra (secuencia de no-espacios) alrededor de col en la cadena s.
    // Devuelve {start, end} (columnas inclusive) o null si col cae en espacio / fuera de rango.
    SelLogic.wordBounds = function (s, col) {
        if (typeof s !== 'string' || col < 0 || col >= s.length || !/\S/.test(s[col])) return null;
        var a = col, b = col;
        while (a > 0 && /\S/.test(s[a - 1])) a--;
        while (b < s.length - 1 && /\S/.test(s[b + 1])) b++;
        return { start: a, end: b };
    };

    // Mueve el foco de la selección por teclado (Shift+flechas/Home/End). Clampa dentro de
    // [0, cols-1] x [0, bufLen-1]; en los extremos de línea envuelve a la fila vecina (F3: sin
    // pasarse del buffer).
    SelLogic.moveFocus = function (key, focus, cols, bufLen) {
        var f = { col: focus.col, row: focus.row };
        switch (key) {
            case 'ArrowLeft':
                if (f.col > 0) f.col--;
                else if (f.row > 0) { f.row--; f.col = cols - 1; }
                break;
            case 'ArrowRight':
                if (f.col < cols - 1) f.col++;
                else if (f.row < bufLen - 1) { f.row++; f.col = 0; }
                break;
            case 'ArrowUp': f.row = Math.max(0, f.row - 1); break;
            case 'ArrowDown': f.row = Math.min(bufLen - 1, f.row + 1); break;
            case 'Home': f.col = 0; break;
            case 'End': f.col = cols - 1; break;
        }
        return f;
    };

    // Paso de auto-scroll (líneas con signo) según qué tan dentro de la zona de borde está el
    // dedo. 0 fuera de los bordes; acelera de 1 a maxStep al acercarse al borde (F2).
    SelLogic.autoScrollStep = function (y, top, bottom, edge, maxStep) {
        maxStep = maxStep || 3;
        if (!(edge > 0)) return 0;
        if (y < top + edge) {
            var dTop = Math.min(1, (top + edge - y) / edge);
            return -Math.min(maxStep, 1 + Math.floor(dTop * (maxStep - 1) + 1e-9));
        }
        if (y > bottom - edge) {
            var dBot = Math.min(1, (y - (bottom - edge)) / edge);
            return Math.min(maxStep, 1 + Math.floor(dBot * (maxStep - 1) + 1e-9));
        }
        return 0;
    };

    // Pixel (clientX/clientY) → celda absoluta {col, row}. F5: si el grid aún no tiene tamaño
    // (rect vacío / cols|rows 0) devuelve la celda del origen del viewport en vez de NaN.
    SelLogic.cellFromPoint = function (cx, cy, rect, cols, rows, viewportY) {
        if (!rect || !(rect.width > 0) || !(rect.height > 0) || !(cols > 0) || !(rows > 0)) {
            return { col: 0, row: viewportY || 0 };
        }
        var cellW = rect.width / cols;
        var cellH = rect.height / rows;
        var col = Math.floor((cx - rect.left) / cellW);
        var vrow = Math.floor((cy - rect.top) / cellH);
        col = Math.max(0, Math.min(cols - 1, col));
        vrow = Math.max(0, Math.min(rows - 1, vrow));
        return { col: col, row: (viewportY || 0) + vrow };
    };

    root.SelLogic = SelLogic;
})(typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : this));

if (typeof module !== 'undefined' && module.exports) {
    module.exports = (typeof window !== 'undefined' ? window.SelLogic
        : (typeof globalThis !== 'undefined' ? globalThis.SelLogic : this.SelLogic));
}
