const $ = selector => document.querySelector(selector);
const categories = [];
const notes = [];
const initialTexts = [
  'I feel calmer after a walk.',
  'What if the library hosted a seed exchange?',
  'Book the train for Friday.',
  'The client approved the revised roadmap.',
];
const labText = 'Why not build a seed exchange?';
const colors = { thoughts: '#80956e', ideas: '#c59740', todos: '#559783', questions: '#8096b1', work: '#bb866c', links: '#8b8fae', junk: '#a1a18f' };
let view = 'inbox';
let filter = null;
let ready = false;
let sending = false;
let generation = 0;
let requestNumber = 0;
let approvedCount = 0;
let toastTimer;
const pending = new Map();

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
function icon(name) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('aria-hidden', 'true');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', `#i-${name}`);
  svg.append(use);
  return svg;
}
const categoryTitle = label => categories.find(c => c.name === label)?.title || label;
function toast(message) {
  $('#toast').textContent = message;
  $('#toast').classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => $('#toast').classList.remove('visible'), 4500);
}
function announce(message) { $('#announce').textContent = message; }
function resultMessage(message) { $('#demo-result').replaceChildren(icon('spark'), element('span', '', message)); }
function setReadyControls() {
  $('#note-input').disabled = !ready;
  $('.send-button').disabled = !ready || sending;
  $('#teach-note').disabled = !ready;
  for (const button of document.querySelectorAll('[data-example]')) button.disabled = !ready;
}
function updateMemory(count) {
  approvedCount = count;
  $('#memory-count').textContent = `${count} confirmed`;
}

let worker;
function request(type, data = {}) {
  const id = ++requestNumber;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(new Error('The local demo took too long. Please reset or reload.')); }, 20000);
    pending.set(id, { resolve, reject, timer });
    worker.postMessage({ id, type, ...data });
  });
}
function startupError(message) {
  ready = false;
  setReadyControls();
  $('#engine-status').textContent = 'Demo unavailable · explore the app below';
  $('#note-area').replaceChildren(element('p', 'empty-state', message));
  $('#lab-before').textContent = 'Local demo unavailable';
  for (const item of pending.values()) { clearTimeout(item.timer); item.reject(new Error(message)); }
  pending.clear();
}

