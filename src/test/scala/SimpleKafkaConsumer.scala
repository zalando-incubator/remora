import java.util.Properties

import com.fasterxml.jackson.databind.KeyDeserializer
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer
import net.manub.embeddedkafka.Codecs.stringDeserializer
import net.manub.embeddedkafka.ConsumerExtensions._

class SimpleKafkaConsumer[K,V](consumerProps : Properties,
                               topic : String,
                               keyDeserializer: Deserializer[K],
                               valueDeserializer: Deserializer[V],
                               function : ConsumerRecords[K, V] => Unit,
                               poll : Long = 2000) {

  private var running = false

  private val consumer = new KafkaConsumer[K, V](consumerProps, keyDeserializer, valueDeserializer)


  private val thread = new Thread {
    import scala.collection.JavaConverters._

    override def run: Unit = {
      consumer.subscribe(List(topic).asJava)
      consumer.partitionsFor(topic)

      while (running) {
        val record: ConsumerRecords[K, V] = consumer.poll(poll)
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
