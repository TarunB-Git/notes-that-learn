import { createClassifier } from './classifier.mjs';

let classifier;
let categories;
const ready = (async () => {
  const response = await fetch('./classes.json');
  if (!response.ok) throw new Error('The demo categories could not load. Please reload.');
  categories = await response.json();
  classifier = await createClassifier(categories);
  postMessage({ type: 'ready', categories });
})().catch(error => postMessage({ type: 'startup-error', error: error.message }));

onmessage = async ({ data }) => {
  try {
    await ready;
    if (!classifier) throw new Error('The demo could not start. Please reload.');
    let result;
    if (data.type === 'predict') result = await classifier.predict(data.text);
    else if (data.type === 'approve') result = { count: classifier.approve(data.text, data.label) };
    else if (data.type === 'reset') { classifier.reset(); result = { count: 0 }; }
    else throw new Error('Unknown demo action.');
    postMessage({ id: data.id, result });
  } catch (error) { postMessage({ id: data.id, error: error.message }); }
};
