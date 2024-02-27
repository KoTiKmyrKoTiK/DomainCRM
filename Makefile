# PG connection
POSTGRES_HOST     ?= postgres_db
POSTGRES_PORT     ?= 5432
POSTGRES_USER     ?= suchimauz
POSTGRES_DB       ?= testbox
POSTGRES_PASSWORD ?= testbox

SPRING_DDL_AUTO ?= update
SPRING_SHOW_SQL ?= true

LOGGING_LEVEL      ?= DEBUG
LOGGING_DESCRIPTOR ?= TRACE

ADMIN_EMAIL    = admin@test.test
ADMIN_USERNAME = admin
ADMIN_PASSWORD = admin

.EXPORT_ALL_VARIABLES:
.PHONY: test build

run:
	docker-compose up --build webserver

run-fg:
	docker-compose up --build -d webserver

stop-fg:
	docker-compose down webserver

up:
	docker-compose up -d postgres_db

down:
	docker-compose down postgres_db

psql:
	docker exec -it domain-crm--postgres psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}