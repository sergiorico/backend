IMAGES = request.svg overview.svg frontend.svg

images: 
	mkdir images
	
images/%.svg : %.dot ; dot -T svg -o $@ $<

build: images $(addprefix images/,$(IMAGES))
	sphinx-build ./ docsbin/

serve: build
	cd docsbin/ && python -m SimpleHTTPServer

watch:
	find . -type d \( -path ./docsbin -o -path ./images \) \
	 	   -prune -o -name '*' | entr -r sh -c \
			"make build && make serve"

