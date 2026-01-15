INSERT INTO usercred
(id, username, email, password, phone, role, created_at, updated_at)
VALUES
(
	1,
  'User1',
  'user1@example.com',
  '$2a$12$VB1l0WCqjB63eoEP/Ojg9OK37K2mxHTo0v2ncDrg5wIt6z5dZ7VTq',
  '1234567890',
  'USER',
  NOW(),
  NOW()
),
(
	2,
  'User2',
  'user2@example.com',
  '$2a$12$wTbzuM.TIydAfVshBfOscOdj4RYgWERWSDCJ.xzHvK.rzz2GYNoPe',
  '1111111111',
  'USER',
  NOW(),
  NOW()
),
(
	3,
  'TejasAdmin',
  'tejasadmin@example.com',
  '$2a$12$Q6QOQQ9aTD1KaCpaM897aeL7r4PJOGhYqa/impLCzNblctgKJoHPK',
  '9999999999',
  'ADMIN',
  NOW(),
  NOW()
);

SELECT setval(
  pg_get_serial_sequence('usercred', 'id'),
  (SELECT MAX(id) FROM usercred)
);



