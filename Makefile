
.PHONY: build
build:clean
	@echo "🔨 Building project with coverage reports..."
	@./gradlew --rerun-tasks \
		build \
		koverLog koverXmlReport
	@echo "✅ Build complete!"

.PHONY: test
test:
	@echo "🧪 Running tests..."
	@./gradlew test wasmJsTest --rerun-tasks
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
	@./gradlew clean && rm -rf kotlin-js-store
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

.PHONY: q
q:
	@echo "🔨 Building project with coverage reports..."
	@./gradlew --debug \
		build
	@echo "✅ Build complete!"