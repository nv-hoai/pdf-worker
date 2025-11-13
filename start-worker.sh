#!/bin/bash
# PDF Worker Startup Script for Linux/Mac

SERVER_HOST=${1:-localhost}
SERVER_PORT=${2:-7777}
JAR_FILE="pdf-worker-1.0.0-jar-with-dependencies.jar"

echo "========================================"
echo "PDF Conversion Worker"
echo "========================================"
echo "Server: $SERVER_HOST:$SERVER_PORT"
echo "Mode: TCP File Transfer"
echo "========================================"
echo

java -Xmx1024m -jar target/$JAR_FILE $SERVER_HOST $SERVER_PORT
