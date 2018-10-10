import sys
from kafka import KafkaProducer


def produce(varargs):
    numberrange = int(varargs[0])
    print numberrange
    producer = KafkaProducer(bootstrap_servers="localhost:9092")
    for _ in range(numberrange):
        print("Producing message to topic test-0")
        producer.send('test-0', b'some_message_bytes')

if __name__ == "__main__":
    produce(sys.argv[1:])
