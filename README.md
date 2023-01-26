# Real-Time Movie Recommendations with DataStax Graph

This repo is a companion to the [Real-time Recommendations with DS Graph](https://thenewstack.io/real-time-recommendations-with-graph-and-event-streaming/) article by Aaron Ploetz and Dr. Denise Gosnell.

## Prerequisites

### DataStax Enterprise - Graph

Download DataStax Enterprise 6.8 : https://downloads.datastax.com/#enterprise

#### Installation

```bash
mv ~/Downloads/dse-6.8.22-bin.tar.gz ~/local
cd ~/local
tar -zxvf dse-6.8.22-bin.tar.gz
cd dse-6.8.22
```

For the purposes of the article, the only configuration change I made was the data center name, which can be done in the `resources/cassandra/conf/cassandra-rackdc.properties` file.

```
dc=MovingPictures
```

#### Starting DSE Graph

```bash
cd dse-6.8.22
bin/dse cassandra -g
```

Verify that it’s running:

```bash
bin/nodetool status

Datacenter: MovingPictures
==========================
Status=Up/Down
|/ State=Normal/Leaving/Joining/Moving/Stopped
--  Address    Load       Owns (effective)  Host ID
UN  127.0.0.1  248.32 MiB  100.0%            26ea88fd-
```

Make note of the `Datacenter` listed above, as you'll need to set that as an environment variable later.

#### Loading the movie data

### DataStax Astra Streaming

Either use your existing DataStax Astra account, or create a new (free) account: https://astra.datastax.com

#### Create a new topic

Create a new Streaming tenant.  Then click on the "Topics" tab.  Create a new topic inside of the "default" namespace, and enter "user-ratings" as the topic's name.

 - Tick the "persistent" switch to make sure the topic is persistent.
 - Leave the "partitioned" switch to the default, so that it is _not_ partitioned.

Be sure to record your stream URL, tenant name, and Astra streaming token.

Additional, step-by-step instructions for creating an Astra Streaming tenant and topic can be found here: https://awesome-astra.github.io/docs/pages/astra/create-topic/.

### Environment variables

Ensure that the following env vars are set via an `export` command.  Example:

```bash
export DSE_ENDPOINT="127.0.0.1"
export DSE_DC="MovingPictures"
export ASTRA_STREAM_URL="pulsar+ssl://pulsar-gcp-uscentral1.streaming.datastax.com:6651"
export ASTRA_STREAM_TOKEN="eyJhbGciOiJSUz...blahblahblah...6LHfSQzVTGchAs8YJ1gQ"
export ASTRA_STREAM_TENANT="movie-recommendations-aaron"
```

## Starting the Service Layer

Next, start up my Spring Boot app via Maven:

```bash
cd ~/Documents/workspace/graphRec
mvn spring-boot:run
```

## Invoking Endpoints

The Swagger spec can be found here [http://127.0.0.1:8080/swagger-ui/index.html#/](http://127.0.0.1:8080/swagger-ui/index.html#/) while the app is running.

## Front End

The front end layer was created w/ Vaadin.  To view, simply open [http://127.0.0.1:8080](http://127.0.0.1:8080) while the app is running.
