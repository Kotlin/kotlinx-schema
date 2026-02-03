.SHELLFLAGS := -e -o pipefail -c

.PHONY: all
all:clean build integration-test examples

.PHONY: build
build:clean
	@echo "🔨 Coverage reports..."
	@./gradlew --rerun-tasks kotlinUpgradePackageLock \
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
	@echo "📦 Publishing to project repository (build/project-repo)..."
	@rm -rf build/project-repo
	@./gradlew publishAllPublicationsToProjectRepository -Pversion=1-SNAPSHOT --rerun-tasks
	@echo "✅ Published to build/project-repo!"

.PHONY: sync
sync:
	git submodule update --init --recursive --depth=1

.PHONY: integration-test
integration-test:clean publish
	@echo "🧪🧩 Starting Integration tests..."
	@(cd gradle-plugin-integration-tests && ./gradlew clean kotlinUpgradePackageLock build -PkotlinxSchemaVersion=1-SNAPSHOT --no-daemon --stacktrace)
	@echo "✅ Integration tests complete!"

.PHONY: examples
examples:
	@echo "Running examples..."
	@(cd examples/gradle-google-ksp && ./gradlew clean build --no-daemon --rerun-tasks)
	@(cd examples/maven-ksp && mvn clean package)
	@echo "✅ Examples complete!"
