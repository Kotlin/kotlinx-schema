
.PHONY: build
build:clean
	@echo "🔨 Coverage reports..."
	@./gradlew \
		build \
		koverLog koverXmlReport koverHtmlReport
	@echo "✅ Build complete!"

.PHONY: test
test:
	@echo "🧪 Running tests..."
	@./gradlew check --rerun-tasks
	@echo "✅ Tests complete!"

.PHONY: apidocs
apidocs:
	@echo "📚 Generating API documentation..."
	@rm -rf docs/public/apidocs && \
	./gradlew clean :docs:dokkaGenerate
	@echo "✅ API docs generated!"

.PHONY: clean
clean:
	@echo "🧹 Cleaning build artifacts..."
	@./gradlew --stop
	@rm -rf .gradle/configuration-cache
	@rm -rf buildSrc/.gradle/configuration-cache
	@rm -rf **/kotlin-js-store && ./gradlew clean
	@(cd gradle-plugin-integration-tests && ./gradlew --stop && rm -rf .gradle/configuration-cache buildSrc/.gradle/configuration-cache kotlin-js-store && ./gradlew clean)
	@echo "✅ Clean complete!"

.PHONY: lint
lint:
	@echo "🕵️‍♀️ Inspecting code..."
	@./gradlew detekt --rerun-tasks
	@echo "✅ Code inspection complete!"

.PHONY: publish
publish:
	@echo "📦 Publishing to local Maven repository..."
	@./gradlew publishToMavenLocal
	@echo "✅ Published to ~/.m2/repository!"

.PHONY: sync
sync:
	git submodule update --init --recursive --depth=1

.PHONY: integration-test
integration-test:
	@echo "🧪 Running tests..."
	@rm -rf **/kotlin-js-store
	@./gradlew build publishToMavenLocal --rerun-tasks
	@echo "✅ Build complete!"

	@echo "🧪🧩 Running integration tests..."

	@#	-Pversion=1-SNAPSHOT
	@echo "🧪🧩 Starting Integration tests..."
	@rm -rf gradle-plugin-integration-tests/**/build gradle-plugin-integration-tests/kotlin-js-store
	@(cd gradle-plugin-integration-tests && ./gradlew clean build --no-daemon --stacktrace --no-configuration-cache)
	@echo "✅ Integration tests complete!"
