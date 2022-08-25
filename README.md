# Real-Time Movie Recommendations with DataStax Graph

This repo is a companion to the Real-time Recommendations with DS Graph by Aaron Ploetz and Dr. Denise Gosnell.

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

For the purposes of the article, the only configuration change I made was the data center name, which can be done in the `resoueces/cassandra/conf/cassandra-rackdc.properties` file.

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

### Environment variables
Following the steps from the README, ensure that the following env vars are set via an export command:

``bash
DSE_ENDPOINT
DSE_DC
ASTRA_STREAM_URL
ASTRA_STREAM_TOKEN
ASTRA_STREAM_TENANT
```

## Starting the Service Layer

Now, I’ll start up my Spring Boot app via Maven:

```bash
cd ~/Documents/workspace/graphRec
mvn spring-boot:run
```

## Invoking Endpoints
