
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
	@./gradlew kotlinWasmUpgradePackageLock build --rerun-tasks
	@echo "✅ Tests complete!"

.PHONY: scan
scan:
	@echo "🔎 Running build with scan..."
	@./gradlew clean kotlinWasmUpgradePackageLock kotlinUpgradePackageLock build --scan --rerun-tasks
	@echo "✅ Build with scan is complete!"

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
	@rm -rf **/kotlin-js-store **/build **/.gradle/configuration-cache
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
integration-test:clean publish
	@#./gradlew build publishToMavenLocal -Pversion=1-SNAPSHOT --rerun-tasks
	@#./gradlew assemble check publishToMavenLocal --rerun-tasks
	@echo "🧪🧩 Starting Integration tests..."
	@#(cd gradle-plugin-integration-tests && ./gradlew clean kotlinUpgradePackageLock build -PkotlinxSchemaVersion=1-SNAPSHOT --no-daemon --stacktrace --no-configuration-cache)
	@(cd gradle-plugin-integration-tests && ./gradlew clean kotlinUpgradePackageLock build --no-daemon --stacktrace --no-configuration-cache)
	@echo "✅ Integration tests complete!"
