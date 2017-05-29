[![Build Status](https://travis-ci.org/zalando-incubator/remora.svg?branch=master)](https://travis-ci.org/zalando-incubator/remora)

# Remora
A simple API application, Built using akka and scala, in front of kafka to give offset information to act as a Kafka lag checker

## Background

Due to using akka streams we could not get the current offset from the api. Instead we used linkedin [burrow](https://github.com/linkedin/Burrow)
but contains some [performance issues](https://github.com/linkedin/Burrow/wiki/Known-Issues)

All we did was wrap and hack the [kafka consumer group command](https://github.com/apache/kafka/blob/0.10.0/core/src/main/scala/kafka/admin/ConsumerGroupCommand.scala) in akka http.

## Prerequisites

* Kafka 0.10.2.1 (for 0.10.0.1 please see the [0.1.0 release](https://github.com/zalando-incubator/remora/releases/tag/v0.1.0) as the api has changed since)
* 2.11.8
* Store offsets in kafka

## Running

Images for all versions are available on [Docker Hub](https://hub.docker.com/r/zalandoremora/remora/tags/)

They can be used as follows:

```
docker run -it --rm -p 9000:9000 -e KAFKA_ENDPOINT=127.0.0.1:9092 zalandoremora/remora:0.2.0
```

For further examples see the [docker-compose.yml](docker-compose/docker-compose.yml)

## Build

* `sbt package`
* `sbt docker:publishLocal`
* `sbt docker:publish -Ddocker.repo=<DOCKER REPO>`

## Server Arguments

* SERVER_PORT - default `9000`
* KAFKA_ENDPOINT - default `localhost:9092`
* ACTOR_TIMEOUT - default `60 seconds`
* AKKA_HTTP_SERVER_REQUEST_TIMEOUT - `default 60 seconds`
* AKKA_HTTP_SERVER_IDLE_TIMEOUT - `default 60 seconds`

## Show Active Consumers
`http://localhost:9000/consumers`

which gives 

```json
["consumer-1", "consumer-2", "consumer-3"]
```

## Show specific consumer info
`curl http://localhost:9000/consumers/<ConsumerGroupId>`

State can be "Empty", "Dead", "Stable", "PreparingRebalance", "AwaitingSync"

```json
{  
   "state":"Empty",
   "partition_assignment":[  
      {  
         "group":"console-consumer-20891",
         "coordinator":{  
            "id":0,
            "id_string":"0",
            "host":"foo.company.com",
            "port":9092
         },
         "topic":"products-in",
         "partition":1,
         "offset":3,
         "lag":0,
         "consumer_id":"-",
         "host":"-",
         "client_id":"-",
         "log_end_offset":3
      },
      {  
         "group":"console-consumer-20891",
         "coordinator":{  
            "id":0,
            "id_string":"0",
            "host":"foo.company.com",
            "port":9092
         },
         "topic":"products-in",
         "partition":0,
         "offset":3,
         "lag":0,
         "consumer_id":"consumer-1-7baba9b9-0ec3-4241-9433-f36255dd4708",
         "host":"/xx.xxx.xxx.xxx",
         "client_id":"consumer-1",
         "log_end_offset":3
      }
   ]
}
```

## Health

`curl http://localhost:9000/health` returns `OK`

## Metrics

`curl http://localhost:9000/metrics`

### Contributing

We are happy to accept contributions. First, take a look at our [contributing guidelines](CONTRIBUTING.md).

You can see our current status in [this task board](https://github.com/zalando-incubator/remora/projects/1).


### TODO

Please check the [Issues Page](https://github.com/zalando-incubator/remora/issues)
for contribution ideas.

### Contact

Feel free to contact one of the [maintainers](MAINTAINERS).

### License

MIT