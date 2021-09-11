@echo off
psql -f setup.sql "dbname=postgres user=postgres password=admin host=localhost"
psql -f createTables.sql "dbname=scaladb user=scala password=scala host=localhost"
