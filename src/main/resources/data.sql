-- Dev seed (Flyway V2) - deterministic UUIDs for Postman/demo
-- H2-compatible: use MERGE ... KEY(...) to be idempotent

-- Insert two users (explicit version = 0 to satisfy @Version)
MERGE INTO users (id, name, email, oauth_id, created_at, version)
KEY(id)
VALUES ('f3817bed-3d0a-9529-02ae-394d1ca41c4a', 'Payer', 'payer@dev', 'dev:sub:1', CURRENT_TIMESTAMP, 0);

MERGE INTO users (id, name, email, oauth_id, created_at, version)
KEY(id)
VALUES ('ef038db3-a2c4-43eb-a062-51ac343f58da', 'Member', 'member@dev', 'dev:sub:2', CURRENT_TIMESTAMP, 0);

-- Insert roles into user_roles (ElementCollection table)
MERGE INTO user_roles (user_id, role)
KEY(user_id, role)
VALUES ('f3817bed-3d0a-9529-02ae-394d1ca41c4a', 'USER');

MERGE INTO user_roles (user_id, role)
KEY(user_id, role)
VALUES ('ef038db3-a2c4-43eb-a062-51ac343f58da', 'USER');

-- Create a dev group (fixed id)
MERGE INTO groups (id, name, created_by, created_at, version)
KEY(id)
VALUES ('2e008609-0fbc-42a7-a197-d94ae4268034', 'dev-group', 'f3817bed-3d0a-9529-02ae-394d1ca41c4a', CURRENT_TIMESTAMP, 0);

-- Group members
MERGE INTO group_members (id, group_id, user_id, status, created_by, created_at, joined_at, version)
KEY(id)
VALUES ('11111111-1111-1111-1111-111111111111',
        '2e008609-0fbc-42a7-a197-d94ae4268034',
        'f3817bed-3d0a-9529-02ae-394d1ca41c4a',
        'ACTIVE',
        'f3817bed-3d0a-9529-02ae-394d1ca41c4a',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0);

MERGE INTO group_members (id, group_id, user_id, status, created_by, created_at, joined_at, version)
KEY(id)
VALUES ('22222222-2222-2222-2222-222222222222',
        '2e008609-0fbc-42a7-a197-d94ae4268034',
        'ef038db3-a2c4-43eb-a062-51ac343f58da',
        'ACTIVE',
        'f3817bed-3d0a-9529-02ae-394d1ca41c4a',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0);

-- Optional: initial zero balances so queries find them immediately
MERGE INTO balances (id, group_id, user_id, balance, created_by, created_at, version)
KEY(id)
VALUES ('33333333-3333-3333-3333-333333333333','2e008609-0fbc-42a7-a197-d94ae4268034','f3817bed-3d0a-9529-02ae-394d1ca41c4a', 0.00, 'f3817bed-3d0a-9529-02ae-394d1ca41c4a', CURRENT_TIMESTAMP, 0);

MERGE INTO balances (id, group_id, user_id, balance, created_by, created_at, version)
KEY(id)
VALUES ('44444444-4444-4444-4444-444444444444','2e008609-0fbc-42a7-a197-d94ae4268034','ef038db3-a2c4-43eb-a062-51ac343f58da', 0.00, 'f3817bed-3d0a-9529-02ae-394d1ca41c4a', CURRENT_TIMESTAMP, 0);