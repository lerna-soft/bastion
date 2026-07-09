// Tests de la lógica pura de selección (HIM-020). Correr: node selection-logic.test.js
// Sin dependencias: mini-harness + assert nativo.
const assert = require('assert');
const S = require('./selection-logic.js');

let passed = 0, failed = 0;
function it(name, fn) {
    try { fn(); passed++; console.log('  ✓ ' + name); }
    catch (e) { failed++; console.log('  ✗ ' + name + '\n      ' + e.message); }
}
function group(name) { console.log('\n' + name); }

group('cmpCell / normalize');
it('ordena por fila y luego columna', () => {
    assert.ok(S.cmpCell({ col: 5, row: 0 }, { col: 0, row: 1 }) < 0);
    assert.ok(S.cmpCell({ col: 5, row: 2 }, { col: 3, row: 2 }) > 0);
    assert.strictEqual(S.cmpCell({ col: 4, row: 1 }, { col: 4, row: 1 }), 0);
});
it('normalize invierte cuando b < a', () => {
    const n = S.normalize({ col: 2, row: 1 }, { col: 5, row: 0 });
    assert.deepStrictEqual(n.start, { col: 5, row: 0 });
    assert.deepStrictEqual(n.end, { col: 2, row: 1 });
});

group('selLength');
it('una sola celda = 1', () => {
    assert.strictEqual(S.selLength({ col: 3, row: 0 }, { col: 3, row: 0 }, 80), 1);
});
it('rango en la misma fila', () => {
    assert.strictEqual(S.selLength({ col: 0, row: 0 }, { col: 4, row: 0 }, 80), 5);
});
it('rango que cruza filas con wrap (end.col < start.col)', () => {
    // (5,0)->(2,1) con cols=10: 5,6,7,8,9 (5) + 0,1,2 (3) = 8
    assert.strictEqual(S.selLength({ col: 5, row: 0 }, { col: 2, row: 1 }, 10), 8);
});
it('nunca menor que 1', () => {
    assert.strictEqual(S.selLength({ col: 5, row: 1 }, { col: 0, row: 0 }, 10), 1);
});

group('wordBounds');
it('palabra en el medio', () => {
    assert.deepStrictEqual(S.wordBounds('foo bar baz', 5), { start: 4, end: 6 });
});
it('palabra al inicio', () => {
    assert.deepStrictEqual(S.wordBounds('hello world', 0), { start: 0, end: 4 });
});
it('palabra al final', () => {
    assert.deepStrictEqual(S.wordBounds('ab cd', 4), { start: 3, end: 4 });
});
it('col sobre un espacio → null', () => {
    assert.strictEqual(S.wordBounds('foo bar', 3), null);
});
it('col fuera de rango → null', () => {
    assert.strictEqual(S.wordBounds('foo', 10), null);
    assert.strictEqual(S.wordBounds('foo', -1), null);
});
it('cadena con símbolos cuenta como no-espacio', () => {
    assert.deepStrictEqual(S.wordBounds('/usr/bin ls', 4), { start: 0, end: 7 });
});

group('moveFocus');
it('ArrowRight avanza columna', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowRight', { col: 3, row: 2 }, 80, 100), { col: 4, row: 2 });
});
it('ArrowRight al fin de línea envuelve a la fila siguiente', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowRight', { col: 79, row: 2 }, 80, 100), { col: 0, row: 3 });
});
it('ArrowRight al fin del buffer NO se pasa (F3)', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowRight', { col: 79, row: 99 }, 80, 100), { col: 79, row: 99 });
});
it('ArrowLeft al inicio de línea envuelve a la fila anterior', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowLeft', { col: 0, row: 3 }, 80, 100), { col: 79, row: 2 });
});
it('ArrowLeft en (0,0) no se mueve', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowLeft', { col: 0, row: 0 }, 80, 100), { col: 0, row: 0 });
});
it('ArrowUp/ArrowDown clampan a los límites del buffer', () => {
    assert.deepStrictEqual(S.moveFocus('ArrowUp', { col: 5, row: 0 }, 80, 100), { col: 5, row: 0 });
    assert.deepStrictEqual(S.moveFocus('ArrowDown', { col: 5, row: 99 }, 80, 100), { col: 5, row: 99 });
});
it('Home / End van a los extremos de la fila', () => {
    assert.deepStrictEqual(S.moveFocus('Home', { col: 40, row: 2 }, 80, 100), { col: 0, row: 2 });
    assert.deepStrictEqual(S.moveFocus('End', { col: 40, row: 2 }, 80, 100), { col: 79, row: 2 });
});

group('autoScrollStep (acelerado, F2)');
it('fuera de los bordes = 0', () => {
    assert.strictEqual(S.autoScrollStep(500, 0, 1000, 100, 3), 0);
});
it('cerca del borde superior = negativo', () => {
    assert.ok(S.autoScrollStep(50, 0, 1000, 100, 3) < 0);
});
it('en el mismísimo borde superior = velocidad máxima', () => {
    assert.strictEqual(S.autoScrollStep(0, 0, 1000, 100, 3), -3);
});
it('apenas entrando a la zona superior = velocidad mínima', () => {
    assert.strictEqual(S.autoScrollStep(99, 0, 1000, 100, 3), -1);
});
it('en el borde inferior = velocidad máxima positiva', () => {
    assert.strictEqual(S.autoScrollStep(1000, 0, 1000, 100, 3), 3);
});
it('edge no positivo = 0 (sin división por cero)', () => {
    assert.strictEqual(S.autoScrollStep(0, 0, 1000, 0, 3), 0);
});

group('cellFromPoint');
const rect = { left: 0, top: 0, width: 800, height: 480 };
it('mapea pixel a celda con offset de viewport', () => {
    // cellW=10, cellH=20; (105, 45) → col 10, vrow 2 → row = 500+2
    assert.deepStrictEqual(S.cellFromPoint(105, 45, rect, 80, 24, 500), { col: 10, row: 502 });
});
it('clampa a los límites del grid', () => {
    assert.deepStrictEqual(S.cellFromPoint(99999, 99999, rect, 80, 24, 0), { col: 79, row: 23 });
    assert.deepStrictEqual(S.cellFromPoint(-50, -50, rect, 80, 24, 0), { col: 0, row: 0 });
});
it('grid sin tamaño → celda de origen, no NaN (F5)', () => {
    const r0 = { left: 0, top: 0, width: 0, height: 0 };
    const c = S.cellFromPoint(100, 100, r0, 0, 0, 7);
    assert.deepStrictEqual(c, { col: 0, row: 7 });
    assert.ok(!Number.isNaN(c.col) && !Number.isNaN(c.row));
});

console.log('\n' + passed + ' passed, ' + failed + ' failed');
process.exit(failed ? 1 : 0);
