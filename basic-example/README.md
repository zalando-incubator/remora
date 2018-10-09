
### To Test Remora

## Run docker compose 

Start up inside this folder using `docker-compose up`

Download [Apache Kafka 2](http://kafka.apache.org/)

## Produce and Consume via Terminal

# consume on terminal 1

`./bin/kafka-console-consumer.sh --bootstrap-server  localhost:9092 --topic test-0
> Wait for input`

# Produce on terminal 2

`./kafka-console-producer.sh --broker-list localhost:9092 --topic test-0
> Send some input
`

## Produce and Consume via Python scripts

# Install Dependencies
`pip install -r requirements.txt`

# Consume via python script in Terminal 1

Let the script run 
`python consumer.py` 

And see output once produced e.g.
`ConsumerRecord(topic=u'test-0', partition=0, offset=8, timestamp=1539096976879, timestamp_type=0, key=None, value='bar', checksum=868036758, serialized_key_size=-1, serialized_value_size=3)`

# Produce via python script in Terminal 2

`python produce.py <NUMBER OF MESSAGES>`

