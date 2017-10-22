# SmartCart

The [Flomio SmartCart](https://www.youtube.com/watch?v=XJkNdyDoPd0&feature=youtu.be)
prototype is a solution for pairing a phone with a cart/basket outfitted with an
rfid reader such that a real time tally of the products in the cart is
available, displayed on an app[3].

In this demo, this is simply a list of products, grouped by product type, and
showing a tally.

# Pairing Process

The SmartCart is a BLE peripheral and advertising with a name of `Flomio
SmartCart` with a random suffix [1], sufficient to discriminate between multiple
carts in the field.

The phone app starts scanning for nearby carts at high frequency/low latency
while giving a visual indication of the closest cart, going from red (low
signal) to green (high signal). Finally, once the phone has reached the signal
strength indicator threshold, it begins to connect (and will reconnect
automatically thereafter, after the initial connection)

    While the BLE tap-to-pair works surprisingly well, currently this means
    tapping the Mini-PC in an awkward place. Alternatives could include QR/NFC
    tag, with the app consulting the cloud for the ip etc that the mini-pc could
    report.

    Another alternative, would be to have an NFC terminal acting in card
    emulation mode, sending a message to the phone that way.

    On Apple, Apple Pay passes associated with an app could probably be used,
    using the PassKit api. This requires an Apple Pay compatible reader though.

# Communication Method

The SmartCart exposes both a WebSocket server (on port 8010) [2] and a custom
generic command/response framework implemented via 2 BLE characteristics
using chunked messages (as commonly seen on BLE nfc readers such as the FloBLE
Plus), so as to work around the 20 byte limit of BLE messages
(characteristics r/w)

When the Cart IP is unknown, such as when first deploying on the field, to a
place with new WIFI, or when using DHCP and the IP may change, it's only
possible to communicate with BLE.

It's possible to set the cart's WIFI configuration, and also to get the IP so as
to be able to communicate with the WebSocket.

# Troubleshooting

It's possible to connect to the websocket port on 8010 and get a log of the
tag events.

eg.
```bash
npm install wscat -g
wscat --connect ws://192.168.10.145:8010
```

# Configuration

The reader is configured by a simple json file `/home/pi/.skytek-config.json`
(not directly exposed)

Configuration is done using the `set_config` WebSocket command (as well as
various specialised BLE commands)

This is the structure of the configuration:

```json
{
  "powerLevel": 15,
  "tagPersistence": 7,
  "tagPersistenceMs": 3000,
  "sleepMsAfterGetTags" : 50,
  "terminateOnWorkerError": false,
  "autoUpdateFrequencySeconds": 120,
  "numScanRoundsBetweenSearchingNewReaders": 5,
  "numberAntennas": 1,
  "autoUpdate": false,
  "wifi": {
    "ssid": "RMHotel1",
    "password": "mekong678"
  }
}
```

Note re: `tagPersistence` vs `tagPersistenceMs`

On cart versions older than 7/27/2017, `tagPersistence` is used. See Scan Loop
Config for details.

## Reader Config

### powerLevel

This is the rf output power in db.

Valid ranges are 10 to 27, in increments of `0.1`

Note that setting the power level to max power of 27 isn't always necessarily
the best move!

There's a balancing act between detecting tags too far from the cart and NOT
detecting them while in the cart, BUT at a distance far from the antenna[s]
(such as as in the extremities of the cart)

### numberAntennas

Valid values are `1` or `2`

The rfid reader has ports for up to 2 antennas, and will multiplex between them,
if configured to do so.

**IMPORTANT: Note that if configured wrongly, and the reader attempts to use a
port with no antenna, damage can occur**

For this reason, unused antenna ports on the reader SHOULD be outfitted with
plugs to drain unused energy. Such as [these](https://www.amazon.com/DHT-Electronics-coaxial-connector-Termination/dp/B00BXUYDMM/ref=sr_1_1?ie=UTF8&qid=1501000548&sr=8-1&keywords=sma+terminator)

## Scan Loop Config

* `tagPersistence`
* `tagPersistenceMs`
* `numScanRoundsBetweenSearchingNewReaders`
* `sleepMsAfterGetTags`

As the reader for any given invocation of a low level `getTags()` reader call,
will not always get ALL the tags (How could it? When would it know when to stop
?) in the given duration(not configurable), the application level code buffers
the seen tags, and presents up any tags that have been seen in the last
`tagPersistenceMs` ms.

    In cart versions older than July 27th 2017, the config was `tagPersistence`
    and it meant the amount of invocations of `getTags()`

In between each `getTags()` call it sleeps for `sleepMsAfterGetTags` ms.

Every `numScanRoundsBetweenSearchingNewReaders` the reader connections will be
disposed, and the usb ports scanned for new readers [4].

## Wifi Config

[6] Note that the wifi configuration is redundant, and only when using the
specialised BLE command, will the wpa_supplicant.conf be set, and the adapter
reconfigured.

**Do NOT set this from WebSocket**

## Automatic Updates

* `autoUpdateFrequencySeconds`
* `autoUpdate`

This is a rudimentary OTA update feature, that pings the git repository for
changes, every `autoUpdateFrequencySeconds`. 

**note that this currently only works if the reader is configured to use the master branch**

# Commands / Events

## WebSocket Events

The WebSocket listens on port 8010, and once connected emits `foundTags`
of all the tags it has in its read buffer:

```typescript
interface FoundTagsEvent {
    eventName: 'foundTags'
    tags: Array<{
      epc: string,
      readerIx: number
      readerName: string,
      antennaIx: number,
      readerFirmware: string,
      round: number,
      tagType: string
      firstSeen: number
      lastSeen: number
    }>
}
```

## WebSocket Commands

The WebSocket commands loosely follow JSON-RPC. All arguments are encoded in
json.

Each command has the following format (TypeScript syntax):
```typescript
    {
        cmd: string,
        id: number,
        ... // additional arguments dependent on `cmd`
    }
```

The `cmd` identifies the type of command

If a reply is applicable to the command, the response will have the `id` echoed
in the message:

Each response has the following format (TypeScript syntax):

```typescript
    {
        eventName: 'response',
        id: number
    }
```

### Clear Events

```json
    {
      "cmd": "clear_events",
      "id": 0
    }
```

This command will clear the buffer of tags in the cart.

### Set Config

```json
    {
        "cmd": "set_config",
        "config" : {
            ... // see Configuration section
        }
    }

```

All configuration values will be recursively merged into the existing config.

### Get Version

Returns the git commit hash of the SmartCart mini pc software in use.

```json
    {
        "cmd" : "get_version",
        "id" : 1
    }
```

Response:
```json
    {
        "eventName" : "response",
        "id" : 1,
        "version" : "$git-hash"
    }

```

## BLE Commands

The BLE command/response framework is custom, and documentation TBD [7].

# Administration

See the websocket/ble commands

# Prototype Components

## Mini Pc Used

Raspberry Pi 3 running raspbian 8

**This requires a 2A power supply or brownouts may occur**

It's USB ports output a maximum of 1.2A across all ports

## RFID Reader used:

[Skyetek SuperNova](https://skyetek.zendesk.com/hc/en-us/articles/212226766-Getting-Started-with-the-SkyeTek-SuperNova)

This reader consumes 500mA while in scan mode.

## Antenna used:

900MHz antennas are used to comply with the FCC' allowed frequency for
UHF RFID in the USA

Some of the off-the-shelf compatible antennas:

* Times-7 A7040
* Times-7 A7060
* Times-7 [A7030C](http://www.times-7.com/a7030c-circular-polarised-uhf-shelf-antenna.html)

## Tags used:

[AD-237R6 inlays](http://rfid.averydennison.com/content/dam/averydennison/rfid/Global/Documents/datasheets/AD-237r6-Datasheet-v1.pdf) that have the [Impinj Monza R6](https://support.impinj.com/hc/article_attachments/203208604/Monza%20R6%20Tag%20Chip%20Datasheet%20R4%2020160818.pdf) chips inside.

# Backend API

The backend is using AWS Api Gateway / lambda, storing the data in DynamoDB.

Administration is done with the [TrackPak application](https://github.com/yummytech/trackpak_android/)

## Models

```typescript
interface Product {
  sku: number,
  name: string,
  description: string
  price: number,
  image: string
}

interface IEPC {
  sku: number,
  epc: string
}
```

The IProduct model is the class of product. The IEPC is the instance of that
product. They join on the `sku` field.

Note that the `image` field of a product expects a base64 encoded bitmap
(png/jpg/gif)

## BASE_URL

https://89tvfawd79.execute-api.us-east-1.amazonaws.com/

## Authentication

* Currently no authentication is in use!*

## Handlers

* POST - dev/product
* PATCH - dev/product/{sku}
* DELETE - dev/product/{sku}
* GET - dev/product/{sku}
* GET - dev/epc/{epc}
* POST - dev/epc
* DELETE - dev/epc/{epc}
* GET - dev/product-by-epc/{epc}
* GET - dev/epcs-by-product/{sku}

# Going forward

* We need multiple platform support
* The db backend needs authentication/authorization
* Secure, encrypted connections should be used for both the websocket and BLE.
    * The FloBLE Plus authentication flow for BLE could be copied.

# Footnotes

[1] Currently a simple random short suffix such that the advertisement name
    does not exceed the BLE limits.

[2] No ssl certificate is used. i.e.  ws:// and not wss://

[3] Currently the app must be preinstalled

[4] It's probably safe to set this quite high, higher than the current default
   config as when an exception occurs, via timeout etc, the reader connections
   are disposed and renewed.

[5] set_config ws command should really restart the wifi as well the BLE cmd

[6] Ideally this should have a single source of truth: the wpa_supplicant conf
    file. The work of parsing this was given a low priority, and a redundant
    config was used.

[7] Originally it was intended that Ethernet would be used to dock the smart
    phones with a cart. The BLE framework was put together hastily in response
    to finding out that not many models of android actually support USB-OTG
    ethernet adapters.
