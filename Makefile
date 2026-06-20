# TheatriaOnboarding build
#
#   make build    Compile and package the plugin -> target/TheatriaOnboarding-1.0.0.jar
#   make clean    Remove build output
#   make help     Show available targets
#
# Requires Maven and JDK 25 (Minecraft 26.1.2 needs Java 25). The build resolves
# paper-api / VaultAPI from the PaperMC and JitPack repos, so it needs network
# access to those.

MVN      ?= mvn
MVNFLAGS ?= -B
JAR      := target/TheatriaOnboarding-1.0.0.jar

.DEFAULT_GOAL := build
.PHONY: build clean help

build: ## Compile and package the plugin jar
	$(MVN) $(MVNFLAGS) clean package
	@echo "Built $(JAR)"

clean: ## Remove build output
	$(MVN) $(MVNFLAGS) clean

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  make %-8s %s\n", $$1, $$2}'
