-- Manual payments without a transfer reference must use NULL. Empty strings
-- are treated as real provider IDs by the unique provider/payment index.
UPDATE payments
SET provider_payment_id = NULL
WHERE provider = 'MANUAL'
  AND btrim(provider_payment_id) = '';
