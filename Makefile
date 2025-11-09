
.PHONY: build
build:
	./gradlew  build koverLog koverXmlReport --rerun-tasks

.PHONY: test
test:
	./gradlew test --rerun-tasks

.PHONY: apidocs
apidocs:
	rm -rf docs/public/apidocs && \
	./gradlew clean :docs:dokkaGenerate

.PHONY: clean
clean:
	./gradlew clean
