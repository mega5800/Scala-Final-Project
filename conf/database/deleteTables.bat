@echo off
psql -f deleteTables.sql "dbname=scaladb user=postgres password=admin host=localhost"