function noteCard(note) {
  const card = element('article', `note-card${note.done ? ' is-done' : ''}`);
  card.style.setProperty('--tag-color', colors[note.result.label] || '#80956e');
  const text = element('p', 'note-text', note.text);
  if (note.result.label === 'todos') {
    const line = element('label', 'task-line');
    const check = element('input');
    check.type = 'checkbox';
    check.checked = Boolean(note.done);
    check.setAttribute('aria-label', `Complete task: ${note.text}`);
    check.addEventListener('change', () => {
      note.done = check.checked;
      card.classList.toggle('is-done', note.done);
      announce(note.done ? 'Task completed. The original stays in Inbox.' : 'Task reopened.');
    });
    line.append(check, text);
    card.append(line);
  } else card.append(text);
  const bottom = element('div', 'note-bottom');
  const dot = element('span', 'note-dot');
  dot.setAttribute('aria-hidden', 'true');
  const select = element('select', 'note-category');
  select.setAttribute('aria-label', `Category for: ${note.text}`);
  for (const category of categories) {
    const option = element('option', '', category.title);
    option.value = category.name;
    option.selected = category.name === note.result.label;
    select.append(option);
  }
  select.addEventListener('change', async () => {
    select.disabled = true;
    try { await approve(note, select.value); }
    catch (error) { select.value = note.result.label; toast(error.message); }
    finally { select.disabled = false; }
  });
  const button = element('button', 'approve-button');
  button.type = 'button';
  button.setAttribute('aria-pressed', String(Boolean(note.approved)));
  button.setAttribute('aria-label', `Confirm ${categoryTitle(note.result.label)} category for: ${note.text}`);
  button.title = note.approved ? 'Your confirmed label' : 'Confirm this category';
  button.append(icon('check'));
  button.addEventListener('click', async () => {
    button.disabled = true;
    try { await approve(note, note.result.label); }
    catch (error) { toast(error.message); button.disabled = false; }
  });
  bottom.append(dot, select, button);
  const reason = element('details', 'note-reason');
  reason.append(element('summary', '', note.approved ? 'Your confirmed label' : 'Why this tag?'), element('p', '', note.result.reason));
  card.append(bottom, reason);
  return card;
}
function render() {
  const area = $('#note-area');
  area.replaceChildren();
  for (const button of document.querySelectorAll('[data-view]')) button.setAttribute('aria-pressed', String(button.dataset.view === view));
  if (view === 'boards' && !filter) {
    area.append(element('p', 'note-date', 'THE SAME NOTES, ANOTHER WAY'));
    const boards = element('div', 'board-grid');
    for (const category of categories) {
      const count = notes.filter(n => n.result.label === category.name).length;
      const board = element('button', 'mini-board');
      board.type = 'button';
      board.style.setProperty('--tag-color', colors[category.name]);
      const dot = element('span', 'note-dot');
      board.append(dot, element('strong', '', category.title), element('small', '', `${count} ${count === 1 ? 'note' : 'notes'}`));
      board.setAttribute('aria-label', `Open ${category.title} board, ${count} notes`);
      board.addEventListener('click', () => { filter = category.name; render(); });
      boards.append(board);
    }
    area.append(boards);
    return;
  }
  if (filter) {
    const back = element('button', 'board-filter');
    back.type = 'button';
    back.append(icon('arrow'), document.createTextNode(`All boards / ${categoryTitle(filter)}`));
    back.addEventListener('click', () => { filter = null; render(); });
    area.append(back);
  } else area.append(element('p', 'note-date', view === 'todos' ? 'A LITTLE ROOM TO GET THINGS DONE' : 'SAMPLE NOTES'));
  const visible = notes.filter(n => view === 'todos' ? n.result.label === 'todos' : !filter || n.result.label === filter);
  if (!visible.length) area.append(element('p', 'empty-state', view === 'todos' ? 'Nothing on your to-do list. Try “Remember to book the train.”' : 'A little empty space. Write a note or choose another category.'));
  for (const note of visible) area.append(noteCard(note));
}
async function approve(note, label) {
  const currentGeneration = generation;
  const { count } = await request('approve', { text: note.text, label });
  if (currentGeneration !== generation) return;
  updateMemory(count);
  note.result = await request('predict', { text: note.text });
  if (currentGeneration !== generation) return;
  note.approved = true;
  if (label !== 'todos') note.done = false;
  render();
  resultMessage(`Your label: ${categoryTitle(label)}. Remembered in this tab.`);
  announce(`Category confirmed as ${categoryTitle(label)}.`);
}
async function addNote(text) {
  if (!ready || sending) return;
  text = text.trim();
  if (!text) { $('#note-input').focus(); return; }
  if (text.length > 2000) { toast('Try a note of 2,000 characters or fewer.'); return; }
  if (notes.length >= 20) { toast('This little demo holds 20 notes. Reset it for a fresh page.'); return; }
  const currentGeneration = generation;
  sending = true;
  setReadyControls();
  try {
    const result = await request('predict', { text });
    if (currentGeneration !== generation) return;
    notes.push({ id: crypto.randomUUID(), text, result, approved: result.source === 'approved', done: false });
    view = 'inbox'; filter = null;
    render();
    $('#note-input').value = '';
    $('#note-input').style.height = '';
    $('#note-area').scrollTop = $('#note-area').scrollHeight;
    resultMessage(`${categoryTitle(result.label)} · ${result.source === 'approved' ? 'Your approved example' : result.source === 'wording' ? 'A wording cue' : 'The small classifier'}`);
    announce(`Note added. Suggested category: ${categoryTitle(result.label)}.`);
  } catch (error) { toast(error.message); }
  finally { sending = false; setReadyControls(); }
}
async function loadSamples() {
  notes.length = 0;
  for (const text of initialTexts) notes.push({ id: crypto.randomUUID(), text, result: await request('predict', { text }), done: false, approved: false });
  render();
  const starter = await request('predict', { text: labText });
  $('#lab-before').textContent = categoryTitle(starter.label);
}
function clearLab() {
  $('#lab-outcome').classList.remove('confirmed');
  $('#lab-outcome').replaceChildren(element('span', 'outcome-icon', '↳'), element('p', '', 'Save a label to see how example memory works.'));
}
async function resetDemo(resetNotes) {
  if (!ready) return;
  ready = false; generation++; setReadyControls();
  try {
    const { count } = await request('reset');
    updateMemory(count); clearLab();
    if (resetNotes) { view = 'inbox'; filter = null; await loadSamples(); $('#note-input').value = ''; }
    else {
      for (const note of notes) { note.result = await request('predict', { text: note.text }); note.approved = false; }
      render();
      $('#lab-before').textContent = categoryTitle((await request('predict', { text: labText })).label);
    }
    resultMessage('A fresh start. Demo labels cleared.');
    toast(resetNotes ? 'Fresh sample notes. All demo labels cleared.' : 'Your demo labels have been cleared.');
  } catch (error) { toast(error.message); }
  finally { ready = true; setReadyControls(); }
}

$('#note-form').addEventListener('submit', event => { event.preventDefault(); addNote($('#note-input').value); });
$('#note-input').addEventListener('keydown', event => {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); addNote(event.currentTarget.value); }
});
$('#note-input').addEventListener('input', event => { const input = event.currentTarget; input.style.height = 'auto'; input.style.height = `${Math.min(input.scrollHeight, 70)}px`; });
for (const button of document.querySelectorAll('[data-example]')) button.addEventListener('click', () => { $('#note-input').value = button.dataset.example; $('#note-input').focus(); });
for (const button of document.querySelectorAll('[data-view]')) button.addEventListener('click', () => { if (!ready) return; view = button.dataset.view; filter = null; render(); });
for (const button of document.querySelectorAll('.reset-demo')) button.addEventListener('click', () => resetDemo(true));
$('#reset-learning').addEventListener('click', () => resetDemo(false));
for (const anchor of document.querySelectorAll('a[href="#demo"]')) anchor.addEventListener('click', () => requestAnimationFrame(() => $('#note-input').focus({ preventScroll: true })));

