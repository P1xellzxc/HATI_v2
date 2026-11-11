// D9 paste-a-receipt: the parser's unit table, in plain node (no browser). Reads receiptCents() from the source
// between /*RECEIPT{*/ and /*}RECEIPT*/ so the test runs the shipped function. Usage: node web/tests/receipt.js
const fs = require('fs'), path = require('path');
const src = fs.readFileSync(path.join(__dirname, '../src/app.src.html'), 'utf8');
const m = /\/\*RECEIPT\{\*\/([\s\S]*?)\/\*\}RECEIPT\*\//.exec(src);
if (!m) { console.log('FAIL D9: no RECEIPT block in app.src.html'); process.exit(1); }
const MAX = Number(/const MAX_CENTS = (\d+)/.exec(src)[1]);
const receiptCents = new Function('MAX_CENTS', m[1] + '\nreturn receiptCents;')(MAX);
let fails = 0;
const ok = (c, msg) => { console.log((c ? 'PASS ' : 'FAIL ') + msg); if (!c) fails++; };
const L = (...a) => a.join('\n');
// [name, pasted text, expected: cents | {cents,label} | {candidates} | null | 'big']
const T = [
  ['plain 334.13', '334.13', 33413],
  ['peso sign', '₱334.13', 33413],
  ['PHP space', 'PHP 334.13', 33413],
  ['Php no space', 'Php334.13', 33413],
  ['thousands', '1,674.72', 167472],
  ['whole pesos', '₱245', 24500],
  ['over MAX_CENTS', '₱2,000,000,000.00', 'big'],
  ['Jollibee: TOTAL with CASH and CHANGE traps', L('JOLLIBEE FOODS CORPORATION', 'SM North EDSA', 'TIN 000-388-474-001', 'OR# 0012345   11/09/2026 12:41', '1 Chickenjoy 1pc w/ Rice   99.00', '2 Jolly Spaghetti   120.00', '1 Yumburger   115.13', 'TOTAL           334.13', 'CASH            500.00', 'CHANGE          165.87', 'VATABLE SALES   298.33', 'VAT 12%          35.80'), { cents: 33413, label: 'TOTAL' }],
  ['Jollibee: AMOUNT DUE after a SC discount', L('Jollibee', 'Subtotal 412.00', 'LESS: SC DISCOUNT 20% 73.57', 'AMOUNT DUE 338.43', 'Cash Tendered 1,000.00', 'Change 661.57'), { cents: 33843, label: 'AMOUNT DUE' }],
  ['Chowking: GRAND TOTAL beats TOTAL', L('CHOWKING', 'Chao Fan Siomai   175.00', 'Halo-Halo Regular   144.75', 'TOTAL 319.75', 'SERVICE CHARGE 10% 31.98', 'GRAND TOTAL 351.73', 'CASH 400.00', 'CHANGE 48.27'), { cents: 35173, label: 'GRAND TOTAL' }],
  ['Chowking: TOTAL (VAT inclusive)', L('Chowking Cubao', 'Lauriat 229.00', 'Total (VAT Inclusive) 319.75', 'Cash 1,000.00', 'Change 680.25'), { cents: 31975, label: 'TOTAL' }],
  ['Grab Food: Total after subtotal and delivery', L('GrabFood', 'Your order from Mang Inasal', 'Subtotal ₱398.00', 'Delivery fee ₱49.00', 'Discount -₱100.00', 'Total ₱347.00', 'Paid by GCash'), { cents: 34700, label: 'TOTAL' }],
  ['Grab ride: Total on its own line', L('Grab', 'Thanks for riding with Grab!', 'GrabCar', 'TOTAL', 'P245.00', 'Paid by Cash'), { cents: 24500, label: 'TOTAL' }],
  ['GCash: Total Amount Sent', L('GCash', 'Sent via GCash', 'Ivy F.', '+63 917 123 4567', 'Amount 1,500.00', 'Total Amount Sent ₱1,500.00', 'Ref No. 4012 345 678901', 'Nov 9, 2026 7:32 PM'), { cents: 150000, label: 'TOTAL AMOUNT' }],
  ['GCash: Pay Bills Meralco, Total Amount Due', L('Meralco', 'Account No. 1234567890', 'Amount 2,540.17', 'Convenience Fee 0.00', 'Total Amount Due PHP 2,540.17'), { cents: 254017, label: 'TOTAL AMOUNT DUE' }],
  ['7-Eleven: TOTAL with item count and CASH', L('7-ELEVEN', 'PHILIPPINE SEVEN CORPORATION', 'Big Bite Hotdog  59.00', 'Coke 1.5L  85.00', 'Total Items: 2', 'TOTAL  144.00', 'CASH  200.00', 'CHANGE  56.00'), { cents: 14400, label: 'TOTAL' }],
  ['7-Eleven: TOTAL DUE', L('7-Eleven Store #1234', 'Siopao Asado 45.00', 'Total Due 45.00', 'Cash 100.00', 'Change 55.00'), { cents: 4500, label: 'TOTAL DUE' }],
  ['Mercury Drug: TOTAL, VAT-exempt and PWD discount', L('MERCURY DRUG CORPORATION', 'Biogesic 500mg 10s   55.00', 'Neozep 10s   68.50', 'SUBTOTAL 123.50', 'LESS PWD DISCOUNT 22.05', 'VAT EXEMPT SALES 110.27', 'TOTAL 101.45', 'CASH 200.00', 'CHANGE 98.55'), { cents: 10145, label: 'TOTAL' }],
  ['Mercury Drug: Total Amount (Php)', L('Mercury Drug', 'Ascof Forte 12s 168.00', 'Total Amount Php168.00'), { cents: 16800, label: 'TOTAL AMOUNT' }],
  ['Mang Inasal: no total line, one amount', L('Mang Inasal', 'PM1 Paa Large 189.00'), { cents: 18900, label: '' }],
  ['Live Text columns: labels then numbers, chips', L('JOLLIBEE', 'Chickenjoy', 'Spaghetti', '219.00', '115.13', '334.13'), { candidates: [33413, 21900, 11513] }],
  ['no total, CASH and CHANGE skipped as chips', L('Cafe', 'Latte 180.00', 'Croissant 125.50', 'CASH 500.00', 'CHANGE 194.50'), { candidates: [18000, 12550] }],
  ['dates, times, TIN and phone are not amounts', L('Dated 11/09/2026 12:41', 'TIN 123-456-789-000', '0917-123-4567'), null],
  ['garbage', 'hello there', null],
  ['HTML pasted', '<div><b>TOTAL</b> ₱1,674.72</div>', { cents: 167472, label: 'TOTAL' }],
  ['European comma is not cents', L('Shop', 'Total 12,50'), null],
  ['huge paste is capped', 'x'.repeat(30000) + '\nTOTAL 99.00', null],
  ['odd whitespace', 'Jollibee\r\n\tTOTAL  ₱ 334.13', { cents: 33413, label: 'TOTAL' }],
];
for (const [name, text, want] of T) {
  const r = receiptCents(text);
  let pass;
  if (want === null) pass = r === null;
  else if (want === 'big') pass = !!r && r.big === true;
  else if (typeof want === 'number') pass = !!r && r.cents === want;
  else if (want.candidates) pass = !!r && JSON.stringify(r.candidates) === JSON.stringify(want.candidates) && r.cents === undefined;
  else pass = !!r && r.cents === want.cents && r.label === want.label && Number.isInteger(r.cents);
  ok(pass, `D9 parser: ${name} → ${JSON.stringify(r)}`);
}
ok(T.length >= 20, `D9 parser: the table has ${T.length} receipts (at least 20)`);
const d = receiptCents(T[7][1]);
ok(d.desc === 'JOLLIBEE FOODS CORPORATION', 'D9 parser: the description is the first text line ' + JSON.stringify(d.desc));
ok(receiptCents('A'.repeat(150) + '\nTOTAL 1.00').desc.length === 100, 'D9 parser: the description is cut at 100 characters');
process.exitCode = fails ? 1 : 0;
