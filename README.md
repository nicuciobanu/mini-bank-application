## Technologies:
- Akka Typed,
- Cassandra,
- Akka Http,
- Cats validation

### Start cassandra docker container
- docker-compose up

### Check for cassandra container
- docker ps

### Connect to cassandra container
- docker exec -it mini-bank-app-cassandra-1 cqlsh

### Check the events table
- select * from akka.messages;

### Clear the events table
- truncate akka.messages;

### Create bank-account request

curl --location --request POST 'localhost:8080/bank/' \
--header 'Content-Type: application/json' \
--data-raw '{
    "user": "Nicolae",
    "currency": "EUR",
    "balance": 250000
}'

### Get bank-account request

curl --location --request GET 'localhost:8080/bank/80561948-4176-4435-9e4d-bc6bd45775f8'

### Update bank-account request

curl --location --request PUT 'localhost:8080/bank/80561948-4176-4435-9e4d-bc6bd45775f8' \
--header 'Content-Type: application/json' \
--data-raw '{
    "currency": "EUR",
    "amount": 250000
}'

###### Sources of inspiration: https://blog.rockthejvm.com/akka-cassandra-project/
