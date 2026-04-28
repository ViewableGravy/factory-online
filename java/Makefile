GRADLEW := ./gradlew
GRADLE_ARGS := --console=plain

.PHONY: run run-client build clean verify-delay

run:
	$(GRADLEW) $(GRADLE_ARGS) runServer

run-client:
	$(GRADLEW) $(GRADLE_ARGS) runClient

build:
	$(GRADLEW) $(GRADLE_ARGS) classes

clean:
	$(GRADLEW) $(GRADLE_ARGS) clean

verify-delay: build
	./scripts/verify-delay-regression.sh