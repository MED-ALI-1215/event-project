#!/bin/bash
echo "=== TEST PIPELINE CI/CD ==="

# 1. Clean
echo "1. Cleaning..."
./mvnw clean

# 2. Compile
echo "2. Compiling..."
./mvnw compile -DskipTests

# 3. Tests
echo "3. Running tests..."
./mvnw test

# 4. Package
echo "4. Packaging..."
./mvnw package -DskipTests

# 5. Docker build
echo "5. Building Docker image..."
docker build -t events-project:test .

# 6. Docker run test
echo "6. Testing Docker container..."
docker run -d -p 8081:8080 --name events-test events-project:test
sleep 10
curl -s http://localhost:8081/events/actuator/health | grep -o '"status":"UP"' && echo "✅ Docker container OK"

# 7. Cleanup
echo "7. Cleaning up..."
docker stop events-test
docker rm events-test

echo "=== PIPELINE TEST COMPLETE ==="
