# Build helpers for the TheatriaOnboarding Paper plugin (Maven).
#
# No shading is needed: both dependencies (paper-api, VaultAPI) are 'provided'
# scope -- supplied by the server at runtime -- and every plugin integration
# (Essentials/Lands/Rankup/LuckPerms/TheatriaSessions) is reflective with no
# compile-time dependency. `mvn package` therefore yields a complete, shippable
# plugin jar with nothing to bundle or relocate:
#
#     target/TheatriaOnboarding-<version>.jar
#
# If a real (compile/runtime-scope) dependency is ever added, introduce the
# maven-shade-plugin then; until that happens it would relocate nothing.

MVN ?= mvn

.PHONY: build
build:
	$(MVN) -q clean package

.PHONY: package
package: build

.PHONY: compile
compile:
	$(MVN) -q clean compile

.PHONY: clean
clean:
	$(MVN) -q clean
