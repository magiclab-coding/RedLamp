const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const html = readFileSync(require('node:path').join(__dirname, '..', 'studio.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
class Element {
  constructor() {
    this.attributes = {};
    this.style = new Map();
    this.style.setProperty = this.style.set;
    this.children = [];
  }
  setAttribute(key, value) { this.attributes[key] = value; }
  removeAttribute(key) { delete this.attributes[key]; }
  appendChild(child) { this.children.push(child); }
  setPointerCapture(id) { this.pointer = id; }
  hasPointerCapture(id) { return this.pointer === id; }
  getBoundingClientRect() { return {left: 0, top: 0, width: 320, height: 286}; }
}
const elements = new Map();
const byId = id => {
  if (!elements.has(id)) elements.set(id, new Element());
  return elements.get(id);
};
const calls = [];
const window = {scrollTo() {}};
const customMode = byId('custom-mode');
const document = {body: {dataset: {}}, getElementById: byId, querySelector: () => customMode, querySelectorAll: () => [], addEventListener() {}, createElement: () => new Element()};
const ui = vm.createContext({window, document, LampApp: {act: (...args) => calls.push(args)}});
vm.runInContext(script, ui);
const update = window.update;
const base = {online: true, b: 10, br: 0, t: 20, tr: 0, k: 3300, kw: 2500, kc: 6500, db: 75, dt: 40};
update(base);
const bright = byId('bright');
const dial = byId('dial');
bright.value = 83;
bright.oninput();
update(base);
assert.equal(bright.value, 83);
bright.onchange();
const point = {pointerId: 1, clientX: 160, clientY: 31};
dial.onpointerdown(point);
update(base);
assert.equal(byId('temp').textContent, '50 %');
dial.onpointerup(point);
update(base);
assert.equal(bright.value, 83);
assert.equal(byId('temp').textContent, '50 %');
dial.onkeydown({key: 'ArrowRight', preventDefault() {}});
update({...base, b: 83, br: 1, t: 50, tr: 1, k: 4500});
assert.equal(byId('temp').textContent, '51 %');
assert.equal(byId('kel').textContent, '4540 K');
assert.equal(dial.attributes['aria-valuetext'], '4540 K');
update({...base, b: 83, br: 1, t: 51, tr: 2, k: 4540});
assert.equal(byId('temp').textContent, '51 %');
assert.deepEqual(calls.map(x=>x.slice(0,3)), [['brightness',83,1],['temp',50,1],['temp',51,2]]);
vm.runInContext("show('defaults')", ui);
assert.equal(byId('tv').textContent, '4100 K');
byId('dt').value = 100;
vm.runInContext('form()', ui);
assert.equal(byId('tv').textContent, '6500 K');
assert.equal(byId('dt').attributes['aria-valuetext'], '6500 K');
update({...base, online: false});
assert.equal(byId('temp').textContent, '—');
calls.length = 0;
update({...base, kw:2703, t:0, k:2703, tr:2, on:true});
assert.equal(byId('kel').textContent, '2700 K');
assert.equal(byId('ringbox').style.get('--bulb-tone'), 'rgb(255,199,104)');
byId('dialPower').onclick();
assert.deepEqual(calls.map(call => call[0]), ['power']);
assert.equal(byId('kel').textContent, '2700 K');
update({...base, kw:2703, t:100, k:6500, tr:2, on:false});
assert.equal(byId('kel').textContent, '6500 K');
assert.equal(byId('ringbox').style.get('--bulb-tone'), 'rgb(196,233,252)');
assert.equal(byId('dialPower').attributes['aria-pressed'], 'false');
update({...base, online:false});
byId('dialPower').onclick();
assert.deepEqual(calls.map(call => call[0]), ['power']);
assert.match(html, /-webkit-tap-highlight-color:transparent/);
assert.match(html, /top:53\.4965%;left:50%;transform:translate\(-50%,-50%\)/);
console.log('PASS: dial, brightness, delayed snapshots, rapid retargeting, touch styles');