$('#teach-note').addEventListener('click', async () => {
  const button = $('#teach-note');
  const currentGeneration = generation;
  button.disabled = true;
  const label = $('#lab-category').value;
  try {
    const { count } = await request('approve', { text: labText, label });
    const after = await request('predict', { text: labText });
    if (currentGeneration !== generation) return;
    updateMemory(count);
    const copy = element('p');
    copy.append(element('strong', '', `Now: ${categoryTitle(after.label)}. `), document.createTextNode('The label is saved. '));
    const tryAgain = element('button', 'lab-try', 'Try this note in Inbox →');
    tryAgain.addEventListener('click', () => {
      addNote(labText);
      $('#demo').scrollIntoView({ behavior: 'auto', block: 'center' });
    });
    copy.append(tryAgain);
    $('#lab-outcome').replaceChildren(element('span', 'outcome-icon', '✓'), copy);
    $('#lab-outcome').classList.add('confirmed');
    announce(`The learner now remembers this exact note as ${categoryTitle(after.label)}.`);
    for (const note of notes.filter(n => n.text.trim().toLowerCase() === labText.toLowerCase())) { note.result = after; note.approved = true; }
    render();
  } catch (error) { toast(error.message); }
  finally { button.disabled = !ready; }
});

const steps = {
  capture: { title: 'Two different possible interfaces!', text: 'The same notes, displayed any way you choose.\nA familiar messaging app view, or the usual notes-and-boards view, with its own personal touch.', tradeoff: 'The browser demo uses temporary text notes.', path: 'Domain.kt', link: 'android/app/src/main/java/dev/notechat/Domain.kt' },
  classify: { title: 'Local classification.', text: 'Wording cues and a small classifier suggest a category.\nOptional MiniLM adds sentence embeddings in the Android app.', tradeoff: 'The browser demo uses the built-in classifier without MiniLM.', path: 'TextClassifier.kt', link: 'android/app/src/main/java/dev/notechat/TextClassifier.kt' },
  confirm: { title: 'Confirmed labels teach it.', text: 'Approvals and category assignments tell the classifier to learn.\nIts normal predictions do not teach it until you confirm them. Thus, it learns from you, your way.', tradeoff: 'Android also offers personal head training with held-out evaluation.', path: 'ClassifierTest.kt', link: 'android/app/src/test/java/dev/notechat/ClassifierTest.kt' },
  keep: { title: 'Storage and export.', text: 'Android encrypts notes and attachments.\nYou can add a folder on your phone to save your notes, and save encrypted backups to Google Drive!', tradeoff: 'Markdown exports are readable files outside app lock.', path: 'Vault.kt', link: 'android/app/src/main/java/dev/notechat/Vault.kt' },
};
function showStep(name) {
  const data = steps[name];
  for (const node of document.querySelectorAll('[data-step]')) { node.classList.toggle('active', node.dataset.step === name); node.setAttribute('aria-pressed', String(node.dataset.step === name)); }
  const heading = element('div');
  const source = element('a', '', `Explore ${data.path}`);
  source.href = `https://github.com/TarunB-Git/note-processing/blob/main/${data.link}`;
  source.append(icon('diagonal'));
  heading.append(element('h3', '', data.title), source);
  const description = element('div');
  description.append(element('p', '', data.text), element('p', 'detail-tradeoff', data.tradeoff));
  $('#system-detail').replaceChildren(heading, description);
}
for (const button of document.querySelectorAll('[data-step]')) button.addEventListener('click', () => showStep(button.dataset.step));
showStep('capture');
const dialog = $('#demo-dialog');
for (const selector of ['#about-demo', '#privacy-link']) $(selector).addEventListener('click', () => dialog.showModal());
$('.dialog-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => {
  const box = dialog.getBoundingClientRect();
  if (event.target === dialog && (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom)) dialog.close();
});

try {
  worker = new Worker(new URL('./classifier.worker.mjs', import.meta.url), { type: 'module' });
  worker.onmessage = async ({ data }) => {
    if (data.type === 'ready') {
      categories.push(...data.categories);
      try { await loadSamples(); ready = true; $('#engine-status').textContent = 'Local classifier'; setReadyControls(); }
      catch (error) { startupError(error.message); }
      return;
    }
    if (data.type === 'startup-error') { startupError(data.error); return; }
    const item = pending.get(data.id);
    if (!item) return;
    clearTimeout(item.timer); pending.delete(data.id);
    if (data.error) item.reject(new Error(data.error)); else item.resolve(data.result);
  };
  worker.onerror = () => startupError('The local classifier could not load. Try reloading in a current browser.');
} catch { startupError('Your browser does not support the local demo. Explore the Android release below.'); }
