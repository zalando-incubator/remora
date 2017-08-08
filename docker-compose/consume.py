import sys
from kafka import KafkaConsumer
import logging as log

log.basicConfig(level=log.INFO)


def consume(varargs):
    ip = varargs[0]
    print(ip + ":9092")
    consumer = KafkaConsumer(
        group_id='test-0-consumer',
        bootstrap_servers=ip + ":9092")
    consumer.subscribe(['test-0'])

    for message in consumer:
        print (message)


if __name__ == "__main__":
    consume(sys.argv[1:])
