INSERT INTO clients (id, name) VALUES
    ('CLIENT-001', 'Alice Demo'),
    ('CLIENT-002', 'Bob Demo');

INSERT INTO balances (client_id, currency, amount) VALUES
    ('CLIENT-001', 'USD', 10000.0000),
    ('CLIENT-001', 'EUR',  8000.0000),
    ('CLIENT-002', 'GBP',  5000.0000);