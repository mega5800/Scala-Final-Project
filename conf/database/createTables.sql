-- Create database tables
CREATE TABLE IF NOT EXISTS scaladb.public.users
(
    id SERIAL PRIMARY KEY,
    username varchar(20) NOT NULL,
    password varchar(200) NOT NULL,
    email text NOT NULL
);

CREATE TABLE IF NOT EXISTS scaladb.public.costs (
    id SERIAL PRIMARY KEY,
    user_id int4 REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    purchase_date DATE NOT NULL,
    category TEXT NOT NULL,
    cost_price NUMERIC NOT NULL
);

-- Grant all privileges to user scala
GRANT ALL ON SCHEMA public TO scala;
GRANT ALL ON SCHEMA public TO public;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO scala;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO scala;
