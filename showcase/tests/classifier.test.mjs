import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createClassifier } from '../src/classifier.mjs';

const categories = JSON.parse(await readFile(new URL('../../android/app/src/main/assets/classes.json', import.meta.url), 'utf8'));
const classifier = await createClassifier(categories);
async function check(name, action) {
  await action();
  console.log(`PASS: ${name}`);
}

await check('the demo handles distinct everyday intentions with the built-in path', async () => {
  const examples = [
    ['I feel calmer after a walk.', 'thoughts'],
    ['What if the library hosted a seed exchange?', 'ideas'],
    ['Book the train for Friday.', 'todos'],
    ['Why do leaves change color?', 'questions'],
    ['https://example.org/books', 'links'],
    ['const value = 42;', 'junk'],
    ['val x = listOf(1,2,3).map { it * 2 }', 'junk'],
    ['fun greet(name: String) = name', 'junk'],
    ['SELECT * FROM notes;', 'junk'],
    ['The client approved the revised roadmap.', 'work'],
  ];
  for (const [text, label] of examples) assert.equal((await classifier.predict(text)).label, label);
});

await check('predictions do not teach labels; an explicit correction overrides a wording cue', async () => {
  const text = 'Why not build a seed exchange?';
  assert.equal((await classifier.predict(text)).label, 'questions');
  assert.equal(classifier.approvedCount, 0);
  classifier.approve(text, 'ideas');
  assert.deepEqual(await classifier.predict('  WHY not build a   seed exchange?  '), {
    label: 'ideas', source: 'approved', reason: 'You previously confirmed this exact note.',
  });
  assert.equal((await classifier.predict('Why not build a bicycle shelter?')).source, 'wording');
  assert.equal(classifier.approvedCount, 1);
});

await check('reset forgets labels and returns to the starter behavior', async () => {
  classifier.reset();
  assert.equal(classifier.approvedCount, 0);
  assert.equal((await classifier.predict('Why not build a seed exchange?')).label, 'questions');
});

await check('invalid confirmations and blank predictions fail without adding memory', async () => {
  assert.throws(() => classifier.approve('A note', 'missing-category'), /valid category/);
  assert.throws(() => classifier.approve(' ', 'thoughts'), /valid category/);
  await assert.rejects(classifier.predict('  '), /Write a note/);
  assert.equal(classifier.approvedCount, 0);
});

await check('demo memory is bounded and an existing label can still be corrected', () => {
  for (let i = 0; i < 100; i++) classifier.approve(`Synthetic note ${i}`, 'thoughts');
  assert.throws(() => classifier.approve('Another synthetic note', 'ideas'), /100 notes/);
  classifier.approve('Synthetic note 0', 'ideas');
  assert.equal(classifier.approvedCount, 100);
  classifier.reset();
});
