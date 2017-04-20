import sys
from kafka import KafkProducer

def produce(varargs):
    ip =varargs[0]
    producer = KafkProducer(bootstrap_servers=ip+":9092")
    for _ in range(10):
        producer.send('test-0', b'some_message_bytes')

if __name__ == "__main__":
    print(produce(sys.argv[1:]))