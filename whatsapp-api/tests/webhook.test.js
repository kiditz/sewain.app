jest.mock('axios', () => ({ post: jest.fn() }))
jest.mock('../src/config', () => ({ disabledCallbacks: [], webhookSecret: 'test-secret', webhookTextOnly: true }))
const axios = require('axios')
const crypto = require('crypto')
const { triggerWebhook } = require('../src/utils')

beforeEach(() => { jest.clearAllMocks(); axios.post.mockResolvedValue({ status: 204 }) })

test('signs exact bytes and projects inbound messages without attachments or private client data', async () => {
  await triggerWebhook('http://backend/webhook', 'session-a', 'message', {
    messageMedia: { data: 'large-base64' },
    message: { id: { _serialized: 'message-1' }, from: '628123456789@c.us', fromMe: false, type: 'chat', body: 'Hello', _data: { secret: 'omit' } }
  })
  const [url, body, options] = axios.post.mock.calls[0]
  expect(url).toBe('http://backend/webhook')
  const payload = JSON.parse(body)
  expect(payload.data.message.body).toBe('Hello')
  expect(payload.data.messageMedia).toBeUndefined()
  expect(payload.data.message._data).toBeUndefined()
  const timestamp = options.headers['X-Webhook-Timestamp']
  const signature = crypto.createHmac('sha256', 'test-secret').update(`${timestamp}.${body}`).digest('hex')
  expect(options.headers['X-Webhook-Signature']).toBe(signature)
  expect(options.maxRedirects).toBe(0)
})

test('never forwards QR credentials in callbacks and skips unsupported events', async () => {
  await triggerWebhook('http://backend/webhook', 'session-a', 'qr', { qr: 'secret-pairing-code' })
  expect(JSON.parse(axios.post.mock.calls[0][1]).data).toEqual({})
  await triggerWebhook('http://backend/webhook', 'session-a', 'message_ack', { message: {} })
  expect(axios.post).toHaveBeenCalledTimes(1)
})

test('does not retry authentication failures', async () => {
  const errorLog = jest.spyOn(console, 'error').mockImplementation(() => {})
  axios.post.mockRejectedValue({ response: { status: 401 } })
  await triggerWebhook('http://backend/webhook', 'session-a', 'ready', {})
  expect(axios.post).toHaveBeenCalledTimes(1)
  errorLog.mockRestore()
})
