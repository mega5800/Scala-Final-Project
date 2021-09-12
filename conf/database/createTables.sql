-- Create database tables
CREATE TABLE IF NOT EXISTS scaladb.public.users
(
    id SERIAL PRIMARY KEY,
    username varchar(20) UNIQUE NOT NULL,
    password varchar(200) NOT NULL,
    email text UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS scaladb.public.costs (
    id SERIAL PRIMARY KEY,
    user_id int4 NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    purchase_date DATE NOT NULL,
    category TEXT NOT NULL,
    cost_price NUMERIC NOT NULL
);

CREATE TABLE IF NOT EXISTS scaladb.public.password_requests (
  id SERIAL PRIMARY KEY,
  user_id int4 NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  password_reset_token TEXT UNIQUE NOT NULL,
  password_reset_expiration TIMESTAMP NOT NULL
);

-- Grant all privileges to user scala
GRANT ALL ON SCHEMA public TO scala;
GRANT ALL ON SCHEMA public TO public;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO scala;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO scala;
