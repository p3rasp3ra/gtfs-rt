#!/usr/bin/env python3
"""
MQTT Test Publisher for GTFS-RT Vehicle Positions
Publishes protobuf messages to MQTT broker using Digitransit topic structure:
/<feed_format>/<type>/<feed_id>/<agency_id>/<agency_name>/<mode>/<route_id>/<direction_id>/
<trip_headsign>/<trip_id>/<next_stop>/<start_time>/<vehicle_id>/<geohash_*>/<short_name>/<color>/

Usage:
    python mqtt_test_publisher.py [--broker localhost] [--interval 5]

For REST API testing, use test_proto_vp.py instead.

Requires:
    pip install gtfs-realtime-bindings paho-mqtt protobuf
"""

import time
import argparse
from google.transit import gtfs_realtime_pb2
from google.protobuf import text_format
import paho.mqtt.client as mqtt

def create_test_vehicle_position(vehicle_id, lat, lon):
    """Create a test GTFS-RT FeedEntity with VehiclePosition"""
    entity = gtfs_realtime_pb2.FeedEntity()
    entity.id = vehicle_id

    # Trip information
    entity.vehicle.trip.trip_id = f"trip_{vehicle_id}"
    entity.vehicle.trip.route_id = "route_123"
    entity.vehicle.trip.direction_id = 1
    entity.vehicle.trip.start_date = "20260109"
    entity.vehicle.trip.start_time = "14:30:00"

    # Position
    entity.vehicle.position.latitude = lat
    entity.vehicle.position.longitude = lon
    entity.vehicle.position.bearing = 180.0
    entity.vehicle.position.speed = 25.5

    # Timestamp
    entity.vehicle.timestamp = int(time.time())

    # Stop info
    entity.vehicle.stop_id = "stop_001"
    entity.vehicle.current_status = gtfs_realtime_pb2.VehiclePosition.STOPPED_AT

    # Occupancy
    entity.vehicle.occupancy_status = gtfs_realtime_pb2.VehiclePosition.FEW_SEATS_AVAILABLE

    return entity

def build_digitransit_topic(feed_id, agency_id, vehicle_id):
    """
    Build Digitransit MQTT topic structure:
    /<feed_format>/<type>/<feed_id>/<agency_id>/<agency_name>/<mode>/
    <route_id>/<direction_id>/<trip_headsign>/<trip_id>/<next_stop>/
    <start_time>/<vehicle_id>/<geohash_*>/<short_name>/<color>/
    """
    return (
        f"/gtfsrt/vp/{feed_id}/{agency_id}/ZTP/BUS/"
        f"route_123/1/Downtown/trip_{vehicle_id}/stop_001/"
        f"14:30:00/{vehicle_id}/u/g/j/k/123/FF0000/"
    )

def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("✅ Connected to MQTT broker")
    else:
        print(f"❌ Failed to connect, return code {rc}")

def on_publish(client, userdata, mid, reason_code=None, properties=None):
    print(f"📤 Published message {mid}")

def main():
    parser = argparse.ArgumentParser(description='MQTT Test Publisher for GTFS-RT')
    parser.add_argument('--broker', default='localhost', help='MQTT broker host')
    parser.add_argument('--port', type=int, default=1883, help='MQTT broker port')
    parser.add_argument('--feed-id', default='ztp-feed', help='Feed ID')
    parser.add_argument('--agency-id', default='ztp-agency', help='Agency ID')
    parser.add_argument('--interval', type=int, default=5, help='Publish interval in seconds')
    parser.add_argument('--count', type=int, default=0, help='Number of messages (0=infinite)')
    parser.add_argument('--vehicles', type=int, default=3, help='Number of test vehicles')
    args = parser.parse_args()

    # Create MQTT client
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_publish = on_publish

    print(f"🔌 Connecting to MQTT broker at {args.broker}:{args.port}")
    client.connect(args.broker, args.port, 60)
    client.loop_start()

    # Wait for connection
    time.sleep(1)

    # Test vehicle starting positions (around Krakow)
    test_positions = [
        (50.0647, 19.9450),  # Vehicle 1
        (50.0537, 19.9353),  # Vehicle 2
        (50.0748, 19.9554),  # Vehicle 3
    ]

    message_count = 0
    try:
        while args.count == 0 or message_count < args.count:
            for i in range(min(args.vehicles, len(test_positions))):
                vehicle_id = f"vehicle_{i+1:03d}"
                lat, lon = test_positions[i]

                # Simulate slight movement
                lat += (message_count * 0.0001)
                lon += (message_count * 0.0001)

                # Create FeedEntity
                entity = create_test_vehicle_position(vehicle_id, lat, lon)

                # Build topic
                topic = build_digitransit_topic(args.feed_id, args.agency_id, vehicle_id)

                # Serialize to protobuf binary
                payload = entity.SerializeToString()

                # Publish
                result = client.publish(topic, payload, qos=1)

                print(f"📍 Published: {vehicle_id} at ({lat:.4f}, {lon:.4f}) to {topic[:50]}...")

                if result.rc != mqtt.MQTT_ERR_SUCCESS:
                    print(f"❌ Publish failed: {result.rc}")

            message_count += 1

            if args.count == 0 or message_count < args.count:
                print(f"⏱️  Waiting {args.interval}s... (Ctrl+C to stop)")
                time.sleep(args.interval)

    except KeyboardInterrupt:
        print("\n🛑 Stopped by user")
    finally:
        client.loop_stop()
        client.disconnect()
        print(f"✅ Published {message_count * args.vehicles} messages total")

if __name__ == '__main__':
    main()

