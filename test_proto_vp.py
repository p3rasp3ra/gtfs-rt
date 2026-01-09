#!/usr/bin/env python3
"""
Test script for sending GTFS-RT protobuf FeedEntity to the VP endpoint.
Supports both binary protobuf and ASCII text format.
Requires: pip install gtfs-realtime-bindings requests
"""

import requests
import time
import sys
from google.transit import gtfs_realtime_pb2
from google.protobuf import text_format

def create_test_feed_entity():
    """Create a test GTFS-RT FeedEntity with VehiclePosition"""
    entity = gtfs_realtime_pb2.FeedEntity()
    entity.id = "test_vehicle_001"

    # Vehicle position data
    entity.vehicle.trip.trip_id = "trip_12345"
    entity.vehicle.trip.route_id = "route_67"
    entity.vehicle.trip.direction_id = 1
    entity.vehicle.trip.start_date = "20260109"
    entity.vehicle.trip.start_time = "14:30:00"

    # Position
    entity.vehicle.position.latitude = 52.2297
    entity.vehicle.position.longitude = 21.0122
    entity.vehicle.position.bearing = 180.0
    entity.vehicle.position.speed = 25.5

    # Timestamp (current time in seconds since epoch)
    entity.vehicle.timestamp = int(time.time())

    # Stop info
    entity.vehicle.stop_id = "stop_central_001"
    entity.vehicle.current_status = gtfs_realtime_pb2.VehiclePosition.STOPPED_AT

    # Occupancy
    entity.vehicle.occupancy_status = gtfs_realtime_pb2.VehiclePosition.FEW_SEATS_AVAILABLE

    return entity

def send_vehicle_position(base_url='http://localhost:8087', feed_id='ztp-feed', agency_id='ztp-agency', use_text_format=False):
    """Send vehicle position to the protobuf endpoint"""
    entity = create_test_feed_entity()

    url = f'{base_url}/vp/f/{feed_id}/a/{agency_id}'

    if use_text_format:
        # ASCII text format for debugging
        headers = {'Content-Type': 'text/plain'}
        data = text_format.MessageToString(entity)
        print(f"Sending FeedEntity in ASCII text format to {url}")
        print(f"\n--- Message Content (text) ---")
        print(data)
        print("--- End Message ---\n")
        data = data.encode('utf-8')
    else:
        # Binary protobuf format
        headers = {'Content-Type': 'application/x-protobuf'}
        data = entity.SerializeToString()
        print(f"Sending FeedEntity in binary protobuf format to {url}")

    print(f"Entity ID: {entity.id}")
    print(f"Vehicle trip_id: {entity.vehicle.trip.trip_id}")
    print(f"Position: ({entity.vehicle.position.latitude}, {entity.vehicle.position.longitude})")

    try:
        response = requests.post(url, headers=headers, data=data)

        if response.status_code == 200:
            print("✅ SUCCESS: Vehicle position sent successfully!")
        else:
            print(f"❌ ERROR: Status {response.status_code}")
            print(f"Response: {response.text}")

        return response

    except requests.exceptions.RequestException as e:
        print(f"❌ REQUEST FAILED: {e}")
        return None

def get_feed(base_url='http://localhost:8087', use_text_format=False):
    """Get aggregated GTFS-RT feed"""
    url = f'{base_url}/gtfs-rt/feed.pb'

    if use_text_format:
        headers = {'Accept': 'text/plain'}
        print(f"\nGetting feed in ASCII text format from {url}")
    else:
        headers = {'Accept': 'application/x-protobuf'}
        print(f"\nGetting feed in binary protobuf format from {url}")

    try:
        response = requests.get(url, headers=headers)

        if response.status_code == 200:
            print("✅ SUCCESS: Feed retrieved!")
            print(f"Content-Type: {response.headers.get('Content-Type')}")
            print(f"Content-Length: {len(response.content)} bytes")

            if use_text_format:
                print("\n--- Feed Content (text) ---")
                print(response.text[:500])  # Print first 500 chars
                if len(response.text) > 500:
                    print(f"... (truncated, total {len(response.text)} chars)")
                print("--- End Feed ---")
            else:
                # Parse binary feed
                feed = gtfs_realtime_pb2.FeedMessage()
                feed.ParseFromString(response.content)
                print(f"Feed contains {len(feed.entity)} entities")
                print(f"Feed timestamp: {feed.header.timestamp}")

        else:
            print(f"❌ ERROR: Status {response.status_code}")

        return response

    except requests.exceptions.RequestException as e:
        print(f"❌ REQUEST FAILED: {e}")
        return None

if __name__ == '__main__':
    use_text = '--text' in sys.argv or '-t' in sys.argv

    print("=" * 60)
    print("GTFS-RT Protobuf Test - Vehicle Position Endpoint")
    if use_text:
        print("Mode: ASCII TEXT FORMAT (development/debug)")
    else:
        print("Mode: BINARY PROTOBUF FORMAT (production)")
    print("=" * 60)

    # Send vehicle position
    send_vehicle_position(use_text_format=use_text)

    # Wait a moment for processing
    time.sleep(1)

    # Get aggregated feed
    get_feed(use_text_format=use_text)

    print("\n" + "=" * 60)
    print("Use --text or -t flag to test with ASCII text format")
    print("=" * 60)

