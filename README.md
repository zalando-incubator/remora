# Remora
A simple API application, Built using akka and scala, in front of kafka to give offset information to act as a Kafka lag checker

## Background

Due to using akka streams we could not get the current offset from the api. Instead we used linkedin [burrow](https://github.com/linkedin/Burrow)
but contains some [performance issues](https://github.com/linkedin/Burrow/wiki/Known-Issues)

All we did was wrap and hack the [kafka consumer group command](https://github.com/apache/kafka/blob/0.10.0/core/src/main/scala/kafka/admin/ConsumerGroupCommand.scala) in akka http.

## Prerequisites

* Kafka 0.10.0.1
* 2.11.8
* Store offsets in kafka

## Build

* `sbt package`
* `sbt docker:publishLocal`
* `sbt docker:publish -Ddocker.repo=<DOCKER REPO>`

## Server Arguments

* SERVER_PORT - default `9000`
* KAFKA_ENDPOINT - default `localhost:9092`

## Show Active Consumers
`http://localhost:9000/consumers`

which gives 

```json
[
    {
      "protocolType": "consumer",
      "groupId": "consumer-1"
    }
    {
      "protocolType": "consumer",
      "groupId": "consumer-2"
    }
    {
      "protocolType": "consumer",
      "groupId": "consumer-3"
    }
]
```

## Show specific consumer info
`curl http://localhost:9000/consumers/<ConsumerGroupId>`

```json
[
    {
      "owner": "consumer-2_/132.34.134.12",
      "lag": 155758,
      "log_end_offset": 2580124,
      "offset": 2424366,
      "partition": 1,
      "topic": "foobar",
      "group": "consumer-1"
    }
    {
      "owner": "consumer-2_/132.34.134.12",
      "lag": 155758,
      "log_end_offset": 2580124,
      "offset": 2424366,
      "partition": 2,
      "topic": "foobar",
      "group": "consumer-1"
    }
    {
      "owner": "consumer-2_/132.34.134.12",
      "lag": 155758,
      "log_end_offset": 2580124,
      "offset": 2424366,
      "partition": 3,
      "topic": "foobar",
      "group": "consumer-1"
    }
]
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