import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.dirname(fileURLToPath(import.meta.url));
const output = path.join(root, 'dist');
await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
await cp(path.join(root, 'src'), output, { recursive: true });
const classes = JSON.parse(await readFile(path.join(root, '../android/app/src/main/assets/classes.json'), 'utf8'));
await cp(path.join(root, '../android/app/src/main/assets/classes.json'), path.join(output, 'classes.json'));
await mkdir(path.join(output, 'assets'), { recursive: true });
for (const name of ['android-inbox.png', 'android-notes.png']) {
  await cp(path.join(root, '../docs', name), path.join(output, 'assets', name));
}
// A self-contained handoff keeps the design review independent of a server.
// Trusted source is embedded at build time; visitor input still uses textContent.
let standalone = await readFile(path.join(output, 'index.html'), 'utf8');
const css = await readFile(path.join(output, 'style.css'), 'utf8');
const classifierSource = await readFile(path.join(output, 'classifier.mjs'), 'utf8');
let workerSource = await readFile(path.join(output, 'classifier.worker.mjs'), 'utf8');
workerSource = workerSource.replace(/  const response = await fetch\('\.\/classes\.json'\);\n  if \(!response.ok\) throw new Error\([^\n]+\n  categories = await response.json\(\);/, `  categories = ${JSON.stringify(classes)};`);
const appSource = await readFile(path.join(output, 'app.mjs'), 'utf8');
// One classic worker avoids module-import restrictions in local-file exports.
const bundledWorker = classifierSource.replace(/^export /gm, '') + '\n' + workerSource.replace("import { createClassifier } from './classifier.mjs';", '');
const script = `const workerURL = URL.createObjectURL(new Blob([${JSON.stringify(bundledWorker)}], {type:'text/javascript'}));\n${appSource.replace("new Worker(new URL('./classifier.worker.mjs', import.meta.url), { type: 'module' })", 'new Worker(workerURL)')}`;
standalone = standalone.replace(/<meta http-equiv="Content-Security-Policy"[^>]+>/, `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; script-src 'unsafe-inline' blob:; worker-src blob:; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'">`);
standalone = standalone.replace('<link rel="stylesheet" href="style.css">', `<style>${css}</style>`);
standalone = standalone.replace('<script type="module" src="app.mjs"></script>', () => `<script type="module">${script.replaceAll('</script', '<\\/script')}</script>`);
for (const [asset, type] of [['icon.svg', 'image/svg+xml'], ['assets/android-inbox.png', 'image/png'], ['assets/android-notes.png', 'image/png']]) {
  const data = `data:${type};base64,${(await readFile(path.join(output, asset))).toString('base64')}`;
  standalone = standalone.replaceAll(`"${asset}"`, `"${data}"`);
}
await writeFile(path.join(output, 'note-chat-showcase.html'), standalone);
console.log(`Built ${classes.length}-category showcase: ${output}`);
