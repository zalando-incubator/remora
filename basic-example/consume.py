import sys
from kafka import KafkaConsumer
import logging as log

log.basicConfig(level=log.INFO)


def consume():
    consumer = KafkaConsumer(
        group_id='test-0-consumer',
        bootstrap_servers="localhost:9092")
    consumer.subscribe(['test-0'])
    consumer.subscription()

    for message in consumer:
        print (message)


if __name__ == "__main__":
    consume()
