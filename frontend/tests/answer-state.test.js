import { test } from 'node:test'
import assert from 'node:assert/strict'
import { remainingSeconds, payload, makeSaveQueue } from '../src/answer-state.js'
test('remaining time uses server offset and clamps at deadline', () => {
  assert.equal(remainingSeconds(10000, 5000), 5)
  assert.equal(remainingSeconds(10000, 10000), 0)
  assert.equal(remainingSeconds(10000, 12000), 0)
})
test('payload includes empty answers to clear previous selections', () => {
  assert.deepEqual(payload({'11': [], '12': ['A','B']}, 4), {version: 4, answers: [{questionId:11,answer:[]},{questionId:12,answer:['A','B']}]})
})
test('save queue serializes versions and survives a failed save', async () => {
  const queue = makeSaveQueue()
  let version = 0
  const a = queue(async () => { await new Promise(r => setTimeout(r, 10)); version++ })
  const b = queue(async () => { assert.equal(version, 1); throw new Error('network') })
  const failed = assert.rejects(b, /network/)
  const c = queue(async () => ++version)
  await a; await failed; assert.equal(await c, 2)
})
