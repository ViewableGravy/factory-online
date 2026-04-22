JAVA := java
JAVAC := javac

BUILD_DIR := build/classes
SERVER_SRC_DIR := apps/server/src/main/java
SERVER_MAIN := com.factoryonline.server.Main

.PHONY: run build clean

run: build
	$(JAVA) -cp $(BUILD_DIR) $(SERVER_MAIN)

build:
	@mkdir -p $(BUILD_DIR)
	$(JAVAC) -d $(BUILD_DIR) $(shell find $(SERVER_SRC_DIR) -name '*.java')

clean:
	rm -rf build