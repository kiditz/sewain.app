jest.mock('whatsapp-web.js', () => ({ Client: jest.fn(), LocalAuth: jest.fn() }))
const fs = require('fs')
const path = require('path')
const { deleteSession } = require('../src/sessions')
const { sessionFolderPath } = require('../src/config')

afterEach(() => jest.restoreAllMocks())

test('disconnect succeeds quietly when the session directory is already gone', async () => {
  jest.spyOn(fs.promises, 'realpath').mockRejectedValue(Object.assign(new Error('missing'), { code: 'ENOENT' }))
  const remove = jest.spyOn(fs.promises, 'rm').mockResolvedValue()
  const log = jest.spyOn(console, 'log').mockImplementation(() => {})
  await expect(deleteSession('already-removed', { success: false })).resolves.toBeUndefined()
  expect(remove).not.toHaveBeenCalled()
  expect(log).not.toHaveBeenCalled()
})

test('permission errors remain visible and are not treated as successful cleanup', async () => {
  const error = Object.assign(new Error('permission denied'), { code: 'EACCES' })
  jest.spyOn(fs.promises, 'realpath').mockRejectedValue(error)
  jest.spyOn(console, 'log').mockImplementation(() => {})
  await expect(deleteSession('permission-error', { success: false })).rejects.toBe(error)
})

test('refuses a session directory resolving outside the sessions volume', async () => {
  const root = path.resolve(sessionFolderPath)
  jest.spyOn(fs.promises, 'realpath').mockImplementation(async value => value === sessionFolderPath ? root : path.join(path.dirname(root), 'outside'))
  const remove = jest.spyOn(fs.promises, 'rm').mockResolvedValue()
  jest.spyOn(console, 'log').mockImplementation(() => {})
  await expect(deleteSession('outside-session', { success: false })).rejects.toThrow('Directory traversal detected')
  expect(remove).not.toHaveBeenCalled()
})
