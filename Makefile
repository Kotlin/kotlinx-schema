
.PHONY: build
build:clean
	@echo "🔨 Building project with coverage reports..."
	@(cd gradle-plugin-integration-tests && ./gradlew allTest --rerun-tasks)
	@./gradlew \
		build \
		koverLog koverXmlReport
	@echo "✅ Build complete!"

.PHONY: test
test:
	@echo "🧪 Running tests..."
	@(cd gradle-plugin-integration-tests && ./gradlew allTest --rerun-tasks)
	@./gradlew allTest --rerun-tasks
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
	@rm -rf kotlin-js-store && ./gradlew clean
	@(cd gradle-plugin-integration-tests && rm -rf kotlin-js-store && ./gradlew clean)
	@echo "✅ Clean complete!"

.PHONY: lint
lint:
	@echo "🕵️‍♀️ Inspecting code..."
	@./gradlew detekt
	@echo "✅ Code inspection complete!"

.PHONY: publish
publish:
	@echo "📦 Publishing to local Maven repository..."
	@./gradlew publishToMavenLocal
	@echo "✅ Published to ~/.m2/repository!"