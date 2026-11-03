export function remainingSeconds(deadline, now) { return Math.max(0, Math.ceil((deadline - now) / 1000)) }
export function payload(answers, version) {
  return { version, answers: Object.entries(answers).map(([id, answer]) => ({ questionId: Number(id), answer: [...answer] })) }
}
export function makeSaveQueue() {
  let tail = Promise.resolve()
  return job => { const next = tail.then(job); tail = next.catch(() => {}); return next }
}
