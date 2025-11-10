
.PHONY: build
build:
	./gradlew --rerun-tasks \
		clean build \
		koverLog koverXmlReport \
		:kotlinx-schema-gradle-plugin:publishToMavenLocal

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
