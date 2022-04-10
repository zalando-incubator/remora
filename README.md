# Remora

![Grafana Graph](https://raw.githubusercontent.com/imduffy15/remora-fetcher/master/img/grafana.png)

[Remora](https://github.com/zalando-incubator/remora) is a monitoring utility for [Apache Kafka](http://kafka.apache.org/) that provides consumer lag checking as a service. An HTTP endpoint is provided to request consumer group information on demand. Combining this with a time series database like [KairosDB](https://kairosdb.github.io/) it is possible to graph your consumer group status; see [remora fetcher](https://github.com/imduffy15/remora-fetcher) for an example of this. 

Remora is stable and **production ready**. A number of production kafka clusters in Zalando are being monitored by Remora right now!

## Inspiration

We created Remora after spending some time using Linkedin's [burrow](https://github.com/linkedin/Burrow) application for monitoring consumer lag and experiencing some performance problems (burrow shut down after an unknown amount with no error stack, alert or sign of error. We have no idea why but we had to keep restarting the app which was very annoying). Remora provides the [Kafka consumer group command](https://github.com/apache/kafka/blob/0.10.0/core/src/main/scala/kafka/admin/ConsumerGroupCommand.scala) as an HTTP endpoint.

## User Testimonials 

> We are using Kafka 0.10.2.1 extensively.  As almost all our applications depend on Kafka, we needed a way to visualise consumer data over a time period in order to discover issues with our consumers. Remora lets us do exactly this, it exposes consumer group metrics over HTTP which allow us to create alarms if a consumer has stopped or slowed consumption from a topic or even on a single partition. ~ Team Buffalo @ Zalando Dublin

> We are using Kafka 0.10.2.1 along with Akka Streams. We use Remora to track, alert, and visualise any lag within any of our components ~ Team Setanta @ Zalando Dublin

> We rely on Kafka for streaming DB change events on to other teams within our organisation. Remora greatly aids us in ensuring our Kafka and Kafka Connect components are functioning correctly by monitoring both the number of events been produced, and any lag present on a per consumer basis. It is proving an excellent tool in providing data which we use to trigger real time alerts ~ Team Warhol @ Zalando Dublin

> We use Kafka and Kafka Streaming to orchestrate the different components of our text processing pipeline. Through data provided by Remora, we monitor lags in differnet topics as part of our monitoring dashboard and alerting system. Remora makes it easier for us to quickly identify and respond to bottlenecks and problems. ~ Team Sapphire @ Zalando Dublin

> We are using Mirror Maker to replicate data between two Kafka brokers and Remora has been a great help to monitor the replication in real time. The metrics exposed by Remora are pushed to Datadog, on top of which we build dashboards and triggers to help us react in case of failure. ~ Sqooba Switzerland

## Getting started

### Dependencies

The latest release of [Remora](https://github.com/zalando-incubator/remora) supports [Apache Kafka](http://kafka.apache.org/) 0.10.2.1, 1.0.0, 1.1.1 and 2X

* For [Apache Kafka](http://kafka.apache.org/) 0.10.0.1 please see the [v0.1.0 release](https://github.com/zalando-incubator/remora/releases/tag/v0.1.0).
* For [Apache Kafka](http://kafka.apache.org/) 0.10.2.1 please see the [v0.2.0 release](https://github.com/zalando-incubator/remora/releases/tag/v0.2.0).
* For [Apache Kafka](http://kafka.apache.org/) 1.0.0 please see the [v1.0.5 release](https://github.com/zalando-incubator/remora/releases/tag/v1.0.5).
* For [Apache Kafka](http://kafka.apache.org/) 1.1.1 please see the [v1.0.6 release](https://github.com/zalando-incubator/remora/releases/tag/v1.0.6).
* For [Apache Kafka](http://kafka.apache.org/) 2X please see the [v1.0.7 release](https://github.com/zalando-incubator/remora/releases/tag/v1.0.7).

To find the latest releases, please see the following examples

`$ curl https://registry.opensource.zalan.do/teams/machina/artifacts/remora/tags | jq ".[] | .name"`

`$ pierone latest machina remora --url registry.opensource.zalan.do` (Which would require a `$ pip3 install stups-pierone`)

### Running it

Images for all versions are available on [Zalando opensource pierone](http://registry.opensource.zalan.do)

They can be used as follows:

```bash
docker run -it --rm -p 9000:9000 -e KAFKA_ENDPOINT=127.0.0.1:9092 registry.opensource.zalan.do/machina/remora
```

Run it with different log level:

```bash
docker run -it --rm -p 9000:9000 -e KAFKA_ENDPOINT=127.0.0.1:9092 -e 'JAVA_OPTS=-Dlogback-root-level=INFO' registry.opensource.zalan.do/machina/remora
```

For further examples see the [docker-compose.yml](basic-example/docker-compose.yml)

```bash
docker-compose -f basic-example/docker-compose.yml up
```

Run remora in IDE with kafka and zookeeper run by docker-compose 

```bash
docker-compose -f basic-example/docker-compose.yml up --scale remora=0
```

Remora is stateless, so test the scale of the API

```bash
docker-compose -f scale-example/docker-compose.yml up --scale remora=3
```

For examples with broker authentication see the [docker-compose.yml](auth-example/docker-compose.yml)

```bash
docker-compose -f auth-example/docker-compose.yml up
```

### Usage

#### Show active consumers 

```bash
$ curl http://localhost:9000/consumers
["consumer-1", "consumer-2", "consumer-3"]
```

#### Show specific consumer group information

```bash
$ curl http://localhost:9000/consumers/<ConsumerGroupId>
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
   ],
   "lag_per_topic":{
        "products-in" : 0
   }
}
```

#### Health

```bash
$ curl http://localhost:9000/health
{
    "cluster_id": "foobar_123",
    "controller": {
        "host": "xx.xxx.xxx.xxx",
        "id": 0,
        "id_string": "0",
        "port": 9092
    },
    "nodes": [
        {
            "host": "xx.xxx.xxx.xxx",
            "id": 0,
            "id_string": "0",
            "port": 9092
        }
    ]
}
```

### Metrics

```bash
$ curl http://localhost:9000/metrics
{
  "version": "3.0.0",
  "gauges": {
    "PS-MarkSweep.count": {
      "value": 7371
    },
    "PS-MarkSweep.time": {
      "value": 310404
    },
    "PS-Scavenge.count": {
      "value": 476530
    },
    "PS-Scavenge.time": {
      "value": 1234370
    },
    "blocked.count": {
      "value": 0
    },
    "count": {
      "value": 12
    },
    "daemon.count": {
      "value": 3
    },
    "deadlock.count": {
      "value": 0
    },
    "deadlocks": {
      "value": []
    },
    "heap.committed": {
      "value": 74448896
    },
    "heap.init": {
      "value": 132120576
    },
    "heap.max": {
      "value": 1860698112
    },
    "heap.usage": {
      "value": 0.021295551247380425
    },
    "heap.used": {
      "value": 39624592
    },
    "new.count": {
      "value": 0
    },
    "non-heap.committed": {
      "value": 73883648
    },
    "non-heap.init": {
      "value": 2555904
    },
    "non-heap.max": {
      "value": -1
    },
    "non-heap.usage": {
      "value": -72377144
    },
    "non-heap.used": {
      "value": 72377144
    },
    "pools.Code-Cache.committed": {
      "value": 27525120
    },
    "pools.Code-Cache.init": {
      "value": 2555904
    },
    "pools.Code-Cache.max": {
      "value": 251658240
    },
    "pools.Code-Cache.usage": {
      "value": 0.10638478597005209
    },
    "pools.Code-Cache.used": {
      "value": 26772608
    },
    "pools.Compressed-Class-Space.committed": {
      "value": 5242880
    },
    "pools.Compressed-Class-Space.init": {
      "value": 0
    },
    "pools.Compressed-Class-Space.max": {
      "value": 1073741824
    },
    "pools.Compressed-Class-Space.usage": {
      "value": 0.004756048321723938
    },
    "pools.Compressed-Class-Space.used": {
      "value": 5106768
    },
    "pools.Metaspace.committed": {
      "value": 41115648
    },
    "pools.Metaspace.init": {
      "value": 0
    },
    "pools.Metaspace.max": {
      "value": -1
    },
    "pools.Metaspace.usage": {
      "value": 0.984972144911835
    },
    "pools.Metaspace.used": {
      "value": 40497768
    },
    "pools.PS-Eden-Space.committed": {
      "value": 40894464
    },
    "pools.PS-Eden-Space.init": {
      "value": 33554432
    },
    "pools.PS-Eden-Space.max": {
      "value": 693108736
    },
    "pools.PS-Eden-Space.usage": {
      "value": 0.02002515230164405
    },
    "pools.PS-Eden-Space.used": {
      "value": 13879608
    },
    "pools.PS-Old-Gen.committed": {
      "value": 31457280
    },
    "pools.PS-Old-Gen.init": {
      "value": 88080384
    },
    "pools.PS-Old-Gen.max": {
      "value": 1395654656
    },
    "pools.PS-Old-Gen.usage": {
      "value": 0.018360885975505965
    },
    "pools.PS-Old-Gen.used": {
      "value": 25625456
    },
    "pools.PS-Survivor-Space.committed": {
      "value": 2097152
    },
    "pools.PS-Survivor-Space.init": {
      "value": 5242880
    },
    "pools.PS-Survivor-Space.max": {
      "value": 2097152
    },
    "pools.PS-Survivor-Space.usage": {
      "value": 0.0625
    },
    "pools.PS-Survivor-Space.used": {
      "value": 131072
    },
    "runnable.count": {
      "value": 4
    },
    "terminated.count": {
      "value": 0
    },
    "timed_waiting.count": {
      "value": 1
    },
    "total.committed": {
      "value": 148332544
    },
    "total.init": {
      "value": 134676480
    },
    "total.max": {
      "value": 1860698111
    },
    "total.used": {
      "value": 112001672
    },
    "waiting.count": {
      "value": 7
    }
  },
  "counters": {
    "KafkaClientActor.receiveCounter": {
      "count": 1443078
    }, 
    "foo.3.bar.GET-rejections": {
      "count": 1
    },
    "foo.3bar.GET-rejections": {
      "count": 1
     },
     "foo.4.bar.GET-rejections": {
       "count": 1
     },
     "health.GET-2xx": {
       "count": 1
     },
     "metrics.GET-2xx": {
       "count": 5
     }   
  },
  "histograms": {},
  "meters": {
    "KafkaClientActor.receiveExceptionMeter": {
      "count": 0,
      "m15_rate": 0,
      "m1_rate": 0,
      "m5_rate": 0,
      "mean_rate": 0,
      "units": "events/second"
    }
  },
  "timers": {
    "KafkaClientActor.receiveTimer": {
      "count": 1443078,
      "max": 0.496106,
      "mean": 0.023955427605185976,
      "min": 0.00855,
      "p50": 0.013158,
      "p75": 0.015818,
      "p95": 0.069989,
      "p98": 0.18145599999999998,
      "p99": 0.193686,
      "p999": 0.47478499999999996,
      "stddev": 0.04561406607191679,
      "m15_rate": 0.8672873098267513,
      "m1_rate": 0.8576046718431439,
      "m5_rate": 0.8704903354041494,
      "mean_rate": 0.34074311090084636,
      "duration_units": "milliseconds",
      "rate_units": "calls/second"
    },
    "RemoraKafkaConsumerGroupService.describe-timer": {
      "count": 1372542,
      "max": 3953.5592429999997,
      "mean": 165.67620936478744,
      "min": 4.631377,
      "p50": 22.125121,
      "p75": 124.258938,
      "p95": 527.534084,
      "p98": 800.1686119999999,
      "p99": 3316.226616,
      "p999": 3611.7097409999997,
      "stddev": 473.995637636751,
      "m15_rate": 0.8508541627113339,
      "m1_rate": 0.8450436821406069,
      "m5_rate": 0.8545541048945428,
      "mean_rate": 0.324087977369598,
      "duration_units": "milliseconds",
      "rate_units": "calls/second"
    },
    "RemoraKafkaConsumerGroupService.list-timer": {
      "count": 70536,
      "max": 2167.1663869999998,
      "mean": 163.13534839326368,
      "min": 56.275192999999994,
      "p50": 162.584495,
      "p75": 162.584495,
      "p95": 162.584495,
      "p98": 200.345285,
      "p99": 200.345285,
      "p999": 437.69862,
      "stddev": 23.321317038931596,
      "m15_rate": 0.016617378383700615,
      "m1_rate": 0.015343754688965648,
      "m5_rate": 0.016501030706405084,
      "mean_rate": 0.016655133007592124,
      "duration_units": "milliseconds",
      "rate_units": "calls/second"
    },
    "metrics.GET": {
      "count": 2,
      "max": 174.712404,
      "mean": 88.26670169568574,
      "min": 4.375856,
      "p50": 4.375856,
      "p75": 174.712404,
      "p95": 174.712404,
      "p98": 174.712404,
      "p99": 174.712404,
      "p999": 174.712404,
      "stddev": 85.15869346735195,
      "m15_rate": 0,
      "m1_rate": 0,
      "m5_rate": 0,
      "mean_rate": 0.6714371986436051,
      "duration_units": "milliseconds",
      "rate_units": "calls/second"
      }
  }
}
```

## Configuring Remora

Additional configuration can be passed via the following environment variables:

* **SERVER_PORT** - default `9000`
* **KAFKA_ENDPOINT** - default `localhost:9092`
* **ACTOR_TIMEOUT** - default `60 seconds`
* **AKKA_HTTP_SERVER_REQUEST_TIMEOUT** - `default 60 seconds`
* **AKKA_HTTP_SERVER_IDLE_TIMEOUT** - `default 60 seconds`
* **TO_REGISTRY** - `default false` reports lag/offset/end to metricsRegistry
* **EXPORT_METRICS_INTERVAL_SECONDS** - `default 20` interval to report lag/offset/end to metricsRegistry


### Configuring Remora with Cloudwatch

The following environment variables can be used to configure reporting to Cloudwatch:

* **CLOUDWATCH_ON** - `default false` reports metricsRegistry to cloudwatch, TO_REGISTRY will need to be switched on!
* **CLOUDWATCH_NAME** - `default 'remora'` name to appear on cloudwatch
* **CLOUDWATCH_METRIC_FILTER** - `default ''` metric names to filter on cloudwatch. Set the CLOUDWATCH_METRIC_FILTER variable to a regex string to filter out metric names that DO NOT match the regex.

### Configuring Remora with Datadog

The following environment variables can be used to configure reporting to Datadog:

* **DATADOG_ON** - `default false` reports metricsRegistry to Datadog, TO_REGISTRY will need to be switched on!
* **DATADOG_NAME** - `default 'remora'` name to appear on datadog
* **DATADOG_INTERVAL_MINUTES** - `default '1'` The reporting interval, in minutes.
* **DATADOG_AGENT_HOST** - `default 'localhost'` The host on which a Datadog agent is running.
* **DATADOG_AGENT_PORT** - `default '8125'` The port of the Datadog agent.
* **DATADOG_CONSUMER_GROUPS** - `default '[]'` List of consumer groups for which metrics will be sent to Datadog. An empty list means that all metrics will be sent.

__Reporting to datadog agent__:

Reporting to Datadog is done via [DogStatsD](https://docs.datadoghq.com/guides/dogstatsd/), which is usually running on the same host as remora.
However, as Remora is running inside a docker container, some steps are required to make the integration:

* Set **DATADOG_AGENT_HOST** as the address of the host on your machine
* In the datadog agent configuration, set `non_local_traffic: yes`

This way, a docker container running Remora will be able to communicate with a Datadog agent on the host machine.

## Building from source

### Prerequisites

 - Scala 2.11.8
 - SBT

### Build

Create docker image locally. The image will be built to `remora:<TAG-GITCOMMIT>` and `latest`

```bash
$ sbt docker:publishLocal
```

## Contributing

We are happy to accept contributions. First, take a look at our [contributing guidelines](CONTRIBUTING.md).

## TODO

Please check the [Issues Page](https://github.com/zalando-incubator/remora/issues)
for contribution ideas.

## Contact

Feel free to contact one of the [maintainers](MAINTAINERS).

## License

MIT
