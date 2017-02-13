all: request.png overview.png build

.PHONY: serve, watch 

request.png: request.dot
	dot -T png -o request.png request.dot

overview.png: overview.dot
	dot -T png -o overview.png overview.dot

build: request.png overview.png
	sphinx-build ./ docsbin/

serve: build
	cd docsbin/ && python -m SimpleHTTPServer

watch:
	find ./ | entr -d -r sh -c "make build && make serve"

