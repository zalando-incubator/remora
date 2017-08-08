import sys
from kafka import KafkaProducer


def produce(varargs):
    ip = varargs[0]
    producer = KafkaProducer(bootstrap_servers=ip + ":9092")
    for _ in range(10):
        print("Producing message to topic test-0")
        producer.send('test-0', b'some_message_bytes')

if __name__ == "__main__":
    produce(sys.argv[1:])
