#!/usr/bin/env python3
"""
Test script for sending GTFS-RT protobuf FeedEntity to the VP endpoint.
Requires: pip install gtfs-realtime-bindings requests
"""

import requests
import time
from google.transit import gtfs_realtime_pb2

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

def send_vehicle_position(base_url='http://localhost:8087', feed_id='ztp-feed', agency_id='ztp-agency'):
    """Send vehicle position to the protobuf endpoint"""
    entity = create_test_feed_entity()

    url = f'{base_url}/vp/f/{feed_id}/a/{agency_id}'
    headers = {'Content-Type': 'application/x-protobuf'}
    data = entity.SerializeToString()

    print(f"Sending FeedEntity to {url}")
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

if __name__ == '__main__':
    print("=" * 60)
    print("GTFS-RT Protobuf Test - Vehicle Position Endpoint")
    print("=" * 60)
    send_vehicle_position()

