
.PHONY: build
build:
	./gradlew  build koverLog koverXmlReport --rerun-tasks

.PHONY: test
test:
	./gradlew test --rerun-tasks

.PHONY: clean
clean:
	./gradlew clean
