.PHONY: help mocks run test k6 docker-build docker-run down

IMAGE := similar-products
NETWORK := backend_default

help:
	@echo "make mocks         levanta los mocks, influxdb y grafana"
	@echo "make run           arranca la app en local (puerto 5000)"
	@echo "make test          ejecuta los tests"
	@echo "make k6            lanza el test de carga (la app debe estar en el 5000)"
	@echo "make docker-build  construye la imagen de la app"
	@echo "make docker-run    arranca la app en un contenedor (puerto 5000)"
	@echo "make down          para y elimina los contenedores"

mocks:
	docker compose up -d simulado influxdb grafana

run:
	./mvnw spring-boot:run

test:
	./mvnw -B test

k6:
	docker compose run --rm k6 run scripts/test.js

docker-build:
	docker build -t $(IMAGE) .

# La app corre en la red de compose y llama a los mocks por su nombre de servicio.
docker-run: mocks docker-build
	docker run --rm --name $(IMAGE) --network $(NETWORK) -p 5000:5000 \
		-e PRODUCTS_API_BASE_URL=http://simulado:80 $(IMAGE)

down:
	-docker rm -f $(IMAGE)
	docker compose down
