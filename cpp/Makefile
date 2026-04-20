CXX := g++
CXXFLAGS := -std=c++20 -Wall -Wextra -pedantic

BUILD_DIR := build/manual
SERVER_SRC := apps/server/src/main.cpp
SERVER_BIN := $(BUILD_DIR)/factory_server

.PHONY: run build clean

run: $(SERVER_BIN)
	./$(SERVER_BIN)

build: $(SERVER_BIN)

$(SERVER_BIN): $(SERVER_SRC)
	@mkdir -p $(BUILD_DIR)
	$(CXX) $(CXXFLAGS) $(SERVER_SRC) -o $(SERVER_BIN)

clean:
	rm -rf build/manual