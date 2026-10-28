.PHONY: help up down logs k6 test test-docker run mocks

help:
	@echo "make up           levanta la app (puerto 5000), los mocks, influxdb y grafana con Docker"
	@echo "make k6           lanza el test de carga; resultados en http://localhost:3000/d/Le2Ku9NMk/k6-performance-test"
	@echo "make logs         muestra los logs de la app"
	@echo "make down         para y elimina los contenedores"
	@echo "make test-docker  ejecuta los tests dentro de Docker (no necesita Java)"
	@echo "make test         ejecuta los tests en local (JDK 21+)"
	@echo "make mocks        levanta solo los mocks, influxdb y grafana"
	@echo "make run          arranca la app en local contra los mocks (JDK 21+)"

up:
	docker compose up -d --build app simulado influxdb grafana
	@echo "App en http://localhost:5000/product/1/similar"

down:
	docker compose down

logs:
	docker compose logs -f app

k6:
	docker compose run --rm k6 run scripts/test.js

test-docker:
	docker build --target test .

test:
	./mvnw -B test

mocks:
	docker compose up -d simulado influxdb grafana

run:
	./mvnw spring-boot:run
