-- Card 1 (Account ID: 1, SAVINGS)
-- Raw values for response: cardNumber="1111 1670 9381 2745", cvv="592", expiry="01/2031"
INSERT INTO cards
(id, account_id, card_number, cvv, last_digits, expiry_date, card_limit, status, created_at, updated_at)
VALUES
(
  1,
  1,
  '0d8ab8506a61ef830d580c67ea51d8d2388e23148ef8a0f937ca342ad26aba9e',
  '793733573a1dfd14a2e889a11b2ad7b6981de29df813863b528dc1ae99416eeb',
  '2745',
  '01/2031',
  50000.0,
  'INACTIVE',
  NOW(),
  NOW()
),
-- Card 2 (Account ID: 2, SAVINGS)
-- Raw values for response: cardNumber="1111 4208 4622 6035", cvv="336", expiry="01/2031"
(
  2,
  2,
  '5b7b1567c9ee26c87f424c9ac6e90d4029b417c6523744094b25e26f2497548f',
  'eaa0689a095d4394a05fb51b84b0175a47f68221261377e4829444cbfcae23ca',
  '6035',
  '01/2031',
  50000.0,
  'INACTIVE',
  NOW(),
  NOW()
),
-- Card 3 (Account ID: 3, CURRENT)
-- Raw values for response: cardNumber="1111 1661 3964 7775", cvv="620", expiry="01/2031"
(
  3,
  3,
  '875ddd3fd64645ae839b4122b0a5708e146bf242bc879020d90be2bef6bbf5ab',
  '524148f24802f8c68974c2e1ecc8b8f47d0d60b7a0d1948951c050a25b5a8e59',
  '7775',
  '01/2031',
  200000.0,
  'INACTIVE',
  NOW(),
  NOW()
);

SELECT setval(
  pg_get_serial_sequence('cards', 'id'),
  (SELECT MAX(id) FROM cards)
);
