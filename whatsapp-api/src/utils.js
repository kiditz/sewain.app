const axios = require('axios')
const crypto = require('crypto')
const { disabledCallbacks, webhookSecret, webhookTextOnly } = require('./config')

// Sign exactly the serialized body; retry inbound callbacks safely (backend deduplicates IDs).
const triggerWebhook = (webhookURL, sessionId, dataType, data) => {
  if (!webhookURL || !webhookSecret) return Promise.resolve()
  if (webhookTextOnly) {
    if (!['message', 'qr', 'ready', 'authenticated', 'status', 'change_state', 'disconnected'].includes(dataType)) return Promise.resolve()
    if (dataType === 'message') {
      const message = data?.message
      if (!message || message.fromMe || message.type !== 'chat') return Promise.resolve()
      data = { message: { id: { _serialized: message.id?._serialized }, from: message.from, fromMe: false, type: 'chat', body: String(message.body || '').slice(0, 4096) } }
    } else data = {} // QR and device details are retrieved through the authenticated gateway API.
  }
  const body = JSON.stringify({ dataType, data, sessionId })
  const timestamp = Math.floor(Date.now() / 1000).toString()
  const signature = crypto.createHmac('sha256', webhookSecret).update(`${timestamp}.${body}`).digest('hex')
  const deliver = async () => {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        await axios.post(webhookURL, body, { timeout: 5000, maxRedirects: 0, headers: { 'Content-Type': 'application/json', 'X-Webhook-Timestamp': timestamp, 'X-Webhook-Signature': signature } })
        return
      } catch (error) {
        if (attempt === 2 || (error.response?.status >= 400 && error.response.status < 500)) {
          console.error('Webhook delivery failed', sessionId, dataType, error.code || error.response?.status)
          return
        }
        await new Promise(resolve => setTimeout(resolve, 500 * (attempt + 1)))
      }
    }
  }
  return deliver()
}

// Function to send a response with error status and message
const sendErrorResponse = (res, status, message) => {
  res.status(status).json({ success: false, error: message })
}

// Function to wait for a specific item not to be null
const waitForNestedObject = (rootObj, nestedPath, maxWaitTime = 10000, interval = 100) => {
  const start = Date.now()
  return new Promise((resolve, reject) => {
    const checkObject = () => {
      const nestedObj = nestedPath.split('.').reduce((obj, key) => obj ? obj[key] : undefined, rootObj)
      if (nestedObj) {
        // Nested object exists, resolve the promise
        resolve()
      } else if (Date.now() - start > maxWaitTime) {
        // Maximum wait time exceeded, reject the promise
        console.log('Timed out waiting for nested object')
        reject(new Error('Timeout waiting for nested object'))
      } else {
        // Nested object not yet created, continue waiting
        setTimeout(checkObject, interval)
      }
    }
    checkObject()
  })
}

const checkIfEventisEnabled = (event) => {
  return new Promise((resolve, reject) => { if (!disabledCallbacks.includes(event)) { resolve() } })
}

module.exports = {
  triggerWebhook,
  sendErrorResponse,
  waitForNestedObject,
  checkIfEventisEnabled
}
