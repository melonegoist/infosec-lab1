\set ON_ERROR_STOP on

SET log_statement = 'none';
SET log_min_error_statement = 'panic';

\getenv owner_user DB_OWNER_USERNAME
\getenv owner_password DB_OWNER_PASSWORD
\getenv app_user DB_APP_USERNAME
\getenv app_password DB_APP_PASSWORD

SELECT NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'owner_user') AS create_owner \gset
\if :create_owner
CREATE ROLE :"owner_user";
\endif
ALTER ROLE :"owner_user" WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT
    CONNECTION LIMIT 3 PASSWORD :'owner_password';

SELECT NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lab1_rw') AS create_rw \gset
\if :create_rw
CREATE ROLE lab1_rw;
\endif
ALTER ROLE lab1_rw WITH NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;

SELECT NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app_user') AS create_app \gset
\if :create_app
CREATE ROLE :"app_user";
\endif
ALTER ROLE :"app_user" WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS INHERIT
    CONNECTION LIMIT 10 PASSWORD :'app_password';
GRANT lab1_rw TO :"app_user";
ALTER ROLE :"app_user" SET statement_timeout = '5s';
ALTER ROLE :"app_user" SET lock_timeout = '3s';
ALTER ROLE :"app_user" SET idle_in_transaction_session_timeout = '15s';

\set db lab1
\ir database.sql
\set db lab1_test
\ir database.sql
