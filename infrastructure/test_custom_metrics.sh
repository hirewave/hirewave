./build.sh
./stop.sh && ./start.sh
docker compose --profile monitoring up -d

echo "Waiting for Spring Boot to start..."
sleep 20

# CLIENTS
# create dummy client
curl -X POST http://localhost:8080/api/clients -H "Content-Type: application/json" -d '{"name":"Test Client","email":"client1@test.com"}'

# trigger error by creating another client with same email
curl -X POST http://localhost:8080/api/clients -H "Content-Type: application/json" -d '{"name":"Test Client 2","email":"client1@test.com"}'

# lookup random client
curl http://localhost:8080/api/clients/smth


# FRELLANCERS
# create dummy freelancer
curl -X POST http://localhost:8080/api/freelancers -H "Content-Type: application/json" -d '{"name":"Test Freelancer","email":"free1@test.com","skills":["Java", "Spring"],"hourlyRate":50}'

# lookup random freelancer
curl http://localhost:8080/api/freelancers/smth

# RATINGS
# create a freelancer for ratings and get its id
FREE_ID=$(curl -s -X POST http://localhost:8080/api/freelancers -H "Content-Type: application/json" -d '{"name":"Rating Test Freelancer","email":"rater@test.com","skills":["Python"],"hourlyRate":40}' | grep -o '"id":"[^"]*"' | head -n 1 | cut -d'"' -f4)

# submit ratings
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":5}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":5}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":5}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":4}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":4}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":3}' # > /dev/null
curl -s -X POST http://localhost:8080/api/freelancers/$FREE_ID/ratings -H "Content-Type: application/json" -d '{"rating":1}' # > /dev/null

echo "\n\nChecking for app_freelancer_rating metrics:\n"
# verify metrics
curl -s http://localhost:8080/actuator/prometheus | grep "app_"
