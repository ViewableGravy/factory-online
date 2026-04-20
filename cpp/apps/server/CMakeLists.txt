add_executable(factory_server)

target_sources(
  factory_server
  PRIVATE
    src/main.cpp
)

target_compile_features(factory_server PRIVATE cxx_std_20)