# jen8583 [![Build Status](https://travis-ci.org/chiknrice/jen8583.svg?branch=master)](https://travis-ci.org/chiknrice/jen8583) [![Coverage Status](https://coveralls.io/repos/chiknrice/jen8583/badge.svg?branch=master&service=github)](https://coveralls.io/github/chiknrice/jen8583?branch=master) 
A *J*ava *EN*coder (and decoder) for [ISO8583](http://en.wikipedia.org/wiki/ISO_8583) messages.

## Overview
jen8583 is a Java based tool which uses XML to define how parts of the ISO8583 message is encoded & decoded. Although ISO8583 has different [versions](http://en.wikipedia.org/wiki/ISO_8583#ISO_8583_version), jen8583 is not limited to a particular version and is designed to handle only the fundamental structure of an ISO8583 message.  However, the tool provides a facility for a user to handle version specific encoding & decoding.

## Usage

### Configuration
Create a configuration located in the classpath (e.g. `config.xml`) and point the location of the schema to http://chiknrice.github.io/schema/jen8583-0.0.3.xsd (ensure the correct version of the library is used for the xsd, in this example 0.0.3 was used).  An example configuration below defines a default char encoding for VLI (variable length indicators), char encoding for numeric fields as well as date fields.  By default, fields not marked with mandatory="false" are mandatory.  The configuration can have a header element as well as message-ext elements but the example below shows the minimal set of elements required to be defined.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<iso xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.chiknrice.org/jen8583"
    xsi:schemaLocation="http://www.chiknrice.org/jen8583 http://chiknrice.github.io/schema/jen8583-0.0.3.xsd">
    
    <defaults>
        <var tag-encoding="CHAR" length-encoding="CHAR" />
        <alpha justified="LEFT" trim="true" />
        <numeric encoding="CHAR" />
        <date timezone="SYSTEM" encoding="CHAR" />
        <ordinality mandatory="true" />
    </defaults>

    <mti-encoding type="CHAR" />

    <msg-bitmap type="BINARY" />
    
    <message mti="0200">
        <alpha-var index="2" length="2" />
        <numeric index="3" length="6" mandatory="false" />
        <numeric index="4" length="12" />
        <date index="7" format="MMddHHmmss" timezone="UTC" />
        <numeric index="11" length="6" />
        <date index="12" format="HHmmss" />
        <date index="13" format="MMdd" />
        <composite index="28" mandatory="false">
            <alpha length="1" />
            <numeric length="8" />
        </composite>
    </message>
    
</iso>
```

###Creating an IsoMessageCodec
Create an instance of the `IsoMessageCodec` using the your configuration file name:

```java
IsoMessageDef def = IsoMessageDef.build("config.xml");
IsoMessageCodec codec = new IsoMessageCodec(def);
```

The codec created is thread safe and can be used for encoding and decoding IsoMessages across multiple threads.

###Encoding
Encoding and IsoMessage requires you to have built an IsoMessage instance first.

Creating instances of IsoMessage requires an MTI (in this example a 0200 message is created):
```java
IsoMessage isoMessage = new IsoMessage(200);
```

Setting fields of an IsoMessage requires an index parameter and the value:
```java
isoMessage.setField(7, new Date());
isoMessage.setField("28.1", "C");
isoMessage.setField("28.2", 200);
```

The index value can be an integer or an index expression.  An index expression is a string which contains indexes separated by dot (.) that refers to nested fields.

Encoding an IsoMessage instance would result in a byte array (without length prefix):
```java
byte[] encodedMessage = codec.encode(isoMessage);
```

###Decoding
Decoding messages decodes a byte array (without length prefix) to an IsoMessage instance:
```java
IsoMessage decodedMessage = codec.decode(isoBytes);
```

After decoding, fields can be accessed from the decoded message by passing the index or index expression.
```java
Date transmissionDate = decodedMessage.getField(7);
Integer stan = decodedMessage.getField(11);
String transactionFeePrefix = decodedMessage.getField("28.1");
Integer transactionFee = decodedMessage.getField("28.2");
```

## Advanced Configuration
TODO

## Customization
TODO
