JAVA := java
JAVAC := javac

BUILD_DIR := build/classes
SRC_DIRS := apps libs
JAVA_SOURCES := $(shell find $(SRC_DIRS) -path '*/src/main/java/*' -name '*.java')
SERVER_MAIN := com.factoryonline.server.Main

.PHONY: run build clean verify-delay

run: build
	$(JAVA) -cp $(BUILD_DIR) $(SERVER_MAIN)

build:
	@mkdir -p $(BUILD_DIR)
	$(JAVAC) -d $(BUILD_DIR) $(JAVA_SOURCES)

clean:
	rm -rf build

verify-delay: build
	./scripts/verify-delay-regression.sh