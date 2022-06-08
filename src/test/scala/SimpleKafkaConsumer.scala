import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer

import java.time.Duration
import java.util.Properties

class SimpleKafkaConsumer[K,V](consumerProps : Properties,
                               topic : String,
                               keyDeserializer: Deserializer[K],
                               valueDeserializer: Deserializer[V],
                               function : ConsumerRecords[K, V] => Unit,
                               poll : Long = 2000) {

  private var running = false

  private val consumer = new KafkaConsumer[K, V](consumerProps, keyDeserializer, valueDeserializer)

  private val thread = new Thread {
    import scala.jdk.CollectionConverters._

    override def run(): Unit = {
      consumer.subscribe(List(topic).asJava)
      consumer.partitionsFor(topic)

      while (running) {
        val record: ConsumerRecords[K, V] = consumer.poll(Duration.ofMillis(poll))
        function(record)
      }
    }
  }

  def start(): Unit = {
    if(!running) {
      running = true
      thread.start()
    }
  }

  def stop(): Unit = {
    if(running) {
      running = false
      thread.join()
      consumer.close()
    }
  }
}
