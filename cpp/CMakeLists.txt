cmake_minimum_required(VERSION 3.22)

project(
  factory_online
  VERSION 0.1.0
  DESCRIPTION "Factory Online native server bootstrap"
  LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_subdirectory(apps/server)